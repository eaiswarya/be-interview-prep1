#!/usr/bin/env bash
# Ticket harness: the git/GitHub mechanics for one assignment question.
#
#   scripts/ticket.sh start <q>              sync main, create or switch to the question's branch
#   scripts/ticket.sh check                  run the full test suite
#   scripts/ticket.sh pr <q> <body-file>     test, push, open the PR, record its link in the README
#   scripts/ticket.sh merge <q>              merge the PR, sync main, re-run tests
#   scripts/ticket.sh status                 show every question's branch and PR state
#
# <q> is q1..q5 (or 1..5).
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

die() { echo "error: $*" >&2; exit 1; }

# Question number -> branch and title, as fixed by the assignment.
branch_for() {
  case "$1" in
    1) echo "feature/q1-task-api" ;;
    2) echo "feature/q2-url-shortener" ;;
    3) echo "feature/q3-auth" ;;
    4) echo "feature/q4-product-catalog" ;;
    5) echo "feature/q5-order-service" ;;
    *) die "unknown question '$1' (expected q1..q5)" ;;
  esac
}

title_for() {
  case "$1" in
    1) echo "Task Manager API" ;;
    2) echo "URL Shortener" ;;
    3) echo "Authentication & Roles" ;;
    4) echo "Product Catalog" ;;
    5) echo "Order Service" ;;
  esac
}

num() {
  local n="${1:-}"
  n="${n#q}"; n="${n#Q}"
  [[ "$n" =~ ^[1-5]$ ]] || die "expected a question q1..q5, got '${1:-}'"
  echo "$n"
}

require_clean() {
  [[ -z "$(git status --porcelain --untracked-files=no)" ]] \
    || die "working tree has uncommitted changes; commit or stash them first"
}

run_tests() {
  echo "==> running tests"
  ./mvnw -q test
  echo "==> tests passed"
}

cmd_start() {
  local n branch
  n="$(num "${1:-}")"; branch="$(branch_for "$n")"
  require_clean
  git switch main
  git pull --ff-only
  if git show-ref --verify --quiet "refs/heads/$branch"; then
    git switch "$branch"
    echo "==> switched to existing $branch"
  else
    git switch -c "$branch"
    echo "==> created $branch from latest main"
  fi
}

cmd_pr() {
  local n branch title body url
  n="$(num "${1:-}")"; branch="$(branch_for "$n")"; title="$(title_for "$n")"
  body="${2:-}"
  [[ -n "$body" && -f "$body" ]] || die "pass the PR description file: ticket.sh pr q$n <body-file>"
  grep -q '^## Problem' "$body" && grep -q '^## Approach' "$body" \
    && grep -q '^## Decisions & trade-offs' "$body" && grep -q '^## How to test' "$body" \
    || die "$body must follow .github/pull_request_template.md (Problem, Approach, Decisions & trade-offs, How to test)"
  [[ "$(git branch --show-current)" == "$branch" ]] || die "not on $branch"
  require_clean
  [[ -n "$(git log --oneline main..HEAD)" ]] || die "no commits on $branch yet"
  run_tests

  git push -u origin "$branch"
  url="$(gh pr view "$branch" --json url,state -q 'select(.state=="OPEN") | .url' 2>/dev/null || true)"
  if [[ -n "$url" ]]; then
    gh pr edit "$branch" --body-file "$body" >/dev/null
    echo "==> PR already open, description updated: $url"
  else
    url="$(gh pr create --base main --head "$branch" --title "Q$n — $title" --body-file "$body")"
    echo "==> opened $url"
  fi

  # Record the PR link in the README question table on this branch.
  if ! grep -q "^| $n | .*$url" README.md; then
    sed -i "s#^| $n | \(.*\) | *[^|]* *|\$#| $n | \1 | $url |#" README.md
    if ! git diff --quiet README.md; then
      git commit -q -m "Add Q$n PR link to README" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" README.md
      git push -q
      echo "==> README updated with the PR link"
    fi
  fi
  echo "$url"
}

cmd_merge() {
  local n branch state
  n="$(num "${1:-}")"; branch="$(branch_for "$n")"
  require_clean
  state="$(gh pr view "$branch" --json state -q .state)"
  [[ "$state" == "OPEN" ]] || die "PR for $branch is $state, not OPEN"
  # Keep the branch: the submission checklist expects 5 branches.
  gh pr merge "$branch" --squash
  git switch main
  git pull --ff-only
  run_tests
  echo "==> Q$n merged; main is up to date"
}

cmd_status() {
  local n branch
  for n in 1 2 3 4 5; do
    branch="$(branch_for "$n")"
    printf 'Q%s  %-28s  %s\n' "$n" "$branch" \
      "$(gh pr view "$branch" --json state,url -q '.state + "  " + .url' 2>/dev/null || echo 'no PR')"
  done
}

case "${1:-}" in
  start)  shift; cmd_start "$@" ;;
  check)  run_tests ;;
  pr)     shift; cmd_pr "$@" ;;
  merge)  shift; cmd_merge "$@" ;;
  status) cmd_status ;;
  *) sed -n '2,10p' "$0"; exit 1 ;;
esac
