# Phase 3 · Linux services and alpha Flatpak — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone.
History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases:
`phase-0.md`, `phase-1.md`, `phase-2.md` (closed summaries).

**Updated:** 2026-10-23. Steps 1–5 done (commits "Phase 3 step 1" … "Phase 3 step 5 (part 1)"; stage 4 was already in commonMain). The Phase 3 step plan (steps 1–11) is in `DEVLOG.md` under "Phase 3 · Linux services and alpha Flatpak" → "Phase 3 step plan". Step 6 parts 0 (re-pair, background jobs), 1 (file accessor, RetroArch config) and 2 (session-end save sync, SQLite driver 2.6.2) committed. Nothing uncommitted. Next action: step 6 part 3 (desktop save-write watcher for the 30 s rule, then a second device's save coming down).

## Scope and exit criterion (`PORT_PLAN.md`, Roadmap)

Emulator registry, host launch, save paths, Apps tab, Quick Settings; Flatpak from the owner's own repo. Exit: emulators detected on a clean Fedora and a SteamOS install; a game launches, exits and returns to Argosy; saves sync to RomM; the alpha installs from the own Flatpak repo. Save sync is a fragile zone: live-data proof (GET /api/saves, negotiate `no_op` twice) for every door.

## Done: steps 1–5 (detection, host launch, play sessions, save-zone inventory, background jobs)

- Detection: `shared/src/desktopMain/.../data/emulator/LinuxEmulators.kt` (table keyed by upstream `EmulatorDef.id`, Flatpak > AppImage > EmuDeck folder > PATH), `platform/HostShell.kt` (`flatpak-spawn --host` when sandboxed). `EmulatorDetector.linuxInstall(packageName)`. No built-in emulator on desktop (Phase 4). RetroDECK not mapped.
- Launch: `GameLauncher.desktop.kt` → `LinuxLaunch.kt` argv (RetroArch `-L <core> <rom>`, Dolphin `-b -e`, PPSSPP, Cemu `-g`, DOSBox-X) in an intent extra → `GameActivityStarter.desktop.kt` → `platform/HostGameProcess.kt` (one supervised process, `running` StateFlow, log `~/.local/share/argosy-desktop/logs/emulator.log`). `MultiResumeLifecycle` holds the window at CREATED while it runs.
- Not launched yet (return `LaunchResult.Error`): DuckStation, PCSX2, RPCS3, Azahar (help aborts headless: verify their CLI with a display), Vita3K (title id), ScummVM (game id), dosbox-staging; variants, multi-disc, Steam, GameNative, Android apps.
- Walk: `pad-walk.sh desktop <dir> wait:5 X wait:4 A wait:12 ... "RUN:pkill -x retroarch" wait:6 shot:returned` (Home → Detail → Play; evidence in `build/desktop-check/p3s2-c/`).
- Play sessions: desktop `PlaySessionTracker.desktop.kt` records the play session row, play time, RA recap facts and RomM "incomplete" (queued; `RomMConnectionManager` drains it on connect) as Android does; `HostSessionWatcher` ends the session via `SessionEndCoordinator.endClosedSession()` when `HostGameProcess` exits. Desktop differences: active play = whole session, no social events, an orphaned session is cleared without play time. Evidence `build/desktop-check/p3s3/` (`db.py`, `romm.sh`, before/after).

- Step 4 (no code change; DEVLOG "Step 4"): Phase 2's rehome passes already moved stages 2 and 4 of the save zone into `commonMain`; the `PlatformContext` stand-ins (`filesDir`, `cacheDir`, `packageManager`) are the `FilePaths` seam (Android: `Context`'s own members; desktop: `DesktopDirectories`, XDG). No separate `FilePaths` interface. **Owner approved (2026-10-22) hand edits inside the save zone for stages 2–4, each as its own change with its own live save proof** (`save-proof.sh`, GET /api/saves, negotiate `no_op` twice).

