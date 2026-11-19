package app.treasure.shared.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;

import org.junit.jupiter.api.Test;

import app.treasure.shared.security.Roles;
import io.quarkiverse.renarde.util.I18N;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;

@QuarkusTest
class LanguageResourceTest
{
	@Test
	@TestSecurity(user = "maria")
	void shouldRenderGermanWhenBrowserPrefersGerman()
	{
		// given
		String acceptLanguage = "de-DE,de;q=0.9,en;q=0.8";

		// when
		Response response = given().header("Accept-Language", acceptLanguage).when().get("/devices");

		// then
		response.then().body(containsString("<h1>Geräte</h1>"));
	}

	@Test
	@TestSecurity(user = "maria")
	void shouldRenderEnglishWhenBrowserPrefersUnsupportedLanguage()
	{
		// given
		String acceptLanguage = "fr-FR,fr;q=0.9";

		// when
		Response response = given().header("Accept-Language", acceptLanguage).when().get("/devices");

		// then
		response.then().body(containsString("<h1>Devices</h1>"));
	}

	@Test
	@TestSecurity(user = "maria")
	void shouldRenderGermanWhenLocaleCookieIsGermanAndBrowserPrefersEnglish()
	{
		// given
		String acceptLanguage = "en-US,en;q=0.9";

		// when
		Response response = given()
			.header("Accept-Language", acceptLanguage)
			.cookie(I18N.LOCALE_COOKIE_NAME, "de")
			.when().get("/devices");

		// then
		response.then().body(containsString("<h1>Geräte</h1>"));
	}

	@Test
	@TestSecurity(user = "maria")
	void shouldMarkPageAsGermanWhenBrowserPrefersGerman()
	{
		// given
		String acceptLanguage = "de";

		// when
		Response response = given().header("Accept-Language", acceptLanguage).when().get("/members");

		// then
		response.then().body(containsString("<html lang=\"de\">"));
	}

	@Test
	@TestSecurity(user = "maria")
	void shouldSetLocaleCookieWhenLanguageIsSwitchedToGerman()
	{
		// given
		String language = "de";

		// when
		Response response = postLanguage(language, "/members");

		// then
		response.then().cookie(I18N.LOCALE_COOKIE_NAME, "de");
	}

	@Test
	@TestSecurity(user = "maria")
	void shouldReturnToThePageWhenLanguageIsSwitched()
	{
		// given
		String page = "/members?page=1";

		// when
		Response response = postLanguage("de", page);

		// then
		response.then().statusCode(303).header("Location", endsWith("/members?page=1"));
	}

	@Test
	@TestSecurity(user = "maria")
	void shouldRedirectToOverviewWhenRedirectTargetIsAnotherSite()
	{
		// given
		String target = "//evil.example/";

		// when
		Response response = postLanguage("en", target);

		// then
		response.then().statusCode(303).header("Location", matchesPattern("https?://[^/]+/"));
	}

	@Test
	@TestSecurity(user = "maria")
	void shouldRefuseWhenLanguageIsNotSupported()
	{
		// given
		String language = "fr";

		// when
		Response response = postLanguage(language, "/members");

		// then
		response.then().statusCode(400);
	}

	@Test
	@TestSecurity(user = "maria", roles = Roles.ADMIN)
	void shouldShowGermanValidationMessageWhenPageIsGermanAndEmailIsInvalid()
	{
		// given
		String csrfToken = csrfToken();

		// when
		Response response = given()
			.header("Accept-Language", "de")
			.cookie("csrf-token", csrfToken)
			.formParam("csrf-token", csrfToken)
			.formParam("firstName", "Erika")
			.formParam("lastName", "Mustermann")
			.formParam("email", "not-an-email")
			.when().post("/members/create");

		// then
		response.then().body(containsString("Geben Sie eine gültige E-Mail-Adresse ein."));
	}

	private Response postLanguage(String language, String redirectUrl)
	{
		String csrfToken = csrfToken();
		return given()
			.redirects().follow(false)
			.cookie("csrf-token", csrfToken)
			.formParam("csrf-token", csrfToken)
			.formParam("lang", language)
			.formParam("redirectUrl", redirectUrl)
			.when().post("/language");
	}

	private String csrfToken()
	{
		return given().when().get("/devices").then().statusCode(200).extract().cookie("csrf-token");
	}
}
