# Emulator launch audit (2026-11-12)

Follow-up to [issue #1](https://github.com/Stealthx3/argosy-desktop/issues/1). Eden was fixed in catalog version 3, commit d21dd4641.

The catalog is `shared/src/desktopMain/resources/emulators/linux-emulators.json`. An entry can only launch games if it has a `launch` object. Without one, `LinuxLaunch.standalone()` returns null and the user sees "Failed to launch <name>". Nothing in the UI hides or marks these emulators, so they are detected and can be chosen, but they never start a game.

Phase 2 left these entries out on purpose (DEVLOG line ~850): their `--help` couldn't be printed on this host. Every command line below was instead checked in the emulator's own current source code, as the upstream mandate requires. None has been run on this host yet.

## Fixable with a catalog change only (no app rebuild)

Each of these takes a single `{rom}` path, which is all `LinuxLaunch` supports today.

| Entry | Proposed `args` | Upstream source |
| --- | --- | --- |
| `duckstation` | `["-batch", "--", "{rom}"]` | `src/duckstation-qt/qthost.cpp`: `-batch` quits when the game stops; `--` ends options, so the rest is the path |
| `pcsx2` | `["-batch", "--", "{rom}"]` | `pcsx2-qt/QtHost.cpp`: same parser shape as DuckStation |
| `ryujinx` | `["{rom}"]` | Ryubing `src/Ryujinx/Utilities/CommandLineState.cs`: an argument it doesn't recognise becomes `LaunchPathArg`. **`-g` is the graphics backend here, not the game.** |
| `azahar` | `["{rom}"]` | `src/citra_qt/citra_qt.cpp`: a last argument that doesn't start with `-` is the game path. **`-g` is the GDB port here.** |
| `melonds` | `["{rom}"]` | `src/frontend/qt_sdl/CLI.cpp`: positional `nds` argument; the default `--boot auto` boots it |
| `flycast` | `["{rom}"]` | `core/cfg/cl.cpp`: a non-option argument is used as the disc image or ROM |
| `mupen64plus_fz` (RMG) | `["--quit-after-emulation", "{rom}"]` | RMG `Source/RMG/main.cpp`: positional `ROM`; `-q/--quit-after-emulation` quits when the game ends |
| `aps3e` (RPCS3) | `["--no-gui", "{rom}"]` | `rpcs3/rpcs3.cpp`: positional boot path goes to `Emu.BootGame`; `--no-gui` runs the game without the main window |

**Also fix:** `linux_rmg` already launches with `["{rom}"]`, but RMG stays open after the game ends. It should get `--quit-after-emulation` as well, so the player returns to Argosy.

**Check before shipping:** RPCS3 games in RomM are often folders, `.iso` files or `.pkg` files. `BootGame` accepts a game folder, an `EBOOT.BIN` or an ISO, but a `.pkg` has to be installed first. Confirm what form a downloaded PS3 game takes on desktop before adding `aps3e`.

**Return to Argosy:** Ryujinx, Azahar, melonDS and Flycast have no "quit when the game ends" option. When the player stops the game, the emulator's own window stays open until they close it. This is the same as Cemu and PPSSPP today, so it's acceptable. Note it in the release notes.

## Needs a small code change: new placeholders

`LinuxLaunch.standalone` only replaces `{rom}`. Two emulators need the ROM's folder or file name instead:

| Entry | Proposed `args` | Why |
| --- | --- | --- |
| `mame4droid` (MAME) | `["-rompath", "{romdir}", "{romname}"]` | MAME takes a system short name plus `-rompath` (`docs/source/commandline/commandline-all.rst`). It does not take a full path to the zip. |
| `scummvm` | `["--path={romdir}", "--auto-detect"]` | `base/commandLine.cpp`: `--auto-detect` detects the game in `--path`, which must be a folder. Needs an extracted game folder. |

**Proposal:** add `{romdir}` (the parent folder) and `{romname}` (the file name without its extension) to `LinuxLaunch.standalone`, with unit tests. These need a Flatpak rebuild, because older builds would pass the placeholders through as literal text. Bump the catalog version only after that build ships. Alternatively, put these entries in a separate `minAppVersion`-gated catalog field. Also check how Argosy unpacks ScummVM games on desktop.

## Needs design: Vita3K

`vita3k/config/src/config.cpp`:
- a positional `content-path` (`.vpk`, `.zip` or a folder) installs the game and then runs it;
- `-r/--installed-path <title id>` runs a game that is already installed;
- `--frontend sdl` runs the `-r` game in a plain window and quits with it.

Installing every time is slow and may ask to overwrite. A proper flow is to install once with the content path, then use `-r <title id>`. That needs the title ID, from the game's `param.sfo` or RomM metadata. Leave Vita3K out until this is designed.

## Prevent this class of bug

1. **Mark emulators that can't launch** in the Emulators manager and the emulator picker, for example "Launching not supported yet", or keep them out of the picker. The launch error should name the reason instead of a generic "Failed to launch". Desktop-only; touches UI strings in every locale.
2. **Add a unit test** that every upstream-id entry either has `launch` or is on an explicit `noLaunchYet` list (today only `linux_` entries are required to have one). A new entry can then never ship silently unlaunchable.
3. **Bump the catalog version** in the same commit as any `launch` change. The `refresh()` path only takes a strictly higher version.

## Suggested order

1. Catalog version 4, no rebuild needed: duckstation, pcsx2, ryujinx, azahar, melonds, flycast, mupen64plus_fz, plus `linux_rmg` `--quit-after-emulation`.
   - Before pushing: try each in the VM lab, or on a host with the emulator installed, with one game per emulator.
   - Add each new entry to `launchArgumentsComeFromTheDatabase`.
2. RPCS3, after confirming how PS3 games are stored on desktop.
3. The "can't launch yet" marking and the catalog test (next release).
4. The `{romdir}`/`{romname}` placeholders for MAME and ScummVM, then Vita3K (next release).
