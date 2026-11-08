# Brief for Claude Cowork: repo reorganization and documentation rework

Written 2026-11-08 by the Claude Code session that ran Phase 5, at the owner's request. Development is paused while this work happens; the current state of the code is in `phase-5.md` ("Paused state"). This brief lists what the reorganization has to respect. What to change is the owner's call.

## What this repository is

Argosy Desktop: a Linux desktop port of the Argosy Android launcher (`rommapp/argosy-launcher`), shipped as a Flatpak. It is a **fork that stays mergeable with upstream**: Android code and the shared Kotlin Multiplatform code are upstream's, and upstream releases are re-applied onto the fork (`scripts/desktop/restructure/`, rehearsed in Phase 1). Remotes: `origin` = `Stealthx3/argosy-desktop` (branches `main`, `upstream`, `gh-pages`), `rommapp` = upstream.

## Hard constraints

1. **Do not move or rename upstream-owned files.** Moving them turns every future upstream sync into conflicts. Upstream-owned: `app/`, `libretrodroid/`, `sigil/`, `libs/`, `config/`, `gradle/`, `design-system-docs/`, `design-handoff/`, `AGENTS.md`, `CONTRIBUTING.md`, `.claude/` (skills and rules), `docs/*.md` at the top of `docs/` (save sync, RomM rules, privacy, Play Store), `README.upstream.md` (upstream's README, kept verbatim), `LICENSE`. When unsure: if `git log --format=%an -- <path>` shows upstream authors, leave it in place.
2. **Code paths are load-bearing.** Gradle modules (`shared/`, `desktopApp/`, `desktop-libs/`, `app/`), `desktopApp/flatpak/` (manifests, launcher script, generated offline sources) and `design-system-docs/tokens.json` (the token source the build generates code from) must not move without updating builds, CI and the Flathub manifest. Treat any code-path move as a separate, owner-approved change.
3. **Branches:** never commit to `upstream` (a workflow fast-forwards it to each upstream release). Never touch or delete `gh-pages` (it serves the live alpha Flatpak repo). Pushes to `origin` happen only at a phase close, unless the owner says otherwise.
4. **Git state to resolve first:** local `main` and `origin/main` have diverged (origin has docs-only commit 6ddef138e; local has 48 code commits). Reconcile before reorganizing, with the owner's go-ahead, or the reorganization lands on one side only.
5. **Owner rules in `CLAUDE.md` that the docs encode** (change them only with the owner):
   - `README.md` = short intro and status, then the full `PORT_PLAN.md`, then a link to `README.upstream.md`. It is the GitHub page. Note: 6ddef138e replaced it with a rewritten GitHub README; reconcile that with the rule.
   - `DEVLOG.md`: every meaningful step, action plus reasoning, one section per phase.
   - One handoff file per phase in `docs/desktop/handoff/`, the current one rewritten at each unit end; `README.md` there names the current phase.
   - Never post to upstream (issues, PRs, Discord) without the owner.

## Files and scripts that parse the docs

Renaming or restructuring these breaks tooling unless the parser is updated in the same change:

| Consumer | Reads |
| --- | --- |
| `scripts/desktop/progress-page.py` (builds the progress page artifact) | the phase table in `docs/desktop/handoff/README.md`; the Roadmap in `PORT_PLAN.md`; each phase's `**Phase N plan:**` numbered list (`N. **Title.**`) in `DEVLOG.md`; the "Steps 1–N done" line in the current handoff; `.git/argosy-pacer.json` |
| `CLAUDE.md` | paths to the handoff folder, `PORT_PLAN.md`, `DEVLOG.md`, `docs/desktop/gotchas.md`, `docs/desktop/testbed-runbook.md`, `scripts/desktop/quiet.sh` |
| `scripts/desktop/restructure/moves.txt` | source paths for the upstream re-apply |
| `scripts/desktop/check/save-proof.sh` | testbed doc paths |
| `.claude/skills/*` (upstream) | upstream doc paths; do not edit |
| Daily update task, pacer task (scheduled, outside the repo) | `docs/desktop/handoff/README.md`, the current handoff, `.git/argosy-pacer.json` |

## Inventory (fork-only material, the candidates for reorganization)

- Top level: `README.md` (145 lines), `PORT_PLAN.md` (366; phases, architecture, parity matrix, Flatpak design, desktop divergences), `DEVLOG.md` (~1,750; history and reasoning by phase), `CLAUDE.md` (session rules for Claude Code).
- `docs/desktop/`: `handoff/` (phase files 0–5 + index + this brief), `gotchas.md`, `testbed-runbook.md`, `flathub-plan.md`, `owner-test-phase-4.md`, `phase-0-survey.md`, `save-sync-move-plan.md`, `upstream-proposal.md` (owner's draft, unsent), `emulators/awesome-linux-candidates.json`.
- `docs/postmortem/` + `scripts/postmortem.mjs` (in 6ddef138e; untracked locally until the reconcile).
- `scripts/desktop/`: `check/` (walks and A/B checks), `flatpak/` (build, sign, publish, Pages), `restructure/` (upstream re-apply), `testbed/` (local RomM), `vmlab/` (VM test lab; three files uncommitted from the closed SteamOS/Bazzite lab), `fonts/`, `quiet.sh`, `progress-page.py`.
- `spikes/` (Phase 0 proof-of-concept code, 298 files), `testbed/` (7 files), `desktop-libs/` (Gradle module, code).
- Local only, not in git (ignored via `.git/info/exclude`): `argosy-desktop-port-plan.pdf`, `argosy-desktop-test-checklist.pdf`; everything under `build/`.

## Suggested acceptance checks after the rework

- `python3 -I scripts/desktop/progress-page.py` still lists all phases and Phase 5's steps.
- `README.md` still follows the owner's README rule (or the rule in `CLAUDE.md` is updated with the owner).
- Every path named in `CLAUDE.md` exists.
- `git diff --stat upstream...main -- <upstream-owned paths>` shows no new moves.
- A fresh Claude Code session can start from `docs/desktop/handoff/README.md` alone and reach the current state.
