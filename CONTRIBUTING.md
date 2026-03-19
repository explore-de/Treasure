# Contributing to Treasure

Thank you for your interest in contributing to Treasure! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Git Workflow](#git-workflow)
- [Pull Request Process](#pull-request-process)
- [Community](#community)

## Code of Conduct

This project is committed to providing a welcoming and inclusive environment for all contributors. Please be respectful and professional in all interactions.

## Getting Started

### Prerequisites

- **Java 21** - Required for building and running the application
- **Maven** - Maven wrapper (`mvnw`) is included in the project
- **Git** - For version control

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR-USERNAME/treasure.git
   cd treasure
   ```
3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/treasure-app/treasure.git
   ```

## Development Setup

The project uses Quarkus DevServices, which automatically starts required services (PostgreSQL, LocalStack S3, Keycloak) in containers. No manual setup required!

### Run in Development Mode

```bash
cd app.treasure.treasure-app
./mvnw quarkus:dev
```

This starts the application with:
- Live reload on code changes
- DevServices for PostgreSQL, S3 (LocalStack), and Keycloak
- Application at http://localhost:8080
- Dev UI at http://localhost:8080/q/dev

### Run Tests

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=TestClassName

# Run tests with coverage
./mvnw verify
```

### Build the Application

```bash
# Build all modules
./mvnw clean package

# Build without tests
./mvnw clean package -DskipTests
```

## Project Structure

Treasure is a multi-module Maven project:

```
treasure/
├── app.treasure.treasure-app/    # Main web application (Quarkus + Renarde)
├── app.treasure.zugferd/         # ZugFerd e-invoice extraction service
├── app.treasure.az-document-ai/  # Azure Document Intelligence service
└── formatter/                 # Java code formatter configuration
```

### Main Application Structure

Feature-based package organization:

```
app.treasure.<feature>/
├── api/          # Renarde controllers (web endpoints)
├── domain/       # JPA entities
├── model/        # DTOs and input classes
├── repository/   # Data access layer
├── service/      # Business logic
└── workflow/     # Workflow tasks
```

## Coding Standards

### Code Formatting

**CRITICAL:** Always format code before committing!

```bash
# Validate formatting
./mvnw formatter:validate

# Auto-format all code
./mvnw formatter:format
```

The formatter configuration is in `formatter/java.xml`. All code must pass validation before merging.

### Java Conventions

- **Java 21** features are encouraged
- Use **SLF4J** for logging (never `System.out.println`)
- Follow **feature-based packaging** structure
- Prefer **constructor injection** over field injection
- Use **records** for DTOs when appropriate

### Logging

Always use SLF4J:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger LOG = LoggerFactory.getLogger(MyClass.class);

// Log levels
LOG.error("Error message: {}", error);  // Failures needing attention
LOG.warn("Warning: {}", issue);         // Potential problems
LOG.info("User action: {}", action);    // Important operations
LOG.debug("Debug info: {}", data);      // Detailed debugging
```

### Security & Roles

Use role constants from `Roles.java`:

```java
import app.treasure.shared.security.Roles;

if (securityIdentity.hasRole(Roles.ADMIN)) {
    // Admin-only logic
}
```

Available roles:
- `Roles.SUPER_ADMIN` - System administrator
- `Roles.ADMIN` - Organization administrator
- `Roles.MEMBER` - Regular member

### Frontend (Carbon Design System)

- Use actual **Carbon Web Components**, not custom HTML/CSS
- Use native `<button>` elements for form submissions
- Use `<cds-button>` only for navigation links
- Reference CSS variables: `var(--treasure-primary)` instead of hardcoded colors
- See `CLAUDE.md` for detailed Carbon component usage

## Testing

### Test Structure

```java
@QuarkusTest
class MyResourceTest {

    @Test
    void shouldDoSomething() {
        // Arrange
        deleteAllData();
        createTestData();

        // Act & Assert
        given()
            .when()
            .get("/endpoint")
            .then()
            .statusCode(200)
            .body(containsString("expected"));
    }

    @Transactional(TxType.REQUIRES_NEW)
    void deleteAllData() {
        repository.deleteAll();
    }
}
```

### Testing Guidelines

- Test names: `shouldDoSomething()` format
- Use `@QuarkusTest` for integration tests
- Use `@TestSecurity` to simulate authenticated users
- Test business logic in service tests, not just HTTP endpoints
- Aim for meaningful test coverage, not just high percentages

## Git Workflow

### Branch Naming

Use descriptive branch names with prefixes:

- `feature/` - New features (e.g., `feature/add-export-function`)
- `fix/` - Bug fixes (e.g., `fix/login-validation`)
- `refactor/` - Code refactoring (e.g., `refactor/document-service`)
- `docs/` - Documentation updates (e.g., `docs/update-readme`)

### Commit Messages

Follow **Gitmoji** convention with Unicode emojis:

```
<emoji> (scope): Short description

Longer description if needed with details about:
- What changed
- Why it changed
- Any breaking changes
```

**Common emojis:**
- ✨ `:sparkles:` - New feature
- 🐛 `:bug:` - Bug fix
- 📝 `:memo:` - Documentation
- ♻️ `:recycle:` - Refactoring
- 🎨 `:art:` - UI/styling changes
- ✅ `:white_check_mark:` - Tests
- 🔧 `:wrench:` - Configuration

**Example:**
```
✨ (document): Add export to PDF functionality

- Implement PDF export using iText library
- Add export button to document detail page
- Include document metadata in PDF header
```

### Keep Your Fork Updated

```bash
# Fetch upstream changes
git fetch upstream

# Update your main branch
git checkout main
git merge upstream/main
git push origin main

# Rebase your feature branch
git checkout feature/your-feature
git rebase main
```

## Pull Request Process

### Before Submitting

1. ✅ **Format code:** `./mvnw formatter:format`
2. ✅ **Run tests:** `./mvnw test`
3. ✅ **Build successfully:** `./mvnw clean verify -DskipTests`
4. ✅ **Update documentation** if needed
5. ✅ **Rebase on latest main**

### PR Title Format

Use the same Gitmoji format as commits:

```
✨ (scope): Add feature description
🐛 (scope): Fix bug description
```

### PR Description Template

```markdown
## What does this PR do?

Brief description of the changes.

## Why is this needed?

Explain the motivation and context.

## How has this been tested?

- [ ] Unit tests added/updated
- [ ] Integration tests pass
- [ ] Manual testing performed

## Screenshots (if applicable)

[Add screenshots for UI changes]

## Checklist

- [ ] Code formatted with `./mvnw formatter:format`
- [ ] All tests pass
- [ ] Documentation updated
- [ ] Commit messages follow convention
```

### Review Process

- At least one approval required before merging
- Address all review comments
- Keep PR focused on a single feature/fix
- Squash commits if requested

## Community

### Getting Help

- **Questions?** Open a [Discussion](https://github.com/treasure-app/treasure/discussions)
- **Bug reports?** Open an [Issue](https://github.com/treasure-app/treasure/issues)
- **Feature ideas?** Start a [Discussion](https://github.com/treasure-app/treasure/discussions)

### Recognition

Contributors with merged code are automatically added to the README using [All Contributors](https://allcontributors.org/). The bot recognizes:

- 💻 Code contributions
- 🚇 Infrastructure (CI/CD, deployment)
- 📖 Documentation
- 🎨 Design
- 🔧 Tools
- 💼 Business development

---

**Thank you for contributing to Treasure!** 🎉

Your contributions help make accounting easier for organizations worldwide.
