package app.treasure.shared.security;

/**
 * The one role the app knows, sent by the identity provider: a Keycloak realm
 * role in dev and test, an Entra App Role on a stand. Everyone who is signed in
 * without it reads everything and changes only their own profile; there is no
 * role for that. Use the constant with {@code @RolesAllowed} and
 * {@code hasRole} instead of a literal string.
 */
public interface Roles
{
	/** Changes devices, assignments and members; creates API keys. */
	String ADMIN = "treasure-admin";
}
