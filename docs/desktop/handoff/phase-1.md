# Phase 1 · Kotlin Multiplatform restructure — closed

**Closed:** 2026-10-16. Exit criteria met; `main` pushed. Upstream v2.24.0 later merged into `main` (`4508ad68d`) with `scripts/desktop/restructure/reapply.sh`.

## Outcome

- `shared` (commonMain / androidMain / desktopMain) and `desktopApp` build; `commonMain` has no Android imports (enforced by compiling it for the desktop).
- Android unchanged: unit tests green at every step; pixel-identical A/B screens (English and German) on the RomM testbed; identical database, DataStore, APK resource table and dependency versions; save-sync live proof identical at every door.
- The restructure is scripted and re-runnable (`scripts/desktop/restructure/`: manifests `*.txt`, `movable.py`, `apply_moves.py`, `rewrite_imports.py`, `extract_decls.py`, `reapply.sh`), so it re-applies to each upstream release.
- Testbed: local RomM 5.1 on `localhost:8093`, emulator `emulator-5556`, snapshots and checks under `scripts/desktop/testbed/` and `scripts/desktop/check/`; runbook `docs/desktop/testbed-runbook.md`.

## Carried forward

- Save-sync stages 2–4 → Phase 3, with the `FilePaths` and `BackgroundJobs` interfaces (`docs/desktop/save-sync-move-plan.md`).
- Flatpak runtime 25.08 → Phase 3 packaging.
- When a move takes a generated file, update its generator in the same step (token generator `scripts/gen-tokens.mjs` now writes to `shared`; Node is not installed here, so it has not been run).
- The "Android unchanged" baseline is now plain upstream v2.24.0 (`build/desktop-baseline/app-debug-upstream-v2.24.0.apk`), not the pre-Phase-1 APK.

## Details

`DEVLOG.md`, section "Phase 1 · Kotlin Multiplatform restructure" (plan, steps, "Phase 1 close").
