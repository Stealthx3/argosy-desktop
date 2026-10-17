# RomM testbed runbook (Phase 1 screen checks)

A disposable local RomM 5.1 server with a 16-game self-made library, so the Argosy debug app on the
Android emulator gets past first run and shows populated Home, Library and Game Detail screens.
It reuses `testbed/romm/` (compose files unchanged) with state kept under the git-ignored
`build/desktop-testbed/`.

## What is where

| Path | What |
|---|---|
| `scripts/desktop/testbed/up.sh` | Idempotent bring-up: secrets, library, venv, containers, admin user, scan, client token |
| `scripts/desktop/testbed/down.sh` | Stop; `--purge` also drops the database/resources/assets volumes |
| `scripts/desktop/testbed/seed-emulator.sh` | Sign the debug app on the emulator in to the testbed and relaunch it |
| `scripts/desktop/testbed/make-library.py` | Writes the library (self-made looping ROMs, no third-party code or art) |
| `scripts/desktop/testbed/romm-setup.py` | Admin user, socket.io scan, client token (used by `up.sh`) |
| `scripts/desktop/testbed/seed-saves.py` | Deterministic server saves for the battery ROMs (used by `up.sh`) |
| `scripts/desktop/testbed/snapshot.sh` | Save, restore and list server-plus-emulator snapshots |
| `scripts/desktop/testbed/save_tables.py` | Device save tables / `GET /api/saves` / DataStore as JSON, raw or normalized |
| `scripts/desktop/check/save-proof.sh` | Save-sync live proof capture (see the last section) |
| `scripts/desktop/testbed/proof-diff.sh` | Compares two proof runs |
| `build/desktop-testbed/compose.env` | Generated DB passwords and `ROMM_AUTH_SECRET_KEY` (git-ignored) |
| `build/desktop-testbed/credentials.env` | **Test credentials**: RomM admin user/password, client token, URLs (git-ignored, mode 600) |
| `build/desktop-testbed/library/` | The library, RomM structure A: `roms/{gb,gbc,gba,nes}/` |
| `build/desktop-testbed/snapshots/` | Server halves of the snapshots |
| `build/desktop-baseline/populated-*.png` | Reference screenshots from the baseline APK (git-ignored) |

The admin password and token are generated on first run and live only in `credentials.env`. Log
in to the web UI at http://localhost:8093 with `ROMM_USER` / `ROMM_PASSWORD` from that file.

## Start and stop

```bash
scripts/desktop/testbed/up.sh
```

First run takes about a minute (image pull aside); re-runs skip every finished step. `--rescan`
forces a new quick scan. Ports: RomM 8093, MariaDB 3399. Compose project name: `argosy-testbed`.

```bash
scripts/desktop/testbed/down.sh
```

`down.sh --purge` deletes the server state; the next `up.sh` recreates the admin, rescans and mints
a new token, so the emulator must be seeded again (its old token is gone).

## Signing the emulator app in

```bash
~/Android/Sdk/emulator/emulator -avd argosy_handheld -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect
```

Wait for `adb -s emulator-5554 shell getprop sys.boot_completed` to print 1, install the APK under
test (baseline: `adb -s emulator-5554 install -r build/desktop-baseline/app-debug-3ad8281f6.apk`),
then:

```bash
scripts/desktop/testbed/seed-emulator.sh
```

This uses the debug-only `DebugSeedReceiver` (present in the baseline APK) with the client token,
grants all-files access, then force-stops and relaunches the app. The emulator reaches the host at
`http://10.0.2.2:8093`. Allow 15 to 45 seconds for "Preparing home..." while the first library sync
runs.

**The relaunch is required.** The ui-ux-testing skill says the seed routes to Home without one;
on this build it does not: the app stays on the Welcome screen after the broadcast (server log shows
it did connect: `users/me`, `POST /api/devices`, `sync/negotiate`), and only a fresh launch routes to
Home.

