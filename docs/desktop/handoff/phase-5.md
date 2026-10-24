# Phase 5 · Beta and Flathub — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`. Earlier phases: `phase-0.md` … `phase-4.md` (closed summaries).

**Updated:** 2026-10-24 (step 2 in progress, code uncommitted). Steps 1–1 done; step 2 code written and the gate passes (131 + 4,426), walk not yet confirming the row.

## Scope and exit criterion

`PORT_PLAN.md` Roadmap (Phase 5): side-by-side checks against Android; GNOME, KDE and SteamOS testing; Flathub submission. Exit: testers using both apps cannot name a behavioural difference outside the listed desktop divergences; `flatpak-builder-lint` clean; offline build green in CI for x86_64 and aarch64; Flathub PR merged.

## Carried forward

- Owner test results for `docs/desktop/owner-test-phase-4.md` (fix any failures first).
- Phase 3 items: download stall with a ROM already in the target folder; standalone-emulator save proof; "Scan for Android games" rows (owner questions).
- Desktop leftovers: Displays → "Enable Dual-screen Mode", About → upstream APK update (in-app updater row: Flatpak performs the update).
- Flathub lint: exceptions or alternatives for emulator folder grants and `flatpak-spawn` access, screenshots, the app id vs repository name; offline from-source build (Gradle dependencies as Flatpak sources).
- YouTube trailers on desktop (deferred; needs yt-dlp and a JavaScript runtime).

## Step 2 state (uncommitted)

- Written: `platform/AppShutdown.kt` (shared request flag), `QuickSettingsItem.CloseApp` (QUICK page, after Swap Start/Select, `visibleWhen` desktopWindow), controller `closeConfirm` flow + `requestClose/cancelClose/confirmClose` and modal-aware input handler, `QuickNoticeRow` + `ArgosyConfirmModal` in `QuickSettingsPanel`, 4 strings × 8 locales (`ui_quick_settings_close_app_*` in strings_common.xml), `Main.kt` observes `AppShutdown.requested` → `exitApplication()`, then after `application {}` releases input and `exitProcess(0)`. Gate green.
- Walk `build/desktop-check/p5s2/`: Quick Settings (R3) opens; after `DOWN*11` focus sat on Haptics and A opened nothing: check how focus moves on the QUICK page (sliders may take Down? hidden rows?), confirm the Close Argosy row is reachable (scroll), then A → modal, B cancels, A,A exits (window gone, process ended).
- **Found:** an uncaught `UnsatisfiedLinkError: no sigil-jni` on the AWT thread from `TitleIdDownloadObserver` → `TitleIdExtractor` → `Sigil.<clinit>` (desktop has no libsigil-jni); track as a bug (catch/disable title-id extraction on desktop or ship the native lib) — may matter for Game Mode stability (step 4).
- Walk gotcha: `desktop-drive.sh` ends with `pkill -f com.nendo.argosy.desktop.MainKt`; never put that string in the same Bash command line.

## Pre-beta fixes (owner, 2026-10-24): step 1 done, step 2 in progress

1. Wizard: the desktop Permissions step (`EmulatorFolderAccessStep` in `FirstRunScreen.kt`, title/intro reuse `firstrun_permissions_*` keys) gets its own title "Emulators Argosy should manage" and a matching intro (new keys, eight locales); Android keeps its Permissions step.
2. Quick Settings: desktop-only "Close Argosy" item (`QuickSettingsItem`, gate on `PlatformFeatures.desktopWindow`) → confirm modal (`ArgosyConfirmModal` pattern) → graceful exit: a shell hook observed by `Main.kt` that stops the gamepad dispatcher, background jobs and players, then `exitApplication()`.
3. Thumbnails: check the desktop `ImageCacheManager` actual (stub?) and how Android caches covers during sync; implement the desktop cache so Home and platform selection show covers. Testbed games have no covers (synthetic): check against a game with IGDB art or the owner's library.
4. Steam Game Mode: no gamescope session here; check whether `gamescope` can run nested for a test; review launch under gamescope (`flatpak-spawn --host` from inside a Steam-launched Flatpak, window focus) and shutdown (SIGTERM handling, non-daemon threads, SDL quit); ask the owner for `~/argosy-run.log` from a Game Mode session.

## First actions

1. Read the owner's Phase 4 test results and fix failures.
2. Write the Phase 5 step plan in `DEVLOG.md` as a numbered `N. **Title.**` list under `**Phase 5 plan:**` (the progress page reads it).
3. Then follow the plan; at every step close republish the alpha (`push-pages.sh --step-close 5.<n>`).

## Where things are

- Desktop graph `shared/src/desktopMain/.../di/`; Flatpak `desktopApp/flatpak/`, scripts `scripts/desktop/flatpak/`; walks `scripts/desktop/check/`; JDK `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.
- Desktop data `~/.local/share/argosy-desktop` (never delete without the owner); Flatpak data `~/.var/app/io.github.stealthx3.ArgosyDesktop`.
