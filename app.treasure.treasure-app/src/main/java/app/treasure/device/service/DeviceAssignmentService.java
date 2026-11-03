package app.treasure.device.service;

import java.time.LocalDateTime;

import app.treasure.device.domain.Device;
import app.treasure.device.repository.DeviceHistoryRepository;
import app.treasure.member.domain.Member;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Hands devices to members and takes them back. Status, pickup time and the
 * history entry change together, so the web pages and the MCP tools behave the
 * same. Callers run inside a transaction.
 */
@ApplicationScoped
public class DeviceAssignmentService
{
	@Inject
	DeviceHistoryRepository deviceHistoryRepository;

	/**
	 * Assigns the device to the member; false when the member already holds it.
	 */
	public boolean assign(Device device, Member member, Member actor)
	{
		if (member.equals(device.getAssignedTo()))
		{
			return false;
		}
		String oldBooked = device.getBookedName();
		String oldStatus = n(device.getStatus());
		String oldPickup = device.getPickupTime() != null ? device.getPickupTime().toString() : "";

		device.setAssignedTo(member);
		device.setStatus("not available");
		device.setPickupTime(LocalDateTime.now());

		String notes = "status: " + oldStatus + " -> not available, pickupTime: " + oldPickup + " -> "
			+ device.getPickupTime();
		deviceHistoryRepository.record(device, actor, "ASSIGNED", "assignedTo", n(oldBooked),
			n(device.getBookedName()), notes);
		return true;
	}

	/** Takes the device back; false when nobody holds it. */
	public boolean unassign(Device device, Member actor)
	{
		if (device.getAssignedTo() == null)
		{
			return false;
		}
		String oldBooked = device.getBookedName();
		String oldStatus = n(device.getStatus());
		String oldPickup = device.getPickupTime() != null ? device.getPickupTime().toString() : "";

		device.setAssignedTo(null);
		device.setStatus("available");
		device.setPickupTime(null);

		String notes = "status: " + oldStatus + " -> available, pickupTime: " + oldPickup + " -> ";
		deviceHistoryRepository.record(device, actor, "UNASSIGNED", "assignedTo", n(oldBooked), "", notes);
		return true;
	}

	private String n(String s)
	{
		return s == null ? "" : s;
	}
}
