# Phase 2 · Desktop UI — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone.
History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases:
`phase-0.md`, `phase-1.md` (closed summaries; carried-forward items are listed there).

**Updated:** 2026-10-22, end of unit p4d

## Where things stand

- **Phase:** 2 · Desktop UI (third of six). Seam stage done; desktop shell (p4a); keyboard input (p4b); **SDL3 gamepad input done (p4c):** `platform/sdl/` (JNA binding `Sdl3`, `SdlGamepadSource` thread, `SdlVirtualPad`) and `ui/input/DesktopGamepadDispatcher` + `SdlGamepadMapping` feed upstream's `GamepadInputHandler` the same key codes and motion events Android does; pads register in `PlatformInputDevices`, and `ConnectedControllerTracker`/`TriggerAxisKeyEmitter` desktop actuals are now real. Plan step 7 is complete. See the p4c entry in `DEVLOG.md`.
- **Counts:** app 30 files, commonMain 1,720, androidMain 243 (unchanged; desktop-only work).
- **Baseline for "Android unchanged":** plain upstream v2.24.0, `build/desktop-baseline/app-debug-upstream-v2.24.0.apk`. Plain save proof: `build/desktop-check/save-proof/v224-plain-1022a` (captured 2026-10-22 04:19; recapture once a candidate is more than 24 hours newer: `save-proof.sh build/desktop-baseline/app-debug-upstream-v2.24.0.apk <dir>`, about 8 min).
- Nothing uncommitted.

## Next action

Plan step 8: walk first run → Home with `desktop-drive.sh`, fixing desktop stubs each screen hits, and compare with Android at 1920×1080. **p4d:** the login step works (input reaches `FirstRunInputHandler`; pad South/East, D-pad Down, Enter, Escape all behave). The earlier "unchanged frame" was the occluded test window throttled to ~1 fps under vsync, which blocked the AWT thread in Skiko `swapBuffers`. The scripts now run with vsync off. See the p4d entry in `DEVLOG.md`.
Next, in order: (1) text entry. Make `KeyPipe` send `KEY_TYPED` (e.g. a `TYPE:<text>` line: per char, press/typed/release on the focus owner), then type the testbed URL (`http://localhost:8093`, see `docs/desktop/testbed-runbook.md`) and continue through login. (2) Compare the login frame with Android at 1920×1080: the desktop layout looks small for that size, so check density/font scale first.
Still unverified from p4c: a physical pad (none attached; Nintendo-layout pads especially), in-app stick navigation, key repeat feel against Android.
Phase 3 question (record, do not fix now): while the window is hidden, vsync blocks the AWT thread about 1 s per frame, and SDL pad events are posted to that thread.

Where the desktop graph lives: `shared/src/desktopMain/.../di/` (`DesktopComponent`, `DesktopAppModule` hand-owned bindings, `DesktopDataModule`, `DesktopViewModelComponent`, `DesktopApplication`, `ViewModels.desktop.kt`); generated `DesktopDaoModule` and `DesktopViewModelModule` (Gradle tasks in `shared/build.gradle.kts`). A new Android-side `@Binds`/`@Provides` that commonMain code injects needs a desktop counterpart in `DesktopAppModule`; `di_closure.py <Root>...` lists the gaps, and the Dagger compile of `:shared` for desktop is the final check.

**Desktop stubs.** 44 `TODO(...)` remain in desktopMain, all functions with typed returns on feature paths (Steam, netplay, save states, frames, music dirs, documents, QuayPass keys, ...): `grep -rn 'TODO(' shared/src/desktopMain`. Each throws when reached; replace with Android's do-nothing answer or a real desktop implementation as screens reach them. Others that compose nothing or report "unsupported": `ClipboardTextReader`, QR scanner, `PlatformMediaPlayer`/`PlatformMediaMetadataRetriever`, `InlineTilePlayer`, `PageThemePlayer`, `YouTubeVideoPlayer`, `Box3dCover` (Skia `Canvas.concat` homography for `setPolyToPoly`), `OpenDocument` (needs a file chooser), `getLaunchIntentForPackage`, Steam and shader-chain stubs, `AmbientLedManager`, `startPlayer`, `DesktopLEDController`, `DesktopFileAccessLayer` (plain `java.io`; fine for Linux).

