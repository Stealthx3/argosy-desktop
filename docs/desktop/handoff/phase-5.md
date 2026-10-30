# Phase 5 · Beta and Flathub — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`. Earlier phases: `phase-0.md` … `phase-4.md` (closed summaries).

**Updated:** 2026-10-29 (owner said "resume" in the main session; VM lab unit 3 done). Steps 1–3 done; step 4 in progress.

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
- Open findings: (1) fresh Flathub RetroArch has no cores, so first launch fails with "No core available" (owner question below); (2) Home may not refresh after the first sync (Ubuntu); (3) the wizard walk's fixed sleeps drift (one Fedora GNOME run never paired); (4) QR buttons move with Up/Down only; (5) one 5.7 s SIGTERM exit that did not reproduce.
- **Next action (unit 4):** investigate finding 2: start on Ubuntu (`reset.sh`, install, bundle, wizard), then compare Home before and after `syncLibrary returned`, and read how the desktop Home rows observe the games table. Fix it if it is a refresh bug. Then replace the wizard's fixed sleeps with log markers, and rerun `desktop-test.sh` on all three (`LAB_BUNDLE=build/vmlab/bundle/argosy.flatpak`; rebuild and re-sign the bundle after any code change). After that: XWayland and HiDPI at 125%/150%.
- Later units: the full test per desktop (alpha repo install, wizard, sync, covers, download and RetroArch launch, Close Argosy, SIGTERM, XWayland and HiDPI at 125%/150%), with a report per desktop.

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
- **Next action (after testing):** the startup abort (`pure virtual method called`) in the Flathub build only. Run the build-dir launcher under `strace -f -e trace=process` to see which process aborts. If it is the jpackage launcher, compare with a host 21.0.13 jpackage. Smoke run: `flatpak build --socket=x11 --device=dri --share=network --env=XDG_{DATA,CONFIG,CACHE}_HOME=<scratch> <build-dir> /app/bin/argosy-desktop --windowed` (never the owner's data).

## Gotchas

- Walks: Quick Settings menus wrap (Close Argosy is `R3 DOWN*8`). `desktop-drive.sh` ends with `pkill -f` on the main class name, so never put that name in the same Bash command.
- Dev data downloads go to the real `~/Games/Argosy`.
- Process probes: match the app instance by its unique `--key-pipe=<path>` and skip the `org.gradle` wrapper JVM; never signal by `pgrep -x java`.

## Where things are

- Desktop graph `shared/src/desktopMain/.../di/`; Flatpak `desktopApp/flatpak/`, scripts `scripts/desktop/flatpak/`; walks `scripts/desktop/check/`; JDK `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.
- Desktop data `~/.local/share/argosy-desktop` (never delete without the owner); Flatpak data `~/.var/app/io.github.stealthx3.ArgosyDesktop`.
- Alpha republish at every step close: `build-alpha.sh` → `publish-repo.sh` → `push-pages.sh --step-close 5.<n>`.
