package app.treasure.shared.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * The English text of a message key, for everything that does not speak the
 * language of a browser: the MCP tools, the log, and the message of a
 * validation exception. Pages are translated by Renarde instead
 * ({@code i18n.formatMessage} in controllers, {@code m:key} in templates),
 * which reads the same {@code messages.properties}.
 */
public final class Messages
{
	private static final String ENGLISH_FILE = "messages.properties";

	private static final Properties ENGLISH = load();

	private Messages()
	{
		// static
	}

	/**
	 * The English text of the key with its {@code %s} arguments filled in; the
	 * key itself when no text exists, like Renarde does.
	 */
	public static String english(String key, Object... args)
	{
		String text = ENGLISH.getProperty(key);
		if (text == null)
		{
			return key;
		}
		return args.length == 0 ? text : String.format(text, args);
	}

	private static Properties load()
	{
		Properties properties = new Properties();
		try (InputStream in = Messages.class.getClassLoader().getResourceAsStream(ENGLISH_FILE))
		{
			if (in == null)
			{
				throw new IllegalStateException(ENGLISH_FILE + " is missing from the classpath");
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				properties.load(reader);
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return properties;
	}
}
