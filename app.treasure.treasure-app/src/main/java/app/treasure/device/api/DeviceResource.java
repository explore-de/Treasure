package app.treasure.device.api;

import app.treasure.device.domain.DeviceHistory;
import app.treasure.device.repository.DeviceHistoryRepository;
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
	DeviceHistoryRepository deviceHistoryRepository;

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

		public static native TemplateInstance index(List<Device> devices, String errorMessage);

		public static native TemplateInstance create(List<Member> members);

		public static native TemplateInstance editadmin(Device device, List<Member> members,
			List<DeviceHistory> history);

		public static native TemplateInstance editnormuser(Device device);
	}

	/**
	 * Resolves the currently logged-in member by Keycloak ID or username as
	 * fallback.
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
	 * Logs a history event for a device.
	 */
	private void logHistory(Device device, String action, String details)
	{
		DeviceHistory entry = new DeviceHistory();
		entry.setDevice(device);
		entry.setPerformedBy(getCurrentMember());
		entry.setTimestamp(LocalDateTime.now());
		entry.setAction(action);
		entry.setDetails(details);
		deviceHistoryRepository.persist(entry);
	}

	/**
	 * Returns true if the current user is allowed to delete the device. Admins
	 * can always delete. Regular users can only delete if the device is newer
	 * than 7 days and has only been touched by 1 user.
	 */
	private boolean canDelete(Device device)
	{
		if (securityIdentity.hasRole("admin") || securityIdentity.hasRole("SUPER_ADMIN"))
		{
			return true;
		}

		boolean isOld = device.getRegisteredAt() != null
			&& device.getRegisteredAt().isBefore(LocalDateTime.now().minusDays(7));

		long uniqueUsers = deviceHistoryRepository.countDistinctUsers(device);

		return !isOld && uniqueUsers <= 1;
	}

	/**
	 * Shows the list of all devices.
	 */
	@GET
	@Path("")
	public TemplateInstance index()
	{
		List<Device> devices = deviceRepository.listAllEager();
		String errorMessage = flash.get("error");
		return Templates.index(devices, errorMessage);
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
			List<DeviceHistory> history = deviceHistoryRepository.findByDevice(device);
			return Templates.editadmin(device, members, history);
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
		device.setRegisteredAt(LocalDateTime.now());
		device.setRegisteredBy(getCurrentMember());

		if (purchaseDate != null && !purchaseDate.isBlank())
		{
			try
			{
				device.setPurchaseDate(LocalDate.parse(purchaseDate));
			}
			catch (Exception e)
			{
				device.setPurchaseDate(null);
			}
		}

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
		logHistory(device, "CREATED", "Device \"" + deviceName + "\" created");

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
		Member previousBookedBy = device.getBookedBy();

		// Track what changed
		StringBuilder changes = new StringBuilder();

		if (!eq(device.getDeviceName(), deviceName))
			changes.append("Name: \"").append(device.getDeviceName()).append("\" → \"").append(deviceName).append("\"\n");
		if (!eq(device.getBrand(), brand))
			changes.append("Brand: \"").append(nvl(device.getBrand())).append("\" → \"").append(nvl(brand)).append("\"\n");
		if (!eq(device.getModelNumber(), modelNumber))
			changes.append("Model: \"").append(nvl(device.getModelNumber())).append("\" → \"").append(nvl(modelNumber)).append("\"\n");
		if (!eq(device.getSerialNumber(), serialNumber))
			changes.append("Serial: \"").append(nvl(device.getSerialNumber())).append("\" → \"").append(nvl(serialNumber)).append("\"\n");
		if (!eq(device.getOperatingSystem(), operatingSystem))
			changes.append("OS: \"").append(nvl(device.getOperatingSystem())).append("\" → \"").append(nvl(operatingSystem)).append("\"\n");
		if (device.isUsed() != "on".equals(isUsed))
			changes.append("Used: ").append(device.isUsed()).append(" → ").append("on".equals(isUsed)).append("\n");
		if (!eq(device.getStorageLocation(), storageLocation))
			changes.append("Location: \"").append(nvl(device.getStorageLocation())).append("\" → \"").append(nvl(storageLocation)).append("\"\n");
		if (!eq(device.getCondition(), condition))
			changes.append("Condition: \"").append(nvl(device.getCondition())).append("\" → \"").append(nvl(condition)).append("\"\n");
		if (!eq(device.getImportantNotes(), importantNotes))
			changes.append("Notes: \"").append(nvl(device.getImportantNotes())).append("\" → \"").append(nvl(importantNotes)).append("\"\n");

		// Parse purchase date safely
		LocalDate newPurchaseDate = null;
		if (purchaseDate != null && !purchaseDate.isBlank())
		{
			try
			{
				newPurchaseDate = LocalDate.parse(purchaseDate);
			}
			catch (Exception e)
			{
				newPurchaseDate = null;
			}
		}
		if (!eq(device.getPurchaseDate(), newPurchaseDate))
			changes.append("Purchase date: \"").append(nvl(device.getPurchaseDate())).append("\" → \"").append(nvl(newPurchaseDate)).append("\"\n");

		// Apply changes
		device.setDeviceName(deviceName);
		device.setBrand(brand);
		device.setModelNumber(modelNumber);
		device.setSerialNumber(serialNumber);
		device.setOperatingSystem(operatingSystem);
		device.setUsed("on".equals(isUsed));
		device.setStorageLocation(storageLocation);
		device.setCondition(condition);
		device.setImportantNotes(importantNotes);
		device.setPurchaseDate(newPurchaseDate);

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

		// Log assignment change
		boolean wasUnassigned = previousBookedBy == null && device.getBookedBy() != null;
		boolean wasUnbooked = previousBookedBy != null && device.getBookedBy() == null;
		boolean wasReassigned = previousBookedBy != null && device.getBookedBy() != null
			&& !previousBookedBy.id.equals(device.getBookedBy().id);

		if (wasUnassigned)
		{
			logHistory(device, "ASSIGNED",
				"Assigned to " + device.getBookedBy().getDisplayName());
		}
		else if (wasUnbooked)
		{
			logHistory(device, "UNASSIGNED",
				"Unassigned from " + previousBookedBy.getDisplayName());
		}
		else if (wasReassigned)
		{
			logHistory(device, "ASSIGNED", "Reassigned from "
				+ previousBookedBy.getDisplayName() + " to "
				+ device.getBookedBy().getDisplayName());
		}

		// Only log EDITED if something actually changed
		if (changes.length() > 0)
		{
			logHistory(device, "EDITED", changes.toString().trim());
		}

		redirect(DeviceResource.class).index();
	}

	/** Null-safe equality check. */
	private boolean eq(Object a, Object b)
	{
		if (a == null && b == null) return true;
		if (a == null || b == null)
		{
			// Treat null and blank string as equal
			String sa = a != null ? a.toString() : "";
			String sb = b != null ? b.toString() : "";
			return sa.equals(sb);
		}
		return a.equals(b);
	}

	/** Null-safe value to string. */
	private String nvl(Object o)
	{
		return o != null ? o.toString() : "";
	}

	/**
	 * Deletes a device by ID. Regular users can only delete devices that are
	 * newer than 7 days and have only been touched by one user.
	 */
	@POST
	@Path("/{id}/delete")
	@Transactional
	public void delete(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);

		if (!canDelete(device))
		{
			flash("error",
				"Only admins can delete devices that are older than 7 days or have been used by multiple users.");
			redirect(DeviceResource.class).index();
			return;
		}

		deviceHistoryRepository.delete("device", device);
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
			device.setBookedBy(currentMember);
			device.setStatus("not available");
			logHistory(device, "CLAIMED",
				currentMember.getDisplayName() + " claimed the device");
		}
		else if (device.getBookedBy().id.equals(currentMember.id))
		{
			logHistory(device, "UNCLAIMED",
				currentMember.getDisplayName() + " returned the device");
			device.setBookedBy(null);
			device.setStatus("available");
		}

		redirect(DeviceResource.class).index();
	}
}