package app.treasure.shared.bootstrap;

import java.util.List;
import java.util.Map;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * The {@code treasure.bootstrap.*} settings: whether demo users are created at
 * startup and who they are. Each user is a block
 * {@code treasure.bootstrap.users.<key>.*} in application.properties.
 */
@ConfigMapping(prefix = "treasure.bootstrap")
public interface BootstrapConfig
{
	/**
	 * Only dev and test create the demo users. On a stand real people sign in
	 * through the identity provider and get their Member row on first login.
	 */
	@WithDefault("false")
	boolean enabled();

	/** The demo users, keyed by the segment after {@code users.}. */
	Map<String, User> users();

	interface User
	{
		String username();

		String email();

		String firstName();

		String lastName();

		List<String> roles();
	}
}
