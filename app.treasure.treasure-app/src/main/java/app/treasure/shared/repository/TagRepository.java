package app.treasure.shared.repository;

import app.treasure.shared.domain.Tag;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TagRepository implements PanacheRepository<Tag>
{
	/**
	 * Finds the tag with this name or creates it. The insert leaves an existing
	 * row alone, so two requests saving the same new name at the same moment
	 * both succeed instead of one failing on the unique name.
	 *
	 * @param name
	 *            The tag name
	 * @return The existing or newly created tag
	 */
	public Tag findOrCreate(String name)
	{
		getEntityManager()
			.createNativeQuery("insert into Tag (id, name) values (nextval('Tag_SEQ'), :name) "
				+ "on conflict (name) do nothing")
			.setParameter("name", name)
			.executeUpdate();
		return find("name", name).firstResult();
	}
}
