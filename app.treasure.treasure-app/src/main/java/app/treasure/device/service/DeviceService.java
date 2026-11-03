package app.treasure.device.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.device.domain.Device;
import app.treasure.device.repository.DeviceHistoryRepository;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.member.domain.Member;
import app.treasure.shared.repository.TagRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Creates and changes devices with their history entries, for the web form and
 * the MCP tools alike. The rules the form enforces in the browser (required
 * name, lengths, condition values, model year range) are checked here, because
 * an MCP client has no browser. Callers run inside a transaction.
 */
@ApplicationScoped
public class DeviceService
{
	private static final Logger LOG = LoggerFactory.getLogger(DeviceService.class);

	/**
	 * The values of the condition select in the device form; blank means "not
	 * recorded".
	 */
	public static final List<String> CONDITIONS = List.of("perfect", "lightly damaged", "heavily damaged", "broken");

	private static final int MIN_MODEL_YEAR = 1980;
	private static final int MAX_MODEL_YEAR = 2100;

	@Inject
	DeviceRepository deviceRepository;

	@Inject
	DeviceHistoryRepository deviceHistoryRepository;

	@Inject
	TagRepository tagRepository;

	/**
	 * Everything the device form edits. Status and holder change through
	 * assignment only.
	 */
	public record DeviceData(String name, String serialNumber, String inventoryNumber, String group, String model,
		String processor, String ram, String storage, Integer modelYear, LocalDate purchasedOn, String company,
		String condition, String location, String extraInfo)
	{
		/**
		 * The current values of the device, as a starting point for a partial
		 * change.
		 */
		public static DeviceData of(Device d)
		{
			return new DeviceData(d.getDeviceName(), d.getDeviceSerialNumber(), d.getDeviceNumber(), d.getGroup(),
				d.getDeviceModel(), d.getDeviceProzessor(), d.getDeviceRAM(), d.getDeviceHDDStorage(),
				d.getDeviceModelDate(), d.getDeviceAge(), d.getRegCompany(), d.getDeviceDamage(),
				d.getDeviceLocation(), d.getExtraInfo());
		}
	}

	/** One text field and the longest value the form accepts for it. */
	private record Limit(String label, Function<DeviceData, String> value, int max)
	{
	}

	private static final List<Limit> LIMITS = List.of(
		new Limit("Name", DeviceData::name, 50),
		new Limit("Serial number", DeviceData::serialNumber, 50),
		new Limit("Inventory number", DeviceData::inventoryNumber, 30),
		new Limit("Group", DeviceData::group, 50),
		new Limit("Model", DeviceData::model, 80),
		new Limit("Processor", DeviceData::processor, 30),
		new Limit("Memory", DeviceData::ram, 30),
		new Limit("Storage", DeviceData::storage, 30),
		new Limit("Owning company", DeviceData::company, 30),
		new Limit("Location", DeviceData::location, 500),
		new Limit("Extra info", DeviceData::extraInfo, 500));

	public Device create(DeviceData data, Member actor)
	{
		validate(data);

		Device device = new Device();
		device.setStatus("available");
		device.setCreatedOn(LocalDateTime.now());
		apply(device, data);
		deviceRepository.persist(device);
		rememberGroup(device.getGroup());

		String criteria = "deviceName=" + n(data.name())
			+ ", serial=" + n(data.serialNumber())
			+ ", group=" + n(device.getGroup())
			+ ", model=" + n(data.model())
			+ ", damage=" + n(data.condition())
			+ ", age=" + text(data.purchasedOn())
			+ ", company=" + n(data.company())
			+ ", number=" + n(data.inventoryNumber())
			+ ", prozessor=" + n(data.processor())
			+ ", hdd=" + n(data.storage())
			+ ", ram=" + n(data.ram())
			+ ", modelDate=" + text(data.modelYear())
			+ ", location=" + n(data.location())
			+ ", extraInfo=" + n(data.extraInfo());
		deviceHistoryRepository.record(device, actor, "CREATED", null, "", "", criteria);

		LOG.info("Created device: {} (id={})", data.name(), device.id);
		return device;
	}

