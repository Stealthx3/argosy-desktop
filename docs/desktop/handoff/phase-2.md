# Phase 2 · Desktop UI — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone.
History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases:
`phase-0.md`, `phase-1.md` (closed summaries; carried-forward items are listed there).

**Updated:** 2026-10-22, end of unit p3g

## Where things stand

- **Phase:** 2 · Desktop UI (third of six). Still on the seam stage: moving upstream's UI into `shared/commonMain` behind Android seams. No desktop window shows the real app yet.
- **Last committed batch:** p3g (`OnScreenControls`' pure slot-layout helpers extracted to commonMain `OnScreenSlotLayout.kt`; `TouchLayoutEditor`, `TouchLayoutEditorModal` rehomed; the overlay itself stays androidMain on `GLRetroView`), verified. See the p3g entry in `DEVLOG.md`.
- **Counts:** app 30 files, commonMain 1,568, androidMain 367. Navigation-graph closure: 128 files, 6 Android roots.
- **Baseline for "Android unchanged":** plain upstream v2.24.0, `build/desktop-baseline/app-debug-upstream-v2.24.0.apk`. Plain save proof: `build/desktop-check/save-proof/v224-plain-1021b` (recaptured 2026-10-21; recapture again when it is days older than the candidate).
- Nothing uncommitted.

## Next action

1. Run the tools from the repo root. List the remaining roots: `python3 -I scripts/desktop/restructure/movable.py --roots ui/navigation/NavGraph.kt` (6). Choose the next batch with `--plan <n> ui/navigation/NavGraph.kt` (`--services-only` for slices) or `--rank-android`.
   Left: `SettingsScreen`/`ViewModel`, `SteamSettingsDelegate`, players (`InlineTilePlayer`, `PageThemePlayer` on Media3, `YouTubeVideoPlayer` WebView). Before slicing a root, run `--path ui/navigation/NavGraph.kt <root>` to see which declarations the graph actually reaches; p3g found only pure helpers were reached and extracted those instead. `--path` needs two arguments (start file, target prefix).
   A composable that only wraps an Android probe: extract the probe to its own androidMain file and slice it with `*` (the p3e pattern); put the data types it returns in commonMain first, or the slice leaves the function out. `RomMConfigForm` uses `QrScannerWithPermission` too but did not rehome; check why with `--path`.
   A new stand-in for a class with listener interfaces: write the Android actual as a wrapper with Kotlin function-type parameters (see `PlatformMedia.kt`), not a typealias.
2. Run the pipeline (`extract_decls.py --refresh`, `inline_constants.py`, `move_companion_functions.py`, `apply_moves.py`, `move_members.py`, `actualize.py`, `slices.py`, `rewrite_imports.py`, then `movable.py --check`; the list is in `reapply.sh`). Then `movable.py --rehome`, append its output to `moves.txt` as a new labelled `[commonMain]` section, and run `apply_moves.py` and `rewrite_imports.py` again.
3. `git add -A app/src/main/kotlin shared/src`, compile first (`quiet.sh <label> ./gradlew :shared:compileKotlinDesktop :shared:compileDebugKotlinAndroid`, about 3 min), then verify with `scripts/desktop/check/verify.sh <label> build/desktop-check/save-proof/v224-plain-1021b` in the background (about 20 min).
4. Commit, log in `DEVLOG.md`, rewrite this file, end the unit.

After the roots are gone: the desktop shell, SDL3 input and screen bring-up (Phase 2 plan, steps 6–8, in `PORT_PLAN.md`). `ClipboardTextReader`'s desktop actual returns null; give it a real clipboard there. The QR scanner's desktop actual composes nothing and `deviceHasCamera` is false; revisit if desktop gets a camera path. `PlatformMediaPlayer`'s desktop actual has no audio backend (it reports an error) and `PlatformMediaMetadataRetriever` returns no embedded art; give both real implementations with Phase 3's audio work. `Box3dCover`'s desktop actual draws nothing and calls `onUnavailable` (callers fall back to the flat box art); give it a Skia renderer (`Canvas.concat` with a 4-point homography for `setPolyToPoly`).

## Commands and tools

- Restructure tools: `scripts/desktop/restructure/` (manifests `*.txt`, scripts `*.py`; `movable.py --path/--roots/--plan/--rank` for planning).
- Verification: `scripts/desktop/check/verify.sh`, `ab-check.sh`, `save-proof.sh`; testbed: `scripts/desktop/testbed/` and `docs/desktop/testbed-runbook.md`.
- Quiet runner for anything noisy: `scripts/desktop/quiet.sh <label> <command...>`.
- JDK: `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.

## Gotchas worth remembering

- After a `slices.py` change, run `slices.py` then `rewrite_imports.py` twice and check the second run adds nothing (`added to` lines); a spill-over into other slices shows in `git status`.

- To see which line trips a guard, import `movable` and call the guard's `search` per line (the Context guard is `movable.ContextGuard()`); the `--roots` report truncates its reasons.

- Block rewrites (`TEXT_BOUNDS_BLOCK`, `DISPLAY_NAME_QUERY`) run before unused imports are dropped, and `movable.py` imports `rewrite_imports` to strip the same blocks in its guards (`StrippedGuard`). When `SettingsViewModel` moves, check `SettingsFontRouter` compiles through `DISPLAY_NAME_QUERY` and the `openInputStream` import.
- `movable.py --check` and `--rehome` do not prove that an expect slice carries every member a rehomed file calls. Only the compile catches that. An expression-bodied member needs its return type in `slices.txt`, or the slice leaves it out.
- A stand-in extension property (like `className`) also needs an entry in `rewrite_imports.py`'s stand-in lists, or the moved files won't import it.
- Check a new stand-in's name against `shared/src/commonMain/kotlin/com/nendo/argosy/platform/` first (`PlatformPackageManager` was taken; only the compile caught it).
- Desktop actuals are generated once and hand-owned from then on; `slices.py` appends missing members to them.
- `verify.sh`'s A/B step can fail silently: its output is filtered by `grep`, so no `home:`…`datastore:` lines means it did not finish. Check `<label>-en/` for `candidate-*.png` and rerun `ab-check.sh` alone (the command is in `verify.sh`, about 6 min).
- `slices.py` slices only public top-level functions (no `internal`).
- Never start a verification while files are still being edited (the race has happened twice).
- A moved `@HiltViewModel` is proven only by a device run; generated-code evidence only from a build that ran the generating task.
- Stand-ins must not add null checks upstream never had (unit tests run on `android.jar` stubs).
- `DownloadManagerPlatformFolderTest` can fail in the full gate with "Dispatchers.Main is used concurrently with setting it" (test-ordering race, untouched by the port). Rerun the class alone, then the full verification.
- A fully qualified rewrite rule must skip import lines (`(?<!import )`): `slices.py` runs imports through `rewrite_imports.RULES` to judge portability, so a rule that matches an import line makes that Android type look portable and widens slices.
- If the testbed containers are gone (it happened while paused), `scripts/desktop/testbed/up.sh` recreates them on the kept volumes; the proof's snapshot restore does the rest. `adb` is not on PATH in a plain shell; the scripts find it.
- The Battery Bay cached-copy count varies between runs of plain upstream (p3a: two plain proofs 25 minutes apart differed in 12 captures). If a proof differs only there with equal verdicts, recapture the plain proof (`save-proof.sh build/desktop-baseline/app-debug-upstream-v2.24.0.apk <dir>`, about 8 min) and compare again before calling it a regression.
- The plain proof ages: once it is more than 24 hours older than a candidate, upstream's daily checks run again in the candidate. The masks now cover the two epoch-millisecond keys; a new unmasked difference of that kind means another clock key, not a regression, until shown otherwise.
