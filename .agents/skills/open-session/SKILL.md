---
name: open-session
description: Open or resume a development session for the Java Recipe Healthifier repository. Use when the user says open session, start session, resume work, continue the project, or asks Codex to establish current project status before implementing, auditing, documenting, or preparing the Hackster submission.
---

# Open Session

## Overview

Establish an evidence-backed starting point, identify the next bounded slice, and begin useful work without disturbing existing changes.

## Workflow

1. Confirm the repository contains `pom.xml`, `README.md`, and `src/main/java/com/healthifier`.
2. Inspect `git status -sb`, the current branch, latest commit, and remotes. Treat existing changes as user-owned. Do not switch branches, discard changes, pull, or publish unless authorized.
3. Read `PLAN.md`, `DECISIONS.md`, `docs/SUBMISSION_CHECKLIST.md`, and relevant parts of `README.md` completely when present.
4. Inspect files and tests relevant to the request. Use `rg` or `rg --files` first.
5. Establish verification expectations:
   - Require Java 26.
   - Use `.\\mvnw.cmd clean verify` on Windows or `./mvnw clean verify` on macOS/Linux for a full build.
   - Use narrower tests for short development loops, but finish risky Java changes with the full build.
   - If Maven fails only because sandboxed network access blocks dependency metadata, rerun with appropriate approval and report the distinction accurately.
6. When GitHub state matters, inspect authentication, pull requests, and Java 26 CI without changing remote state. Do not merge, close, commit, or push unless requested.
7. Reconcile the request with `PLAN.md`. Update it only when work materially changes project status.
8. State the current condition, selected next slice, and verification target. Begin work unless a material user decision is required.

## Guardrails

- Preserve the vocabulary-based compliance semantics in `DECISIONS.md`; never describe results as medical or nutritional certification.
- Keep Java 26 usage intentional and avoid preview features added only for novelty.
- Preserve the zero-runtime-dependency design unless the user approves otherwise.
- Preserve the phone server's trusted-network warning, validation, escaping, request limits, and security headers.
- Do not mark eligibility, screenshots, video, or Hackster submission complete without user evidence.
- Treat August 16, 2026 at 11:59 PM PT as the documented deadline unless Hackster provides a newer written correction.

## Opening Update

Report the branch and worktree state, latest verification and CI state, remaining repository and entrant tasks, and the slice being started with its success check.
