package app.treasure.shared.bootstrap;

import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import app.treasure.member.service.KeycloakAdminService;
import app.treasure.organization.domain.Organization;
import app.treasure.organization.repository.OrganizationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap service that ensures the application has the necessary initial
 * data: organizations and users with Member records.
 */
@ApplicationScoped
public class BootstrapService
{
	private static final Logger LOG = LoggerFactory.getLogger(BootstrapService.class);

	@Inject
	OrganizationRepository organizationRepository;

	@Inject
	MemberRepository memberRepository;

	@Inject
	KeycloakAdminService keycloakAdminService;

	// Organization A config
	@ConfigProperty(name = "treasure.bootstrap.org-a.name")
	String orgAName;

	@ConfigProperty(name = "treasure.bootstrap.org-a.slug")
	String orgASlug;

	@ConfigProperty(name = "treasure.bootstrap.org-a.display-name")
	String orgADisplayName;

	// Organization B config
	@ConfigProperty(name = "treasure.bootstrap.org-b.name")
	String orgBName;

	@ConfigProperty(name = "treasure.bootstrap.org-b.slug")
	String orgBSlug;

	@ConfigProperty(name = "treasure.bootstrap.org-b.display-name")
	String orgBDisplayName;

	// User 1: Super Admin config
	@ConfigProperty(name = "treasure.bootstrap.users.admin.username")
	String adminUsername;

	@ConfigProperty(name = "treasure.bootstrap.users.admin.email")
	String adminEmail;

	@ConfigProperty(name = "treasure.bootstrap.users.admin.first-name")
	String adminFirstName;

	@ConfigProperty(name = "treasure.bootstrap.users.admin.last-name")
	String adminLastName;

	@ConfigProperty(name = "treasure.bootstrap.users.admin.roles")
	List<String> adminRoles;

	@ConfigProperty(name = "treasure.bootstrap.users.admin.organization")
	String adminOrganization;

	// User 2: Maria config
	@ConfigProperty(name = "treasure.bootstrap.users.maria.username")
	String mariaUsername;

	@ConfigProperty(name = "treasure.bootstrap.users.maria.email")
	String mariaEmail;

	@ConfigProperty(name = "treasure.bootstrap.users.maria.first-name")
	String mariaFirstName;

	@ConfigProperty(name = "treasure.bootstrap.users.maria.last-name")
	String mariaLastName;

	@ConfigProperty(name = "treasure.bootstrap.users.maria.roles")
	List<String> mariaRoles;

	@ConfigProperty(name = "treasure.bootstrap.users.maria.organization")
	String mariaOrganization;

	// User 3: Thomas config
	@ConfigProperty(name = "treasure.bootstrap.users.thomas.username")
	String thomasUsername;

	@ConfigProperty(name = "treasure.bootstrap.users.thomas.email")
	String thomasEmail;

	@ConfigProperty(name = "treasure.bootstrap.users.thomas.first-name")
	String thomasFirstName;

	@ConfigProperty(name = "treasure.bootstrap.users.thomas.last-name")
	String thomasLastName;

	@ConfigProperty(name = "treasure.bootstrap.users.thomas.roles")
	List<String> thomasRoles;

	@ConfigProperty(name = "treasure.bootstrap.users.thomas.organization")
	String thomasOrganization;

	private record UserConfig(String username, String email, String firstName, String lastName,
		List<String> roles, String organizationSlug)
	{
	}

	@Transactional
	public List<Organization> bootstrapOrganizations()
	{
		LOG.info("Bootstrapping organizations...");

		List<Organization> orgs = new ArrayList<>();

		Organization orgA = ensureOrganization(orgASlug, orgAName, orgADisplayName);
		orgs.add(orgA);

		Organization orgB = ensureOrganization(orgBSlug, orgBName, orgBDisplayName);
		orgs.add(orgB);

		LOG.info("Bootstrapped {} organizations", orgs.size());
		return orgs;
	}

	@Transactional
	public void bootstrapUsers()
	{
		LOG.info("Bootstrapping users...");

		List<UserConfig> users = List.of(new UserConfig(adminUsername, adminEmail, adminFirstName,
			adminLastName, adminRoles, adminOrganization),
			new UserConfig(mariaUsername, mariaEmail, mariaFirstName, mariaLastName, mariaRoles,
				mariaOrganization),
			new UserConfig(thomasUsername, thomasEmail, thomasFirstName, thomasLastName, thomasRoles,
				thomasOrganization));

		for (UserConfig userConfig : users)
		{
			bootstrapUser(userConfig);
		}

		LOG.info("Bootstrapped {} users", users.size());
	}

