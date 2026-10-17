# Save-sync move plan (Phase 1 and later)

Status: approved by the owner 2026-10-16. Stage 1 done and live-proven (see `DEVLOG.md`). Measured 2026-10-16 on `main` after the
Phase 1 data-layer catch-up.

Upstream treats save sync as a fragile zone: "every door into it (pre-launch negotiation,
session-end archiving, reconcile) requires live-data proof - GET /api/saves, negotiate no_op
twice. Code-reading is never sufficient evidence of correctness here" (`AGENTS.md`). This plan
moves the zone into `shared` without changing its behaviour, and proves that against a live
RomM server, not by reading code.

## The zone

Held back from every move so far (`movable.py --exclude ...`, globs in `moves.txt`):

| Part | Files |
| --- | --- |
| `data/sync/**` (coordinator, orchestrator, strategies, reconcile, archiver, platform save handlers, workers) | 73 |
| Save and state repositories (`data/repository/*Save*`, `*State*`) | 13 |
| Save, state and sync use cases (`domain/usecase/{save,state,sync}/**`) | 12 |
| `libretro/LibretroStateSlots.kt` | 1 |
| **Total** | **99** |

The save DAOs and entities already moved in step 3, the save-related preference keys in step 6,
both unchanged; they are data shapes, not doors.

## What keeps it in `app`

42 zone files have direct blockers; the rest wait on them.

| Blocker | Uses | What removes it |
| --- | --- | --- |
| `android.content.Context` | 31 | file-path and file-access interfaces (Phase 3 `FilePaths`) |
| Hilt `@ApplicationContext` | 22 | same; the qualifier goes with `Context` |
| WorkManager (`CoroutineWorker`, `WorkManager`, constraints) | ~25 in 4 workers | Phase 3 `BackgroundJobs` interface |
| `dagger.Lazy`, `dagger.assisted.*` | 13 | add `com.google.dagger:dagger` to `commonMain` (plain JVM, already on Android through Hilt): cheap, no code change |
| `R` | 10 | step 8 (resources) |
| `android.os.Build` | 5 | a device-info stand-in like `DualScreenDeviceInfo` |
| Sigil (`com.nendo.sigil`, JNI) | 3 | a desktop Sigil binding, Phase 3 |

22 zone files are portable today (models, strategies' interfaces, the Xbox disk-image and PS2
memory-card parsers, `ReconcilePlan`, `SyncQueueManager`, `SaveRecoveryGate`, ...).

## Rules for every stage

1. **Renames only.** Files move with `git mv` and keep their package; the only content changes are
   the existing scripted import rewrites (`PlatformLog`, `AppBuildConfig`, ...). No hand edit inside
   the zone. If a stage needs one, it becomes its own change with its own proof and the owner's
   approval.
2. **One stage, one proof.** Each stage below is built, A/B-checked and live-proven before the next.
3. **The baseline is the previous stage's APK**, plus the pre-Phase-1 APK for the first stage.

## Stages

1. **Stage 1 (Phase 1):** the 22 portable files, plus whatever `dagger` in `commonMain` unblocks.
2. **Stage 2 (Phase 3, with `FilePaths`):** the path resolvers and save handlers that only need
   `Context` for directories.
3. **Stage 3 (Phase 3, with `BackgroundJobs`):** the workers and the coordinator around them.
4. **Stage 4:** what remains (Sigil-bound handlers, `Build` checks), as their seams land.

Stages 2 to 4 are where desktop save sync actually starts working; they sit with the Phase 3
platform interfaces and the Phase 3 gate ("saves sync to RomM").

## Live proof protocol

### Testbed with saves (one-time setup)

The current testbed has 13 games and no saves. Extend it:

- Add two or three battery-backed homebrew ROMs (Game Boy cartridge type MBC1+RAM+BATTERY), so a
  core writes an SRAM file on exit.
- Seed server-side saves through the RomM API for some games (`POST /api/saves`), including one
  game with saves in two slots, so download, no-op and the slot logic are all exercised.
- Snapshot and restore both sides, so baseline and candidate start from identical state without
  clearing app data:
  - server: back up and restore the testbed's Docker volumes (database and assets) from a script;
  - device: emulator snapshots on the check AVD (`adb emu avd snapshot save|load`).

### Each run (baseline, then candidate, from the same snapshots)

1. Restore both snapshots; install the APK under test over the same app data; launch.
2. **Reconcile:** let the startup sync finish, then trigger a sync from the Save Sync screen twice.
   Capture every `planReconcile: ... upload= download= conflict= no_op=` line from logcat.
   Pass: after convergence, two consecutive negotiations report `upload=0 download=0 conflict=0`
   with `no_op` equal to the number of saves.
3. **Pre-launch negotiation:** launch a game that has a server save; capture its single-ROM
   negotiation and confirm the on-disk save matches the server's (by hash).
4. **Session-end archiving:** quit the game unchanged (expect no new server version), then play a
   battery-backed ROM long enough for the core to write SRAM and quit (expect exactly one new
   version on the server, in the right slot and channel).
5. Capture after each door: `GET /api/saves` (ids, file names, sizes, hashes, slots), the device's
   save tables (`db_summary.py`), the on-device save files' hashes, and the DataStore summary.

### Pass

The candidate's server state, device database rows, on-disk save hashes and negotiate results
equal the baseline's at every step (timestamps aside), and the candidate reports `no_op` twice.
Anything else stops the stage.

## Proposed next actions

1. Owner approves this plan.
2. Extend the testbed with saves and snapshot/restore scripts (the testbed session has the
   context for this).
3. Run stage 1 with the protocol above.
