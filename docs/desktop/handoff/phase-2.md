# Phase 2 · Desktop UI — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone.
History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases:
`phase-0.md`, `phase-1.md` (closed summaries; carried-forward items are listed there).

**Updated:** 2026-10-22, end of unit p3i

## Where things stand

- **Phase:** 2 · Desktop UI (third of six). Still on the seam stage: moving upstream's UI into `shared/commonMain` behind Android seams. No desktop window shows the real app yet.
- **Last committed batch:** p3i (settings roots cleared; 104 files rehomed: `SettingsScreen`, `SettingsViewModel` and the whole settings tree), verified. See the p3i entry in `DEVLOG.md`.
- **Counts:** app 30 files, commonMain 1,688, androidMain 258. Navigation-graph closure: 13 files, **0 Android roots**.
- **Baseline for "Android unchanged":** plain upstream v2.24.0, `build/desktop-baseline/app-debug-upstream-v2.24.0.apk`. Plain save proof: `build/desktop-check/save-proof/v224-plain-1021b` (captured 2026-10-21 16:29; past 24 hours older than a candidate from 2026-10-22 16:29, so recapture before the next verification: `save-proof.sh build/desktop-baseline/app-debug-upstream-v2.24.0.apk <dir>`, about 8 min).
- Nothing uncommitted.

## Next action

1. The 13 closure files (`NavGraph` among them) stay in androidMain only because `NavGraph` reaches `ui/screens/quaypass/QuayPassCheckInScreen.kt`, held back in `scripts/desktop/restructure/keep-in-app.txt`: line 179's exhaustive `when (serviceState)` runs over `QuayPassService.QuayPassRunState`, a nested enum in an expect class, and K2 requires `else` there. Fix that by making the enum a plain commonMain enum, not an expect one. It is nested, and `extract_decls.py` moves only top-level declarations, so check whether a typealias or a top-level enum with a nested `typealias` keeps upstream's `QuayPassService.QuayPassRunState` name compiling, or teach `slices.py` to emit a nested enum as a plain (non-expect) enum the actuals reuse. Then drop the `keep-in-app.txt` line, run the debug `--rehome` (step 2) and see what follows.
   Debug "why doesn't X rehome": copy the restructure `*.py` and `*.txt` to a scratch dir, make the candidate loop in `movable.py --rehome` print the blocking deps, and run the copy from the repo root (p3i did this; the real `--rehome` prints nothing when every candidate is blocked).
2. Run the pipeline (`extract_decls.py --refresh`, `inline_constants.py`, `move_companion_functions.py`, `apply_moves.py`, `move_members.py`, `actualize.py`, `slices.py`, `rewrite_imports.py`, then `movable.py --check`; the list is in `reapply.sh`). Then `movable.py --rehome`, append its output to `moves.txt` as a new labelled `[commonMain]` section, and run `apply_moves.py` and `rewrite_imports.py` again.
3. `git add -A app/src/main/kotlin shared/src`, compile first (`quiet.sh <label> ./gradlew :shared:compileKotlinDesktop :shared:compileDebugKotlinAndroid`, about 4 min; `quiet.sh` exits 0 even on failure, so read its `BUILD` line, and the full log for the complete error list), then verify with `scripts/desktop/check/verify.sh <label> <plain proof>` in the background (about 20 min).
4. Commit, log in `DEVLOG.md`, rewrite this file, end the unit.

After `NavGraph` is portable: the desktop shell, SDL3 input and screen bring-up (Phase 2 plan, steps 6–8, in `PORT_PLAN.md`). Desktop stubs to give real implementations then or in Phase 3: `ClipboardTextReader` (returns null); the QR scanner (composes nothing, `deviceHasCamera` false); `PlatformMediaPlayer` (no audio backend) and `PlatformMediaMetadataRetriever` (no embedded art); `InlineTilePlayer`, `PageThemePlayer` (compose nothing) and `YouTubeVideoPlayer` (calls `onError`); `Box3dCover` (draws nothing; Skia `Canvas.concat` with a 4-point homography for `setPolyToPoly`); `OpenDocument` in `PlatformActivityResult.desktop.kt` (returns null; needs a file chooser); `getLaunchIntentForPackage` (null, so "restart app" does nothing); `SteamSettingsDelegate`, `ShaderStackSection` and `ShaderChainManager.shaderStack` (stubs; Steam and the in-app shader chain are Android-only for now).

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
- `slices.py` slices only public top-level functions (no `internal`). Opt-ins in `slices.txt`'s member list: an `internal` companion function by name, `constructor: ()` for an implicit no-arg constructor. A plain nested class without a body slices; one with a body does not.
- A `var` with a restricted setter cannot actualize an expect `val`. Give commonMain an expect extension property of the same name whose Android actual returns the member (`ShaderChainManagerState.kt`), and add a `MEMBER_EXTENSIONS` import rule in `rewrite_imports.py`.
- An expect default argument that reads an inherited member of an expect object crashes the Kotlin backend ("no references to expect classes at this point"). Read it through the declaring type; `slices.py` keeps the edited expect line.
- Adding a name to a `COMPANION_IMPORTS` list adds that import to every file carrying the list (~200 files). Expected; unused imports only warn.
- Never start a verification while files are still being edited (the race has happened twice).
- A moved `@HiltViewModel` is proven only by a device run; generated-code evidence only from a build that ran the generating task.
- Stand-ins must not add null checks upstream never had (unit tests run on `android.jar` stubs).
- `DownloadManagerPlatformFolderTest` can fail in the full gate with "Dispatchers.Main is used concurrently with setting it" (test-ordering race, untouched by the port). Rerun the class alone, then the full verification.
- A fully qualified rewrite rule must skip import lines (`(?<!import )`): `slices.py` runs imports through `rewrite_imports.RULES` to judge portability, so a rule that matches an import line makes that Android type look portable and widens slices.
- If the testbed containers are gone (it happened while paused), `scripts/desktop/testbed/up.sh` recreates them on the kept volumes; the proof's snapshot restore does the rest. `adb` is not on PATH in a plain shell; the scripts find it.
- The Battery Bay cached-copy count varies between runs of plain upstream (p3a: two plain proofs 25 minutes apart differed in 12 captures). If a proof differs only there with equal verdicts, recapture the plain proof (`save-proof.sh build/desktop-baseline/app-debug-upstream-v2.24.0.apk <dir>`, about 8 min) and compare again before calling it a regression.
- The plain proof ages: once it is more than 24 hours older than a candidate, upstream's daily checks run again in the candidate. The masks now cover the two epoch-millisecond keys; a new unmasked difference of that kind means another clock key, not a regression, until shown otherwise.
