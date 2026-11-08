# Phase 5 · Beta and Flathub — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`. Earlier phases: `phase-0.md` … `phase-4.md` (closed summaries).

**Updated:** 2026-11-08 (Emulators manager checked in the VM; Game Mode Dolphin test waits on the owner). Steps 1–3 done; step 4 in progress.

## Before the next push to origin (owner request, 2026-11-08)

At the owner's request, the documents were pushed ahead of the phase close from a cloud session: `origin/main` now has commit `6ddef138e` ("Docs: new README, Phase 5 records and the interim post-mortem") on top of `1a1e1c4dc`. It holds this workspace's copies of `README.md` (rewritten for GitHub), `PORT_PLAN.md`, `DEVLOG.md`, `CLAUDE.md`, the handoffs, `gotchas.md`, `flathub-plan.md`, `docs/desktop/emulators/`, `docs/postmortem/` and `scripts/postmortem.mjs`; no code. Local `main` has diverged from it. Before the next push: (1) commit those files locally as they are now (including the untracked `docs/postmortem/` and `scripts/postmortem.mjs`, or the merge refuses to overwrite them); (2) `git fetch origin` and `git merge origin/main` (merge, not rebase: identical content on both sides merges cleanly, a rebase would conflict on every DEVLOG commit); (3) on any conflict keep the local version, which is newer. The README rule above (README = full `PORT_PLAN.md`) is waiting on an owner decision: the new README links to `PORT_PLAN.md` instead of copying it, so do not rebuild it at the phase close without asking.

## Restart point (2026-11-08, owner reboot)

- Committed today: TV parts on every desktop profile and Game Mode → TV profile (d06f5d222); Emulators manager (b2fd80701) and its VM fixes plus the AppImage download fix. VMs shut off; the nested Game Mode session and Steam closed; the host Argosy is a local build of the uncommitted tree.
- **Uncommitted, gate-green, needs an on-screen check** (details in `DEVLOG.md`, "Emulators manager"): Game Mode emulator launch fix (`GameSessionEnvironment`, `HostShell`, `HostGameProcess`), Back button in the folder picker's hint row (`FileBrowserScreen.kt`), nav bar glow clip fix (`FloatingNavBar.kt`).
- **Next actions, in order:** (1) once the owner says Dolphin is reinstalled: nested Game Mode on the host (`gamescope -W 1920 -H 1080 -e -- steam -gamepadui`, launch the "Argosy Desktop (test)" shortcut with `steam steam://rungameid/<id>` from `build/gamemode/`, screenshots only with `GAMESCOPE_WAYLAND_DISPLAY=gamescope-0 gamescopectl screenshot <file>`, never full-desktop captures), Play Bust-A-Move 3000 (downloaded) and check Dolphin opens inside gamescope; check the nav glow; then commit those three. (2) Emulators manager: install picker, AppImage install, Assign BIOS (file; RomM with no firmware) checked in the VM; still open: EmuDeck detection on the owner's host and Assign BIOS with real RomM firmware. GitHub AppImage downloads were broken in every build until `java.net.http` was added to the jlink modules (DEVLOG). (3) Remove the test Steam shortcut when Game Mode testing ends (backup `build/gamemode/shortcuts.vdf.bak-20261108`; Steam must be closed while editing).
- The SteamOS/Bazzite VM lab is closed by owner decision (Game Mode testing on the host); its images and uncommitted `scripts/desktop/vmlab` changes stay.
- Never run two bundle builds at once (they collide in `desktopApp/build` and package a stale build).

## Scope and exit criterion

Owner decision 2026-10-27: the goal is Linux adaptation and compatibility, not a 1:1 clone of the Android app. `PORT_PLAN.md` Roadmap (Phase 5): Linux desktop and Steam Game Mode compatibility (GNOME, KDE, SteamOS); Flathub submission. Exit: install, wizard, sync, downloads, launches, save sync and a clean exit work on GNOME, KDE and SteamOS (desktop and Game Mode); `flatpak-builder-lint` clean; offline build green in CI for x86_64 and aarch64; Flathub PR merged.

Plan (`DEVLOG.md`, Phase 5 plan): 1 wizard wording, 2 Close Argosy, 3 thumbnails (done); 4 Linux desktop and Steam Game Mode compatibility; 5 Flathub submission.

## Carried forward

