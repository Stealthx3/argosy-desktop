# Phase 2 · Desktop UI — closed

**Closed:** 2026-10-22. `main` pushed. Exit criterion met for every screen the RomM testbed can reach; the rest is listed under "Carried forward".

## Outcome

- Upstream's whole UI runs in the desktop window: `shared/src/commonMain` 1,720 Kotlin files, `androidMain` 243, `app` 30. Desktop DI is Dagger (`shared/src/desktopMain/.../di/`), Android stays on Hilt.
- Input: SDL3 gamepads (JNA) and the keyboard feed upstream's own `InputDispatcher`; focus and navigation behave as on Android.
- Compared with the same pad walk on the Android check AVD and on the desktop, and matching (units p4d–p4r): first run and sign-in, Home and drawer, Game Detail and its sheets, Library (both views, filters), Collections, Collection Detail, Virtual Browser, Downloads, Library Sync, Save Sync, Check-In, Apps, all Settings pages, About, Changelog, the file browser.
- Desktop fixes on the way: Roboto via a Java shim over Compose 1.7.3 internals (recheck on any Compose upgrade), `MultiResumeLifecycle` (window STARTED counts as RESUMED, as Android multi-resume), real desktop music directory and storage volumes, `flowOf("")` where Android's flows emit on registration.
- Accepted differences: release palette (teal) vs the debug APK's orange; no soft keyboard on desktop.

## Carried forward

- Screens not yet opened on the desktop: Media Library/Detail (needs Jellyfin in the testbed: owner decision), the Social family (needs an Argosy Social session; maintainer domain), Save Timeline and Per-Game Settings' emulator rows (Phase 3 emulators). Search and Manage Pins have no entry point in v2.24.0 on either platform.
- Unverified: a physical pad (Nintendo layouts), stick navigation, key-repeat feel.
- Phase 3 and 4 items: listed in `phase-3.md`.
- 40 `TODO(...)` in desktopMain on feature paths: `grep -rn 'TODO(' shared/src/desktopMain`.

## Details

`DEVLOG.md`, section "Phase 2 · Desktop UI" (units p4a–p4r, "Phase 2 close"). Gotchas and walk recipes: `docs/desktop/gotchas.md`.
