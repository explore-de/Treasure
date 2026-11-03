package app.treasure.mcp.security;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import app.treasure.mcp.McpTestSupport;
import app.treasure.mcp.service.McpTokenService.IssuedToken;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@QuarkusTest
class McpTokenAuthenticationTest extends McpTestSupport
{
	private static final String INITIALIZE = """
		{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26",
		"capabilities":{},"clientInfo":{"name":"auth-test","version":"1"}}}""";

	@Test
	void shouldAnswerUnauthorizedWhenMcpRequestHasNoToken()
	{
		// given
		String body = INITIALIZE;

		// when
		Response response = postToMcp(null, body);

		// then
		response.then().statusCode(401);
	}

	@Test
	void shouldAnswerUnauthorizedWhenMcpTokenIsUnknown()
	{
		// given
		String unknownToken = "trs_" + unique("not-issued");

		// when
		Response response = postToMcp(unknownToken, INITIALIZE);

		// then
		response.then().statusCode(401);
	}

	@Test
	void shouldAnswerUnauthorizedWhenMcpTokenWasRevoked()
	{
		// given
		IssuedToken issued = memberWithToken("mcp-revoked");
		tokenService.revoke(issued.token().id, issued.token().getMember());

		// when
		Response response = postToMcp(issued.secret(), INITIALIZE);

		// then
		response.then().statusCode(401);
	}

	@Test
	void shouldInitializeSessionWhenMcpTokenIsValid()
	{
		// given
		IssuedToken issued = memberWithToken("mcp-valid");
		try
		{
			// when
			Response response = postToMcp(issued.secret(), INITIALIZE);

			// then
			response.then().statusCode(200);
		}
		finally
		{
			deleteToken(issued);
		}
	}

	@Test
	void shouldRedirectToLoginWhenWebPageRequestCarriesMcpToken()
	{
		// given
		IssuedToken issued = memberWithToken("mcp-web");
		try
		{
			// when
			Response response = given()
				.redirects().follow(false)
				.header("Authorization", "Bearer " + issued.secret())
				.when().get("/devices");

			// then
			response.then().statusCode(302);
		}
		finally
		{
			deleteToken(issued);
		}
	}

	private static Response postToMcp(String token, String body)
	{
		var request = given()
			.contentType(ContentType.JSON)
			.accept("application/json, text/event-stream")
			.body(body);
		if (token != null)
		{
			request.header("Authorization", "Bearer " + token);
		}
		return request.when().post("/mcp");
	}
}
