package app.treasure.shared.bootstrap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Unified startup orchestrator that coordinates bootstrap sequence in
 * deterministic order.
 * <p>
 * Execution order: 1. Bootstrap users + members 2. DEV mode only: Seed demo
 * data
 */
@ApplicationScoped
public class StartupOrchestrator
{
	private static final Logger LOG = LoggerFactory.getLogger(StartupOrchestrator.class);

	@Inject
	BootstrapService bootstrapService;

	@Inject
	DataSeeder dataSeeder;

	/**
	 * Only dev and test create the demo users. On a stand real people sign in
	 * through the identity provider and get their Member row on first login.
	 */
	@ConfigProperty(name = "treasure.bootstrap.enabled", defaultValue = "false")
	boolean bootstrapEnabled;

	/**
	 * Called when the application starts. Runs the bootstrap process in a
	 * deterministic order.
	 *
	 * @param event
	 *            The startup event
	 */
	void onStart(@Observes StartupEvent event)
	{
		if (!bootstrapEnabled)
		{
			LOG.info("Bootstrap disabled (treasure.bootstrap.enabled=false); members are created on first login");
			return;
		}
		LOG.info("Application starting - running bootstrap...");

		try
		{
			// 1. Create users + members
			bootstrapService.bootstrapUsers();
			LOG.info("Bootstrapped users and members");

			// 2. DEV ONLY: Seed demo data
			if (LaunchMode.current() == LaunchMode.DEVELOPMENT)
			{
				dataSeeder.seedDemoData();
				LOG.info("Seeded demo data for development mode");
			}

			LOG.info("Bootstrap completed successfully");
		}
		catch (Exception e)
		{
			LOG.error("Bootstrap failed during startup", e);
			// Don't prevent app startup, but log the error
		}
	}
}
