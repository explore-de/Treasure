package app.treasure.organization.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import app.treasure.organization.domain.Organization;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class OrganizationRepositoryTest
{
	@Inject
	OrganizationRepository organizationRepository;

	@TestTransaction
	@Test
	void shouldFindBySlug()
	{
		Organization org = new Organization();
		org.setName("Acme Inc");
		org.setSlug("acme-inc");
		organizationRepository.persist(org);

		Organization found = organizationRepository.findBySlug("acme-inc");

		assertNotNull(found);
		assertEquals("Acme Inc", found.getName());
	}

	@TestTransaction
	@Test
	void shouldReturnNullWhenSlugNotFound()
	{
		assertNull(organizationRepository.findBySlug("does-not-exist"));
	}

	@TestTransaction
	@Test
	void shouldFindByName()
	{
		Organization org = new Organization();
		org.setName("Globex");
		org.setSlug("globex");
		organizationRepository.persist(org);

		Organization found = organizationRepository.findByName("Globex");

		assertNotNull(found);
		assertEquals("globex", found.getSlug());
	}
}
