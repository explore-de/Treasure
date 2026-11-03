package app.treasure.mcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import app.treasure.mcp.McpTestSupport;
import app.treasure.mcp.domain.McpToken;
import app.treasure.mcp.service.McpTokenService.IssuedToken;
import app.treasure.member.domain.Member;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@QuarkusTest
class McpTokenServiceTest extends McpTestSupport
{
	@Test
	void shouldStoreSha256OfSecretWhenTokenIsIssued()
	{
		// given
		IssuedToken issued = memberWithToken("svc-hash");
		try
		{
			// when
			McpToken stored = findToken(issued.token().id);

			// then
			assertEquals(McpTokenService.hash(issued.secret()), stored.getTokenHash());
		}
		finally
		{
			deleteToken(issued);
		}
	}

	@Test
	void shouldReturnNullWhenSecretLacksTreasurePrefix()
	{
		// given
		String foreignSecret = unique("ghp_github-style");

		// when
		McpToken token = tokenService.authenticate(foreignSecret);

		// then
		assertNull(token);
	}

	@Test
	void shouldRefuseRevokeWhenTokenBelongsToAnotherMember()
	{
		// given
		IssuedToken issued = memberWithToken("svc-owner");
		Member stranger = createTestMember(unique("svc-stranger"));
		try
		{
			// when
			boolean revoked = tokenService.revoke(issued.token().id, stranger);

			// then
			assertFalse(revoked);
		}
		finally
		{
			deleteToken(issued);
		}
	}

	@Transactional(TxType.REQUIRES_NEW)
	McpToken findToken(Long id)
	{
		return tokenRepository.findById(id);
	}
}
