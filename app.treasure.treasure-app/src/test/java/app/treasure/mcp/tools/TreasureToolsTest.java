package app.treasure.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.treasure.device.domain.Device;
import app.treasure.device.domain.DeviceHistory;
import app.treasure.mcp.McpTestSupport;
import app.treasure.mcp.service.McpTokenService.IssuedToken;
import io.quarkiverse.mcp.server.JsonRpcErrorCodes;
import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkiverse.mcp.server.test.McpAssured.McpStreamableTestClient;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@QuarkusTest
class TreasureToolsTest extends McpTestSupport
{
	@TestHTTPResource
	URI testUri;

	@BeforeEach
	void pointMcpClientAtTestServer()
	{
		McpAssured.baseUri = testUri;
	}

	@Test
	void shouldRecordAssignmentUnderTokenOwnerWhenAssignDeviceIsCalled()
	{
		// given
		IssuedToken issued = adminWithToken("tools-actor");
		String holder = createTestMember(unique("tools-holder")).getUserName();
		Long deviceId = createFreeDevice(unique("ToolsLaptop"), unique("SN"));
		McpStreamableTestClient client = connect(issued);
		try
		{
			// when
			client.when()
				.toolsCall("assign_device", Map.of("deviceId", deviceId, "username", holder), r -> {
				})
				.thenAssertResults();

			// then
			DeviceHistory entry = latestHistory(deviceId);
			assertEquals("ASSIGNED by " + issued.token().getMember().getUserName(),
				entry.getEventType() + " by " + entry.getActor().getUserName());
		}
		finally
		{
			client.disconnect();
			deleteDevice(deviceId);
			deleteToken(issued);
		}
	}

	@Test
	void shouldMakeDeviceAvailableWhenUnassignDeviceIsCalled()
	{
		// given
		IssuedToken issued = adminWithToken("tools-unassign");
		Long deviceId = createDeviceHeldBy(unique("ToolsPhone"), issued.token().getMember().getUserName());
		McpStreamableTestClient client = connect(issued);
		try
		{
			// when
			client.when()
				.toolsCall("unassign_device", Map.of("deviceId", deviceId), r -> {
				})
				.thenAssertResults();

			// then
			assertEquals("available", findDevice(deviceId).getStatus());
		}
		finally
		{
			client.disconnect();
			deleteDevice(deviceId);
			deleteToken(issued);
		}
	}

	@Test
	void shouldReturnToolErrorWhenAssignDeviceGetsUnknownMember()
	{
		// given
		IssuedToken issued = adminWithToken("tools-unknown");
		Long deviceId = createFreeDevice(unique("ToolsTablet"), unique("SN"));
		String unknownUser = unique("nobody");
		McpStreamableTestClient client = connect(issued);
		try
		{
			// when / then
			client.when()
				.toolsCall("assign_device", Map.of("deviceId", deviceId, "username", unknownUser),
					r -> assertEquals(
						"true: No member with username '" + unknownUser
							+ "'. Use list_members to see the usernames.",
						r.isError() + ": " + r.firstContent().asText().text()))
				.thenAssertResults();
		}
		finally
		{
			client.disconnect();
			deleteDevice(deviceId);
			deleteToken(issued);
		}
	}

	@Test
	void shouldFindDeviceWhenSearchMatchesSerialNumber()
	{
		// given
		IssuedToken issued = memberWithToken("tools-search");
		String serial = unique("SERIAL");
		Long deviceId = createFreeDevice(unique("ToolsMonitor"), serial);
		McpStreamableTestClient client = connect(issued);
		try
		{
			// when / then
			client.when()
				.toolsCall("list_devices", Map.of("search", serial.toLowerCase()),
					r -> assertEquals(List.of(deviceId.intValue()),
						new JsonObject(r.firstContent().asText().text()).getJsonArray("devices").stream()
							.map(d -> ((JsonObject)d).getInteger("id")).toList()))
				.thenAssertResults();
		}
		finally
		{
			client.disconnect();
			deleteDevice(deviceId);
			deleteToken(issued);
		}
	}

