package app.treasure.device.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import app.treasure.device.domain.Device;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class DeviceRepositoryTest
{
	@Inject
	DeviceRepository deviceRepository;

	@TestTransaction
	@Test
	void shouldListAllPersistedDevices()
	{
		long before = deviceRepository.count();

		Device laptop = new Device();
		laptop.setDeviceName("Laptop");
		deviceRepository.persist(laptop);

		Device phone = new Device();
		phone.setDeviceName("Phone");
		deviceRepository.persist(phone);

		List<Device> all = deviceRepository.listAll();

		assertEquals(before + 2, all.size());
	}

	@TestTransaction
	@Test
	void shouldListEachGroupOnceWhenSeveralDevicesShareIt()
	{
		// given
		String group = "Shared group " + UUID.randomUUID();
		persistDeviceInGroup("SharedGroupLaptop", group);
		persistDeviceInGroup("SharedGroupPhone", group);

		// when
		List<String> groups = deviceRepository.findDistinctGroups();

		// then
		assertEquals(1, groups.stream().filter(group::equals).count());
	}

	@TestTransaction
	@Test
	void shouldLeaveOutBlankGroupWhenListingGroups()
	{
		// given
		persistDeviceInGroup("BlankGroupTablet", "");

		// when
		List<String> groups = deviceRepository.findDistinctGroups();

		// then
		assertFalse(groups.contains(""));
	}

	private void persistDeviceInGroup(String name, String group)
	{
		Device device = new Device();
		device.setDeviceName(name);
		device.setGroup(group);
		deviceRepository.persist(device);
	}
}