The real sign-in flow (typing the URL, pairing code) was not driven; seeding skips it, and with it
storage, platform and core setup. Use the wizard only when onboarding is what is under test.

## Reachable screens and how to get there (gamepad keyevents)

| Screen | From Home |
|---|---|
| Home (platform rows Game Boy, GBC, GBA, NES, Recent) | launch |
| Game Detail | `KEYCODE_BUTTON_X` on a game |
| Side menu (Home, Check-In, Collections, Library, Downloads, Library Sync, Save Sync, Apps) | `KEYCODE_BUTTON_START` |
| Library, platform grid (All Games 16, NES 3, GB 7, GBC 3, GBA 3) | menu, `DPAD_DOWN` x3, `BUTTON_A` |
| Library, all games A-Z with section index | `BUTTON_A` on All Games |

`KEYCODE_BUTTON_Y` toggles Favorite (it writes to the server; press it again to undo). Download
works too (each ROM is 16 to 256 KB) but was not exercised.

## Limits

- No cover art or metadata: no IGDB/ScreenScraper keys are configured and the ROMs are self-made, so
  every card is a title placeholder (`identified_roms: 0`). Layout, lists, counts, platform icons and
  navigation are comparable; artwork rendering is not.
- No saves, achievements, collections or play history on the server, so those screens show their
  empty states.
- The ROMs only loop forever; launching one proves the launch path, not gameplay.
- One RomM version (5.1.0). For 4.9/5.0 see `testbed/romm/README.md`.

## Saves and save-sync proof

The live proof protocol in `docs/desktop/save-sync-move-plan.md` runs on this testbed. Everything
below uses the check AVD `argosy_handheld_check` on `emulator-5556`.

### What the testbed adds for saves

- Three battery-backed Game Boy ROMs (MBC1+RAM+BATTERY, 8 KiB SRAM), written by
  `make-library.py` next to the other GB games. Their SRAM layout and behaviour:

  | Game | Cart | Server saves (seeded) | What playing it does |
  |---|---|---|---|
  | Ledger Isle | passive | `autosave` (counter 0) | nothing: quitting leaves the save unchanged |
  | Battery Bay | counter | `autosave` (counter 5), `checkpoint` (counter 2) | +1 to byte 0 per boot: one deterministic new save per session |
  | Save Point Harbor | counter | none | first session creates a new save |

  The ROMs carry the first four header-logo bytes (`CE ED 66 66`) because mGBA, the built-in
  emulator's default GB core, rejects a GB ROM without them ("Cannot load game").
- `seed-saves.py` uploads the seeded saves through `POST /api/saves` (`rom_id`, `emulator=mgba`,
  `slot`, multipart `saveFile` named `<rom name>.srm`, the shape the built-in emulator uploads).
  `up.sh` runs it; it skips any slot whose content is already on the server. RomM 5.1 stores
  uploads as `<name> [YYYY-MM-DD_HH-MM-SS].srm` and overwrites a save with the same stored name, so
  uploads for one rom are spaced a second apart.
- After changing ROM bytes in place, `up.sh --hashes` runs a "hashes" scan so the server's ROM
  hashes follow.

### Snapshots

```bash
scripts/desktop/testbed/snapshot.sh list
```

```bash
scripts/desktop/testbed/snapshot.sh restore saves-baseline
```

```bash
scripts/desktop/testbed/snapshot.sh save <new-name>
```

A snapshot is the server's four Docker volumes (database, resources, redis, assets) taken with the
containers stopped, the `credentials.env` that matches them, and an emulator snapshot of
`argosy_handheld_check` (`adb emu avd snapshot save|load`). Server parts live in
`build/desktop-testbed/snapshots/<name>/`; the emulator part lives in the AVD. Saving takes about
40 seconds and restoring about 35. Restore instead of clearing app data or reinstalling.

**`saves-baseline`** (the snapshot to start every proof run from):

- Baseline APK `app-debug-3ad8281f6.apk` installed with `--abi arm64-v8a`, signed in to the testbed,
  library synced (16 games).
