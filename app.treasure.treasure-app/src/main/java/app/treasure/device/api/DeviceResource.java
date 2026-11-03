package app.treasure.device.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.resteasy.reactive.RestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.device.domain.Device;
import app.treasure.device.domain.DeviceHistory;
import app.treasure.device.filter.DeviceFieldFilter;
import app.treasure.device.filter.DeviceFilter;
import app.treasure.device.repository.DeviceHistoryRepository;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.device.service.DeviceAssignmentService;
import app.treasure.device.service.DeviceService;
import app.treasure.device.service.DeviceService.DeviceData;
import app.treasure.device.service.DeviceValidationException;
import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import app.treasure.shared.repository.TagRepository;
import app.treasure.shared.util.FlashKeys;
import io.quarkiverse.renarde.Controller;
import io.quarkus.panache.common.Sort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

@Authenticated
@Path("/devices")
public class DeviceResource extends Controller
{

	private static final Logger LOG = LoggerFactory.getLogger(DeviceResource.class);

	@Inject
	DeviceRepository deviceRepository;

	@Inject
	DeviceHistoryRepository deviceHistoryRepository;

	@Inject
	SecurityIdentity securityIdentity;

	@Inject
	MemberRepository memberRepository;

	@Inject
	TagRepository tagRepository;

	@Inject
	DeviceAssignmentService assignmentService;

	@Inject
	DeviceService deviceService;

	@CheckedTemplate
	public static class Templates
	{
		private Templates()
		{
		}

		public static native TemplateInstance index(List<Device> devices, Member currentmember, List<Member> members);

		public static native TemplateInstance index$row(Device device, List<Member> members);

		public static native TemplateInstance create(Device device, List<String> groups);

		public static native TemplateInstance edit(Device device, List<String> groups, List<DeviceHistory> history);
	}

	/**
	 * Groups offered in the device form: every group ever saved (kept in the
	 * Tag table) plus any group still present on a device, so data written
	 * before the registry existed shows up too. Groups are shared by everyone.
	 */
	private List<String> loadKnownGroups()
	{
		Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		tagRepository.listAll().forEach(tag -> names.add(tag.getName()));
		deviceRepository.listAll().stream()
			.map(Device::getGroup)
			.filter(g -> g != null && !g.isBlank())
			.forEach(names::add);
		return new ArrayList<>(names);
	}

	private static final String INVALID_DATES_MESSAGE = "Purchased on must be a date and model year a four-digit year.";

	/** The two typed form fields; blank means "not recorded". */
	private record DateFields(LocalDate purchasedOn, Integer modelYear)
	{
	}

	/**
	 * Reads the date input (ISO yyyy-MM-dd) and the year input of the form.
	 * Returns null when a value cannot be read, so the caller can show an
	 * error.
	 */
	private DateFields parseDateFields(String deviceAge, String deviceModelDate)
	{
		try
		{
			LocalDate purchasedOn = deviceAge == null || deviceAge.isBlank() ? null : LocalDate.parse(deviceAge.trim());
			Integer modelYear = deviceModelDate == null || deviceModelDate.isBlank() ? null
				: Integer.valueOf(deviceModelDate.trim());
			return new DateFields(purchasedOn, modelYear);
		}
		catch (DateTimeParseException | NumberFormatException e)
		{
			return null;
		}
	}

	/** Text for history entries: "" for null, otherwise the ISO form. */
	private String text(Object value)
	{
		return value == null ? "" : value.toString();
	}

	private Member currentMember()
	{
		String username = securityIdentity.getPrincipal().getName();
		return memberRepository.findByUsername(username);
	}

