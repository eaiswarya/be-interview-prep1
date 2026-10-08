# be-interview-prep

Java 17 / Spring Boot 3.5 / Maven wrapper / H2 in-memory. Requirements for the five questions are in `docs/PRODUCT.md`.

## Workflow

- One question = one branch, one PR, one merge, in order Q1 -> Q5. Use `/ticket q<n>` to implement and raise a PR, `/merge-ticket q<n>` to merge.
- Git mechanics go through `scripts/ticket.sh` (`start`, `check`, `pr`, `merge`, `status`).
- Never commit on `main` directly. Never commit `Backend_Interview_Prep_Assignment.pdf`.
- Small commits with specific imperative messages (`Add create task endpoint`). Never `fix`, `changes`, `final`.

## Code conventions

- Package per feature under `com.interviewprep`: `task`, `url`, `auth`, `product`, `order`; shared code in `common` (e.g. the global error handler).
- Layers: controller -> service -> repository. Controllers take and return DTOs (Java records), never entities.
- Validation with Bean Validation annotations on request DTOs and `@Valid` in controllers.
- One JSON error shape for every error, produced by a single `@RestControllerAdvice` in `common`; correct HTTP status every time.
- `@Transactional` on service methods that write.
- No secrets in source: read them from environment variables with a dev-only default in `application.properties` at most.
- Tests: JUnit 5 + MockMvc (`@SpringBootTest` + `@AutoConfigureMockMvc`) per acceptance criterion. Every test must pass before a PR (`./mvnw test`).
