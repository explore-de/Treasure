package app.treasure.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

class AdminGroupAugmentorTest
{
	private static final String ADMIN_GROUP = "11111111-aaaa-4bbb-8ccc-222222222222";
	private static final String OTHER_GROUP = "33333333-dddd-4eee-8fff-444444444444";

	@Test
	void shouldGrantOnlyAdminRoleWhenLoginIsInAdminGroup()
	{
		// given
		AdminGroupAugmentor augmentor = augmentorFor(Optional.of(ADMIN_GROUP));
		SecurityIdentity login = identity(oidcPrincipal(), Set.of(OTHER_GROUP, ADMIN_GROUP));

		// when
		SecurityIdentity result = augmentor.withAdminRole(login);

		// then
		assertEquals(Set.of(Roles.ADMIN), result.getRoles());
	}

	@Test
	void shouldGrantNoRoleWhenLoginIsNotInAdminGroup()
	{
		// given
		AdminGroupAugmentor augmentor = augmentorFor(Optional.of(ADMIN_GROUP));
		SecurityIdentity login = identity(oidcPrincipal(), Set.of(OTHER_GROUP));

		// when
		SecurityIdentity result = augmentor.withAdminRole(login);

		// then
		assertEquals(Set.of(), result.getRoles());
	}

	@Test
	void shouldKeepPrincipalWhenRolesAreReplaced()
	{
		// given
		AdminGroupAugmentor augmentor = augmentorFor(Optional.of(ADMIN_GROUP));
		JsonWebToken token = oidcPrincipal();
		SecurityIdentity login = identity(token, Set.of(ADMIN_GROUP));

		// when
		SecurityIdentity result = augmentor.withAdminRole(login);

		// then
		assertEquals(token, result.getPrincipal());
	}

	@Test
	void shouldKeepRolesWhenNoAdminGroupIsConfigured()
	{
		// given
		AdminGroupAugmentor augmentor = augmentorFor(Optional.empty());
		SecurityIdentity login = identity(oidcPrincipal(), Set.of(Roles.ADMIN));

		// when
		SecurityIdentity result = augmentor.withAdminRole(login);

		// then
		assertEquals(Set.of(Roles.ADMIN), result.getRoles());
	}

	@Test
	void shouldKeepRolesWhenLoginIsAnApiKeyAndNotAnOidcLogin()
	{
		// given
		AdminGroupAugmentor augmentor = augmentorFor(Optional.of(ADMIN_GROUP));
		SecurityIdentity apiKey = identity(new QuarkusPrincipal("key.owner"), Set.of(Roles.ADMIN));

		// when
		SecurityIdentity result = augmentor.withAdminRole(apiKey);

		// then
		assertEquals(Set.of(Roles.ADMIN), result.getRoles());
	}

	private static AdminGroupAugmentor augmentorFor(Optional<String> adminGroupId)
	{
		AdminGroupAugmentor augmentor = new AdminGroupAugmentor();
		augmentor.adminGroupId = adminGroupId;
		return augmentor;
	}

	private static JsonWebToken oidcPrincipal()
	{
		JsonWebToken token = mock(JsonWebToken.class);
		when(token.getName()).thenReturn("erika.mustermann@example.com");
		return token;
	}

	private static SecurityIdentity identity(Principal principal, Set<String> roles)
	{
		return QuarkusSecurityIdentity.builder().setPrincipal(principal).addRoles(roles).build();
	}
}
