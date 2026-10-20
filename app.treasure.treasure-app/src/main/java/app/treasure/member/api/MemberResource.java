package app.treasure.member.api;

import java.util.ArrayList;
import java.util.List;

import app.treasure.device.domain.Device;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.panache.common.Sort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Everyone who can hold a device, with the devices each of them holds.
 * Assigning and unassigning reuse the bulk endpoints of DeviceResource.
 */
@Authenticated
@Path("/members")
public class MemberResource extends Controller
{
	@Inject
	MemberRepository memberRepository;

	@Inject
	DeviceRepository deviceRepository;

	/** A member together with the devices currently assigned to them. */
	public record MemberRow(Member member, List<Device> devices)
	{
	}

	@CheckedTemplate
	public static class Templates
	{
		private Templates()
		{
			// static
		}

		public static native TemplateInstance index(List<MemberRow> rows, List<Device> available, long heldTotal);
	}

	@GET
	@Path("")
	public TemplateInstance index()
	{
		List<MemberRow> rows = new ArrayList<>();
		for (Member member : memberRepository.listAll(Sort.by("lastName").and("firstName")))
		{
			rows.add(new MemberRow(member, deviceRepository.findAssignedTo(member)));
		}
		long heldTotal = rows.stream().mapToLong(row -> row.devices().size()).sum();
		return Templates.index(rows, deviceRepository.findAvailable(), heldTotal);
	}
}