	@Test
	void shouldRecordCreatedHistoryUnderTokenOwnerWhenCreateDeviceIsCalled()
	{
		// given
		IssuedToken issued = adminWithToken("tools-create");
		String name = unique("ToolsNewLaptop");
		McpStreamableTestClient client = connect(issued);
		Long[] created = new Long[1];
		try
		{
			// when
			client.when()
				.toolsCall("create_device", Map.of("name", name, "group", "Laptops"),
					r -> created[0] = deviceIdOf(r.firstContent().asText().text()))
				.thenAssertResults();

			// then
			DeviceHistory entry = latestHistory(created[0]);
			assertEquals("CREATED by " + issued.token().getMember().getUserName(),
				entry.getEventType() + " by " + entry.getActor().getUserName());
		}
		finally
		{
			client.disconnect();
			if (created[0] != null)
			{
				deleteDevice(created[0]);
			}
			deleteToken(issued);
		}
	}

	@Test
	void shouldKeepFieldsLeftOutWhenUpdateDeviceChangesOnlyTheModel()
	{
		// given
		IssuedToken issued = adminWithToken("tools-update");
		String serial = unique("SN");
		Long deviceId = createFreeDevice(unique("ToolsDock"), serial);
		McpStreamableTestClient client = connect(issued);
		try
		{
			// when
			client.when()
				.toolsCall("update_device", Map.of("deviceId", deviceId, "model", "TB4"), r -> {
				})
				.thenAssertResults();

			// then
			Device device = findDevice(deviceId);
			assertEquals("TB4 " + serial, device.getDeviceModel() + " " + device.getDeviceSerialNumber());
		}
		finally
		{
			client.disconnect();
			deleteDevice(deviceId);
			deleteToken(issued);
		}
	}

	@Test
	void shouldReturnToolErrorWhenUpdateDeviceGetsUnknownCondition()
	{
		// given
		IssuedToken issued = adminWithToken("tools-condition");
		Long deviceId = createFreeDevice(unique("ToolsMouse"), unique("SN"));
		McpStreamableTestClient client = connect(issued);
		try
		{
			// when / then
			client.when()
				.toolsCall("update_device", Map.of("deviceId", deviceId, "condition", "like new"),
					r -> assertEquals(
						"true: Condition must be one of: perfect, lightly damaged, heavily damaged, broken.",
						r.isError() + ": " + r.firstContent().asText().text()))
				.thenAssertResults();
		}
		finally
		{
			client.disconnect();
			deleteDevice(deviceId);
			deleteToken(issued);
		}
	}

	@Test
	void shouldReturnSecurityErrorWhenTokenWithoutAdminRoleCallsUnassignDevice()
	{
		// given
		IssuedToken issued = memberWithToken("tools-reader");
		String holder = createTestMember(unique("tools-reader-holder")).getUserName();
		Long deviceId = createDeviceHeldBy(unique("ToolsReaderLaptop"), holder);
		McpStreamableTestClient client = connect(issued);
		try
		{
			// when / then
			client.when()
				.toolsCall("unassign_device")
				.withArguments(Map.of("deviceId", deviceId))
				.withErrorAssert(error -> assertEquals(JsonRpcErrorCodes.SECURITY_ERROR, error.code()))
				.send()
				.thenAssertResults();
		}
		finally
		{
			client.disconnect();
			deleteDevice(deviceId);
			deleteToken(issued);
		}
	}

