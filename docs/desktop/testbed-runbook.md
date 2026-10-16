# RomM testbed runbook (Phase 1 screen checks)

A disposable local RomM 5.1 server with a 13-game self-made library, so the Argosy debug app on the
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
| `build/desktop-testbed/compose.env` | Generated DB passwords and `ROMM_AUTH_SECRET_KEY` (git-ignored) |
| `build/desktop-testbed/credentials.env` | **Test credentials**: RomM admin user/password, client token, URLs (git-ignored, mode 600) |
| `build/desktop-testbed/library/` | The library, RomM structure A: `roms/{gb,gbc,gba,nes}/` |
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
| Library, platform grid (All Games 13, NES 3, GB 4, GBC 3, GBA 3) | menu, `DPAD_DOWN` x3, `BUTTON_A` |
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
