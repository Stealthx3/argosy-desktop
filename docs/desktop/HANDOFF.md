# Handoff — read this first

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone.
History and reasoning live in `DEVLOG.md`; read only the parts this file points to.

**Updated:** 2026-10-17 (seeded by the orchestrator session when work units were introduced)

## Where things stand

- **Phase:** 2 · Desktop UI (third of six). Still on the seam stage: moving upstream's UI into `shared/commonMain` behind Android seams. No desktop window shows the real app yet.
- **Last committed batch:** `f13e679f5` (p2u: navigation, `ArgosyApp`/`ArgosyViewModel` seams, 142 files rehomed), verified. Pacing rule committed in `3ffa1950e`.
- **Counts:** app 30 files, commonMain ~1,508, androidMain ~400. Navigation-graph closure: 174 files, 34 Android roots after the parked tooling.
- **Baseline for "Android unchanged":** plain upstream v2.24.0, `build/desktop-baseline/app-debug-upstream-v2.24.0.apk`. Run proof baseline and candidate on the same day.

## In progress (uncommitted, unverified)

Parked tooling for the next batch (see `DEVLOG.md`, last "Parked" paragraph):
- `scripts/desktop/restructure/movable.py`: `DisplayAffinityHelper` stand-in guard removed; `Application` guard allows `openStorageAccessSettings`.
- `scripts/desktop/restructure/rewrite_imports.py`: no longer aliases `DisplayAffinityHelper` to `DualScreenDeviceInfo`.
- `scripts/desktop/restructure/slices.txt`: lists `GameActivityStarter` (slice not generated yet).

## Next action

1. Run the restructure pipeline: extract, apply moves, actualize, slices, rewrite, `movable.py --check`; then `movable.py --rehome`.
2. Compile, then verify with `scripts/desktop/check/verify.sh <label> <plain-proof-dir>` (gate, A/B, save-sync proof).
3. Commit the batch, log it in `DEVLOG.md`, rewrite this file, end the work unit.

Remaining Phase 2 roots after that (from `DEVLOG.md`, "Where it stands" under p2u): Settings routers/delegates and `SettingsScreen`/`ViewModel`, players (Media3, YouTube WebView), `QrScanner`, `Box3dCover`, `SectionBreadcrumb`, session end, first run, clipboard, image prefetch. Then the desktop shell, SDL3 input and screen bring-up (Phase 2 plan, steps 6–8).

## Commands and tools

- Restructure tools: `scripts/desktop/restructure/` (manifests `*.txt`, scripts `*.py`; `movable.py --path/--roots/--plan/--rank` for planning).
- Verification: `scripts/desktop/check/verify.sh`, `ab-check.sh`, `save-proof.sh`; testbed: `scripts/desktop/testbed/` and `docs/desktop/testbed-runbook.md`.
- Quiet runner for anything noisy: `scripts/desktop/quiet.sh <label> <command...>` (full log to `build/desktop-logs/`, prints only status, time and error lines).
- JDK: `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.

## Gotchas worth remembering

- Never start a verification while files are still being edited (the race has happened twice).
- A moved `@HiltViewModel` is proven only by a device run; generated-code evidence only from a build that ran the generating task.
- Stand-ins must not add null checks upstream never had (unit tests run on `android.jar` stubs).
