---
name: merge-ticket
description: Merge a question's reviewed PR into main, sync main, and confirm tests still pass. Use when the user says "/merge-ticket q1", "merge Q2", "ship it" after reviewing a ticket PR.
argument-hint: q1..q5
---

# Merge a ticket PR

Argument: `q1`..`q5`. Only run this when the user asked to merge.

1. Show the PR state and checks: `gh pr view feature/... --json state,url,mergeable` (branch names are in `scripts/ticket.sh`). If it is not OPEN or not mergeable, stop and report why.
2. Merge:

   ```bash
   scripts/ticket.sh merge q<n>
   ```

   This squash-merges, keeps the branch (the submission checklist expects 5 branches), pulls `main` and re-runs the tests.
3. Report: merged PR link, test result on `main`, and the next question (`/ticket q<n+1>`). After Q5, remind the user of the video step: record at most 2 minutes, upload to YouTube as Unlisted, add the link to the `**Video:**` line in the README.
