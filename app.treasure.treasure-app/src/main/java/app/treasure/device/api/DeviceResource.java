
package app.treasure.device.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import io.quarkus.panache.common.Sort;
import org.jboss.resteasy.reactive.RestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.device.domain.Device;
import app.treasure.device.domain.DeviceHistory;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.device.repository.DeviceHistoryRepository;
import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;



@Authenticated
@Path("/devices")
public class DeviceResource extends Controller {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceResource.class);

	@Inject
	DeviceRepository deviceRepository;

	@Inject
	DeviceHistoryRepository deviceHistoryRepository;

	@Inject
	SecurityIdentity securityIdentity;

	@Inject
	MemberRepository memberRepository;

	@CheckedTemplate
	public static class Templates {
		private Templates() {}
		public static native TemplateInstance index(List<Device> devices, Member currentmember, List<Member> members);
		public static native TemplateInstance create(List<String> groups);

		public static native TemplateInstance edit(Device device, List<String> groups, List<DeviceHistory> history);
	}

	private List<String> loadKnownGroups() {
		return deviceRepository.listAll().stream()
				.map(Device::getGroup)
				.filter(g -> g != null && !g.isBlank())
				.distinct()
				.sorted()
				.toList();
	}

	private Member currentMember() {
		String username = securityIdentity.getPrincipal().getName();
		return memberRepository.findByUsername(username);
	}

	private String n(String s) {
		return s == null ? "" : s;
	}

	private void recordChange(Device device, Member actor,
	                          String eventType, String field,
	                          String oldVal, String newVal,
	                          String notes) {
		DeviceHistory h = new DeviceHistory();
		h.setDevice(device);
		h.setActor(actor);
		h.setHappenedAt(LocalDateTime.now());
		h.setEventType(eventType);
		h.setFieldName(field);
		h.setOldValue(oldVal);
		h.setNewValue(newVal);
		h.setNotes(notes);
		deviceHistoryRepository.persist(h);
	}

	private void recordIfChanged(Device device, Member actor, String field, String oldVal, String newVal) {
		String o = n(oldVal);
		String nn = n(newVal);
		if (!o.equals(nn)) {
			recordChange(device, actor, "UPDATED", field, o, nn, null);
		}
	}


	@GET
	@Path("")
	public TemplateInstance index(
		@QueryParam("searchName") String searchName,
		@QueryParam("name") List<String> names,
		@QueryParam("status") List<String> statuses,
		@QueryParam("bookedBy") List<String> bookedBy,
		@QueryParam("serial") List<String> serials,
		@QueryParam("group") List<String> groups,
		@QueryParam("model") List<String> models,
		@QueryParam("damage") List<String> damages,

		@QueryParam("company") List<String> companys,
		@QueryParam("number") List<String> numbers,
		@QueryParam("prozessor") List<String> prozessors,
		@QueryParam("hddStorage") List<String> hddStorages,
		@QueryParam("ram") List<String> rams,
		@QueryParam("modelDate") List<String> modelDates)
	{

		List<String> nameTerms = normalize(names);
		if ((nameTerms == null || nameTerms.isEmpty()) && searchName != null && !searchName.isBlank())
		{
			nameTerms = List.of(searchName.trim());
		}

		List<String> st = normalize(statuses);
		List<String> bb = normalize(bookedBy);
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
		List<String> finalNameTerms = nameTerms;
		List<Device> filtered = all.stream()
			.filter(d -> matches(d, finalNameTerms, st, bb, se, gr, mo, da, cp, nb, pz, hd, rm, md))
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

	private boolean matches(Device d,
		List<String> nameFallback,
		List<String> statuses,
		List<String> bookedBy,
		List<String> serials,
		List<String> groups,
		List<String> models,
		List<String> damages,
		List<String> company,
		List<String> number,
		List<String> prozessor,
		List<String> hddStorage,
		List<String> ram,
		List<String> modelDate

	)
	{

		boolean nameOk = nameFallback.isEmpty() ||
			nameFallback.stream().anyMatch(t -> containsIgnoreCase(d.getDeviceName(), t));
		boolean statusOk = statuses.isEmpty() ||
			statuses.stream().anyMatch(t -> equalsIgnoreCase(d.getStatus(), t));
		boolean bookedOk = bookedBy.isEmpty() ||
			bookedBy.stream().anyMatch(t -> containsIgnoreCase(d.getBookedName(), t));
		boolean serialOk = serials.isEmpty() ||
			serials.stream().anyMatch(t -> containsIgnoreCase(d.getDeviceSerialNumber(), t));
		boolean groupOk = groups.isEmpty() ||
			groups.stream().anyMatch(t -> containsIgnoreCase(d.getGroup(), t));
		boolean modelOk = models.isEmpty() ||
			models.stream().anyMatch(t -> containsIgnoreCase(d.getDeviceModel(), t));
		boolean damageOk = damages.isEmpty() ||
			damages.stream().anyMatch(t -> equalsIgnoreCase(d.getDeviceDamage(), t));
		boolean companyOk = company.isEmpty() ||
			company.stream().anyMatch(t -> containsIgnoreCase(d.getRegCompany(), t));
		boolean numberOk = number.isEmpty() ||
			number.stream().anyMatch(t -> containsIgnoreCase(d.getDeviceNumber(), t));
		boolean prozessorOk = prozessor.isEmpty() ||
			prozessor.stream().anyMatch(t -> containsIgnoreCase(d.getDeviceProzessor(), t));
		boolean hddStorageOk = hddStorage.isEmpty() ||
			hddStorage.stream().anyMatch(t -> containsIgnoreCase(d.getDeviceHDDStorage(), t));
		boolean ramOk = ram.isEmpty() ||
			ram.stream().anyMatch(t -> containsIgnoreCase(d.getDeviceRAM(), t));
		boolean modelDateOk = modelDate.isEmpty() ||
			modelDate.stream().anyMatch(t -> containsIgnoreCase(d.getDeviceModelDate(), t));

		return nameOk && statusOk && bookedOk && serialOk && groupOk && modelOk && damageOk && companyOk && numberOk && prozessorOk && hddStorageOk && ramOk && modelDateOk;
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
		return Templates.create(loadKnownGroups());
	}


	@GET
	@Path("/{id}/edit")
	public TemplateInstance edit(@PathParam("id") Long id) {
		Device device = deviceRepository.findById(id);
		List<DeviceHistory> history = deviceHistoryRepository.forDevice(id);
		return Templates.edit(device, loadKnownGroups(), history);
	}


	@POST
	@Path("/{id}/search")
	@Transactional
	public void search(
		@PathParam("id") Long id,
		@RestForm String searchName)
	{
		{
			String query = (searchName == null) ? "" : searchName.trim();
			Device device = deviceRepository.findById(id);

			if (query.isEmpty())
			{
				device.setVisible(true);
			}
			else
			{
				boolean visible = device.getVisible();
				if (visible)
				{
					String name = device.getDeviceName();
					if (name != null && name.toLowerCase().contains(query.toLowerCase()))
					{
						device.setVisible(true);
					}
					else
					{
						device.setVisible(false);
					}
				}
				else
				{
					device.setVisible(false);
				}
			}
		}

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
			@RestForm String deviceLocation
	) {
		if (deviceName != null && deviceName.matches(".*[a-zA-Z0-9а-яА-Я].*")) {
			Device device = new Device();
			device.setDeviceName(deviceName);
			device.setDeviceSerialNumber(deviceSerialNumber);
			device.setStatus("available");
			device.setCreatedOn(String.valueOf(LocalDateTime.now()));
			device.setGroup(group);
			device.setDeviceModel(deviceModel);
			device.setExtraInfo(extraInfo);
			device.setDeviceDamage(deviceDamage);
			device.setDeviceAge(deviceAge);
			device.setRegCompany(regCompany);
			device.setDeviceNumber(deviceNumber);
			device.setDeviceProzessor(deviceProzessor);
			device.setDeviceHDDStorage(deviceHDDStorage);
			device.setDeviceRAM(deviceRAM);
			device.setDeviceModelDate(deviceModelDate);
			device.setDeviceLocation(deviceLocation);

			deviceRepository.persist(device);

			Member actor = currentMember();
			String criteria = "deviceName=" + n(deviceName)
					+ ", serial=" + n(deviceSerialNumber)
					+ ", group=" + n(group)
					+ ", model=" + n(deviceModel)
					+ ", damage=" + n(deviceDamage)
					+ ", age=" + n(deviceAge)
					+ ", company=" + n(regCompany)
					+ ", number=" + n(deviceNumber)
					+ ", prozessor=" + n(deviceProzessor)
					+ ", hdd=" + n(deviceHDDStorage)
					+ ", ram=" + n(deviceRAM)
					+ ", modelDate=" + n(deviceModelDate)
					+ ", location=" + n(deviceLocation)
					+ ", extraInfo=" + n(extraInfo);

			recordChange(device, actor, "CREATED", null, "", "", criteria);
		}
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
			@RestForm String deviceLocation
	) {
		if (deviceName == null || !deviceName.matches(".*[a-zA-Z0-9а-яА-Я].*")) {
			seeOther("/devices");
			return;
		}

		Device device = deviceRepository.findById(id);
		Member actor = currentMember();

		recordIfChanged(device, actor, "deviceName", device.getDeviceName(), deviceName);
		recordIfChanged(device, actor, "deviceSerialNumber", device.getDeviceSerialNumber(), deviceSerialNumber);
		recordIfChanged(device, actor, "group", device.getGroup(), group);
		recordIfChanged(device, actor, "deviceModel", device.getDeviceModel(), deviceModel);
		recordIfChanged(device, actor, "extraInfo", device.getExtraInfo(), extraInfo);
		recordIfChanged(device, actor, "deviceDamage", device.getDeviceDamage(), deviceDamage);
		recordIfChanged(device, actor, "deviceAge", device.getDeviceAge(), deviceAge);
		recordIfChanged(device, actor, "regCompany", device.getRegCompany(), regCompany);
		recordIfChanged(device, actor, "deviceNumber", device.getDeviceNumber(), deviceNumber);
		recordIfChanged(device, actor, "deviceProzessor", device.getDeviceProzessor(), deviceProzessor);
		recordIfChanged(device, actor, "deviceHDDStorage", device.getDeviceHDDStorage(), deviceHDDStorage);
		recordIfChanged(device, actor, "deviceRAM", device.getDeviceRAM(), deviceRAM);
		recordIfChanged(device, actor, "deviceModelDate", device.getDeviceModelDate(), deviceModelDate);
		recordIfChanged(device, actor, "deviceLocation", device.getDeviceLocation(), deviceLocation);

		device.setDeviceName(deviceName);
		device.setDeviceSerialNumber(deviceSerialNumber);
		device.setGroup(group);
		device.setDeviceModel(deviceModel);
		device.setExtraInfo(extraInfo);
		device.setDeviceDamage(deviceDamage);
		device.setDeviceAge(deviceAge);
		device.setRegCompany(regCompany);
		device.setDeviceNumber(deviceNumber);
		device.setDeviceProzessor(deviceProzessor);
		device.setDeviceHDDStorage(deviceHDDStorage);
		device.setDeviceRAM(deviceRAM);
		device.setDeviceModelDate(deviceModelDate);
		device.setDeviceLocation(deviceLocation);

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
		device.delete();
		seeOther(safeRedirect(redirectUrl));
	}

	@POST
	@Path("/delete-many")
	@Transactional
	public void deleteMany(
			@RestForm String ids,
			@RestForm String redirectUrl) {

		for (Long id : parseIds(ids)) {
			Device device = deviceRepository.findById(id);
			if (device != null) {
				device.delete();
			}
		}
		seeOther(safeRedirect(redirectUrl));
	}


	@POST
	@Path("/assign-many")
	@Transactional
	public void assignMany(
			@RestForm String ids,
			@RestForm String bookedBy,
			@RestForm String redirectUrl
	) {
		Member member = memberRepository.findByUsername(bookedBy);
		if (member == null) {
			seeOther(safeRedirect(redirectUrl));
			return;
		}

		Member actor = currentMember();

		for (Long id : parseIds(ids)) {
			Device device = deviceRepository.findById(id);
			if (device == null) continue;
			String oldBooked = device.getBookedName();
			String oldStatus = n(device.getStatus());
			String oldPickup = device.getPickupTime() != null ? device.getPickupTime().toString() : "";

			if (device.getBookedBy() != null && device.getBookedBy().equals(member)) {
				device.setBookedBy(null);
				device.setStatus("available");
				device.setPickupTime(null);
			} else {
				device.setBookedBy(member);
				device.setStatus("not available");
				device.setPickupTime(LocalDateTime.now());
			}
			String newBooked = device.getBookedName();
			String newStatus = n(device.getStatus());
			String newPickup = device.getPickupTime() != null ? device.getPickupTime().toString() : "";
			String type = (newBooked == null || newBooked.isBlank()) ? "UNASSIGNED" : "ASSIGNED";
			String notes = "status: " + oldStatus + " -> " + newStatus + ", pickupTime: " + oldPickup + " -> " + newPickup;

			recordChange(device, actor, type, "bookedBy", n(oldBooked), n(newBooked), notes);
		}
		seeOther(safeRedirect(redirectUrl));
	}


	@POST
	@Path("/{id}/assign")
	@Transactional
	public void assign(
			@PathParam("id") Long id,
			@RestForm String bookedBy,
			@RestForm String redirectUrl
	) {
		Device device = deviceRepository.findById(id);
		Member actor = currentMember();
		String oldBooked = device.getBookedName();
		String oldStatus = n(device.getStatus());
		String oldPickup = device.getPickupTime() != null ? device.getPickupTime().toString() : "";
		Member member = memberRepository.findByUsername(bookedBy);

		if (device.getBookedBy() != null && device.getBookedBy().equals(member)) {
			device.setBookedBy(null);
			device.setStatus("available");
			device.setPickupTime(null);
		} else {
			device.setBookedBy(member);
			device.setStatus("not available");
			device.setPickupTime(LocalDateTime.now());
		}
		String newBooked = device.getBookedName();
		String newStatus = n(device.getStatus());
		String newPickup = device.getPickupTime() != null ? device.getPickupTime().toString() : "";

		String type = (newBooked == null || newBooked.isBlank()) ? "UNASSIGNED" : "ASSIGNED";
		String notes = "status: " + oldStatus + " -> " + newStatus + ", pickupTime: " + oldPickup + " -> " + newPickup;
		recordChange(device, actor, type, "bookedBy", n(oldBooked), n(newBooked), notes);

		seeOther(safeRedirect(redirectUrl));
	}


	private String safeRedirect(String redirectUrl)
	{
		if (redirectUrl == null || redirectUrl.isBlank())
		{
			return "/devices";
		}
		if (!redirectUrl.startsWith("/devices"))
		{
			return "/devices";
		}
		return redirectUrl;
	}

	private List<Long> parseIds(String idsCsv) {
		if (idsCsv == null || idsCsv.isBlank()) {
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