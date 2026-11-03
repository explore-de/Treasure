package app.treasure.device.service;

/** Device data that breaks a rule; the message is shown to the user as is. */
public class DeviceValidationException extends RuntimeException
{
	public DeviceValidationException(String message)
	{
		super(message);
	}
}
