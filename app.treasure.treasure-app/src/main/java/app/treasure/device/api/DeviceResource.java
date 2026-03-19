package app.treasure.device.api;

import app.treasure.member.domain.Member;
import java.util.List;

import org.jboss.resteasy.reactive.RestForm;

import app.treasure.device.domain.Device;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.member.repository.MemberRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import io.quarkus.security.identity.SecurityIdentity;
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
	@Inject
	SecurityIdentity securityIdentity;
	@Inject
	MemberRepository memberRepository;

	@Inject
	SecurityIdentity identity;

	@CheckedTemplate
	public static class Templates
	{
		private Templates() {}

		public static native TemplateInstance index(List<Device> devices);
		public static native TemplateInstance create();
		public static native TemplateInstance editadmin(Device device);
		public static native TemplateInstance editnormuser(Device device);
	}

	@GET
	@Path("")
	public TemplateInstance index()
	{
		List<Device> devices = deviceRepository.listAll();
		return Templates.index(devices);
	}

	@GET
	@Path("/new")
	public TemplateInstance create()
	{
		return Templates.create();
	}

	@GET
	@Path("/{id}/edit")
	public TemplateInstance edit(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);
		if (identity.hasRole("admin") || identity.hasRole("SUPER_ADMIN")) {
			return Templates.editadmin(device);
		} else {
			return Templates.editnormuser(device);
		}
	}

	@POST
	@Path("/create")
	@Transactional
	public void save(@RestForm String deviceName, @RestForm String status)
	{
		if (deviceName.matches(".*[a-zA-Z0-9].*")) {
			Device device = new Device();
			device.setDeviceName(deviceName);
			device.setStatus(status);
			deviceRepository.persist(device);
		}
		redirect(DeviceResource.class).index();
	}

	@POST
	@Path("/{id}/update")
	@Transactional
	public void update(@PathParam("id") Long id, @RestForm String deviceName)
	{
		if (!deviceName.matches(".*[a-zA-Z0-9].*")) {
			redirect(DeviceResource.class).index();
			return;
		}
		Device device = deviceRepository.findById(id);
		device.setDeviceName(deviceName);
		redirect(DeviceResource.class).index();
	}

	@POST
	@Path("/{id}/delete")
	@Transactional
	public void delete(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);
		device.delete();
		redirect(DeviceResource.class).index();
	}

	@POST
	@Path("/{id}/claim")
	@Transactional
	public void claim(@PathParam("id") Long id)
	{
		Device device = deviceRepository.findById(id);
		String username = securityIdentity.getPrincipal().getName();
		Member currentmember = memberRepository.findByUsername(username);
		if (device.getBookedBy() != null && device.getBookedBy().equals(currentmember))
		{
			device.setStatus("available");
			device.setBookedBy(null);
		}
		else
		{
			device.setStatus("not available");
			device.setBookedBy(currentmember);
		}
		redirect(DeviceResource.class).index();
	}

}
