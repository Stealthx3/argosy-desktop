# Submitting IceBreaker to Flathub: checklist and manual manifest guide

For the owner. Written by Claude (AI) on 2026-11-27, from Flathub's
[Submission](https://docs.flathub.org/docs/for-app-authors/submission) and
[Requirements](https://docs.flathub.org/docs/for-app-authors/requirements) pages as they read on that
date. Flathub's pages are the authority; where this guide and Flathub disagree, follow Flathub.

## Read this first: the AI rules for the submission

Flathub's [Generative AI policy](https://docs.flathub.org/docs/for-app-authors/requirements#generative-ai-policy)
sets these rules:

- **The manifest must not contain AI-generated or AI-assisted content**, and disclosure does not
  exempt it. Write every line of the manifest yourself, and understand each one.
- **The PR is yours:** opening it, its commit messages, its description, and every review reply.
  AI tools must not write any of these.
- **Disclose AI-generated material** in the app and its packaging, naming the parts and roughly how
  much. The disclosure for this project is [`AI-DISCLOSURE.md`](../../AI-DISCLOSURE.md). Check it,
  fill in the parts marked for you, and link it from your PR.

This guide was written by an AI. It contains no manifest text: it tells you what the manifest has to
cover and where to look each value up. Flathub may still count a manifest written with an AI-written
guide as "AI-assisted". So:

- don't copy anything into the manifest from this guide or from the repo's own manifests (both were
  written with Claude);
- say in the disclosure that you used this guide;
- if you're unsure, ask Flathub before you start, in the
  [Matrix room](https://matrix.to/#/#flathub:matrix.org) or a
  [Flathub issue](https://github.com/flathub/flathub/issues).

## Checklist

✅ done and checked · ⚠️ needs a decision or a fix before you submit · ⬜ yours to do

### Inclusion policy

| Item | Status | Notes |
|---|---|---|
| Functional, not a minimal wrapper | ✅ | A full RomM client: library, downloads, save sync, play sessions, controller UI. |
| Host-dependent apps | ⚠️ | Launching emulators goes through `flatpak-spawn --host` (`HostShell.kt`). This is the biggest review risk: see "Review risks". |
| Duplicate or conflicting submissions | ✅ | Argosy isn't on Flathub, and upstream ships no Flatpak. The icesuite-test build is your own. |
| Emulation or translation layers (Wine and similar) | ✅ | Doesn't apply: IceBreaker is a native JVM app. It starts the user's emulators but isn't one itself. |
| Latest runtime | ✅ | Moved to `26.08` (2026-11-27), and the repo's manifests and CI image follow. Tested: the Flathub-style offline build of 1.0.0 builds on 26.08 (17 min), starts into setup, and libmpv picks up the runtime's full codecs (`codecs-extra`, FFmpeg 8). Video playback itself wasn't exercised. |
| Development history | ⚠️ | The port has existed since 2026-10-16, with 381 commits and one tag. Reviewers decide whether that's enough; upstream's long history helps. |
| Generative AI disclosure | ⚠️ | [`AI-DISCLOSURE.md`](../../AI-DISCLOSURE.md) is drafted; you finish and confirm it. |

### App ID, license, metadata (in the app repo: done where marked)

| Item | Status | Notes |
|---|---|---|
| App ID rules | ✅ | `io.github.stealthx3.icebreaker-client` has 4 components, matches the metainfo, and maps to `https://github.com/Stealthx3/icebreaker-client`, which is reachable (GitHub ignores case in user names). Flathub verifies `io.github.` IDs through your GitHub sign-in. |
| License declared | ✅ | `GPL-3.0-only` in the metainfo; `LICENSE` is the GPL v3 text. |
| License files installed for every module | ⚠️ | The 26.08 test build installs libass's, libplacebo's and IceBreaker's. mpv, sigil (MPL-2.0) and the Steam helper install none. Your manifest must install each one under `/app/share/licenses/io.github.stealthx3.icebreaker-client/<module>`. |
| Metainfo passes validation | ✅ | `flatpak-builder-lint appstream` and `appstreamcli validate --pedantic` both pass. The only pedantic note is the capital letters in the ID, which is normal for GitHub IDs. |
| Screenshots | ✅ | 4 screenshots with captions, pinned to the tag `icebreaker-v1.0.0` (pushed 2026-11-27; all four links load). |
| Desktop file | ✅ | `desktop-file-validate` passes. |
| Icons | ✅ | Scalable SVG, PNGs up to 512×512, and a symbolic icon. |
| Name and icon are distinct; no implied affiliation | ⚠️ | The description credits Argosy and the RomM team and says IceBreaker isn't affiliated with or endorsed by rommapp.  **Found 2026-11-28:** Flathub already has a game named "icebreaker" (`org.mattdm.icebreaker`). The app IDs differ, but reviewers may ask about the shared name; decide before you submit whether to keep "IceBreaker" or give the store name a qualifier, and mention it in your PR. |
| Metadata lives in the app's own repo | ✅ | `desktopApp/flatpak/` (desktop file, metainfo, icons). |
| Stable release | ✅ | 1.0.0 (owner, 2026-11-27) is a stable release in the metainfo, and the beta line is gone. Earlier releases stay marked `development`, as they were. |
| Localisation | ⚠️ | Flathub bars machine-generated and mixed translations. 157 of the port's strings were translated by Claude into each of 6 languages, and Brazilian Portuguese shows those 157 in English. **Owner decision 2026-11-27: keep them and disclose them** (`AI-DISCLOSURE.md` names them). Risk: this rule is worded as a ban, not left to reviewers' discretion. If a reviewer objects, the fallback is an English-only desktop build, which Claude can make. |

### Build and packaging

| Item | Status | Notes |
|---|---|---|
| No network during the build | ✅ | The offline Gradle build works (CI x86_64 and aarch64 green on `6e7234b8b`, runtime 26.08). |
| aarch64 | ✅ | The arm64 Compose/Skiko files are in the sources list; the CI aarch64 build on 26.08 passed at `6e7234b8b` (2026-11-27). |
| Built from source | ✅ | The app, libass, libplacebo, mpv and sigil build from source. Java libraries come from Maven as jars, the usual practice for JVM apps. The JRE comes from the `openjdk21` SDK extension. |
| Dependency sources file | ⚠️ | Produced by Claude-written scripts (`regen-gradle-sources.sh`, `gradle-sources.py`). No existing tool fits this build, so this was decided 2026-11-27 and is disclosed. You run the generator yourself (step 4). |
| Patches | ✅ | None. |
| Manifest, sources file, PR | ⬜ | Yours (steps below). |

## Decide these before you write anything

1. **Release version and status: done.** You chose 1.0.0. The version, the stable metainfo release and
   the pinned screenshots are in the repo. The tag `icebreaker-v1.0.0` is pushed (2026-11-27, at
   `6e7234b8b`, the commit CI built on both architectures); point your manifest at that tag.
   `icebreaker-v0.5.0` doesn't have the aarch64 fix.
2. **Translations: decided.** You chose to keep them and disclose them. Mention them in your PR; if a
   reviewer asks, the options are native-speaker review or an English-only desktop build.
3. **Runtime 26.08: done.** Tested and adopted; see the checklist.
4. **Permissions.** Each one needs a reason in your PR. See "Review risks" for the ones to rethink.

## Step by step

### 1. Tools

Install `org.flatpak.Builder` from Flathub and add the Flathub remote user-wide, as described in
Flathub's Submission page, "Build and install".

### 2. Fork and branch

Fork `flathub/flathub` with "Copy the master branch only" **unchecked**. Clone the `new-pr` branch and
make a branch from it for the submission. The PR goes against `new-pr`, never against `master`.

### 3. Write the manifest yourself

Name it `io.github.stealthx3.icebreaker-client.yml` (or `.json`) and put it at the top level of the
submission. Follow Flathub's style guide on the Requirements page: two-space YAML indentation, keys
in the order the guide lists, and blank lines between modules.

What the manifest has to describe, in the order of Flathub's key list:

- **ID, runtime and SDK:** the freedesktop Platform and Sdk at the latest version, plus the
  `org.freedesktop.Sdk.Extension.openjdk21` SDK extension. The app runs on a JRE that the extension
  installs into the app.
- **Command:** the launcher script `desktopApp/flatpak/icebreaker-jre.sh`, installed as the app's
  command.
- **Permissions (finish-args):** decide each one yourself.
  - `IceBreaker → Settings → Flatpak permissions` lists what the app checks for at runtime.
  - `docs/desktop/flathub-plan.md` explains each permission.
  - The save-folder list is in `HostSavePaths` / `RetroArchHostLayout` in
    `shared/src/desktopMain/`.
- **Codecs: nothing to add.** Video wallpapers and trailers play through libmpv. Since 25.08 the
  freedesktop Platform brings the full codecs itself, through its own `codecs-extra` extension
  (branch `<version>-extra`). The old `ffmpeg-full` extension has no 25.08 or 26.08 branch, so don't
  add it. The repo's manifests carried a dead `ffmpeg-full` entry until 2026-11-27.
- **Modules, in build order:**
  1. **libass**: subtitle rendering, needed by mpv. Source: the libass release tarball (look up the
     latest release and its checksum).
  2. **libplacebo**: mpv's renderer, built with OpenGL only. Source: the videolan git repository at a
     release tag and commit.
  3. **mpv**: the library only (`libmpv`), with the player, Lua, JavaScript and disc support turned
     off. Source: mpv's git repository at a release tag and commit. Install its license files:
     they're named `LICENSE.GPL` and `LICENSE.LGPL`, which flatpak-builder doesn't pick up by itself.
  4. **sigil JNI library**: sigil is the `sigil/` submodule (MPL-2.0). Build its JNI host library
     with its own CMake (option `SIGIL_BUILD_JNI_HOST`, target `sigil_jni_host`) against the
     openjdk21 headers, and install `libsigil-jni.so` next to the app's jars. Source: the sigil git
     repository at the commit the IceBreaker release pins (`git rev-parse <tag>:sigil`).
  5. **Steam helper**: `packaging/steam/add_to_steam.py`, installed as
     `io.github.stealthx3.icebreaker-client-steam`, plus `steam-shortcut.json` and the five PNGs, installed
     under `share/io.github.stealthx3.icebreaker-client/steam`. Source: the IceBreaker repo at your release
     tag (subfolder `packaging/steam`). Install the repo's `LICENSE` for this module.
  6. **IceBreaker**:
     - **Source:** the IceBreaker repo at your release tag and commit, with submodules (the build
       needs `sigil`), plus the Gradle 8.9 distribution and the dependency sources file (step 4).
     - **Build:** Gradle runs offline with `--offline`, using the init script
       `desktopApp/flatpak/offline.init.gradle.kts`. Run
       `-Pargosy.desktopOnly=true :desktopApp:createDistributable`. Set `ANDROID_HOME` to an empty
       folder and point `ARGOSY_OFFLINE_REPOSITORY` at the folder the sources file lays out.
     - **Install:**
       - copy the distributable from `desktopApp/build/compose/binaries/main/app/icebreaker/`, leaving
         out its own `bin/` and bundled `lib/runtime`;
       - run the openjdk21 extension's `install.sh`;
       - install the launcher script;
       - install the desktop file, metainfo and icons from `desktopApp/flatpak/`;
       - install `LICENSE`.

### 4. The dependency sources file: run the generator yourself

The build needs every Maven file listed as a source. **Decided 2026-11-27:** the list comes from the
repository's own generator, which Claude wrote. `AI-DISCLOSURE.md` says so and explains why no
existing tool fits:
- Flathub's own `flatpak-gradle-generator.py` produces a flat folder without Gradle's `.module`
  metadata;
- the meshtastic plugin needs Gradle 9;
- jwharm's plugin missed about 370 of the 1,452 files.

Run it yourself on the release you submit, so the list in your PR is one you produced.

**Before you start**
- You need `git`, `rsync`, `python3`, a network connection, and JDK 21. The script uses
  `~/.jdks/jdk-21.0.12.1+1`, or the JDK in `JAVA_HOME` if you set it. It stops if that isn't a JDK 21.
- It downloads about 1.2 GB: a full desktop build from an empty Gradle cache, into a scratch folder.
- The release tag must already be on GitHub.

**Steps**

1. Get a clean copy of the release. The script copies the files of the copy you run it in, uncommitted
   changes included, so don't run it in a working copy with changes.

   ```bash
   git clone --recurse-submodules --branch icebreaker-v1.0.0 https://github.com/Stealthx3/icebreaker-client.git ~/icebreaker-release
   ```

2. Run the generator from that copy. Give it a new folder for its scratch work; it deletes and
   recreates that folder, and refuses one that holds anything else.

   ```bash
   cd ~/icebreaker-release && scripts/desktop/flatpak/regen-gradle-sources.sh ~/icebreaker-gradle-capture
   ```

   With your JDK 21 elsewhere, put `JAVA_HOME=/path/to/jdk-21` in front of the command.

3. Read what it prints. The last lines say `<N> sources, <M> skipped`, then list each skipped URL.
   Only `maven-metadata.xml` files should be skipped. Anything else comes from a repository the
   generator doesn't know about, and needs fixing before you submit.

4. Check the result, `desktopApp/flatpak/generated/gradle-sources.json`:

   ```bash
   git diff --stat -- desktopApp/flatpak/generated/gradle-sources.json
   ```

   ```bash
   grep -c '"only-arches"' desktopApp/flatpak/generated/gradle-sources.json
   ```

   - **No diff:** the list matches the one committed for the release.
   - **A diff:** expected only if dependencies changed. Look at what changed.
   - **The count should be 6:** three x86_64 files and their three aarch64 counterparts.

5. Copy the file into your submission next to your manifest, and list it as a source of the
   IceBreaker module. Its entries download into `offline-repository/`, the folder the build's
   `ARGOSY_OFFLINE_REPOSITORY` variable must point at.

6. Rerun it for every update whose dependencies change, from that update's tag.

Flathub allows a separate sources file because it's generated by a tool.

### 5. Build, run and lint locally

Use the commands from Flathub's Submission page: `flathub-build --install` with your manifest, then
`flatpak run io.github.stealthx3.icebreaker-client`, then `flatpak-builder-lint` in `manifest` mode and in
`repo` mode. Check:

- the app starts, signs in to RomM, downloads a game and launches it;
- `/app/share/licenses/io.github.stealthx3.icebreaker-client/` has a folder for every module.

Lint findings to expect:

- `finish-args-flatpak-spawn-access` (error): needs an exception from Flathub. Explain it in the PR.
- `appstream-screenshots-not-mirrored-in-ostree` (repo mode): goes away on Flathub's own build.

### 6. The pull request

- Commit only your manifest and the sources file (no source code, no build output), and push.
- Open the PR on github.com against `new-pr`, titled `Add io.github.stealthx3.icebreaker-client`.
- Keep Flathub's PR template: PRs that remove it are closed without review.
- In your own words, say why each permission is needed, and link `AI-DISCLOSURE.md`.
- Answer reviewers yourself. Once comments are resolved, you can comment `bot, build` to start a
  test build.

## Review risks

- **Host access (`--talk-name=org.freedesktop.Flatpak`).** Without it IceBreaker can't start
  emulators. Our notes say Lutris and Heroic Games Launcher ship with the same permission. Check that
  on their Flathub manifests before you cite them.
- **`--device=all`.** It covers both the controllers and the GPU. The repo manifests dropped the
  redundant `--device=dri` on 2026-11-28 (your decision); explain `all` in your PR.
- **Broad file access:** `/mnt`, `/media` and `/run/media` (ROM libraries on other drives), the
  Steam folders (Add to Steam), and one path per emulator for save sync. Each needs a reason. File
  access through the portal can't cover save sync, which writes in place.
- **Beta status** and the **young port history** (see the checklist).
- **AI share:** most of the desktop port was written with Claude, and Flathub rejects at its
  discretion. The disclosure tells reviewers exactly what that covers.

## After the merge

You'll be invited to the new `flathub/io.github.stealthx3.icebreaker-client` repo (2FA required; accept
within a week). Updates go there, not through a new submission. Users on icesuite-test move by
installing from Flathub: an end-of-life rebase only works within one remote.
