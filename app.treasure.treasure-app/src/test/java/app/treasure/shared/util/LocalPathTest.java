package app.treasure.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LocalPathTest
{
	@Test
	void shouldKeepTargetWhenItIsAPathOnThisSite()
	{
		// given
		String target = "/devices?status=available";

		// when
		String path = LocalPath.orElse(target, "/");

		// then
		assertEquals("/devices?status=available", path);
	}

	@Test
	void shouldReturnFallbackWhenTargetIsAnotherSite()
	{
		// given
		String target = "//evil.example/devices";

		// when
		String path = LocalPath.orElse(target, "/");

		// then
		assertEquals("/", path);
	}

	@Test
	void shouldReturnFallbackWhenTargetIsMissing()
	{
		// given
		String target = null;

		// when
		String path = LocalPath.orElse(target, "/devices");

		// then
		assertEquals("/devices", path);
	}
}
