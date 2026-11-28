# AI disclosure

IceBreaker is a Linux desktop port of [Argosy](https://github.com/rommapp/argosy-launcher), the
Android game launcher for RomM. Most of the port, meaning the code, packaging and documentation that
make Argosy run on the Linux desktop, was written with an AI coding assistant, Anthropic's Claude
(through Claude Code). This page says what Claude produced, roughly how much, what it did not
produce, and how the work was directed and checked.

Figures are as of 2026-11-27. The port began on 2026-10-16.

> **Owner: before linking this page, fill in or confirm every line marked "Owner to confirm", then
> remove this note.** This page was drafted by Claude from the repository's history and logs.

## How the port was made

- **The owner directed the work.** The plan was split into phases and steps (`PORT_PLAN.md`), with
  every decision and its reasoning recorded in `DEVLOG.md`. The owner set the scope, made the product
  and design decisions, approved each phase, and decided what shipped.
- **Claude wrote the code, scripts, packaging and documentation**, ran the builds and tests, drove
  the app on test machines to check behaviour, and recorded the results.
- **Checks:** automated tests (unit tests for the desktop code, upstream's Android tests, lint and
  CI), runs on a local RomM test server, and testing on real hardware by the owner and beta testers.
- **Owner review:** the owner read the code in the riskiest areas (save sync, file access and the
  Steam integration) and judged the rest by testing it. Not every line was read by a person.

## What Claude produced

### Port history

379 of the port's 381 commits (not counting merges of upstream releases) were made in Claude Code
sessions and carry a `Co-Authored-By: Claude` trailer. The other 2 are README edits made outside Claude
Code, in GitHub's web editor.

### Application code

| Part | Rough size | What it is |
|---|---|---|
| Desktop code: `shared/src/desktopMain`, `desktopApp/src` | about 13,600 lines of Kotlin (all of it) | The Linux side of the app, written new for the port: emulator detection and launch, Flatpak sandbox handling, desktop file access, save-folder layouts, windows and input, Steam Game Mode support, Add to Steam, data migration for the rename. |
| Changes to Argosy's shared code: `shared/src/commonMain`, `shared/src/androidMain` | about 31,000 added or changed lines, in a codebase of about 410,000 | Upstream's code moved into a Kotlin Multiplatform module and adapted to build for desktop too: platform seams, desktop fallbacks, and some fixes. Most of that codebase is unchanged upstream code. |
| Desktop tests: `shared/src/desktopTest` | about 2,800 lines | Unit tests for the desktop code. |

Put another way: of about 424,000 lines of Kotlin in the shipped app, roughly 45,000 (about 10%) were
written or changed by Claude. The rest is Argosy's own code (see "Upstream" below).

### Text the user sees

- **English:** 260 interface strings added for the desktop, the desktop file, the AppStream metainfo
  (description and release notes), and the setup and settings text specific to the desktop.
- **Translations:** 157 of those strings were translated by Claude into Chinese (Simplified and
  Traditional), German, Spanish, French, Hindi and Russian. They are machine translations and have not
  been checked by native speakers. Brazilian Portuguese doesn't have them, so they show in English
  there. The rest of each translation is upstream Argosy's. The owner chose to ship these
  translations rather than fall back to English; native speakers' corrections are welcome as issues
  or pull requests.
- **Screenshots:** the four store screenshots were captured by Claude from the running app. They show
  the real app and were not generated as images.

### Packaging and tooling (in this repository)

- **This repository's Flatpak manifests**, used for test builds on the IceSuite test repository.
  These are not the Flathub manifest (see below).
- The launcher scripts and the offline Gradle setup (`desktopApp/flatpak/offline.init.gradle.kts`).
- The generator for the Gradle dependency list (see the next section).
- CI workflow changes, build, test and release scripts (`scripts/desktop/`), and prototypes
  (`spikes/`, not part of the app).

### The Gradle dependency list in the Flathub submission

Flathub builds offline, so every Java library the build needs must be listed as a source, with its
download URL and checksum. That list (`gradle-sources.json`, about 1,450 entries) is produced by two
scripts **written by Claude**:

- `scripts/desktop/flatpak/regen-gradle-sources.sh`: runs a normal desktop-only build in a scratch
  copy of the repository, with an empty Gradle cache, and logs every file Gradle downloads.
- `scripts/desktop/flatpak/gradle-sources.py`: turns that log into the source list, taking each
  checksum from the file Gradle downloaded. Builds run on x86_64, so it also adds the arm64
  counterparts of the three architecture-specific files (Compose Desktop and Skiko), checked against
  the arm64 Compose POM.

**What the list contains:** only URLs on Maven Central and Google's Maven repository, with checksums,
recorded from a real build. Claude didn't write any entry by hand.

**Who ran it:** **Owner to confirm.** For example: "The owner ran the generator by hand on the release
tag to produce the list in the submission."

**Why not an existing tool:** the owner asked for an existing tool first, and three were checked on
2026-11-27:

- **`flatpak-gradle-generator.py`** from Flathub's
  [flatpak-builder-tools](https://github.com/flatpak/flatpak-builder-tools/tree/master/gradle) takes
  the same approach: it reads the URLs from a real build's log. It doesn't fit this build, for three
  reasons:
  - it puts every file in one flat folder, but the build resolves libraries from a Maven-layout
    repository;
  - it keeps only `.jar` and `.pom` files, dropping Gradle's `.module` metadata. That's 400 of the
    1,452 files, and Kotlin Multiplatform and Compose libraries need it to pick their JVM variant;
  - its architecture handling matches only `linux-x86_64`-style `.exe` names, not Compose's
    `linux-x64` / `linux-arm64` files.

  Claude's two scripts are, in effect, that approach with those three gaps closed.

- [meshtastic/gradle-flatpak-sources](https://github.com/meshtastic/gradle-flatpak-sources) needs
  Gradle 9. This project builds with Gradle 8.9, which it shares with upstream Argosy.
- [jwharm/flatpak-gradle-generator](https://github.com/jwharm/flatpak-gradle-generator) 1.9.0 was
  tried. Its lists covered only about 1,080 of the 1,452 files a real offline build downloads. It
  missed two kinds of file:
  - metadata for the app's Android libraries. IceBreaker shares one Gradle build with the Android app,
    so even a desktop-only build configures the Android projects and reads that metadata, but those
    configurations can't be resolved without Android;
  - tools that Gradle plugins (KSP, Room, Compose) fetch through their own private configurations.

  Including the Android configurations made the gap larger. Making the plugin's list complete would
  first need the desktop build separated from the Android build.

Recording what a real build downloads, as these scripts do, covers all of these cases.

### Artwork and the Steam helper (made with the owner)

- **Artwork** (app icon, splash, About logo, Steam artwork) comes from the owner's IceBreaker brand
  kit. The owner created the core assets: the first icons and drafts of the other pieces. Claude then
  refined those and expanded them into the full kit, working from the owner's originals.
- **Steam helper** (`packaging/steam/add_to_steam.py`) was written by the owner and Claude together,
  for the owner's shared Steam shortcut standard, which the owner's other apps also use. IceBreaker
  carries it unchanged. Claude wrote IceBreaker's integration of it.

### Documentation

`README.md` (except the 2 web-editor edits), `PORT_PLAN.md`, `DEVLOG.md`, the `docs/desktop/` folder
(including the Flathub submission guide), and this page.

## Where Claude's involvement ends

- **The Flathub submission:** the owner wrote the Flathub manifest by hand and opened the pull request
  themself, with its description and all replies to reviewers. Claude didn't write or open any of it.
  The owner wrote the manifest with a Claude-written guide (`docs/desktop/flathub-submission-guide.md`)
  as reference; no text was copied from it or from this repository's manifests.
  **Owner to confirm:** that this matches what you did. The dependency list in the submission is the
  output of the Claude-written generator described above.
- **Upstream Argosy:** most of the app's code is Argosy's, written by its developers, and is not
  covered by the figures above. Upstream also uses AI tools: its repository has agent instructions
  (`AGENTS.md`, `.claude/`), and 98 of its 3,665 commits carry AI co-author trailers. We don't know how
  much of Argosy is AI-written. Argosy's maintainers are the source for that.
- **Third-party components** built into the Flatpak (libass, libplacebo, mpv, sigil, the Java
  libraries) are their projects' own work.
- **Bug reports and one fix:** beta testers reported issues. One supplied a patch for play-session
  sync (issue #4), which Claude applied and extended.

## Questions

Open an issue at https://github.com/Stealthx3/icebreaker-client/issues.
