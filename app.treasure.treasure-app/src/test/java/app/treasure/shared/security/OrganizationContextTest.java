package app.treasure.shared.security;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import app.treasure.member.domain.Member;
import app.treasure.organization.domain.Organization;
import app.treasure.member.repository.MemberRepository;

import org.junit.jupiter.api.Test;

import io.quarkus.security.identity.SecurityIdentity;

import java.security.Principal;

class OrganizationContextTest
{
	@Test
	void shouldThrowWhenUserIsAnonymous()
	{
		OrganizationContext context = new OrganizationContext();

		SecurityIdentity identity = mock(SecurityIdentity.class);
		when(identity.isAnonymous()).thenReturn(true);

		context.securityIdentity = identity;

		assertThrows(IllegalStateException.class, context::getCurrentOrganization);
	}

	@Test
	void shouldReturnOrganizationForRegularUser()
	{
		OrganizationContext context = new OrganizationContext();

		SecurityIdentity identity = mock(SecurityIdentity.class);
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.hasRole(Roles.SUPER_ADMIN)).thenReturn(false);

		Principal principal = mock(Principal.class);
		when(principal.getName()).thenReturn("maria");
		when(identity.getPrincipal()).thenReturn(principal);

		MemberRepository memberRepo = mock(MemberRepository.class);
		Organization org = new Organization();
		org.setName("Test Org");
		Member member = new Member();
		member.setOrganization(org);
		when(memberRepo.findByUsername("maria")).thenReturn(member);

		context.securityIdentity = identity;
		context.memberRepository = memberRepo;

		Organization result = context.getCurrentOrganization();
		assertSame(org, result);
	}

	@Test
	void shouldThrowWhenMemberNotFound()
	{
		OrganizationContext context = new OrganizationContext();

		SecurityIdentity identity = mock(SecurityIdentity.class);
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.hasRole(Roles.SUPER_ADMIN)).thenReturn(false);

		Principal principal = mock(Principal.class);
		when(principal.getName()).thenReturn("maria");
		when(identity.getPrincipal()).thenReturn(principal);

		MemberRepository memberRepo = mock(MemberRepository.class);
		when(memberRepo.findByUsername("maria")).thenReturn(null);

		context.securityIdentity = identity;
		context.memberRepository = memberRepo;

		assertThrows(IllegalStateException.class, context::getCurrentOrganization);
	}

	@Test
	void shouldThrowWhenMemberIsNotSuperAdminWhileSwitching()
	{
		OrganizationContext context = new OrganizationContext();

		SecurityIdentity identity = mock(SecurityIdentity.class);
		when(identity.isAnonymous()).thenReturn(false);
		when(identity.hasRole(Roles.SUPER_ADMIN)).thenReturn(false);

		context.securityIdentity = identity;
		assertThrows(SecurityException.class, () -> context.switchOrganization(1L));
	}

	@Test
	void shouldReturnTrueWhenUserIsSuperAdmin()
	{
		OrganizationContext context = new OrganizationContext();

		SecurityIdentity identity = mock(SecurityIdentity.class);
		when(identity.hasRole(Roles.SUPER_ADMIN)).thenReturn(true);
		context.securityIdentity = identity;

		assertTrue(context.isSuperAdmin());
	}

	@Test
	void shouldReturnTrueWhenUserIsAdmin()
	{
		OrganizationContext context = new OrganizationContext();

		SecurityIdentity identity = mock(SecurityIdentity.class);
		when(identity.hasRole(Roles.ADMIN)).thenReturn(true);
		when(identity.hasRole(Roles.SUPER_ADMIN)).thenReturn(false);
		context.securityIdentity = identity;

		assertTrue(context.isAdmin());
	}
}