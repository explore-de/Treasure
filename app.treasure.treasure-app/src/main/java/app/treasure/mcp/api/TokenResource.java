package app.treasure.mcp.api;

import java.util.List;

import org.jboss.resteasy.reactive.RestForm;

import app.treasure.mcp.domain.McpToken;
import app.treasure.mcp.repository.McpTokenRepository;
import app.treasure.mcp.service.McpTokenService;
import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import app.treasure.shared.util.FlashKeys;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

/**
 * Personal API tokens for the MCP endpoint. Everyone manages only their own
 * tokens; a new token is shown once, on the page the create form returns.
 */
@Authenticated
@Path("/tokens")
public class TokenResource extends Controller
{
	private static final int MAX_NAME_LENGTH = 100;

	@Inject
	McpTokenRepository tokenRepository;

	@Inject
	McpTokenService tokenService;

	@Inject
	MemberRepository memberRepository;

	@Inject
	SecurityIdentity securityIdentity;

	@CheckedTemplate
	public static class Templates
	{
		private Templates()
		{
			// static
		}

		public static native TemplateInstance index(List<McpToken> tokens, String newSecret, String mcpUrl);
	}

	@GET
	@Path("")
	public TemplateInstance index(@Context UriInfo uriInfo)
	{
		return Templates.index(myTokens(), null, mcpUrl(uriInfo));
	}

	@POST
	@Path("")
	@Transactional
	public TemplateInstance create(@RestForm String name, @Context UriInfo uriInfo)
	{
		Member member = currentMember();
		if (member == null)
		{
			flash(FlashKeys.ERROR, "Your member profile does not exist yet, so no API key can be created.");
			seeOther("/tokens");
			return null;
		}
		if (name == null || name.isBlank() || name.trim().length() > MAX_NAME_LENGTH)
		{
			flash(FlashKeys.ERROR, "Give the API key a name of at most " + MAX_NAME_LENGTH + " characters.");
			seeOther("/tokens");
			return null;
		}
		McpTokenService.IssuedToken issued = tokenService.issue(member, name, securityIdentity.getRoles());
		// rendered directly instead of redirecting, so the secret never travels
		// in a flash cookie
		return Templates.index(myTokens(), issued.secret(), mcpUrl(uriInfo));
	}

	@POST
	@Path("/{id}/revoke")
	@Transactional
	public void revoke(@PathParam("id") Long id)
	{
		if (tokenService.revoke(id, currentMember()))
		{
			flash(FlashKeys.SUCCESS, "API key revoked. Clients using it can no longer connect.");
		}
		else
		{
			flash(FlashKeys.ERROR, "That API key does not exist.");
		}
		seeOther("/tokens");
	}

	private List<McpToken> myTokens()
	{
		Member member = currentMember();
		return member != null ? tokenRepository.findByMember(member) : List.of();
	}

	private Member currentMember()
	{
		return memberRepository.findByUsername(securityIdentity.getPrincipal().getName());
	}

	private static String mcpUrl(UriInfo uriInfo)
	{
		return uriInfo.getBaseUriBuilder().replacePath("/mcp").build().toString();
	}
}