- Step 5: `SaveSyncWorker.doWork` → commonMain `SaveSyncPass` (worker maps outcome to `Result`); `SaveSyncDownloadObserver` → commonMain. Desktop `data/sync/DesktopBackgroundJobs` (started in `Main.kt`): download observer, `SaveSyncPass` every 6 h, `processQueue()` every 15 min, RA patch backfill (desktop `RAPatchBackfillWorker` actual calls it). Each job waits for a RomM connection. Android-only by reason: foreground service/notification trio, `SocialSyncWorker` (social), `AchievementSubmissionWorker` (built-in libretro, Phase 4). Save proof `save-proof/p3s5` identical to baseline. Desktop pass against the connected testbed proven in step 6 part 0 (`build/desktop-check/p3s6-jobs/`: negotiate 200, reconcile, queue drain).

## Next action: step 6 part 3

- The desktop is paired and connected, **Save Sync is on** in its Settings → Saves (Secure Saves on, the default). After any `save-proof.sh` (its snapshot restore drops the desktop's token and device row) run `scripts/desktop/testbed/desktop-repair.sh [out-dir]` and check `connect: success` in the newest `build/desktop-logs/*-desktop-run.log`; the restore may also reset Save Sync (re-enable: drawer START, DOWN×8, A, DOWN×6, A, A).
- `pair-code.py` runs only under `build/desktop-testbed/venv/bin/python`. The pairing form connects via Down → Connect → South, not Enter.
- Part 3, in order: (1) a desktop save-write watcher feeding `PlaySessionTracker.reportSaveWritten()` (Android: `GameSessionService` file watcher), because `SessionEndCoordinator` cancels sessions under 30 s with no observed write; prove a <30 s session that saves still uploads. (2) A second device's save comes down: play Battery Bay on the Android emulator (`emulator-5556`, built-in mGBA), then launch it on the desktop → pre-launch download places the newer save. (3) Check the orphan-cache upload naming (`autosave [time].srm` for Save Point Harbor, server save 5) against Android's same path.
- Proof recipe (evidence `build/desktop-check/p3s6-p2/`, DEVLOG "Step 6, part 2"): Home → Game Boy tab is three left-trigger presses (`PAD:LEFT_TRIGGER=32767 wait:0.5 PAD:LEFT_TRIGGER=0`; L1 opens Downloads instead); the GB row is ordered by recent play (now Battery Bay, Save Point Harbor, Argosy Test Quest, Ledger Isle, …): take a `shot:` before choosing RIGHT×n. Play = A, then `wait:40` (over 30 s), `RUN:pkill -x retroarch`, `wait:20`. Server saves: `build/desktop-check/p3s6-p2/saves.sh`. Negotiate twice: drawer START, DOWN×6, A (Save Sync), Y (Scan RomM), wait, Y again; startup reconcile is under a 5 min negotiate cooldown.
- RetroArch here auto-loads `.state.auto`, so Save Point Harbor's SRAM no longer changes per session (use Battery Bay for an upload, or a game without an auto state).

## Step 6 notes (desktop save sync)

- Done: part 1 (file accessor, `RetroArchHostLayout`; desktop reads `~/.var/app/org.libretro.RetroArch/config/retroarch/retroarch.cfg`, `savefile_directory` = the owner's EmuDeck `~/Emulation/saves/retroarch/saves`). Part 2: session-end and orphan save/state sync through `SessionSaveFinalizer`, orphan check at launch (`HostSessionWatcher`), self-creating `filesDir`/`cacheDir`, `sqlite-bundled` 2.6.2 (2.5.x threw `SQLiteException: Error code: ` with no text in the app, which breaks every Room `@Upsert` onto an existing row; desktop-only dependency).
- **Real owner data folders.** Testbed artifacts now there (leave them, never delete): `saves/{Ledger Isle,Save Point Harbor,Battery Bay} (Homebrew).srm`, `states/{Argosy Color Trial,Save Point Harbor,Battery Bay} (Homebrew).state.auto(.png)` under `~/Emulation/saves/retroarch/`.
- Still open: `SaveStateManager.desktop` paths are `TODO`s (built-in libretro, Phase 4). Android storage assumptions (`/storage/emulated`, `Environment`) in `SavePathResolver`, `SavePathRegistry` (standalone `{extStorage}` templates), `FolderSaveHandler`, `SigilSaveHandler`, `N3dsSaveCaseRepair`; desktop `getExternalStorageDirectory()` is `$HOME`. Standalone emulators (Dolphin, PPSSPP, …) need Linux save-path templates.
- Also step 6: the Save Timeline comparison (Manage Saves; Save Sync is now on), the Save Path row showing Android's `Internal/RetroArch/saves`, and re-check the bare "Error code:" line after pairing (likely the driver bug, fixed by 2.6.2).
- RA login on desktop writes cheevos credentials into the host RetroArch cfg (Android parity; other keys kept).
- ROMs live under `~/.local/share/argosy-desktop/external/downloads` (inside the Flatpak `~/.var/app/<id>/data/...`); step 9 moves them to `~/Games/Argosy`.

## Owner checkpoints already known (ask when reached, one question each)

- Step 10: where the own Flatpak repo is hosted. Step 11: which Fedora/SteamOS machines or VMs.

## Carried forward

- From Phase 1: save-sync stages 2–4; Flatpak runtime `org.freedesktop.Platform` 25.08; when a move takes a generated file, update its generator in the same step (Node is not installed, so `scripts/gen-tokens.mjs` has not been run).
- Read `~/.config/user-dirs.dirs` for XDG Music/Download. `OpenDocument` (Backdrop custom image, Fonts import) waits for the XDG FileChooser portal. `withUserCertTrust` asks for `AndroidCAStore` (absent on a JVM): an imported cert would replace system trust; desktop never calls `userCertStore.initialize()`.
- While the window is hidden, vsync blocks the AWT thread about 1 s per frame, and SDL pad events are posted to that thread.
- Unverified from Phase 2: a physical pad (Nintendo layouts), stick navigation, key-repeat feel.
- Phase 4 parity questions: "Android Settings" on Linux (open the desktop's settings app, or hide the row); About's "Stop Crashes on Heavy Emulators" (writes an Android root script). Screens not yet opened: Media Library/Detail (needs Jellyfin in the testbed: ask the owner), the Social family (needs an Argosy Social session; maintainer domain).

## Where things are

- **Baseline for "Android unchanged":** plain upstream v2.24.0, `build/desktop-baseline/app-debug-upstream-v2.24.0.apk`. Plain save proof: `build/desktop-check/save-proof/v224-plain-1022a` (recapture once a candidate is more than 24 hours newer: `save-proof.sh build/desktop-baseline/app-debug-upstream-v2.24.0.apk <dir>`, about 8 min).
- Desktop graph: `shared/src/desktopMain/.../di/` (`DesktopComponent`, `DesktopAppModule` hand-owned bindings, `DesktopDataModule`, `DesktopViewModelComponent`, `DesktopApplication`, `ViewModels.desktop.kt`); generated `DesktopDaoModule` and `DesktopViewModelModule`. A new Android `@Binds`/`@Provides` that commonMain injects needs a desktop counterpart; `scripts/desktop/restructure/di_closure.py <Root>...` lists the gaps.
- Desktop stubs: 40 `TODO(...)` in desktopMain (`grep -rn 'TODO(' shared/src/desktopMain`), each throws when reached. Others compose nothing or report "unsupported": `ClipboardTextReader`, QR scanner, media players, `YouTubeVideoPlayer`, `Box3dCover`, `OpenDocument`, `getLaunchIntentForPackage`, Steam and shader-chain stubs, `AmbientLedManager`, `DesktopLEDController`, `DesktopFileAccessLayer` (plain `java.io`).
- Desktop state persists in `~/.local/share/argosy-desktop` (never delete it without the owner).

## Commands and tools

- Desktop run + screenshot: `scripts/desktop/check/desktop-shot.sh`; driven run `desktop-drive.sh`; pad walks on both sides `pad-walk.sh android|desktop <dir> <steps>`.
- Verification: `scripts/desktop/check/verify.sh`, `ab-check.sh`, `save-proof.sh`; testbed `scripts/desktop/testbed/` and `docs/desktop/testbed-runbook.md`.
- Quiet runner: `scripts/desktop/quiet.sh <label> <command...>`. JDK: `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.
- Screenshots: window captures only (`shot:`). Never full-screen (`spectacle -f`, `import -window root`): this host's other monitors show the owner's private apps. `pkill -f <pattern>` inside a walk step kills the walk itself (its argv contains the pattern): use `pkill -x <name>`.
- Gotchas (restructure scripts, DI, desktop runtime, input injection, walk recipes): `docs/desktop/gotchas.md`. Read the relevant part before touching that area.
