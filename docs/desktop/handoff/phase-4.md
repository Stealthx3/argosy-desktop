# Phase 4 · Linux adaptation — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md`; read only the parts this file points to. Earlier phases: `phase-0.md` … `phase-3.md` (closed summaries).

**Updated:** 2026-10-24 (step 9 investigation). Steps 1–8 done (step 1's owner hardware check still pending, below); step 9 in progress. The Phase 4 step plan (steps 1–10) is in `DEVLOG.md` under "Phase 4 · Linux adaptation" → "Phase 4 step plan". Nothing uncommitted.

## Scope and exit criterion

`PORT_PLAN.md` Roadmap (Phase 4) and "Desktop divergences". Exit: every Port, Adapt and Linux row of the parity matrix works on the desktop; every Remove row has no entry point on the desktop and the Android app is unchanged (A/B screenshots and unit tests); display profiles and the wizard's Display setup step work on a handheld, a monitor and a TV-sized screen.

## Step 1 (input mode, Steam Input): code done, owner check pending

- Done (DEVLOG "Step 1, part 1"): `ControllerInputMode` setting (Navigation → Controller → Input Mode, desktop only via `ControllerInputSupport`), SDL hints per mode at OVERRIDE priority, Steam-virtual pads skipped outside Steam Input, pad logging on connect, SDL restart on change. Walk evidence `build/desktop-check/p4s1/`, Android unchanged `p4s1-android`.
- Owner check (ask once, with the Phase 4 close alpha; the live repo changes only at a phase close): which Steam Controller model; in Desktop Mode with Steam running and Argosy started normally (expect mode SDL3, the controller via SDL's HIDAPI driver, no rumble: SDL has none for it), and Argosy added to Steam and started from it (expect Steam Input, rumble through Steam). The `grep -i "DesktopGamepad" ~/argosy-run.log` lines show what Argosy saw. A new alpha needs `build-alpha.sh` → `publish-repo.sh` → `push-pages.sh --phase-close 4` (recipe in `docs/desktop/gotchas.md`).
- Inside the Flatpak, `SteamVirtualGamepadInfo` points to a host `/tmp` file the sandbox cannot read, so Steam handles may read 0; detection also uses the 28de:11ff ids. Check in the owner test.

## Step 2 (navigation button mapping): closed

- Parts 1–3 in DEVLOG ("Step 2, part 1/2/3"). Footer glyphs follow the active controller's remap through `LocalButtonGlyphRemap` (identity on Android); mapping rows use friendly key names. Evidence `build/desktop-check/p4s2/`, `p4s2p3/`, Android `p4s2-android` (identical to `p4s1-android` apart from live data).
- Not verified: a real controller with a remap, a Nintendo-layout pad with a remap (ask with the step 1 owner check).
- Walk route to Button Mapping: from Home `START wait:2 DOWN*7 A` (Settings; DOWN×8 now opens Accounts), `DOWN*2 A`, `DOWN A`; focus 1–16 actions, 17 reset, 21–36 keyboard, 37 keyboard reset. End walks with the resets.

## Step 3 (desktop removals): closed

- DEVLOG "Step 3, part 1" (entry-point list, `PlatformFeatures`), "part 2" (QuayPass with Social; JavaSteam inert by owner decision; no remaining traces; wizard verified). Evidence `p4s3/`, `p4s3b/`, `fresh-p4s3w/`, Android `p4s3-android`, `p4s3b-android`.

## Step 4 (window and full screen): closed

- DEVLOG "Step 4". Decorated 1280×720 window, F11, gamescope forced, `--fullscreen`/`--windowed`, Interface → Start in Full Screen (desktop only; Android's system-bar rows hidden on desktop), dark when no theme is stored. Evidence `p4s4/`, `fresh-p4s4/`, Android `p4s4-android2`. Not verified: launch with the setting on, real gamescope (owner test with the next alpha).

## Step 5 (display profiles and the wizard's Display setup step): closed

- DEVLOG "Step 5, part 1" and "part 2". TV = option B Couch scale (owner, https://claude.ai/artifact/4DJ1zZT3T26ALojCQkdrdB). Evidence `fresh-p4s5/`, `fresh-p4s5b/`, Android `p4s5-android`, `p4s5b-android`. Not verified: cursor hiding, detection on a real handheld/TV, touch on a handheld (add to the owner test).

## Step 6 (Flatpak permissions screen): closed

- DEVLOG "Step 6, part 1" and "part 2". Not verified: the grant modal and a real grant (needs a revoked permission on the owner's machine; owner-test recipe in the part 2 entry).

## Step 7 (host GPU screen): closed

- DEVLOG "Step 7". Not verified: AMD/Intel/multi-GPU hardware (fixture tests).

## Step 8 (emulator database, suggestions, managed installers): closed

- DEVLOG "Step 8 decisions" and parts 1–3. Not verified: a real Flathub install from the picker, an AppImage install or a Flatpak update from Managed Installers (owner tests in the part 2 and part 3 entries), the remote database refresh (repo `main` has no copy until the phase push).

## Step 9 (libmpv video wallpaper): parts 1–2 done, next part 3

- Done: libmpv in the Flatpak (part 1); `platform/mpv/` binding, software-rendered `MpvPlayer`, `InlineTilePlayer` and `PageThemePlayer` desktop actuals, `--probe-video=<file>` check (part 2). Probe inside the Flatpak: VP9 and H.264 both ≈30 fps.
- **Blocked on the owner (2026-10-24):** running the downloaded `yt-dlp` release (2026.10.21, sha256 b7af77f6…bf14, matches the release's SHA2-256SUMS) was denied by the session's permission check (code from an external source). Asked the owner whether to allow running/bundling yt-dlp, or to close step 9 with local video only and leave YouTube trailers empty on the desktop. Open question either way: yt-dlp now wants a JavaScript runtime (Deno/Node) for YouTube, which the Flatpak runtime lacks.
- **Next action, part 3 (if allowed):** YouTube trailers (`YouTubeVideoPlayer` desktop actual, Home's video wallpaper): bundle `yt-dlp` (Python, from its release zipapp or pip module in the manifest, pinned) and resolve `yt-dlp -g -f "best[height<=720][vcodec!=none]"` for the video id on IO, then `MpvPlayer.load(url)`; call `onReady` on the first frame and `onError` when resolution or playback fails; respect `muted`. Probe with `--probe-video=https://…` after resolving in the probe too, or add a `--probe-youtube=<id>` flag.
- Also: a UI walk with a real local video (custom-grid tile or page backdrop) if a setup path is quick; map `seekTicks` (Android seeks `SEEK_STEP_MS` per tick).

## Every step close from now on

- After the close commit: `build-alpha.sh` → `GNUPGHOME=~/.local/share/argosy-desktop-signing/gnupg publish-repo.sh https://stealthx3.github.io/argosy-desktop/repo/ 5CC88C563BCBCD4F71D096277DDEE596AEB5F186` → `push-pages.sh --step-close 4.<n>` (owner rule 2026-10-24).
- Walks on throwaway data: `fresh-walk.sh` with the gotchas recipe (`wait:35`; the wizard now has the Display step: one `PAD:SOUTH` after Get Started chooses the focused card). Do not switch profiles on the owner's data.

## Carried forward

- From Phase 3: see `phase-3.md` "Carried forward" (download stall is Phase 5; open owner questions on standalone save proof and rows 18–30).
- Phase 4 decisions still open inside steps: emulator database schema/location/update path and initial emulators (step 8); TV safe-area default 3 % and live thumbnails (step 5, confirm with the owner when reached).
- Rules that still apply: label vs token for every new string; settings chain end to end; desktop-only gates, never deletions from shared code; Android A/B baseline `build/desktop-baseline/app-debug-upstream-v2.24.0.apk`.

## Where things are

- Desktop graph `shared/src/desktopMain/.../di/`; new commonMain injections need desktop bindings (`scripts/desktop/restructure/di_closure.py`).
- Desktop data `~/.local/share/argosy-desktop` (never delete without the owner); Flatpak data `~/.var/app/io.github.stealthx3.ArgosyDesktop`.
- Testbed now has a GameCube platform (`Big Disc Test`, 1.4 GB synthetic); save-proof expectations count only the battery ROMs.

## Commands and tools

- Runs and walks: `scripts/desktop/check/{desktop-shot,desktop-drive,pad-walk}.sh`; Flatpak build/publish: `scripts/desktop/flatpak/`.
- Verification: `verify.sh`, `ab-check.sh`, `save-proof.sh`; quiet runner `scripts/desktop/quiet.sh`; JDK `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1`.
- Screenshots: window captures only, never full screen. `pkill -x <name>` inside walks.
- Gotchas: `docs/desktop/gotchas.md`.
