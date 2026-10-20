package app.treasure.shared;

import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Base class for tests that need members in the database.
 */
public abstract class BaseMemberTest
{
	@Inject
	protected MemberRepository memberRepository;

	/**
	 * Creates a test member with the given username, or returns the existing
	 * one.
	 */
	@Transactional(Transactional.TxType.REQUIRES_NEW)
	protected Member createTestMember(String username)
	{
		Member member = memberRepository.findByUsername(username);
		if (member != null)
		{
			return member;
		}
		member = new Member();
		member.setUserName(username);
		member.setEmail(username + "@test.local");
		member.setFirstName("Test");
		member.setLastName("User");
		memberRepository.persist(member);
		return member;
	}
}
