package app.treasure.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class MessagesTest
{
	@Test
	void shouldHaveNoKeyWithoutGermanTextWhenEnglishFileIsComplete() throws IOException
	{
		// given
		Set<String> english = keysOf("messages.properties");
		Set<String> german = keysOf("messages_de.properties");

		// when
		Set<String> missing = new TreeSet<>(english);
		missing.removeAll(german);

		// then
		assertEquals(Set.of(), missing);
	}

	@Test
	void shouldHaveNoGermanKeyWithoutEnglishTextWhenGermanFileIsComplete() throws IOException
	{
		// given
		Set<String> english = keysOf("messages.properties");
		Set<String> german = keysOf("messages_de.properties");

		// when
		Set<String> unknown = new TreeSet<>(german);
		unknown.removeAll(english);

		// then
		assertEquals(Set.of(), unknown);
	}

	@Test
	void shouldFillArgumentsWhenEnglishTextHasPlaceholders()
	{
		// given
		String key = "device.error.modelYear";

		// when
		String text = Messages.english(key, 1980, 2100);

		// then
		assertEquals("Model year must be between 1980 and 2100.", text);
	}

	@Test
	void shouldReturnKeyWhenNoEnglishTextExists()
	{
		// given
		String key = "no.such.message";

		// when
		String text = Messages.english(key);

		// then
		assertEquals("no.such.message", text);
	}

	private static Set<String> keysOf(String file) throws IOException
	{
		Properties properties = new Properties();
		try (InputStream in = MessagesTest.class.getClassLoader().getResourceAsStream(file);
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			properties.load(reader);
		}
		return new TreeSet<>(properties.stringPropertyNames());
	}
}
