# Backend Interview Prep Assignment (Java / Spring Boot)

This document restates the assignment and records the design decisions for each question, as built in this repo.

## Overview

Build 5 Spring Boot features in one GitHub repository. Ship each one as its own pull request, then record one 2-minute video explaining all five. The code must be explainable without AI help.

- **Stack:** Java 17+ and Spring Boot 3.x. Build tool, database, libraries and test tools are our choice.
  - This repo uses Java 17, Spring Boot 3.5, Maven (wrapper included), Spring Data JPA, H2, Spring Security (OAuth2 resource server) and Caffeine.
- **Time limit:** 2 hours for all 5 questions, including PRs and merges. Then 30 minutes to record and upload the video.
- **Suggested split:** Q1 and Q2 15 minutes each, Q3 and Q4 25 minutes each, Q5 40 minutes.
- **Assessed:** working code, clean structure, tests, Git/PR discipline, and above all understanding of what was built.
- **Interview:** be ready to explain every decision and the alternatives, and to make a short live change. Expect questions such as:
  - Walk me through what happens from the HTTP request to the database.
  - Why this approach? What alternatives were considered?
  - What breaks if two requests hit this endpoint at the same time?
  - What happens if this line is removed?
  - Change this behaviour live.

## Submission workflow

Each question is one branch, one PR and one merge, in order from Q1 to Q5.

1. Public repo `be-interview-prep`. On `main`, a README and a base Spring Boot project. Here these land through the `feature/base-project` PR.
2. Each question gets a branch from the latest `main`:

   | # | Branch |
   |---|--------|
   | 1 | `feature/q1-task-api` |
   | 2 | `feature/q2-url-shortener` |
   | 3 | `feature/q3-auth` |
   | 4 | `feature/q4-product-catalog` |
   | 5 | `feature/q5-order-service` |

3. Commit in small, meaningful steps, such as `Add create task endpoint` or `Return field errors for invalid input`. Never use commits like `fix`, `changes` or `final`.
4. Open a PR into `main` with the description template: Problem, Approach, Decisions & trade-offs, How to test. Review the diff before merging.
5. Merge (merge commit), then pull `main` before starting the next branch.
6. After the last merge, record the video, upload it to YouTube as Unlisted, and add the link to the README.

## Shared conventions

- **Packages (by layer, under `com.interviewprep`):** `controller`, `service`, `repository` (including `ProductSpecifications`), `model` (JPA entities and enums), `dto` (request/response records, `ApiError`, `PageResponse`), `exception` (exceptions and `GlobalExceptionHandler`), `config` (security, JWT, cache), `validation` (`@HttpUrl`), `seeder` (startup data). The layout was introduced by the `refactor/layered-packages` PR.
- **Errors (added in Q1, reused by later questions):** every error uses one JSON shape, `ApiError`: `timestamp`, `status`, `error`, `message`, `path` and `fieldErrors`. `GlobalExceptionHandler` (`@RestControllerAdvice`) maps the cases:
  - 400: Bean Validation failures, an invalid enum or date in the body (reported against that field), malformed JSON, bad query or path parameters, a missing header, and `InvalidRequestException` for cross-field rules.
  - 401: a failed login (`BadCredentialsException`).
  - 404: `ResourceNotFoundException` and unknown URLs.
  - 405 and 415: unsupported method or content type.
  - 409: `ConflictException` (duplicate email, insufficient stock, reused idempotency key, already-cancelled order).
  - 410: `ResourceGoneException` (expired short link).
  - 500: anything else, logged, with no details leaked.
- **Persistence:** an H2 in-memory database through Spring Data JPA. `ddl-auto=create-drop`, `open-in-view=false`, and `LOCK_TIMEOUT=10000` so contended row updates wait instead of failing. Entities are never returned directly; each has a `*Response.from` mapper.
- **Security (from Q3):** every `/api/**` endpoint except register and login needs a JWT. Short-link redirects (`GET /{code}`) and the H2 console stay public.

---

## Q1: Task Manager API

Build a REST API to create, view, update, delete and filter tasks.