	@GET
	@Path("")
	public TemplateInstance index(
		@QueryParam("searchName") String searchName,
		@QueryParam("name") List<String> names,
		@QueryParam("status") List<String> statuses,
		@QueryParam("assignedTo") List<String> assignedTo,
		@QueryParam("serial") List<String> serials,
		@QueryParam("group") List<String> groups,
		@QueryParam("model") List<String> models,
		@QueryParam("damage") List<String> damages,
		@QueryParam("company") List<String> companys,
		@QueryParam("deviceNumber") List<String> numbers,
		@QueryParam("deviceProzessor") List<String> prozessors,
		@QueryParam("deviceHDDStorage") List<String> hddStorages,
		@QueryParam("deviceRAM") List<String> rams,
		@QueryParam("deviceModelDate") List<String> modelDates)
	{

		List<String> nameTerms = normalize(names);
		if ((nameTerms == null || nameTerms.isEmpty()) && searchName != null && !searchName.isBlank())
		{
			nameTerms = List.of(searchName.trim());
		}

		List<String> st = normalize(statuses);
		List<String> bb = normalize(assignedTo);
		List<String> se = normalize(serials);
		List<String> gr = normalize(groups);
		List<String> mo = normalize(models);
		List<String> da = normalize(damages);
		List<String> cp = normalize(companys);
		List<String> nb = normalize(numbers);
		List<String> pz = normalize(prozessors);
		List<String> hd = normalize(hddStorages);
		List<String> rm = normalize(rams);
		List<String> md = normalize(modelDates);

		List<Device> all = deviceRepository.listAll(Sort.by("id").ascending());
		List<DeviceFilter> filters = List.<DeviceFilter> of(
			new DeviceFieldFilter(nameTerms, Device::getDeviceName, this::containsIgnoreCase),
			new DeviceFieldFilter(st, Device::getStatus, this::equalsIgnoreCase),
			new DeviceFieldFilter(bb, Device::getBookedName, this::containsIgnoreCase),
			new DeviceFieldFilter(se, Device::getDeviceSerialNumber, this::containsIgnoreCase),
			new DeviceFieldFilter(gr, Device::getGroup, this::containsIgnoreCase),
			new DeviceFieldFilter(mo, Device::getDeviceModel, this::containsIgnoreCase),
			new DeviceFieldFilter(da, Device::getDeviceDamage, this::equalsIgnoreCase),
			new DeviceFieldFilter(cp, Device::getRegCompany, this::containsIgnoreCase),
			new DeviceFieldFilter(nb, Device::getDeviceNumber, this::containsIgnoreCase),
			new DeviceFieldFilter(pz, Device::getDeviceProzessor, this::containsIgnoreCase),
			new DeviceFieldFilter(hd, Device::getDeviceHDDStorage, this::containsIgnoreCase),
			new DeviceFieldFilter(rm, Device::getDeviceRAM, this::containsIgnoreCase),
			new DeviceFieldFilter(md, d -> text(d.getDeviceModelDate()), this::containsIgnoreCase));
		List<Device> filtered = all.stream()
			.filter(d -> filters.stream().allMatch(f -> f.matches(d)))
			.toList();

		String username = securityIdentity.getPrincipal().getName();
		Member currentmember = memberRepository.findByUsername(username);

		return Templates.index(filtered, currentmember, memberRepository.listAll());
	}

	private List<String> normalize(List<String> in)
	{
		if (in == null) return List.of();
		return in.stream()
			.map(s -> s == null ? "" : s.trim())
			.filter(s -> !s.isBlank())
			.toList();
	}

	private boolean containsIgnoreCase(String haystack, String needle)
	{
		if (haystack == null || needle == null) return false;
		return haystack.toLowerCase().contains(needle.toLowerCase());
	}

	private boolean equalsIgnoreCase(String a, String b)
	{
		if (a == null || b == null) return false;
		return a.equalsIgnoreCase(b);
	}

	@GET
	@Path("/new")
	public TemplateInstance create()
	{
		return Templates.create(new Device(), loadKnownGroups());
	}

