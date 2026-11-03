package app.treasure.member.service;

import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Adds members. An admin can add a person before they have signed in: the
 * member can hold devices right away and shows as never signed in until their
 * first login, when {@link MemberProvisioningService} links the login to this
 * row by email.
 */
@ApplicationScoped
public class MemberService
{
	private static final Logger LOG = LoggerFactory.getLogger(MemberService.class);

	static final int MAX_NAME_LENGTH = 100;

	/**
	 * Deliberately loose: one @, something on both sides, a dot in the domain.
	 */
	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	@Inject
	MemberRepository memberRepository;

	/**
	 * Creates a member who has not signed in yet. The username is the email
	 * address, which is what Entra sends as the principal name on a stand.
	 *
	 * @throws MemberValidationException
	 *             if a field is missing or the email is already taken
	 */
	@Transactional
	public Member add(String firstName, String lastName, String email, Member addedBy)
	{
		String first = required(firstName, "first name");
		String last = required(lastName, "last name");
		String address = validEmail(email);
		requireEmailFree(address, null);

		Member member = new Member();
		member.setFirstName(first);
		member.setLastName(last);
		member.setEmail(address);
		member.setUserName(address);
		member.setInviteType(Member.INVITE_TYPE_INVITED);
		member.setInvitedByMemberId(addedBy != null ? addedBy.getId() : null);
		memberRepository.persist(member);
		LOG.info("Added member {} ({}) by {}", member.getDisplayName(), address,
			addedBy != null ? addedBy.getUserName() : "unknown");
		return member;
	}

	private void requireEmailFree(String address, Member self)
	{
		Member byEmail = memberRepository.findByEmail(address);
		Member byUserName = memberRepository.findByUsername(address);
		if ((byEmail != null && byEmail != self) || (byUserName != null && byUserName != self))
		{
			throw new MemberValidationException("A member with the email " + address + " already exists.");
		}
	}

	private static String validEmail(String email)
	{
		String address = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		if (!EMAIL.matcher(address).matches())
		{
			throw new MemberValidationException("Enter a valid email address.");
		}
		return address;
	}

	private static String required(String value, String label)
	{
		String trimmed = value == null ? "" : value.trim();
		if (trimmed.isEmpty())
		{
			throw new MemberValidationException("Enter a " + label + ".");
		}
		if (trimmed.length() > MAX_NAME_LENGTH)
		{
			throw new MemberValidationException("The " + label + " can have at most " + MAX_NAME_LENGTH
				+ " characters.");
		}
		return trimmed;
	}
}
