# Phase 5 · Beta and Flathub — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`. Earlier phases: `phase-0.md` … `phase-4.md` (closed summaries).

**Updated:** 2026-10-29 (owner said "resume"; VM lab unit 1 done). Steps 1–3 done; step 4 in progress.

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
- Findings so far (in `DEVLOG.md`, 2026-10-29): QR pairing buttons move with Up/Down only; "Sign in on this device" opened no browser in the Flatpak (the VM has no browser; retest where one exists before calling it a bug).
- **Next action (unit 2):** write `scripts/desktop/vmlab/desktop-test.sh <profile> <out>` from the steps proven by hand: `reset.sh`, `app.sh install`, `app.sh start`, the Flatpak wizard walk in `gotchas.md` (server `http://10.0.2.2:8093`), covers on Home, `app.sh term` timing, plus a short report file. Run it on all three; then add a download and a RetroArch launch (install `org.libretro.RetroArch` from Flathub in the guest), Close Argosy, XWayland and HiDPI at 125%/150%.
- Later units: the full test per desktop (alpha repo install, wizard, sync, covers, download and RetroArch launch, Close Argosy, SIGTERM, XWayland and HiDPI at 125%/150%), with a report per desktop.

## Open owner questions (revisit after testing, owner decision 2026-10-27)

1. Quick Settings Brightness is hidden on desktop (`screenBrightnessAvailable`): leave it out, or add backlight control for Linux handhelds and laptops?
2. The Flathub build's startup abort (step 5 next action).
3. The app-id rename `io.github.Stealthx3.argosy_desktop` and the Flathub screenshots.

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
