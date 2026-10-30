package app.treasure.shared.repository;

import app.treasure.shared.domain.Tag;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TagRepository implements PanacheRepository<Tag>
{
	/**
	 * Finds the tag with this name or creates it.
	 *
	 * @param name
	 *            The tag name
	 * @return The existing or newly created tag
	 */
	public Tag findOrCreate(String name)
	{
		Tag existing = find("name", name).firstResult();
		if (existing != null)
		{
			return existing;
		}
		Tag tag = new Tag(name);
		persist(tag);
		return tag;
	}
}
