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
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

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

	@Test
	void shouldDeriveNamesFromUsernameWhenTokenHasNoNameClaims()
	{
		// given
		String username = "lena.meier@example.com";
		JsonWebToken token = mock(JsonWebToken.class);
		try
		{
			// when
			Member member = provisioningService.ensureMember(username, token);

			// then
			assertEquals("Lena Meier", member.getDisplayName());
		}
		finally
		{
			deleteMember(username);
		}
	}

	@Test
	void shouldTakeLastNameFromUsernameWhenDisplayNameIsOneWord()
	{
		// given
		String username = "pia.lang@example.com";
		JsonWebToken token = mock(JsonWebToken.class);
		when(token.getClaim("name")).thenReturn("Pia");
		try
		{
			// when
			Member member = provisioningService.ensureMember(username, token);

			// then
			assertEquals("Pia Lang", member.getDisplayName());
		}
		finally
		{
			deleteMember(username);
		}
	}

	@Test
	void shouldCreateMemberWhenOnlyASingleWordNameIsKnown()
	{
		// given
		String username = "mononym@example.com";
		JsonWebToken token = mock(JsonWebToken.class);
		when(token.getClaim("name")).thenReturn("Mononym");
		try
		{
			// when
			Member member = provisioningService.ensureMember(username, token);

			// then
			assertEquals("Mononym Mononym", member.getDisplayName());
		}
		finally
		{
			deleteMember(username);
		}
	}

	@Test
	void shouldLinkLoginToAddedMemberWhenEmailMatches()
	{
		// given
		String email = "linked.login@example.com";
		String username = "Linked.Login@example.com";
		Long addedId = addMemberWhoNeverSignedIn(email);
		try
		{
			// when
			Member member = provisioningService.ensureMember(username, entraToken(email));

			// then
			assertEquals(addedId, member.getId());
		}
		finally
		{
			deleteMember(username);
			deleteMember(email);
		}
	}

	@Test
	void shouldMarkAddedMemberAsSignedInWhenTheyLogInForTheFirstTime()
	{
		// given
		String email = "first.sign-in@example.com";
		addMemberWhoNeverSignedIn(email);
		try
		{
			// when
			Member member = provisioningService.ensureMember(email, entraToken(email));

			// then
			assertEquals("In the system", member.getStatusLabel());
		}
		finally
		{
			deleteMember(email);
		}
	}

	@Transactional(TxType.REQUIRES_NEW)
	Long addMemberWhoNeverSignedIn(String email)
	{
		Member member = new Member();
		member.setFirstName("Added");
		member.setLastName("Person");
		member.setEmail(email);
		member.setUserName(email);
		member.setInviteType(Member.INVITE_TYPE_INVITED);
		memberRepository.persist(member);
		return member.id;
	}

	@Transactional(TxType.REQUIRES_NEW)
	void deleteMember(String username)
	{
		memberRepository.delete("userName", username);
	}
}
