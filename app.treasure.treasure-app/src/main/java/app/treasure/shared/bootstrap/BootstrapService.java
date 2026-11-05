package app.treasure.shared.bootstrap;

import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import app.treasure.member.service.KeycloakAdminService;
import app.treasure.shared.bootstrap.BootstrapConfig.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Creates the demo users of dev and test mode in Keycloak and as Members, as
 * configured in {@link BootstrapConfig}.
 */
@ApplicationScoped
public class BootstrapService
{
	private static final Logger LOG = LoggerFactory.getLogger(BootstrapService.class);

	@Inject
	MemberRepository memberRepository;

	@Inject
	KeycloakAdminService keycloakAdminService;

	@Inject
	BootstrapConfig config;

	@Transactional
	public void bootstrapUsers()
	{
		LOG.info("Bootstrapping users...");

		for (User user : config.users().values())
		{
			bootstrapUser(user);
		}

		LOG.info("Bootstrapped {} users", config.users().size());
	}

	private void bootstrapUser(User user)
	{
		Member existingMember = memberRepository.findByUsername(user.username());
		if (existingMember != null)
		{
			LOG.info("User {} already exists (Member ID: {})", user.username(), existingMember.getId());
			return;
		}

		String keycloakUserId = findOrCreateKeycloakUser(user, 10);
		if (keycloakUserId == null)
		{
			LOG.warn("Could not find or create Keycloak user for {}", user.username());
			return;
		}

		createIfNotExisting(user, keycloakUserId);
	}

	private String findOrCreateKeycloakUser(User user, int maxRetries)
	{
		String existingUserId = findKeycloakUserWithRetry(user.username(), maxRetries, 1000);
		if (existingUserId != null)
		{
			LOG.info("Found existing Keycloak user: {}", user.username());
			return existingUserId;
		}

		try
		{
			String newUserId = keycloakAdminService.createUser(user.username(), user.email(),
				user.firstName(), user.lastName(), user.roles());
			LOG.info("Created Keycloak user: {} (id: {})", user.username(), newUserId);
			return newUserId;
		}
		catch (RuntimeException e)
		{
			if (e.getMessage() != null && e.getMessage().contains("User exists"))
			{
				LOG.info("User was created concurrently, fetching: {}", user.username());
				String userId = keycloakAdminService.findUserIdByUsername(user.username());
				if (userId != null)
				{
					return userId;
				}
			}
			LOG.error("Failed to create Keycloak user: {}", user.username(), e);
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
	void createIfNotExisting(User user, String keycloakUserId)
	{
		Member existingByUsername = memberRepository.findByUsername(user.username());
		if (existingByUsername != null)
		{
			LOG.info("Member {} already exists", existingByUsername.getUserName());
			return;
		}

		try
		{
			Member newMember = new Member();
			newMember.setUserName(user.username());
			newMember.setEmail(user.email());
			newMember.setFirstName(user.firstName());
			newMember.setLastName(user.lastName());
			newMember.setSubject(keycloakUserId);

			newMember.setInviteType(Member.INVITE_TYPE_BOOTSTRAP);
			newMember.setJoinedAt(Instant.now());

			memberRepository.persist(newMember);

			LOG.info("Created member: {} {} ({}) linked to Keycloak user: {} (ID: {}) with inviteType={}",
				newMember.getFirstName(), newMember.getLastName(), newMember.getEmail(), user.username(),
				keycloakUserId, Member.INVITE_TYPE_BOOTSTRAP);
		}
		catch (Exception e)
		{
			if (e.getMessage() != null
				&& (e.getMessage().contains("unique") || e.getMessage().contains("duplicate")))
			{
				LOG.info("Member {} was created concurrently, skipping creation", user.username());
			}
			else
			{
				LOG.error("Failed to create member: {}", user.username(), e);
				throw e;
			}
		}
	}
}
