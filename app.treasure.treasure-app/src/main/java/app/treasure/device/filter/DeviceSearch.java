package app.treasure.device.filter;

import java.util.List;

/**
 * The filters of the device list. Every list holds the values given for one
 * filter: a device matches a filter when it matches one of its values, and it
 * is listed when it matches every filter that has values. Text filters look for
 * the value anywhere in the field, ignoring case; statuses and conditions must
 * be equal, ignoring case.
 */
public record DeviceSearch(List<String> names, List<String> statuses, List<String> holders, List<String> serialNumbers,
	List<String> groups, List<String> models, List<String> conditions, List<String> companies,
	List<String> inventoryNumbers, List<String> processors, List<String> storages, List<String> rams,
	List<String> modelYears)
{
	/** No filter at all: every device. */
	public static DeviceSearch all()
	{
		List<String> none = List.of();
		return new DeviceSearch(none, none, none, none, none, none, none, none, none, none, none, none, none);
	}

	public DeviceSearch withNames(List<String> values)
	{
		return new DeviceSearch(values, statuses, holders, serialNumbers, groups, models, conditions, companies,
			inventoryNumbers, processors, storages, rams, modelYears);
	}

	public DeviceSearch withStatuses(List<String> values)
	{
		return new DeviceSearch(names, values, holders, serialNumbers, groups, models, conditions, companies,
			inventoryNumbers, processors, storages, rams, modelYears);
	}

	public DeviceSearch withHolders(List<String> values)
	{
		return new DeviceSearch(names, statuses, values, serialNumbers, groups, models, conditions, companies,
			inventoryNumbers, processors, storages, rams, modelYears);
	}

	public DeviceSearch withConditions(List<String> values)
	{
		return new DeviceSearch(names, statuses, holders, serialNumbers, groups, models, values, companies,
			inventoryNumbers, processors, storages, rams, modelYears);
	}

	public DeviceSearch withModelYears(List<String> values)
	{
		return new DeviceSearch(names, statuses, holders, serialNumbers, groups, models, conditions, companies,
			inventoryNumbers, processors, storages, rams, values);
	}
}
