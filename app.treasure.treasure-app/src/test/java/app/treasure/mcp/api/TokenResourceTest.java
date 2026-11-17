package app.treasure.mcp.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.Set;

import org.junit.jupiter.api.Test;

import app.treasure.mcp.McpTestSupport;
import app.treasure.mcp.service.McpTokenService.IssuedToken;
import app.treasure.shared.security.Roles;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@QuarkusTest
class TokenResourceTest extends McpTestSupport
{
	private static final String PAGE_USER = "token-page-user";

	@Test
	@TestSecurity(user = PAGE_USER, roles = Roles.ADMIN)
	void shouldListOwnTokenByNameWhenTokenPageIsOpened()
	{
		// given
		String tokenName = unique("Claude on laptop");
		IssuedToken issued = tokenService.issue(createTestMember(PAGE_USER), tokenName, Set.of(Roles.ADMIN));
		try
		{
			// when
			Response response = given().when().get("/tokens");

			// then
			response.then().statusCode(200).body(containsString(tokenName));
		}
		finally
		{
			deleteToken(issued);
		}
	}

	@Test
	@TestSecurity(user = "token-creator", roles = Roles.ADMIN)
	void shouldForbidStoringThePageWhenNewKeyIsShown()
	{
		// given
		Long memberId = createTestMember("token-creator").getId();
		String csrfToken = given().when().get("/tokens").then().statusCode(200).extract().cookie("csrf-token");
		try
		{
			// when
			Response response = given()
				.cookie("csrf-token", csrfToken)
				.formParam("csrf-token", csrfToken)
				.formParam("name", unique("Key shown once"))
				.contentType("application/x-www-form-urlencoded; charset=UTF-8")
				.when().post("/tokens");

			// then
			response.then().header("Cache-Control", "no-store");
		}
		finally
		{
			deleteTokensOf(memberId);
		}
	}

	@Transactional(TxType.REQUIRES_NEW)
	void deleteTokensOf(Long memberId)
	{
		tokenRepository.delete("member.id", memberId);
	}

	@Test
	@TestSecurity(user = PAGE_USER)
	void shouldForbidTokenPageWhenUserIsNotAdmin()
	{
		// given
		// a regular user is logged in, see @TestSecurity

		// when
		Response response = given().when().get("/tokens");

		// then
		response.then().statusCode(403);
	}

	@Test
	@TestSecurity(user = PAGE_USER)
	void shouldHideApiKeysLinkWhenUserIsNotAdmin()
	{
		// given
		// a regular user is logged in, see @TestSecurity

		// when
		Response response = given().when().get("/devices");

		// then
		response.then().body(not(containsString("href=\"/tokens\"")));
	}

	@Test
	@TestSecurity(user = PAGE_USER, roles = Roles.ADMIN)
	void shouldShowApiKeysLinkWhenUserIsAdmin()
	{
		// given
		// an admin is logged in, see @TestSecurity

		// when
		Response response = given().when().get("/devices");

		// then
		response.then().body(containsString("href=\"/tokens\""));
	}
}
