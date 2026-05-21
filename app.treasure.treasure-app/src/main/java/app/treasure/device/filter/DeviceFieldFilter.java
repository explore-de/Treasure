package app.treasure.device.filter;

import app.treasure.device.domain.Device;
import java.util.function.Function;
import java.util.List;
import java.util.function.BiPredicate;

public class DeviceFieldFilter implements DeviceFilter
{
	private final List<String> values;
	private final Function<Device, String> getter;
	private final BiPredicate<String, String> compare;

	public DeviceFieldFilter(List<String> values, Function<Device, String> getter, BiPredicate<String, String> compare)
	{
		this.values = values;
		this.getter = getter;
		this.compare = compare;
	}

	@Override
	public boolean matches(Device d)
	{

		if (values.isEmpty())
		{
			return true;
		}
		else
		{
			return values.stream().anyMatch(v -> compare.test(getter.apply(d), v));
		}
	}
}
