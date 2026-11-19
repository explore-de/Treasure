package app.treasure.shared.util;

/**
 * Redirect targets that come from a form: only paths on this site are followed.
 */
public final class LocalPath
{
	private LocalPath()
	{
		// static
	}

	/**
	 * The given target when it is a path on this site (starts with a single
	 * slash), else the fallback. Keeps a tampered form from sending someone to
	 * another site.
	 */
	public static String orElse(String redirectUrl, String fallback)
	{
		if (redirectUrl == null || redirectUrl.isBlank())
		{
			return fallback;
		}
		if (!redirectUrl.startsWith("/") || redirectUrl.startsWith("//") || redirectUrl.startsWith("/\\"))
		{
			return fallback;
		}
		return redirectUrl;
	}
}
