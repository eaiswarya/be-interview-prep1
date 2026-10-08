# be-interview-prep

Backend interview prep assignment: five Spring Boot features, each shipped as its own branch and PR.

Product doc: [docs/PRODUCT.md](docs/PRODUCT.md)

**Stack:** Java 17+, Spring Boot 3.5, Maven (wrapper included), H2 in-memory database.

## Run the app

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

The app starts on http://localhost:8080. The H2 console is at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:beprep`, user `sa`, no password).

## Run the tests

```bash
./mvnw test                   # Windows: mvnw.cmd test
```

## Workflow

Each question goes through `scripts/ticket.sh` (or the Claude Code skills `/ticket q<n>` and `/merge-ticket q<n>`):

```bash
scripts/ticket.sh start q1             # branch from latest main
scripts/ticket.sh check                # run all tests
scripts/ticket.sh pr q1 pr-body.md     # test, push, open PR, add its link below
scripts/ticket.sh merge q1             # squash-merge, sync main, re-run tests
scripts/ticket.sh status               # every question's PR state
```

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | |
| 2 | URL Shortener | |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

**Video:**
