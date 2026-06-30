package app.treasure.member.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
