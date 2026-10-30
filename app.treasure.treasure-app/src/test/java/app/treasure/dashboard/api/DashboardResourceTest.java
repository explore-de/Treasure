package app.treasure.dashboard.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class DashboardResourceTest
{
	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldShowOverview()
	{
		given()
			.when().get("/")
			.then()
			.statusCode(200)
			.body(containsString("Overview"))
			.body(containsString("Devices per group"));
	}

	@Test
	void shouldRequireLogin()
	{
		given()
			.redirects().follow(false)
			.when().get("/")
			.then()
			.statusCode(302);
	}
}
