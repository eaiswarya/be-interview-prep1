# be-interview-prep

Backend interview prep assignment: five Spring Boot features, each shipped as its own branch and PR.

Product doc: [docs/PRODUCT.md](docs/PRODUCT.md)

**Stack:** Java 17+, Spring Boot 3.5, Maven (wrapper included), H2 in-memory database.

## Run the app

The JWT signing key is never stored in the repo. Set it (at least 32 characters) before starting the app; optionally set an admin account to create at startup:

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
export ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=change-me-please   # optional

./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

All `/api/**` endpoints except register and login need `Authorization: Bearer <accessToken>` from `POST /api/auth/login`. Short-link redirects (`GET /{code}`) are public.

The app starts on http://localhost:8080. The H2 console is at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:beprep`, user `sa`, no password).

## Run the tests

```bash
./mvnw test                   # Windows: mvnw.cmd test
```

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | https://github.com/eaiswarya/be-interview-prep/pull/3 |
| 2 | URL Shortener | https://github.com/eaiswarya/be-interview-prep/pull/4 |
| 3 | Authentication & Roles | https://github.com/eaiswarya/be-interview-prep/pull/5 |
| 4 | Product Catalog | https://github.com/eaiswarya/be-interview-prep/pull/6 |
| 5 | Order Service | |

**Video:**