**Requirements**
- A task has:
  - a title (required, max 100 characters)
  - a description
  - a status (To do / In progress / Done)
  - a due date (cannot be in the past)
  - a created date
- Create, list, get one, update and delete tasks. Filter the list by status.
- Reject invalid input with a clear message for each invalid field.
- All errors (invalid input, not found, unexpected) return one consistent JSON format with the right HTTP status.

**Acceptance criteria**
- Invalid input returns 400 with field-level messages. An unknown task returns 404.
- At least one automated test.

**Endpoints**

| Method | Path | Success | Errors |
|--------|------|---------|--------|
| POST | `/api/tasks` | 201 + `Location` | 400 |
| GET | `/api/tasks?status=TODO` | 200 | 400 (unknown status) |
| GET | `/api/tasks/{id}` | 200 | 400 (non-numeric id), 404 |
| PUT | `/api/tasks/{id}` | 200 | 400, 404 |
| DELETE | `/api/tasks/{id}` | 204 | 404 |

Request body: `{"title": "...", "description": "...", "status": "TODO|IN_PROGRESS|DONE", "dueDate": "2026-12-31"}`.

**Flow:** `TaskController` (validates with `@Valid`) → `TaskService` (`@Transactional`, defaults) → `TaskRepository` (Spring Data JPA, `findByStatus`) → H2 `task` table. `TaskResponse.from` maps the entity.

**Since Q3:** every task endpoint needs a Bearer token.

**Decisions**
- **Status:** `TaskStatus` is an enum stored as text (`EnumType.STRING`), so reordering the enum can't corrupt existing rows. It is optional; a missing status becomes `TODO` on create and on update.
- **Validation:** `@NotBlank @Size(max = 100)` on the title, `@Size(max = 2000)` on the description (matching the column, so an oversized value is a 400 rather than a database error), and `@FutureOrPresent` on the due date, so today is allowed.
- **Field-level errors:** Bean Validation errors are collected per field. An invalid enum or date fails in Jackson before validation runs, so the handler reads the field name from Jackson's exception path and reports "has an invalid value" against it.
- **`createdAt`** is set by the server in `@PrePersist`, truncated to milliseconds so the value returned on create matches what the database returns later. It is never read from the request.
- **PUT replaces the whole task.** It is simpler than PATCH and enough for the spec.
- **Unexpected errors** return 500 with "Unexpected error"; the stack trace is only logged. `TaskUnexpectedErrorTest` checks nothing leaks.
- **Known limitation:** no optimistic locking (`@Version`), so two overlapping updates of one task end with the last write winning.

## Q2: URL Shortener

Build a service that turns long URLs into short links.

**Requirements**
- Submit a long URL, optionally with an expiry date, and get back a short code and a short URL.
- Visiting the short URL redirects to the original URL.
- Count every visit. A stats endpoint shows the original URL, the visit count and the created date.
- Short codes are at most 8 characters, unique and URL-safe.
- Reject invalid URLs. Handle unknown and expired codes with an appropriate status.

**Acceptance criteria**
- Shortening the same URL twice behaves the way we decided it should, and we can explain why.
- Visit counts stay accurate when many people open the same link at once.
- At least one automated test.

**Endpoints**

| Method | Path | Success | Errors |
|--------|------|---------|--------|
| POST | `/api/urls` `{url, expiresAt?}` | 201 `{code, shortUrl, originalUrl, expiresAt, createdAt}` | 400 |
| GET | `/{code}` | 302 to the original URL | 404 unknown, 410 expired |
| GET | `/api/urls/{code}/stats` | 200 `{code, originalUrl, visitCount, createdAt, expiresAt}` | 404 |

**Flow:** `ShortLinkController` → `ShortLinkService` → `ShortLinkRepository` → H2 `short_link` table. `ShortCodeGenerator` produces the codes.

**Since Q3:** creating links and reading stats need a Bearer token. The `/{code}` redirect stays public, because people clicking a shared link have no token.

