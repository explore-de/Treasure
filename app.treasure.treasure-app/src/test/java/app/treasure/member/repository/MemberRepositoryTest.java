package app.treasure.member.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.treasure.member.domain.Member;
import app.treasure.shared.BaseMemberTest;
import app.treasure.shared.TestSecurityHelper;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class MemberRepositoryTest extends BaseMemberTest
{
	@BeforeEach
	void setupMembers()
	{
		createTestMember(TestSecurityHelper.TEST_USER_MARIA);
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
	void shouldOrderByLastNameThenFirstNameWhenListingByName()
	{
		// given
		String family = "Zz-order-" + UUID.randomUUID();
		Member berta = persistMember("Berta", family);
		Member anton = persistMember("Anton", family);
		Member young = persistMember("Anton", family + "-younger");

		// when
		List<Long> ordered = memberRepository.listByName().stream()
			.filter(m -> m.getLastName().startsWith(family))
			.map(Member::getId)
			.toList();

		// then
		assertEquals(List.of(anton.getId(), berta.getId(), young.getId()), ordered);
	}

	private Member persistMember(String firstName, String lastName)
	{
		Member member = new Member();
		member.setFirstName(firstName);
		member.setLastName(lastName);
		member.setUserName(firstName.toLowerCase() + "." + lastName.toLowerCase());
		member.setEmail(member.getUserName() + "@test.local");
		memberRepository.persist(member);
		return member;
	}

	@TestTransaction
	@Test
	void shouldLeaveOutDeactivatedMemberWhenListingActiveMembers()
	{
		// given
		Member leaver = persistMember("Leaver", "Zz-active-" + UUID.randomUUID());
		leaver.setActive(false);

		// when
		List<Long> active = memberRepository.listActiveByName().stream().map(Member::getId).toList();

		// then
		assertFalse(active.contains(leaver.getId()));
	}

	@TestTransaction
	@Test
	void shouldListDeactivatedMemberAfterActiveOnesWhenListingForTheMembersPage()
	{
		// given
		String family = "Aa-first-" + UUID.randomUUID();
		Member leaver = persistMember("Anton", family);
		leaver.setActive(false);
		Member colleague = persistMember("Berta", family);

		// when
		List<Long> ordered = memberRepository.findAllForList().stream()
			.filter(m -> m.getLastName().equals(family))
			.map(Member::getId)
			.toList();

		// then
		assertEquals(List.of(colleague.getId(), leaver.getId()), ordered);
	}
}
