package app.treasure.member.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.treasure.device.domain.Device;
import app.treasure.device.repository.DeviceRepository;
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

	@Inject
	DeviceRepository deviceRepository;

	@Test
	@TestSecurity(user = "maria")
	void shouldShowMembersPage()
	{
		given()
			.when().get("/members")
			.then()
			.statusCode(200)
			.body(containsString("Members"));
	}

	@Test
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
	void shouldShowAddMemberButtonWhenUserIsAdmin()
	{
		// given
		// an admin is logged in, see @TestSecurity

		// when
		Response response = given().when().get("/members");

		// then
		response.then().body(containsString("href=\"/members/new\""));
	}

	@Test
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
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
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
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
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
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
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
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
	@TestSecurity(user = "maria")
	void shouldForbidAddingMemberWhenUserIsNotAdmin()
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
	@TestSecurity(user = "maria")
	void shouldHideAddMemberButtonWhenUserIsNotAdmin()
	{
		// given
		// a reader is logged in, see @TestSecurity

		// when
		Response response = given().when().get("/members");

		// then
		response.then().body(not(containsString("href=\"/members/new\"")));
	}

	@Test
	@TestSecurity(user = "self.editor")
	void shouldChangeOwnNameWhenUserEditsThemselves()
	{
		// given
		Long id = createMember("self.editor", "self.editor@example.com", true);
		try
		{
			// when
			postUpdate(id, "Changed", "Self", "self.editor@example.com", "");

			// then
			assertEquals("Changed Self", findById(id).getDisplayName());
		}
		finally
		{
			deleteMember("self.editor@example.com");
		}
	}

	@Test
	@TestSecurity(user = "nosy.reader")
	void shouldForbidChangingAnotherMemberWhenUserIsNotAdmin()
	{
		// given
		Long id = createMember("someone.else", "someone.else@example.com", true);
		try
		{
			// when
			Response response = postUpdate(id, "Hacked", "Name", "someone.else@example.com", "");

			// then
			response.then().statusCode(403);
		}
		finally
		{
			deleteMember("someone.else@example.com");
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
	void shouldChangeAnotherMemberWhenAdminEdits()
	{
		// given
		Long id = createMember("edited.by.admin", "edited.by.admin@example.com", true);
		try
		{
			// when
			postUpdate(id, "Edited", "ByAdmin", "edited.by.admin@example.com", "+49 89 1");

			// then
			assertEquals("Edited ByAdmin", findById(id).getDisplayName());
		}
		finally
		{
			deleteMember("edited.by.admin@example.com");
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
	void shouldRefuseEmailOfAnotherMemberWhenEditing()
	{
		// given
		Long id = createMember("email.owner.one", "email.owner.one@example.com", true);
		createMember("email.owner.two", "email.owner.two@example.com", true);
		try
		{
			// when
			Response response = postUpdate(id, "One", "Owner", "email.owner.two@example.com", "");

			// then
			response.then().body(containsString("A member with the email email.owner.two@example.com already exists."));
		}
		finally
		{
			deleteMember("email.owner.one@example.com");
			deleteMember("email.owner.two@example.com");
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
	void shouldMoveUsernameWithEmailWhenMemberNeverSignedIn()
	{
		// given
		Long id = createMember("old.address@example.com", "old.address@example.com", false);
		try
		{
			// when
			postUpdate(id, "Moved", "Address", "new.address@example.com", "");

			// then
			assertEquals("new.address@example.com", findById(id).getUserName());
		}
		finally
		{
			deleteMember("new.address@example.com");
			deleteMember("old.address@example.com");
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
	void shouldKeepUsernameWhenSignedInMemberGetsNewEmail()
	{
		// given
		Long id = createMember("kept.login", "kept.login@example.com", true);
		try
		{
			// when
			postUpdate(id, "Kept", "Login", "kept.login.new@example.com", "");

			// then
			assertEquals("kept.login", findById(id).getUserName());
		}
		finally
		{
			deleteMember("kept.login.new@example.com");
			deleteMember("kept.login@example.com");
		}
	}

	@Test
	@TestSecurity(user = "profile.owner")
	void shouldRedirectToOwnEditPageWhenProfileLinkIsOpened()
	{
		// given
		Long id = createMember("profile.owner", "profile.owner@example.com", true);
		try
		{
			// when
			Response response = given().redirects().follow(false).when().get("/members/me");

			// then
			response.then().header("Location", endsWith("/members/" + id + "/edit"));
		}
		finally
		{
			deleteMember("profile.owner@example.com");
		}
	}

	private Response postUpdate(Long id, String firstName, String lastName, String email, String phone)
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
			.formParams(Map.of("firstName", firstName, "lastName", lastName, "email", email, "phone", phone))
			.when().post("/members/" + id + "/update");
	}

	@Transactional(TxType.REQUIRES_NEW)
	Long createMember(String userName, String email, boolean signedIn)
	{
		Member member = new Member();
		member.setFirstName("Test");
		member.setLastName("Member");
		member.setUserName(userName);
		member.setEmail(email);
		member.setJoinedAt(signedIn ? Instant.now() : null);
		memberRepository.persist(member);
		return member.id;
	}

	@Transactional(TxType.REQUIRES_NEW)
	Member findById(Long id)
	{
		return memberRepository.findById(id);
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

	@Test
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
	void shouldDeactivateMemberWhenAdminSubmitsDeactivation()
	{
		// given
		Long id = createMember("leaver", "leaver@example.com", true);
		try
		{
			// when
			postRowAction("/members/" + id + "/deactivate");

			// then
			assertEquals("Deactivated", findById(id).getStatusLabel());
		}
		finally
		{
			deleteMember("leaver@example.com");
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
	void shouldReactivateMemberWhenAdminSubmitsActivation()
	{
		// given
		Long id = createMember("returner", "returner@example.com", true);
		setActive(id, false);
		try
		{
			// when
			postRowAction("/members/" + id + "/activate");

			// then
			assertEquals("In the system", findById(id).getStatusLabel());
		}
		finally
		{
			deleteMember("returner@example.com");
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
	void shouldKeepMemberActiveWhenTheyStillHoldADevice()
	{
		// given
		Long id = createMember("holder.leaving", "holder.leaving@example.com", true);
		Long device = createDeviceHeldBy("HeldByLeaver", id);
		try
		{
			// when
			postRowAction("/members/" + id + "/deactivate");

			// then
			assertEquals("In the system", findById(id).getStatusLabel());
		}
		finally
		{
			deleteDevice(device);
			deleteMember("holder.leaving@example.com");
		}
	}

	@Test
	@TestSecurity(user = "self.deactivator", roles = Roles.ADMIN)
	void shouldKeepAdminActiveWhenTheyDeactivateThemselves()
	{
		// given
		Long id = createMember("self.deactivator", "self.deactivator@example.com", true);
		try
		{
			// when
			postRowAction("/members/" + id + "/deactivate");

			// then
			assertEquals("In the system", findById(id).getStatusLabel());
		}
		finally
		{
			deleteMember("self.deactivator@example.com");
		}
	}

	@Test
	@TestSecurity(user = "maria")
	void shouldForbidDeactivationWhenUserIsNotAdmin()
	{
		// given
		Long id = createMember("protected.member", "protected.member@example.com", true);
		try
		{
			// when
			Response response = postRowAction("/members/" + id + "/deactivate");

			// then
			response.then().statusCode(403);
		}
		finally
		{
			deleteMember("protected.member@example.com");
		}
	}

	private Response postRowAction(String path)
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
			.when().post(path);
	}

	@Transactional(TxType.REQUIRES_NEW)
	void setActive(Long id, boolean active)
	{
		memberRepository.findById(id).setActive(active);
	}

	@Transactional(TxType.REQUIRES_NEW)
	Long createDeviceHeldBy(String name, Long memberId)
	{
		Device device = new Device();
		device.setDeviceName(name);
		device.setAssignedTo(memberRepository.findById(memberId));
		deviceRepository.persist(device);
		return device.id;
	}

	@Transactional(TxType.REQUIRES_NEW)
	void deleteDevice(Long id)
	{
		deviceRepository.deleteById(id);
	}
}
