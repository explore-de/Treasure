package app.treasure.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SortHelperTest
{
	private static final Map<String, String> ALLOWED = Map.of("name", "d.name", "createdAt", "d.createdAt");

	@Test
	void shouldBuildOrderByForValidFieldAndDirection()
	{
		String result = SortHelper.buildOrderByClause(ALLOWED, "name", "asc", "name", "asc");

		assertEquals("d.name ASC, d.id ASC", result);
	}

	@Test
	void shouldFallbackToDefaultWhenFieldNotAllowed()
	{
		String result = SortHelper.buildOrderByClause(ALLOWED, "hacker; DROP TABLE", "asc", "name", "asc");

		assertEquals("d.name ASC, d.id ASC", result);
	}

	@Test
	void shouldFallbackToDefaultWhenFieldIsNull()
	{
		String result = SortHelper.buildOrderByClause(ALLOWED, null, "asc", "name", "asc");

		assertEquals("d.name ASC, d.id ASC", result);
	}

	@Test
	void shouldFallbackToDefaultDirectionWhenDirectionInvalid()
	{
		String result = SortHelper.buildOrderByClause(ALLOWED, "name", "garbage", "name", "desc");

		assertEquals("d.name DESC, d.id DESC", result);
	}

	@Test
	void shouldNormalizeDirectionCaseInsensitively()
	{
		String result = SortHelper.buildOrderByClause(ALLOWED, "name", "DeSc", "name", "asc");

		assertEquals("d.name DESC, d.id DESC", result);
	}

	@Test
	void shouldOmitAliasPrefixWhenColumnHasNoDot()
	{
		Map<String, String> noAlias = Map.of("name", "name");

		String result = SortHelper.buildOrderByClause(noAlias, "name", "asc", "name", "asc");

		assertEquals("name ASC, id ASC", result);
	}
}
