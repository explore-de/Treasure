package app.treasure.device.api;

import app.treasure.device.repository.DeviceRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import app.treasure.device.domain.Device;
import java.util.List;

import org.jboss.resteasy.reactive.RestForm;

@Authenticated
@Path("/devices")
public class DeviceResource extends Controller
{
	@Inject
	DeviceRepository deviceRepository;

	@CheckedTemplate
	public static class Templates
	{
		private Templates()
		{
		}

		public static native TemplateInstance index(List<Device> devices); // Declares
																			// the
																			// index
																			// template

		public static native TemplateInstance create();
	}

	@GET
	@Path("")
	public TemplateInstance index()
	{
		List<Device> devices = deviceRepository.listAll(); // gets all devices
															// and sends them to
															// the page
		return Templates.index(devices);
	}

	@GET
	@Path("/new")
	public TemplateInstance create()
	{
		return Templates.create(); // shows the create device page
	}

	@POST
	@Path("/create")
	@Transactional
	public void save(@RestForm String deviceName, @RestForm String status)
	{
		Device device = new Device(); // create the new object
		device.setDeviceName(deviceName);
		device.setStatus(status);
		deviceRepository.persist(device); // saves it

	}

	@POST
	@Path("/{id}/delete")
	@Transactional
	public void delete(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id); // searching for id, and
														// assign it to variable
		device.delete(); // deletes
		redirect(DeviceResource.class).index(); // reloads the site
	}

}
