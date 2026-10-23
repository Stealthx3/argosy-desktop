# Phase 3 · Linux services and alpha Flatpak — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone.
History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases:
`phase-0.md`, `phase-1.md`, `phase-2.md` (closed summaries).

**Updated:** 2026-10-23 (step 10 part 3: publish tooling, alpha installed over HTTP from the repo). Steps 1–9 done (commits "Phase 3 step 1" … "Phase 3 step 9"; step 6's standalone-emulator proof waits on the owner's answer, below). The Phase 3 step plan (steps 1–11) is in `DEVLOG.md` under "Phase 3 · Linux services and alpha Flatpak" → "Phase 3 step plan". Step 6 history: DEVLOG "Step 6, part 0" … "part 8"; step 7: "Step 7, part 1" … "part 3"; step 8: "Step 8"; step 9: "Step 9, part 1" … "Step 9 closed"; step 10: "Step 10, part 1" … "part 3". Nothing uncommitted. **Waiting on the owner:** step 10's remaining work (publishing) needs the hosting answer, see below; state `waiting-owner`.

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

## Step 10 (alpha Flatpak): in progress

- Done (part 1): `desktopApp/flatpak/` (manifest `io.github.stealthx3.ArgosyDesktop.yml`, wrapper, .desktop, metainfo; icons from `argosy_icon.svg`), jlink modules in `desktopApp/build.gradle.kts`. `scripts/desktop/flatpak/build-alpha.sh [--no-gradle]` (Gradle `createDistributable` → `flatpak run org.flatpak.Builder` into `build/flatpak/repo` → user install → lint), about 2 min; run it through `quiet.sh`. `flatpak-shot.sh <out.png> [settle] [WxH]` runs the installed app and screenshots it. Installed for the user; its data is `~/.var/app/io.github.stealthx3.ArgosyDesktop` (fresh: wizard), separate from the dev run. Lint errors are Flathub-only (DEVLOG "Step 10, part 1").
- Save-folder grants are explicit (`~/Emulation`, each mapped emulator's `~/.var/app/<id>`, native xdg folders); `PORT_PLAN.md` updated. SD-card EmuDeck links stay unreadable (step 11 note).
- Done (part 2, DEVLOG "Step 10, part 2"): the installed Flatpak walked on its fresh sandbox data: wizard to Home (`build/desktop-check/p3s10-wizard/`), Battery Bay downloaded to `~/Games/Argosy/gb`, Play ran host RetroArch via `flatpak-spawn --host`, kill → return, session recorded, state uploaded (`p3s10-play/`); session-end save upload (server save 6, synthetic counter bump, see DEVLOG) and Scan RomM twice `no_op` (`p3s10-save2/`). Drive the installed app with `pad-walk.sh flatpak <dir> ...` (`DRIVE_TARGET=flatpak` for `desktop-drive.sh`); wizard and upload recipes in `docs/desktop/gotchas.md`. The Flatpak's data (`~/.var/app/io.github.stealthx3.ArgosyDesktop`) is now paired (second testbed device), Save Sync on; a relaunch opens Home on Recent.
- Done (part 3, DEVLOG "Step 10, part 3"): `scripts/desktop/flatpak/publish-repo.sh <repo-url> [gpg-key-id]` (static deltas, prune, optional signing; writes `build/flatpak/argosy-desktop.flatpakrepo` and `io.github.stealthx3.ArgosyDesktop.flatpakref`). Proven over local HTTP: install via `remote-add` of the `.flatpakrepo` and via `install --from` the `.flatpakref`; app opened Home with its sandbox data intact (`build/desktop-check/p3s10-repo/`). The app is back on the builder origin `argosydesktop-origin`; test remotes deleted. Gotchas in `docs/desktop/gotchas.md` (last line).
- **Next action (once the owner answers):** create or receive the GPG key (only with the owner's go-ahead), `build-alpha.sh` → `publish-repo.sh <url> <key>`, upload `build/flatpak/repo` plus the two files to the chosen host, then install from the real URL on a clean user (`flatpak install --user --from <url>/…flatpakref`) and record it. If the app id changes, rename the manifest/desktop/metainfo/icon ids and the scripts' `app_id` first. Without an answer, step 11 machines are the other owner question; nothing in Phase 3 proceeds without the owner.
- **Owner questions (asked 2026-10-23, record answers here):** (1) where the own Flatpak repo is hosted (e.g. GitHub Pages of `Stealthx3/argosy-desktop`, or another host); (2) app id: Flathub derives the repo `github.com/stealthx3/argosydesktop` from `io.github.stealthx3.ArgosyDesktop`; keep it (and rename/alias the repo for Flathub later) or switch now to `io.github.stealthx3.argosy_desktop` (a later switch moves the sandbox data dir). Publishing waits on (1); park `waiting-owner` only when nothing else is left.

## Step 9 closed: what carries

- Desktop default ROM folder `~/Games/Argosy` (`defaultRomStorageBase`); wizard order ROMM_SUCCESS → Games Path → Permissions (`EmulatorFolderAccessStep`, `HostFolderAccess`: `flatpak override --user --filesystem=<folder>`) → Image Cache → cores → All Set. `XdgUserDirs` for Music/Download. `PortalFileChooser` (dbus-java 5.1.1) behind `OpenDocument`; private-bus tests: `ARGOSY_PRIVATE_DBUS=1 dbus-run-session -- ./gradlew --no-daemon :shared:desktopTest --tests '*PortalFileChooserTest'`.
- Full wizard to Home on a fresh dir: evidence `build/desktop-check/fresh-e/`. Wizard walks: `fresh-walk.sh <new-name> <steps>` (a reused name relaunches to Home); recipe in DEVLOG "Step 9, part 2" … "closed". After Permissions: DOWN×5, SOUTH; then SOUTH ×4 (Image Cache default, cores prompt, no cores, All Set → Home). **Never press A at focus 0 on the QR screen** (opens the owner's browser). Delete `build/desktop-fresh/<name>` after a walk.
- **Verify in step 11:** a live `flatpak override` grant (every emulator Flatpak here already has `host`/`home`; never add overrides to the owner's apps), a folder other than `~/Games/Argosy` picked in the browser, a completed FileChooser pick, the Flatpak's document-portal path and reading the host `user-dirs.dirs` from inside the sandbox.

## Step 8 closed: what carries

- Desktop volume (`wpctl` > `pactl`, 20 steps), backlight (`/sys/class/backlight` + logind `SetBrightness`), SDL rumble haptics (`GamepadRumble`, `SdlGamepadSource.rumble`). Brightness row hidden without a backlight (`screenBrightnessAvailable`). Performance/fan already device-gated (Phase 4: Deck/Legion TDP and fan).
- **Verify in step 11:** a live logind brightness write on a machine with a backlight (SteamOS Deck or a Fedora laptop); rumble on a physical pad.
- Walk recipe: R3 opens Quick Settings; Volume is DOWN×2; RIGHT/LEFT = one 5 % step. Always read `wpctl get-volume @DEFAULT_AUDIO_SINK@` before and restore it after (owner's desktop). `pad-walk.sh` out dir must be repo-relative (an absolute one hangs the walk).

## Step 7 closed: what carries

- Desktop `data/apps/` (`HostApplications`, `DesktopEntryValues`, `HostIcons`, `HostAppLaunch`), `AppsRepository.desktop`, `HostAppIconFetcher`; Home app tiles launch supervised through `HostGameProcess`; `InstalledAppResolver.desktop`; ad-hoc emulator binding (`HostApplications.commandFor`). `PlayStoreAccess` (desktop `false`) stops every Play Store request; desktop `ImageCacheManager.queueAppIconCache` sets the host icon as COVER (cache `~/.cache/argosy-desktop/icons/`). Every other desktop `ImageCacheManager` member is still a neutral stub (covers load from source URLs).
- Apps tab: drawer START, DOWN×7, A. On Apps: X = Show Hidden, SELECT = Options (DOWN, A = Add to/Remove from Home), A = Open. App picker modal not opened live.
- **Open owner question (record the answer here):** a walk pressed Settings → Platforms → Android → "Scan for Android games", which added 13 of the owner's game-category desktop entries (game rows 18–30: Steam shortcuts, Flatpak games, Wine launchers) to the desktop library. Remove them (Apps tab → Remove from Home), or keep? And should Steam `steam://` entries count as games in that scan? Do not touch rows 18–30 before the answer. (They show no icon: they were added before part 3.)
- Settings → Platforms is drawer START, DOWN×8, A, DOWN×10, A; the Android row's first item (A) is the scan, not an Emulator row.
- Live launch walks: temporary probe entry `~/.local/share/applications/argosy-probe.desktop` named `0 Argosy Probe` (sorts first), removed afterwards; never launch the owner's real apps or Steam games.

## Step 6 closed: what carries

- **Open owner question (asked 2026-10-23; record the answer here):** how to prove a standalone emulator's save/state sync on the desktop: add a free homebrew GC/PSP/PS2 title to the testbed, or use one of the owner's own games. Do not download anything or touch the owner's games before the answer.
- **Parity finding for the owner (part 8, DEVLOG "Step 6, part 8"):** after the server loses a save the client uploaded (testbed snapshot restore, or a delete in RomM's web UI), upstream's `SaveUploader` skips the planned UPLOAD as "content unchanged" until the save changes; every pass re-plans it. Same code as upstream v2.24.0; no app change. Possible upstream report, owner's call. Testbed effect: desktop Save Point Harbor (rom 16) never re-uploads; harmless log noise.
- Save Timeline: compare once a snapshot-capable RomM is in the testbed (ask the owner when due; no download before).
- The desktop is paired and connected, **Save Sync is on** (Settings → Saves; Secure Saves on). After any `save-proof.sh` (its snapshot restore drops the desktop's token and device row) run `scripts/desktop/testbed/desktop-repair.sh [out-dir]` and check `connect: success` in the newest `build/desktop-logs/*-desktop-run.log`; the restore may also reset Save Sync (re-enable: drawer START, DOWN×8, A, DOWN×6, A, A). `pair-code.py` runs only under `build/desktop-testbed/venv/bin/python`; the pairing form connects via Down → Connect → South, not Enter.
- Save path pieces: `HostSavePaths.saveDirectories/stateDirectories(configId, templates)` (desktop: EmuDeck link > Flatpak > native XDG for mapped standalone emulators; other configs keep `{filesDir}`/`{builtinSaves}` and the host RetroArch cfg folders; phone folders dropped). Save Path row is in Per-Game Settings (Detail: X on a Home card, DOWN×3, A). Manage Cached Saves: Detail → Options (DOWN×4, A) → A; **Y there is Delete**.
- Walk gotcha: A on a Home card is **Play**, not Details. End every play walk with `RUN:pkill -x retroarch` and a wait (else RetroArch runs unsupervised and becomes an orphan session on the next start).
- Proof recipe (DEVLOG "Step 6, part 2"; evidence `build/desktop-check/p3s6-p2/`): Home → Game Boy tab is three left-trigger presses (`PAD:LEFT_TRIGGER=32767 wait:0.5 PAD:LEFT_TRIGGER=0`; L1 opens Downloads); GB row ordered by recent play: `shot:` before RIGHT×n. Play = A, `wait:40`, `RUN:pkill -x retroarch`, `wait:20`. Server saves: `build/desktop-check/p3s6-p2/saves.sh`. Negotiate twice: drawer START, DOWN×6, A (Save Sync), Y (Scan RomM), wait, Y again; startup reconcile has a 5 min cooldown. RetroArch auto-loads `.state.auto`, so use Battery Bay for an upload.
- Desktop DB: `~/.local/share/argosy-desktop/alauncher.db` (no `sqlite3` binary: read with `python3 -I`, `mode=ro`).

## Step 6 notes (desktop save sync)

- Done: part 1 (file accessor, `RetroArchHostLayout`; desktop reads `~/.var/app/org.libretro.RetroArch/config/retroarch/retroarch.cfg`, `savefile_directory` = the owner's EmuDeck `~/Emulation/saves/retroarch/saves`). Part 2: session-end and orphan save/state sync through `SessionSaveFinalizer`, orphan check at launch (`HostSessionWatcher`), self-creating `filesDir`/`cacheDir`, `sqlite-bundled` 2.6.2 (2.5.x threw `SQLiteException: Error code: ` with no text in the app, which breaks every Room `@Upsert` onto an existing row; desktop-only dependency).
- **Real owner data folders.** Testbed artifacts now there (leave them, never delete): `saves/{Ledger Isle,Save Point Harbor,Battery Bay} (Homebrew).srm`, `states/{Argosy Color Trial,Save Point Harbor,Battery Bay} (Homebrew).state.auto(.png)` under `~/Emulation/saves/retroarch/`.
- Still open: `SaveStateManager.desktop` paths are `TODO`s (built-in libretro, Phase 4). Android storage assumptions: done in parts 4, 5 and 7.
- RA login on desktop writes cheevos credentials into the host RetroArch cfg (Android parity; other keys kept).
- ROMs live under `~/.local/share/argosy-desktop/external/downloads` (inside the Flatpak `~/.var/app/<id>/data/...`); step 9 moves them to `~/Games/Argosy`.

## Owner checkpoints already known (ask when reached, one question each)

- Step 10: repo hosting and app id (asked, see Step 10). Step 11: which Fedora/SteamOS machines or VMs.

## Carried forward

- From Phase 1: save-sync stages 2–4; Flatpak runtime `org.freedesktop.Platform` 25.08; when a move takes a generated file, update its generator in the same step (Node is not installed, so `scripts/gen-tokens.mjs` has not been run).
- `withUserCertTrust` asks for `AndroidCAStore` (absent on a JVM): an imported cert would replace system trust; desktop never calls `userCertStore.initialize()`.
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
