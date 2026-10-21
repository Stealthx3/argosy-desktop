# Phase 2 · Desktop UI — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone.
History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases:
`phase-0.md`, `phase-1.md` (closed summaries; carried-forward items are listed there).

**Updated:** 2026-10-21, end of unit p2z

## Where things stand

- **Phase:** 2 · Desktop UI (third of six). Still on the seam stage: moving upstream's UI into `shared/commonMain` behind Android seams. No desktop window shows the real app yet.
- **Last committed batch:** p2z (display-manager stand-in, `Settings.System.canWrite`, `Application.startActivity`; `PermissionsSettingsDelegate` rehomed, `SettingsInitRouter` cleared but waits on `SettingsViewModel`), verified. See the p2z entry in `DEVLOG.md`.
- **Counts:** app 30 files, commonMain 1,540, androidMain 378. Navigation-graph closure: 146 files, 16 Android roots.
- **Baseline for "Android unchanged":** plain upstream v2.24.0, `build/desktop-baseline/app-debug-upstream-v2.24.0.apk`. Plain save proof: `build/desktop-check/save-proof/v224-plain-1021` (recaptured 2026-10-21; recapture again when it is days older than the candidate).
- Nothing uncommitted.

## Next action

1. List the remaining roots: `python3 -I scripts/desktop/restructure/movable.py --roots ui/navigation/NavGraph.kt` (16). Choose the next batch with `--plan <n> ui/navigation/NavGraph.kt` (`--services-only` for slices) or `--rank-android`.
   Smaller next: `SettingsAboutRouter` (a `setApplicationLocales(context, tag)` stand-in plus a `rewrite_imports.py` rewrite of the multi-line `LocaleManager` block), `ImagePrefetchManager` (Context + `coil.imageLoader`), `QrScanner` (unblocks `FirstRunViewModel`, which is otherwise clear now).
   Then `SettingsFontRouter`, `SettingsScreen`/`ViewModel`, `SteamSettingsDelegate`, players (Media3, YouTube WebView), `QrScanner`, `Box3dCover`, `SectionBreadcrumb`, `SystemStatusBar`, `OnScreenControls`, `BgmTrackArt`, `MusicBrowserViewModel`.
2. Run the pipeline (`extract_decls.py --refresh`, `inline_constants.py`, `move_companion_functions.py`, `apply_moves.py`, `move_members.py`, `actualize.py`, `slices.py`, `rewrite_imports.py`, then `movable.py --check`; the list is in `reapply.sh`). Then `movable.py --rehome`, append its output to `moves.txt` as a new labelled `[commonMain]` section, and run `apply_moves.py` and `rewrite_imports.py` again.
3. `git add -A app/src/main/kotlin shared/src`, compile first (`quiet.sh <label> ./gradlew :shared:compileKotlinDesktop :shared:compileDebugKotlinAndroid`, about 3 min), then verify with `scripts/desktop/check/verify.sh <label> build/desktop-check/save-proof/v224-plain-1021` in the background (about 20 min).
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
- `DownloadManagerPlatformFolderTest` can fail in the full gate with "Dispatchers.Main is used concurrently with setting it" (test-ordering race, untouched by the port). Rerun the class alone, then the full verification.
- A fully qualified rewrite rule must skip import lines (`(?<!import )`): `slices.py` runs imports through `rewrite_imports.RULES` to judge portability, so a rule that matches an import line makes that Android type look portable and widens slices.
- If the testbed containers are gone (it happened while paused), `scripts/desktop/testbed/up.sh` recreates them on the kept volumes; the proof's snapshot restore does the rest. `adb` is not on PATH in a plain shell; the scripts find it.
- The plain proof ages: once it is more than 24 hours older than a candidate, upstream's daily checks run again in the candidate. The masks now cover the two epoch-millisecond keys; a new unmasked difference of that kind means another clock key, not a regression, until shown otherwise.
