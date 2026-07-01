package app.treasure.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import app.treasure.organization.repository.OrganizationRepository;

class SlugUtilTest
{
	@Test
	void shouldGenerateSlugFromSimpleName()
	{
		OrganizationRepository repo = mock(OrganizationRepository.class);
		when(repo.findBySlug(anyString())).thenReturn(null);

		String slug = SlugUtil.generateUniqueSlug("Musikverein Harmonie", repo);

		assertEquals("musikverein-harmonie", slug);
	}

	@Test
	void shouldRemoveAccentsAndSpecialCharacters()
	{
		OrganizationRepository repo = mock(OrganizationRepository.class);
		when(repo.findBySlug(anyString())).thenReturn(null);

		String slug = SlugUtil.generateUniqueSlug("Café Glück & Co.!", repo);

		assertEquals("cafe-gluck-co", slug);
	}

	@Test
	void shouldFallbackToOrgForBlankName()
	{
		OrganizationRepository repo = mock(OrganizationRepository.class);
		when(repo.findBySlug(anyString())).thenReturn(null);

		String slug = SlugUtil.generateUniqueSlug("   ", repo);

		assertEquals("org", slug);
	}

	@Test
	void shouldAppendCounterWhenSlugAlreadyExists()
	{
		OrganizationRepository repo = mock(OrganizationRepository.class);
		// "verein" is taken, "verein-2" is free
		when(repo.findBySlug("verein")).thenReturn(new app.treasure.organization.domain.Organization());
		when(repo.findBySlug("verein-2")).thenReturn(null);

		String slug = SlugUtil.generateUniqueSlug("Verein", repo);

		assertEquals("verein-2", slug);
	}
}
