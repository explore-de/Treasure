package app.treasure.shared.bootstrap;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import app.treasure.organization.domain.Organization;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Seeds demo data (members) for development mode.
 */
@ApplicationScoped
public class DataSeeder
{
	private static final Logger LOG = LoggerFactory.getLogger(DataSeeder.class);

	@Inject
	MemberRepository memberRepository;

	@Transactional
	public void seedDemoData(List<Organization> orgs)
	{
		for (Organization org : orgs)
		{
			if ("musikverein-harmonie".equals(org.getSlug()))
			{
				seedMusikvereinsDemo(org);
			}
			else if ("sportverein-alpenblick".equals(org.getSlug()))
			{
				seedSportvereinDemo(org);
			}
		}
	}

	private void seedMusikvereinsDemo(Organization org)
	{
		if (memberRepository.findByUsername("max.mustermann") == null)
		{
			createMember("Max", "Mustermann", "max.mustermann",
				"max.mustermann@harmonie.local", "+49 89 123456", org);
			createMember("Lisa", "Schmidt", "lisa.schmidt",
				"lisa.schmidt@harmonie.local", null, org);

			LOG.info("Seeded demo data for organization: {}", org.getName());
		}
	}

	private void seedSportvereinDemo(Organization org)
	{
		if (memberRepository.findByUsername("anna.weber") == null)
		{
			createMember("Anna", "Weber", "anna.weber",
				"anna.weber@alpenblick.local", "+49 89 654321", org);
			createMember("Peter", "Huber", "peter.huber",
				"peter.huber@alpenblick.local", null, org);

			LOG.info("Seeded demo data for organization: {}", org.getName());
		}
	}

	private Member createMember(String firstName, String lastName, String userName, String email,
		String phone, Organization org)
	{
		Member member = new Member();
		member.setFirstName(firstName);
		member.setLastName(lastName);
		member.setUserName(userName);
		member.setEmail(email);
		member.setPhone(phone);
		member.setOrganization(org);
		memberRepository.persist(member);
		return member;
	}
}
