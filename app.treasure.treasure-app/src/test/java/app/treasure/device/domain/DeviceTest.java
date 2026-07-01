package app.treasure.device.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		device.setBookedBy(member);

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
}
