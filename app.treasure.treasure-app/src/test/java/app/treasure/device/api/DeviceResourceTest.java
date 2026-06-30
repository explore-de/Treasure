package app.treasure.device.api;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class DeviceResourceTest
{
	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldOpenDeviceList()
	{
		given()
			.when().get("/devices")
			.then()
			.statusCode(200);
	}
}