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

@QuarkusTest
class TokenResourceTest extends McpTestSupport
{
	private static final String PAGE_USER = "token-page-user";

	@Test
	@TestSecurity(user = PAGE_USER, roles = { "user", Roles.ADMIN })
	void shouldListOwnTokenByNameWhenTokenPageIsOpened()
	{
		// given
		String tokenName = unique("Claude on laptop");
		IssuedToken issued = tokenService.issue(createTestMember(PAGE_USER), tokenName, Set.of("user", Roles.ADMIN));
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
	@TestSecurity(user = PAGE_USER, roles = "user")
	void shouldForbidTokenPageWhenUserHasOnlyUserRole()
	{
		// given
		// a regular user is logged in, see @TestSecurity

		// when
		Response response = given().when().get("/tokens");

		// then
		response.then().statusCode(403);
	}

	@Test
	@TestSecurity(user = PAGE_USER, roles = "user")
	void shouldHideApiKeysLinkWhenUserHasOnlyUserRole()
	{
		// given
		// a regular user is logged in, see @TestSecurity

		// when
		Response response = given().when().get("/devices");

		// then
		response.then().body(not(containsString("href=\"/tokens\"")));
	}

	@Test
	@TestSecurity(user = PAGE_USER, roles = { "user", Roles.ADMIN })
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