	@GET
	@Path("/{id}/edit")
	public TemplateInstance edit(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);
		if (device == null)
		{
			throw new NotFoundException();
		}
		List<DeviceHistory> history = deviceHistoryRepository.forDevice(id);
		return Templates.edit(device, loadKnownGroups(), history);
	}

	@POST
	@Path("/create")
	@Transactional
	public void save(
		@RestForm String deviceName,
		@RestForm String deviceSerialNumber,
		@RestForm String group,
		@RestForm String deviceModel,
		@RestForm String extraInfo,
		@RestForm String deviceDamage,
		@RestForm String deviceAge,
		@RestForm String regCompany,
		@RestForm String deviceNumber,
		@RestForm String deviceProzessor,
		@RestForm String deviceHDDStorage,
		@RestForm String deviceRAM,
		@RestForm String deviceModelDate,
		@RestForm String deviceLocation)
	{
		DateFields dates = parseDateFields(deviceAge, deviceModelDate);
		if (dates == null)
		{
			flash(FlashKeys.ERROR, INVALID_DATES_MESSAGE);
			seeOther("/devices/new");
			return;
		}
		DeviceData data = new DeviceData(deviceName, deviceSerialNumber, deviceNumber, group, deviceModel,
			deviceProzessor, deviceRAM, deviceHDDStorage, dates.modelYear(), dates.purchasedOn(), regCompany,
			deviceDamage, deviceLocation, extraInfo);
		try
		{
			deviceService.create(data, currentMember());
		}
		catch (DeviceValidationException e)
		{
			flash(FlashKeys.ERROR, e.getMessage());
			seeOther("/devices/new");
			return;
		}
		flash(FlashKeys.SUCCESS, "Device \"" + deviceName + "\" was created.");
		seeOther("/devices");
	}

	@POST
	@Path("/{id}/update")
	@Transactional
	public void update(
		@PathParam("id") Long id,
		@RestForm String deviceName,
		@RestForm String deviceSerialNumber,
		@RestForm String group,
		@RestForm String deviceModel,
		@RestForm String extraInfo,
		@RestForm String deviceDamage,
		@RestForm String deviceAge,
		@RestForm String regCompany,
		@RestForm String deviceNumber,
		@RestForm String deviceProzessor,
		@RestForm String deviceHDDStorage,
		@RestForm String deviceRAM,
		@RestForm String deviceModelDate,
		@RestForm String deviceLocation)
	{
		DateFields dates = parseDateFields(deviceAge, deviceModelDate);
		if (dates == null)
		{
			flash(FlashKeys.ERROR, INVALID_DATES_MESSAGE);
			seeOther("/devices/" + id + "/edit");
			return;
		}

		Device device = deviceRepository.findById(id);
		if (device == null)
		{
			throw new NotFoundException();
		}
		DeviceData data = new DeviceData(deviceName, deviceSerialNumber, deviceNumber, group, deviceModel,
			deviceProzessor, deviceRAM, deviceHDDStorage, dates.modelYear(), dates.purchasedOn(), regCompany,
			deviceDamage, deviceLocation, extraInfo);
		try
		{
			deviceService.update(device, data, currentMember());
		}
		catch (DeviceValidationException e)
		{
			flash(FlashKeys.ERROR, e.getMessage());
			seeOther("/devices/" + id + "/edit");
			return;
		}
		flash(FlashKeys.SUCCESS, "Changes to \"" + deviceName + "\" were saved.");
		seeOther("/devices");
	}

	@POST
	@Path("/{id}/delete")
	@Transactional
	public void delete(
		@PathParam("id") Long id,
		@RestForm String redirectUrl)
	{
		Device device = deviceRepository.findById(id);
		if (device != null)
		{
			String name = device.getDeviceName();
			device.delete();
			LOG.info("Deleted device: {} (id={})", name, id);
			flash(FlashKeys.SUCCESS, "Device \"" + name + "\" was deleted.");
		}
		seeOther(safeRedirect(redirectUrl));
	}

	@POST
	@Path("/delete-many")
	@Transactional
	public void deleteMany(
		@RestForm String ids,
		@RestForm String redirectUrl)
	{
		int deleted = 0;
		for (Long id : parseIds(ids))
		{
			Device device = deviceRepository.findById(id);
			if (device != null)
			{
				device.delete();
				deleted++;
			}
		}
		LOG.info("Deleted {} devices", deleted);
		flash(FlashKeys.SUCCESS, countOf(deleted, "device") + " deleted.");
		seeOther(safeRedirect(redirectUrl));
	}

	@POST
	@Path("/assign-many")
	@Transactional
	public void assignMany(
		@RestForm String ids,
		@RestForm String assignedTo,
		@RestForm String redirectUrl)
	{
		Member member = memberRepository.findByUsername(assignedTo);
		if (member == null)
		{
			flash(FlashKeys.ERROR, "The selected member does not exist.");
			seeOther(safeRedirect(redirectUrl));
			return;
		}

		Member actor = currentMember();
		int assigned = 0;

		for (Long id : parseIds(ids))
		{
			Device device = deviceRepository.findById(id);
			if (device != null && assignmentService.assign(device, member, actor))
			{
				assigned++;
			}
		}
		LOG.info("Assigned {} devices to {}", assigned, member.getUserName());
		flash(FlashKeys.SUCCESS, countOf(assigned, "device") + " assigned to " + member.getDisplayName() + ".");
		seeOther(safeRedirect(redirectUrl));
	}

	@POST
	@Path("/unassign-many")
	@Transactional
	public void unassignMany(
		@RestForm String ids,
		@RestForm String redirectUrl)
	{
		Member actor = currentMember();
		int unassigned = 0;

		for (Long id : parseIds(ids))
		{
			Device device = deviceRepository.findById(id);
			if (device == null) continue;

			String oldBooked = device.getBookedName();
			if (assignmentService.unassign(device, actor))
			{
				unassigned++;
				LOG.info("Unassigned device {} from member {}", id, oldBooked);
			}
		}
		flash(FlashKeys.SUCCESS, countOf(unassigned, "device") + " unassigned.");
		seeOther(safeRedirect(redirectUrl));
	}

	@POST
	@Path("/{id}/assign")
	@Transactional
	public TemplateInstance assign(
		@PathParam("id") Long id,
		@RestForm String assignedTo)
	{
		Device device = deviceRepository.findById(id);
		if (device == null)
		{
			throw new NotFoundException();
		}
		Member member = memberRepository.findByUsername(assignedTo);
		if (member == null)
		{
			LOG.warn("Refused to assign device {} to unknown member '{}'", id, assignedTo);
			throw new BadRequestException("Unknown member: " + assignedTo);
		}

		if (assignmentService.assign(device, member, currentMember()))
		{
			LOG.info("Device {} assigned to {}", id, member.getUserName());
		}

		return Templates.index$row(device, memberRepository.listAll());
	}

	@POST
	@Path("/{id}/unassign")
	@Transactional
	public TemplateInstance unassign(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);
		if (device == null)
		{
			throw new NotFoundException();
		}

		String oldBooked = device.getBookedName();
		if (assignmentService.unassign(device, currentMember()))
		{
			LOG.info("Unassigned device {} from member {}", id, oldBooked);
		}

		return Templates.index$row(device, memberRepository.listAll());
	}

	private String countOf(int count, String noun)
	{
		return count + " " + noun + (count == 1 ? "" : "s");
	}

	private String safeRedirect(String redirectUrl)
	{
		if (redirectUrl == null || redirectUrl.isBlank())
		{
			return "/devices";
		}
		// only paths on this site: must start with a single slash
		if (!redirectUrl.startsWith("/") || redirectUrl.startsWith("//") || redirectUrl.startsWith("/\\"))
		{
			return "/devices";
		}
		return redirectUrl;
	}

	private List<Long> parseIds(String idsCsv)
	{
		if (idsCsv == null || idsCsv.isBlank())
		{
			return Collections.emptyList();
		}
		return Arrays.stream(idsCsv.split(","))
			.map(String::trim)
			.filter(s -> !s.isBlank())
			.map(Long::valueOf)
			.distinct()
			.toList();
	}

}