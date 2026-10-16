# Argosy Desktop — Linux Flatpak Port Plan

As of 2026-10-16 · Micaiah Fonken · Source of truth: the "Argosy Desktop — Linux Flatpak Port Plan" doc. This file is a copy kept in the repository.

> **Phase 0 corrections** (from the codebase survey, see `docs/desktop/phase-0-survey.md`): upstream is now **v2.23.0** (not v2.10), and **TV Compose is no longer used anywhere**, so the TV Compose swap below is already done upstream.

> **Phase 0 results** (2026-10-16, details in `DEVLOG.md` and each spike's folder): gate **passed**. The Android app builds unchanged. Upstream's Room database (v214) runs on the JVM under Room KMP with no source edits and an identical schema hash. Upstream's Library list and theme render in a Compose desktop window with 71 files compiled unchanged; 15 Android APIs needed shims, and resources (6,254 strings) are the largest Phase 2 item. A sandboxed Flatpak starts the RetroArch Flatpak on a ROM in `~/Games/Argosy` and receives its exit status. Proposed change: `org.freedesktop.Platform` 24.08 is end-of-life, so build on 25.08 from Phase 1.

## Summary

Ship Argosy Desktop for Linux as a Flathub Flatpak that a user cannot tell apart from the Android app, by running Argosy's own Jetpack Compose UI on the desktop through Compose Multiplatform. Alpha from a self-hosted Flatpak repo at about week 19; 1.0 on Flathub at about week 32.

The guiding rule is **reuse, don't rebuild**. Compose Multiplatform uses the same `androidx.compose` APIs as Jetpack Compose, so Argosy's screens, view models and data layer move into shared modules almost unchanged. Only what touches Android itself (intents, package manager, ExoPlayer, WorkManager, device controls, the libretro wrapper) gets a desktop implementation behind an interface.

The strongest version of this plan is one codebase for both apps: the Android app and Argosy Desktop build from the same shared modules, so every upstream feature and visual change lands on Linux automatically. That needs upstream buy-in, which is the first open decision.

Timeline assumes one developer at about 20 hours a week with AI-assisted coding; it scales roughly linearly with hours.

## Starting point: upstream Argosy

Upstream is a single-module Android app, but nearly all of it is Kotlin and Compose that can run on the desktop JVM. Facts below are from the [rommapp/argosy-launcher repo](https://github.com/rommapp/argosy-launcher) and its [version catalog](https://github.com/rommapp/argosy-launcher/blob/main/gradle/libs.versions.toml).

| Area | Upstream today | Desktop path |
| --- | --- | --- |
| Language / build | Kotlin 2.3.20, Gradle, AGP 8.7.3, one `app` module | Kotlin Multiplatform: `shared` + `androidApp` + `desktopApp` |
| UI | Jetpack Compose, Material 3 | Compose Multiplatform; same APIs, code moves almost as is. CMP release matched to Android's Compose BOM so Android does not change |
| TV Compose | No longer used (Phase 0 finding) | Nothing to do |
| Navigation, ViewModel | Navigation Compose 2.8, Lifecycle 2.8 | JetBrains multiplatform builds of the same libraries |
| DI | Hilt 2.57 | Decided in Phase 1: plain Dagger 2 component on the desktop, Hilt kept on Android; `javax.inject` and `@InstallIn` (`hilt-core`) are plain JVM |
| Database | Room 2.7.1 | Room KMP with the bundled SQLite driver; same schema |
| Settings | DataStore Preferences 1.1.1 | DataStore KMP |
| Network | Retrofit 2.9, OkHttp 4.12, Moshi, Socket.IO | Unchanged: both targets are JVM, so these stay in a shared JVM source set |
| Images | Coil 2.7 | Coil 3 (multiplatform) |
| Media | Media3/ExoPlayer (Jellyfin, video wallpaper) | libmpv rendered into a Compose surface |
| Colour extraction | Palette | Multiplatform port of Palette |
| Background work | WorkManager 2.10 | Interface; WorkManager on Android, persistent coroutine queue on desktop |
| Archives | commons-compress, xz, zstd-jni | Unchanged (plain JVM) |
| Steam | JavaSteam fork + DepotDownloader | Unchanged libraries; Linux Steam library integration |
| Built-in emulation | `libretrodroid` module | Desktop libretro frontend loading Linux cores |
| Resources | Android `R.*` strings and drawables | Compose Multiplatform resources |
| Device | CameraX (QR scanner only), BLE (QuayPass only), haptics | Webcam, BlueZ, SDL rumble behind interfaces |

License is GPL-3.0, so the port must also be GPL-3.0 and ship source. `testbed/romm` holds Docker Compose setups for RomM 4.9, 5.0 and 5.1 for integration tests.

## Architecture

Argosy Desktop is a single JVM app whose screens, view models and data layer are Argosy's own code in shared modules; a thin desktop platform layer replaces each Android service.

```
┌──────────────── Shared Argosy code (Kotlin Multiplatform, commonMain) ────────────────┐
│  Compose UI                   ViewModels and domain          Data layer               │
│  every screen and layout      navigation, focus, actions     RomM client, Room,       │
│  themes, sounds, animations   collections, search            DataStore, downloads,    │
│  identical on both apps       launch and sync rules          archives, Steam libs     │
└───────────┬───────────────────────────────────────────────────────┬───────────────────┘
            ▼                                                       ▼
┌──────────────────────┐          ┌ ─ ─ ─ ─ ─ ─ Flatpak sandbox ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
│ Android app          │            Argosy Desktop platform layer (desktopMain)
│ androidMain          │          │ SDL3 gamepad input, fullscreen window              │
│ Intents, ExoPlayer   │            flatpak-spawn launcher, emulator registry
│ WorkManager          │          │ libmpv media, libretro frontend                    │
│ libretrodroid        │            brightness, volume, rumble via the host
└──────────────────────┘          │ Secret portal, XDG paths                           │
                                   ─ ─ ─ ─ ─ ┬ ─ ─ ─ ─ ─ ─ ┬ ─ ─ ─ ─ ─ ─ ┬ ─ ─ ─ ─ ─ ─ ─
                                             ▼             ▼             ▼
                                        RomM server    ROM library   Host emulators
                                        (network)      ~/Games/Argosy and desktop apps
```

Look-alike is guaranteed by construction: the pixels come from the same composables, themes, fonts and animations as on Android, so the work is in the platform layer, not the UI.

**Module layout.** Restructure upstream into Kotlin Multiplatform modules with three source sets: `commonMain` (UI, view models, domain), `jvmShared` (Retrofit, Moshi, OkHttp, archive and Steam libraries that run on both Android and desktop JVMs) and per-target `androidMain` / `desktopMain`. The Android app becomes a thin `androidApp` module over `shared`.

> **Phase 1 decision** (owner, 2026-10-16): no separate `jvmShared` set. Both targets are JVMs, so `commonMain` can use JDK APIs and the plain-Java libraries directly. The Android app keeps its upstream module name `app/` (it plays the `androidApp` role), which avoids renaming about 1,000 Android-only files on every upstream merge. Modules: `:shared` (KMP: `commonMain`, `androidMain`, `desktopMain`), `:app`, `:desktopApp`. Package names never change. Plan and order of moves: `DEVLOG.md`, Phase 1.

**Platform interfaces.** Every Android call site gets an interface in `commonMain` with two implementations: `GameLauncher`, `AppSource`, `MediaPlayer`, `BackgroundJobs`, `DeviceControls` (brightness, volume, haptics, performance), `SecretStore`, `FilePaths`, `Updater`.

**Input.** Compose desktop has no gamepad support, so `desktopMain` reads controllers through SDL3 (via JNA or Java's foreign function API) and feeds the same button actions Argosy's input handling expects: D-pad, A/B/X/Y, shoulders, L3/R3, Start/Select. Keyboard maps to the same actions. Phase 0 finding: upstream already maps Android key codes to a platform-free `GamepadEvent` / `InputHandler` layer (`ui/input/GamepadInputHandler.kt`), so SDL3 only has to emit the same key codes.

**Window.** Undecorated fullscreen by default (the Android app is always fullscreen), with a windowed mode for desktop use. Handheld targets such as Steam Deck Game Mode get the same layout as an Android handheld of that resolution.

**Media.** Jellyfin playback, trailers and video wallpaper use libmpv rendered into a Compose surface, with Argosy's existing player controls on top.

**Built-in emulator.** A desktop counterpart to `libretrodroid`: a small native libretro frontend (C via JNI) that loads Linux cores, renders through OpenGL into the Compose window and reuses Argosy's in-game menu, so the built-in experience matches Android.

## Feature parity matrix

Nothing is dropped: Port means the shared code runs as is, Adapt means the same screen and behaviour sit on a Linux implementation, and the two Defer items need a sandbox permission or a fact-finding step first.

| Feature | Decision | Phase | Linux notes |
| --- | --- | --- | --- |
| RomM connection, library sync, IGDB metadata | Port | 1 | Token stored through the Secret portal |
| ROM and BIOS downloads, sort by platform | Port | 1 | Writes to the shared ROM folder |
| Archive extraction | Port | 1 | Same JVM libraries |
| Collections, smart collections, favorites, hide | Port | 1 | Shared data layer |
| Ratings and play status synced to RomM | Port | 1 | Shared data layer |
| Home layouts, Library, Details, Downloads, Settings screens | Port | 2 | Same composables; resource swaps only |
| Sidebar, guide bar, Quick Menu (L3) | Port | 2 | Same composables; SDL3 supplies the buttons |
| Themes, backgrounds, 3D box art, UI sounds, BGM | Port | 2 | Same assets and composables |
| RetroAchievements | Port | 2 | Shared code, including offline support |
| Restart from Settings > About | Port | 2 | Relaunches the Flatpak |
| Save sync and save channels | Port | 3 | Argosy points emulator save folders into the shared folder, as it configures RetroArch on Android |
| Now-playing to RomM, Argosy Social | Port | 3 | Fired on launch and process exit |
| First-run wizard | Adapt | 3 | Same screens plus an emulator folder-access step |
| Quick Settings (R3) | Adapt | 3 | Brightness and volume via the host; performance and fan on supported Linux handhelds |
| Haptics | Adapt | 3 | SDL controller rumble |
| Emulator detection, assignment, autoconfig, RetroArch cores, multi-disc | Adapt | 3 | Linux registry: Flatpak, native and AppImage emulators |
| Apps tab | Adapt | 3 | Host .desktop apps, launched on the host |
| Jellyfin media with offline downloads | Adapt | 4 | libmpv player behind Argosy's own controls |
| Video wallpaper | Adapt | 4 | Same libmpv surface |
| Built-in emulation and netplay | Adapt | 4 | Desktop libretro frontend, Linux cores |
| Steam integration | Adapt | 4 | Reads the host Steam library; launches via `steam://rungameid/` |
| Dual-screen | Adapt | 4 | Second window on a second display |
| In-app updater | Adapt | 5 | Same About screen; Flatpak performs the update |
| QuayPass (BLE) | Defer | 1.1 | Needs Bluetooth access through BlueZ; decide after 1.0 |
| Camera features (QR scanner) | Defer | 1.1 | Webcam QR scanning; manual code entry covers 1.0 |

## Flatpak design

The package is one Flatpak holding a trimmed Java runtime, Argosy Desktop and its native helpers (SDL3, libmpv, the libretro frontend); it launches external emulators on the host through `flatpak-spawn --host`, the only supported way for one Flatpak to start another.

### Manifest basics

| Item | Choice |
| --- | --- |
| App ID | `io.github.<your-account>.ArgosyDesktop` (must match a GitHub account or domain you control) |
| Runtime | `org.freedesktop.Platform` + `org.freedesktop.Sdk`, current branch |
| SDK extensions | `openjdk` for the build and the bundled runtime |
| Java runtime | `jlink`-trimmed JRE in `/app/jre`; JetBrains Runtime later for native Wayland |
| Bundled modules | SDL3 and libmpv if the runtime lacks them; the libretro frontend built from source |
| Libretro cores | Downloaded at runtime into the app's data folder, as Android downloads cores |
| Architectures | x86_64 and aarch64 |
| Updates | Flatpak updates the app; About screen keeps its look |

### Permissions (finish-args)

| Permission | Why |
| --- | --- |
| `--share=network` | RomM, Jellyfin, artwork |
| `--share=ipc`, `--socket=wayland`, `--socket=x11` | Display; Java's desktop toolkit runs on X11 or XWayland until native Wayland lands |
| `--device=dri` | GPU acceleration |
| `--device=all` | Gamepads in `/dev/input`; switch to `--device=input` once Flathub builders and target distros all run Flatpak 1.16+ |
| `--socket=pulseaudio` | UI sounds, BGM, video |
| `--filesystem=~/Games/Argosy:create` | Shared ROM and BIOS library at a host path both Argosy and emulators can see |
| `--talk-name=org.freedesktop.Flatpak` | `flatpak-spawn --host` to start emulators and list installed Flatpaks |

No `--filesystem=home`. Secrets use libsecret, which falls back to the Secret portal inside the sandbox. Database, settings and cache live under `~/.var/app/<app-id>/`.

### Launching emulators from the sandbox

- **Host-visible paths only.** Every ROM path passed to an emulator must exist on the host, so ROMs never live under `~/.var` or a document-portal path. That is why the library uses a static filesystem grant instead of the file chooser.
- **Three launch strategies** behind one `LaunchStrategy` interface: Flatpak emulator (`flatpak-spawn --host flatpak run <id> <args>`), native binary on the host PATH, and AppImage.
- **Emulator Flatpaks need the ROM folder too.** The first-run wizard detects each installed emulator Flatpak and, with consent, runs `flatpak override --user --filesystem=~/Games/Argosy <emulator-id>` on the host.
- **Supervision.** `flatpak-spawn` stays attached until the host process exits, which gives play-session start and end for now-playing reports and save sync.
- **Discovery** runs `flatpak list --app` on the host, checks PATH, `~/Applications` AppImages, and RetroDECK/EmuDeck install folders against a Linux emulator registry (ID, launch template, supported platforms, save path).

### Offline build (Flathub requirement)

Flathub builds without network, so every dependency is pre-declared as a source:

- Gradle: generate `gradle-sources.json` with [jwharm/flatpak-gradle-generator](https://github.com/jwharm/flatpak-gradle-generator) or the [meshtastic gradle-flatpak-sources](https://plugins.gradle.org/plugin/org.meshtastic.flatpak.sources) plugin, then build with `--offline` against the local Maven folder.
- Native modules (SDL3, libmpv, the libretro frontend): pinned source archives or git commits with checksums in the manifest.
- Regenerate the Gradle sources in CI on every dependency change; a stale sources file is the most common Flathub build failure.

### Store listing

AppStream metainfo with controller-first screenshots, OARS rating, release notes per version and a `launchable`; a .desktop file with the `Game` category; icons at 128, 256 and scalable SVG. Validate with `flatpak-builder-lint` before every submission.

Sources: [Flathub forum on launching Flatpaks from Flatpaks](https://discourse.flathub.org/t/launching-flatpak-inside-flatpak/5708), [Flathub forum on device=input](https://discourse.flathub.org/t/support-for-device-input/6645).

## Roadmap

Six phases over about 32 weeks; restructure first, then get every screen on the desktop, then replace Android services one by one, so a playable alpha exists at week 19.

| Phase | Weeks | Scope | Gate after |
| --- | --- | --- | --- |
| 0 · Discovery and spikes | 1–2 | Library screen in a desktop window, sandbox launch, Room KMP; upstream conversation started | All three spikes pass |
| 1 · Kotlin Multiplatform restructure | 3–8 | Shared modules, DI, Room and DataStore KMP, shared resources; Android app unchanged for users | — |
| 2 · Desktop UI | 9–13 | Every screen runs on the desktop; SDL3 gamepad input; identical focus feel | — |
| 3 · Linux services and alpha Flatpak | 14–19 | Emulator registry, host launch, save paths, Apps tab, Quick Settings; Flatpak from your own repo | Alpha: sync, download, play, save sync |
| 4 · Remaining parity | 20–28 | libmpv media and video wallpaper, Steam, dual-screen, desktop libretro frontend and netplay | — |
| 5 · Beta and Flathub | 29–32 | Side-by-side checks against Android; GNOME, KDE and SteamOS testing; Flathub submission | 1.0 live on Flathub |

Exit criteria per phase:

1. **Phase 0:** upstream's Library screen renders in a desktop window; a sandboxed test Flatpak starts the RetroArch Flatpak on a ROM; upstream's schema opens under Room KMP on the JVM.
2. **Phase 1:** `shared` builds for Android and desktop with no Android imports in `commonMain`; the Android app passes its existing tests and looks unchanged.
3. **Phase 2:** every screen opens on the desktop and is fully navigable by gamepad; screenshots match an Android handheld at the same resolution.
4. **Phase 3:** emulators detected on a clean Fedora and a SteamOS install; a game launches, exits and returns to Argosy; saves sync to RomM; alpha installs from your own Flatpak repo.
5. **Phase 4:** every Port and Adapt row of the parity matrix works; built-in emulation runs at least the cores Android ships, or is moved to 1.1 by decision.
6. **Phase 5:** testers using both apps cannot name a behavioural difference; `flatpak-builder-lint` clean; offline build green in CI for x86_64 and aarch64; Flathub PR merged.

## Risks and mitigations

The two risks that decide the project are upstream drift and the sandbox; both are tested in Phase 0 before any large refactor.

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Upstream moves fast (3,300+ commits) and the KMP restructure is a big diff | A private fork drifts; the desktop app looks older than Android within months | Pitch the KMP split to rommapp first; if declined, script the restructure (file moves, package swaps) so it re-applies to each upstream release |
| Flathub questions `org.freedesktop.Flatpak` access | Blocks the Flathub listing | Justify it (a launcher must start emulators); ship from a self-hosted repo first; AppImage as fallback |
| Emulator Flatpaks cannot read ROMs or write saves in the shared folder | Games fail; save sync breaks | Wizard applies per-emulator overrides with consent; launch preflight explains any fix needed |
| Android-only calls scattered through screens (Context, toasts, R.*) | UI phase slips | Phase 0 counted them (see survey); compatibility shims in `desktopMain` for the common ones |
| Focus feel differs on desktop | App feels different on a gamepad | Side-by-side video tests of every screen against an Android handheld before alpha |
| Gamepad edge cases (hotplug, Steam Input remapping, handheld built-in pads) | Wrong buttons or none | SDL3 controller database; test on Steam Deck, an Xbox pad and a DualSense |
| Desktop libretro frontend effort (video, audio sync, input, save states) | Built-in emulation slips | Scheduled last in Phase 4; may ship in 1.1 while external emulators cover 1.0 |
| Java toolkit on XWayland (fractional scaling, frame pacing) | Blurry or stuttery UI on Wayland desktops | Test GNOME and KDE at 125% and 150%; move to JetBrains Runtime's native Wayland toolkit if needed |
| `--device=all` flagged potentially unsafe | Lower trust on the store page | Move to `--device=input` once Flatpak 1.16+ is universal |
| Naming, icon and GPL-3.0 obligations | Confusion with upstream; legal exposure | Agree name and icon with rommapp; whole port GPL-3.0 with a source link in About |

## Open decisions and next steps

Four decisions shape everything after Phase 0; settle them in the first two weeks.

- **Upstream or fork.** Contribute the KMP restructure to rommapp so both apps share one codebase (recommended), or keep a fork with a re-apply script.
- **Name and app ID.** "Argosy Desktop" with upstream's blessing, under rommapp or your own GitHub account.
- **Java runtime.** Stock OpenJDK on XWayland for 1.0, or JetBrains Runtime with native Wayland from the start.
- **Built-in emulation timing.** In 1.0, or 1.1 with external emulators only at launch.

Phase 0 tasks:

- [x] Fork rommapp/argosy-launcher (done: Stealthx3/argosy-desktop)
- [x] Build the Android app unchanged, as the baseline
- [x] Count Android-only imports in `app/` by package and by screen to size the restructure
- [x] Record how upstream turns gamepad key events into actions, so SDL3 can feed the same path
- [x] Spike: one real Argosy screen (Library) rendering in a Compose desktop window with fake data
- [x] Spike: a sandboxed test Flatpak starts the RetroArch Flatpak on a ROM in `~/Games/Argosy`
- [x] Spike: Room KMP + bundled SQLite opening upstream's schema on the JVM
- [x] Check what CameraX and BLE are used for, and whether `testbed/romm` gives a disposable RomM for tests
- [ ] Open an issue or Discord thread with rommapp maintainers proposing the shared KMP codebase (draft in `docs/desktop/upstream-proposal.md`; posting is the owner's call)
