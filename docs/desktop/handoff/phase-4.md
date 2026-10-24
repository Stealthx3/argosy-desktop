# Phase 4 · Linux adaptation — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases: `phase-0.md` … `phase-3.md` (closed summaries).

**Updated:** 2026-10-23 (step 3 closed). Steps 1–3 done (step 1's owner hardware check still pending, below); next is step 4. The Phase 4 step plan (steps 1–10) is in `DEVLOG.md` under "Phase 4 · Linux adaptation" → "Phase 4 step plan". Nothing uncommitted.

## Scope and exit criterion

`PORT_PLAN.md` Roadmap (Phase 4) and "Desktop divergences". Exit: every Port, Adapt and Linux row of the parity matrix works on the desktop; every Remove row has no entry point on the desktop and the Android app is unchanged (A/B screenshots and unit tests); display profiles and the wizard's Display setup step work on a handheld, a monitor and a TV-sized screen.

## Step 1 (input mode, Steam Input): code done, owner check pending

- Done (DEVLOG "Step 1, part 1"): `ControllerInputMode` setting (Navigation → Controller → Input Mode, desktop only via `ControllerInputSupport`), SDL hints per mode at OVERRIDE priority, Steam-virtual pads skipped outside Steam Input, pad logging on connect, SDL restart on change. Walk evidence `build/desktop-check/p4s1/`, Android unchanged `p4s1-android`.
- Owner check (ask once, with the Phase 4 close alpha; the live repo changes only at a phase close): which Steam Controller model; in Desktop Mode with Steam running and Argosy started normally (expect mode SDL3, the controller via SDL's HIDAPI driver, no rumble: SDL has none for it), and Argosy added to Steam and started from it (expect Steam Input, rumble through Steam). The `grep -i "DesktopGamepad" ~/argosy-run.log` lines show what Argosy saw. A new alpha needs `build-alpha.sh` → `publish-repo.sh` → `push-pages.sh --phase-close 4` (recipe in `docs/desktop/gotchas.md`).
- Inside the Flatpak, `SteamVirtualGamepadInfo` points to a host `/tmp` file the sandbox cannot read, so Steam handles may read 0; detection also uses the 28de:11ff ids. Check in the owner test.

## Step 2 (navigation button mapping): closed

- Parts 1–3 in DEVLOG ("Step 2, part 1/2/3"). Footer glyphs follow the active controller's remap through `LocalButtonGlyphRemap` (identity on Android); mapping rows use friendly key names. Evidence `build/desktop-check/p4s2/`, `p4s2p3/`, Android `p4s2-android` (identical to `p4s1-android` apart from live data).
- Not verified: a real controller with a remap, a Nintendo-layout pad with a remap (ask with the step 1 owner check).
- Walk route to Button Mapping: from Home `START wait:2 DOWN*8 A`, `DOWN*2 A`, `DOWN A`; focus 1–16 actions, 17 reset, 21–36 keyboard, 37 keyboard reset. End walks with the resets.

## Step 3 (desktop removals): closed

- DEVLOG "Step 3, part 1" (entry-point list, `PlatformFeatures`), "part 2" (QuayPass with Social; JavaSteam inert by owner decision; no remaining traces; wizard verified). Evidence `p4s3/`, `p4s3b/`, `fresh-p4s3w/`, Android `p4s3-android`, `p4s3b-android`.

## Step 4 (window and full screen): next action

- Scope (plan step 4): decorated window with the system title bar; first launch windowed and dark; Interface → "Start in full screen" through the full settings chain (DataStore key → prefs repo → UserPreferences → SettingsModels → SettingsInitRouter → delegate/confirm router → section → consumption in `desktopApp` `Main.kt`); F11 toggle; gamescope forces full screen; `--windowed` / `--fullscreen` override the setting.
- First actions: read `desktopApp/src/.../Main.kt` window creation and its current flags (`--windowed`, `--size` already exist for walks); find how Interface rows are declared (follow `NavigationItem.ButtonMapping`'s desktop-only visibility); detect gamescope (`GAMESCOPE_WAYLAND_DISPLAY` / `XDG_CURRENT_DESKTOP=gamescope`); default theme dark on first launch only (check upstream's theme default before changing anything shared).

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
