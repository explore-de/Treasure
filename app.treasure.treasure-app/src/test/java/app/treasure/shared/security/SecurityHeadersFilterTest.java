package app.treasure.shared.security;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

@QuarkusTest
class SecurityHeadersFilterTest
{
	@Test
	@TestSecurity(user = "maria")
	void shouldSendContentSecurityPolicyWhenPageIsRendered()
	{
		// given
		// a signed-in reader, see @TestSecurity

		// when
		Response response = given().when().get("/devices");

		// then
		response.then().header("Content-Security-Policy", SecurityHeadersFilter.CONTENT_SECURITY_POLICY);
	}

	@Test
	@TestSecurity(user = "maria")
	void shouldForbidContentTypeSniffingWhenPageIsRendered()
	{
		// given
		// a signed-in reader, see @TestSecurity

		// when
		Response response = given().when().get("/devices");

		// then
		response.then().header("X-Content-Type-Options", "nosniff");
	}

	@Test
	@TestSecurity(user = "maria")
	void shouldRenderNoInlineScriptWhenPageIsRendered()
	{
		// given
		// the policy allows scripts from files only, so an inline one would
		// silently not run

		// when
		String page = given().when().get("/devices").then().extract().body().asString();

		// then
		assertFalse(page.contains("<script>"));
	}
}