- Settings → Built-in Emulator → Architecture set to **ARMv8 (64-bit)**, mGBA (arm64) installed
  from Manage Cores.
- Settings → Saves → **Save Sync on** (Secure Saves on, the default once sync is enabled).
  Notifications allowed.
- Ledger Isle, Battery Bay and Save Point Harbor downloaded. Battery Bay's `checkpoint` save was
  pulled to disk when sync was switched on; nothing has been played.
- Server: the three seeded saves (ids 1-3), no states.

Two emulator facts behind those settings (both are AVD facts, not app regressions):

- The APK ships `liblibretrodroid.so` and `libsigil-jni.so` for arm64-v8a only, but other native
  libs for x86_64 too, so a plain install on the x86_64 AVD runs as x86_64. Launching any game then
  crashes with `UnsatisfiedLinkError: libsigil-jni.so`. Installing with `--abi arm64-v8a` runs the
  whole app through the image's arm64 translation (`libndk_translation`).
- Even then the app picks core downloads from the device's first 64-bit ABI (x86_64), and mGBA
  fails to `dlopen`. The Architecture setting overrides that and clears the wrong cores itself.

### Proof run

```bash
scripts/desktop/check/save-proof.sh build/desktop-baseline/app-debug-3ad8281f6.apk build/desktop-check/save-proof/baseline
```

```bash
scripts/desktop/check/save-proof.sh app/build/outputs/apk/debug/app-debug.apk build/desktop-check/save-proof/candidate
```

```bash
scripts/desktop/testbed/proof-diff.sh build/desktop-check/save-proof/baseline build/desktop-check/save-proof/candidate
```

A run takes about 6 minutes. It restores `saves-baseline` (third argument: another snapshot
name, or `--no-restore`), installs the APK over the app data with `--abi arm64-v8a`, and captures
after each step:

| Step | What happens | Baseline result |
|---|---|---|
| `00-startup` | launch, startup reconcile | `upload=0 download=2 conflict=0 no_op=1` (pulls both autosaves) |
| `01-sync-1` | menu → Save Sync → Y (Scan RomM) | `upload=0 download=0 conflict=0 no_op=3` |
| `02-sync-2` | the same again | `upload=0 download=0 conflict=0 no_op=3` |
| `03-prelaunch-ledger` | `argosy://game/<id>` → Play, 40 s in game | pre-launch `NO_OP`, `disk=Matches` |
| `04-quit-unchanged` | in-game menu → Quit Game | `Sync result: NO_CHANGE`, server unchanged |
| `05-prelaunch-battery` | Battery Bay → Play, 40 s | pre-launch `NO_OP`, `disk=Matches` |
| `06-session-end` | Quit Game | `Sync result: UPLOADED`, one new `autosave` (counter 6, md5 `3108569c…`) |

Each step directory holds `logcat.txt` (everything logged during the step), `plan-reconcile.txt`,
`saves-api.json` (raw `GET /api/saves`), `states-api.json`, `db/` plus `db-summary.json`,
`save-tables.json`, `datastore.json`, `save-files.sha256` and `save-files.md5` (built-in saves,
states and the save cache), and a screenshot. The `*.normalized.*` files are what `proof-diff.sh`
compares. They mask clocks, the timestamps RomM puts in stored file names, and the dated save-cache
folders. They leave out emulator save states, whose bytes depend on how long the game ran and whose
upload can race the RomM reconnect after a game closes (`state-tables.normalized.json` keeps them
for reading). Server ids are kept: both runs start from the same snapshot, so ids must match too.

Things the run depends on:

- Sessions shorter than 30 seconds with no save activity are cancelled without any session-end save
  work (`SessionEndCoordinator.SHORT_SESSION_SECONDS`), so each game runs 40 seconds. A cancelled
  session shows as `Cancelled (no save backup)` in `run.log`.
- Navigation is by gamepad keyevents and the `argosy://game/<id>` deep link; game ids come from the
  device database. "Quit Game" is the ninth entry of the in-game menu (Back opens it).
