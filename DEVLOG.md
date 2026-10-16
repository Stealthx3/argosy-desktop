# Argosy Desktop — Development Log

Action and reasoning log for the Linux desktop port. Each phase has its own section. Every entry records what was done and why. Newest entries go at the bottom of their phase.

Project rules (set by the project owner, 2026-10-16):

- Workspace: `/mnt/FileDrive/Personal/Projects/Git/argosy-desktop/` (git working copy).
- Remote: `https://github.com/Stealthx3/argosy-desktop` (fork of `rommapp/argosy-launcher`).
- Push to the remote **only after a phase is complete**.
- Ask the owner between phases: proceed to the next phase, or schedule the continuation for later.
- `README.md` carries a copy of the port plan (`PORT_PLAN.md`).

---

## Phase 0 · Discovery and spikes

**Status:** in progress. Survey done; spikes not yet built. Gate (all three spikes pass) still open.

### 2026-10-16 — Session 1 (claude.ai cloud session)

**Action:** Attached `Stealthx3/argosy-desktop` and cloned it (shallow) into the cloud workspace.
**Reasoning:** The plan's first task is a fork of upstream to build from. The repo turned out to already be that fork (parent `rommapp/argosy-launcher`, head `6e64baa`, v2.23.0), so the fork step is done.

**Action:** Requested and was granted access to the local workspace folder; found it held only the plan PDF.
**Reasoning:** The owner named this folder as the git workspace. The session could copy files into it but had no shell there, so it could not run git or Gradle locally.

**Action:** Tested network access for builds. Maven Central, Google Maven, the Gradle plugin portal, Gradle distributions, JetBrains' Compose repo and Flathub all returned a 403 policy denial from the cloud session's proxy. The GitHub Actions API was also blocked.
**Reasoning:** Every Phase 0 spike needs Kotlin/Compose/Room artifacts from those hosts, so builds could not run in the cloud session. I checked this before writing spike code so the owner could choose a build route early.

**Action:** Asked the owner how builds should run. The owner chose to move development into a Claude Code session on their own computer.
**Reasoning:** Their computer has unrestricted network access and an existing Android/Java setup, and it is where the workspace lives, so builds, git and the Flatpak spike can all run in one place.

**Action:** Ran a full import survey of `app/src/main` (1,695 Kotlin files, 386,679 lines). Results are in `docs/desktop/phase-0-survey.md`.
**Reasoning:** Sizing the Kotlin Multiplatform restructure before touching code is the cheapest way to check the plan's core assumption that most of Argosy can be shared. Result: 54% of files are portable as they are, and only 28% call real Android APIs.

**Action:** Traced the gamepad input pipeline (`ArgosyActivity.dispatchKeyEvent` → `GamepadInputHandler.mapKeycodeToGamepadEvent` → `GamepadEvent` → `InputHandler`).
**Reasoning:** The plan needs SDL3 to feed "the same path" as Android. The trace shows the right seam: SDL3 should emit Android key-code integers, so all mapping, swap and repeat logic is reused unchanged.

**Action:** Inspected the Room database (schema v214, 214 migrations on `SupportSQLiteDatabase`).
**Reasoning:** The Room KMP spike needs to know what will not compile on the desktop. Migrations are the only blocker, and a fresh desktop install does not need them.

**Action:** Checked CameraX, BLE and `testbed/romm` use.
**Reasoning:** These were open questions in the plan's task list. CameraX = QR scanner only; BLE = QuayPass only; `testbed/romm` = Docker RomM 4.9/5.0/5.1 test servers.

**Action:** Corrected two stale facts in the plan copy (`PORT_PLAN.md`): upstream is v2.23.0, not v2.10, and TV Compose is no longer used.
**Reasoning:** The repository copy of the plan should match the code it describes.

**Action:** Wrote the handoff for Claude Code: `CLAUDE.md` (project instructions and next steps), this log, the plan copy, the survey and an upstream proposal draft, all copied into the local workspace.
**Reasoning:** The owner asked to continue in Claude Code. These files let a new session pick up without this conversation. Nothing was committed or pushed, because Phase 0 is not complete.

### Next (for the Claude Code session)

See `CLAUDE.md` → "Phase 0: remaining work".
