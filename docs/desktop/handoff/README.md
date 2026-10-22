# Phase handoffs

One handoff file per phase. The **current** phase's file is the working handoff: rewritten (not
appended) at the end of every work unit, so a fresh session can start from it alone. When a phase
closes, its file is rewritten one last time as a short closed summary (outcome, what carries
forward, where the details are) and the next phase's file is created from it.

**Current phase: 3** → [phase-3.md](phase-3.md) (not started: waiting for the owner's go-ahead)

| Phase | File | Status |
| --- | --- | --- |
| 0 · Discovery and spikes | [phase-0.md](phase-0.md) | Closed 2026-10-16 |
| 1 · Kotlin Multiplatform restructure | [phase-1.md](phase-1.md) | Closed 2026-10-16 |
| 2 · Desktop UI | [phase-2.md](phase-2.md) | Closed 2026-10-22 |
| 3 · Linux services and alpha Flatpak | [phase-3.md](phase-3.md) | Current (not started) |
| 4 · Remaining parity | — | Not started |
| 5 · Beta and Flathub | — | Not started |

A new session reads the current phase's file first. Earlier phases' files are short summaries;
open one only when the current handoff points to something carried forward from it.