- Owner test results for `docs/desktop/owner-test-phase-4.md` (fix any failures first).
- Phase 3 items: download stall with a ROM already in the target folder; standalone-emulator save proof; "Scan for Android games" rows (owner questions).
- YouTube trailers on desktop (deferred; needs yt-dlp and a JavaScript runtime).

## Step 4 (Linux desktop and Steam Game Mode): in progress

- Game Mode part done in code, shipped as alpha 5.4: `libsigil-jni` (`:desktopApp:sigilJni` for dev runs; the Flatpak `sigil-jni` module with staged JDK headers), and a shutdown hook (`Main.kt` `shutDown`: `DesktopGamepadDispatcher.stop()` plus a 3 s halt watchdog; SIGTERM exits in about 0.4 s).
- Owner checks pending (alpha 5.4, the owner's Deck): real RomM covers on Home and Library; in Game Mode launch a GameCube/Switch/PS game and a GBA game, exit Argosy from the Steam overlay, type a collection name with Steam's keyboard (Steam + X). Failures: ask for `argosy-run.log` (Flatpak: under `~/.var/app/io.github.stealthx3.ArgosyDesktop`).
- Desktop part: the VM test lab below.
- Close with `--step-close 5.4` (republish) once the lab runs and the owner's Game Mode checks pass.

### VM test lab (owner approved 2026-10-27; start after the weekly reset)

- Fedora Workstation (GNOME, Wayland), Fedora KDE (Plasma, Wayland), Ubuntu LTS (GNOME). Downloads approved (about 10 GB).
- Disks under `/mnt/FileDrive/VMs/argosy-lab/`, libvirt `qemu:///system`, VM names `argosy-lab-*`. Never touch `debian13` or the Windows VMs.
- Lab runs in `qemu:///session` (visible in virt-manager under "QEMU/KVM User session"); scripts in `scripts/desktop/vmlab/`: `build.sh`, `apply.sh`, `reset.sh`, `exec.sh [--long] [--user]`, `shot.sh`, `app.sh`, `walk.sh`. Guests reach RomM at `http://10.0.2.2:8093`. Gotchas in `docs/desktop/gotchas.md` ("VM test lab").
- Unit 1 done (2026-10-29): all three VMs proven from `clean` (autologin desktop, guest-exec, screenshots, RomM 200, Flathub). KDE and Ubuntu were rebuilt (the pause had cut their first boot short) and repaired; `build.sh` now refuses to snapshot without `tester`'s seat0 session. Fedora GNOME: alpha 0.3.0 installed from the Pages `.flatpakref`, wizard to Home with 17 games synced, SIGTERM exit 497 ms (screens in `build/vmlab/fedora-gnome/`, not in git).
- Unit 2 done (2026-10-29): `desktop-test.sh <profile> <out>` (report in `<out>/report.txt`; `LAB_BUNDLE=<file>` installs a local build over the alpha) PASSES on all three desktops with the local build f684db1a. It fixes "Sign in on this device" (host `xdg-open`; it opened nothing in the Flatpak before). The fix is committed but not published: the alpha on Pages is still 2e7a3c30 until the step-5.4 close.
- Unit 3 done (2026-10-29): the test now covers RetroArch from Flathub (plus buildbot cores), the first sync, ROM download, RetroArch launch, session end and Close Argosy. Fedora KDE passes everything; on GNOME and Ubuntu, Close Argosy, log and SIGTERM pass, but download and launch fail because A on Home hit "View All" (details in `DEVLOG.md`, unit 3).
- Owner change (2026-11-04): Close Argosy moved from Quick Settings to the left drawer (under Settings), plus a last row on the Settings page. Checked on Fedora KDE with the gamepad. Published early at the owner's request: Pages serves f99eb770 (from 996687dd8; `gh-pages` b0e1c150, labelled "step 5.4 close", but step 4 is still open until the Deck checks pass). From Home: `DPAD_LEFT DOWN*8 A RIGHT A`.
- Owner changes (2026-11-04, on the alpha repo by the owner's OK): accent hover highlight on the Desktop profile; Platforms "Reset to Defaults"; Settings "Factory Reset" (restart through exit code 75 and the launcher loop). Details in `DEVLOG.md`. Lab: `mouse.sh` drives a real pointer. The wizard walk's `LEFT` picks Handheld in the VM; the hover highlight needs Desktop.
- Second early publish (2026-11-04, owner OK): Pages serves 7b68c15f (from 87824d162; `gh-pages` 10ab2ded, labelled "step 5.4 close", but step 4 is still open).
- VM tests of that build (2026-11-05): `desktop-test.sh` and new `reset-test.sh` pass on Fedora GNOME and Ubuntu GNOME; new `gamescope-test.sh` passes under headless gamescope on software rendering. Reports in `build/vmlab/fedora-gnome/{test-u7,reset-u7}`, `build/vmlab/ubuntu-gnome/{test-u9,reset-u9}`, `build/vmlab/fedora-kde/gamescope`. Not covered: real Steam (overlay, keyboard), a real GPU, mouse clicks on the new rows, Managed Installer removal, the Flathub launcher script. No product bug found. Open lab flake: Ubuntu's RetroArch install hung twice right after a reset; run by hand it took 6 min.
- Owner changes (2026-11-06, local): hover shows each control's own focus look (`HoverFocus`), fallback wash matches the focus fill, card hover capped at 1.1x, `LocalPointerInWindow` clears hover when the pointer leaves. Beta 1: version 0.4.0, README with install steps, plan with "Where to go from here". Beta 1 is published.
- Unit 4 done (2026-10-30): Home now refreshes when any library sync ends (`HomeSyncDelegate.observeLibrarySyncEnd`; shared code, Android gate green, Android emulator not run). `desktop-test.sh` passes every product check on all three desktops with local build e68b1170 (signed bundle `build/vmlab/bundle/argosy.flatpak`); reports in `build/vmlab/<profile>/test-u4*`. Not yet published: Pages still serves 2e7a3c30.
- Open findings: (1) fresh Flathub RetroArch has no cores (owner question 3); (2) the wizard walk still relies on waits (made longer; markers would be sturdier); (3) QR buttons move with Up/Down only; (4) one 5.7 s SIGTERM exit that did not reproduce; (5) Ubuntu VM has no browser.
- Unit 5 done (2026-10-30): XWayland and HiDPI at 125% and 150% render correctly on KDE and GNOME (`scale.sh`; screens in `build/vmlab/<profile>/u5/`). GNOME at 125% opens a smaller window (cosmetic; fullscreen is fine). The desktop part of step 4 is covered.
- **Beta 1 published (2026-11-06):** Pages serves 963973e6 (0.4.0, from 02494a9e3; `gh-pages` 7e0e20b, labelled "step 5.4 close" while step 4 stays open). Next action: step 4 closes only after the owner's Game Mode checks on the Deck (list above), now on Beta 1.

- **TV work (2026-11-07):** grey edge strips under gamescope fixed and committed (bd4ffcdc5; undecorated window), verified in the VM only, not published. TV tweaks approved by the owner and now coded, uncommitted, TV profile only (`isTvLayout`): screen switcher (nav ring) in each top row with controller bumper glyphs and the hint bar no longer auto-hiding; per-controller button glyphs (`ControllerGlyphs.kt`); sidebar capped at 1/4 width with `SpindleScrollIndicator`, tighter rows, battery hidden when absent; 14 sp text floor and lighter secondary text; accent edge glow behind focus (`tvFocusGlow`); NEW badge only on the focused card (dot elsewhere); 2 dp lines. Left alone: content above the hint bar. Deferred: 480. Owner approved the previews (2026-11-07); committed with the sidebar fix (hint bar hidden while the sidebar is open on TV, clock row visible), checked in the VM (`build/vmlab/fedora-kde/tvfix1080/`, bundle 8f56bf43). Still open: Home's platform selector stays narrow next to the centred switcher (`HomeScreen.kt` header row; `fillAvailableWidth` did not widen the pill; look at `SectionBreadcrumb`'s own width cap). Next action: fix that, then the queued owner requests below.
- **Owner requests, 2026-11-07/08 (owner said to proceed past the usage limit):** done and committed: Home switcher compact on TV so the platform selector has room; setup folder picker shows "Home" plus mounted drives (Flatpak now grants `/run/media`, `/media`, `/mnt`; `FlatpakInfo.REMOVABLE_MEDIA_FOLDERS`) and a Back button bottom-left; Settings > Navigation > Controller Layout offers Auto, Xbox, PlayStation, Nintendo, Steam on desktop, and button glyphs follow it on every desktop profile (`controllerFamily(layout, guid, nintendo)`; detection reads the SDL GUID vendor; under Steam Input every pad reads as Valve). Not verified with a real extra drive (the VM has none). Open: (1) emulators not appearing in Game Mode: cause is `flatpak-spawn --host` leaving Steam's process tree; options given to the owner (tag the emulator window with Argosy's `STEAM_GAME`, bundle RetroArch inside, or launch through a Steam shortcut); build after the SteamOS lab exists. (2) Linux emulator database: 22 desktop-only entries added (database v2, `EmulatorRegistry.registerPlatformEmulators`; owner chose desktop-only). VM-tested 2026-11-08 (DEVLOG): 16 of 20 Flatpaks run from Argosy's command, gopher64/Supermodel reject placeholder files as expected, shadPS4 fixed to `-d -g` (core needs real hardware), VBA-M crashed in the VM even with no arguments and was removed at the owner's request (21 desktop-only entries now); Mednafen and Mesen untested (no Flatpak); end-to-end Play with Nestopia UE works. (3) SteamOS/Bazzite lab in session `gamemode-lab` (images downloaded, builds pending).

