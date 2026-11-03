package app.treasure.mcp.security;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/**
 * The bearer token an MCP client sent; checked by
 * {@link McpTokenIdentityProvider}.
 */
public class McpTokenAuthenticationRequest extends BaseAuthenticationRequest
{
	private final String token;

	public McpTokenAuthenticationRequest(String token)
	{
		this.token = token;
	}

	public String getToken()
	{
		return token;
	}
}
