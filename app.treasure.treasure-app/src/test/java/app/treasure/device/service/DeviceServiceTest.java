package app.treasure.device.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import app.treasure.device.domain.Device;
import app.treasure.device.repository.DeviceHistoryRepository;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.device.service.DeviceService.DeviceData;
import app.treasure.member.domain.Member;
import app.treasure.shared.BaseMemberTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@QuarkusTest
class DeviceServiceTest extends BaseMemberTest
{
	@Inject
	DeviceService deviceService;

	@Inject
	DeviceRepository deviceRepository;

	@Inject
	DeviceHistoryRepository deviceHistoryRepository;

	@Test
	void shouldStoreLocationWhenItHasTheFull500Characters()
	{
		// given
		Member actor = createTestMember("svc-long-" + UUID.randomUUID().toString().substring(0, 8));
		String location = "x".repeat(500);
		Long deviceId = null;
		try
		{
			// when
			deviceId = createDevice(withNameAndLocation("LongLocationLaptop", location), actor);

			// then
			assertEquals(location, findDevice(deviceId).getDeviceLocation());
		}
		finally
		{
			if (deviceId != null)
			{
				deleteDevice(deviceId);
			}
		}
	}

	@Test
	void shouldTrimNameWhenCreatingDevice()
	{
		// given
		Member actor = createTestMember("svc-trim-" + UUID.randomUUID().toString().substring(0, 8));
		DeviceData data = withNameAndSerial("  Trimmed Laptop  ", null);
		Long deviceId = null;
		try
		{
			// when
			deviceId = createDevice(data, actor);

			// then
			assertEquals("Trimmed Laptop", findDevice(deviceId).getDeviceName());
		}
		finally
		{
			if (deviceId != null)
			{
				deleteDevice(deviceId);
			}
		}
	}

	@Test
	void shouldStoreNoSerialNumberWhenItIsBlank()
	{
		// given
		Member actor = createTestMember("svc-blank-" + UUID.randomUUID().toString().substring(0, 8));
		DeviceData data = withNameAndSerial("BlankSerialLaptop", "   ");
		Long deviceId = null;
		try
		{
			// when
			deviceId = createDevice(data, actor);

			// then
			assertNull(findDevice(deviceId).getDeviceSerialNumber());
		}
		finally
		{
			if (deviceId != null)
			{
				deleteDevice(deviceId);
			}
		}
	}

	@Test
	void shouldRefuseCreateWhenModelYearIsBefore1980()
	{
		// given
		Member actor = createTestMember("svc-year-" + UUID.randomUUID().toString().substring(0, 8));
		DeviceData data = new DeviceData("OldLaptop", null, null, null, null, null, null, null, 1979, null, null,
			null, null, null);

		// when
		DeviceValidationException e = assertThrows(DeviceValidationException.class,
			() -> createDevice(data, actor));

		// then
		assertEquals("Model year must be between 1980 and 2100.", e.getMessage());
	}

	private static DeviceData withNameAndLocation(String name, String location)
	{
		return new DeviceData(name, null, null, null, null, null, null, null, null, null, null, null, location,
			null);
	}

	private static DeviceData withNameAndSerial(String name, String serialNumber)
	{
		return new DeviceData(name, serialNumber, null, null, null, null, null, null, null, null, null, null, null,
			null);
	}

	@Transactional(TxType.REQUIRES_NEW)
	Long createDevice(DeviceData data, Member actor)
	{
		return deviceService.create(data, memberRepository.findById(actor.id)).id;
	}

	@Transactional(TxType.REQUIRES_NEW)
	Device findDevice(Long id)
	{
		return deviceRepository.findById(id);
	}

	@Transactional(TxType.REQUIRES_NEW)
	void deleteDevice(Long id)
	{
		deviceHistoryRepository.delete("device.id = ?1", id);
		deviceRepository.deleteById(id);
	}
}
