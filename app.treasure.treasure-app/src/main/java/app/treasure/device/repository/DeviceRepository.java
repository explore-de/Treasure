package app.treasure.device.repository;

import java.util.List;

import app.treasure.device.domain.Device;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeviceRepository implements PanacheRepository<Device>
{
	public List<Device> findAllDevices() // Lists all devices
	{
		return listAll();
	}

	public List<Device> searchByName(String query)
	{
		if (query == null || query.isEmpty())
		{ // call of method on object
			return listAll(); // if u return null it will crash, cause index
								// waits for list and not null
		}
		return list("lower(deviceName) like ?1", "%" + query.toLowerCase() + "%"); // lower
																					// compares
																					// device
																					// name
																					// with
																					// param
																					// and
																					// both
																					// of
																					// them
																					// are
																					// making
																					// the
																					// name
																					// in
																					// lowercase
	}

}
