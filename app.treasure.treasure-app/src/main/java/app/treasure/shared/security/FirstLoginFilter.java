package app.treasure.shared.security;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import app.treasure.member.service.MemberProvisioningService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;

/**
 * Runs on every request that reached a controller. If the signed-in user has no
 * Member row yet, one is created from the ID token. Only real OIDC logins carry
 * a token; test identities and anonymous requests are skipped.
 */
@ApplicationScoped
public class FirstLoginFilter
{
	@Inject
	SecurityIdentity securityIdentity;

	@Inject
	MemberProvisioningService memberProvisioningService;

	@ServerRequestFilter(priority = Priorities.USER)
	public void ensureMemberExists()
	{
		if (securityIdentity == null || securityIdentity.isAnonymous())
		{
			return;
		}
		if (!(securityIdentity.getPrincipal() instanceof JsonWebToken token))
		{
			return;
		}
		String username = securityIdentity.getPrincipal().getName();
		if (username == null || username.isBlank() || memberProvisioningService.isKnown(username))
		{
			return;
		}
		memberProvisioningService.ensureMember(username, token);
	}
}
