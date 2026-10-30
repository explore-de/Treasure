package app.treasure.dashboard.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.treasure.dashboard.api.DashboardResource.GroupCount;

class GroupCountTest
{
	@Test
	void shouldEncodeReservedCharactersWhenNameContainsAmpersandAndSpace()
	{
		// given
		GroupCount group = new GroupCount("R&D home", 3, 50);

		// when
		String encoded = group.encodedName();

		// then
		assertEquals("R%26D%20home", encoded);
	}
}
