# Treasure 💻

```
████████╗██████╗ ███████╗ █████╗ ███████╗██╗   ██╗██████╗ ███████╗    
╚══██╔══╝██╔══██╗██╔════╝██╔══██╗██╔════╝██║   ██║██╔══██╗██╔════╝    
   ██║   ██████╔╝█████╗  ███████║███████╗██║   ██║██████╔╝█████╗      
   ██║   ██╔══██╗██╔══╝  ██╔══██║╚════██║██║   ██║██╔══██╗██╔══╝      
   ██║   ██║  ██║███████╗██║  ██║███████║╚██████╔╝██║  ██║███████╗    
   ╚═╝   ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚══════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝  
```

**Device management for organizations.**

Track hardware inventory, assign devices to members, and keep an audit trail of who
changed what and when.

---

## Features ✨

- **Inventory** — devices with detailed specs: model, CPU, RAM, storage, serial number, age, location, damage notes
- **Assignment** — assign a device to a member; status and pickup time update accordingly. Bulk assign, unassign and delete supported
- **History** — every change is recorded with field name, old value, new value, actor and timestamp
- **Search & filters** — filter the device list by field
- **Delete** — removes a device together with its history; there is no soft delete yet
- **Members** — everyone who can hold a device, linked to their sign-in identity (Keycloak in dev, Entra ID on the stand), with the devices they currently hold
- **Dashboard** — landing page with an overview

---

## Tech Stack 🛠️

| Layer | Choice |
|---|---|
| Language | Java 25 |
| Framework | Quarkus 3.40.1.1 |
| Web / UI | Renarde 3.1.10 (server-side rendering with Qute) |
| Persistence | Hibernate ORM with Panache, repository pattern |
| Database | PostgreSQL, schema managed by Flyway |
| Auth | OIDC authorization-code flow: Keycloak Dev Services in dev and test, Microsoft Entra ID on the stand; Keycloak Admin REST Client for the dev bootstrap only |
| Extras | SmallRye Health |
| Testing | JUnit 5, REST Assured, Mockito, JaCoCo |

---

## Quick Start 🚀

**Prerequisites:** JDK 25 and Docker — Quarkus Dev Services provisions PostgreSQL
and Keycloak for you.

```shell
cd app.treasure.treasure-app
./mvnw quarkus:dev
```

The app comes up on <http://localhost:8080>. No manual database or Keycloak setup
needed; Flyway migrates the schema on boot.

### Dev credentials

| Username | Password | Roles |
|---|---|---|
| `admin` | `password` | super_admin, admin, user |
| `maria` | `password` | admin, user |
| `thomas` | `password` | user |

### Dev endpoints

| URL | What |
|---|---|
| <http://localhost:8080/q/dev/> | Quarkus Dev UI |
| <http://localhost:8080/q/health> | Health checks |

---

## Data Model 🧩

```mermaid
erDiagram
    MEMBER |o--o{ DEVICE : "assigned"
    DEVICE ||--o{ DEVICE_HISTORY : "logged in"
    MEMBER |o--o{ DEVICE_HISTORY : "acted on"
    TAG |o..o{ DEVICE : "names the group of"
```

`TAG` holds every group name ever saved, so a group survives after the last
device leaves it. `Device.deviceGroup` is a plain string matched by name; there is
no foreign key to `TAG`. `Member.invited_by_member_id` is a leftover column that
nothing writes. Full column list: `src/main/resources/db/migration`.

---

## Project Structure 📁

```
app.treasure.treasure-app/
└── src/main/java/app/treasure/
    ├── dashboard/     api — overview page
    ├── device/        api · domain · filter · repository
    ├── member/        api · domain · repository · service — members and the devices they hold
    └── shared/        bootstrap · domain · repository · security · template · util
```

Packages are organized by feature, not by layer. Inside a feature: `api/` controllers,
`domain/` entities, `filter/` list filtering, `repository/` data access, `service/`
business logic. Small view models are records nested in the controller that renders
them (`MemberRow`, `GroupCount`); there is no separate DTO package.

Migrations live in `src/main/resources/db/migration` — never edit an applied migration,
always add a new versioned file.

### Routes

Authentication is required everywhere. `GET /` is the dashboard and `GET /members`
the Members page; everything else lives under `/devices`:

| Method | Path | What |
|---|---|---|
| `GET` | `/devices` | list; filter with query parameters such as `name`, `status`, `assignedTo`, `group`, `model`, `serial` |
| `GET` | `/devices/new` | create form |
| `GET` | `/devices/{id}/edit` | edit form with the device's history |
| `POST` | `/devices/create` | create |
| `POST` | `/devices/{id}/update` | update |
| `POST` | `/devices/{id}/assign`, `/devices/{id}/unassign` | single-device actions; called by htmx, return the swapped table row |
| `POST` | `/devices/{id}/delete` | delete |
| `POST` | `/devices/assign-many`, `/unassign-many`, `/delete-many` | bulk actions; the Members page uses the first two with `redirectUrl=/members` |

