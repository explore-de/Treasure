package app.treasure.member.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
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
	 * Derives first and last name when the token has no name claims: from the
	 * display name ("Maria Schmidt"), else from the username
	 * ("maria.schmidt@example.com"). Both parts are always filled, because a
	 * Member must have a first and a last name.
	 */
	private static String[] splitName(String displayName, String username)
	{
		String[] words = displayName == null || displayName.isBlank() ? new String[0] : displayName.trim().split("\\s+");
		if (words.length >= 2)
		{
			return new String[] { words[0], String.join(" ", Arrays.copyOfRange(words, 1, words.length)) };
		}
		String localPart = username.contains("@") ? username.substring(0, username.indexOf('@')) : username;
		List<String> parts = Arrays.stream(localPart.split("[._-]+")).filter(p -> !p.isBlank()).toList();
		String first = words.length == 1 ? words[0] : capitalize(parts.isEmpty() ? username : parts.get(0));
		String last = parts.size() >= 2 ? capitalize(parts.get(parts.size() - 1)) : first;
		return new String[] { first, last };
	}

	/** "mustermann" becomes "Mustermann". */
	private static String capitalize(String word)
	{
		return word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1);
	}
}
