package app.treasure.member.service;

/** A new member that breaks a rule; the message is shown to the user as is. */
public class MemberValidationException extends RuntimeException
{
	public MemberValidationException(String message)
	{
		super(message);
	}
}