	/**
	 * Replaces every form field of the device and records each value that
	 * changed.
	 */
	public void update(Device device, DeviceData data, Member actor)
	{
		validate(data);
		String group = normalizeGroup(data.group());

		recordIfChanged(device, actor, "deviceName", device.getDeviceName(), data.name());
		recordIfChanged(device, actor, "deviceSerialNumber", device.getDeviceSerialNumber(), data.serialNumber());
		recordIfChanged(device, actor, "group", device.getGroup(), group);
		recordIfChanged(device, actor, "deviceModel", device.getDeviceModel(), data.model());
		recordIfChanged(device, actor, "extraInfo", device.getExtraInfo(), data.extraInfo());
		recordIfChanged(device, actor, "deviceDamage", device.getDeviceDamage(), data.condition());
		recordIfChanged(device, actor, "deviceAge", text(device.getDeviceAge()), text(data.purchasedOn()));
		recordIfChanged(device, actor, "regCompany", device.getRegCompany(), data.company());
		recordIfChanged(device, actor, "deviceNumber", device.getDeviceNumber(), data.inventoryNumber());
		recordIfChanged(device, actor, "deviceProzessor", device.getDeviceProzessor(), data.processor());
		recordIfChanged(device, actor, "deviceHDDStorage", device.getDeviceHDDStorage(), data.storage());
		recordIfChanged(device, actor, "deviceRAM", device.getDeviceRAM(), data.ram());
		recordIfChanged(device, actor, "deviceModelDate", text(device.getDeviceModelDate()), text(data.modelYear()));
		recordIfChanged(device, actor, "deviceLocation", device.getDeviceLocation(), data.location());

		apply(device, data);
		rememberGroup(device.getGroup());
		LOG.info("Updated device: {} (id={})", data.name(), device.id);
	}

	private void validate(DeviceData data)
	{
		if (data.name() == null || data.name().codePoints().noneMatch(Character::isLetterOrDigit))
		{
			throw new DeviceValidationException("A device name is required.");
		}
		for (Limit limit : LIMITS)
		{
			String value = limit.value().apply(data);
			if (value != null && value.length() > limit.max())
			{
				throw new DeviceValidationException(
					limit.label() + " can be at most " + limit.max() + " characters long.");
			}
		}
		if (data.condition() != null && !data.condition().isBlank() && !CONDITIONS.contains(data.condition()))
		{
			throw new DeviceValidationException("Condition must be one of: " + String.join(", ", CONDITIONS) + ".");
		}
		if (data.modelYear() != null && (data.modelYear() < MIN_MODEL_YEAR || data.modelYear() > MAX_MODEL_YEAR))
		{
			throw new DeviceValidationException(
				"Model year must be between " + MIN_MODEL_YEAR + " and " + MAX_MODEL_YEAR + ".");
		}
	}

	private void apply(Device device, DeviceData data)
	{
		device.setDeviceName(data.name());
		device.setDeviceSerialNumber(data.serialNumber());
		device.setGroup(normalizeGroup(data.group()));
		device.setDeviceModel(data.model());
		device.setExtraInfo(data.extraInfo());
		device.setDeviceDamage(data.condition());
		device.setDeviceAge(data.purchasedOn());
		device.setRegCompany(data.company());
		device.setDeviceNumber(data.inventoryNumber());
		device.setDeviceProzessor(data.processor());
		device.setDeviceHDDStorage(data.storage());
		device.setDeviceRAM(data.ram());
		device.setDeviceModelDate(data.modelYear());
		device.setDeviceLocation(data.location());
	}

	private void recordIfChanged(Device device, Member actor, String field, String oldVal, String newVal)
	{
		String o = n(oldVal);
		String nn = n(newVal);
		if (!o.equals(nn))
		{
			deviceHistoryRepository.record(device, actor, "UPDATED", field, o, nn, null);
		}
	}

	/**
	 * Remembers a group name so it stays available after the last device using
	 * it is moved elsewhere.
	 */
	private void rememberGroup(String group)
	{
		if (group != null)
		{
			tagRepository.findOrCreate(group);
		}
	}

	/**
	 * Trims the group name; blank means "no group" and is stored as null, so
	 * the device and the Tag registry hold the same spelling.
	 */
	private static String normalizeGroup(String group)
	{
		return group == null || group.isBlank() ? null : group.trim();
	}

	/** Text for history entries: "" for null, otherwise the ISO form. */
	private static String text(Object value)
	{
		return value == null ? "" : value.toString();
	}

	private static String n(String s)
	{
		return s == null ? "" : s;
	}
}
