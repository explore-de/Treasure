package app.treasure.device.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.treasure.device.domain.Device;
import app.treasure.device.domain.DeviceHistory;
import app.treasure.device.repository.DeviceHistoryRepository;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import app.treasure.shared.repository.TagRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@QuarkusTest
class DeviceResourceTest
{
	@Inject
	DeviceRepository deviceRepository;

	@Inject
	DeviceHistoryRepository deviceHistoryRepository;

	@Inject
	MemberRepository memberRepository;

	@Inject
	TagRepository tagRepository;

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldOpenDeviceList()
	{
		given()
			.when().get("/devices")
			.then()
			.statusCode(200);
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldUnassignAllSelectedDevices()
	{
		Long assignedLaptop = createAssignedDevice("BulkLaptop", "maria");
		Long assignedPhone = createAssignedDevice("BulkPhone", "maria");

		try
		{
			postUnassignMany(assignedLaptop + "," + assignedPhone);

			assertUnassigned(assignedLaptop);
			assertUnassigned(assignedPhone);
		}
		finally
		{
			deleteDevices(assignedLaptop, assignedPhone);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldLeaveAlreadyUnassignedDeviceUntouched()
	{
		Long freeDevice = createFreeDevice("BulkFreeTablet");

		try
		{
			postUnassignMany(String.valueOf(freeDevice));

			Device device = findDevice(freeDevice);
			assertNull(device.getAssignedTo());
			assertEquals("available", device.getStatus());
			assertTrue(historyFor(freeDevice).isEmpty());
		}
		finally
		{
			deleteDevices(freeDevice);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldRecordUnassignHistoryWithPreviousHolder()
	{
		Long assignedDevice = createAssignedDevice("BulkHistoryLaptop", "maria");
		String previousHolder = findMember("maria").getDisplayName();

		try
		{
			postUnassignMany(String.valueOf(assignedDevice));

			List<DeviceHistory> history = historyFor(assignedDevice);
			assertEquals(1, history.size());

			DeviceHistory entry = history.get(0);
			assertEquals("UNASSIGNED", entry.getEventType());
			assertEquals("assignedTo", entry.getFieldName());
			assertEquals(previousHolder, entry.getOldValue());
			assertEquals("", entry.getNewValue());
			assertNotNull(entry.getActor());
		}
		finally
		{
			deleteDevices(assignedDevice);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldIgnoreUnknownIds()
	{
		postUnassignMany("999999999,");
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldReturnNotFoundWhenEditingUnknownDevice()
	{
		// given
		long unknownId = 999_999_999L;

		// when
		Response response = given().when().get("/devices/" + unknownId + "/edit");

		// then
		response.then().statusCode(404);
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldRejectAssignmentWhenMemberIsUnknown()
	{
		// given
		Long device = createFreeDevice("AssignGhostTablet");
		try
		{
			// when
			Response response = postAssign(device, "ghost-user-does-not-exist");

			// then
			response.then().statusCode(400);
		}
		finally
		{
			deleteDevices(device);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldLeaveDeviceAvailableWhenAssignmentToUnknownMemberIsRejected()
	{
		// given
		Long device = createFreeDevice("AssignGhostPhone");
		try
		{
			// when
			postAssign(device, "ghost-user-does-not-exist");

			// then
			assertUnassigned(device);
		}
		finally
		{
			deleteDevices(device);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldAssignDeviceWhenMemberExists()
	{
		// given
		Long device = createFreeDevice("AssignLaptop");
		Member maria = findMember("maria");
		try
		{
			// when
			postAssign(device, "maria");

			// then
			Device assigned = findDevice(device);
			assertEquals(maria.getId(), assigned.getAssignedTo().getId());
			assertEquals("not available", assigned.getStatus());
		}
		finally
		{
			deleteDevices(device);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldNotRecordHistoryWhenDeviceIsAssignedToTheSameMemberAgain()
	{
		// given
		Long device = createAssignedDevice("AssignTwiceLaptop", "maria");
		try
		{
			// when
			postAssign(device, "maria");

			// then
			assertTrue(historyFor(device).isEmpty());
		}
		finally
		{
			deleteDevices(device);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldCreateDeviceWhenNameUsesNonLatinLetters()
	{
		// given
		String name = "ノートパソコン Ökö";
		try
		{
			// when
			postCreate(name, "");

			// then
			assertNotNull(findDeviceByName(name));
		}
		finally
		{
			deleteDevicesNamed(name);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldStoreTrimmedGroupWhenCreatingDevice()
	{
		// given
		String name = "TrimGroupLaptop";
		try
		{
			// when
			postCreate(name, "  Trim Test Group  ");

			// then
			assertEquals("Trim Test Group", findDeviceByName(name).getGroup());
		}
		finally
		{
			deleteDevicesNamed(name);
			deleteTag("Trim Test Group");
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldStoreNoGroupWhenGroupIsBlank()
	{
		// given
		String name = "BlankGroupLaptop";
		try
		{
			// when
			postCreate(name, "   ");

			// then
			assertNull(findDeviceByName(name).getGroup());
		}
		finally
		{
			deleteDevicesNamed(name);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldStorePurchaseDateWhenCreatingDevice()
	{
		// given
		String name = "PurchaseDateLaptop";
		try
		{
			// when
			postCreate(Map.of("deviceName", name, "deviceAge", "2023-07-19"));

			// then
			assertEquals(LocalDate.of(2023, 7, 19), findDeviceByName(name).getDeviceAge());
		}
		finally
		{
			deleteDevicesNamed(name);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldStoreModelYearWhenCreatingDevice()
	{
		// given
		String name = "ModelYearLaptop";
		try
		{
			// when
			postCreate(Map.of("deviceName", name, "deviceModelDate", "2023"));

			// then
			assertEquals(2023, findDeviceByName(name).getDeviceModelDate());
		}
		finally
		{
			deleteDevicesNamed(name);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldRejectCreationWhenPurchaseDateIsNotADate()
	{
		// given
		String name = "BadDateLaptop";
		try
		{
			// when
			postCreate(Map.of("deviceName", name, "deviceAge", "yesterday"));

			// then
			assertNull(findDeviceByName(name));
		}
		finally
		{
			deleteDevicesNamed(name);
		}
	}

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldSetCreatedOnWhenCreatingDevice()
	{
		// given
		String name = "CreatedOnLaptop";
		try
		{
			// when
			postCreate(name, "");

			// then
			assertNotNull(findDeviceByName(name).getCreatedOn());
		}
		finally
		{
			deleteDevicesNamed(name);
		}
	}

	private String csrfToken()
	{
		return given()
			.when().get("/devices")
			.then()
			.statusCode(200)
			.extract().cookie("csrf-token");
	}

	private Response postAssign(Long deviceId, String username)
	{
		String csrfToken = csrfToken();
		return given()
			.redirects().follow(false)
			.cookie("csrf-token", csrfToken)
			.formParam("csrf-token", csrfToken)
			.contentType("application/x-www-form-urlencoded; charset=UTF-8")
			.formParam("assignedTo", username)
			.when().post("/devices/" + deviceId + "/assign");
	}

	private Response postCreate(String deviceName, String group)
	{
		return postCreate(Map.of("deviceName", deviceName, "group", group));
	}

	private Response postCreate(Map<String, String> fields)
	{
		String csrfToken = csrfToken();
		return given()
			.redirects().follow(false)
			.cookie("csrf-token", csrfToken)
			.formParam("csrf-token", csrfToken)
			.contentType("application/x-www-form-urlencoded; charset=UTF-8")
			.formParams(fields)
			.when().post("/devices/create");
	}

	@Transactional(TxType.REQUIRES_NEW)
	Device findDeviceByName(String name)
	{
		return deviceRepository.find("deviceName", name).firstResult();
	}

	@Transactional(TxType.REQUIRES_NEW)
	void deleteDevicesNamed(String name)
	{
		for (Device device : deviceRepository.list("deviceName", name))
		{
			deviceHistoryRepository.delete("device.id = ?1", device.id);
			deviceRepository.delete(device);
		}
	}

	@Transactional(TxType.REQUIRES_NEW)
	void deleteTag(String name)
	{
		tagRepository.delete("name", name);
	}

	private void postUnassignMany(String ids)
	{
		String csrfToken = given()
			.when().get("/devices")
			.then()
			.statusCode(200)
			.extract().cookie("csrf-token");

		given()
			.redirects().follow(false)
			.cookie("csrf-token", csrfToken)
			.formParam("csrf-token", csrfToken)
			.formParam("ids", ids)
			.formParam("redirectUrl", "/devices")
			.when().post("/devices/unassign-many")
			.then()
			.statusCode(303);
	}

	@Transactional(TxType.REQUIRES_NEW)
	Long createAssignedDevice(String name, String username)
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
	Long createFreeDevice(String name)
	{
		Device device = new Device();
		device.setDeviceName(name);
		device.setStatus("available");
		deviceRepository.persist(device);
		return device.id;
	}

	@Transactional(TxType.REQUIRES_NEW)
	void deleteDevices(Long... ids)
	{
		for (Long id : ids)
		{
			deviceHistoryRepository.delete("device.id = ?1", id);
			deviceRepository.deleteById(id);
		}
	}

	@Transactional(TxType.REQUIRES_NEW)
	Device findDevice(Long id)
	{
		return deviceRepository.findById(id);
	}

	@Transactional(TxType.REQUIRES_NEW)
	Member findMember(String username)
	{
		return memberRepository.findByUsername(username);
	}

	@Transactional(TxType.REQUIRES_NEW)
	List<DeviceHistory> historyFor(Long deviceId)
	{
		return deviceHistoryRepository.forDevice(deviceId);
	}

	void assertUnassigned(Long deviceId)
	{
		Device device = findDevice(deviceId);
		assertNull(device.getAssignedTo());
		assertNull(device.getPickupTime());
		assertEquals("available", device.getStatus());
	}
}
