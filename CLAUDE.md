# Argosy Desktop — instructions for Claude Code

This folder is the development workspace for **Argosy Desktop**: a faithful Linux desktop port of the Argosy Android launcher (`rommapp/argosy-launcher`), shipped as a Flatpak. Goal: a user of Argosy Desktop cannot tell it apart from the Android app by using it.

## Start here

1. `docs/desktop/HANDOFF.md` — where the work stands and the exact next action. Read it first, every time.
2. Only as the handoff points you there: `PORT_PLAN.md` (phases, architecture, parity matrix, Flatpak design) and `DEVLOG.md` (history and reasoning). Both are long: search them or read a line range; do not read them whole.
3. Upstream's own agent rules, `AGENTS.md` and `.claude/CLAUDE.md`, apply to code that stays shared with Android; where the desktop port must diverge, say so in `DEVLOG.md`.

## Standing rules from the project owner

- **This project is separate** from any other RomM/Argosy project (e.g. FreightCar, IceRom). Do not mix them.
- **Git workspace:** this folder. **Remote:** `git@github.com:Stealthx3/argosy-desktop.git` (fork of `rommapp/argosy-launcher`, default branch `main`; pushes go over SSH through the repo-local `core.sshCommand`).
- **Branches:** `main` is the Argosy Desktop workspace; all work lands there. `upstream` is a read-only reference copy of `rommapp/argosy-launcher` at its newest stable release. Never commit to `upstream`; `.github/workflows/sync-upstream-release.yml` fast-forwards it to each new stable release tag and does nothing between releases.
- **Push only after a phase is complete.** Commit locally as you go; push to `origin` once, at the end of each phase.
- **Between phases, stop and ask** the owner whether to proceed to the next phase or schedule the continuation for later. Never roll into the next phase on your own.
- **`README.md` must contain a copy of the port plan.** At each phase end, rebuild `README.md` as: a short Argosy Desktop intro and status, then the full contents of `PORT_PLAN.md`, then a link to upstream's original README (kept as `README.upstream.md`). The same README is pushed to GitHub, so both the local and remote repositories carry the plan.
- **Keep `DEVLOG.md` current:** for every meaningful step, record the action and the reasoning, under the current phase's heading. Each phase gets its own clearly separated section.
- Never post to upstream (issues, PRs, Discord) without the owner's explicit go-ahead. `docs/desktop/upstream-proposal.md` is a draft for the owner.
- Commit messages end with the attribution lines your harness specifies.
- **Usage pacing:** use at most half of each 5-hour usage window. Before each new step or batch, and before any long build or verification run, check usage (`mcp__ccd_session_mgmt__get_usage`); continue only while the 5-hour window is below 50% and the weekly all-models window below 90%. At a threshold, finish or park the step in hand (commit it if verified, otherwise note in `HANDOFF.md` and `DEVLOG.md` where it stopped), set the state file to `paused-usage` and stop; a pacer task sends "Pacer: continue" once there is room, and usage is re-checked before resuming. State file (not in git): `.git/argosy-pacer.json`, `{"status": "working" | "unit-done" | "paused-usage" | "phase-gate" | "waiting-owner", "phase": n, "note": "...", "updated": "<ISO time>"}`. Phase gates take priority: at a phase end, push, set `phase-gate` and ask the owner; the pacer never resumes from `phase-gate` or `waiting-owner`.

## Token economy (owner rule, 2026-10-17)

Every tool call re-sends the whole conversation, so context size is the main cost. Keep it small.

- **Work in units.** A unit is one verified, committed batch (or one plan step). At the end of a unit: commit, add the `DEVLOG.md` entry, **rewrite** `docs/desktop/HANDOFF.md` (where things stand, anything uncommitted, the exact next action, gotchas; under about 80 lines), set the state file to `unit-done`, then call `mcp__ccd_session_mgmt__clear_session` with `"self"` and end the turn. The pacer starts the next unit in the cleared session. When you stop for usage or to ask the owner, update `HANDOFF.md` the same way before stopping, so any later start works from it alone.
- **On "Pacer: continue":** the context is fresh. Read `HANDOFF.md`, check usage, set `working`, and carry on from its next action.
- **Batch shell work.** Put multi-step sequences in one script (reusable ones under `scripts/desktop/`, one-offs in a scratch file) and run it once, rather than many small calls.
- **Keep output out of context.** Run builds, tests and verification through `scripts/desktop/quiet.sh <label> <command...>` (or a script that logs to a file and prints a summary). Read logs and large files with `grep` or a line range, never whole. Compare screenshots with the A/B scripts and open an image only when they report a difference.
- Do not poll long runs with repeated checks: start them in the background and wait for the completion notice.

## Environment notes

- JDK: `JAVA_HOME=~/.jdks/jdk-21.0.12.1+1` (the system JDK 17 lacks `jlink`; Gradle 8.9 cannot run on the system JDK 25). Android SDK: `~/Android/Sdk` (`local.properties`).
- Testbed (RomM on `localhost:8093`, emulator `emulator-5556`): `docs/desktop/testbed-runbook.md`.
- Phase 0 setup (fork checkout, toolchain install, spikes) is done; its record is in `DEVLOG.md` under Phase 0.
