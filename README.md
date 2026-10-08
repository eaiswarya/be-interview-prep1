# be-interview-prep

Five backend features built with Java 17, Spring Boot 3.5, Maven and an in-memory H2 database, each delivered as its own pull request.

## Run the app

The JWT signing key is read from the environment and must be at least 32 characters. An admin account can optionally be created at startup.

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
export ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=change-me-please   # optional

./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

The app runs on http://localhost:8080. Register with `POST /api/auth/register`, log in with `POST /api/auth/login`, and send the returned token as `Authorization: Bearer <accessToken>` on every other `/api/**` request. Short-link redirects (`GET /{code}`) are public.

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
| 5 | Order Service | https://github.com/eaiswarya/be-interview-prep/pull/8 |

**Video:**
