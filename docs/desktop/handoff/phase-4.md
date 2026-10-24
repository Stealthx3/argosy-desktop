# Phase 4 · Linux adaptation — closed

**Closed:** 2026-10-24 (exit verification, step 10). `main` pushed; `gh-pages` carries the signed Flatpak repo (republished at every step close since the owner's 2026-10-24 rule).

## Outcome

- Controllers: input mode Auto / Steam Input / SDL3 (`ControllerInputSupport`, SDL hints, Steam-virtual pads), per-controller navigation button mapping (`NavigationButtonMaps`, Button Mapping screen, footer glyphs follow the remap).
- Removals on desktop through `PlatformFeatures`: Steam (JavaSteam kept inert by owner decision), Jellyfin/Media, Social and QuayPass/Check-In, Android Settings row, built-in emulation (rows, core downloads, first-run core steps).
- Window: decorated, 1280×720 centred, F11, gamescope forces full screen, `--fullscreen`/`--windowed`, Interface → Start in Full Screen, dark when no theme is stored.
- Display profiles Handheld / Desktop / TV (detection by DMI, EDID size, gamescope; TV = option B "Couch scale": UI Scale 125 %, full footer, strong glow, 3 % safe area; cursor auto-hide), schematic profile cards in Interface and the first-run Display setup step.
- Linux screens: Flatpak permissions from `/.flatpak-info` with consented grants; read-only host GPU page (`vulkaninfo`, `glxinfo`, sysfs).
- Emulator compatibility database (`linux-emulators.json`, bundled + cached refresh from the repo): detection and launch arguments, Flathub suggestions in the emulator picker, managed installers (AppImage releases into `~/Applications`, Flathub update rows).
- Video: libmpv (+ libass, libplacebo, ffmpeg-full extension) in the Flatpak, software-rendered local video and theme audio; YouTube trailers deferred (owner decision).
- Exit verification: parity matrix rows for Phase 4 all closed; full desktop Settings walk `build/desktop-check/p4x-desktop/` (16 rows render); Android unchanged since step 1 apart from live data (`p4s1-android` vs `p4s8c-android`); gate 131 desktop + 4,426 Android tests.

## Carried forward

- Owner test list: `docs/desktop/owner-test-phase-4.md` (hardware-only checks gathered through the phase).
- Upstream entry points still on desktop: Displays → "Enable Dual-screen Mode" (dual-screen is not in this version), About → upstream "Update Available — Download" (in-app updater is a Phase 5 row).
- Not verified here: real Steam Controller and Nintendo-layout pads, a real handheld/TV, real installs and grants, a video tile in the running UI, theme audio output, `seekTicks`, the remote emulator-database refresh (needs `main` pushed, now done).
- From Phase 3 (still open): download stall with a ROM already in the folder; standalone-emulator save proof and "Scan for Android games" rows (owner questions).
- Flathub lint items (Phase 5): emulator folder grants, `flatpak-spawn` access, screenshots, app-id URL.

## Details

`DEVLOG.md`, section "Phase 4 · Linux adaptation". Gotchas and walk recipes: `docs/desktop/gotchas.md`.
