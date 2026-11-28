# Phase 6 · After 1.0: Flathub and Linux depth — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md` (Phase 6 section). Phase 5's summary: `phase-5.md`.

**Updated:** 2026-11-28. No steps done yet. **Phase gate:** Phase 5 closed on 2026-11-28; Phase 6 starts only on the owner's go-ahead. The plan below is a proposal for the owner to confirm or change at the gate.

## State

- **Pacer:** `phase-gate`. It never resumes from there; wait for the owner.
- **Git:** `main` was pushed at the Phase 5 close (normal push, no rewrite). Tags `icebreaker-v1.0.0` (at `6e7234b8b`) and `icebreaker-v0.5.0` unchanged.
- **Other sessions' uncommitted files:** `docs/desktop/flathub-plan.md`, `docs/desktop/upstream-2.31-triage.md`, `scripts/desktop/vmlab/lab.sh`, `gm-define.sh`, `qemu-venus.sh`. Leave them alone.
- **Lab VMs** are shut down (owner pause, 2026-11-27). Start only what a step needs; the IceFin guests are shared.

## Before the gate: app ID change to `io.github.stealthx3.icebreaker-client` (1.0.1)

Owner decisions 2026-11-28 (after the Phase 5 close, before Phase 6): new app ID `io.github.stealthx3.icebreaker-client`; repo renamed `Stealthx3/IceBreaker` → `Stealthx3/icebreaker-client` (Flathub checks `github.com/stealthx3/icebreaker-client` for `io.github.` IDs; a dash is allowed only in the last component); display name stays **IceBreaker**; existing installs move by **end-of-life rebase**; ships **now as 1.0.1** with a new tag. Reason: Flathub already has `org.mattdm.icebreaker`. **Not started** beyond the inventory (weekly usage at 48%).

Inventory (2026-11-28): 90 references in ~31 tracked files (outside DEVLOG and handoffs); 14 files named after the ID (manifests `.yml`/`.flathub.yml`, `.desktop`, metainfo, 8 PNG icons, 2 SVG icons); code: `SandboxPermission.APP_ID`, `AppsRepository.desktop.kt` (`ARGOSY_DESKTOP_ID`), tests `BrandRelocationTest`, `FlatpakInfoTest`; `.github/workflows` (2), `scripts/desktop` (9), `packaging/steam` (1), docs.

Order:
1. **Rename in code:** IDs, file names, `X-Flatpak-RenamedFrom` (add the IceBreaker `.desktop`), metainfo `<id>`, `<provides>`/replaces for both old IDs, screenshots URLs, version 1.0.1 and its release entry.
2. **Data move:** extend `BrandRelocation` so data under `~/.var/app/io.github.stealthx3.IceBreaker` moves to the new ID (chained after the Argosy Desktop step); test with `BrandRelocationTest`.
3. **Steam:** `steam-shortcut.json` and the helper config for the new ID; existing shortcuts run the old ID, so the first start refreshes them (Add to Steam's refresh path).
4. **Gate:** `:shared:desktopTest`, `testDebugUnitTest`, `flatpak-builder-lint`, a Flatpak build and start; then tag `icebreaker-v1.0.1`.
5. **Repo rename** on GitHub (`gh repo rename icebreaker-client`), update `origin`, links and the `catalog`.
6. **Publish:** new Pages repo at `/icebreaker-client/`; an end-of-life rebase build for `io.github.stealthx3.IceBreaker` served where the old `/IceBreaker/` address lives (user site folder `IceBreaker/`, like `argosy-desktop/`), and for icesuite-test's 0.5.0 users (needs the owner's OK to publish there); the GitHub Release 1.0.1 with bundles.
7. **Docs:** guide, disclosure, README notice, `CLAUDE.md`, `DECISIONS.md`.

## Phase 6 plan (proposed)

1. **Flathub follow-through.** The owner writes the manifest, runs the generator on `icebreaker-v1.0.0` and opens the PR. Claude fixes app-side review findings in this repo only; never on the PR, never in the manifest.
2. **Catalog and Eden (5c).** Vita3K, ScummVM, PICO-8, xemu; Flycast VMU, Azahar and PPSSPP save paths; m3u multi-disc launch; Eden keys and firmware.
3. **RetroArch core installs (5d).** Fetch a missing core from the libretro buildbot into the RetroArch Flatpak's `cores/`.
4. **Screenshot upload (5e).** Watch each emulator's screenshot folder during the session; hand off to upstream's upload queue.
5. **Live hardware readings (5f)** and the Connections page as status only.
6. **Handheld seat picker (5g).** SDL3 built-in pad detection (Deck, Legion Go, ROG Ally, MSI Claw).
7. **Keep running, Resume and the Home combo (5h).**
8. **Upstream 3.1.1 merge.** Triage first, like v2.31 (`scripts/desktop/merge/`).

## Owner items

- **History rewrite:** the scanner's rewrite inputs (`~/.config/pii-redaction/rewrite-argosy-desktop/`) are unsafe as generated: 261 of 290 literals also occur in current files (short fragments, version numbers). A tree-safe subset (29 entries, `replacements.tree-safe.txt`) changes no current file but leaves the short identity fragments in old commits. Owner decides: tree-safe subset only, a curated context-aware list, or no rewrite. Any rewrite moves every port commit hash and the tags, so do it before the Flathub manifest pins a commit. Test clone: `build/history-rewrite/` (`--refs main <our tags> ^upstream` keeps upstream's commits intact).
- **Flathub name:** Flathub already has `org.mattdm.icebreaker` (a game named "icebreaker"); decide on the store name before submitting.

- Fill in the two "Owner to confirm" lines left in `AI-DISCLOSURE.md` after the submission.
- Reorganize icesuite-test into testing and stable branches. Until then, stable releases go to IceBreaker's own Pages repo and GitHub Releases (owner, 2026-11-28).

## Publishing (owner decision 2026-11-28)

Stable releases go to IceBreaker's own Pages Flatpak repo (`https://stealthx3.github.io/IceBreaker/`) and to a GitHub Release with `.flatpak` bundles. The recipe is in `docs/desktop/gotchas.md`.

## Gotchas

- Never run two Gradle or bundle builds at once (they share `desktopApp/build`).
- `build-alpha.sh` installs into the user installation: run it with a throwaway `FLATPAK_USER_DIR` so the owner's install keeps its origin.
- A Flatpak run can't be pointed at other data folders (`--env` is ignored for XDG); use `FLATPAK_USER_DIR` for a separate install, but data is still shared.
- After each upstream merge, regenerate desktop migrations: `python3 -I scripts/desktop/gen_desktop_migrations.py`.
