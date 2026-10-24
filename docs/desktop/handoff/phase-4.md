# Phase 4 · Linux adaptation — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases: `phase-0.md` … `phase-3.md` (closed summaries).

**Updated:** 2026-10-23 (step 2 part 1: button mapping layer). No steps closed yet; step 1's code is in, its real-hardware check waits on the owner; step 2 is in progress. The Phase 4 step plan (steps 1–10) is in `DEVLOG.md` under "Phase 4 · Linux adaptation" → "Phase 4 step plan". Nothing uncommitted.

## Scope and exit criterion

`PORT_PLAN.md` Roadmap (Phase 4) and "Desktop divergences". Exit: every Port, Adapt and Linux row of the parity matrix works on the desktop; every Remove row has no entry point on the desktop and the Android app is unchanged (A/B screenshots and unit tests); display profiles and the wizard's Display setup step work on a handheld, a monitor and a TV-sized screen.

## Step 1 (input mode, Steam Input): code done, owner check pending

- Done (DEVLOG "Step 1, part 1"): `ControllerInputMode` setting (Navigation → Controller → Input Mode, desktop only via `ControllerInputSupport`), SDL hints per mode at OVERRIDE priority, Steam-virtual pads skipped outside Steam Input, pad logging on connect, SDL restart on change. Walk evidence `build/desktop-check/p4s1/`, Android unchanged `p4s1-android`.
- Owner check (ask once, with the next alpha build): which Steam Controller model; in Desktop Mode with Steam running and Argosy started normally (expect mode SDL3, the controller via SDL's HIDAPI driver, no rumble: SDL has none for it), and Argosy added to Steam and started from it (expect Steam Input, rumble through Steam). The `grep -i "DesktopGamepad" ~/argosy-run.log` lines show what Argosy saw. A new alpha needs `build-alpha.sh` → `publish-repo.sh` → `push-pages.sh` (recipe in `docs/desktop/gotchas.md`).
- Inside the Flatpak, `SteamVirtualGamepadInfo` points to a host `/tmp` file the sandbox cannot read, so Steam handles may read 0; detection also uses the 28de:11ff ids. Check in the owner test.

## Step 2 (navigation button mapping): part 1 done, next part 2

- Done (DEVLOG "Step 2, part 1"): `NavigationButtonMaps` (per-GUID key-code permutation, key `navigation_button_maps`, in settings backup), desktop `NavigationButtonRemapper` applied in `DesktopGamepadDispatcher` and `DesktopKeyDispatcher`, with `captureNext`/`cancelCapture` for "press a button" rows. The A/B, X/Y, Start/Select swaps stay as they are and apply on top of the map.
- **Next action, part 2:** read the menu-patterns skill, then build Navigation → "Button mapping" (desktop only, gate like `ControllerInputSupport.modeConfigurable`): a row per action (Confirm, Back, Context, Secondary, Menu, Select, L1/R1, L2/R2, L3/R3, D-pad Up/Down/Left/Right) showing the effective physical button for the active controller (`physicalFor(controller, s)` where `s` is the standard code that `mapKeycodeToGamepadEvent(s, swaps)` turns into the action), A arms `captureNext` and writes `assign`, a "Reset to default" row, the three swap toggles shown as presets, and a keyboard list (keyboard Confirm/Back targets are ENTER/ESCAPE, other actions the button codes). Strings with all seven translations; full settings chain through `ControlsState`.
- Part 3: footer glyphs for remapped buttons (`InputButton.toPainter()` in `FooterHint.kt` is the single icon-side resolution; add a desktop-provided local), desktop walk, Android A/B.
- Active-controller key: `NavigationButtonRemapper.controllerKey(descriptor)` (GUID half of `guid:location`); `ConnectedControllerTracker` knows the active device.

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
