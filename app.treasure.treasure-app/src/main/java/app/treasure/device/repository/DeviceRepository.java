package app.treasure.device.repository;

import java.util.List;
import app.treasure.device.domain.Device;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeviceRepository implements PanacheRepository<Device>
{
	/**
	 * Returns all devices with their relations eagerly loaded to avoid lazy
	 * loading issues in templates.
	 */
	public List<Device> listAllEager()
	{
		return list(
			"SELECT d FROM Device d LEFT JOIN FETCH d.bookedBy");
	}
}