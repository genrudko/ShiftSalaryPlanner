# ShiftSalaryPlanner — M15 integration archive / M16 handoff

Updated 2026-10-12, new VPS `v10360`. **The M15 development worktree and its branch were cleanly retired after integration.** Their old path `/home/admin/workspaces/shift-salary-planner/m15-weather-settings` is no longer active; do not resume work there.

## Canonical baseline

- Repository: `github.com/genrudko/ShiftSalaryPlanner`
- Canonical local checkout: `/home/admin/control-plane/migration-staging/shift-salary-planner`
- Default branch: `master`; verified GitHub SHA before this final docs-only closeout: `c806e0f23ef228bc2e3e642a8dc7eba2c410fd0d` (M13–M15 + integration ledger). `git status` clean and `origin/master` synchronized.
- Owner-authorized merge method: **non-force fast-forward** from M12 `f10baa8` to completed M15 `3a867e4`, with subsequent docs/whitespace integration commits `13b98f3` and `c806e0f`. Full history preserved, no merge/rebase/squash.
- M15 proof gate from original feature worktree: `072f5657fd998c35b7b2de9fdc3ac568` (422 JVM / 58 screenshots / phone+Wear APK/lint).
- **Repeated from merged canonical `master`**: `4d999f4efa0d968ed4221e653213599c` (exit 0, BUILD SUCCESSFUL in 8m19s, all 106 tasks executed). Android JVM 422/422; 58 screenshots; phone and Wear APK; lint 0 errors; Wear JVM NO-SOURCE.
- Merged-master APK checksums: Android `d2361efa7903d16e02609089c81175c9fb11a539ae13d54b7ffb7fd93c2f1764`; Wear `6e833d46cfd5b6303ea87058690ca5a0f41d655deca58fb3c233aceb891da07a`.
- Worktree cleanup: removed clean detached `m15-task5-baseline` and clean integrated `m15-weather-settings`, then deleted local `work/m15-weather-settings-recovery` and temporary GitHub `integration/m13-m15-verified` after validating their heads as ancestors of `master`. No personal untracked files were present. Kept externally managed detached qualification checkout and migration/recovery archive refs.

## Next milestone / limitations

Start M16 from a **fresh isolated worktree based on the current `origin/master`**, not the retired M15 path. M16–M21 and real-device GPS, notification and JobScheduler verification remain pending. M15 integration is **not** a release, and no APK was signed for production or deployed. See `docs/project/CURRENT_STATE.md` and `docs/project/WORK_PROGRESS.md` for the latest evidence and source-of-truth status.
