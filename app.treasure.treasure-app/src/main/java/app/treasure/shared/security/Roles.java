package app.treasure.shared.security;

/**
 * Role constants used throughout the application for authorization.
 * <p>
 * These constants match the roles defined in Keycloak and should be used with
 *
 * @RolesAllowed annotations instead of hardcoded strings.
 *               </p>
 */
public interface Roles
{
	/**
	 * Super administrator role - full system access
	 */
	String SUPER_ADMIN = "super_admin";

	/**
	 * Administrator role - can manage users and settings
	 */
	String ADMIN = "admin";

	/**
	 * Member role - standard user access
	 */
	String MEMBER = "member";
}
