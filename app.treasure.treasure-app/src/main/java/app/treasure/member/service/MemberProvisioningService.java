package app.treasure.member.service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

/**
 * Creates the Member row for a person the first time they sign in through the
 * identity provider. Dev mode bootstraps its demo users; on a stand this is the
 * only way members come into existence.
 */
@ApplicationScoped
public class MemberProvisioningService
{
	private static final Logger LOG = LoggerFactory.getLogger(MemberProvisioningService.class);
	static final String INVITE_TYPE_SSO = "SSO";

	@Inject
	MemberRepository memberRepository;

	/**
	 * Usernames already confirmed to have a Member row, to skip the lookup on
	 * later requests.
	 */
	private final Set<String> knownUsernames = ConcurrentHashMap.newKeySet();

	public boolean isKnown(String username)
	{
		return knownUsernames.contains(username);
	}

	/**
	 * Makes sure a Member exists for the signed-in user, creating it from the
	 * token claims if needed.
	 *
	 * @param username
	 *            The principal name, which is the username the app uses
	 *            everywhere
	 * @param token
	 *            The ID token with the user's profile claims
	 * @return the member
	 */
	@Transactional(TxType.REQUIRES_NEW)
	public Member ensureMember(String username, JsonWebToken token)
	{
		Member member = memberRepository.findByUsername(username);
		if (member == null)
		{
			member = createFromToken(username, token);
			LOG.info("Created member {} ({}) on first login", member.getDisplayName(), username);
		}
		knownUsernames.add(username);
		return member;
	}

	private Member createFromToken(String username, JsonWebToken token)
	{
		String email = firstClaim(token, "email", "upn");
		if (email == null && username.contains("@"))
		{
			email = username;
		}
		String firstName = firstClaim(token, "given_name");
		String lastName = firstClaim(token, "family_name");
		if (firstName == null || lastName == null)
		{
			String[] parts = splitName(firstClaim(token, "name"), username);
			firstName = firstName != null ? firstName : parts[0];
			lastName = lastName != null ? lastName : parts[1];
		}

		Member member = new Member();
		member.setUserName(username);
		member.setEmail(email);
		member.setFirstName(firstName);
		member.setLastName(lastName);
		member.setSubject(firstClaim(token, "oid", "sub"));
		member.setInviteType(INVITE_TYPE_SSO);
		member.setJoinedAt(Instant.now());
		memberRepository.persist(member);
		return member;
	}

	private static String firstClaim(JsonWebToken token, String... names)
	{
		for (String name : names)
		{
			Object value = token.getClaim(name);
			if (value instanceof String text && !text.isBlank())
			{
				return text.trim();
			}
		}
		return null;
	}

	/**
	 * "Maria Schmidt" becomes Maria / Schmidt; with no display name the
	 * username is used.
	 */
	private static String[] splitName(String displayName, String username)
	{
		String source = displayName != null && !displayName.isBlank() ? displayName.trim() : username;
		int space = source.indexOf(' ');
		if (space > 0)
		{
			return new String[] { source.substring(0, space), source.substring(space + 1).trim() };
		}
		return new String[] { source, "" };
	}
}