**Decisions**
- **Codes:** 7 random Base62 characters (`[0-9A-Za-z]`, URL-safe without encoding) from `SecureRandom`. That gives 62^7 ≈ 3.5 trillion combinations.
  - The unique constraint on `code` is the real guarantee. On a collision the insert fails and is retried with a new code, up to 5 attempts (`ShortLinkServiceTest`).
  - Random rather than sequential, so codes can't be enumerated to discover other links.
- **Same URL twice creates two independent links**, each with its own expiry and stats.
  - Alternative: return the existing link. That saves rows, but two people sharing one link share its stats and expiry, and dedupe needs extra care under concurrency.
- **`shorten` is deliberately not `@Transactional`.** Each `saveAndFlush` commits on its own, so a code collision rolls back only that attempt and the retry starts with a clean transaction.
- **URL validation:** `@HttpUrl` accepts only absolute `http`/`https` URLs with a host, up to 2048 characters. This rejects `javascript:`, `ftp:` and relative paths like `example.com`. `expiresAt` must be in the future.
- **Visit counting:** a single atomic `UPDATE short_link SET visit_count = visit_count + 1 WHERE id = ?` per redirect. The database serialises the increments, so concurrent visits are never lost; `concurrentVisitsAreAllCounted` checks this.
  - Alternative: read the count, add one and save. Two visitors would read the same value and one increment would be lost.
- **302, not 301:** browsers cache a 301 and skip the server on later visits, so those visits would never be counted.
- **Redirect path:** `/{code:[0-9A-Za-z]{1,8}}`, so the regex keeps paths like `/favicon.ico` or `/h2-console` from reaching it.
- **Expired vs unknown:** an expired code returns 410 Gone (it existed, but is no longer valid) and is not counted. An unknown code returns 404. Stats stay viewable after expiry.

## Q3: Authentication & Roles

Secure an API so that only logged-in users can use it, and some endpoints are admin-only.

**Requirements**
- Users can register and log in. Store passwords securely.
- The API serves web and mobile clients, so authentication must not rely on server-side sessions.
- Login expires after 15 minutes.
- There are two roles, USER and ADMIN. Any logged-in user can view their own profile. Only an ADMIN can list all users.
- A request that isn't logged in returns 401. A logged-in user without the right role gets 403. Both return JSON, not an HTML error page.

**Acceptance criteria**
- A test proves that a USER cannot access the admin endpoint.
- No secrets are hard-coded in the source.

**Endpoints**

| Method | Path | Access | Success | Errors |
|--------|------|--------|---------|--------|
| POST | `/api/auth/register` `{email, password}` | public | 201 `{id, email, role, createdAt}` | 400, 409 (email taken) |
| POST | `/api/auth/login` `{email, password}` | public | 200 `{accessToken, tokenType: "Bearer", expiresIn}` | 400, 401 |
| GET | `/api/users/me` | USER, ADMIN | 200 own profile | 401 |
| GET | `/api/admin/users` | ADMIN | 200 all users | 401, 403 |

**Flow:**
1. **Login:** `AuthController` → `AuthService` checks the password against the BCrypt hash, then `TokenService` signs a JWT.
2. **Every later request:** Spring Security's bearer-token filter reads `Authorization: Bearer …`, and `JwtDecoder` verifies the signature, expiry and issuer.
3. **Roles:** the `roles` claim becomes `ROLE_USER` or `ROLE_ADMIN`, and URL rules in `SecurityConfig` decide access.
4. **Controllers:** they read the user id from the verified token's subject (`@AuthenticationPrincipal Jwt`), never from the request.

**Decisions**
- **Stateless JWT** (`SessionCreationPolicy.STATELESS`, CSRF off since tokens travel in a header, not cookies). This works for web and mobile clients.
  - Alternatives: server sessions, which the spec rules out; opaque tokens, which need a token-store lookup on every request.
- **Spring's OAuth2 resource server** (Nimbus, HS256) instead of a hand-written filter. Spring verifies the signature and expiry itself.
  - The JWT carries `iss`, `sub` (user id), `email`, `roles`, `iat` and `exp`.
  - Trade-off: a role change takes effect only when the token expires.
