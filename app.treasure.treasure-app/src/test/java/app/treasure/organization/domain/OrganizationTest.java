package app.treasure.organization.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class OrganizationTest
{
	@Test
	void shouldSetCreatedAtOnCreate()
	{
		Organization org = new Organization();
		org.setName("Acme");

		org.onCreate();

		assertNotNull(org.getCreatedAt());
	}

	@Test
	void shouldDefaultDisplayNameToNameWhenBlank()
	{
		Organization org = new Organization();
		org.setName("Acme");

		org.onCreate();

		assertEquals("Acme", org.getDisplayName());
	}

	@Test
	void shouldKeepExplicitDisplayName()
	{
		Organization org = new Organization();
		org.setName("Acme");
		org.setDisplayName("Acme Corp");

		org.onCreate();

		assertEquals("Acme Corp", org.getDisplayName());
	}
}
