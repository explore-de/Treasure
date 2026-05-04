package app.treasure.device.repository;

import app.treasure.device.domain.DeviceHistory;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class DeviceHistoryRepository implements PanacheRepository<DeviceHistory> {

    public List<DeviceHistory> forDevice(Long deviceId) {
        return find("device.id = ?1", Sort.by("happenedAt").descending(), deviceId).list();
    }
}