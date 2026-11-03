package app.treasure.member.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class MemberTest
{
	@Test
	void shouldBuildDisplayNameFromFirstAndLastName()
	{
		Member member = new Member();
		member.setFirstName("Max");
		member.setLastName("Mustermann");

		assertEquals("Max Mustermann", member.getDisplayName());
	}

	@Test
	void shouldShowNeverSignedInWhenMemberHasNoJoinDate()
	{
		// given
		Member member = new Member();

		// when
		String status = member.getStatusLabel();

		// then
		assertEquals("Never signed in", status);
	}

	@Test
	void shouldShowInTheSystemWhenMemberHasJoined()
	{
		// given
		Member member = new Member();
		member.setJoinedAt(Instant.parse("2026-11-03T08:00:00Z"));

		// when
		String status = member.getStatusLabel();

		// then
		assertEquals("In the system", status);
	}
}
