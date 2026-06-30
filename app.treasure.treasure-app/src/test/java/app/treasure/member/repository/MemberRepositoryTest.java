package app.treasure.member.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.treasure.member.domain.Member;
import app.treasure.organization.domain.Organization;
import app.treasure.shared.BaseOrganizationTest;
import app.treasure.shared.TestSecurityHelper;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

@QuarkusTest
@TestSecurity(user = TestSecurityHelper.TEST_USER_MARIA, roles = "user")
class MemberRepositoryTest extends BaseOrganizationTest
{
	@Inject
	MemberRepository memberRepository;

	@BeforeEach
	void setupOrganizationContext()
	{
		Organization testOrg = getOrCreateTestOrganization();
		createTestMember(TestSecurityHelper.TEST_USER_MARIA, testOrg);
	}

	private Member persistMember(String firstName, String lastName, String userName, Organization org)
	{
		Member member = new Member();
		member.setFirstName(firstName);
		member.setLastName(lastName);
		member.setEmail(userName + "@test.local");
		member.setUserName(userName);
		member.setOrganization(org);
		memberRepository.persist(member);
		return member;
	}

	@TestTransaction
	@Test
	void shouldFindByUsername()
	{
		Member found = memberRepository.findByUsername(TestSecurityHelper.TEST_USER_MARIA);

		assertNotNull(found);
		assertEquals(TestSecurityHelper.TEST_USER_MARIA, found.getUserName());
	}

	@TestTransaction
	@Test
	void shouldReturnNullWhenUsernameNotFound()
	{
		assertNull(memberRepository.findByUsername("ghost"));
	}

	@TestTransaction
	@Test
	void shouldFindByEmail()
	{
		Organization org = getOrCreateTestOrganization();
		persistMember("Anna", "Adams", "anna", org);

		Member found = memberRepository.findByEmail("anna@test.local");

		assertNotNull(found);
		assertEquals("anna", found.getUserName());
	}

	@TestTransaction
	@Test
	void shouldFindByKeycloakUserId()
	{
		Organization org = getOrCreateTestOrganization();
		Member member = persistMember("Bob", "Baker", "bob", org);
		member.setKeycloakUserId("kc-123");

		Member found = memberRepository.findByKeycloakUserId("kc-123");

		assertNotNull(found);
		assertEquals("bob", found.getUserName());
	}

	@TestTransaction
	@Test
	void shouldFindAllMembersInCurrentOrganizationOrderedByName()
	{
		Organization org = getOrCreateTestOrganization();
		persistMember("Zoe", "Zimmer", "zoe", org);
		persistMember("Anna", "Adams", "anna", org);

		List<Member> members = memberRepository.findAllOrderedByName();

		int annaIndex = -1;
		int zoeIndex = -1;
		for (int i = 0; i < members.size(); i++)
		{
			if ("anna".equals(members.get(i).getUserName()))
			{
				annaIndex = i;
			}
			if ("zoe".equals(members.get(i).getUserName()))
			{
				zoeIndex = i;
			}
		}

		assertTrue(annaIndex >= 0);
		assertTrue(zoeIndex >= 0);
		assertTrue(annaIndex < zoeIndex);
	}

	@TestTransaction
	@Test
	void shouldSearchMembersByName()
	{
		Organization org = getOrCreateTestOrganization();
		persistMember("Alexander", "Schmidt", "alex", org);

		List<Member> results = memberRepository.searchByName("alex");

		assertTrue(results.stream().anyMatch(m -> "alex".equals(m.getUserName())));
	}

	@TestTransaction
	@Test
	void shouldFindByIdScopedWithinCurrentOrganization()
	{
		Organization org = getOrCreateTestOrganization();
		Member member = persistMember("Clara", "Cook", "clara", org);

		Member found = memberRepository.findByIdScoped(member.getId());

		assertNotNull(found);
		assertEquals("clara", found.getUserName());
	}

	@TestTransaction
	@Test
	void shouldReturnNullWhenMemberBelongsToAnotherOrganization()
	{
		Organization otherOrg = new Organization();
		otherOrg.setName("Other Org");
		otherOrg.setSlug("other-org");
		organizationRepository.persist(otherOrg);

		Member external = persistMember("Extern", "Person", "extern", otherOrg);

		Member found = memberRepository.findByIdScoped(external.getId());

		assertNull(found);
	}
}
