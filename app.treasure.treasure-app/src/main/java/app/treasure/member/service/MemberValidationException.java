package app.treasure.member.service;

import app.treasure.shared.util.Messages;

/**
 * Member data that breaks a rule. Carries the key of its text in
 * messages.properties and the arguments, so a page can show it in the reader's
 * language; {@link #getMessage()} is the English text, which the MCP tools and
 * the log use.
 */
public class MemberValidationException extends RuntimeException
{
	private final String key;
	private final transient Object[] args;

	public MemberValidationException(String key, Object... args)
	{
		super(Messages.english(key, args));
		this.key = key;
		this.args = args;
	}

	public String getKey()
	{
		return key;
	}

	public Object[] getArgs()
	{
		return args;
	}
}
