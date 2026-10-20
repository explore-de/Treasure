package app.treasure.shared.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import app.treasure.shared.domain.Tag;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class TagRepositoryTest
{
	@Inject
	TagRepository tagRepository;

	@TestTransaction
	@Test
	void shouldCreateTagWhenMissing()
	{
		Tag tag = tagRepository.findOrCreate("laptops");

		assertNotNull(tag.getId());
		assertEquals("laptops", tag.getName());
		assertEquals("laptops", tagRepository.find("name", "laptops").firstResult().getName());
	}

	@TestTransaction
	@Test
	void shouldReturnExistingTagInsteadOfDuplicating()
	{
		tagRepository.persist(new Tag("phones"));
		long countBefore = tagRepository.count();

		Tag tag = tagRepository.findOrCreate("phones");

		assertEquals("phones", tag.getName());
		assertEquals(countBefore, tagRepository.count());
	}
}
