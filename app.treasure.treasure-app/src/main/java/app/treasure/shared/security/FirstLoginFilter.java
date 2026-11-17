package app.treasure.shared.security;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.member.service.MemberProvisioningService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.Priorities;

/**
 * Runs on every request that reached a controller. If the signed-in user has no
 * Member row yet, one is created from the ID token. Only real OIDC logins carry
 * a token; test identities and anonymous requests are skipped.
 */
@ApplicationScoped
public class FirstLoginFilter
{
	private static final Logger LOG = LoggerFactory.getLogger(FirstLoginFilter.class);

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
		try
		{
			memberProvisioningService.ensureMember(username, token);
		}
		catch (PersistenceException e)
		{
			// a first page load sends several requests at once; another one
			// created the member a moment earlier, so this time it is found
			LOG.debug("Member {} was created by a parallel request, reading it again", username, e);
			memberProvisioningService.ensureMember(username, token);
		}
	}
}
