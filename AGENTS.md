# ShiftSalaryPlanner Agent Guide

This file is the mandatory entry point for AI/coding executors working on `genrudko/ShiftSalaryPlanner`.

## Read first

Before making changes, read in this order:

1. `README.md`
2. `docs/project/CURRENT_STATE.md`
3. `docs/project/VNEXT_MODERNIZATION.md`
4. the current bounded task / issue / PR description
5. relevant implementation files and tests

Then inspect the actual Git branch, HEAD, worktree status and diff. Never assume a chat summary is newer than the repository.

## Canonical project model

- GitHub is the intended canonical source after M0 baseline recovery is proven.
- `CURRENT_STATE.md` is the live boundary and must be updated when a milestone changes state.
- `VNEXT_MODERNIZATION.md` is the long-lived architecture and roadmap.
- A task contract may narrow scope but must not silently contradict canonical architecture. Escalate or update the decision first.
- Do not confuse this repository with `genrudko/shift_helper` or a Bridge project named `shift-helper`.

## Development rules

- No big-bang rewrite.
- Preserve behavior before changing appearance.
- For payroll, persistence and alarms: characterization/failing test before dangerous refactor whenever feasible.
- Do not change payroll semantics as incidental cleanup.
- Do not mix architecture refactor, redesign and broad dependency upgrades in one changeset.
- Keep bounded tasks and PRs reviewable.
- Prefer package/feature boundaries before adding Gradle modules.
- Do not add frameworks solely for architectural fashion.
- Do not destroy or reset recovery evidence/local owner state.
- Do not merge, release or deploy unless the owner or current task explicitly authorizes it.
- Production artifacts must come from clean, traceable Git state.

## Intended execution environment

Development is intended to move to Development Bridge on VPS using managed isolated worktrees and durable jobs for long operations. GitHub remains canonical; VPS runtime/worktrees are execution state, not an alternative source of truth.

Every implementation task should follow roughly:

```text
inspect actual state
→ confirm bounded scope
→ characterization/failing test when applicable
→ implement
→ targeted tests
→ relevant full gate
→ inspect diff
→ review
→ update CURRENT_STATE when milestone boundary changes
```

## Current boundary

Do not infer it from this file. Read `docs/project/CURRENT_STATE.md` every time.
