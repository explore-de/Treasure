package app.fuggs.device.repository;

import java.util.List;

import app.fuggs.device.domain.Device;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeviceRepository implements PanacheRepository<Device>
{
	public List<Device> findAllDevices() // Lists all devices
	{
		return listAll();
	}
}
