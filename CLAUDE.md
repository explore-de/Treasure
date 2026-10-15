# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with
code in this repository.

## What this project is

Treasure is a device-management web app for organizations (tenants): a
hardware inventory, assignment of devices to members, and a change history of
who changed what and when. `README.md` is the authoritative overview and
`CONTRIBUTING.md` holds the house rules.

The repository contains **one** Maven project, `app.treasure.treasure-app/`,
plus the shared formatter config in `formatter/` and CI in `.github/`. There
are no other modules or microservices.

## Build Commands

All Maven commands run from `app.treasure.treasure-app/`:

```bash
./mvnw quarkus:dev              # dev mode with live reload, http://localhost:8080
./mvnw test                     # run tests
./mvnw test -Dtest=ClassName    # run a single test class
./mvnw verify                   # tests + JaCoCo report (what CI runs)
./mvnw package                  # builds target/quarkus-app/quarkus-run.jar
./mvnw formatter:validate       # check formatting
./mvnw formatter:format         # apply formatting
```

Quarkus Dev Services start PostgreSQL, Keycloak and LocalStack in Docker
automatically in dev and test mode. There is no compose file; Docker must be
running.

`formatter:validate` is bound to the `validate` phase, so every build
(including the IDE's background import) fails on unformatted code. Run
`./mvnw formatter:format` before committing.

Opening the folder in VS Code triggers a background import and compile by the
Java language server. That fills `target/` but does not start the app.

## Architecture

**Stack:**

- **Quarkus 3.31.2**, Java 21
- **Renarde 3.1.5** - server-side web framework on Qute + RESTEasy Reactive
- **Hibernate ORM Panache** with the repository pattern
- **PostgreSQL**, schema owned by **Flyway** (`src/main/resources/db/migration`,
  Hibernate only validates). Never edit an applied migration; add a new
  versioned file.
- **Keycloak** via OIDC (`application-type=web-app`) plus the Keycloak Admin
  REST client for user management
- **htmx 2.0.4** for row-level partial page updates
- **SmallRye Health** at `/q/health`, **SmallRye OpenAPI** at `/q/swagger-ui`
- Declared but currently unused by any Java code: `quarkus-amazon-s3`
  (LocalStack still starts in dev), `quarkus-mailer`, `quarkus-scheduler`.
  The `templates/EmailService/` templates have no matching Java class.

**Package layout** (feature-based, under `src/main/java/app/treasure/`):

```
dashboard/api        DashboardResource: "/" redirects to /devices
device/
  api                DeviceResource: all /devices routes (list, create, edit, assign, bulk actions)
  domain             Device, DeviceHistory (every field change is recorded)
  filter             DeviceFilter, DeviceFieldFilter (list filtering)
  repository         DeviceRepository, DeviceHistoryRepository
member/
  domain             Member (belongs to an Organization, linked to a Keycloak user)
  repository         MemberRepository
  service            KeycloakAdminService, MemberKeycloakSyncService
organization/
  domain             Organization (the tenant)
  repository         OrganizationRepository
shared/
  bootstrap          StartupOrchestrator -> BootstrapService (orgs, users, members) -> DataSeeder (dev demo data)
  domain/repository  Tag, TagRepository
  security           Roles, OrganizationContext (resolves the current tenant per request)
  template           UserContext (available in templates as inject:userContext)
  util               FlashKeys, JavaExtensions (Qute @TemplateExtension), SlugUtil, SortHelper
```

**Multi-tenancy:** a Member belongs to an Organization; a Device reaches its
tenant through the Member it is assigned to. `OrganizationContext` returns the
member's organization for normal users and a session-selected organization for
super admins.

**Dev users** (provisioned by the Keycloak dev service and `BootstrapService`):
`admin` (super_admin, admin, user), `maria` (admin, user), `thomas` (user),
all with password `password`. See `README.md` for the organizations.

**Templates:** `src/main/resources/templates/<ControllerName>/<method>.html`.
Layout is `main.html`; reusable tags live in `templates/tags/` (`header`,
`flashNotifications`, `carbonIcon`, `iconPicker`).

## Renarde & Qute Conventions

**Controllers:**

- Extend `Controller` from Renarde, annotate the class with `@Authenticated`
  and `@Path`
- Declare templates in a `@CheckedTemplate public static class Templates`
  with `native` methods, one per template
- A native method named `index$row(...)` renders the `{#fragment id=row}`
  block of `index.html`. htmx posts to `/devices/{id}/assign` and
  `/devices/{id}/unassign` and swaps that single table row
- Form fields arrive via `@RestForm` parameters; POST handlers are
  `@Transactional`
- After a POST, redirect with `seeOther("/devices")`
- Flash messages: `flash(FlashKeys.SUCCESS, "...")` or `FlashKeys.ERROR`;
  `tags/flashNotifications.html` renders those two keys
- Record every device mutation in `DeviceHistory` through the `recordChange`
  / `recordIfChanged` helpers in `DeviceResource`

**Qute CheckedTemplate limitations:**

- NO direct Java method calls like `ZoneId.systemDefault()` in templates
- NO safe navigation operator `?.`; use the Elvis operator `?:` instead
- Put presentation helpers on the entity (`getFormattedPickupTime()`,
  `getBookedName()`) or in `JavaExtensions` (`capitalise`, `formatGerman`)

```java
// In entity class
public String getFormattedPickupTime()
{
	return pickupTime != null ? pickupTime.format(formatter) : "";
}
```

**Template injections:** `inject:userContext.username`, `.email`, `.roles`,
`.firstLetter`, `.isSuperAdmin` and `inject:vertxRequest.path` (used for the
active side-nav entry).

**Logging:**

**IMPORTANT:** Always use SLF4J for logging. Never use `System.out.println()`
or other logging frameworks.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger LOG = LoggerFactory.getLogger(MyClass.class);
```

Log levels:
- `LOG.error()` - failures and exceptions that need attention
- `LOG.warn()` - issues that might cause problems
- `LOG.info()` - important operations (user actions, service calls, state changes)
- `LOG.debug()` - detailed debugging information

Add reasonable logging for create/update/delete, external calls (Keycloak),
state changes and error conditions.

**Roles and Security:**

**IMPORTANT:** Always use role constants from `Roles.java` instead of
hardcoded strings.

```java
import app.treasure.shared.security.Roles;

if (securityIdentity.hasRole(Roles.ADMIN)) {
    // Admin-only operations
}
```

- `Roles.SUPER_ADMIN` = "super_admin" - full access, can switch organizations
- `Roles.ADMIN` = "admin" - organization administrator
- `Roles.MEMBER` = "member" - regular member

**Note:** the Keycloak dev users carry the roles `super_admin`, `admin` and
`user`, and the existing tests use `@TestSecurity(user = "maria", roles =
"user")`. Use the `Roles` constants in new code.

**Form handling:**

- Every POST form needs `{#authenticityToken /}` for CSRF protection
- Use `@RestForm` for form parameters

## Testing

**Patterns:**

- `@QuarkusTest` on the class, `@TestSecurity(user = ..., roles = ...)` on
  methods that hit authenticated routes
- Name tests `shouldDoSomething()`
- RESTAssured for HTTP, Hamcrest matchers or JUnit assertions
- Helper methods annotated `@Transactional(TxType.REQUIRES_NEW)` for test data
- Extend `BaseOrganizationTest` when a test needs an organization, members or
  tags; it provides `getOrCreateTestOrganization()`, `createOrganization()`,
  `createTestMember()`, `createTags()`
- `TestSecurityHelper.TEST_USER_MARIA` holds the default test username
- Clean up what a test created (see the `try/finally` blocks in
  `DeviceResourceTest`)
- Tests tagged `integration` are excluded by Surefire

**Example:**

```java
@QuarkusTest
class DeviceResourceTest
{
	@Inject
	DeviceRepository deviceRepository;

	@Test
	@TestSecurity(user = "maria", roles = "user")
	void shouldOpenDeviceList()
	{
		given()
			.when().get("/devices")
			.then()
			.statusCode(200);
	}

	@Transactional(TxType.REQUIRES_NEW)
	void deleteDevices(Long... ids)
	{
		for (Long id : ids)
		{
			deviceRepository.deleteById(id);
		}
	}
}
```

**Testing forms with CSRF:** POST endpoints require the CSRF token. Test GET
endpoints and verify the form is present, and test business logic through the
repositories or services directly.

## CI

- `.github/workflows/treasure-app.yml` runs `./mvnw -B verify` and pushes a
  Docker image to GHCR on every push to `main` or `deploy` that touches the
  app
- `.github/workflows/claude-code-review.yml` runs an automated review on every
  pull request
- `.github/workflows/claude.yml` runs when `@claude` is mentioned in an issue
  or PR comment
- Dependabot opens daily grouped update PRs for Maven, Docker and Actions

## Frontend

This application uses the [Carbon Design System](https://carbondesignsystem.com/)
with [@carbon/web-components](https://web-components.carbondesignsystem.com/).

**Important:** Use actual Carbon web components, not custom HTML/CSS mimicking
Carbon styles.

### Loading

`main.html` loads the Carbon stylesheet from a local copy
(`/css/carbon-styles.min.css`) and the web components from the IBM CDN at
`https://1.www.s81c.com/common/carbon/web-components/tag/v2/latest/<component>.min.js`,
one script per component (ui-shell, button, text-input, number-input, select,
date-picker, checkbox, textarea, tile, tag, notification, tree-view, dropdown,
data-table, file-uploader, loading, ai-label). Add a new component by adding
its script there. Chart.js 4.4.0 and htmx 2.0.4 are loaded the same way.

App styles live in `treasure-core.css` (tokens and brand colours),
`treasure-components.css` (buttons, notifications) and `treasure-pages.css`
(page layouts). A light/dark toggle switches the `cds--g100` class on `<html>`.

### Form components

```html
<cds-text-input label="Name" name="deviceName" value="{device.deviceName ?: ''}">
</cds-text-input>

<cds-select label="Status" name="status">
  <cds-select-item value="available">available</cds-select-item>
</cds-select>

<cds-date-picker>
  <cds-date-picker-input label="Abholzeit" name="pickupTime" placeholder="dd.mm.yyyy">
  </cds-date-picker-input>
</cds-date-picker>

<cds-checkbox name="visible" label-text="Sichtbar" {#if device.visible}checked{/if}>
</cds-checkbox>

<cds-textarea label="Beschreibung" name="extraInfo" rows="4" value="{device.extraInfo ?: ''}">
</cds-textarea>
```

### Buttons

**IMPORTANT:** Always use native `<button>` elements for form submissions and
actions. Use `<cds-button>` ONLY for navigation links (with `href`). The
`<cds-button type="submit">` web component does not reliably submit forms.

```html
<!-- Form submit - native button -->
<button type="submit" class="cds-btn cds-btn-primary">Speichern</button>

<!-- Destructive action - native button -->
<button type="submit" class="cds-btn cds-btn-danger">Löschen</button>

<!-- Navigation - cds-button -->
<cds-button href="/devices" kind="secondary">Abbrechen</cds-button>
```

Classes `.cds-btn`, `.cds-btn-primary`, `.cds-btn-secondary` and
`.cds-btn-danger` are defined in `treasure-components.css`.

### Notifications

```html
<cds-inline-notification kind="success" title="Erfolg" subtitle="{flash:success}">
</cds-inline-notification>
```

### Carbon UI Shell (navigation)

`main.html` renders `<cds-header>` and `<cds-side-nav collapse-mode="fixed">`.
Carbon's side nav is `position: fixed` and does NOT offset the page content.
The `.app-container` gets its `margin-left` from a `body.side-nav-expanded`
class that a `MutationObserver` in `main.html` toggles when the nav's
`expanded` attribute changes. Keep that mechanism when touching the layout.

Use `collapse-mode="fixed"` for the main navigation, not `rail`, which
collapses as soon as the mouse leaves.

### Colours

Use the CSS custom properties from `treasure-core.css` instead of hardcoded
colours:

```css
var(--treasure-primary)        /* #FF521D - Treasure orange */
var(--treasure-primary-hover)  /* #E6491A */
var(--treasure-primary-active) /* #CC4017 */
var(--treasure-primary-light)  /* #FFE5DC */
```

Carbon status colours: green `#24a148` (success), red `#da1e28` (error),
yellow `#f1c21b` (warning). For Chart.js use the Treasure orange as the
primary series colour.

### Qute syntax with Carbon

```html
<!-- Conditional attribute -->
<cds-side-nav-link href="/devices" {#if inject:vertxRequest.path.startsWith('/devices')}active{/if}>

<!-- Null-safe value: Elvis, never ?. -->
<cds-text-input value="{device.deviceName ?: ''}"></cds-text-input>

<!-- Loop -->
{#for member in members}
<cds-select-item value="{member.id}">{member.fullName}</cds-select-item>
{/for}
```

### Loading states

```html
<cds-inline-loading status="active"></cds-inline-loading>
<cds-loading></cds-loading>
```

## Common Gotchas

1. **Formatter runs on the validate phase** - run `./mvnw formatter:format`
   before committing
2. **CheckedTemplate expressions** - no Java calls and no `?.` in templates
3. **CSRF tokens** - every POST form needs `{#authenticityToken /}`
4. **.gitignore patterns** - use `/tags` not `tags` to avoid ignoring
   `templates/tags/`
5. **Carbon UI Shell layout** - the side nav does not offset content; see the
   Frontend section
6. **Qute section blocks** - `{#moreStyles}` must close with `{/moreStyles}`,
   not `{/if}`. Mismatched closing tags cause template parser errors
7. **Carbon collapse modes** - use `collapse-mode="fixed"` for the main
   navigation
8. **Port 8080 already in use** - a previous dev-mode run is still alive; see
   the Troubleshooting section of `README.md`

## Git

- Gitmoji with the official emoji as Unicode and a scope:
  `✨ (device): Add assignment filter`, followed by a longer description or
  bullet list if needed
- Branches: `feature/`, `fix/`, `refactor/`, `docs/`

## Dialog scoped

u are my teacher and u have to teach me during the session, if i ask u u give me code or write it down for me, but when I learn u should not write it for me, you are always allowed to watch my code