- **Exactly 15 minutes:** `JwtTimestampValidator(Duration.ZERO)` removes Spring's default 60-second clock-skew allowance (`tokenExpiresAfterFifteenMinutes`, `expiredTokenIsRejected`).
- **No hard-coded secrets:**
  - The signing key comes from `JWT_SECRET`. `JwtProperties` refuses to start the app if it is missing or shorter than 32 bytes (HS256 needs 256 bits).
  - Tests get a fresh random key on every run (`${random.value}`), so no key is stored in the repo.
  - Registration always creates `USER`. The first ADMIN is seeded by `AdminSeeder` only from the `ADMIN_EMAIL`/`ADMIN_PASSWORD` environment variables.
- **Passwords:**
  - BCrypt hashes, never returned in any response (`UserResponse` has no hash).
  - Validated to 8–72 characters, because BCrypt only uses the first 72 bytes.
  - Login answers "Invalid email or password" for both unknown emails and wrong passwords. For unknown emails it still runs a BCrypt match against a dummy hash, so the response time doesn't reveal which emails are registered.
- **Emails:** trimmed and lowercased, so `Alice@x.com` and `alice@x.com` are the same account. Duplicates return 409.
  - Like Q2, `register` relies on the unique constraint (`saveAndFlush`, catch the violation), so two simultaneous registrations can't both succeed.
- **JSON 401/403:** `JsonSecurityErrorHandler` implements `AuthenticationEntryPoint` (401, with `WWW-Authenticate: Bearer`) and `AccessDeniedHandler` (403) and writes the shared `ApiError`. Errors from the security filters never reach `@RestControllerAdvice`, so they need their own handler.
- **Known limitation:** a token can't be revoked before it expires. The optional refresh-token and logout work would add that.

## Q4: Product Catalog

Build a product listing API that stays fast as the catalog grows.

**Requirements**
- A product has a name, category, price, stock, rating and created date. Seed 100 products on startup.
- List products with pagination and sorting by any field. The response includes the total count and the number of pages.
- Optional filters that can be combined freely: category, price range, in-stock only, and name search.
- The page size is capped at 100.
- Single-product lookups happen far more often than products change. Make repeated lookups fast, but never return stale data after a product is updated or deleted.

**Acceptance criteria**
- Any combination of filters works in a single request.
- Repeated lookups of the same product don't query the database every time. Be able to show how we know.
- At least one automated test.

**Endpoints** (all need a Bearer token)

| Method | Path | Access | Success | Errors |
|--------|------|--------|---------|--------|
| GET | `/api/products?category&minPrice&maxPrice&inStock&q&page&size&sort` | USER, ADMIN | 200 page | 400 |
| GET | `/api/products/{id}` | USER, ADMIN | 200 (cached) | 404 |
| PUT | `/api/products/{id}` | ADMIN | 200 (evicts the cache) | 400, 403, 404 |
| DELETE | `/api/products/{id}` | ADMIN | 204 (evicts the cache) | 403, 404 |

**Page response:** `{content, page, size, totalElements, totalPages}`. Defaults are `page=0`, `size=20` and sort by `id`.
- `sort=price,desc` sorts one field; repeat it for several.
- Sortable fields: `id`, `name`, `category`, `price`, `stock`, `rating`, `createdAt`.

**Flow:**
- **Search:** `ProductController` → `ProductService.list` → `ProductSpecifications.matching` builds one WHERE clause → `ProductRepository.findAll(spec, pageable)`, which runs a SELECT plus a COUNT.
- **Lookup:** `ProductService.get` checks the Caffeine `products` cache first; on a miss it calls `findById` and stores the `ProductResponse`.

**Decisions**
- **Filters as JPA Specifications.** Each present filter adds one condition and `Specification.allOf` ANDs them, so any combination becomes a single query. Values are bound as parameters, never concatenated into SQL.
  - Alternative: a repository method per combination, which grows to 2^5 methods.
  - Category is an exact match, which keeps the category index usable. `inStock=true` means `stock > 0` (`false` means no filter).
  - `q` is a case-insensitive "contains" search on the name, with `%` and `_` escaped so they match literally.
