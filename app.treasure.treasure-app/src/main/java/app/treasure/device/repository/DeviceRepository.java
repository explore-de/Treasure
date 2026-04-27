package app.treasure.device.repository;

import java.util.List;

import app.treasure.device.domain.Device;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeviceRepository implements PanacheRepository<Device>
{
	public List<Device> findAllDevices()
	{
		return listAll();
	}

	public List<Device> searchByName(String query)
	{
		if (query == null || query.isEmpty())
		{
			return listAll();
		}
		return list("lower(deviceName) like ?1", "%" + query.toLowerCase() + "%");
	}
}
