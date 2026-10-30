package app.treasure.member.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