## Open owner questions (revisit after testing, owner decision 2026-10-27)

1. Quick Settings Brightness is hidden on desktop (`screenBrightnessAvailable`): leave it out, or add backlight control for Linux handhelds and laptops?
2. The Flathub build's startup abort (step 5 next action).
3. Fresh Flathub RetroArch has no cores: should Argosy install the core a launch needs (libretro buildbot, into RetroArch's cores folder), or tell the user to use RetroArch's Core Downloader?
4. The app-id rename `io.github.Stealthx3.argosy_desktop` and the Flathub screenshots.

## Step 5 (Flathub) groundwork (owner OK 2026-10-27)

- Plan and lint status: `docs/desktop/flathub-plan.md`. Wayland socket dropped (lint error fixed).
- SDK-less desktop build: `ANDROID_HOME=<empty dir>` plus `-Pargosy.desktopOnly=true` (leaves out `:sigil` and `:libretrodroid`).
- Offline sources: `desktopApp/flatpak/generated/gradle-sources.json` (1,448 files; regenerate with `scripts/desktop/flatpak/regen-gradle-sources.sh <scratch>`).
- The from-source manifest `io.github.stealthx3.ArgosyDesktop.flathub.yml` builds offline locally with the openjdk21 extension; lint shows only owner-gated items. CI draft `.github/workflows/flatpak.yml` is untested until the phase-close push.
- Startup abort fixed (2026-10-30): the Flathub manifest starts the app from `/app/jre` through `desktopApp/flatpak/argosy-desktop-jre.sh` (reads the jpackage `.cfg`), not the jpackage launcher, which hit JDK-8348560. The smoke run shows no abort and no core dump; lint shows only the owner-gated items. Scratch build in `build/flathub-scratch/` (Builder: `flatpak run --filesystem=<repo> org.flatpak.Builder ...`; commit manifest inputs first, since the build uses git HEAD).
- Remaining step 5 work needs the owner: the app id (question 4), screenshots, the CI run (at the phase-close push), and the Flathub PR (owner go-ahead).
- **Next action:** nothing left that does not need the owner. Ask the owner, in one message: the Deck Game Mode checks for 5.4 (list above), the RetroArch core question, the app id and screenshots, and the Brightness question. Then set the pacer state to `waiting-owner`.

## Gotchas

- Walks: Quick Settings menus wrap; Close Argosy from Home is `DPAD_LEFT DOWN*8 A RIGHT A`. `desktop-drive.sh` ends with `pkill -f` on the main class name, so never put that name in the same Bash command.
- Dev data downloads go to the real `~/Games/Argosy`.
- Process probes: match the app instance by its unique `--key-pipe=<path>` and skip the `org.gradle` wrapper JVM; never signal by `pgrep -x java`.

## Where things are

- Desktop graph `shared/src/desktopMain/.../di/`; Flatpak `desktopApp/flatpak/`, scripts `scripts/desktop/flatpak/`; walks `scripts/desktop/check/`; JDK `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.
- Desktop data `~/.local/share/argosy-desktop` (never delete without the owner); Flatpak data `~/.var/app/io.github.stealthx3.ArgosyDesktop`.
- Alpha republish at every step close: `build-alpha.sh` → `publish-repo.sh` → `push-pages.sh --step-close 5.<n>`.
