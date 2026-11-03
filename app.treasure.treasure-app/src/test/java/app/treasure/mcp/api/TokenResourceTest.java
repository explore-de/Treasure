package app.treasure.mcp.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.util.Set;

import org.junit.jupiter.api.Test;

import app.treasure.mcp.McpTestSupport;
import app.treasure.mcp.service.McpTokenService.IssuedToken;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

@QuarkusTest
class TokenResourceTest extends McpTestSupport
{
	private static final String PAGE_USER = "token-page-user";

	@Test
	@TestSecurity(user = PAGE_USER, roles = "user")
	void shouldListOwnTokenByNameWhenTokenPageIsOpened()
	{
		// given
		String tokenName = unique("Claude on laptop");
		IssuedToken issued = tokenService.issue(createTestMember(PAGE_USER), tokenName, Set.of("user"));
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
}
