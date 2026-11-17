package app.treasure.member.repository;

import java.util.List;
import java.util.Locale;

import app.treasure.member.domain.Member;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MemberRepository implements PanacheRepository<Member>
{
	/** Every member, by last name and then first name, for lists and menus. */
	public List<Member> listByName()
	{
		return listAll(Sort.by("lastName").and("firstName"));
	}

	/**
	 * The members who can be given a device, by last name and then first name:
	 * everyone who is not deactivated.
	 */
	public List<Member> listActiveByName()
	{
		return list("active", Sort.by("lastName").and("firstName"), true);
	}

	/**
	 * Every member for the paged Members list: active ones first, then by last
	 * name and first name.
	 */
	public PanacheQuery<Member> findAllForList()
	{
		return findAll(Sort.by("active", Sort.Direction.Descending).and("lastName").and("firstName").and("id"));
	}

	/**
	 * Finds a member by their username.
	 *
	 * @param userName
	 *            The username of the member to search for.
	 * @return The member with the specified username, or null if no such member
	 *         exists.
	 */
	public Member findByUsername(String userName)
	{
		return find("userName", userName).firstResult();
	}

	/**
	 * Finds a member by email address, ignoring case.
	 *
	 * @param email
	 *            The email address to search for.
	 * @return The member with that email, or null if no such member exists.
	 */
	public Member findByEmail(String email)
	{
		return find("lower(email) = ?1", email.toLowerCase(Locale.ROOT)).firstResult();
	}
}
