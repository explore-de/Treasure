package app.treasure.shared.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Seeds demo members for development mode, so the assign menus have people in
 * them from the first start.
 */
@ApplicationScoped
public class DataSeeder
{
	private static final Logger LOG = LoggerFactory.getLogger(DataSeeder.class);

	@Inject
	MemberRepository memberRepository;

	@Transactional
	public void seedDemoData()
	{
		if (memberRepository.findByUsername("max.mustermann") != null)
		{
			return;
		}
		createMember("Max", "Mustermann", "max.mustermann", "max.mustermann@treasure.local", "+49 89 123456");
		createMember("Lisa", "Schmidt", "lisa.schmidt", "lisa.schmidt@treasure.local", null);
		createMember("Anna", "Weber", "anna.weber", "anna.weber@treasure.local", "+49 89 654321");
		createMember("Peter", "Huber", "peter.huber", "peter.huber@treasure.local", null);
		LOG.info("Seeded demo members");
	}

	private void createMember(String firstName, String lastName, String userName, String email, String phone)
	{
		Member member = new Member();
		member.setFirstName(firstName);
		member.setLastName(lastName);
		member.setUserName(userName);
		member.setEmail(email);
		member.setPhone(phone);
		memberRepository.persist(member);
	}
}
