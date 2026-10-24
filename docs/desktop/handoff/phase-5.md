# Phase 5 · Beta and Flathub — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`. Earlier phases: `phase-0.md` … `phase-4.md` (closed summaries).

**Updated:** 2026-10-24 (phase not started; waiting for the owner at the phase gate). Steps 0 done.

## Scope and exit criterion

`PORT_PLAN.md` Roadmap (Phase 5): side-by-side checks against Android; GNOME, KDE and SteamOS testing; Flathub submission. Exit: testers using both apps cannot name a behavioural difference outside the listed desktop divergences; `flatpak-builder-lint` clean; offline build green in CI for x86_64 and aarch64; Flathub PR merged.

## Carried forward

- Owner test results for `docs/desktop/owner-test-phase-4.md` (fix any failures first).
- Phase 3 items: download stall with a ROM already in the target folder; standalone-emulator save proof; "Scan for Android games" rows (owner questions).
- Desktop leftovers: Displays → "Enable Dual-screen Mode", About → upstream APK update (in-app updater row: Flatpak performs the update).
- Flathub lint: exceptions or alternatives for emulator folder grants and `flatpak-spawn` access, screenshots, the app id vs repository name; offline from-source build (Gradle dependencies as Flatpak sources).
- YouTube trailers on desktop (deferred; needs yt-dlp and a JavaScript runtime).

## First actions (once the owner starts the phase)

1. Read the owner's Phase 4 test results and fix failures.
2. Write the Phase 5 step plan in `DEVLOG.md` as a numbered `N. **Title.**` list under `**Phase 5 plan:**` (the progress page reads it).
3. Then follow the plan; at every step close republish the alpha (`push-pages.sh --step-close 5.<n>`).

## Where things are

- Desktop graph `shared/src/desktopMain/.../di/`; Flatpak `desktopApp/flatpak/`, scripts `scripts/desktop/flatpak/`; walks `scripts/desktop/check/`; JDK `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.
- Desktop data `~/.local/share/argosy-desktop` (never delete without the owner); Flatpak data `~/.var/app/io.github.stealthx3.ArgosyDesktop`.
