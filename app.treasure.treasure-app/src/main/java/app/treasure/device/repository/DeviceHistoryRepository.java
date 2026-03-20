package app.treasure.device.repository;

import app.treasure.device.domain.Device;
import app.treasure.device.domain.DeviceHistory;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class DeviceHistoryRepository implements PanacheRepository<DeviceHistory>
{
	/**
	 * Returns all history entries for a device, ordered chronologically.
	 */
	public List<DeviceHistory> findByDevice(Device device)
	{
		return list("device = ?1 ORDER BY timestamp ASC", device);
	}

	/**
	 * Returns the number of distinct users who have interacted with a device.
	 */
	public long countDistinctUsers(Device device)
	{
		return find("device = ?1", device)
			.stream()
			.filter(h -> h.getPerformedBy() != null)
			.map(h -> h.getPerformedBy().id)
			.distinct()
			.count();
	}
}