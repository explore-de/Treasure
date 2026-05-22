package app.treasure.device.filter;

import app.treasure.device.domain.Device;

public interface DeviceFilter
{
	boolean matches(Device d);
}
