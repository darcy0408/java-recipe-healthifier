---
name: close-session
description: Close and hand off a development session for the Java Recipe Healthifier repository. Use when the user says close session, wrap up, stop work, finish for now, prepare a handoff, or asks Codex to verify changes and leave clear project status before ending work.
---

# Close Session

## Overview

Leave the repository verified, accurately documented, and easy to resume. Closing a session does not authorize publishing or destructive cleanup.

## Workflow

1. Inspect `git status -sb`, the current branch, staged and unstaged diffs, untracked files, and `git diff --check`. Preserve unrelated user changes.
2. Review completed work against the user request. Do not call partial work complete.
3. Verify in proportion to risk:
   - For Java or build changes, run `.\\mvnw.cmd clean verify` on Windows or `./mvnw clean verify` on macOS/Linux with Java 26.
   - When compliance changes, also exercise `examples/adversarial-chicken-parmesan.txt` and `examples/unresolved-rice-bowl.txt`.
   - For documentation-only changes, validate paths, commands, claims, deadline text, and `git diff --check`; run the build when documentation claims a new verified result.
   - Record the exact command, failure, and reason for checks that cannot run. Never convert an environmental failure into a passing claim.
4. Reconcile repository records:
   - Update `PLAN.md` with genuinely completed and next work.
   - Update `DECISIONS.md` only for durable engineering decisions.
   - Update `docs/SUBMISSION_CHECKLIST.md` only for tasks proven complete.
5. Check the diff for accidental secrets, personal data, generated artifacts, temporary files, and unintended third-party assets. Do not delete questionable user files without permission.
6. Inspect GitHub CI or pull-request state when relevant and authorized. Do not commit, push, merge, close pull requests, or delete branches unless explicitly requested.
7. Confirm the final worktree state after all edits and checks.

## Handoff

Provide a self-contained closing summary containing:

- outcome and user-visible behavior;
- files or components materially changed;
- exact verification commands and results, including test counts;
- branch, commit, worktree, pull-request, and CI state when known;
- blockers or unverified assumptions;
- the smallest useful next step;
- remaining entrant-only actions such as eligibility confirmation, screenshots, video, Hackster entry creation, and final submission.

Use clickable local file links. Keep the summary factual and do not claim publication unless remote state actually changed.