---

## Development 🧑‍💻

```shell
./mvnw quarkus:dev              # dev mode with live reload
./mvnw test                     # run tests
./mvnw test -Dtest=ClassName    # run a single test class
./mvnw verify                   # tests + JaCoCo report (what CI runs)
./mvnw formatter:validate       # check formatting (bound to every build)
./mvnw formatter:format         # apply formatting
./mvnw package                  # build target/quarkus-app/quarkus-run.jar
```

**House rules** — see [CONTRIBUTING.md](CONTRIBUTING.md):

- Branches: `feature/`, `fix/`, `refactor/`, `docs/`
- Commits and PR titles use Gitmoji: `✨ (device): Add assignment filter`
- Format before pushing, keep tests green, rebase on `main`
- SLF4J instead of `System.out.println`, records for small view models
- CSS variables like `var(--treasure-primary)`, no hardcoded colors

---

## Deploying to a stand 🚀

The packaged app (`./mvnw package`, then `java -jar target/quarkus-app/quarkus-run.jar`)
runs with the `prod` profile and signs people in through **Microsoft Entra ID**.
No demo users or demo members are created there; a person's `Member` row is
created automatically the first time they sign in.

Environment variables the stand needs:

| Variable | What |
|---|---|
| `ENTRA_TENANT_ID` | the directory (tenant) id of the company tenant |
| `ENTRA_CLIENT_ID` | the application (client) id of the app registration |
| `ENTRA_CLIENT_SECRET` | a client secret of that app registration |
| `OIDC_SESSION_SECRET` | at least 32 random characters; encrypts the session cookie and must be identical on every instance |
| `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD` | the PostgreSQL database; Flyway migrates it on start |

CI (`.github/workflows/treasure-app.yml`) also builds a container image from
`src/main/docker/Dockerfile.jvm` and pushes it to `ghcr.io/<owner>/treasure-app`
(tags `latest` and the run number) on every push to `main` or `deploy`. The container
takes the same environment variables. Under the `prod` profile the management
interface is enabled, so health checks are served at `http://<host>:9000/q/health`,
not on port 8080.

In the Entra app registration:

- add a **Web** redirect URI of `https://<host>/callback`
- set the front-channel logout URL to `https://<host>/`
- define **App roles** with the values `super_admin`, `admin` and `user`, and assign
  them to people or groups. The roles are shown in the account panel; the app has no
  role-restricted functions yet, so a person without a role can still use everything.

**First sign-in checklist.** The Entra settings were written from the Quarkus
documentation, not from a live tenant, so verify once with a real account:

1. The login completes and lands on the page that was requested, not on `/callback`.
2. The account panel (top right) shows the person's roles. If it is empty, the ID
   token carries no `roles` claim: check that App Roles are assigned, or change
   `quarkus.oidc.roles.role-claim-path` if your tenant emits `groups` instead.
3. The Members page lists the person after that first login, with name and email
   filled from the token.
4. Sign out returns to `/` and signing in again works without a second consent screen.

Dev mode is unaffected: it keeps using the Keycloak dev container and the demo users
listed above.

---

## Troubleshooting 🔧

**`Failed to start quarkus`** — usually port 8080 is still held by an older run:

```shell
# Linux
sudo ss -ltnp | grep ':8080'
# macOS
lsof -nP -iTCP:8080 -sTCP:LISTEN
kill <pid>
```

Repeat until the lookup returns nothing, then start again.

**Dev Services misbehaving** — check that the Docker daemon is up and no stale Keycloak
/ PostgreSQL containers are left over (`docker ps`, then `docker rm -f <id>`).

---

## Requirements 📋

<details>
<summary>Original project scope</summary>

**Functional**

- Users can see a list of all devices and their current status
- Users can add a new device by filling out a simple form
- Users can edit device details like name and pickup time
- Users can delete a device that is no longer needed
- When a device is assigned to someone it automatically becomes unavailable, when returned it becomes available again
- Users can see who has a device and when they are picking it up

**Non-Functional**

- The app is built with Java and runs as a web application
- All data is saved to a database so nothing is lost on restart
- The interface should be clean and easy to use
- The app should be easy to set up and run locally

**User Stories**

- As a user, I want to see a list of all devices so that I know what is available and what is not
- As a user, I want to add a new device so that it can be tracked in the system
- As a user, I want to edit a device so that I can update its information if something changes
- As a user, I want to delete a device so that the list stays clean and up to date
- As a user, I want to see who has booked a device so that I know who is responsible for it
- As a user, I want the device status to update automatically so that I don't have to do it manually
- As a user, I want to see the pickup time of a device so that I know when it will be returned

</details>
