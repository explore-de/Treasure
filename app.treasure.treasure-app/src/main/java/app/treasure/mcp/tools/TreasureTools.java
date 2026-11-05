package app.treasure.mcp.tools;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.device.domain.Device;
import app.treasure.device.domain.DeviceHistory;
import app.treasure.device.repository.DeviceHistoryRepository;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.device.service.DeviceAssignmentService;
import app.treasure.device.service.DeviceService;
import app.treasure.device.service.DeviceService.DeviceData;
import app.treasure.device.service.DeviceValidationException;
import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import app.treasure.member.service.MemberValidationException;
import app.treasure.member.service.MemberService;
import app.treasure.shared.security.Roles;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.panache.common.Sort;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * The MCP tools of Treasure, served at /mcp. Every call runs as the member who
 * owns the API token, and assignments are recorded in the device history like
 * the ones made on the web pages.
 */
@Authenticated
@Transactional
public class TreasureTools
{
	private static final Logger LOG = LoggerFactory.getLogger(TreasureTools.class);

	private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final int DEFAULT_LIMIT = 50;
	private static final int DEFAULT_HISTORY_LIMIT = 20;
	private static final int OVERVIEW_LIST_LIMIT = 5;
	private static final String NO_GROUP = "No group";
	private static final String CONDITION_DESCRIPTION = "Condition: 'perfect', 'lightly damaged', "
		+ "'heavily damaged' or 'broken'";

	@Inject
	DeviceRepository deviceRepository;

	@Inject
	DeviceHistoryRepository deviceHistoryRepository;

	@Inject
	MemberRepository memberRepository;

	@Inject
	DeviceAssignmentService assignmentService;

	@Inject
	DeviceService deviceService;

	@Inject
	SecurityIdentity securityIdentity;

	@Inject
	MemberService memberService;

	/** One line of a device list. */
	public record DeviceSummary(long id, String name, String group, String status, String model,
		String serialNumber, String inventoryNumber, String assignedTo, String assignedToName,
		String assignedSince, long daysHeld)
	{
	}

	/** Everything stored about one device. */
	public record DeviceDetails(DeviceSummary summary, String condition, String owningCompany,
		String processor, String memory, String storage, Integer modelYear, String purchaseDate,
		String location, String extraInfo, String createdOn)
	{
	}

	public record HistoryEntry(String at, String actor, String event, String field, String oldValue,
		String newValue, String notes)
	{
	}

	public record HeldDevice(long id, String name, String since, long daysHeld)
	{
	}

	public record MemberHoldings(String username, String name, String email, String status,
		List<HeldDevice> devices)
	{
	}

	public record GroupCount(String group, long devices)
	{
	}

	/**
	 * Lists are wrapped, so a client always gets one JSON document, also when
	 * nothing matches.
	 */
	public record DeviceList(int count, List<DeviceSummary> devices)
	{
	}

	public record HistoryList(int count, List<HistoryEntry> entries)
	{
	}

	public record MemberList(int count, List<MemberHoldings> members)
	{
	}

	public record Overview(long total, long available, long assigned, List<GroupCount> groups,
		List<DeviceSummary> longestHeld, List<HistoryEntry> recentActivity)
	{
	}

