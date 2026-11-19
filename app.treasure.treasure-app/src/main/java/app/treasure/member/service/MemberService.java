package app.treasure.member.service;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.device.repository.DeviceRepository;
import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import app.treasure.shared.security.Roles;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Adds and changes members. An admin can add a person before they have signed
 * in: the member can hold devices right away and shows as never signed in until
 * their first login, when {@link MemberProvisioningService} links the login to
 * this row by email. Admins can change every member, everyone else only
 * themselves. Members are never deleted, because the device history refers to
 * them; an admin deactivates a member who should no longer be given devices.
 */
@ApplicationScoped
public class MemberService
{
	private static final Logger LOG = LoggerFactory.getLogger(MemberService.class);

	static final int MAX_NAME_LENGTH = 100;
	static final int MAX_PHONE_LENGTH = 50;

	/**
	 * Deliberately loose: one @, something on both sides, a dot in the domain.
	 */
	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	@Inject
	MemberRepository memberRepository;

	@Inject
	DeviceRepository deviceRepository;

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
		String first = required(firstName, "firstName");
		String last = required(lastName, "lastName");
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

	/**
	 * Changes name, email and phone of a member. The caller checks
	 * {@link #mayEdit} first.
	 *
	 * @param phone
	 *            optional; blank clears it
	 * @throws MemberValidationException
	 *             if a field is missing or the email belongs to someone else
	 */
	@Transactional
	public Member update(Member member, String firstName, String lastName, String email, String phone,
		String changedBy)
	{
		String first = required(firstName, "firstName");
		String last = required(lastName, "lastName");
		String address = validEmail(email);
		requireEmailFree(address, member);
		String phoneNumber = phone == null || phone.isBlank() ? null : phone.trim();
		if (phoneNumber != null && phoneNumber.length() > MAX_PHONE_LENGTH)
		{
			throw new MemberValidationException("member.error.phoneTooLong", MAX_PHONE_LENGTH);
		}

		// someone who never signed in is known by their email until the first
		// login, so the username follows it; after that it is the login name
		if (member.isNeverSignedIn() && Objects.equals(member.getUserName(), member.getEmail()))
		{
			member.setUserName(address);
		}
		member.setFirstName(first);
		member.setLastName(last);
		member.setEmail(address);
		member.setPhone(phoneNumber);
		LOG.info("Member {} ({}) changed by {}", member.getDisplayName(), member.getUserName(), changedBy);
		return member;
	}

	/**
	 * Deactivates a member: they stay in the list and the history, but can no
	 * longer be given devices and their API keys stop working. The caller
	 * checks that the actor is an admin.
	 *
	 * @param actorName
	 *            the username of the admin doing it
	 * @throws MemberValidationException
	 *             if the admin tries to deactivate themselves or the member
	 *             still holds devices
	 */
	@Transactional
	public void deactivate(Member member, String actorName)
	{
		if (Objects.equals(actorName, member.getUserName()))
		{
			throw new MemberValidationException("member.error.deactivateSelf");
		}
		long held = deviceRepository.countAssignedTo(member);
		if (held > 0)
		{
			throw new MemberValidationException(held == 1 ? "member.error.holds.one" : "member.error.holds.many",
				member.getDisplayName(), held);
		}
		member.setActive(false);
		LOG.info("Member {} ({}) deactivated by {}", member.getDisplayName(), member.getUserName(), actorName);
	}

	/** Makes a deactivated member available for devices again. */
	@Transactional
	public void reactivate(Member member, String actorName)
	{
		member.setActive(true);
		LOG.info("Member {} ({}) reactivated by {}", member.getDisplayName(), member.getUserName(), actorName);
	}

	/** Admins may change every member, everyone else only their own. */
	public boolean mayEdit(Member member, SecurityIdentity identity)
	{
		if (identity == null || identity.isAnonymous())
		{
			return false;
		}
		return identity.hasRole(Roles.ADMIN) || identity.getPrincipal().getName().equals(member.getUserName());
	}

	private void requireEmailFree(String address, Member self)
	{
		Member byEmail = memberRepository.findByEmail(address);
		Member byUserName = memberRepository.findByUsername(address);
		if ((byEmail != null && byEmail != self) || (byUserName != null && byUserName != self))
		{
			throw new MemberValidationException("member.error.emailTaken", address);
		}
	}

	private static String validEmail(String email)
	{
		String address = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		if (!EMAIL.matcher(address).matches())
		{
			throw new MemberValidationException("member.error.emailInvalid");
		}
		return address;
	}

	/**
	 * @param field
	 *            "firstName" or "lastName", the middle part of the message keys
	 */
	private static String required(String value, String field)
	{
		String trimmed = value == null ? "" : value.trim();
		if (trimmed.isEmpty())
		{
			throw new MemberValidationException("member.error." + field + "Required");
		}
		if (trimmed.length() > MAX_NAME_LENGTH)
		{
			throw new MemberValidationException("member.error." + field + "TooLong", MAX_NAME_LENGTH);
		}
		return trimmed;
	}
}