	@Test
	void shouldAddMemberWhoNeverSignedInWhenAddMemberIsCalled()
	{
		// given
		IssuedToken issued = adminWithToken("tools-add");
		String email = unique("tools-added") + "@example.com";
		McpStreamableTestClient client = connect(issued);
		try
		{
			// when / then
			client.when()
				.toolsCall("add_member", Map.of("firstName", "Tool", "lastName", "Added", "email", email),
					r -> assertEquals(email + ": Never signed in",
						new JsonObject(r.firstContent().asText().text()).getString("username") + ": "
							+ new JsonObject(r.firstContent().asText().text()).getString("status")))
				.thenAssertResults();
		}
		finally
		{
			client.disconnect();
			deleteMemberByEmail(email);
			deleteToken(issued);
		}
	}

	@Test
	void shouldShowStatusWhenListMembersIsCalled()
	{
		// given
		IssuedToken issued = memberWithToken("tools-status");
		String username = issued.token().getMember().getUserName();
		McpStreamableTestClient client = connect(issued);
		try
		{
			// when / then
			client.when()
				.toolsCall("list_members", Map.of(),
					r -> assertEquals(List.of("Never signed in"),
						new JsonObject(r.firstContent().asText().text()).getJsonArray("members").stream()
							.map(m -> (JsonObject)m)
							.filter(m -> username.equals(m.getString("username")))
							.map(m -> m.getString("status")).toList()))
				.thenAssertResults();
		}
		finally
		{
			client.disconnect();
			deleteToken(issued);
		}
	}

	@Test
	void shouldReturnToolErrorWhenAddMemberGetsTakenEmail()
	{
		// given
		IssuedToken issued = adminWithToken("tools-taken");
		String takenEmail = issued.token().getMember().getEmail();
		McpStreamableTestClient client = connect(issued);
		try
		{
			// when / then
			client.when()
				.toolsCall("add_member", Map.of("firstName", "Again", "lastName", "Person", "email", takenEmail),
					r -> assertEquals("true: A member with the email " + takenEmail + " already exists.",
						r.isError() + ": " + r.firstContent().asText().text()))
				.thenAssertResults();
		}
		finally
		{
			client.disconnect();
			deleteToken(issued);
		}
	}

	@Test
	void shouldReturnSecurityErrorWhenTokenWithoutAdminRoleCallsAddMember()
	{
		// given
		IssuedToken issued = memberWithToken("tools-add-reader");
		String email = unique("tools-not-added") + "@example.com";
		McpStreamableTestClient client = connect(issued);
		try
		{
			// when / then
			client.when()
				.toolsCall("add_member")
				.withArguments(Map.of("firstName", "Not", "lastName", "Added", "email", email))
				.withErrorAssert(error -> assertEquals(JsonRpcErrorCodes.SECURITY_ERROR, error.code()))
				.send()
				.thenAssertResults();
		}
		finally
		{
			client.disconnect();
			deleteMemberByEmail(email);
			deleteToken(issued);
		}
	}

	@Transactional(TxType.REQUIRES_NEW)
	void deleteMemberByEmail(String email)
	{
		memberRepository.delete("lower(email) = ?1", email.toLowerCase());
	}

	private static Long deviceIdOf(String deviceDetailsJson)
	{
		return new JsonObject(deviceDetailsJson).getJsonObject("summary").getLong("id");
	}

	private static McpStreamableTestClient connect(IssuedToken issued)
	{
		return McpAssured.newStreamableClient().setBearerToken(issued.secret()).build().connect();
	}

	@Transactional(TxType.REQUIRES_NEW)
	Long createDeviceHeldBy(String name, String username)
	{
		Device device = new Device();
		device.setDeviceName(name);
		device.setStatus("not available");
		device.setPickupTime(LocalDateTime.now());
		device.setAssignedTo(memberRepository.findByUsername(username));
		deviceRepository.persist(device);
		return device.id;
	}

	@Transactional(TxType.REQUIRES_NEW)
	DeviceHistory latestHistory(Long deviceId)
	{
		DeviceHistory entry = deviceHistoryRepository.forDevice(deviceId).get(0);
		entry.getActor().getUserName();
		return entry;
	}
}
