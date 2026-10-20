package app.treasure.device.repository;

import app.treasure.device.domain.DeviceHistory;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class DeviceHistoryRepository implements PanacheRepository<DeviceHistory>
{

	/** The most recent changes across all devices. */
	public List<DeviceHistory> findRecent(int limit)
	{
		return findAll(Sort.by("happenedAt").descending()).page(0, limit).list();
	}

	public List<DeviceHistory> forDevice(Long deviceId)
	{
		return find("device.id = ?1", Sort.by("happenedAt").descending(), deviceId).list();
	}
}