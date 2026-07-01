package app.treasure.device.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

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

		List<Device> all = deviceRepository.findAllDevices();

		assertEquals(before + 2, all.size());
	}
}
