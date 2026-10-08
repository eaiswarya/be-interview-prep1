---
name: ticket
description: Implement one assignment question (q1..q5) end to end - branch from latest main, build it with tests in small commits, self-review, and raise the PR with the template. Use when the user says "/ticket q2", "implement Q3", "do the next ticket" or similar.
argument-hint: q1..q5
---

# Ticket: implement a question and raise its PR

Argument: the question, `q1`..`q5`. If none is given, run `scripts/ticket.sh status` and take the lowest question without a merged PR. Questions go strictly in order: if an earlier question's PR is not merged, stop and say so.

The requirements live in `docs/PRODUCT.md` (section `### Q<n>`). The original brief is `Backend_Interview_Prep_Assignment.pdf` if present. Do not add features the question does not ask for; optional "finish early" items only when the user asks.

## 1. Start

```bash
scripts/ticket.sh start q<n>
```

This syncs `main` and creates the branch from it. Never work on `main`.

## 2. Plan (show the user, keep it short)

Before writing code, post a plan of at most ~10 lines:
- the endpoints, entities and classes you will add
- the key design decision for this question and the alternative you rejected, with the reason. Each question has one the interviewer will ask about (Q1 error format, Q2 duplicate URLs and concurrent visit counts, Q3 stateless tokens and 401/403 JSON, Q4 filter composition and cache invalidation, Q5 oversell prevention and retry detection)
- the tests that prove each acceptance criterion

Then proceed without waiting unless a decision really belongs to the user.

## 3. Implement in small commits

Follow `CLAUDE.md` conventions. Work in slices, and commit after each one once it compiles and its tests pass:

1. Entity + repository
2. Service with business rules
3. Controller + DTOs + validation
4. Error handling for this feature
5. Tests for each acceptance criterion

Commit messages are imperative and specific, e.g. `Add create task endpoint`, `Return field errors for invalid input`. Never `fix`, `changes`, `final`, `wip`. End each commit message with the attribution trailer from the system instructions.

Run tests with `scripts/ticket.sh check` (or `./mvnw -q test -Dtest=<Class>` for one class while iterating).

## 4. Self-review

```bash
git diff main...HEAD
```

Read the whole diff as the interviewer would. Check for: dead code, hard-coded secrets, missing validation, wrong HTTP statuses, unused imports, and any line you could not explain. Fix findings in their own commits.

## 5. Raise the PR

Write the description to a scratch file using exactly the four headings of `.github/pull_request_template.md`:

- **Problem** - 1-2 lines.
- **Approach** - key classes and the request flow: controller -> service -> repository -> DB.
- **Decisions & trade-offs** - each decision, the alternative, and why. This is what the interviewer reads first.
- **How to test** - `./mvnw test`, the test classes, and 2-3 `curl` samples.

End it with the PR attribution line from the system instructions. Then:

```bash
scripts/ticket.sh pr q<n> <body-file>
```

This re-runs the tests, pushes, opens the PR titled `Q<n> — <title>`, and commits the PR link into the README table.

## 6. Hand off

Reply with the PR link, the one key decision in one sentence, and a reminder to review the diff before merging with `/merge-ticket q<n>`. Do not merge yourself unless the user asks.