- **Paging:**
  - A page size over 100 is **clamped** to 100 (`spring.data.web.pageable.max-page-size`) rather than rejected, so clients never break.
  - Unknown sort fields, a negative `minPrice` and `minPrice > maxPrice` return 400 with `fieldErrors`. Sort fields are whitelisted, so a typo is a 400 instead of a 500 from JPA.
  - `PageResponse` gives a stable JSON shape instead of serialising Spring's `Page`.
- **Price as `BigDecimal`** (`precision 12, scale 2`), so money never picks up binary rounding errors.
- **Caching single-product lookups (Caffeine via Spring Cache):**
  - `@Cacheable` stores the immutable `ProductResponse` record, not the JPA entity, so callers can't modify cached state.
  - **Never stale:**
    1. `TransactionAwareCacheManagerProxy` delays each evict until after the database commit. Otherwise a reader could re-cache the old row between the evict and the commit.
    2. Update and delete *evict* instead of writing the new value, so the cache never holds uncommitted data.
    3. `@Cacheable(sync = true)` makes concurrent misses for one id wait for a single load, and an evict issued during that load waits for it and then removes it.
  - Bounded to 10,000 entries; null values are not cached, so a 404 is never cached.
  - **How we know it works:** `repeatedLookupsQueryTheDatabaseOnlyOnce` turns on Hibernate statistics and checks that 5 lookups run exactly 1 SQL statement. `updateIsVisibleOnTheNextLookup` and `deletedProductIsNotServedFromTheCache` check the evicts.
  - Alternative: Hibernate's second-level cache, which caches entities, is harder to reason about and must be configured per entity. For multiple instances, a shared Redis cache would replace Caffeine (an optional extra).
- **Writes are ADMIN-only**, enforced in `SecurityConfig`; a USER gets 403 on PUT or DELETE. Products are created only by the seeder.
- **Seeding:** `ProductSeeder` inserts 100 products on startup from a fixed `Random(42)` seed, so the data is the same every run, and skips seeding if products already exist. About 1 in 5 is out of stock.
- **Indexes** on `category`, `price` and `name`, the columns used to filter and sort.

## Q5: Order Service

Build an order API that stays correct under heavy, simultaneous use.

**Requirements**
- Products have limited stock. A customer places an order with one or more items.
- An order is all-or-nothing: either every item is reserved, or none is.
- Stock must never go negative or be oversold, even when many customers order the same product at the same moment.
- Clients may retry a request after a network failure. A retried request must not create a duplicate order. Design how a retry is recognised.
- Insufficient stock returns 409 with a clear message.
- Cancelling an order returns its stock.

**Acceptance criteria**
- An automated test fires 50 simultaneous orders for a product with stock 10. Exactly 10 succeed, and the stock ends at 0.
- Retrying the same request creates only one order.

**Endpoints** (all need a Bearer token)

| Method | Path | Success | Errors |
|--------|------|---------|--------|
| POST | `/api/orders` + header `Idempotency-Key` + `{items: [{productId, quantity}]}` | 201 new order; 200 retry returning the original order | 400 (missing/invalid key or body), 404 product, 409 insufficient stock or key reused for a different order |
| GET | `/api/orders/{id}` | 200 | 404 |
| POST | `/api/orders/{id}/cancel` | 200 | 404, 409 (already cancelled) |

Response: `{id, status, items: [{productId, quantity}], createdAt}`. An order has 1 to 50 items, each with `quantity ≥ 1`.

**Flow:**
1. `OrderController` → `OrderService.place`.
2. **Replay check:** an existing order for (user, key) is returned as-is.
3. **One transaction** (`TransactionTemplate`): insert the `Order` first to claim the key, then `reserveStock` for each product in id order, then flush.
4. **After commit:** the product cache entries are evicted.

