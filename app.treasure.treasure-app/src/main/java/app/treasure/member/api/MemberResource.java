package app.treasure.member.api;

import java.util.ArrayList;
import java.util.List;

import org.jboss.resteasy.reactive.RestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.device.domain.Device;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import app.treasure.member.service.MemberValidationException;
import app.treasure.member.service.MemberService;
import app.treasure.shared.security.Roles;
import app.treasure.shared.util.FlashKeys;
import io.quarkiverse.renarde.Controller;
import io.quarkus.panache.common.Sort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

/**
 * Everyone who can hold a device, with the devices each of them holds.
 * Assigning and unassigning reuse the bulk endpoints of DeviceResource. Admins
 * can add people who have not signed in yet.
 */
@Authenticated
@Path("/members")
public class MemberResource extends Controller
{
	private static final Logger LOG = LoggerFactory.getLogger(MemberResource.class);

	@Inject
	MemberRepository memberRepository;

	@Inject
	DeviceRepository deviceRepository;

	@Inject
	MemberService memberService;

	@Inject
	SecurityIdentity securityIdentity;

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

		public static native TemplateInstance create(String firstName, String lastName, String email, String error);
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

	@GET
	@Path("/new")
	@RolesAllowed({ Roles.ADMIN, Roles.SUPER_ADMIN })
	public TemplateInstance create()
	{
		return Templates.create("", "", "", null);
	}

	@POST
	@RolesAllowed({ Roles.ADMIN, Roles.SUPER_ADMIN })
	@Path("/create")
	@Transactional
	public TemplateInstance add(
		@RestForm String firstName,
		@RestForm String lastName,
		@RestForm String email)
	{
		Member inviter = currentMember();
		Member member;
		try
		{
			member = memberService.add(firstName, lastName, email, inviter);
		}
		catch (MemberValidationException e)
		{
			LOG.warn("Refused to add member '{}': {}", email, e.getMessage());
			// rendered directly so the admin keeps what they typed
			return Templates.create(n(firstName), n(lastName), n(email), e.getMessage());
		}

		flash(FlashKeys.SUCCESS, member.getDisplayName() + " is added. They show as never signed in until "
			+ "they sign in with " + member.getEmail() + ".");
		seeOther("/members");
		return null;
	}

	private Member currentMember()
	{
		return memberRepository.findByUsername(securityIdentity.getPrincipal().getName());
	}

	private static String n(String value)
	{
		return value == null ? "" : value;
	}
}
