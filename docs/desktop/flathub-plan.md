# Flathub submission plan (Phase 5, step 7)

Started 2026-10-27, while step 5 waits for the owner's checklist (owner OK). Flathub builds offline in its own sandbox from sources listed in the manifest, then lints the manifest, the repo and the metainfo.

## Lint status (`flatpak-builder-lint`, alpha manifest)

| Finding | Kind | Plan |
|---|---|---|
| `finish-args-contains-both-x11-and-wayland` | error | **Fixed 2026-10-27:** `--socket=wayland` dropped. Compose Desktop on JDK 21 draws through AWT, which is X11-only and runs through XWayland under Wayland and gamescope. mpv renders in software into Compose, and SDL reads only input devices, so neither needs the Wayland socket. |
| `appid-url-not-reachable` | error | The id `io.github.stealthx3.ArgosyDesktop` must map to `github.com/stealthx3/argosydesktop`, which does not exist. Flathub's rule is `io.github.<owner>.<repo>`, with `-` in the repo name written as `_`: **`io.github.Stealthx3.argosy_desktop`**. **Owner decision:** renaming changes the Flatpak data folder (`~/.var/app/<id>`) and the alpha remote's ref, so alpha users would need a one-time reinstall and a data move. |
| `finish-args-flatpak-spawn-access` | error | Needs a Flathub exception. Justification below. |
| `metainfo-missing-screenshots` | error | Add 4–6 screenshots (Home, Library, game details, Settings, Quick Settings) at 16:9. Take them on the owner's real library with real covers, since the testbed has placeholder art. The images must be at stable URLs, either the repository's raw files after the phase-close push or `gh-pages`. |
| `appstream-screenshots-not-mirrored-in-ostree` | warning | Goes away on Flathub's own build, which mirrors the screenshots. |

### Exception request: `--talk-name=org.freedesktop.Flatpak`

Argosy is a launcher. Its job is to start the user's own emulators (Flatpak or native: RetroArch, Dolphin, PCSX2, and so on) and follow the play session: start, exit and play time. Inside the sandbox it can do that only through `flatpak-spawn --host`. Every command it runs is an emulator the user picked in Settings, with a ROM from the user's library. Lutris and Heroic Games Launcher ship on Flathub with the same permission for the same reason.

The emulator save folders (`--filesystem=~/.var/app/<emulator>` and the XDG config/data paths) are listed one by one, not `home`. Save sync reads and writes them in place.

## Offline from-source build

Today's alpha packages a jlink distributable built on the host. Flathub needs the whole build to run inside `flatpak-builder` with no network:

1. **Desktop-only Gradle build (done 2026-10-27).** Two measures together are enough:
   - `ANDROID_HOME` points at an empty folder. The Android Gradle plugin only checks that the SDK folder exists while it configures; nothing Android ever runs.
   - `-Pargosy.desktopOnly=true` leaves out `:sigil` and `:libretrodroid`, the two modules that want the NDK at configure time. `shared` and `app` reference them through `findProject`.
   Proven from a clean copy with no `local.properties`: `ANDROID_HOME=<empty> ./gradlew -Pargosy.desktopOnly=true :desktopApp:createDistributable` builds the distributable. The Android Gradle plugin jars are still resolved, as plugin artifacts. Whether any Android library is resolved, the step 3 dependency list will show.
2. **JDK.** Add `org.freedesktop.Sdk.Extension.openjdk21` under `sdk-extensions`. This replaces the host JDK, and it also provides the JNI headers that `build-alpha.sh` stages today for the `sigil-jni` module. **Owner OK needed** to install the extension on this machine for local test builds.
3. **Dependencies as sources.** Generate a `gradle-sources.json` of every Maven artifact the desktop build resolves, using `flatpak-gradle-generator` or Gradle's `--write-verification-metadata` turned into flatpak `file` sources. Then build with `--offline` against a local Maven repository in the build directory. Kotlin/Native and Android artifacts must not appear, which step 1 ensures.
4. **Natives.** `sigil` (git submodule) and libmpv, libass and libplacebo already build from source as modules. SDL3 comes from the runtime. Skiko's native library ships inside its Maven jar, so it is covered by step 3.
5. **CI.** A GitHub Actions job runs `flatpak-builder` with the generated sources for x86_64 and aarch64 (`flatpak/flatpak-github-actions`), plus `flatpak-builder-lint` on the manifest and the repo.
6. **Submission.** Fork `flathub/flathub`, add the manifest, the generated sources and the metainfo on a `new-pr` branch, and open the PR. **Owner go-ahead needed** (it posts to a third-party repository).
