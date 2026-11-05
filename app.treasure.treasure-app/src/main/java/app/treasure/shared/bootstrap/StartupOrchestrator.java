package app.treasure.shared.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Runs the bootstrap in a fixed order at startup: first the demo users and
 * their members, then (dev mode only) the seeded demo members.
 */
@ApplicationScoped
public class StartupOrchestrator
{
	private static final Logger LOG = LoggerFactory.getLogger(StartupOrchestrator.class);

	@Inject
	BootstrapService bootstrapService;

	@Inject
	DataSeeder dataSeeder;

	@Inject
	BootstrapConfig config;

	void onStart(@Observes StartupEvent event)
	{
		if (!config.enabled())
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