## Commands and tools

- Desktop run + screenshot: `scripts/desktop/check/desktop-shot.sh <out.png> [settle-s] [WxH]` (windowed, X window via `import`; prints exceptions from the app log). Driven run: `scripts/desktop/check/desktop-drive.sh <out-dir> <settle-s> shot:<name> wait:<s> ENTER DOWN*3 ESCAPE ... [-- WxH]` (AWT VK names through the app's `--key-pipe`). DI gaps: `scripts/desktop/restructure/di_closure.py <Root>... [--tree]`.
- Restructure tools: `scripts/desktop/restructure/` (manifests `*.txt`, scripts `*.py`; `movable.py --path/--roots/--plan/--rank` for planning).
- Verification: `scripts/desktop/check/verify.sh`, `ab-check.sh`, `save-proof.sh`; testbed: `scripts/desktop/testbed/` and `docs/desktop/testbed-runbook.md`.
- Quiet runner for anything noisy: `scripts/desktop/quiet.sh <label> <command...>`.
- JDK: `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.

## Gotchas worth remembering

- Timing in driven runs: `desktop-drive.sh` prints ms timestamps per key and shot; an EDT watchdog (a Swing timer heartbeat plus a thread that prints the AWT thread's stack when the heartbeat is >150 ms late) found the vsync stall where `jstack` was useless (about 5.7 s per sample here).
- XTest/xdotool-style injection does not work here: KDE's Xwayland runs `-enable-ei-portal` (consent portal). Use `desktop-drive.sh` (in-process key pipe). No Xvfb installed; installing one needs the owner.
- SDL: C `bool` returns are bound as `Byte` (JNA's `Boolean` reads 4 bytes). SDL sends an initial zero axis sample when a pad opens. A desktopApp class must not name `Sdl3Library` (JNA is `implementation` in `:shared`; the compile fails with "Cannot access com.sun.jna.Library"). This machine already has SDL joystick instance 1 (a non-gamepad device), so the first virtual pad is id 2 → device id 102.

- Desktop: `kotlinx-coroutines-swing` must be on the runtime classpath (`Dispatchers.Main`). A `TODO` in a desktop property initializer throws at injection; one in a function throws when a coroutine reaches it, often off-screen (startup hangs on a spinner). Read the app log, not only the screenshot.
- Gradle does not track `filter {}` lambdas: change `prepareDesktopComposeResources`'s `stringFilter` input property along with its filter.
- `@HiltViewModel` scan: the Android task's pattern skips a class with an annotation argument between `@HiltViewModel` and `class` (e.g. `FirstRunViewModel`), yet Android still resolves it. Understand why before changing the Android task.
- `slices.py` only carries members commonMain uses when it runs: after a rehome, run `slices.py` (then `rewrite_imports.py` twice) again, or the compile reports the newly used members. Its "left out" lines give the reason per member (`no declared type`: add the return type in `slices.txt`; a nested type: hoist it).
- A Hilt `@EntryPoint` that commonMain needs: plain commonMain interface, Hilt interface in a `.android.kt` file extending it with the members redeclared (`LaunchProgressEntryPoint.android.kt`).
- A nested enum or sealed class that commonMain code switches over or names cannot ride in a slice (expect enum: K2 wants `else`; sealed subclasses are not carried). List it in `nested-hoists.txt`; `hoist_nested.py` makes it a plain top-level commonMain declaration.
- A member an expect class cannot carry (a `var` with `private set`, an interface member with a default body) goes through an expect extension of the same name whose Android actual calls the member (`AmbientLedManagerState.kt`, `SteamLauncherInstalled.kt`, `ShaderChainManagerState.kt`), plus a `MEMBER_EXTENSIONS` rule in `rewrite_imports.py`.
- Hand-listed expect classes (`actuals.txt`, hand-owned commonMain file) need each new member added in three places: `actuals.txt`, the commonMain expect, the desktop actual.
- After a `slices.py` change, run `slices.py` then `rewrite_imports.py` twice and check the second run adds nothing (`added to` lines); a spill-over into other slices shows in `git status`.

- To see which line trips a guard, import `movable` and call the guard's `search` per line (the Context guard is `movable.ContextGuard()`); the `--roots` report truncates its reasons.

- Block rewrites (`TEXT_BOUNDS_BLOCK`, `DISPLAY_NAME_QUERY`) run before unused imports are dropped, and `movable.py` imports `rewrite_imports` to strip the same blocks in its guards (`StrippedGuard`). When `SettingsViewModel` moves, check `SettingsFontRouter` compiles through `DISPLAY_NAME_QUERY` and the `openInputStream` import.
- `movable.py --check` and `--rehome` do not prove that an expect slice (or a hand-listed expect class) carries every member a rehomed file calls. Only the compile catches that. An expression-bodied member needs its return type in `slices.txt`, or the slice leaves it out.
- A stand-in extension property (like `className`) also needs an entry in `rewrite_imports.py`'s stand-in lists, or the moved files won't import it.
- Check a new stand-in's name against `shared/src/commonMain/kotlin/com/nendo/argosy/platform/` first (`PlatformPackageManager` was taken; only the compile caught it).
- Desktop actuals are generated once and hand-owned from then on; `slices.py` appends missing members to them.
- `verify.sh`'s A/B step can fail silently: its output is filtered by `grep`, so no `home:`…`datastore:` lines means it did not finish. Check `<label>-en/` for `candidate-*.png` and rerun `ab-check.sh` alone (the command is in `verify.sh`, about 6 min).
- `slices.py` slices only public top-level functions (no `internal`). Opt-ins in `slices.txt`'s member list: an `internal` companion function by name, `constructor: ()` for an implicit no-arg constructor. A plain nested class without a body slices; one with a body does not.
- A `var` with a restricted setter cannot actualize an expect `val`. Give commonMain an expect extension property of the same name whose Android actual returns the member (`ShaderChainManagerState.kt`), and add a `MEMBER_EXTENSIONS` import rule in `rewrite_imports.py`.
- An expect default argument that reads an inherited member of an expect object crashes the Kotlin backend ("no references to expect classes at this point"). Read it through the declaring type; `slices.py` keeps the edited expect line.
- Adding a name to a `COMPANION_IMPORTS` list adds that import to every file carrying the list (~200 files). Expected; unused imports only warn.
- Never start a verification while files are still being edited (the race has happened twice).
- A comment on the `[commonMain]` line of `moves.txt` becomes part of the destination path (p3j moved a file into a folder named after it). Comments go on their own line.
- A moved `@HiltViewModel` is proven only by a device run; generated-code evidence only from a build that ran the generating task.
- Stand-ins must not add null checks upstream never had (unit tests run on `android.jar` stubs).
- `DownloadManagerPlatformFolderTest` can fail in the full gate with "Dispatchers.Main is used concurrently with setting it" (test-ordering race, untouched by the port). Rerun the class alone, then the full verification.
- A fully qualified rewrite rule must skip import lines (`(?<!import )`): `slices.py` runs imports through `rewrite_imports.RULES` to judge portability, so a rule that matches an import line makes that Android type look portable and widens slices.
- If the testbed containers are gone (it happened while paused), `scripts/desktop/testbed/up.sh` recreates them on the kept volumes; the proof's snapshot restore does the rest. `adb` is not on PATH in a plain shell; the scripts find it.
- The Battery Bay cached-copy count varies between runs of plain upstream (p3a: two plain proofs 25 minutes apart differed in 12 captures). If a proof differs only there with equal verdicts, recapture the plain proof (`save-proof.sh build/desktop-baseline/app-debug-upstream-v2.24.0.apk <dir>`, about 8 min) and compare again before calling it a regression.
- The plain proof ages: once it is more than 24 hours older than a candidate, upstream's daily checks run again in the candidate. The masks now cover the two epoch-millisecond keys; a new unmasked difference of that kind means another clock key, not a regression, until shown otherwise.
