package app.treasure.shared.security;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import jakarta.ws.rs.container.ContainerResponseContext;

/**
 * Adds the browser security headers to every page the controllers render. The
 * Content-Security-Policy lets a page run scripts only from this app and from
 * the two CDNs main.html loads (Carbon web components, htmx), so text that
 * found its way into a page cannot run as script. That is also why templates
 * carry no inline scripts and no on* attributes: the policy would block them.
 */
public class SecurityHeadersFilter
{
	static final String CONTENT_SECURITY_POLICY = String.join("; ",
		"default-src 'self'",
		"script-src 'self' https://1.www.s81c.com https://unpkg.com",
		// Carbon's web components and the templates set style attributes
		"style-src 'self' 'unsafe-inline'",
		// the vendored Carbon stylesheet loads IBM Plex from IBM's CDN
		"font-src 'self' https://1.www.s81c.com",
		"img-src 'self' data:",
		"connect-src 'self'",
		"object-src 'none'",
		"base-uri 'self'",
		"frame-ancestors 'none'");

	@ServerResponseFilter
	public void addSecurityHeaders(ContainerResponseContext response)
	{
		response.getHeaders().putSingle("Content-Security-Policy", CONTENT_SECURITY_POLICY);
		response.getHeaders().putSingle("X-Content-Type-Options", "nosniff");
	}
}
