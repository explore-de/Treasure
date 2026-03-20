package app.treasure.device.api;

import app.treasure.member.domain.Member;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.jboss.resteasy.reactive.RestForm;
import app.treasure.device.domain.Device;
import app.treasure.device.repository.DeviceRepository;
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

@Authenticated
@Path("/devices")
public class DeviceResource extends Controller
{
	@Inject
	DeviceRepository deviceRepository;

	@Inject
	SecurityIdentity securityIdentity;

	@Inject
	MemberRepository memberRepository;

	@CheckedTemplate
	public static class Templates
	{
		private Templates()
		{
		}

		public static native TemplateInstance index(List<Device> devices);

		public static native TemplateInstance create(List<Member> members);

		public static native TemplateInstance editadmin(Device device, List<Member> members);

		public static native TemplateInstance editnormuser(Device device);
	}

	/**
	 * Resolves the currently logged-in member by Keycloak ID or username as
	 * fallback.
	 *
	 * @return the current member, or null if not found
	 */
	private Member getCurrentMember()
	{
		String keycloakId = securityIdentity.getPrincipal().getName();
		Member member = memberRepository.findByKeycloakUserId(keycloakId);
		if (member == null)
		{
			member = memberRepository.findByUsername(keycloakId);
		}
		return member;
	}

	/**
	 * Shows the list of all devices.
	 */
	@GET
	@Path("")
	public TemplateInstance index()
	{
		List<Device> devices = deviceRepository.listAllEager();
		return Templates.index(devices);
	}

	/**
	 * Shows the form to create a new device.
	 */
	@GET
	@Path("/new")
	public TemplateInstance create()
	{
		List<Member> members = memberRepository.listAll();
		return Templates.create(members);
	}

	/**
	 * Shows the edit form. Admins see the admin view, regular users see the
	 * restricted view.
	 */
	@GET
	@Path("/{id}/edit")
	public TemplateInstance edit(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);

		if (securityIdentity.hasRole("admin") || securityIdentity.hasRole("SUPER_ADMIN"))
		{
			List<Member> members = memberRepository.listAll();
			return Templates.editadmin(device, members);
		}
		else
		{
			return Templates.editnormuser(device);
		}
	}

	/**
	 * Creates a new device. Automatically sets registration date and author.
	 */
	@POST
	@Path("/create")
	@Transactional
	public void save(
		@RestForm String deviceName,
		@RestForm String brand,
		@RestForm String modelNumber,
		@RestForm String serialNumber,
		@RestForm String operatingSystem,
		@RestForm String isUsed,
		@RestForm String purchaseDate,
		@RestForm String storageLocation,
		@RestForm String condition,
		@RestForm String importantNotes,
		@RestForm Long assignedToId)
	{
		if (!deviceName.matches(".*[a-zA-Z0-9].*"))
		{
			redirect(DeviceResource.class).index();
			return;
		}

		Device device = new Device();
		device.setDeviceName(deviceName);
		device.setBrand(brand);
		device.setModelNumber(modelNumber);
		device.setSerialNumber(serialNumber);
		device.setOperatingSystem(operatingSystem);
		device.setUsed("on".equals(isUsed));
		device.setStorageLocation(storageLocation);
		device.setCondition(condition);
		device.setImportantNotes(importantNotes);

		if (purchaseDate != null && !purchaseDate.isBlank())
		{
			device.setPurchaseDate(LocalDate.parse(purchaseDate));
		}

		// Automatically set registration info
		device.setRegisteredAt(LocalDateTime.now());
		device.setRegisteredBy(getCurrentMember());

		if (assignedToId != null && assignedToId > 0)
		{
			Member member = memberRepository.findById(assignedToId);
			if (member != null)
			{
				device.setBookedBy(member);
				device.setStatus("not available");
			}
		}
		else
		{
			device.setStatus("available");
		}

		deviceRepository.persist(device);
		redirect(DeviceResource.class).index();
	}

	/**
	 * Updates an existing device.
	 */
	@POST
	@Path("/{id}/update")
	@Transactional
	public void update(
		@PathParam("id") Long id,
		@RestForm String deviceName,
		@RestForm String brand,
		@RestForm String modelNumber,
		@RestForm String serialNumber,
		@RestForm String operatingSystem,
		@RestForm String isUsed,
		@RestForm String purchaseDate,
		@RestForm String storageLocation,
		@RestForm String condition,
		@RestForm String importantNotes,
		@RestForm Long assignedToId)
	{
		if (!deviceName.matches(".*[a-zA-Z0-9].*"))
		{
			redirect(DeviceResource.class).index();
			return;
		}

		Device device = deviceRepository.findById(id);
		device.setDeviceName(deviceName);
		device.setBrand(brand);
		device.setModelNumber(modelNumber);
		device.setSerialNumber(serialNumber);
		device.setOperatingSystem(operatingSystem);
		device.setUsed("on".equals(isUsed));
		device.setStorageLocation(storageLocation);
		device.setCondition(condition);
		device.setImportantNotes(importantNotes);

		if (purchaseDate != null && !purchaseDate.isBlank())
		{
			device.setPurchaseDate(LocalDate.parse(purchaseDate));
		}
		else
		{
			device.setPurchaseDate(null);
		}

		if (assignedToId != null && assignedToId > 0)
		{
			Member member = memberRepository.findById(assignedToId);
			device.setBookedBy(member != null ? member : null);
		}
		else
		{
			device.setBookedBy(null);
		}

		device.setStatus(device.getBookedBy() != null ? "not available" : "available");
		redirect(DeviceResource.class).index();
	}

	/**
	 * Deletes a device by ID.
	 */
	@POST
	@Path("/{id}/delete")
	@Transactional
	public void delete(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);
		device.delete();
		redirect(DeviceResource.class).index();
	}

	/**
	 * Toggles the claim on a device. Only the member who claimed it can unclaim
	 * it. Other users cannot override an existing claim.
	 */
	@POST
	@Path("/{id}/claim")
	@Transactional
	public void claim(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);
		Member currentMember = getCurrentMember();

		if (currentMember == null)
		{
			redirect(DeviceResource.class).index();
			return;
		}

		if (device.getBookedBy() == null)
		{
			// Nobody has claimed it — claim it
			device.setBookedBy(currentMember);
			device.setStatus("not available");
		}
		else if (device.getBookedBy().id.equals(currentMember.id))
		{
			// Same user — unclaim it
			device.setBookedBy(null);
			device.setStatus("available");
		}
		// Another user has claimed it — do nothing

		redirect(DeviceResource.class).index();
	}
}