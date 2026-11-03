package app.treasure.mcp.security;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.mcp.domain.McpToken;
import app.treasure.mcp.service.McpTokenService;
import app.treasure.shared.security.Roles;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Turns a valid MCP token into the identity of its member: the principal is the
 * member's userName, the same name the OIDC login produces, and the roles are
 * the ones stored with the token.
 */
@ApplicationScoped
public class McpTokenIdentityProvider implements IdentityProvider<McpTokenAuthenticationRequest>
{
	private static final Logger LOG = LoggerFactory.getLogger(McpTokenIdentityProvider.class);

	@Inject
	McpTokenService tokenService;

	@Override
	public Class<McpTokenAuthenticationRequest> getRequestType()
	{
		return McpTokenAuthenticationRequest.class;
	}

	@Override
	public Uni<SecurityIdentity> authenticate(McpTokenAuthenticationRequest request,
		AuthenticationRequestContext context)
	{
		// the token lookup is a blocking database call
		return context.runBlocking(() -> identityFor(request.getToken()));
	}

	private SecurityIdentity identityFor(String secret)
	{
		McpToken token = tokenService.authenticate(secret);
		if (token == null)
		{
			LOG.warn("Rejected an MCP request with an unknown or revoked token");
			throw new AuthenticationFailedException("Invalid MCP token");
		}
		Set<String> roles = token.getRoleSet();
		if (!roles.contains(Roles.ADMIN) && !roles.contains(Roles.SUPER_ADMIN))
		{
			// API access is for admins only; keys issued to regular users
			// before that rule no longer work
			LOG.warn("Rejected MCP token {} of {}: it has no admin role", token.id,
				token.getMember().getUserName());
			throw new AuthenticationFailedException("MCP token without admin role");
		}
		LOG.debug("MCP request authenticated as {} with token {}", token.getMember().getUserName(), token.id);
		return QuarkusSecurityIdentity.builder()
			.setPrincipal(new QuarkusPrincipal(token.getMember().getUserName()))
			.addRoles(roles)
			.build();
	}
}
