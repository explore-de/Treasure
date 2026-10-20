package app.treasure.device.repository;

import java.util.List;

import app.treasure.device.domain.Device;
import app.treasure.member.domain.Member;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeviceRepository implements PanacheRepository<Device>
{
	/**
	 * Devices currently assigned to the member, the ones held longest first.
	 */
	public List<Device> findAssignedTo(Member member)
	{
		return list("assignedTo = ?1 ORDER BY pickupTime, deviceName", member);
	}

	/** Devices nobody holds right now, by name. */
	public List<Device> findAvailable()
	{
		return list("assignedTo is null ORDER BY deviceName");
	}

	/** Every assigned device, the one held longest first. */
	public List<Device> findAssignedLongestFirst()
	{
		return list("assignedTo is not null ORDER BY pickupTime, deviceName");
	}
}
