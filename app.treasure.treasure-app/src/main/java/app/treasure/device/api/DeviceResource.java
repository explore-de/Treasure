package app.treasure.device.api;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.panache.common.Sort;
import org.jboss.resteasy.reactive.RestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.device.domain.Device;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
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
	SecurityIdentity securityIdentity;

	@Inject
	MemberRepository memberRepository;

	@Inject
	SecurityIdentity identity;

	@CheckedTemplate
	public static class Templates
	{
		private Templates()
		{
		}

		public static native TemplateInstance index(List<Device> devices, Member currentmember, List<Member> members, String query);

		public static native TemplateInstance create(List<String> groups);

		public static native TemplateInstance edit(Device device, List<String> groups);
	}

	private List<String> loadKnownGroups()
	{
		return deviceRepository.listAll().stream()
			.map(Device::getGroup)
			.filter(g -> g != null && !g.isBlank())
			.distinct()
			.sorted()
			.toList();
	}

	private void redirectToIndex() {
		redirect(DeviceResource.class).index(null, null, null, null, null, null, null);
	}

	@GET
	@Path("")
	public TemplateInstance index(
		@QueryParam("query") String query,
		@QueryParam("status") List<String> statuses,
		@QueryParam("bookedBy") List<String> bookedBy,
		@QueryParam("serial") List<String> serials,
		@QueryParam("group") List<String> groups,
		@QueryParam("model") List<String> models,
		@QueryParam("damage") List<String> damages)
	{
		List<String> nameTerms = (query != null && !query.isBlank()) ? List.of(query.trim()) : List.of();
		List<String> st = normalize(statuses);
		List<String> bb = normalize(bookedBy);
		List<String> se = normalize(serials);
		List<String> gr = normalize(groups);
		List<String> mo = normalize(models);
		List<String> da = normalize(damages);

		List<Device> all = deviceRepository.listAll(Sort.by("id").ascending());
		List<Device> filtered = all.stream()
			.filter(d -> matches(d, nameTerms, st, bb, se, gr, mo, da))
			.toList();

		String username = securityIdentity.getPrincipal().getName();
		Member currentmember = memberRepository.findByUsername(username);
		return Templates.index(filtered, currentmember, memberRepository.listAll(), query);
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
		List<String> names,
		List<String> statuses,
		List<String> bookedBy,
		List<String> serials,
		List<String> groups,
		List<String> models,
		List<String> damages)
	{
		boolean nameOk = names.isEmpty() ||
			names.stream().anyMatch(t -> containsIgnoreCase(d.getDeviceName(), t));
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
		return nameOk && statusOk && bookedOk && serialOk && groupOk && modelOk && damageOk;
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
	public TemplateInstance edit(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);
		return Templates.edit(device, loadKnownGroups());
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

		// ✅ neue Felder
		@RestForm String group,
		@RestForm String deviceModel,
		@RestForm String extraInfo,
		@RestForm String deviceDamage,
		@RestForm String deviceAge)
	{
		if (deviceName != null && deviceName.matches(".*[a-zA-Z0-9а-яА-Я].*"))
		{
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

			deviceRepository.persist(device);
		}
		redirectToIndex();
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
		@RestForm String deviceAge)
	{
		if (deviceName == null || !deviceName.matches(".*[a-zA-Z0-9а-яА-Я].*"))
		{
			redirectToIndex();
			return;
		}

		Device device = deviceRepository.findById(id);

		device.setDeviceName(deviceName);
		device.setDeviceSerialNumber(deviceSerialNumber);

		device.setGroup(group);
		device.setDeviceModel(deviceModel);
		device.setExtraInfo(extraInfo);
		device.setDeviceDamage(deviceDamage);
		device.setDeviceAge(deviceAge);

		redirectToIndex();
	}

	@POST
	@Path("/{id}/delete")
	@Transactional
	public void delete(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);
		device.delete();
		redirectToIndex();
	}

	@POST
	@Path("/delete-many")
	@Transactional
	public void deleteMany(@RestForm List<Long> ids){
		for (Long id : ids){
			Device device = deviceRepository.findById(id);
			device.delete();
		};
		redirectToIndex(); // this thing saves everything to index
	}

	@POST
	@Path("/assign-many")
	@Transactional
	public void assignMany(@RestForm List<Long> ids, @RestForm String bookedBy){
		Member member = memberRepository.findByUsername(bookedBy);
		for (Long id : ids){
			Device device = deviceRepository.findById(id);
			if (device.getBookedBy() == member)
			{
				device.setBookedBy(null);
				device.setStatus("available");
				device.setPickupTime(null);
				redirectToIndex();
			}
			else {
				device.setBookedBy(member);
				device.setStatus("not available");
				device.setPickupTime(LocalDateTime.now());
				redirectToIndex();
			}
		}
	}


	@POST
	@Path("/{id}/assign")
	@Transactional
	public void assign(@PathParam("id") Long id, @RestForm String bookedBy)
	{
		Device device = deviceRepository.findById(id);
		Member member = memberRepository.findByUsername(bookedBy);

		if (device.getBookedBy() == member)
		{
			device.setBookedBy(null);
			device.setStatus("available");
			device.setPickupTime(null);
		}
		else
		{
			LOG.info("member found: {}", member);
			LOG.info("bookedBy param: {}", bookedBy);
			device.setBookedBy(member);
			device.setStatus("not available");
			device.setPickupTime(LocalDateTime.now());
		}
		redirectToIndex();
	}
}