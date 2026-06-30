package app.treasure.device.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import app.treasure.member.domain.Member;

class DeviceHistoryTest
{
	@Test
	void shouldReturnSystemActorNameWhenActorIsNull()
	{
		DeviceHistory history = new DeviceHistory();

		assertEquals("system", history.getActorName());
	}

	@Test
	void shouldReturnActorDisplayName()
	{
		Member actor = new Member();
		actor.setFirstName("Anna");
		actor.setLastName("Schmidt");

		DeviceHistory history = new DeviceHistory();
		history.setActor(actor);

		assertEquals("Anna Schmidt", history.getActorName());
	}

	@Test
	void shouldReturnEmptyFormattedAtWhenNull()
	{
		DeviceHistory history = new DeviceHistory();

		assertEquals("", history.getFormattedAt());
	}

	@Test
	void shouldFormatHappenedAt()
	{
		DeviceHistory history = new DeviceHistory();
		history.setHappenedAt(LocalDateTime.of(2026, 1, 15, 9, 30));

		assertEquals("15.01.2026 09:30", history.getFormattedAt());
	}
}
