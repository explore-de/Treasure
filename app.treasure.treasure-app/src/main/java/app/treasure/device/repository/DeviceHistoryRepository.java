package app.treasure.device.repository;

import app.treasure.device.domain.Device;
import app.treasure.device.domain.DeviceHistory;
import app.treasure.member.domain.Member;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
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

	/** Records one change of the device, made now by the actor. */
	public void record(Device device, Member actor,
		String eventType, String field,
		String oldVal, String newVal,
		String notes)
	{
		DeviceHistory h = new DeviceHistory();
		h.setDevice(device);
		h.setActor(actor);
		h.setHappenedAt(LocalDateTime.now());
		h.setEventType(eventType);
		h.setFieldName(field);
		h.setOldValue(oldVal);
		h.setNewValue(newVal);
		h.setNotes(notes);
		persist(h);
	}
}