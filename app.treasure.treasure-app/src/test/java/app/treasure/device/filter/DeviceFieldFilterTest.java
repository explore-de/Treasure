package app.treasure.device.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import app.treasure.device.domain.Device;

class DeviceFieldFilterTest
{
	@Test
	void shouldMatchAnyDeviceWhenNoValuesGiven()
	{
		DeviceFieldFilter filter = new DeviceFieldFilter(List.of(), Device::getStatus, String::equals);

		Device device = new Device();
		device.setStatus("AVAILABLE");

		assertTrue(filter.matches(device));
	}

	@Test
	void shouldMatchWhenFieldEqualsOneOfValues()
	{
		DeviceFieldFilter filter = new DeviceFieldFilter(List.of("BOOKED", "AVAILABLE"), Device::getStatus,
			String::equals);

		Device device = new Device();
		device.setStatus("AVAILABLE");

		assertTrue(filter.matches(device));
	}

	@Test
	void shouldNotMatchWhenFieldDoesNotEqualAnyValue()
	{
		DeviceFieldFilter filter = new DeviceFieldFilter(List.of("BOOKED"), Device::getStatus, String::equals);

		Device device = new Device();
		device.setStatus("AVAILABLE");

		assertFalse(filter.matches(device));
	}
}
