package app.treasure.member.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

@Entity
public class Member extends PanacheEntity
{
	/** Invite type of a member an admin added on the Members page. */
	public static final String INVITE_TYPE_INVITED = "INVITED";

	/** Invite type of the demo users created in dev and test mode. */
	public static final String INVITE_TYPE_BOOTSTRAP = "BOOTSTRAP";

	@NotBlank
	private String firstName;

	@NotBlank
	private String lastName;

	@Email
	private String email;

	private String phone;

	@Column(unique = true)
	private String userName;

	/**
	 * ID of the member who invited this member (if invited).
	 */
	@Column(name = "invited_by_member_id")
	private Long invitedByMemberId;

	/**
	 * How the member came into the system: BOOTSTRAP (demo user in dev and
	 * test), SSO (created on first sign-in), INVITED (added by an admin), or
	 * null for the seeded demo members.
	 */
	@Column(length = 20)
	private String inviteType;

	/**
	 * When the member joined.
	 */
	@Column(name = "joined_at")
	private Instant joinedAt;

	/**
	 * Stable user id at the identity provider: Keycloak's user id in dev, the
	 * Entra object id (oid) on a stand.
	 */
	@Column(name = "subject", unique = true)
	private String subject;

	public Long getId()
	{
		return id;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	public String getLastName()
	{
		return lastName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	public String getPhone()
	{
		return phone;
	}

	public void setPhone(String phone)
	{
		this.phone = phone;
	}

	public String getUserName()
	{
		return userName;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	public String getDisplayName()
	{
		return firstName + " " + lastName;
	}

	/** Two letters for avatars, e.g. "MS" for Maria Schmidt. */
	public String getInitials()
	{
		String first = firstName != null && !firstName.isBlank() ? firstName.substring(0, 1) : "";
		String last = lastName != null && !lastName.isBlank() ? lastName.substring(0, 1) : "";
		return (first + last).toUpperCase();
	}

	/**
	 * True until the person signs in for the first time, for example after an
	 * admin added them or for the dev demo members. A first login sets
	 * {@code joinedAt}.
	 */
	public boolean isNeverSignedIn()
	{
		return joinedAt == null;
	}

	public String getStatusLabel()
	{
		return isNeverSignedIn() ? "Never signed in" : "In the system";
	}

	public Long getInvitedByMemberId()
	{
		return invitedByMemberId;
	}

	public void setInvitedByMemberId(Long invitedByMemberId)
	{
		this.invitedByMemberId = invitedByMemberId;
	}

	public String getInviteType()
	{
		return inviteType;
	}

	public void setInviteType(String inviteType)
	{
		this.inviteType = inviteType;
	}

	public Instant getJoinedAt()
	{
		return joinedAt;
	}

	public void setJoinedAt(Instant joinedAt)
	{
		this.joinedAt = joinedAt;
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}
}
