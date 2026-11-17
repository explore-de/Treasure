package app.treasure.shared.security;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Turns membership of one Entra security group into the admin role. On a stand
 * the ID token carries the object ids of the person's groups, which arrive here
 * as the roles of the login. Someone in the group named by
 * {@code treasure.admin-group-id} gets {@link Roles#ADMIN}; the group ids
 * themselves are dropped, so they neither show up in the account panel nor
 * count as roles.
 * <p>
 * Without that setting (dev and test, where Keycloak sends the role itself) and
 * for logins that are not OIDC logins (API keys on /mcp, test identities)
 * nothing is changed.
 */
@ApplicationScoped
public class AdminGroupAugmentor implements SecurityIdentityAugmentor
{
	@ConfigProperty(name = "treasure.admin-group-id")
	Optional<String> adminGroupId;

	@Override
	public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context)
	{
		return Uni.createFrom().item(withAdminRole(identity));
	}

	SecurityIdentity withAdminRole(SecurityIdentity identity)
	{
		String groupId = adminGroupId.map(String::trim).orElse("");
		if (groupId.isEmpty() || identity.isAnonymous() || !(identity.getPrincipal() instanceof JsonWebToken))
		{
			return identity;
		}
		QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder()
			.setPrincipal(identity.getPrincipal())
			.addAttributes(identity.getAttributes())
			.addCredentials(identity.getCredentials());
		if (identity.getRoles().contains(groupId))
		{
			builder.addRole(Roles.ADMIN);
		}
		return builder.build();
	}
}
