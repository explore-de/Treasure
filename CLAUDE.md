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

Quarkus Dev Services start PostgreSQL and Keycloak in Docker automatically in
dev and test mode. There is no compose file; Docker must be running.

`formatter:validate` is bound to the `validate` phase, so every build
(including the IDE's background import) fails on unformatted code. Run
`./mvnw formatter:format` before committing.

Opening the folder in VS Code triggers a background import and compile by the
Java language server. That fills `target/` but does not start the app.

## Architecture

**Stack:**

- **Quarkus 3.40.1.1**, Java 25
- **Renarde 3.1.10** - server-side web framework on Qute + RESTEasy Reactive
- **Hibernate ORM Panache** with the repository pattern
- **PostgreSQL**, schema owned by **Flyway** (`src/main/resources/db/migration`,
  Hibernate only validates). Never edit an applied migration; add a new
  versioned file.
- **OIDC** authorization-code flow (`application-type=web-app`): Keycloak Dev
  Services in dev and test, Microsoft Entra ID under the `prod` profile (see
  the `%prod.quarkus.oidc.*` block and the README's deployment section). Roles
  come from the token: Keycloak realm roles in dev, Entra App Roles via
  `roles.role-claim-path=roles` on a stand. The session cookie key comes from
  `OIDC_SESSION_SECRET` on the stand and a fixed dev-only value in dev and test
- **htmx 2.0.4** for row-level partial page updates
- **SmallRye Health** at `/q/health`

**Package layout** (feature-based, under `src/main/java/app/treasure/`):

```
dashboard/api        DashboardResource: "/" overview (counts, devices per group, longest held, recent activity)
device/
  api                DeviceResource: all /devices routes (list, create, edit, assign, bulk actions)
  domain             Device, DeviceHistory (every field change is recorded)
  filter             DeviceFilter, DeviceFieldFilter (list filtering)
  repository         DeviceRepository, DeviceHistoryRepository
member/
  api                MemberResource: /members, everyone who can hold a device and what they hold
  domain             Member (userName = OIDC principal name; subject = provider user id)
  repository         MemberRepository
  service            KeycloakAdminService (dev bootstrap only), MemberProvisioningService (first-login Member creation)
shared/
  bootstrap          StartupOrchestrator -> BootstrapService (users, members) -> DataSeeder; only when treasure.bootstrap.enabled
  domain/repository  Tag, TagRepository (the device-group registry)
  security           Roles, FirstLoginFilter (creates the Member for an unknown OIDC principal)
  template           UserContext (available in templates as inject:userContext)
  util               FlashKeys
```

**Device groups:** `Device.group` is a plain string. Every group that is
saved through the form is also registered in the `Tag` table
(`DeviceResource.rememberGroup`), and the form offers the union of those tags
and the groups still present on devices. That is why a group survives after
the last device leaves it. There is no UI to delete a group yet.

**No organizations (decided 2026-10-15).** The original use cases never
mention tenants, so the organization model inherited from the earlier
accounting app was removed entirely: entity, columns, context class, bootstrap
clubs and tests. Migration `V1.0.2__remove_organizations.sql` dropped the
table and columns. Everyone sees every device and every member.

**Page composition:** the Members page assigns and unassigns through the
existing bulk endpoints `/devices/assign-many` and `/devices/unassign-many`
with a `redirectUrl` of `/members`; `DeviceResource.safeRedirect` accepts any
local path. Overview and Members pass small records (`GroupCount`,
`MemberRow`) to their templates; Qute reads record components like
properties.

**Dev users** (provisioned by the Keycloak dev service and `BootstrapService`):
`admin` (super_admin, admin, user), `maria` (admin, user), `thomas` (user),
all with password `password`. `DataSeeder` adds four demo members in dev mode.
`treasure.bootstrap.enabled` is true only under `%dev` and `%test`; on a stand
nothing is bootstrapped and `FirstLoginFilter` creates each person's Member
from the ID token (`preferred_username`, `email`, `given_name`,
`family_name`, `oid`). `@TestSecurity` identities carry no token and are
skipped by the filter, so tests create members themselves.

**Templates:** `src/main/resources/templates/<ControllerName>/<method>.html`.
Layout is `main.html`; reusable tags live in `templates/tags/` (`header`,
`flashNotifications`, `deviceFormFields`).
`create.html` and `edit.html` share the form through
`{#deviceFormFields device=device groups=groups /}`; the create page passes an
empty `new Device()` as the form-backing object.

**Static assets:** `src/main/resources/META-INF/resources/`. The behaviour of
the device pages (search, filter panel, selection, assign menus, dialogs,
flash auto-dismiss) lives in `js/devices.js`, loaded by `main.html`. Keep page
JavaScript in static files: Qute parses `{` in inline scripts as expressions.

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
- After a POST, redirect with `seeOther("/devices")` and leave a message with
  `flash(FlashKeys.SUCCESS, "...")` or `flash(FlashKeys.ERROR, "...")`.
  `main.html` includes `tags/flashNotifications.html`, which renders the
  `success`, `error`, `warning` and `info` keys as Carbon notifications
- htmx responses (`index$row`) cannot carry a flash; the swapped row is the
  feedback
- Record every device mutation in `DeviceHistory` through the `recordChange`
  / `recordIfChanged` helpers in `DeviceResource`

**Qute CheckedTemplate limitations:**

- NO direct Java method calls like `ZoneId.systemDefault()` in templates
- NO safe navigation operator `?.`; use the Elvis operator `?:` instead
- Put presentation helpers on the entity (`getFormattedPickupTime()`,
  `getBookedName()`, `getDaysHeld()`)

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

- `Roles.SUPER_ADMIN` = "super_admin" - full access
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
- Extend `BaseMemberTest` when a test needs members; it provides
  `createTestMember(username)`
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

`main.html` loads the Carbon stylesheet from a vendored copy of
`@carbon/styles` 1.118.0 (`/css/carbon-styles.min.css`) and only the web
components the app renders from the IBM CDN, pinned to 2.66.0:
`https://1.www.s81c.com/common/carbon/web-components/version/v2.66.0/<component>.min.js`
for `ui-shell` (header and side nav) and `notification` (flash messages). Add a
script there before using another `cds-*` element, and bump both the
stylesheet and the pinned version together. htmx 2.0.11 and `/js/devices.js`
are loaded the same way.

App styles live in `treasure-core.css` (brand tokens, UI shell layout, account
panel) and `treasure-pages.css` (everything on the pages: buttons `.btn*`, toolbar,
filter panel, chips, bulk bar, `.device-table`, `.menu`, `dialog.modal`,
`.form-card` and `.field*`). `treasure-pages.css` defines surface tokens
(`--surface`, `--hover`, `--field`, `--success-bg`, ...) once for light and
again under `body.cds--g100` for dark, so new styles should use those tokens
instead of literal colours. The theme toggle switches `cds--g100` on `<html>`
and `<body>`.

The device pages use plain HTML (`<table>`, `<form>`, `<dialog>`) styled with
those tokens rather than Carbon's table and form web components, because the
rows are swapped by htmx and the forms must submit natively.

### Form fields

Every field follows the same markup, see `tags/deviceFormFields.html`:

```html
<div class="field">
  <label class="field-label" for="deviceModel">Model</label>
  <input class="field-input" type="text" id="deviceModel" name="deviceModel"
         placeholder="e.g. A2779" value="{device.deviceModel ?: ''}">
  <span class="field-hint">Optional helper text.</span>
</div>
```

Group fields in a `.form-section` with a `.form-section-title`, lay them out
with `.form-grid` (`.span-2` for full width), and finish the card with a
`.form-actions` bar. Required fields get the `required` attribute; the
`.field-error` text appears through `:user-invalid`, no JavaScript needed.

### Buttons

Native `<button>` and `<a>` elements with the `.btn` classes from
`treasure-pages.css`:

```html
<button type="submit" class="btn btn-primary">Save device</button>
<a href="/devices" class="btn btn-secondary">Cancel</a>
<button type="button" class="btn btn-ghost btn-sm">Clear all</button>
<button type="submit" class="btn btn-danger btn-icon" title="Delete" aria-label="Delete X">...</button>
```

`.btn-primary` is the Treasure orange with dark text and is used once per view
for the main action. Do not use `<cds-button>`; it does not submit forms reliably.

### Dialogs

Use a native `<dialog class="modal">` with `.modal-head`, `.modal-body` and
`.modal-foot`. Open it with a `data-open="dialogId"` button and close it with
`data-close`; `devices.js` wires both. Destructive actions go through the
shared `#confirmDialog` by adding `data-confirm="single"` (plus
`data-device-name`) or `data-confirm="bulk"` to the form.

### Menus

A `.menu-wrap` form with a `data-menu="menuId"` button and a `.menu` child
gives a dropdown. Items with `data-assign="username"` fill the `assignedTo`
field and submit; items with `data-pick-name="ids" data-pick-value="…"` fill
any named field and submit. Both are handled by `devices.js` on every page.

### Notifications

Controllers set flash messages; the layout renders them. If a page needs a
static notice, reuse the same element:

```html
<cds-inline-notification kind="info" title="Note" subtitle="..." low-contrast>
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
var(--treasure-primary)        /* #FF8200 - Treasure orange, for fills and borders */
var(--treasure-primary-hover)  /* #E67500 */
var(--treasure-primary-active) /* #CC6800 */
var(--treasure-primary-light)  /* #FFECD9 light, #3D2A14 dark - tinted backgrounds */
var(--treasure-primary-text)   /* #A35300 light, #FFB366 dark - orange as text */
var(--treasure-on-primary)     /* #161616 - text on an orange fill */
```

#FF8200 is too light for white text (about 2.4:1), so primary buttons use dark
text, and orange text on light surfaces uses the darker `--treasure-primary-text`
shade instead of the fill colour.

Carbon status colours: green `#24a148` (success), red `#da1e28` (error),
yellow `#f1c21b` (warning). For Chart.js use the Treasure orange as the
primary series colour.

### Qute syntax in these templates

```html
<!-- Conditional attribute -->
<cds-side-nav-link href="/devices" {#if inject:vertxRequest.path.startsWith('/devices')}active{/if}>

<!-- Null-safe value: Elvis, never ?. -->
<input class="field-input" value="{device.deviceName ?: ''}">

<!-- Loop -->
{#for member in members}
<button type="button" data-assign="{member.userName}">{member.firstName} {member.lastName}</button>
{/for}
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
9. **Inline JavaScript in templates** - Qute treats `{name` as an expression,
   so object literals and template strings in inline scripts break rendering.
   Put page scripts in `META-INF/resources/js/` instead
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
