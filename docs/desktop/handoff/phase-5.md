# Phase 5 · Beta and Flathub — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`. Earlier phases: `phase-0.md` … `phase-4.md` (closed summaries).

**Updated:** 2026-10-27 (step 7 groundwork unit 4 committed; step 5 open, waiting for the owner's checklist). Steps 1–4 done; step 5 in progress.

## Scope and exit criterion

`PORT_PLAN.md` Roadmap (Phase 5): side-by-side checks against Android; GNOME, KDE and SteamOS testing; Flathub submission. Exit: testers using both apps cannot name a behavioural difference outside the listed desktop divergences; `flatpak-builder-lint` clean; offline build green in CI for x86_64 and aarch64; Flathub PR merged.

## Carried forward

- Owner test results for `docs/desktop/owner-test-phase-4.md` (fix any failures first).
- Phase 3 items: download stall with a ROM already in the target folder; standalone-emulator save proof; "Scan for Android games" rows (owner questions).
- Desktop leftovers: Displays → "Enable Dual-screen Mode", About → upstream APK update (in-app updater row: Flatpak performs the update).
- Flathub lint: exceptions or alternatives for emulator folder grants and `flatpak-spawn` access, screenshots, the app id vs repository name; offline from-source build (Gradle dependencies as Flatpak sources).
- YouTube trailers on desktop (deferred; needs yt-dlp and a JavaScript runtime).

## Owner checks pending (alpha 5.4)

- Real RomM covers on Home and Library (step 3 was verified with injected rows).
- Steam Game Mode: launch a GameCube/Switch/PS game and a GBA game; exit Argosy from the Steam overlay; type a collection name with Steam's on-screen keyboard (Steam + X). If anything fails, send `argosy-run.log` (Flatpak: under `~/.var/app/io.github.stealthx3.ArgosyDesktop`). Results feed step 6 (SteamOS testing).

## Step 4 (Steam Game Mode): done

- `libsigil-jni`: `:desktopApp:sigilJni` (host, dev runs, via a JVM argument provider on `run`) and the Flatpak `sigil-jni` module (built in the SDK; `build-alpha.sh` stages JDK headers into `desktopApp/build/jni-include`). The Flathub offline build will need the openjdk SDK extension in place of those staged headers.
- Shutdown hook (`Main.kt` `shutDown`): `DesktopGamepadDispatcher.stop()` plus a 3 s halt watchdog. SIGTERM exits in about 0.4 s.

## Step 5 (side-by-side): in progress

- Done: tester sheet `docs/desktop/side-by-side-checklist.md`; `scripts/desktop/check/side-by-side.sh <out> <fresh-name>` (AVD + fresh desktop data, tiles `pairs-*.png`); first pass in `build/desktop-check/p5s5/`; dual-screen row and subtitle gated off on desktop (`PlatformFeatures.dualScreen`).
- Owner question: Quick Settings Brightness is hidden on desktop (`screenBrightnessAvailable`). Should it be an approved divergence, or should desktop get backlight control?
- Unit 2: the pass now covers game details and the Theme, Interface, Navigation, Audio and Displays sections (`build/desktop-check/p5s5b/`); no new real differences.
- Unit 3: `settings2` plan covers the remaining sections (`build/desktop-check/p5s5c/`). Fixed: About's APK updater and System App section are off on desktop (`selfUpdate`, `systemApp`); the Check row says Flatpak updates the app.
- Unit 4: Collections create matches (`build/desktop-check/p5s5d/`). Search has no nav entry of its own. Automated pass complete.
- **Next action:** wait for the owner. Needed: the checklist results, the Brightness decision, and the Game Mode checks. Fix every reported difference, then close step 5 (`--step-close 5.5`). For a fair Home comparison, give the fresh desktop data a played game. Then, from the owner's checklist results, fix every difference outside the divergences. Close step 5 when the checklist comes back clean, and republish (`--step-close 5.5`).

## Step 7 groundwork (owner OK 2026-10-27, step 5 stays open)

- Plan and lint status: `docs/desktop/flathub-plan.md`. Wayland socket dropped (lint error fixed).
- Owner decisions open: the app-id rename `io.github.Stealthx3.argosy_desktop`, installing the openjdk21 SDK extension, screenshots from the owner's library, and the Flathub PR.
- Unit 2: SDK-less desktop build works with `ANDROID_HOME=<empty dir>` and `-Pargosy.desktopOnly=true`, which leaves out `:sigil` and `:libretrodroid` (they want the NDK at configure time). Proven from a clean copy; the default gate is unchanged.
- Unit 3: offline sources done (`generated/gradle-sources.json`, 1,448 files; regenerate with `scripts/desktop/flatpak/regen-gradle-sources.sh <scratch>`). The host `--offline` proof passes. Draft from-source manifest `io.github.stealthx3.ArgosyDesktop.flathub.yml` is not yet run.
- **Next action:** with the owner's OK, install `org.freedesktop.Sdk.Extension.openjdk21//25.08` (user) and run `flatpak-builder` on the `.flathub.yml` into a scratch repo. Fix what breaks (likely: git `branch: HEAD`, submodules, Compose's jlink inside the sandbox), then lint it. Plan step 5 is done as a draft: `.github/workflows/flatpak.yml` (untested; its first run is the phase-close push). Without the extension OK, the remaining groundwork is screenshots, which need the owner's library, so wait for the owner.

## Gotchas

- Walks: Quick Settings menus wrap (Close Argosy is `R3 DOWN*8`). `PAD:RIGHT_SHOULDER` from Home went to Favs on 2026-10-26, not the Library: check with a shot before relying on it. `desktop-drive.sh` ends with `pkill -f` on the main class name, so never put that name in the same Bash command.
- Dev data downloads go to the real `~/Games/Argosy` (a walk's A on Home downloaded `gba/Argosy Advance Demo (Homebrew).gba` there on 2026-10-26).
- Process probes: match the app instance by its unique `--key-pipe=<path>` and skip the `org.gradle` wrapper JVM; never signal by `pgrep -x java` (blocked, and could hit other Java programs).

## Pre-beta fixes (owner, 2026-10-24): steps 1–4 done

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
