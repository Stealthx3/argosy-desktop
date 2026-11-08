# Argosy Desktop

A Linux desktop port of the [Argosy launcher](https://github.com/rommapp/argosy-launcher), shipped as a Flatpak. Argosy Desktop runs Argosy's own interface on Linux and connects to a [RomM](https://github.com/rommapp/romm) server for your game library, downloads and save sync. You can drive it with a gamepad, a mouse and keyboard, or from the couch on a TV.

> **Status: Beta 1 (0.4.0).** Tested on Fedora GNOME, Fedora KDE and Ubuntu GNOME, at 125 % and 150 % scaling, and under gamescope. Steam Deck Game Mode checks and the Flathub submission are in progress.

This repository is a fork of `rommapp/argosy-launcher` and is licensed under GPL-3.0, like upstream. It is an independent port and not an official rommapp release.

## Install the beta

You need Flatpak. It comes preinstalled on Fedora, SteamOS, Bazzite and Linux Mint; on Ubuntu, run `sudo apt install flatpak`.

```bash
flatpak install --user --from https://stealthx3.github.io/argosy-desktop/io.github.stealthx3.ArgosyDesktop.flatpakref
flatpak run io.github.stealthx3.ArgosyDesktop
```

On first launch, the setup wizard asks for your RomM server, signs you in and picks a display profile (Handheld, Desktop or TV). It then shows the emulators Argosy can manage. Emulators install from Flathub; RetroArch is the usual first one:

```bash
flatpak install --user flathub org.libretro.RetroArch
```

- **Steam Deck / Game Mode:** install in Desktop Mode, add Argosy Desktop to Steam as a non-Steam game, then start it from Game Mode.
- **Updates:** the repository is signed, so `flatpak update` brings new builds and keeps your data.
- **Uninstall:** `flatpak uninstall --user io.github.stealthx3.ArgosyDesktop`. Add `--delete-data` to remove settings too; downloaded games stay in `~/Games/Argosy`.

This is a beta. Back up your emulator saves before turning on Save Sync, and include `argosy-run.log` from `~/.var/app/io.github.stealthx3.ArgosyDesktop` when you report a problem.

## Goals and how they are met

**1. Feel like Argosy, not a remake of it.** Argosy Desktop does not rebuild Argosy's screens. It runs the same Jetpack Compose UI on the desktop through [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/), so layouts, themes, sounds, animations and focus behaviour come from the same code as the Android app.

**2. Share one codebase with Android.** The single Android module was restructured into a Kotlin Multiplatform `shared` module. `commonMain` holds about 1,700 of Argosy's Kotlin files: the UI, view models, domain logic and the RomM data layer. Small `androidMain` and `desktopMain` layers replace the platform services underneath. The Android app still builds from the same tree, passes its 4,400+ unit tests, and showed pixel-identical screenshots before and after the restructure.

**3. Swap only what Android owns.** Each Android-only service sits behind an interface with a Linux implementation:

| Android | Linux |
| --- | --- |
| Intents, package manager | Emulator registry and host launch via `flatpak-spawn --host` |
| Gamepad key events | SDL3, feeding Argosy's own input dispatcher |
| WorkManager | Coroutine job queue |
| ExoPlayer | libmpv |
| Android storage and permissions | XDG folders, portals and Flatpak permissions |
| Hilt | Dagger (Hilt stays on Android) |
| Room and DataStore | Their Kotlin Multiplatform versions, same schema |

**4. Ship as a sandboxed Flatpak.** The app bundles a trimmed Java runtime, SDL3 and libmpv. It reaches the ROM library in `~/Games/Argosy` and emulator save folders through scoped filesystem grants, with no home-directory access. It starts emulators on the host and watches their process, so it knows when a session ends and can sync saves. A from-source manifest builds offline for Flathub.

**5. Stay close to upstream.** The restructure is scripted (`scripts/desktop/restructure/`), so it can be re-applied to each upstream release instead of maintained by hand. A read-only `upstream` branch tracks rommapp's newest stable release.

**6. Verify against real behaviour.**
- Every change passes the Android build and unit tests, A/B screenshots against upstream, and live save-sync proofs against a RomM test server.
- A VM lab (`scripts/desktop/vmlab/`) runs install, setup, sync, download, emulator launch and exit on several desktops and under gamescope.

## Desktop-specific features

- **Display profiles:** Handheld, Desktop and TV, detected automatically (device model, gamescope session, screen size). They are chosen in a new first-run Display step and can be changed in Settings.
- **TV profile:** couch-scale layout, a safe-area margin for overscan, controller-friendly screen switcher, larger text floor and a focus glow.
- **Window controls:** a normal title bar in windowed mode, F11 for full screen, a Start in Full Screen setting, `--windowed` / `--fullscreen` flags, and automatic full screen under gamescope. The first launch opens in the dark theme.
- **Controller input mode:** Auto, Steam Input or SDL3. SDL3 reads Steam Deck and Steam Controller buttons directly.
- **Per-controller button mapping:** every navigation action can be remapped, stored per controller. The A/B, X/Y and Start/Select swaps are presets.
- **Controller layout and glyphs:** Auto, Xbox, PlayStation, Nintendo or Steam, with footer button icons that follow the layout and any remapping.
- **Mouse support:** on the Desktop profile, hovering shows each control's gamepad-focus look. The cursor hides while a controller is in use on Handheld and TV.
- **Linux emulator support:**
  - Finds emulators installed as Flatpaks, AppImages, through EmuDeck or on `PATH`, and launches them on the host.
  - A Linux emulator compatibility database drives suggestions, with consented one-click Flathub installs.
  - Managed updates for AppImage releases and Flathub emulators.
  - 21 desktop-only emulator entries.
- **Save sync for host emulators:** reads and writes RetroArch and standalone-emulator save folders in place, including EmuDeck layouts.
- **Flatpak Permissions screen:** shows each sandbox permission live from `/.flatpak-info`, says what it is for, and can grant emulator Flatpaks access to the games folder with your consent.
- **Host GPU page:** read-only GPU, driver, VRAM, OpenGL and Vulkan details, plus the displays in use.
- **Linux file locations:** the default ROM folder is `~/Games/Argosy`, Music and Downloads follow your XDG folders, and the setup folder picker lists mounted drives.
- **Apps tab with host apps:** lists, launches and pins your installed desktop applications.
- **Quick Settings through the host:** volume and controller rumble.
- **Close Argosy:** in the left drawer and as the last Settings row, with a confirmation.
- **Resets:** Platforms → Reset to Defaults, and Settings → Factory Reset, which removes Argosy's data and restarts into setup.
- **Media:** libmpv plays local video and theme audio.

## How Argosy Desktop differs from Android

Owner decision on 2026-10-23, confirmed on 2026-10-27: the desktop app aims at **Linux adaptation and compatibility, not a 1:1 copy** of the Android app. Every difference is a desktop-only switch, so the Android app and its tests are unchanged.

- **Removed on desktop:**
  - Steam account and library import.
  - Jellyfin media.
  - Argosy Social, including QuayPass / Check-In.
  - The "Android Settings" row.
- **Not in this version:**
  - Built-in libretro emulation and netplay. Games run in external emulators.
  - Dual-screen. The desktop uses a single window.
  - YouTube trailers. Local video works.
- **Replaced with Linux equivalents:**
  - Play Store emulator suggestions become Flathub suggestions.
  - APK installers become AppImage and Flathub management.
  - Android GPU driver downloads become a read-only host GPU page.
  - Android runtime permissions become Flatpak permissions.
- **Added on desktop:** display profiles and the wizard's Display step, window and full-screen controls, the controller input mode, button mapping and layout choice, mouse hover, and the resets listed above.
- **Planned after 1.0:** native Steam Input (Steam glyphs and configuration), loaded at runtime when Steam is present.

The full reasoning, with each decision dated, is in [`PORT_PLAN.md`](PORT_PLAN.md) under "Desktop divergences".

## Building from source

Requirements: JDK 21 and Gradle (the wrapper is included). The Android SDK is needed only for the Android app.

```bash
git clone https://github.com/Stealthx3/argosy-desktop.git
cd argosy-desktop
git submodule update --init --recursive
./gradlew :desktopApp:run                                    # run the desktop app
ANDROID_HOME=$(mktemp -d) ./gradlew -Pargosy.desktopOnly=true :desktopApp:assemble   # desktop-only build without the Android SDK
```

Flatpak builds use `scripts/desktop/flatpak/build-alpha.sh` (local repository) or the from-source manifest in `desktopApp/flatpak/` (offline build for Flathub).

## Repository layout

| Path | What it holds |
| --- | --- |
| `shared/` | Kotlin Multiplatform module: `commonMain` (UI, domain, data), `androidMain`, `desktopMain` |
| `app/` | Thin Android application module |
| `desktopApp/` | Desktop entry point, window, SDL3 input, Flatpak manifests |
| `desktop-libs/` | Small desktop shims (graphics, libretrodroid API, sigil) |
| `scripts/desktop/` | Restructure and re-apply tooling, verification walks, VM lab, Flatpak publishing |
| `docs/desktop/` | Phase handoffs, survey, testbed runbook, Flathub plan, gotchas |
| `PORT_PLAN.md` | The full port plan: architecture, parity matrix, Flatpak design, roadmap, risks |
| `DEVLOG.md` | Action-and-reasoning log, one section per phase |

## Roadmap

| Phase | Status |
| --- | --- |
| 0 · Discovery and spikes | Done (2026-10-16) |
| 1 · Kotlin Multiplatform restructure | Done (2026-10-16) |
| 2 · Desktop UI | Done (2026-10-22) |
| 3 · Linux services and alpha Flatpak | Done (2026-10-23) |
| 4 · Linux adaptation | Done (2026-10-24) |
| 5 · Beta and Flathub | In progress: Beta 1 released; Steam Deck Game Mode checks, then Flathub |

Ideas for after 1.0 are listed in [`PORT_PLAN.md`](PORT_PLAN.md) under "Where to go from here".

## Credits and license

Argosy is by the [rommapp](https://github.com/rommapp) team; upstream's own README is kept as [`README.upstream.md`](README.upstream.md). Argosy Desktop is licensed under the [GNU GPL v3.0](LICENSE), like upstream.
