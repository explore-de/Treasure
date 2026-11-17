package app.treasure.shared.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import app.treasure.member.domain.Member;
import app.treasure.member.service.MemberProvisioningService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.persistence.PersistenceException;

class FirstLoginFilterTest
{
	@Test
	void shouldReadMemberAgainWhenParallelRequestCreatedItFirst()
	{
		// given
		JsonWebToken token = mock(JsonWebToken.class);
		when(token.getName()).thenReturn("first.login");
		SecurityIdentity identity = mock(SecurityIdentity.class);
		when(identity.getPrincipal()).thenReturn(token);
		MemberProvisioningService provisioning = mock(MemberProvisioningService.class);
		when(provisioning.ensureMember("first.login", token))
			.thenThrow(new PersistenceException("duplicate key value violates unique constraint"))
			.thenReturn(new Member());
		FirstLoginFilter filter = new FirstLoginFilter();
		filter.securityIdentity = identity;
		filter.memberProvisioningService = provisioning;

		// when
		filter.ensureMemberExists();

		// then
		verify(provisioning, times(2)).ensureMember("first.login", token);
	}
}