	@Tool(name = "list_devices", description = "Lists devices, optionally filtered. All filters are "
		+ "case-insensitive and combined with AND. Use it to find a device id for the other tools.")
	DeviceList listDevices(
		@ToolArg(description = "Text contained in the name, serial number, inventory number or model", required = false) String search,
		@ToolArg(description = "'available' or 'not available'", required = false) String status,
		@ToolArg(description = "Device group, exact name, e.g. 'Laptops'", required = false) String group,
		@ToolArg(description = "Username or part of the name of the member holding the device", required = false) String assignedTo,
		@ToolArg(description = "Maximum number of devices to return, default 50", required = false) Integer limit)
	{
		Stream<Device> devices = deviceRepository.listAll(Sort.by("id").ascending()).stream();
		if (hasText(search))
		{
			devices = devices.filter(d -> contains(d.getDeviceName(), search)
				|| contains(d.getDeviceSerialNumber(), search)
				|| contains(d.getDeviceNumber(), search)
				|| contains(d.getDeviceModel(), search));
		}
		if (hasText(status))
		{
			devices = devices.filter(d -> status.trim().equalsIgnoreCase(d.getStatus()));
		}
		if (hasText(group))
		{
			devices = devices.filter(d -> group.trim().equalsIgnoreCase(trim(d.getGroup())));
		}
		if (hasText(assignedTo))
		{
			devices = devices.filter(heldBy(assignedTo));
		}
		List<DeviceSummary> found = devices.limit(limit != null && limit > 0 ? limit : DEFAULT_LIMIT)
			.map(this::summary).toList();
		return new DeviceList(found.size(), found);
	}

	@Tool(name = "get_device", description = "Shows every stored detail of one device.")
	DeviceDetails getDevice(@ToolArg(description = "Device id, see list_devices") long deviceId)
	{
		return details(findDevice(deviceId));
	}

	@Tool(name = "device_history", description = "Shows who changed what on a device and when, newest first.")
	HistoryList deviceHistory(
		@ToolArg(description = "Device id, see list_devices") long deviceId,
		@ToolArg(description = "Maximum number of entries, default 20", required = false) Integer limit)
	{
		findDevice(deviceId);
		List<HistoryEntry> entries = deviceHistoryRepository.forDevice(deviceId).stream()
			.limit(limit != null && limit > 0 ? limit : DEFAULT_HISTORY_LIMIT)
			.map(this::historyEntry)
			.toList();
		return new HistoryList(entries.size(), entries);
	}

	@Tool(name = "list_members", description = "Lists every member who can hold a device, with the devices "
		+ "each one holds and their status: 'In the system' or 'Never signed in'. The username is what "
		+ "assign_device expects.")
	MemberList listMembers()
	{
		List<MemberHoldings> result = new ArrayList<>();
		for (Member member : memberRepository.listByName())
		{
			result.add(holdings(member));
		}
		return new MemberList(result.size(), result);
	}

	@RolesAllowed(Roles.ADMIN)
	@Tool(name = "add_member", description = "Adds a person who has not signed in yet, so devices can be "
		+ "assigned to them right away. They show as 'Never signed in' until their first sign-in with this "
		+ "email. No email is sent. The username is the email in lower case.")
	MemberHoldings addMember(
		@ToolArg(description = "First name, up to 100 characters") String firstName,
		@ToolArg(description = "Last name, up to 100 characters") String lastName,
		@ToolArg(description = "Email of their company account, e.g. 'erika.mustermann@example.com'") String email)
	{
		Member member;
		try
		{
			member = memberService.add(firstName, lastName, email, actor());
		}
		catch (MemberValidationException e)
		{
			throw new ToolCallException(e.getMessage());
		}
		LOG.info("MCP: member {} added by {}", member.getUserName(), actorName());
		return holdings(member);
	}

	@Tool(name = "update_member", description = "Changes the name, email or phone of a member. Only the "
		+ "fields you pass change; an empty phone clears it. Admins can change every member, everyone else "
		+ "only themselves. Leave out the username to change yourself.")
	MemberHoldings updateMember(
		@ToolArg(description = "Username of the member, see list_members; default is you", required = false) String username,
		@ToolArg(description = "New first name, up to 100 characters", required = false) String firstName,
		@ToolArg(description = "New last name, up to 100 characters", required = false) String lastName,
		@ToolArg(description = "New email address", required = false) String email,
		@ToolArg(description = "New phone number, up to 50 characters", required = false) String phone)
	{
		boolean self = username == null || username.isBlank();
		Member member = self ? actor() : memberRepository.findByUsername(username.trim());
		if (member == null)
		{
			throw new ToolCallException(self ? "Your member profile does not exist yet."
				: "No member with username '" + username + "'. Use list_members to see the usernames.");
		}
		if (!memberService.mayEdit(member, securityIdentity))
		{
			throw new ToolCallException("You can only change your own details; changing other members needs "
				+ "the admin role.");
		}
		try
		{
			memberService.update(member,
				firstName != null ? firstName : member.getFirstName(),
				lastName != null ? lastName : member.getLastName(),
				email != null ? email : member.getEmail(),
				phone != null ? phone : member.getPhone(),
				actorName());
		}
		catch (MemberValidationException e)
		{
			throw new ToolCallException(e.getMessage());
		}
		LOG.info("MCP: member {} changed by {}", member.getUserName(), actorName());
		return holdings(member);
	}

