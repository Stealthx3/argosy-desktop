# Phase 5 · Beta and Flathub — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md` (Phase 5 section). Earlier phases: `phase-0.md` … `phase-4.md`.

**Updated:** 2026-11-08 (development paused by the owner for a Claude Cowork repo reorganization and documentation rework; brief in `cowork-brief.md`). Steps 1–3 done; step 4 in progress.

## Paused state (read first)

- **Development is paused.** The owner asked to hand the repo to Claude Cowork for a reorganization and documentation rework. Do not resume step 4 until the owner says so. Pacer state: `waiting-owner`.
- **Git:** local `main` merged `origin/main` (6ddef138e, docs only; local versions of the shared docs kept) and was pushed on 2026-11-09 at the owner's request, mid-phase. `docs/postmortem/` and `scripts/postmortem.mjs` are now tracked.
- **Other sessions' uncommitted files:** `scripts/desktop/vmlab/lab.sh` (modified), `scripts/desktop/vmlab/gm-define.sh`, `scripts/desktop/vmlab/qemu-venus.sh` (SteamOS/Bazzite lab, closed by owner decision: Game Mode testing moved to the host).
- **Current beta (owner decision, 2026-11-09):** the build Pages serves is the current beta release (0.4.0); labels say beta, not alpha. Republished 2026-11-09 with the beta metainfo: app commit 72eee22e, `gh-pages` b548dc6 (code as 663022ed). Earlier publish of 2026-11-08: app commit 490fc8d3. `main` itself is not pushed.

## Scope and exit criterion

Owner decision 2026-10-27: Linux adaptation and compatibility, not a 1:1 clone. `PORT_PLAN.md` Roadmap (Phase 5): Linux desktop and Steam Game Mode compatibility (GNOME, KDE, SteamOS); Flathub submission. Exit: install, wizard, sync, downloads, launches, save sync and a clean exit on GNOME, KDE and SteamOS (desktop and Game Mode); `flatpak-builder-lint` clean; offline CI build for x86_64 and aarch64; Flathub PR merged.

Plan (`DEVLOG.md`, Phase 5 plan): 1 wizard wording, 2 Close Argosy, 3 thumbnails (done); 4 Linux desktop and Steam Game Mode compatibility (in progress); 5 Flathub submission.

## Step 4: done since the first beta build (all in the current beta, 663022ed)

- TV layout tweaks; grey edge strips under gamescope fixed; TV navigation and focus parts now on every desktop profile; Game Mode picks the TV profile over a saved Desktop one.
- Setup folder picker: Home plus mounted drives, Back in the hint row. Controller Layout choices (Auto, Xbox, PlayStation, Nintendo, Steam) and per-controller glyphs.
- Linux emulator database v2: 21 desktop-only emulators (VBA-M removed), VM-tested; Dolphin snap command detected.
- Emulators manager (desktop Settings > Managed Installers): every compatible emulator by platform, per-emulator submenu (status, package, EmuDeck, logs, data folder, Assign BIOS, install/uninstall).
- GitHub AppImage installs fixed (`java.net.http` was missing from the jlink runtime; broken in every build including Beta 1).
- Game Mode emulator launches: environment forwarding to `flatpak-spawn --host` plus `GameModeWindowTagger` (`STEAM_GAME` on emulator windows). Proven in nested Game Mode on the host with the Dolphin Flatpak: emulator in front, back to Argosy on close.
- Setup wizard: Start presses Next, a clickable hint bar on every step, Display step rows (Full Screen, Safe Area with TV, UI Scale) and layout selection that no longer advances; emulator list scroll indicator. The LB/RB top-row switcher is gone on desktop; a clickable "Menu" hint at the left of the hint bar opens the left menu (DEVLOG, "Start is Next").
- Desktop UI sounds (were a no-op): Ogg/WAV through `libvorbisfile` + `libpulse-simple`. Game Mode audio proven with Dolphin (video and audio) once Steam Game Recording is off; with it on, the nested setup stalls all PipeWire audio (DEVLOG, "desktop UI sounds").

## Step 4: open

- Published 2026-11-08 (Pages app commit 3d10438a): wizard and Menu-hint changes. Republished the same day (Pages app commit 663022ed): Steam controller variables forwarded to emulators (owner's Deck report: no RetroArch input in Game Mode) and the quiet return from a game (`GameInputGate`, window raise); both need the Deck to confirm. Dev host now has the RetroArch Flatpak plus Gambatte core (owner OK).
- Owner's Deck checks in real Game Mode (launch GameCube/Switch/PS and GBA games, exit from the Steam overlay, Steam keyboard), now on a build with the tagger.
- Not verified: `HostGameProcess.stopAll` ending a Flatpak emulator on Argosy exit; EmuDeck detection on the owner's host; Assign BIOS with real RomM firmware; the folder picker's Back row on screen.
- Snap emulators: tagged and focused but rendered black and cannot be signalled from outside snap confinement.
- Check on the Deck whether Steam Game Recording stalls audio in real Game Mode (it does in nested gamescope on the owner's host; owner set recording Off in that Steam during the test).
- Test Steam shortcut "Argosy Desktop (test)" stays in the owner's Steam library (backup of the original `shortcuts.vdf`: `build/gamemode/shortcuts.vdf.bak-20261108`; edit only with Steam closed).
- Step 4 closes with `--step-close 5.4` (republish) after the Deck checks.

## Open owner questions

1. Quick Settings Brightness on desktop: leave out, or add backlight control?
2. Fresh Flathub RetroArch has no cores: install cores for the user, or point to RetroArch's downloader?
3. App-id rename `io.github.Stealthx3.argosy_desktop` and the Flathub screenshots.

## Step 5 (Flathub) groundwork

`docs/desktop/flathub-plan.md`. The from-source manifest builds offline locally; lint shows only owner-gated items; CI draft `.github/workflows/flatpak.yml` untested until the phase-close push. Remaining work needs the owner (app id, screenshots, CI run, Flathub PR).

## Gotchas

- Never run two bundle builds at once: they share `desktopApp/build` and the second packages a stale build.
- Nested Game Mode on the host: `gamescope -W 1920 -H 1080 -w 1920 -h 1080 -e -- steam -gamepadui` (close normal Steam first); launch the shortcut with `steam steam://rungameid/<id>`; screenshots only with `GAMESCOPE_WAYLAND_DISPLAY=gamescope-0 gamescopectl screenshot <file>`, never full-desktop captures.
- Walks: Close Argosy from Home is `DPAD_LEFT DOWN*8 A RIGHT A`. Dev data downloads go to the real `~/Games/Argosy`.

## Where things are

- Desktop code `shared/src/desktopMain`, app `desktopApp/`, Flatpak `desktopApp/flatpak/`, scripts `scripts/desktop/` (VM lab `vmlab/`, walks `check/`); JDK `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.
- Desktop data `~/.local/share/argosy-desktop` (never delete without the owner); Flatpak data `~/.var/app/io.github.stealthx3.ArgosyDesktop`.
- Beta republish at a step close: `build-alpha.sh` → `publish-repo.sh` → `push-pages.sh --step-close 5.<n>`.
