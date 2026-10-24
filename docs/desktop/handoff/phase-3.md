# Phase 3 · Linux services and alpha Flatpak — closed

**Closed:** 2026-10-23 by owner decision ("Begin phase 4 and steaminput integration"). `main` pushed; `gh-pages` carries the signed Flatpak repo.

## Outcome

- Detection: `LinuxEmulators.kt` (keyed by upstream `EmulatorDef.id`; Flatpak > AppImage > EmuDeck > PATH), `HostShell` (`flatpak-spawn --host` when sandboxed).
- Launch and return: `LinuxLaunch.kt` argv for RetroArch, Dolphin, PPSSPP, Cemu, DOSBox-X; `HostGameProcess` supervises one process; play sessions recorded (`PlaySessionTracker.desktop`, `HostSessionWatcher`).
- Save sync on desktop: `HostSavePaths`, `RetroArchHostLayout`, session-end and orphan sync, `SaveSyncPass` and `DesktopBackgroundJobs`; live proofs in DEVLOG steps 5, 6 and 10.
- Apps tab (`data/apps/`), Quick Settings (`wpctl`/`pactl`, backlight + logind, SDL rumble), first-run wizard with `EmulatorFolderAccessStep`, XDG FileChooser portal (`PortalFileChooser`).
- Alpha Flatpak: `desktopApp/flatpak/`, `scripts/desktop/flatpak/{build-alpha,publish-repo,push-pages}.sh`; signed (key `5CC88C56…F186` in `~/.local/share/argosy-desktop-signing/gnupg`, only copy: owner to back up); live at `https://stealthx3.github.io/argosy-desktop/`.
- Owner test on Bazzite (DEVLOG "Owner test on Bazzite; Phase 3 close"): 15 pass, 3 fail, 13 skipped.
- Plan re-scoped during the phase: `PORT_PLAN.md` "Desktop divergences" (DEVLOG "Plan change: …" entries).

## Carried forward

- To Phase 4 step 1: Steam Controller mapping and rumble (diagnosis in the DEVLOG close entry).
- To Phase 5 (beta), owner decision: stall when downloading a ROM already in the target folder (GameCube `.rvz`). Repro: delete the game's ROM in the UI, copy the file back, download. Testbed fixture `roms/ngc/Big Disc Test (Homebrew).rvz` (1.4 GB) and dev copy `~/Games/Argosy/ngc/`; sampler `build/desktop-check/dl-stall/edt-sampler.sh`.
- Not verified on a real machine: save sync, RetroArch launch, a clean Fedora install; live `flatpak override` grant, document-portal path, logind brightness write, rumble on a physical pad.
- Not launched yet: DuckStation, PCSX2, RPCS3, Azahar, Vita3K, ScummVM, dosbox-staging (verify their CLI with a display); multi-disc, variants.
- Open owner questions: standalone-emulator save proof (homebrew GC/PSP/PS2 in the testbed, or one of the owner's games); game rows 18–30 added by "Scan for Android games" (keep or remove; count `steam://` entries as games?). Do not touch either before the answer.
- Parity finding for the owner: upstream `SaveUploader` skips a re-upload after the server loses a save (DEVLOG "Step 6, part 8").
- 40 `TODO(...)` stubs in desktopMain; `SaveStateManager.desktop` paths wait on built-in emulation (not in this version).

## Details

`DEVLOG.md`, section "Phase 3 · Linux services and alpha Flatpak". Gotchas and walk recipes: `docs/desktop/gotchas.md`. Testbed: `docs/desktop/testbed-runbook.md`.
