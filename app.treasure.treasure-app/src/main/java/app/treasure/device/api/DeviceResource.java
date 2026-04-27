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

		public static native TemplateInstance index(List<Device> devices, Member currentmember, List<Member> members);

		// ✅ groups als Vorschlagsliste für das Group-Input
		public static native TemplateInstance create(List<String> groups);

		public static native TemplateInstance edit(Device device, List<String> groups);
	}

	// ✅ bekannte Gruppen aus existierenden Devices (distinct + sort)
	private List<String> loadKnownGroups()
	{
		return deviceRepository.listAll().stream()
			.map(Device::getGroup)
			.filter(g -> g != null && !g.isBlank())
			.distinct()
			.sorted()
			.toList();
	}

	@GET
	@Path("")
	public TemplateInstance index(
		@QueryParam("searchName") String searchName,
		@QueryParam("name") List<String> names,
		@QueryParam("group") List<String> groups)
	{
		// 1) Fallback: altes searchName als Name-Tag behandeln (nur wenn keine
		// name-Tags existieren)
		if ((names == null || names.isEmpty()) && searchName != null && !searchName.trim().isBlank())
		{
			names = List.of(searchName.trim());
		}

		// 2) Normalisieren (trim + empty raus)
		List<String> nameTerms = (names == null) ? List.of() : names.stream().map(s -> s == null ? "" : s.trim()).filter(s -> !s.isBlank()).toList();

		List<String> groupTerms = (groups == null) ? List.of() : groups.stream().map(s -> s == null ? "" : s.trim()).filter(s -> !s.isBlank()).toList();

		// 3) Laden + filtern
		List<Device> all = deviceRepository.listAll(Sort.by("id").ascending());

		List<Device> filtered = all.stream()
			.filter(d -> matchesTerms(d, nameTerms, groupTerms))
			.toList();

		String username = securityIdentity.getPrincipal().getName();
		Member currentmember = memberRepository.findByUsername(username);

		return Templates.index(filtered, currentmember, memberRepository.listAll());
	}

	private boolean matchesTerms(Device d, List<String> names, List<String> groups)
	{
		boolean nameOk = names.isEmpty() || names.stream().anyMatch(t -> containsIgnoreCase(d.getDeviceName(), t));
		boolean groupOk = groups.isEmpty() || groups.stream().anyMatch(t -> containsIgnoreCase(d.getGroup(), t));
		return nameOk && groupOk;
	}

	private boolean containsIgnoreCase(String haystack, String needle)
	{
		if (haystack == null || needle == null) return false;
		return haystack.toLowerCase().contains(needle.toLowerCase());
	}

	@GET
	@Path("/new")
	public TemplateInstance create()
	{
		return Templates.create(loadKnownGroups());
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

			// ✅ neue Felder setzen
			device.setGroup(group);
			device.setDeviceModel(deviceModel);
			device.setExtraInfo(extraInfo);
			device.setDeviceDamage(deviceDamage);
			device.setDeviceAge(deviceAge);

			deviceRepository.persist(device);
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

		// ✅ neue Felder
		@RestForm String group,
		@RestForm String deviceModel,
		@RestForm String extraInfo,
		@RestForm String deviceDamage,
		@RestForm String deviceAge)
	{
		if (deviceName == null || !deviceName.matches(".*[a-zA-Z0-9а-яА-Я].*"))
		{
			seeOther("/devices");
			return;
		}

		Device device = deviceRepository.findById(id);

		device.setDeviceName(deviceName);
		device.setDeviceSerialNumber(deviceSerialNumber);

		// ✅ neue Felder updaten
		device.setGroup(group);
		device.setDeviceModel(deviceModel);
		device.setExtraInfo(extraInfo);
		device.setDeviceDamage(deviceDamage);
		device.setDeviceAge(deviceAge);

		seeOther("/devices");
	}

	@POST
	@Path("/{id}/delete")
	@Transactional
	public void delete(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);
		device.delete();
		seeOther("/devices");
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
			seeOther("/devices");
		}
		else
		{
			LOG.info("member found: {}", member);
			LOG.info("bookedBy param: {}", bookedBy);
			device.setBookedBy(member);
			device.setStatus("not available");
			device.setPickupTime(LocalDateTime.now());
			seeOther("/devices");
		}
	}
}