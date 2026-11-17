package app.treasure.member.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import app.treasure.shared.util.Pager;
import io.quarkiverse.renarde.Controller;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

/**
 * Everyone who can hold a device, with the devices each of them holds.
 * Assigning and unassigning reuse the bulk endpoints of DeviceResource. Admins
 * can add people who have not signed in yet, change every member and deactivate
 * or reactivate one; everyone else can change their own name, email and phone.
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

	/**
	 * A member together with the devices currently assigned to them, whether
	 * the viewer may change the member, and whether the member is the viewer.
	 */
	public record MemberRow(Member member, List<Device> devices, boolean editable, boolean self)
	{
	}

	@CheckedTemplate
	public static class Templates
	{
		private Templates()
		{
			// static
		}

		public static native TemplateInstance index(List<MemberRow> rows, List<Device> available, long heldTotal,
			Pager pager);

		public static native TemplateInstance create(String firstName, String lastName, String email, String error);

		public static native TemplateInstance edit(Member member, String firstName, String lastName, String email,
			String phone, String error);
	}

	@GET
	@Path("")
	public TemplateInstance index(@QueryParam(Pager.PARAM) Integer page, @Context UriInfo uriInfo)
	{
		PanacheQuery<Member> query = memberRepository.findAllForList();
		Pager pager = Pager.of(page, query.count(), uriInfo);
		List<Member> members = query.page(Page.of(pager.index(), Pager.PAGE_SIZE)).list();

		// one query for the devices of the whole page instead of one per member
		Map<Long, List<Device>> held = deviceRepository.findAssignedToAny(members).stream()
			.collect(Collectors.groupingBy(device -> device.getAssignedTo().getId()));
		String viewer = securityIdentity.getPrincipal().getName();
		List<MemberRow> rows = new ArrayList<>();
		for (Member member : members)
		{
			rows.add(new MemberRow(member, held.getOrDefault(member.getId(), List.of()),
				memberService.mayEdit(member, securityIdentity), viewer.equals(member.getUserName())));
		}
		return Templates.index(rows, deviceRepository.findAvailable(), deviceRepository.countAssigned(), pager);
	}

	@GET
	@Path("/new")
	@RolesAllowed(Roles.ADMIN)
	public TemplateInstance create()
	{
		return Templates.create("", "", "", null);
	}

	@POST
	@RolesAllowed(Roles.ADMIN)
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

	/** The signed-in person's own member, for the "Edit profile" link. */
	@GET
	@Path("/me")
	public void me()
	{
		Member member = currentMember();
		if (member == null)
		{
			flash(FlashKeys.ERROR, "Your member profile does not exist yet.");
			seeOther("/members");
			return;
		}
		seeOther("/members/" + member.getId() + "/edit");
	}

	@GET
	@Path("/{id}/edit")
	public TemplateInstance edit(@PathParam("id") Long id)
	{
		Member member = editableMember(id);
		return Templates.edit(member, member.getFirstName(), member.getLastName(), n(member.getEmail()),
			n(member.getPhone()), null);
	}

	@POST
	@Path("/{id}/update")
	@Transactional
	public TemplateInstance update(
		@PathParam("id") Long id,
		@RestForm String firstName,
		@RestForm String lastName,
		@RestForm String email,
		@RestForm String phone)
	{
		Member member = editableMember(id);
		try
		{
			memberService.update(member, firstName, lastName, email, phone,
				securityIdentity.getPrincipal().getName());
		}
		catch (MemberValidationException e)
		{
			LOG.warn("Refused to change member {}: {}", id, e.getMessage());
			// rendered directly so the form keeps what was typed
			return Templates.edit(member, n(firstName), n(lastName), n(email), n(phone), e.getMessage());
		}
		flash(FlashKeys.SUCCESS, "Saved the details of " + member.getDisplayName() + ".");
		seeOther("/members");
		return null;
	}

	@POST
	@RolesAllowed(Roles.ADMIN)
	@Path("/{id}/deactivate")
	@Transactional
	public void deactivate(@PathParam("id") Long id, @RestForm String redirectUrl)
	{
		Member member = existingMember(id);
		try
		{
			memberService.deactivate(member, securityIdentity.getPrincipal().getName());
			flash(FlashKeys.SUCCESS, member.getDisplayName() + " is deactivated and can no longer be given devices.");
		}
		catch (MemberValidationException e)
		{
			LOG.warn("Refused to deactivate member {}: {}", id, e.getMessage());
			flash(FlashKeys.ERROR, e.getMessage());
		}
		seeOther(membersPage(redirectUrl));
	}

	@POST
	@RolesAllowed(Roles.ADMIN)
	@Path("/{id}/activate")
	@Transactional
	public void activate(@PathParam("id") Long id, @RestForm String redirectUrl)
	{
		Member member = existingMember(id);
		memberService.reactivate(member, securityIdentity.getPrincipal().getName());
		flash(FlashKeys.SUCCESS, member.getDisplayName() + " is active again.");
		seeOther(membersPage(redirectUrl));
	}

	/**
	 * Where to go after a row action: back to the page of the Members list the
	 * form was on, never anywhere else.
	 */
	private static String membersPage(String redirectUrl)
	{
		return redirectUrl != null && redirectUrl.matches("/members\\?page=\\d{1,9}") ? redirectUrl : "/members";
	}

	private Member existingMember(Long id)
	{
		Member member = memberRepository.findById(id);
		if (member == null)
		{
			throw new NotFoundException();
		}
		return member;
	}

	/** The member, if it exists and the signed-in person may change it. */
	private Member editableMember(Long id)
	{
		Member member = existingMember(id);
		if (!memberService.mayEdit(member, securityIdentity))
		{
			LOG.warn("{} may not change member {}", securityIdentity.getPrincipal().getName(), id);
			throw new ForbiddenException();
		}
		return member;
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
