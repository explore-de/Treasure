package app.treasure.device.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import app.treasure.member.domain.Member;

class DeviceTest
{
	@Test
	void shouldReturnEmptyBookedNameWhenNotBooked()
	{
		Device device = new Device();

		assertEquals("", device.getBookedName());
	}

	@Test
	void shouldReturnBookedMemberDisplayName()
	{
		Member member = new Member();
		member.setFirstName("Max");
		member.setLastName("Mustermann");

		Device device = new Device();
		device.setAssignedTo(member);

		assertEquals("Max Mustermann", device.getBookedName());
	}

	@Test
	void shouldReturnEmptyFormattedPickupTimeWhenNull()
	{
		Device device = new Device();

		assertEquals("", device.getFormattedPickupTime());
	}

	@Test
	void shouldFormatPickupTime()
	{
		Device device = new Device();
		device.setPickupTime(LocalDateTime.of(2026, 6, 30, 14, 5));

		assertEquals("30.06.2026 14:05", device.getFormattedPickupTime());
	}

	@Test
	void shouldReturnEmptyFormattedCreatedOnWhenNull()
	{
		// given
		Device device = new Device();

		// when
		String formatted = device.getFormattedCreatedOn();

		// then
		assertEquals("", formatted);
	}

	@Test
	void shouldFormatCreatedOnWhenSet()
	{
		// given
		Device device = new Device();
		device.setCreatedOn(LocalDateTime.of(2026, 10, 22, 12, 22));

		// when
		String formatted = device.getFormattedCreatedOn();

		// then
		assertEquals("22.10.2026 12:22", formatted);
	}

	@Test
	void shouldReturnEmptyFormattedDeviceAgeWhenNull()
	{
		// given
		Device device = new Device();

		// when
		String formatted = device.getFormattedDeviceAge();

		// then
		assertEquals("", formatted);
	}

	@Test
	void shouldFormatDeviceAgeAsDayMonthYearWhenSet()
	{
		// given
		Device device = new Device();
		device.setDeviceAge(LocalDate.of(2023, 7, 19));

		// when
		String formatted = device.getFormattedDeviceAge();

		// then
		assertEquals("19.07.2023", formatted);
	}
}
