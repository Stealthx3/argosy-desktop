# Argosy Desktop — Linux Flatpak Port Plan

As of 2026-10-16 · Micaiah Fonken · Source of truth: the "Argosy Desktop — Linux Flatpak Port Plan" doc. This file is a copy kept in the repository.

> **Phase 0 corrections** (from the codebase survey, see `docs/desktop/phase-0-survey.md`): upstream is now **v2.23.0** (not v2.10), and **TV Compose is no longer used anywhere**, so the TV Compose swap below is already done upstream.

> **Phase 0 results** (2026-10-16, details in `DEVLOG.md` and each spike's folder): gate **passed**. The Android app builds unchanged. Upstream's Room database (v214) runs on the JVM under Room KMP with no source edits and an identical schema hash. Upstream's Library list and theme render in a Compose desktop window with 71 files compiled unchanged; 15 Android APIs needed shims, and resources (6,254 strings) are the largest Phase 2 item. A sandboxed Flatpak starts the RetroArch Flatpak on a ROM in `~/Games/Argosy` and receives its exit status. Proposed change: `org.freedesktop.Platform` 24.08 is end-of-life, so build on 25.08 from Phase 1.

> **Phase 1 results** (2026-10-16, details in `DEVLOG.md`): exit criteria **met**. `shared` (Kotlin Multiplatform, Android + desktop) holds 560 Kotlin files in `commonMain` and 10 in `androidMain`; `app` keeps 1,140 of the original 1,695. `commonMain` has no Android imports, enforced by compiling it for the desktop without `android.jar`. The Android app passes its 4,359 unit tests, its dependency versions are unchanged, its APK resource table is identical, and A/B screenshots against the pre-Phase-1 APK (English and German, populated RomM testbed) are pixel-identical. The save-sync zone moved its first stage with live proof (`no_op` twice, identical server and device state). Decisions made in the phase: no `jvmShared` set (both targets are JVMs); `app/` keeps its name; Dagger on desktop, Hilt on Android (Hilt also runs in `shared`); Compose Multiplatform 1.7.3 to match Android's Compose BOM; Android keeps native resources while desktop serves the same strings through Compose Multiplatform resources behind the same `R` API; `app` treats `shared` as a Kotlin friend module so upstream's smart casts and `internal` declarations keep working across the module boundary. Re-applying the restructure on upstream v2.24.0 was rehearsed: `scripts/desktop/restructure/reapply.sh` resolved both conflicts, and the merged tree builds and passes 4,426 tests. Phase 2 sizing: 636 UI files remain in `app`, 307 with direct blockers (`Context`/`LocalContext`, Coil 2, Hilt Android entry points, Android lifecycle, Media3, `Bitmap`).

> **Phase 2 results** (2026-10-22, details in `DEVLOG.md`): upstream's whole UI runs in the desktop window. `commonMain` now holds 1,720 Kotlin files and `androidMain` 243; `app` keeps 30. SDL3 gamepad input and the keyboard drive upstream's own InputDispatcher, so focus and navigation behave as on Android. Every screen reachable with the RomM testbed was walked with the same pad steps on an Android handheld emulator and on the desktop and matches (first run, Home and drawer, Game Detail and its sheets, Library, Collections, Downloads, Library Sync, Save Sync, Check-In, Apps, every Settings page, About, Changelog, file browser). Not yet opened on the desktop: Media Library/Detail (needs a Jellyfin server in the testbed), the Social screens (need an Argosy Social session), Save Timeline and the emulator rows of Per-Game Settings (need Phase 3 emulators); Search and Manage Pins have no entry point in upstream v2.24.0 on either platform. Still to check: a physical pad, Nintendo layouts and stick navigation.

> **Desktop divergences** (owner decision, 2026-10-23, after a first look at the alpha): Argosy Desktop no longer aims to match Android feature for feature. Steam, Jellyfin, Argosy Social and the "Android Settings" row are removed from the desktop app; built-in emulation, netplay and dual-screen are not part of this version; Android-only screens (emulator suggestions, managed installers, GPU drivers, permissions) become Linux screens; the desktop gains a system title bar, windowed dark first launch, a full-screen setting, a controller input mode (Auto / Steam Input / SDL3), per-button navigation mapping, display profiles (Handheld/Desktop/TV) and a Display setup step in the first-run wizard. Details: "Desktop divergences" below; Phase 4 in the Roadmap now carries this work. The Android app is unchanged: every removal is a desktop-only gate, not a deletion from shared code.

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
| Media | Media3/ExoPlayer (Jellyfin, video wallpaper) | libmpv rendered into a Compose surface (video wallpaper only; Jellyfin removed on desktop) |
| Colour extraction | Palette | Multiplatform port of Palette |
| Background work | WorkManager 2.10 | Interface; WorkManager on Android, persistent coroutine queue on desktop |
| Archives | commons-compress, xz, zstd-jni | Unchanged (plain JVM) |
| Steam | JavaSteam fork + DepotDownloader | Removed on desktop (2026-10-23) |
| Built-in emulation | `libretrodroid` module | Not in this version; external emulators only |
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
│ WorkManager          │          │ libmpv media                                       │
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

**Window.** Undecorated fullscreen by default (the Android app is always fullscreen), with a windowed mode for desktop use. Handheld targets such as Steam Deck Game Mode get the same layout as an Android handheld of that resolution. *Changed 2026-10-23 (Desktop divergences):* first launch is windowed with the system title bar and the dark theme, "Start in full screen" is a setting, and display profiles (Handheld/Desktop/TV) pick the layout.

**Media.** Trailers and video wallpaper use libmpv rendered into a Compose surface, with Argosy's existing player controls on top (Jellyfin playback removed on desktop, 2026-10-23).

**Built-in emulator.** Not in this version (owner decision, 2026-10-23): the earlier plan for a desktop counterpart to `libretrodroid` is dropped, and the desktop launches external emulators only.

## Desktop divergences (owner decision, 2026-10-23)

Where Argosy Desktop deliberately differs from Android. Every item is desktop-only: shared code keeps the Android behaviour, and the desktop turns a feature off through a desktop capability (an `expect`/`actual` flag or a desktop DI binding), so the Android app, its tests and the upstream re-apply script are unaffected. Each divergence is recorded in `DEVLOG.md` when it lands.

### Removed from the desktop app

| Feature | What goes | How |
| --- | --- | --- |
| Steam | Settings → Steam (`SteamSection`), the Steam account login, Steam library import and launch, Steam rows in launch targets | Entry points hidden on desktop; JavaSteam/DepotDownloader stay out of the desktop dependency graph; Steam stubs deleted from `desktopMain` |
| Jellyfin | Settings → Jellyfin (`JellyfinSection`), Media Library and Media Detail, offline media downloads, Storage → Media | Entry points and the media tab hidden; Jellyfin network client not bound on desktop |
| Argosy Social | Settings → Social (`SocialSection`), the Social screens, presence and friends, social events from play sessions | Entry points hidden; the social WebSocket never connects on desktop (now-playing to RomM stays) |
| "Android Settings" | The main Settings row that opens the device's settings (`settings_main_device_title`) | Row hidden on desktop |

### Not in this version

| Feature | Note |
| --- | --- |
| Built-in emulation (desktop libretro frontend) | External emulators only. Built-in emulator settings, core downloads and the first-run cores step are hidden on desktop |
| Netplay | Depends on built-in emulation |
| Dual-screen | Single window |

### Linux replacements for Android-only screens

| Screen | Android | Desktop |
| --- | --- | --- |
| Emulator suggestions | Play Store and APK suggestions per platform | Linux emulators from the emulator compatibility database (per platform: Flathub app id, AppImage or native package, launch support, save paths): "Install" runs `flatpak install --user flathub <id>` on the host with the user's consent, or opens the project page for an AppImage |
| Managed installers | Seeded APK installers (Handheld Keyboard, GameNative, DroidDeck) updated from GitHub releases | Linux-only, driven by the emulator compatibility database (owner decision 2026-10-23): AppImage emulators updated from their releases into `~/Applications` (marked executable, picked up by detection) and Flathub emulators shown with their installed/updatable state; the Android seeds never appear on desktop. The initial emulator list and the execution are decided in Phase 4 |
| GPU drivers | Downloads and selects custom Android GPU drivers (Turnip etc.) | Read-only host GPU page: GPU name and vendor, kernel driver, Mesa/NVIDIA driver version, VRAM, OpenGL version and renderer, Vulkan API version, device type and key features, and the display(s) in use. Sources: `/sys/class/drm`, the app's own GL context, and `vulkaninfo --summary`/`glxinfo -B` on the host when present |
| Permissions | Android runtime permissions (storage, usage stats, notifications, write settings, screen capture, overlay, live wallpaper) | Flatpak permissions, read live from the sandbox's `/.flatpak-info` (including user overrides). Each row names the Flatpak permission, shows granted or missing, explains what it is for in Flatpak terms, and offers the fix (a consented `flatpak override --user`, or the exact command / Flatseal) |

Android → Flatpak permission mapping:

| Android permission | Flatpak equivalent | Desktop row |
| --- | --- | --- |
| All files access (storage) | `--filesystem=~/Games/Argosy:create` plus each emulator save folder | ROM library folder; emulator save folders (one row per grant) |
| Usage stats (play-session detection) | `--talk-name=org.freedesktop.Flatpak` (`flatpak-spawn --host` supervises the emulator process) | Launch and supervise host emulators |
| Notifications | Notification portal (`org.freedesktop.portal.Notification`) | Notifications (portal; always available) |
| Write system settings (brightness) | System bus `org.freedesktop.login1` (`SetBrightness`) and `/sys/class/backlight` | Screen brightness (only with a backlight) |
| Screen capture | Screenshot / ScreenCast portals | Screenshots (portal) |
| Display over other apps | none (no overlay concept) | not shown |
| Live wallpaper | none | not shown |
| (no Android row) | `--device=all` (gamepads), `--share=network`, `--socket=pulseaudio`, `--device=dri`, `--socket=wayland`/`x11` | Controllers, network, audio, GPU, display: shown for completeness |
| (no Android row) | Steam client access for native Steam Input (`--filesystem=~/.steam:ro` and the Steam data folder, `--device=shm`), only if the native revisit runs it in the sandbox | "Steam Input (native)": shown only once that revisit lands, and only while Steam Input is available |

The screen lists every permission the manifest requires (from `/.flatpak-info`, so user overrides and revocations show), each with its Flatpak name, its state and what Argosy uses it for, plus the optional grants Argosy can request (emulator save folders, Steam client access).

### New desktop features

| Feature | Behaviour |
| --- | --- |
| Window | System title bar (decorated window) in windowed mode. First launch opens windowed and in the dark theme; later launches restore the user's choice |
| Start in full screen | New Interface setting, off by default; consumed by `desktopApp` `Main.kt` at window creation. `--windowed`/`--fullscreen` on the command line override it; F11 toggles at runtime. Under gamescope (SteamOS/Bazzite Game Mode) the window is always full screen |
| Controller input mode | Navigation → Controller → "Input mode": **Auto**, **Steam Input**, **SDL3** (owner decision 2026-10-23; Phase 4 builds it on SDL3 only). **SDL3** reads pads directly through SDL3, including the Steam Deck's and Steam Controller's own controls (back buttons, gyro, trackpads) via SDL's HIDAPI drivers; always offered. **Steam Input** is listed only while it is available, i.e. a Steam Input virtual gamepad is connected (Argosy started from Steam with Steam Input on); it reads that virtual pad through SDL3 with the user's Steam layout applied and ignores the raw device behind it so input is not doubled. **Auto** (default) picks Steam Input when available, else SDL3. Input goes through one controller-source interface so the native layer below can be added later without touching the SDL3 path. Native `ISteamInput` is a planned later revisit (next table) |
| Button mapping | Navigation → "Button mapping" sub-section: one row per navigation action (Confirm, Back, Context, Secondary, Menu, Select, L1/R1, L2/R2, L3/R3, D-pad directions), each remappable to any button of the active controller, stored per controller (SDL GUID), with "Reset to default". The existing Swap A/B, X/Y and Start/Select toggles become presets over the same mapping. Keyboard bindings get the same list |

### Native Steam Input: planned revisit (after 1.0)

Not part of Phase 4. Adds Steam's glyphs, action sets and Steam's controller configuration screen on top of the Steam Input mode; SDL3 keeps doing the input.

| Piece | Plan |
| --- | --- |
| Loading | Load-at-runtime workaround: Argosy never bundles `libsteam_api.so`; when Steam launched Argosy under its app ID (`SteamAppId` set) and the library is present on the system, Argosy loads it at runtime and maps each pad with SDL3's `SDL_GetGamepadSteamHandle`. Any failure (no library, `SteamAPI_Init` false) leaves input on SDL3 without a message |
| Where it runs | Decided by a spike: in the sandbox (needs the permissions below), or a small helper started on the host with `flatpak-spawn --host` that talks to Argosy over a socket (needs no new permission; the separate-process shape RetroArch's Steam build uses for Steamworks) |
| Permissions it may need | Read access to the Steam client (`--filesystem=~/.steam:ro`, `~/.local/share/Steam:ro`, or `~/.var/app/com.valvesoftware.Steam/.local/share/Steam:ro` for Flatpak Steam) and host shared memory (`--device=shm`, to be verified against `--device=all`). These are added to the manifest only if the in-sandbox route wins |
| Licence | Runtime loading of a library the user already has, never distributed with Argosy; the owner accepts the GPL-3.0 question (2026-10-23) |

**Resolving blocker 1, the Steam app ID** (`SteamAPI_Init` only succeeds for an app Steam launched under a registered app ID; a non-Steam shortcut has none). Steps, owner-driven where marked:

1. **Upstream and naming (owner):** agree a Steam listing with rommapp, since it presents their product; settle the Steam name and icon (the plan's existing "Name and app ID" decision).
2. **Valve rules check:** confirm an emulator frontend that ships no games and no BIOS fits Steam's content rules (precedents: RetroArch), and word the store page as a launcher for the user's own RomM library.
3. **Steamworks account (owner):** join the Steamworks partner programme (identity, tax and bank details are entered by the owner only) and pay the Steam Direct fee ($100 per app).
4. **Create the app:** receive the app ID; set type, Linux as the platform, and launch options.
5. **Steam build:** package a Linux build for the Steam Linux Runtime (the Flatpak cannot be uploaded as is); upload with SteamPipe to a private branch.
6. **Steam Input defaults:** publish an official controller configuration for the app so Steam Input has sane defaults for Argosy's actions.
7. **Native layer:** implement the loading above; test on Steam Deck/SteamOS and Bazzite Game Mode with the private branch (the app ID is now real); confirm the Flatpak build also initialises when launched by Steam with `SteamAppId` set for an owner of the app, or document that native Steam Input is a Steam-build feature.
8. **Store review and release (owner):** store page, Valve's build and store review, then release; the Flatpak and Flathub builds keep working without Steam.

### Display profiles and wizard display setup (owner decision, 2026-10-23)

Chosen from the ten proposed options: 1, 2, 5 and 9, plus a new first-run step. Not chosen for this version: interface scale, text size, grid density, focus visibility, navigation speed and handheld performance options.

| Piece | Behaviour |
| --- | --- |
| Display profile (option 1) | **Handheld** (7–8.8" built-in screens, 800p–1600p: Steam Deck, ROG Ally, Legion Go and similar), **Desktop** (monitor, mouse and keyboard nearby), **TV** (10-foot, controller only). Auto-detected from the DMI product name, a gamescope session (SteamOS/Bazzite Game Mode) and the display's physical size from EDID; changeable later in Settings → Interface → Display profile. Stored as a desktop preference through the full settings chain (DataStore key → prefs → UserPreferences → SettingsModels → hydrate → router → section → consumption) |
| TV layout reuse (option 2) | The TV profile maps the window to upstream's existing TV `AspectRatioClass`, so large screens get the controller-only layout Android TV already has (every action reachable without touch) instead of a new layout. Handheld and Desktop keep the handheld class chosen by window size, as today |
| TV safe area (option 5) | An overscan margin of 0–5 % around the whole UI, on by default for TV (proposed default 3 %, adjustable in Interface) and 0 % for the other profiles, so edges are not cut off on TVs |
| Touch on handhelds (option 9) | Handheld keeps touch targets active on touchscreen devices (Deck, Ally) and hides the mouse cursor while a controller is in use; it reappears on mouse movement. Desktop shows the cursor; TV hides it unless the mouse moves |
| Wizard: Display setup step | A new first-run step, first after the welcome screen (so every later step already uses the chosen layout): three cards, Handheld / Desktop / TV, each with a thumbnail preview of the Home screen in that profile, the detected profile preselected and tagged "Recommended", one line on what the profile changes. Controller (left/right, A to choose) and touch/mouse both work; sound feedback and footer hints as on the other wizard steps. Thumbnails are live miniatures rendered from the Home composable with sample cards and the current theme (so they follow theme and language), at the profile's layout class and safe area, scaled down. Choosing a profile applies it immediately; Back returns to the welcome screen. Desktop divergence: Android's wizard has no such step |
| Thumbnails elsewhere | Settings → Interface → Display profile reuses the same three preview cards |

## Feature parity matrix

Port means the shared code runs as is, Adapt means the same screen and behaviour sit on a Linux implementation, Linux means the screen is replaced by a Linux one, Remove means the feature is gone from the desktop app, Later means not in this version, and the two Defer items need a sandbox permission or a fact-finding step first (owner decisions of 2026-10-23: see Desktop divergences).

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
| Now-playing to RomM | Port | 3 | Fired on launch and process exit |
| Argosy Social | Remove | 4 | Desktop divergence |
| First-run wizard | Adapt | 3 | Same screens plus an emulator folder-access step |
| Quick Settings (R3) | Adapt | 3 | Brightness and volume via the host; performance and fan on supported Linux handhelds |
| Haptics | Adapt | 3 | SDL controller rumble |
| Emulator detection, assignment, autoconfig, RetroArch cores, multi-disc | Adapt | 3 | Linux registry: Flatpak, native and AppImage emulators |
| Emulator suggestions, managed installers | Linux | 4 | Flathub and AppImage instead of Play Store and APKs |
| GPU drivers screen | Linux | 4 | Host GPU and API capabilities, read-only |
| Permissions screen | Linux | 4 | Flatpak permissions and their live state |
| Android Settings row | Remove | 4 | Desktop divergence |
| Window, full-screen setting, controller input mode, button mapping, display profiles | Linux | 4 | New desktop features |
| First-run wizard: Display setup step | Linux | 4 | Profile choice with thumbnail previews |
| Apps tab | Adapt | 3 | Host .desktop apps, launched on the host |
| Jellyfin media with offline downloads | Remove | 4 | Desktop divergence |
| Video wallpaper | Adapt | 4 | Same libmpv surface |
| Built-in emulation and netplay | Later | — | Not in this version |
| Steam integration | Remove | 4 | Desktop divergence |
| Dual-screen | Later | — | Not in this version |
| In-app updater | Adapt | 5 | Same About screen; Flatpak performs the update |
| QuayPass (BLE) | Defer | 1.1 | Needs Bluetooth access through BlueZ; decide after 1.0 |
| Camera features (QR scanner) | Defer | 1.1 | Webcam QR scanning; manual code entry covers 1.0 |

## Flatpak design

The package is one Flatpak holding a trimmed Java runtime, Argosy Desktop and its native helpers (SDL3, libmpv); it launches external emulators on the host through `flatpak-spawn --host`, the only supported way for one Flatpak to start another.

### Manifest basics

| Item | Choice |
| --- | --- |
| App ID | `io.github.<your-account>.ArgosyDesktop` (must match a GitHub account or domain you control) |
| Runtime | `org.freedesktop.Platform` + `org.freedesktop.Sdk`, current branch |
| SDK extensions | `openjdk` for the build and the bundled runtime |
| Java runtime | `jlink`-trimmed JRE in `/app/jre`; JetBrains Runtime later for native Wayland |
| Bundled modules | SDL3 and libmpv if the runtime lacks them |
| Libretro cores | None shipped or downloaded (no built-in emulation in this version); host RetroArch uses its own cores |
| Architectures | x86_64 and aarch64 |
| Updates | Flatpak updates the app; About screen keeps its look |

### Permissions (finish-args)

| Permission | Why |
| --- | --- |
| `--share=network` | RomM, artwork |
| `--share=ipc`, `--socket=wayland`, `--socket=x11` | Display; Java's desktop toolkit runs on X11 or XWayland until native Wayland lands |
| `--device=dri` | GPU acceleration |
| `--device=all` | Gamepads in `/dev/input`; switch to `--device=input` once Flathub builders and target distros all run Flatpak 1.16+ |
| `--socket=pulseaudio` | UI sounds, BGM, video |
| `--filesystem=~/Games/Argosy:create` | Shared ROM and BIOS library at a host path both Argosy and emulators can see |
| `--talk-name=org.freedesktop.Flatpak` | `flatpak-spawn --host` to start emulators and list installed Flatpaks |
| Emulator save folders (`~/Emulation`, each mapped emulator's `~/.var/app/<id>`, its native `xdg-config`/`xdg-data` folder) | Save sync reads and writes saves and states where the emulators keep them (alpha, Phase 3 step 10); Flathub needs an exception or a portal-free alternative (Phase 5) |

No `--filesystem=home`; the emulator save folders are granted one by one. Secrets use libsecret, which falls back to the Secret portal inside the sandbox. Database, settings and cache live under `~/.var/app/<app-id>/`.

### Launching emulators from the sandbox

- **Host-visible paths only.** Every ROM path passed to an emulator must exist on the host, so ROMs never live under `~/.var` or a document-portal path. That is why the library uses a static filesystem grant instead of the file chooser.
- **Three launch strategies** behind one `LaunchStrategy` interface: Flatpak emulator (`flatpak-spawn --host flatpak run <id> <args>`), native binary on the host PATH, and AppImage.
- **Emulator Flatpaks need the ROM folder too.** The first-run wizard detects each installed emulator Flatpak and, with consent, runs `flatpak override --user --filesystem=~/Games/Argosy <emulator-id>` on the host.
- **Supervision.** `flatpak-spawn` stays attached until the host process exits, which gives play-session start and end for now-playing reports and save sync.
- **Discovery** runs `flatpak list --app` on the host, checks PATH, `~/Applications` AppImages, and RetroDECK/EmuDeck install folders against a Linux emulator registry (ID, launch template, supported platforms, save path).

### Offline build (Flathub requirement)

Flathub builds without network, so every dependency is pre-declared as a source:

- Gradle: generate `gradle-sources.json` with [jwharm/flatpak-gradle-generator](https://github.com/jwharm/flatpak-gradle-generator) or the [meshtastic gradle-flatpak-sources](https://plugins.gradle.org/plugin/org.meshtastic.flatpak.sources) plugin, then build with `--offline` against the local Maven folder.
- Native modules (SDL3, libmpv): pinned source archives or git commits with checksums in the manifest.
- Regenerate the Gradle sources in CI on every dependency change; a stale sources file is the most common Flathub build failure.

### Store listing

AppStream metainfo with controller-first screenshots, OARS rating, release notes per version and a `launchable`; a .desktop file with the `Game` category; icons at 128, 256 and scalable SVG. Validate with `flatpak-builder-lint` before every submission.

Sources: [Flathub forum on launching Flatpaks from Flatpaks](https://discourse.flathub.org/t/launching-flatpak-inside-flatpak/5708), [Flathub forum on device=input](https://discourse.flathub.org/t/support-for-device-input/6645).

## Roadmap

Six phases over about 32 weeks; restructure first, then get every screen on the desktop, then replace Android services one by one, so a playable alpha exists at week 19. Phase 4 was re-scoped on 2026-10-23 (Desktop divergences).

| Phase | Weeks | Scope | Gate after |
| --- | --- | --- | --- |
| 0 · Discovery and spikes | 1–2 | Library screen in a desktop window, sandbox launch, Room KMP; upstream conversation started | All three spikes pass |
| 1 · Kotlin Multiplatform restructure | 3–8 | Shared modules, DI, Room and DataStore KMP, shared resources; Android app unchanged for users | — |
| 2 · Desktop UI | 9–13 | Every screen runs on the desktop; SDL3 gamepad input; identical focus feel | — |
| 3 · Linux services and alpha Flatpak | 14–19 | Emulator registry, host launch, save paths, Apps tab, Quick Settings; Flatpak from your own repo | Alpha: sync, download, play, save sync |
| 4 · Linux adaptation | 20–28 | libmpv video wallpaper; Desktop removals (Steam / Jellyfin / Social / Android Settings); Linux emulator suggestions and managed installers; Host GPU screen; Flatpak permissions screen; Window title bar and full-screen setting; Controller input mode (Auto / Steam Input / SDL3); Navigation button mapping; Display profiles and wizard display setup | — |
| 5 · Beta and Flathub | 29–32 | Side-by-side checks against Android; GNOME, KDE and SteamOS testing; Flathub submission | 1.0 live on Flathub |

Exit criteria per phase:

1. **Phase 0:** upstream's Library screen renders in a desktop window; a sandboxed test Flatpak starts the RetroArch Flatpak on a ROM; upstream's schema opens under Room KMP on the JVM.
2. **Phase 1:** `shared` builds for Android and desktop with no Android imports in `commonMain`; the Android app passes its existing tests and looks unchanged.
3. **Phase 2:** every screen opens on the desktop and is fully navigable by gamepad; screenshots match an Android handheld at the same resolution.
4. **Phase 3:** emulators detected on a clean Fedora and a SteamOS install; a game launches, exits and returns to Argosy; saves sync to RomM; alpha installs from your own Flatpak repo.
5. **Phase 4:** every Port, Adapt and Linux row of the parity matrix works on the desktop; every Remove row has no entry point left on the desktop and the Android app is unchanged (A/B screenshots and unit tests); the display profiles and the wizard's Display setup step work on a handheld, a desktop monitor and a TV-sized screen.
6. **Phase 5:** testers using both apps cannot name a behavioural difference outside the listed desktop divergences; `flatpak-builder-lint` clean; offline build green in CI for x86_64 and aarch64; Flathub PR merged.

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
| Native Steam Input through the Steamworks SDK | `libsteam_api` is proprietary; `SteamAPI_Init` needs a Steam app ID; in the sandbox it may need Steam client and shared-memory access | Phase 4 ships SDL3 input only; the native layer is a later revisit: never bundled, loaded at runtime if present (owner accepted the GPL-3.0 question), app ID through the Steam listing steps |
| Desktop-only gates drift from upstream | A removed feature reappears after an upstream re-apply | Gates live in desktop source sets and a desktop capability; the re-apply check lists new Steam/Jellyfin/Social entry points |
| Java toolkit on XWayland (fractional scaling, frame pacing) | Blurry or stuttery UI on Wayland desktops | Test GNOME and KDE at 125% and 150%; move to JetBrains Runtime's native Wayland toolkit if needed |
| `--device=all` flagged potentially unsafe | Lower trust on the store page | Move to `--device=input` once Flatpak 1.16+ is universal |
| Naming, icon and GPL-3.0 obligations | Confusion with upstream; legal exposure | Agree name and icon with rommapp; whole port GPL-3.0 with a source link in About |

## Open decisions and next steps

Four decisions shape everything after Phase 0; settle them in the first two weeks.

- **Upstream or fork.** Contribute the KMP restructure to rommapp so both apps share one codebase (recommended), or keep a fork with a re-apply script.
- **Name and app ID.** "Argosy Desktop" with upstream's blessing, under rommapp or your own GitHub account.
- **Java runtime.** Stock OpenJDK on XWayland for 1.0, or JetBrains Runtime with native Wayland from the start.
- **Built-in emulation timing.** Decided 2026-10-23: not in this version; external emulators only.
- **Steam Input approach.** Decided 2026-10-23: Phase 4 builds input mode Auto / Steam Input / SDL3 on SDL3 (Steam Input listed only when available). Native `ISteamInput` is a planned revisit after 1.0 with the load-at-runtime workaround, gated on a Steam app ID (steps under "Native Steam Input: planned revisit").
- **Handheld/TV options.** Decided 2026-10-23: display profiles (Handheld/Desktop/TV), TV layout reuse, TV safe area, touch on handhelds, and a Display setup step with thumbnail previews in the first-run wizard.
- **Emulator catalogue and managed installers.** Decided 2026-10-23: a database approach for emulator compatibility (a data-driven Linux emulator catalogue that emulator suggestions, managed installers and detection read from, instead of hard-coded seeds). Which emulators it starts with, its schema, where it lives and how it is updated are decided while planning Phase 4.

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
