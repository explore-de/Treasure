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
	public TemplateInstance index(@QueryParam("searchName") String searchName)
	{
		List<Device> devices = deviceRepository.listAll(Sort.by("id").ascending());
		String searchinName = (searchName == null) ? "" : searchName.trim().toLowerCase();
		for (Device device : devices)
		{
			if (searchinName.isEmpty())
			{
				device.setVisible(true);
			}
			else
			{
				String name = device.getDeviceName();
				device.setVisible(name != null && name.toLowerCase().contains(searchinName));
			}
		}
		String username = securityIdentity.getPrincipal().getName();
		Member currentmember = memberRepository.findByUsername(username);

		return Templates.index(devices, currentmember, memberRepository.listAll());
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
		redirect(DeviceResource.class).index(null);
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
			redirect(DeviceResource.class).index(null);
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

		redirect(DeviceResource.class).index(null);
	}

	@POST
	@Path("/{id}/delete")
	@Transactional
	public void delete(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);
		device.delete();
		redirect(DeviceResource.class).index(null);
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
			redirect(DeviceResource.class).index(null);
		}
		else
		{
			LOG.info("member found: {}", member);
			LOG.info("bookedBy param: {}", bookedBy);
			device.setBookedBy(member);
			device.setStatus("not available");
			device.setPickupTime(LocalDateTime.now());
			redirect(DeviceResource.class).index(null);
		}
	}
}