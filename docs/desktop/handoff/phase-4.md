# Phase 4 · Linux adaptation — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases: `phase-0.md` … `phase-3.md` (closed summaries).

**Updated:** 2026-10-23 (step 2 part 2: Button Mapping sub-section). No steps closed yet; step 1's code is in, its real-hardware check waits on the owner; step 2 is in progress. The Phase 4 step plan (steps 1–10) is in `DEVLOG.md` under "Phase 4 · Linux adaptation" → "Phase 4 step plan". Nothing uncommitted.

## Scope and exit criterion

`PORT_PLAN.md` Roadmap (Phase 4) and "Desktop divergences". Exit: every Port, Adapt and Linux row of the parity matrix works on the desktop; every Remove row has no entry point on the desktop and the Android app is unchanged (A/B screenshots and unit tests); display profiles and the wizard's Display setup step work on a handheld, a monitor and a TV-sized screen.

## Step 1 (input mode, Steam Input): code done, owner check pending

- Done (DEVLOG "Step 1, part 1"): `ControllerInputMode` setting (Navigation → Controller → Input Mode, desktop only via `ControllerInputSupport`), SDL hints per mode at OVERRIDE priority, Steam-virtual pads skipped outside Steam Input, pad logging on connect, SDL restart on change. Walk evidence `build/desktop-check/p4s1/`, Android unchanged `p4s1-android`.
- Owner check (ask once, with the next alpha build): which Steam Controller model; in Desktop Mode with Steam running and Argosy started normally (expect mode SDL3, the controller via SDL's HIDAPI driver, no rumble: SDL has none for it), and Argosy added to Steam and started from it (expect Steam Input, rumble through Steam). The `grep -i "DesktopGamepad" ~/argosy-run.log` lines show what Argosy saw. A new alpha needs `build-alpha.sh` → `publish-repo.sh` → `push-pages.sh` (recipe in `docs/desktop/gotchas.md`).
- Inside the Flatpak, `SteamVirtualGamepadInfo` points to a host `/tmp` file the sandbox cannot read, so Steam handles may read 0; detection also uses the 28de:11ff ids. Check in the owner test.

## Step 2 (navigation button mapping): parts 1–2 done, next part 3

- Done (DEVLOG "Step 2, part 1" and "part 2"): `NavigationButtonMaps` + desktop `NavigationButtonRemapper` (triggers now included); Navigation → Button Mapping screen (`SettingsSection.BUTTON_MAPPING`, `ButtonMappingSection.kt`): controller, presets (swaps) and keyboard groups, capture dialog, resets. Bridge `expect class NavigationButtonCapture` (Android no-op). Walk evidence `build/desktop-check/p4s2/`.
- **Next action, part 3:** footer glyphs for remapped buttons (`InputButton.toPainter()` in `FooterHint.kt` is the single icon-side resolution; add a desktop-provided local that maps a standard button to the active controller's physical one via `NavigationButtonMaps.physicalFor`); friendlier names in the mapping rows for D-pad and keyboard codes (now raw `HotkeyManager.getKeyName`: "DPAD_DOWN", "ENTER"; needs string resources, label vs token); then Android A/B (Settings, Navigation must match `p4s1-android`; the new row is hidden there) and close step 2.
- Walk route: from Home `PAD:START wait:2 PAD:DPAD_DOWN*8 PAD:SOUTH` (Settings), `PAD:DPAD_DOWN*2 PAD:SOUTH` (Navigation), `PAD:DPAD_DOWN PAD:SOUTH` (Button Mapping). Focus indices there: 0 controller name, 1–16 actions, 17 reset, 18–20 presets, 21–36 keyboard actions, 37 keyboard reset. A walk writes to the real desktop data; end it with both resets.

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
