# Phase 4 · Linux adaptation — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases: `phase-0.md` … `phase-3.md` (closed summaries).

**Updated:** 2026-10-23 (Phase 4 started; step plan written). No steps done yet. The Phase 4 step plan (steps 1–10) is in `DEVLOG.md` under "Phase 4 · Linux adaptation" → "Phase 4 step plan". Nothing uncommitted.

## Scope and exit criterion

`PORT_PLAN.md` Roadmap (Phase 4) and "Desktop divergences". Exit: every Port, Adapt and Linux row of the parity matrix works on the desktop; every Remove row has no entry point on the desktop and the Android app is unchanged (A/B screenshots and unit tests); display profiles and the wizard's Display setup step work on a handheld, a monitor and a TV-sized screen.

## Next action: step 1, controller input mode and Steam Input

- Code: `shared/src/desktopMain/.../platform/sdl/SdlGamepadSource.kt` (SDL init sets only `JOYSTICK_ALLOW_BACKGROUND_EVENTS`; `opened()` builds `SdlPadEvent.Added`), `Sdl3.kt` (JNA bindings), `GamepadRumble`.
- First: log every pad on connect (name, vendor, product, `SDL_GetGamepadType`, Steam-virtual flag) and every keyboard source; set `SDL_HINT_JOYSTICK_HIDAPI_STEAM` (and Steam Deck HIDAPI) so SDL drives Valve hardware when Steam does not hold it; check rumble on it.
- Then the "Input mode" setting (Auto / Steam Input / SDL3) through the full settings chain into Navigation → Controller; Steam Input listed only while a Steam virtual gamepad is connected, and the raw device behind it ignored. Native `ISteamInput` is a later revisit, not this step.
- Ask the owner: which Steam Controller model, and the `grep -i -E "gamepad|controller" ~/argosy-run.log` line from the Bazzite run. Their test is the verification for this step.

## Carried forward

- From Phase 3: see `phase-3.md` "Carried forward" (download stall is Phase 5; open owner questions on standalone save proof and rows 18–30).
- Phase 4 decisions still open inside steps: emulator database schema/location/update path and initial emulators (step 8); TV safe-area default 3 % and live thumbnails (step 5, confirm with the owner when reached).
- Rules that still apply: label vs token for every new string; settings chain end to end; desktop-only gates, never deletions from shared code; Android A/B baseline `build/desktop-baseline/app-debug-upstream-v2.24.0.apk`.

## Where things are

- Desktop graph `shared/src/desktopMain/.../di/`; new commonMain injections need desktop bindings (`scripts/desktop/restructure/di_closure.py`).
- Desktop data `~/.local/share/argosy-desktop` (never delete without the owner); Flatpak data `~/.var/app/io.github.stealthx3.ArgosyDesktop`.
- Testbed now has a GameCube platform (`Big Disc Test`, 1.4 GB synthetic); save-proof expectations count only the battery ROMs.

## Commands and tools

- Runs and walks: `scripts/desktop/check/{desktop-shot,desktop-drive,pad-walk}.sh`; Flatpak build/publish: `scripts/desktop/flatpak/`.
- Verification: `verify.sh`, `ab-check.sh`, `save-proof.sh`; quiet runner `scripts/desktop/quiet.sh`; JDK `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.
- Screenshots: window captures only, never full screen. `pkill -x <name>` inside walks.
- Gotchas: `docs/desktop/gotchas.md`.
