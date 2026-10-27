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
3. **Dependencies as sources (done 2026-10-27).**
   - `scripts/desktop/flatpak/regen-gradle-sources.sh` runs the desktop-only build in a scratch copy with an empty Gradle home and logs every download. `gradle-sources.py` turns those downloads into `desktopApp/flatpak/generated/gradle-sources.json`: 1,448 `file` sources from Google Maven and Maven Central, laid out as one Maven repository under `offline-repository/`, with checksums from Gradle's cache.
   - `desktopApp/flatpak/offline.init.gradle.kts` replaces every repository with that one, plus the in-tree `libs/maven`.
   - Proof: a fresh Gradle home with `--offline` and only those files builds the full distributable, with every checksum matching.
   - Rerun the script after any dependency or plugin change.
   - The from-source manifest is `desktopApp/flatpak/io.github.stealthx3.ArgosyDesktop.flathub.yml`, with the openjdk21 extension and the Gradle 8.9 zip pinned by its published sha256.
   - **Built locally 2026-10-27:** `flatpak-builder` ran all five modules offline inside the sandbox, and Gradle printed `BUILD SUCCESSFUL in 4m 59s`.
     - Manifest lint gives info only: git sources without a URL (they become release URLs), and runtime 26.08 available.
     - Repo lint shows the same three owner-gated errors as the alpha.
     - Started with `flatpak build` on scratch XDG data, the app stays up and initialises.
   - **Open:** the Flathub build prints `pure virtual method called` / `terminate called without an active exception` at startup, and the app keeps running. The alpha build dir run the same way does not, and it still appears when the run is ended with SIGKILL. Lead: the jpackage launcher of the extension's JDK 21.0.13, where the alpha uses the host's 21.0.12; it may abort in a process that is not the JVM. To check: run the build-dir launcher under `strace -f`, or compare against a host-JDK 21.0.13 jpackage.
4. **Natives.** `sigil` (git submodule) and libmpv, libass and libplacebo already build from source as modules. SDL3 comes from the runtime. Skiko's native library ships inside its Maven jar, so it is covered by step 3.
5. **CI (drafted 2026-10-27).** `.github/workflows/flatpak.yml` builds the `.flathub.yml` manifest for x86_64 and aarch64 (QEMU) in the freedesktop 25.08 builder image, using the `flatpak/flatpak-github-actions` flatpak-builder action. It uploads a bundle per architecture and lints the manifest in a separate job. It runs on pushes to `main` that touch the desktop build, and on demand. Pushes happen only at phase close, so its first run is the Phase 5 close push. **Untested.**
6. **Submission.** Fork `flathub/flathub`, add the manifest, the generated sources and the metainfo on a `new-pr` branch, and open the PR. **Owner go-ahead needed** (it posts to a third-party repository).
