package app.treasure.shared.security;

/**
 * The roles the identity provider sends: Keycloak realm roles in dev and test,
 * Entra App Roles on a stand. Use these constants with {@code @RolesAllowed}
 * and {@code hasRole} instead of literal strings.
 */
public interface Roles
{
	/** Full access. */
	String SUPER_ADMIN = "super_admin";

	/** Changes devices, assignments and members; creates API keys. */
	String ADMIN = "admin";

	/** Reads everything and changes only their own profile. */
	String USER = "user";
}
