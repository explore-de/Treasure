package app.treasure.shared.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TagTest
{
	@Test
	void shouldBeEqualWhenNamesAreEqual()
	{
		Tag a = new Tag("food");
		Tag b = new Tag("food");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void shouldNotBeEqualWhenNamesDiffer()
	{
		Tag a = new Tag("food");
		Tag b = new Tag("travel");

		assertNotEquals(a, b);
	}

	@Test
	void shouldUseNameAsStringRepresentation()
	{
		Tag tag = new Tag("food");

		assertEquals("food", tag.toString());
	}
}
