package app.treasure.device.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.treasure.device.domain.Device;
import app.treasure.device.domain.DeviceHistory;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class DeviceHistoryRepositoryTest
{
	@Inject
	DeviceHistoryRepository historyRepository;

	@Inject
	DeviceRepository deviceRepository;

	@TestTransaction
	@Test
	void shouldReturnHistoryOrderedByHappenedAtDescending()
	{
		Device device = new Device();
		device.setDeviceName("Laptop");
		deviceRepository.persist(device);

		DeviceHistory older = new DeviceHistory();
		older.setDevice(device);
		older.setHappenedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
		historyRepository.persist(older);

		DeviceHistory newer = new DeviceHistory();
		newer.setDevice(device);
		newer.setHappenedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
		historyRepository.persist(newer);

		List<DeviceHistory> result = historyRepository.forDevice(device.id);

		assertEquals(2, result.size());
		assertEquals(newer.id, result.get(0).id);
		assertEquals(older.id, result.get(1).id);
	}

	@TestTransaction
	@Test
	void shouldReturnEmptyListForDeviceWithoutHistory()
	{
		Device device = new Device();
		device.setDeviceName("Empty");
		deviceRepository.persist(device);

		assertTrue(historyRepository.forDevice(device.id).isEmpty());
	}
}
