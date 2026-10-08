# Backend Interview Prep — Product Doc

Oct 8, 2026 · @Aryan C R

## Overview

We will ship one Spring Boot 3.x service (Java 17+) with five backend features, each delivered as its own branch, PR and merge, within 2 hours, followed by a 2-minute walkthrough video.

The five features cover the core skills of backend work: CRUD with validation, link generation with safe counters, stateless auth, fast filtered listings with caching, and concurrency-safe ordering.

| # | Feature | Problem it solves | Branch |
| --- | --- | --- | --- |
| 1 | Task Manager API | Create, track and filter tasks with clean validation and errors | `feature/q1-task-api` |
| 2 | URL Shortener | Turn long URLs into short, expiring, tracked links | `feature/q2-url-shortener` |
| 3 | Authentication & Roles | Only logged-in users reach the API; admins get extra access | `feature/q3-auth` |
| 4 | Product Catalog | Browse a growing catalog quickly with filters, paging and caching | `feature/q4-product-catalog` |
| 5 | Order Service | Place orders that never oversell stock, even under heavy load and retries | `feature/q5-order-service` |

## Features and requirements

Each feature lists what the assignment requires, and how it is accepted.

### Q1 — Task Manager API

**Requirements**

- Task fields: title (required, max 100 chars), description, status (TODO / IN\_PROGRESS / DONE), due date (not in the past), created date
- Create, list, get one, update and delete; filter the list by status
- Field-level messages for every invalid input
- One consistent JSON error format for invalid input, not found and unexpected errors

**Acceptance:** invalid input → 400 with field errors; unknown task → 404; at least one automated test.

### Q2 — URL Shortener

**Requirements**

- Submit a long URL with optional expiry; return short code and short URL
- Short URL redirects to the original
- Count every visit; stats endpoint returns original URL, visit count, created date
- Codes ≤ 8 chars, unique, URL-safe
- Reject invalid URLs; unknown code → 404, expired code → 410 Gone

**Acceptance:** shortening the same URL twice behaves as decided and is explained; visit counts stay accurate under concurrent opens; at least one test.

### Q3 — Authentication & Roles

**Requirements**

- Register and log in; passwords stored hashed
- Stateless auth (no server sessions) for web and mobile
- Login expires after 15 minutes
- Roles USER and ADMIN: any user sees own profile; only ADMIN lists all users
- Not logged in → 401 JSON; wrong role → 403 JSON

**Acceptance:** a test proves a USER cannot reach the admin endpoint; no secrets in source.

### Q4 — Product Catalog

**Requirements**

- Product: name, category, price, stock, rating, created date; seed 100 products on startup
- Pagination and sorting by any field; response includes total count and total pages
- Optional, freely combinable filters: category, price range, in-stock only, name search
- Page size capped at 100
- Fast repeated single-product lookups, never stale after update or delete

**Acceptance:** any filter combination works in one request; repeated lookups skip the DB and you can show how you know; at least one test.

### Q5 — Order Service

**Requirements**

- Orders contain one or more items against limited stock
- All-or-nothing: every item reserved, or none
- Stock never negative or oversold under concurrent orders
- Retried requests never create duplicate orders; design how a retry is recognised
- Insufficient stock → 409 with a clear message
- Cancelling an order returns its stock

**Acceptance:** a test fires 50 simultaneous orders against stock 10 — exactly 10 succeed and stock ends at 0; retrying the same request creates one order.

## Delivery plan and evaluation

The work runs in strict order Q1 → Q5, one branch, PR and merge each, then a video within 30 minutes of the cut-off.

**Milestones (150 minutes total)**

1. Setup: public repo `be-interview-prep`, README, base Spring Boot project on `main`
2. Q1 Task Manager API — 15 min
3. Q2 URL Shortener — 15 min
4. Q3 Authentication & Roles — 25 min
5. Q4 Product Catalog — 25 min
6. Q5 Order Service — 40 min (2-hour cut-off)
7. Video — 30 min: record ≤ 2 minutes, upload to YouTube as Unlisted, add link to README

**Per-feature workflow:** branch from latest `main` → small, meaningful commits → PR with the template (Problem, Approach, Decisions & trade-offs, How to test) → self-review the diff → merge → pull `main`.

**Video script**

| Time | Content |
| --- | --- |
| 0:00–0:10 | Repo tour and merged PRs |
| 0:10–1:50 | \~20 s per feature: what it does and its key decision |
| 1:50–2:00 | One improvement with more time |

**Evaluation criteria:** working code, clean structure, tests, Git/PR discipline, and above all understanding. A feature whose code can't be explained counts as not done.

**Submission checklist**

- [ ] Public repo `be-interview-prep` with a README
- [ ] 5 branches, 5 PRs, all merged into `main`
- [ ] Every PR uses the description template
- [ ] README explains how to run the app and the tests
- [ ] All tests pass
- [ ] Code and PRs within 2 hours; video ≤ 2 min uploaded within 30 minutes after
- [ ] README lists every PR link and the YouTube link
- [ ] Every file can be explained without AI help

**Stretch goals (only after all five merge)**

- Q1: interactive API documentation
- Q2: let users choose their own custom short code
- Q3: stay logged in beyond 15 minutes without re-entering the password, plus a logout that ends that
- Q4: make the fast lookups work across several running instances
- Q5: a second approach to the concurrency problem with a short comparison; run the tests against a real database
