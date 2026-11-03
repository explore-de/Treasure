package app.treasure.mcp.security;

import java.util.Set;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Reads "Authorization: Bearer trs_..." on the MCP endpoint. It is selected for
 * /mcp by quarkus.http.auth.permission.mcp.auth-mechanism, and ignores every
 * other path, so a token never opens the web pages, which keep the OIDC login.
 */
@ApplicationScoped
public class McpTokenAuthenticationMechanism implements HttpAuthenticationMechanism
{
	/** The name used in application.properties to select this mechanism. */
	public static final String SCHEME = "mcp-token";

	private static final String BEARER = "Bearer ";

	@Override
	public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager)
	{
		if (!isMcpPath(context.normalizedPath()))
		{
			return Uni.createFrom().nullItem();
		}
		String header = context.request().getHeader(HttpHeaderNames.AUTHORIZATION);
		if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length()))
		{
			return Uni.createFrom().nullItem();
		}
		String token = header.substring(BEARER.length()).trim();
		McpTokenAuthenticationRequest request = new McpTokenAuthenticationRequest(token);
		HttpSecurityUtils.setRoutingContextAttribute(request, context);
		return identityProviderManager.authenticate(request);
	}

	@Override
	public Uni<ChallengeData> getChallenge(RoutingContext context)
	{
		return Uni.createFrom().item(new ChallengeData(HttpResponseStatus.UNAUTHORIZED.code(),
			HttpHeaderNames.WWW_AUTHENTICATE, "Bearer realm=\"treasure-mcp\""));
	}

	@Override
	public Set<Class<? extends AuthenticationRequest>> getCredentialTypes()
	{
		return Set.of(McpTokenAuthenticationRequest.class);
	}

	@Override
	public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context)
	{
		return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, SCHEME));
	}

	private static boolean isMcpPath(String path)
	{
		return path != null && (path.equals("/mcp") || path.startsWith("/mcp/"));
	}
}
