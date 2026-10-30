package app.treasure.member.repository;

import app.treasure.member.domain.Member;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MemberRepository implements PanacheRepository<Member>
{
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
}
