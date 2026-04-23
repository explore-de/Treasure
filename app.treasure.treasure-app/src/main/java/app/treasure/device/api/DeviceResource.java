package app.treasure.device.api;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.RestForm;

import app.treasure.device.domain.Device;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import io.quarkiverse.renarde.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.panache.common.Sort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.QueryParam;

@Authenticated
@Path("/devices")
public class DeviceResource extends Controller
{
	private static final Logger LOG = LoggerFactory.getLogger(DeviceResource.class);

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
		private Templates()
		{
		}

		public static native TemplateInstance index(List<Device> devices, Member currentmember, List<Member> members, String query);

		public static native TemplateInstance create();

		public static native TemplateInstance edit(Device device);
	}
	@GET
	@Path("")
	public TemplateInstance index(@QueryParam("query") String query)
	{
		List<Device> devices = deviceRepository.searchByName(query); // on link
																		// /device
																		// will
																		// heppen
																		// method
																		// searchByName
		String username = securityIdentity.getPrincipal().getName();
		Member currentmember = memberRepository.findByUsername(username);
		return Templates.index(devices, currentmember, memberRepository.listAll(), query);
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
		return Templates.edit(device);
	}

	@POST
	@Path("/create")
	@Transactional
	public void save(@RestForm String deviceName, @RestForm String status, @RestForm String deviceSerialNumber)
	{
		if (deviceName.matches(".*[a-zA-Z0-9а-яА-Я].*"))
		{
			Device device = new Device();
			device.setDeviceName(deviceName);
			device.setDeviceSerialNumber(deviceSerialNumber);
			device.setStatus("available");
			deviceRepository.persist(device);
			device.setCreatedOn(String.valueOf(LocalDateTime.now()));
		}
		redirect(DeviceResource.class).index(null);
	}

	@POST
	@Path("/{id}/update")
	@Transactional
	public void update(@PathParam("id") Long id, @RestForm String deviceName, @RestForm String bookedBy)
	{
		if (!deviceName.matches(".*[a-zA-Z0-9а-яА-Я].*"))
		{
			redirect(DeviceResource.class).index(null);
			return;
		}
		Device device = deviceRepository.findById(id);
		device.setDeviceName(deviceName);
		Member member = memberRepository.findByUsername(bookedBy);
		LOG.debug("bookedBy param: {}, member found: {}", bookedBy, member);
		device.setBookedBy(member);
		redirect(DeviceResource.class).index(null);
	}

	@POST
	@Path("/{id}/delete")
	@Transactional
	public void delete(@PathParam("id") Long id)
	{

		Device device = deviceRepository.findById(id);
		device.delete();
		redirect(DeviceResource.class).index(null);
	}

	@POST
	@Path("/{id}/assign")
	@Transactional
	public void assign(@PathParam("id") Long id, @RestForm String bookedBy)
	{
		Device device = deviceRepository.findById(id);
		Member member = memberRepository.findByUsername(bookedBy);
		LOG.info("member found: {}, bookedBy param: {}", member, bookedBy);
		if (device.getBookedBy() == member)
		{
			device.setBookedBy(null);
			device.setStatus("available");
			device.setPickupTime(null);
		}
		else
		{
			device.setBookedBy(member);
			device.setStatus("not available");
			device.setPickupTime(LocalDateTime.now());
		}
		redirect(DeviceResource.class).index(null);
	}
}
