package app.treasure.member.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import app.treasure.shared.security.Roles;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class MemberResourceTest
{
	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldShowMembersPage()
	{
		given()
			.when().get("/members")
			.then()
			.statusCode(200)
			.body(containsString("Members"));
	}

	@Test
	@TestSecurity(user = "admin", roles = { Roles.SUPER_ADMIN, Roles.ADMIN })
	void shouldShowMembersPageForSuperAdmin()
	{
		given()
			.when().get("/members")
			.then()
			.statusCode(200)
			.body(containsString("Members"));
	}
}