**Decisions**
- **No overselling: a conditional atomic update per item.**
  `UPDATE product SET stock = stock - :q WHERE id = :id AND stock >= :q`.
  - The database checks and decrements in one statement while holding the row lock, so two buyers can never both take the last unit, and stock can never go negative.
  - 0 rows updated means the product is missing (404) or short of stock (409 with "requested X, available Y").
  - Rejected alternatives:
    - **Read stock, check, save:** a classic race that oversells.
    - **`SELECT … FOR UPDATE` (pessimistic lock):** correct, but two round trips while holding the lock.
    - **`@Version` optimistic locking:** under a burst for one hot product most attempts fail and must retry. Both remain candidates for the optional "second approach".
- **All-or-nothing:** the key claim and every reservation run in one transaction. Any failure (one item short, unknown product) throws, and the rollback undoes the reservations already made for the other items (`anOrderReservesAllItemsOrNone`).
  - Items are processed in product-id order (duplicate lines are merged), so two orders for the same products always lock rows in the same order and can't deadlock.
- **Retries via a required `Idempotency-Key` header** (the client sends a UUID per logical order and resends it on retry):
  - A unique constraint on `(user_id, idempotency_key)` lets the database guarantee one order per key; keys are scoped per user.
  - The order stores a fingerprint of the merged items. The same key with the same items returns **200 with the original order**; the same key with different items returns **409**, so a reused key can't silently return the wrong order.
  - A missing, blank or over-100-character key returns 400.
  - **Concurrent copies of one request:** each copy runs the transaction. The key is claimed before any stock is touched, so a losing copy fails fast on the unique constraint (`DataIntegrityViolationException`), rolls back, re-reads the winner's order and returns it.
  - `place` has no `@Transactional` itself, for the same reason as Q2: the losing transaction must roll back before the replay check can read the winner's committed order.
- **Cancel:** `UPDATE orders SET status = CANCELLED WHERE id = ? AND status = PLACED`.
  - Stock is returned only when exactly one row changed, so repeated or simultaneous cancels return stock once.
  - Cancelling an already-cancelled order returns 409.
- **Cache interaction with Q4:** every stock change evicts that product's cache entry. The transaction-aware cache applies the evict after commit, so product lookups never show stale stock (`productLookupShowsTheNewStockAfterAnOrder`).
- **Ownership:** orders are looked up by id *and* user id, so another user's order returns 404, not 403, and its existence isn't confirmed.
- The entity is `Order` mapped to the `orders` table, because `ORDER` is a reserved SQL word.
- **Known limitation:** an admin `PUT /api/products/{id}` sets stock to an absolute value, so it can overwrite a reservation made between its read and its write. `@Version` on `Product` would turn that into a 409.
- **Tests (`OrderControllerTest`):**
  - 50 threads released together by a `CountDownLatch` order 1 unit of a product with stock 10: exactly 10 succeed and the stock ends at 0.
  - Retrying the same request creates one order, also when the retries arrive simultaneously.
  - Reusing a key for different items is rejected; the same key from another user is a separate order.
  - Cancelling returns the stock once.

---

## If we finish early (optional)

None of these are built yet.

- Q1: interactive API documentation.
- Q2: let users choose their own custom short code.
- Q3: let users stay logged in beyond 15 minutes without re-entering their password (refresh token), plus a logout that ends that.
- Q4: make the fast lookups work across several app instances (for example Redis).
- Q5: implement a second approach to the concurrency problem with a short comparison, and run the tests against a real database.

## Submission checklist

- [ ] Public repo `be-interview-prep` created, with a README
- [ ] 5 branches, 5 PRs, all merged into `main`
- [ ] Every PR uses the description template
- [ ] The README explains how to run the app and the tests
- [ ] All tests pass
- [ ] Code and PRs done within 2 hours; one video of at most 2 minutes uploaded to YouTube (Unlisted) within 30 minutes after
- [ ] The README has every PR link and the YouTube link
- [ ] Every file in the repo can be explained without AI help

### Video outline (max 2 minutes)

| Time | What to show |
|------|--------------|
| 0:00–0:10 | Quick tour of the repo and the merged PRs |
| 0:10–1:50 | About 20 seconds per question: what it does, and the most important decision |
| 1:50–2:00 | One thing to improve with more time |