	private void bootstrapUser(UserConfig config)
	{
		Organization org = organizationRepository.findBySlug(config.organizationSlug());
		if (org == null)
		{
			LOG.error("Cannot bootstrap user {} - organization '{}' not found", config.username(),
				config.organizationSlug());
			return;
		}

		Member existingMember = memberRepository.findByUsername(config.username());
		if (existingMember != null)
		{
			LOG.info("User {} already exists (Member ID: {})", config.username(), existingMember.getId());
			return;
		}

		String keycloakUserId = findOrCreateKeycloakUser(config, 10);
		if (keycloakUserId == null)
		{
			LOG.warn("Could not find or create Keycloak user for {}", config.username());
			return;
		}

		createIfNotExisting(config, org, keycloakUserId);
	}

	private String findOrCreateKeycloakUser(UserConfig config, int maxRetries)
	{
		String existingUserId = findKeycloakUserWithRetry(config.username(), maxRetries, 1000);
		if (existingUserId != null)
		{
			LOG.info("Found existing Keycloak user: {}", config.username());
			return existingUserId;
		}

		try
		{
			String newUserId = keycloakAdminService.createUser(config.username(), config.email(),
				config.firstName(), config.lastName(), config.roles());
			LOG.info("Created Keycloak user: {} (id: {})", config.username(), newUserId);
			return newUserId;
		}
		catch (RuntimeException e)
		{
			if (e.getMessage() != null && e.getMessage().contains("User exists"))
			{
				LOG.info("User was created concurrently, fetching: {}", config.username());
				String userId = keycloakAdminService.findUserIdByUsername(config.username());
				if (userId != null)
				{
					return userId;
				}
			}
			LOG.error("Failed to create Keycloak user: {}", config.username(), e);
			throw e;
		}
	}

	private String findKeycloakUserWithRetry(String username, int maxAttempts, long delayMs)
	{
		for (int attempt = 1; attempt <= maxAttempts; attempt++)
		{
			String userId = keycloakAdminService.findUserIdByUsername(username);
			if (userId != null)
			{
				if (attempt > 1)
				{
					LOG.info("Found Keycloak user '{}' on attempt {}", username, attempt);
				}
				return userId;
			}

			if (attempt < maxAttempts)
			{
				LOG.debug("Keycloak user '{}' not found, retrying in {}ms (attempt {}/{})", username,
					delayMs, attempt, maxAttempts);
				try
				{
					Thread.sleep(delayMs);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					LOG.warn("Interrupted while waiting to retry finding Keycloak user '{}'", username);
					return null;
				}
			}
		}
		return null;
	}

	@Transactional(TxType.REQUIRES_NEW)
	void createIfNotExisting(UserConfig config, Organization org, String keycloakUserId)
	{
		Member existingByUsername = memberRepository.findByUsername(config.username());
		if (existingByUsername != null)
		{
			LOG.info("Member {} already exists", existingByUsername.getUserName());
			return;
		}

		try
		{
			Member newMember = new Member();
			newMember.setUserName(config.username);
			newMember.setEmail(config.email());
			newMember.setFirstName(config.firstName());
			newMember.setLastName(config.lastName());
			newMember.setOrganization(org);
			newMember.setKeycloakUserId(keycloakUserId);

			newMember.setInviteType("BOOTSTRAP");
			newMember.setJoinedAt(Instant.now());

			memberRepository.persist(newMember);

			LOG.info(
				"Created member: {} {} ({}) linked to Keycloak user: {} (ID: {}) with inviteType=BOOTSTRAP",
				newMember.getFirstName(), newMember.getLastName(), newMember.getEmail(), config.username(),
				keycloakUserId);
		}
		catch (Exception e)
		{
			if (e.getMessage() != null
				&& (e.getMessage().contains("unique") || e.getMessage().contains("duplicate")))
			{
				LOG.info("Member {} was created concurrently, skipping creation", config.username());
			}
			else
			{
				LOG.error("Failed to create member: {}", config.username(), e);
				throw e;
			}
		}
	}

	private Organization ensureOrganization(String slug, String name, String displayName)
	{
		Organization org = organizationRepository.findBySlug(slug);
		if (org != null)
		{
			LOG.debug("Organization already exists: {}", org.getName());
			return org;
		}

		org = new Organization();
		org.setName(name);
		org.setSlug(slug);
		org.setDisplayName(displayName);
		organizationRepository.persist(org);

		LOG.info("Created organization: {} (slug: {})", org.getName(), org.getSlug());
		return org;
	}
}
