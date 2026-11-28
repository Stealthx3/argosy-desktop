# Phase 6 · After 1.0: Flathub and Linux depth — handoff (current phase)

Rewritten (not appended) at the end of every work unit, so a fresh session can start from it alone. History and reasoning live in `DEVLOG.md` (Phase 6 section). Phase 5's summary: `phase-5.md`.

**Updated:** 2026-11-28. No steps done yet. **Phase gate:** Phase 5 closed on 2026-11-28; Phase 6 starts only on the owner's go-ahead. The plan below is a proposal for the owner to confirm or change at the gate.

## State

- **Pacer:** `phase-gate`. It never resumes from there; wait for the owner. Usage limits are suspended for this project (owner, 2026-11-28).
- **Git:** remote `git@github.com:Stealthx3/icebreaker-client.git` (renamed 2026-11-28). Tags `icebreaker-v1.0.1` (at `cd3ee5457`), `icebreaker-v1.0.0`, `icebreaker-v0.5.0`. No history rewrite.
- **Other sessions' uncommitted files:** `docs/desktop/flathub-plan.md`, `docs/desktop/upstream-2.31-triage.md`, `scripts/desktop/vmlab/lab.sh`, `gm-define.sh`, `qemu-venus.sh`. Leave them alone.
- **Lab VMs** are shut down (owner pause, 2026-11-27). Start only what a step needs; the IceFin guests are shared.

## Done before the gate: app ID `io.github.stealthx3.icebreaker-client` (1.0.1, 2026-11-28)

Owner decisions: new ID, repo renamed to `Stealthx3/icebreaker-client`, name stays IceBreaker, existing installs move by end-of-life rebase, shipped as 1.0.1; usage limits ignored for this project until the owner says otherwise.

- **Code:** ID, file names, URLs; `BrandRelocation` moves data from either earlier Flatpak ID (marker version 2); a version-1 marker sets `.steam-shortcut-stale`, so the Steam screen offers Add, which updates the old shortcut in place. `:shared:desktopTest` 186/186. Not run locally: `testDebugUnitTest` (CI Build covers it).
- **Tag** `icebreaker-v1.0.1` at `cd3ee5457`; `main` pushed to the renamed repo.
- **Published:** Pages `https://stealthx3.github.io/icebreaker-client/` (1.0.1; a fresh throwaway install works); the old address `https://stealthx3.github.io/IceBreaker/` is the user-site folder `IceBreaker/` with the end-of-life rebase (`flatpak update` on a throwaway 1.0.0 install rebased it to 1.0.1); GitHub Release `icebreaker-v1.0.1` (x86_64 bundle from the local build; CI's x86_64 and aarch64 bundles replace it when the CI run finishes).
- **Not verified:** a real start of 1.0.1 (it would move the owner's real data out of the old ID's folder); the Steam re-add on a real Steam; the data move after a real rebase on a machine.
- **icesuite-test (owner: "Yes, publish it"):** catalog PR #7 merged (`app_id` now the new ID); `gh-pages` `8f56a153b8d3`: `io.github.stealthx3.IceBreaker` end-of-life with the rebase, `io.github.stealthx3.icebreaker-client` 1.0.1 added; 516.6 MB of 950. A throwaway 0.5.0 install from icesuite-test rebased to 1.0.1 on `flatpak update`. Published on `lab-icefin-qa2` (lent by IceFin QA, with a QA walk running: fresh site clone, low priority, their `~/icesuite-site` untouched); IceFin QA told to sync.

## Phase 6 plan (proposed)

1. **Flathub follow-through.** The owner writes the manifest, runs the generator on `icebreaker-v1.0.1` and opens the PR. Claude fixes app-side review findings in this repo only; never on the PR, never in the manifest.
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

Stable releases go to IceBreaker's own Pages Flatpak repo (`https://stealthx3.github.io/icebreaker-client/`) and to a GitHub Release with `.flatpak` bundles. The recipe is in `docs/desktop/gotchas.md`.

## Gotchas

- Never run two Gradle or bundle builds at once (they share `desktopApp/build`).
- `build-alpha.sh` installs into the user installation: run it with a throwaway `FLATPAK_USER_DIR` so the owner's install keeps its origin.
- A Flatpak run can't be pointed at other data folders (`--env` is ignored for XDG); use `FLATPAK_USER_DIR` for a separate install, but data is still shared.
- After each upstream merge, regenerate desktop migrations: `python3 -I scripts/desktop/gen_desktop_migrations.py`.
