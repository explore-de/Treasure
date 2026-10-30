package app.treasure.shared.template;

import java.util.Set;

import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import app.treasure.shared.security.Roles;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Makes user data from SecurityIdentity available in all Qute templates via
 * inject:userContext
 */
@RequestScoped
@Named("userContext")
public class UserContext
{
	@Inject
	SecurityIdentity securityIdentity;

	@Inject
	MemberRepository memberRepository;

	public boolean isAuthenticated()
	{
		return securityIdentity != null && !securityIdentity.isAnonymous();
	}

	public String getUsername()
	{
		return isAuthenticated() ? securityIdentity.getPrincipal().getName() : null;
	}

	public String getEmail()
	{
		if (!isAuthenticated())
		{
			return null;
		}
		// Get email from Member record
		String username = securityIdentity.getPrincipal().getName();
		Member member = memberRepository.findByUsername(username);
		return member != null ? member.getEmail() : null;
	}

	public Set<String> getRoles()
	{
		return isAuthenticated() ? securityIdentity.getRoles() : Set.of();
	}

	public boolean hasRole(String role)
	{
		return isAuthenticated() && securityIdentity.hasRole(role);
	}

	public String getFirstLetter()
	{
		String username = getUsername();
		return username != null && !username.isEmpty() ? username.substring(0, 1).toUpperCase() : "?";
	}

	/**
	 * Checks if the current user is a super admin.
	 *
	 * @return true if the user has the super_admin role
	 */
	public boolean isSuperAdmin()
	{
		return hasRole(Roles.SUPER_ADMIN);
	}
}
