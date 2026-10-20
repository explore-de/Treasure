package app.treasure.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class MemberProvisioningServiceTest
{
	@Inject
	MemberProvisioningService provisioningService;

	@Inject
	MemberRepository memberRepository;

	private JsonWebToken entraToken(String username)
	{
		JsonWebToken token = mock(JsonWebToken.class);
		when(token.getClaim("email")).thenReturn(username);
		when(token.getClaim("given_name")).thenReturn("Erika");
		when(token.getClaim("family_name")).thenReturn("Mustermann");
		when(token.getClaim("oid")).thenReturn("oid-" + username);
		return token;
	}

	@Test
	void shouldCreateMemberFromTokenOnFirstLogin()
	{
		String username = "erika.mustermann@example.com";

		Member member = provisioningService.ensureMember(username, entraToken(username));

		assertNotNull(member.getId());
		assertEquals("Erika", member.getFirstName());
		assertEquals("Mustermann", member.getLastName());
		assertEquals(username, member.getEmail());
		assertEquals("oid-" + username, member.getSubject());
		assertEquals(MemberProvisioningService.INVITE_TYPE_SSO, member.getInviteType());
		assertTrue(provisioningService.isKnown(username));
	}

	@Test
	void shouldNotDuplicateAnExistingMember()
	{
		String username = "second.login@example.com";
		Member first = provisioningService.ensureMember(username, entraToken(username));
		long countAfterFirst = memberRepository.count("userName", username);

		Member second = provisioningService.ensureMember(username, entraToken(username));

		assertEquals(1, countAfterFirst);
		assertEquals(first.getId(), second.getId());
	}

	@Test
	void shouldFallBackToDisplayNameAndUsernameWhenNameClaimsAreMissing()
	{
		String username = "plain.user@example.com";
		JsonWebToken token = mock(JsonWebToken.class);
		when(token.getClaim("name")).thenReturn("Plain User");
		when(token.getClaim("sub")).thenReturn("sub-123");

		Member member = provisioningService.ensureMember(username, token);

		assertEquals("Plain", member.getFirstName());
		assertEquals("User", member.getLastName());
		assertEquals(username, member.getEmail());
		assertEquals("sub-123", member.getSubject());
	}
}
