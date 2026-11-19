package app.treasure.shared.api;

import java.util.Set;

import org.jboss.resteasy.reactive.RestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.shared.util.LocalPath;
import io.quarkiverse.renarde.Controller;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

/**
 * The language switch of the account panel. Renarde keeps the choice in a
 * cookie and uses it for every page from then on; without the cookie the
 * browser's preferred language decides, and English is the fallback.
 */
@Authenticated
@Path("/language")
public class LanguageResource extends Controller
{
	private static final Logger LOG = LoggerFactory.getLogger(LanguageResource.class);

	/** The languages with a messages file; must match quarkus.locales. */
	public static final Set<String> LANGUAGES = Set.of("en", "de");

	@POST
	@Path("")
	public void change(@RestForm String lang, @RestForm String redirectUrl)
	{
		if (lang == null || !LANGUAGES.contains(lang))
		{
			LOG.warn("Refused to switch to unsupported language '{}'", lang);
			throw new BadRequestException("Unsupported language");
		}
		i18n.set(lang);
		LOG.debug("Language switched to {}", lang);
		seeOther(LocalPath.orElse(redirectUrl, "/"));
	}
}
