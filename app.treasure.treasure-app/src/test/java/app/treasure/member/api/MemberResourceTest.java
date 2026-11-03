package app.treasure.member.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import app.treasure.shared.security.Roles;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@QuarkusTest
class MemberResourceTest
{
	@Inject
	MemberRepository memberRepository;

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

	@Test
	@TestSecurity(user = "maria", roles = { "user", Roles.ADMIN })
	void shouldAddMemberWhoNeverSignedInWhenAdminSubmitsForm()
	{
		// given
		String email = "added.by.admin@example.com";
		try
		{
			// when
			postCreate("Added", "ByAdmin", email);

			// then
			assertEquals("Added ByAdmin: Never signed in", describe(email));
		}
		finally
		{
			deleteMember(email);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = { "user", Roles.ADMIN })
	void shouldStoreLowercaseEmailAsUsernameWhenAdminAddsMember()
	{
		// given
		String email = "mixed.case@example.com";
		try
		{
			// when
			postCreate("Mixed", "Case", "  Mixed.Case@Example.com ");

			// then
			assertEquals(email, findByEmail(email).getUserName());
		}
		finally
		{
			deleteMember(email);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = { "user", Roles.ADMIN })
	void shouldRefuseNewMemberWhenEmailIsAlreadyUsed()
	{
		// given
		String email = "taken.address@example.com";
		postCreate("First", "Person", email);
		try
		{
			// when
			Response response = postCreate("Second", "Person", "Taken.Address@example.com");

			// then
			response.then().body(containsString("A member with the email " + email + " already exists."));
		}
		finally
		{
			deleteMember(email);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = { "user", Roles.ADMIN })
	void shouldRefuseNewMemberWhenEmailIsInvalid()
	{
		// given
		String email = "not-an-address";

		// when
		Response response = postCreate("No", "Address", email);

		// then
		response.then().body(containsString("Enter a valid email address."));
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldForbidAddingMemberWhenUserHasOnlyUserRole()
	{
		// given
		String email = "reader.attempt@example.com";
		try
		{
			// when
			Response response = postCreate("Reader", "Attempt", email);

			// then
			response.then().statusCode(403);
		}
		finally
		{
			deleteMember(email);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldHideAddMemberButtonWhenUserHasOnlyUserRole()
	{
		// given
		// a reader is logged in, see @TestSecurity

		// when
		Response response = given().when().get("/members");

		// then
		response.then().body(not(containsString("href=\"/members/new\"")));
	}

	private Response postCreate(String firstName, String lastName, String email)
	{
		String csrfToken = given()
			.when().get("/members")
			.then()
			.statusCode(200)
			.extract().cookie("csrf-token");
		return given()
			.redirects().follow(false)
			.cookie("csrf-token", csrfToken)
			.formParam("csrf-token", csrfToken)
			.contentType("application/x-www-form-urlencoded; charset=UTF-8")
			.formParams(Map.of("firstName", firstName, "lastName", lastName, "email", email))
			.when().post("/members/create");
	}

	@Transactional(TxType.REQUIRES_NEW)
	Member findByEmail(String email)
	{
		return memberRepository.findByEmail(email);
	}

	@Transactional(TxType.REQUIRES_NEW)
	String describe(String email)
	{
		Member member = memberRepository.findByEmail(email);
		return member.getDisplayName() + ": " + member.getStatusLabel();
	}

	@Transactional(TxType.REQUIRES_NEW)
	void deleteMember(String email)
	{
		memberRepository.delete("lower(email) = ?1", email.toLowerCase());
	}
}
