# Phase 2 · Desktop UI — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone.
History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases:
`phase-0.md`, `phase-1.md` (closed summaries; carried-forward items are listed there).

**Updated:** 2026-10-17, end of unit p2x

## Where things stand

- **Phase:** 2 · Desktop UI (third of six). Still on the seam stage: moving upstream's UI into `shared/commonMain` behind Android seams. No desktop window shows the real app yet.
- **Last committed batch:** p2x (`PlatformContext.externalCacheDir`/`dataDir` stand-ins; Context guard no longer flags scope functions, a file's own `Context.` extensions, or `.assets` on non-contexts; 8 files rehomed incl. `DriversSettingsDelegate`, `StorageSettingsDelegate`, `SettingsLayout`, `SaveSyncPresentation`), verified. See the p2x entry in `DEVLOG.md`.
- **Counts:** app 30 files, commonMain 1,535, androidMain 379. Navigation-graph closure: 149 files, 21 Android roots.
- **Baseline for "Android unchanged":** plain upstream v2.24.0, `build/desktop-baseline/app-debug-upstream-v2.24.0.apk`. Plain save proof: `build/desktop-check/save-proof/v224-plain-1017`.
- Nothing uncommitted.

## Next action

1. List the remaining roots: `python3 -I scripts/desktop/restructure/movable.py --roots ui/navigation/NavGraph.kt` (21). Choose the next batch with `--plan <n> ui/navigation/NavGraph.kt` (`--services-only` for slices) or `--rank-android`.
   Smaller next: Coil `SubcomposeAsyncImage` (`AppsScreen`, `AppPickerModal`), `FirstRunViewModel` (`Settings.canDrawOverlays` beyond the `Settings` stand-in), `ImagePrefetchManager` (Context + `coil.imageLoader`), `SettingsAboutRouter` (`LocaleManager`/`LocaleList`), `SettingsInitRouter` (`DisplayManager`), `PermissionsSettingsDelegate`.
   Then `SettingsFontRouter`, `SettingsScreen`/`ViewModel`, `SteamSettingsDelegate`, players (Media3, YouTube WebView), `QrScanner`, `Box3dCover`, `SectionBreadcrumb`, `SystemStatusBar`, `OnScreenControls`, `BgmTrackArt`, `MusicBrowserViewModel`.
2. Run the pipeline (`extract_decls.py --refresh`, `inline_constants.py`, `move_companion_functions.py`, `apply_moves.py`, `move_members.py`, `actualize.py`, `slices.py`, `rewrite_imports.py`, then `movable.py --check`; the list is in `reapply.sh`). Then `movable.py --rehome`, append its output to `moves.txt` as a new labelled `[commonMain]` section, and run `apply_moves.py` and `rewrite_imports.py` again.
3. `git add -A app/src/main/kotlin shared/src`, compile first (`quiet.sh <label> ./gradlew :shared:compileKotlinDesktop :shared:compileDebugKotlinAndroid`, about 3 min), then verify with `scripts/desktop/check/verify.sh <label> build/desktop-check/save-proof/v224-plain-1017` in the background (about 20 min).
4. Commit, log in `DEVLOG.md`, rewrite this file, end the unit.

After the roots are gone: the desktop shell, SDL3 input and screen bring-up (Phase 2 plan, steps 6–8, in `PORT_PLAN.md`). `ClipboardTextReader`'s desktop actual returns null; give it a real clipboard there.

## Commands and tools

- Restructure tools: `scripts/desktop/restructure/` (manifests `*.txt`, scripts `*.py`; `movable.py --path/--roots/--plan/--rank` for planning).
- Verification: `scripts/desktop/check/verify.sh`, `ab-check.sh`, `save-proof.sh`; testbed: `scripts/desktop/testbed/` and `docs/desktop/testbed-runbook.md`.
- Quiet runner for anything noisy: `scripts/desktop/quiet.sh <label> <command...>`.
- JDK: `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.

## Gotchas worth remembering

- To see which line trips a guard, import `movable` and call the guard's `search` per line (the Context guard is `movable.ContextGuard()`); the `--roots` report truncates its reasons.

- `movable.py --check` and `--rehome` do not prove that an expect slice carries every member a rehomed file calls. Only the compile catches that. An expression-bodied member needs its return type in `slices.txt`, or the slice leaves it out.
- A stand-in extension property (like `className`) also needs an entry in `rewrite_imports.py`'s stand-in lists, or the moved files won't import it.
- Check a new stand-in's name against `shared/src/commonMain/kotlin/com/nendo/argosy/platform/` first (`PlatformPackageManager` was taken; only the compile caught it).
- Desktop actuals are generated once and hand-owned from then on; `slices.py` appends missing members to them.
- Never start a verification while files are still being edited (the race has happened twice).
- A moved `@HiltViewModel` is proven only by a device run; generated-code evidence only from a build that ran the generating task.
- Stand-ins must not add null checks upstream never had (unit tests run on `android.jar` stubs).
- The plain proof ages: once it is more than 24 hours older than a candidate, upstream's daily checks run again in the candidate. The masks now cover the two epoch-millisecond keys; a new unmasked difference of that kind means another clock key, not a regression, until shown otherwise.
