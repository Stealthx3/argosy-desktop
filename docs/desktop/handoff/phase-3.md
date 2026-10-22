# Phase 3 · Linux services and alpha Flatpak — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone.
History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases:
`phase-0.md`, `phase-1.md`, `phase-2.md` (closed summaries).

**Updated:** 2026-10-22. The Phase 3 step plan (steps 1–11) is in `DEVLOG.md` under "Phase 3 · Linux services and alpha Flatpak" → "Phase 3 step plan". Nothing uncommitted. Next action: step 1.

## Scope and exit criterion (`PORT_PLAN.md`, Roadmap)

Emulator registry, host launch, save paths, Apps tab, Quick Settings; Flatpak from the owner's own repo. Exit: emulators detected on a clean Fedora and a SteamOS install; a game launches, exits and returns to Argosy; saves sync to RomM; the alpha installs from the own Flatpak repo. Save sync is a fragile zone: live-data proof (GET /api/saves, negotiate `no_op` twice) for every door.

## Next action: step 1 (Linux emulator registry and detection)

- Seams: `shared/src/commonMain/.../data/emulator/EmulatorDetector.expect.kt` (API keyed by Android `packageName`), desktop actual `EmulatorDetector.desktop.kt`, Android actual `androidMain/.../EmulatorDetector.kt` (209 lines) as the model; `EmulatorRegistry.kt` (`EmulatorDef` id/packageName/supportedPlatforms) in commonMain.
- Approach: a desktop-only Linux table keyed by upstream `EmulatorDef.id` (Flatpak id, PATH names, AppImage patterns, RetroDECK/EmuDeck paths, launch template, save path); report `InstalledEmulator` under the upstream package names so both resolvers (`EmulatorResolver.getEmulatorPackageForGame`, `GameLauncher.resolveEmulator`) agree unchanged. Flatpak ids per emulator: verify against Flathub, never guess.
- Host calls: `flatpak list --app --columns=application`, via `flatpak-spawn --host` when `/.flatpak-info` exists. Spike precedent: `spikes/flatpak-launch/launch-spike.sh`.
- Check: Per-Game Settings shows the host's RetroArch Flatpak; desktop unit tests for the parsers.

## Owner checkpoints already known (ask when reached, one question each)

- Step 4: hand edits inside the save zone (replacing `Context` with `FilePaths`) break the move plan's renames-only rule; ask once for stages 2–4 before the first zone edit.
- Step 10: where the own Flatpak repo is hosted. Step 11: which Fedora/SteamOS machines or VMs.

## Carried forward

- From Phase 1: save-sync stages 2–4; Flatpak runtime `org.freedesktop.Platform` 25.08; when a move takes a generated file, update its generator in the same step (Node is not installed, so `scripts/gen-tokens.mjs` has not been run).
- Game launch: `GameLauncher.launch` is a desktop `TODO` (A on Play throws `NotImplementedError: desktop: launch`). Per-Game Settings shows only "Emulator: None detected" until emulators and cores exist. Save Timeline becomes reachable once an emulator resolves (`SaveManagementDelegate.showSaveCacheDialog`): compare it with Android then.
- While a launched emulator covers Argosy, lower `MultiResumeLifecycle` (`desktopApp`) to STARTED or CREATED for the session, as Android stops Argosy; otherwise Home's video preview plays underneath.
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
- Gotchas (restructure scripts, DI, desktop runtime, input injection, walk recipes): `docs/desktop/gotchas.md`. Read the relevant part before touching that area.