	@Tool(name = "overview", description = "Inventory summary: device counts, devices per group, the "
		+ "devices held longest and the latest changes.")
	Overview overview()
	{
		List<Device> all = deviceRepository.listAll();
		long assigned = all.stream().filter(d -> d.getAssignedTo() != null).count();

		Map<String, Long> perGroup = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Device d : all)
		{
			String group = trim(d.getGroup()).isEmpty() ? NO_GROUP : trim(d.getGroup());
			perGroup.merge(group, 1L, Long::sum);
		}
		List<GroupCount> groups = perGroup.entrySet().stream()
			.map(e -> new GroupCount(e.getKey(), e.getValue()))
			.sorted(Comparator.comparingLong(GroupCount::devices).reversed().thenComparing(GroupCount::group))
			.toList();

		List<DeviceSummary> longestHeld = deviceRepository.findAssignedLongestFirst().stream()
			.limit(OVERVIEW_LIST_LIMIT).map(this::summary).toList();
		List<HistoryEntry> recent = deviceHistoryRepository.findRecent(OVERVIEW_LIST_LIMIT).stream()
			.map(this::historyEntry).toList();

		return new Overview(all.size(), all.size() - assigned, assigned, groups, longestHeld, recent);
	}

	@RolesAllowed(Roles.ADMIN)
	@Tool(name = "assign_device", description = "Hands a device to a member. The change is recorded in the "
		+ "device history under your name. A device held by someone else moves to the new member.")
	DeviceSummary assignDevice(
		@ToolArg(description = "Device id, see list_devices") long deviceId,
		@ToolArg(description = "Username of the member, see list_members") String username)
	{
		Device device = findDevice(deviceId);
		Member member = memberRepository.findByUsername(username == null ? null : username.trim());
		if (member == null)
		{
			throw new ToolCallException("No member with username '" + username + "'. Use list_members to see "
				+ "the usernames.");
		}
		if (assignmentService.assign(device, member, actor()))
		{
			LOG.info("MCP: device {} assigned to {} by {}", deviceId, member.getUserName(), actorName());
		}
		return summary(device);
	}

	@RolesAllowed(Roles.ADMIN)
	@Tool(name = "unassign_device", description = "Takes a device back from the member holding it, so it is "
		+ "available again. The change is recorded in the device history under your name.")
	DeviceSummary unassignDevice(@ToolArg(description = "Device id, see list_devices") long deviceId)
	{
		Device device = findDevice(deviceId);
		String holder = device.getBookedName();
		if (assignmentService.unassign(device, actor()))
		{
			LOG.info("MCP: device {} unassigned from {} by {}", deviceId, holder, actorName());
		}
		return summary(device);
	}

	@RolesAllowed(Roles.ADMIN)
	@Tool(name = "create_device", description = "Adds a new device to the inventory. It starts as available; "
		+ "use assign_device to hand it to someone. The creation is recorded in the device history under "
		+ "your name.")
	DeviceDetails createDevice(
		@ToolArg(description = "Device name, e.g. 'MacBook Pro 14'; up to 50 characters") String name,
		@ToolArg(description = "Device group, e.g. 'Laptops'", required = false) String group,
		@ToolArg(description = "Model, e.g. 'A2779'", required = false) String model,
		@ToolArg(description = "Serial number", required = false) String serialNumber,
		@ToolArg(description = "Inventory number, e.g. 'LT-001'", required = false) String inventoryNumber,
		@ToolArg(description = "Processor, e.g. 'M2 Pro'", required = false) String processor,
		@ToolArg(description = "Memory (RAM), e.g. '16 GB'", required = false) String memory,
		@ToolArg(description = "Storage, e.g. '1 TB'", required = false) String storage,
		@ToolArg(description = "Model year, 1980 to 2100", required = false) Integer modelYear,
		@ToolArg(description = "Purchase date as yyyy-MM-dd", required = false) String purchaseDate,
		@ToolArg(description = "Company that owns the device", required = false) String owningCompany,
		@ToolArg(description = CONDITION_DESCRIPTION, required = false) String condition,
		@ToolArg(description = "Where the device is kept", required = false) String location,
		@ToolArg(description = "Free-text notes", required = false) String extraInfo)
	{
		DeviceData data = new DeviceData(name, serialNumber, inventoryNumber, group, model, processor, memory,
			storage, modelYear, parseDate(purchaseDate, null), owningCompany, condition, location, extraInfo);
		Device device;
		try
		{
			device = deviceService.create(data, actor());
		}
		catch (DeviceValidationException e)
		{
			throw new ToolCallException(e.getMessage());
		}
		LOG.info("MCP: device {} created by {}", device.id, actorName());
		return details(device);
	}

	@RolesAllowed(Roles.ADMIN)
	@Tool(name = "update_device", description = "Changes details of a device. Only the fields you pass change; "
		+ "pass an empty string to clear a text field, modelYear 0 to clear the year. Who holds the device is "
		+ "changed with assign_device and unassign_device instead. Every changed field is recorded in the "
		+ "device history under your name.")
	DeviceDetails updateDevice(
		@ToolArg(description = "Device id, see list_devices") long deviceId,
		@ToolArg(description = "New name; up to 50 characters, cannot be cleared", required = false) String name,
		@ToolArg(description = "Device group", required = false) String group,
		@ToolArg(description = "Model", required = false) String model,
		@ToolArg(description = "Serial number", required = false) String serialNumber,
		@ToolArg(description = "Inventory number", required = false) String inventoryNumber,
		@ToolArg(description = "Processor", required = false) String processor,
		@ToolArg(description = "Memory (RAM)", required = false) String memory,
		@ToolArg(description = "Storage", required = false) String storage,
		@ToolArg(description = "Model year, 1980 to 2100; 0 clears it", required = false) Integer modelYear,
		@ToolArg(description = "Purchase date as yyyy-MM-dd; empty clears it", required = false) String purchaseDate,
		@ToolArg(description = "Company that owns the device", required = false) String owningCompany,
		@ToolArg(description = CONDITION_DESCRIPTION, required = false) String condition,
		@ToolArg(description = "Where the device is kept", required = false) String location,
		@ToolArg(description = "Free-text notes", required = false) String extraInfo)
	{
		Device device = findDevice(deviceId);
		DeviceData current = DeviceData.of(device);
		DeviceData changed = new DeviceData(
			keepOr(current.name(), name),
			keepOr(current.serialNumber(), serialNumber),
			keepOr(current.inventoryNumber(), inventoryNumber),
			keepOr(current.group(), group),
			keepOr(current.model(), model),
			keepOr(current.processor(), processor),
			keepOr(current.ram(), memory),
			keepOr(current.storage(), storage),
			modelYear == null ? current.modelYear() : (modelYear == 0 ? null : modelYear),
			parseDate(purchaseDate, current.purchasedOn()),
			keepOr(current.company(), owningCompany),
			keepOr(current.condition(), condition),
			keepOr(current.location(), location),
			keepOr(current.extraInfo(), extraInfo));
		try
		{
			deviceService.update(device, changed, actor());
		}
		catch (DeviceValidationException e)
		{
			throw new ToolCallException(e.getMessage());
		}
		LOG.info("MCP: device {} updated by {}", deviceId, actorName());
		return details(device);
	}

	private Device findDevice(long deviceId)
	{
		Device device = deviceRepository.findById(deviceId);
		if (device == null)
		{
			throw new ToolCallException("No device with id " + deviceId + ". Use list_devices to find it.");
		}
		return device;
	}

	/** The member with the devices they hold right now. */
	private MemberHoldings holdings(Member member)
	{
		List<HeldDevice> held = deviceRepository.findAssignedTo(member).stream()
			.map(d -> new HeldDevice(d.id, d.getDeviceName(), since(d), d.getDaysHeld()))
			.toList();
		return new MemberHoldings(member.getUserName(), member.getDisplayName(), member.getEmail(),
			member.getStatusLabel(), held);
	}

	private Member actor()
	{
		return memberRepository.findByUsername(actorName());
	}

	private String actorName()
	{
		return securityIdentity.getPrincipal().getName();
	}

	private DeviceSummary summary(Device d)
	{
		Member holder = d.getAssignedTo();
		return new DeviceSummary(d.id, d.getDeviceName(), d.getGroup(), d.getStatus(), d.getDeviceModel(),
			d.getDeviceSerialNumber(), d.getDeviceNumber(),
			holder != null ? holder.getUserName() : null,
			holder != null ? holder.getDisplayName() : null,
			since(d), d.getDaysHeld());
	}

	private DeviceDetails details(Device d)
	{
		return new DeviceDetails(summary(d), d.getDeviceDamage(), d.getRegCompany(), d.getDeviceProzessor(),
			d.getDeviceRAM(), d.getDeviceHDDStorage(), d.getDeviceModelDate(),
			d.getDeviceAge() != null ? d.getDeviceAge().toString() : null, d.getDeviceLocation(),
			d.getExtraInfo(), d.getCreatedOn() != null ? d.getCreatedOn().format(ISO) : null);
	}

	/**
	 * The current value when the argument was left out, otherwise the argument;
	 * an empty argument clears the field. DeviceService trims the text.
	 */
	private static String keepOr(String current, String given)
	{
		return given == null ? current : given;
	}

	/**
	 * Reads yyyy-MM-dd; null keeps the fallback, an empty value clears the
	 * date.
	 */
	private static LocalDate parseDate(String value, LocalDate fallback)
	{
		if (value == null)
		{
			return fallback;
		}
		if (value.isBlank())
		{
			return null;
		}
		try
		{
			return LocalDate.parse(value.trim());
		}
		catch (DateTimeParseException e)
		{
			throw new ToolCallException("Purchase date must be written as yyyy-MM-dd, e.g. 2024-03-15.");
		}
	}

	private HistoryEntry historyEntry(DeviceHistory h)
	{
		return new HistoryEntry(h.getHappenedAt() != null ? h.getHappenedAt().format(ISO) : null,
			h.getActorName(), h.getEventType(), h.getFieldName(), h.getOldValue(), h.getNewValue(), h.getNotes());
	}

	private static String since(Device d)
	{
		return d.getPickupTime() != null ? d.getPickupTime().format(ISO) : null;
	}

	private static Predicate<Device> heldBy(String who)
	{
		return d -> d.getAssignedTo() != null
			&& (who.trim().equalsIgnoreCase(d.getAssignedTo().getUserName())
				|| contains(d.getAssignedTo().getDisplayName(), who));
	}

	private static boolean contains(String haystack, String needle)
	{
		return haystack != null
			&& haystack.toLowerCase(Locale.ROOT).contains(needle.trim().toLowerCase(Locale.ROOT));
	}

	private static boolean hasText(String s)
	{
		return s != null && !s.isBlank();
	}

	private static String trim(String s)
	{
		return s == null ? "" : s.trim();
	}
}
