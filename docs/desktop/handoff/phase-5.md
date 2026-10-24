# Phase 5 · Beta and Flathub — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`. Earlier phases: `phase-0.md` … `phase-4.md` (closed summaries).

**Updated:** 2026-10-24 (step 3 diagnosed, no code yet). Steps 1–2 done; step 3 in progress.

## Scope and exit criterion

`PORT_PLAN.md` Roadmap (Phase 5): side-by-side checks against Android; GNOME, KDE and SteamOS testing; Flathub submission. Exit: testers using both apps cannot name a behavioural difference outside the listed desktop divergences; `flatpak-builder-lint` clean; offline build green in CI for x86_64 and aarch64; Flathub PR merged.

## Carried forward

- Owner test results for `docs/desktop/owner-test-phase-4.md` (fix any failures first).
- Phase 3 items: download stall with a ROM already in the target folder; standalone-emulator save proof; "Scan for Android games" rows (owner questions).
- Desktop leftovers: Displays → "Enable Dual-screen Mode", About → upstream APK update (in-app updater row: Flatpak performs the update).
- Flathub lint: exceptions or alternatives for emulator folder grants and `flatpak-spawn` access, screenshots, the app id vs repository name; offline from-source build (Gradle dependencies as Flatpak sources).
- YouTube trailers on desktop (deferred; needs yt-dlp and a JavaScript runtime).

## Known issue (for step 4)

- Desktop throws an uncaught `UnsatisfiedLinkError: no sigil-jni` from `TitleIdDownloadObserver` → `TitleIdExtractor` → `Sigil.<clinit>` (no libsigil-jni on desktop): catch it or skip title-id extraction on desktop, or ship the native library.
- Walk gotchas: Quick Settings menus wrap (Close Argosy is `R3 DOWN*8`); `desktop-drive.sh` ends with `pkill -f` on the main class name, so never put that name in the same Bash command.

## Step 3 (thumbnails): diagnosis and next action

- Cause: desktop `ImageCacheManager` (`shared/src/desktopMain/.../data/cache/ImageCacheManager.desktop.kt`) is a full stub: `gameArt` empty, `artFor`/`loadArt`/`observeArt` null, background caching no-ops. Sync already records art URLs in shared code (`ArtSourceRecorder` → `gameArtDao.setSourceUrl`, `RomMUserPropertyService`), and rows resolve as override ?: cachedPath ?: sourceUrl (`GameArtEntity.resolvedPath`). The testbed has no `game_art` rows because its synthetic games have no covers; the owner's library has them.
- **Next action:** in the desktop actual, copy Android's art model (`gameArt = gameArtDao.observeRowCount().conflate().transform { emit(gameArtDao.getAll().toResolvedArtByGame()); delay(ART_MODEL_RELOAD_INTERVAL_MS) }.stateIn(...)`, `artFor = gameArt.value[id]`, `loadArt = gameArtDao.resolved(id)`, `observeArt = gameArt.map { it[id] }.distinctUntilChanged()`); make `queueBackgroundCache(url…)` prefetch through Coil's singleton loader (disk cache) so covers are pulled during sync; check `installDesktopImageLoader` has a disk cache (add one under `DesktopDirectories.cache` if not). Verify with a fresh-walk data dir: either give one testbed ROM a cover in RomM (romm skill / API) and sync, or insert a `game_art` COVER row pointing at a local PNG served by `python3 -m http.server`; Home and platform screens must show it.

## Pre-beta fixes (owner, 2026-10-24): steps 1–2 done, step 3 in progress

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
