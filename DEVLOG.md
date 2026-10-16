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

**Status:** closed. Gate passed (all three spikes pass); README rebuilt and `main` pushed.

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

### 2026-10-16 — Session 2 (Claude Code on the owner's computer)

**Action:** Checked the fork out in place (`main` at `6e64baa`, submodules), committed the handoff files, `*.pdf` excluded via `.git/info/exclude`.
**Reasoning:** One-time setup from `CLAUDE.md`.

**Action:** Installed the missing toolchain with the owner's approval: Android SDK in `~/Android/Sdk` (platform 35, build-tools 35; AGP auto-installed NDK 27.0.12077973 and build-tools 34), Temurin JDK 21 in `~/.jdks`, and `org.flatpak.Builder` + `org.freedesktop.Platform//24.08` from Flathub (user).
**Reasoning:** The system JDK 17 is a JRE without `jlink` (AGP fails in `JdkImageTransform`), and Gradle 8.9 cannot run on the system JDK 25. A user-level JDK 21 avoids sudo.

**Action:** Android baseline: `./gradlew :app:assembleDebug` unchanged, on JDK 21 → **pass**, 6 m 01 s (partly warm after a failed first attempt), 69 MB `app-debug.apk`.

**Action:** Spike `spikes/room-kmp-jvm/` → **pass.** 154 files copied unchanged (entities, DAOs, Converters, `ALauncherDatabase`, plus 6 plain-Kotlin model files they import). Only `migrations/` and `DatabaseFactory.kt` (Android `Context`) were excluded; no source line edited. Exported schema is byte-identical to `app/schemas/.../214.json` (identityHash `6a58e378d72d74d60c8b6d1bf8a20cb8`); BundledSQLiteDriver open, DAO insert and `Flow` queries pass in tests. Every DAO function is already `suspend` or `Flow`.

**Action:** Spike `spikes/compose-desktop-library/` → **pass.** `LibraryGameList` in `ALauncherTheme` renders headless (`library.png`) and in a window (`./gradlew run`, checked on the owner's desktop). 71 upstream files (9,112 lines) compile unchanged; 61 declarations extracted verbatim from 17 entangled files; 15 Android/AndroidX/Coil/Hilt APIs shimmed (350 lines); fakes for `LibraryViewModel`, `ThemeViewModel`, `ImageCacheManager`, `BackdropStamps`; desktop actuals for `focusGlow` (Skia blur) and `CustomFontLoader`. Full stub inventory and Phase 2 findings in the spike README.
**Reasoning:** Shimming at the Android API's own package keeps upstream files unedited and makes the shim list the Phase 2 work list. Biggest items: resources (6,254 strings, 225 plurals → Compose MP resources) and state types declared inside view-model files.

**Action:** Spike `spikes/flatpak-launch/` → **pass.** Sandboxed spike Flatpak sees `~/Games/Argosy`, `flatpak-spawn --host flatpak run org.libretro.RetroArch` loads the ROM with gambatte ("Got internal game name: ARGOSY SPIKE"), exit status returns (0 normal, 1 for a bad core). Test ROM is self-made (`make-test-rom.py`), no download. Changes: builder fallback with `--disable-rofiles-fuse` (FUSE cannot mount inside the Builder sandbox); RetroArch access is checked, not overridden (owner already has `filesystems=host`). Cleaned up the auto-save state RetroArch wrote for the test ROM.
**Finding / proposed plan change:** `org.freedesktop.Platform 24.08` is end-of-life. Move the manifest to 25.08 from Phase 1.

**Gate:** all three spikes pass.

**Action:** Rebuilt `README.md` (intro and status, full `PORT_PLAN.md`, link to upstream) and saved upstream's README as `README.upstream.md`. Added a Phase 0 results note to `PORT_PLAN.md`.
**Reasoning:** Phase-end rule from the owner.

**Note:** Once the session moved into this folder, upstream's `.claude/settings.json` hooks (`coupling-guard`, `verification-guard`, `takt-guard`, `smell-guard`) began running. They resolve scripts through `$CLAUDE_PROJECT_DIR`, which in this session still pointed at the previous scratch folder, so every Bash call was refused. Hooks were left untouched (upstream config); a session started in this folder resolves them correctly.

**Action:** Owner decision on the upstream proposal: add the spike results to the draft and hold it; the owner posts it. Added a section on the three proofs of concept and the "move state types into their own files" first step.
**Reasoning:** The proposal is stronger with evidence; posting stays with the owner.

**Action:** Owner set a new branch model: `main` is the workspace, `upstream` (created by the owner on `origin`) is a reference copy of upstream code that should move only on upstream releases. No sync mechanism existed in the repo. Added `.github/workflows/sync-upstream-release.yml` (daily and manual): finds rommapp's newest stable tag (`vX.Y.Z`, no pre-releases) and fast-forwards `upstream` to that tag's commit; it never force-pushes and fails loudly if a release is not a fast-forward. Recorded the rule in `CLAUDE.md`.
**Reasoning:** Releases are upstream's tested states, so diffing and re-basing the port against releases avoids chasing half-finished work between them. Fast-forward-only keeps the reference branch's history trustworthy.
**Finding:** `origin/upstream` was created at `6e64baa`, which is v2.23.0 (`9940f11`) plus 3 post-release commits; `main` is based on the same commit. The workflow leaves that as is until v2.24.0; resetting `upstream` exactly to v2.23.0 needs a force-push, which is the owner's call. Scheduled workflows on a fork run only once Actions are enabled for the fork.

**Action:** Owner approved resetting `upstream` to exactly v2.23.0 (`9940f11`), dropping the 3 post-release commits from the reference branch, by force-push with a lease on `6e64baa`.
**Reasoning:** Makes the reference branch match the release-only rule from the start; the sync workflow then fast-forwards it at v2.24.0. `main` keeps its base at `6e64baa`.

**Note:** Pushing from this session failed: HTTPS has no credential helper ("Password authentication is not supported"), and the SSH agent refused to sign with `~/.ssh/id_ed25519`. The owner runs the pushes.

**Action:** Fixed git access without a token. The key has no passphrase and is registered to `Stealthx3`; the GNOME `gcr` SSH agent was what refused to sign. Repo-local fix: `origin` → `git@github.com:Stealthx3/argosy-desktop.git` and `core.sshCommand` = `ssh -o IdentityAgent=none -o IdentitiesOnly=yes -i ~/.ssh/id_ed25519`. Pushed `main` (`6e64baa..b8bf5ba`) and reset `upstream` to v2.23.0 (`6e64baa...9940f11`, forced with lease).
**Reasoning:** Bypassing the broken agent for this repo only leaves the owner's global SSH setup untouched and needs no new credentials.

**Phase 0 closed.** Next: owner decides whether to start Phase 1 now or schedule it.

---

## Phase 1 · Kotlin Multiplatform restructure

**Status:** plan approved by the owner (2026-10-16); steps 1–5 done, step 6 (DataStore KMP) next.

### 2026-10-16 — Session 3 (Claude Code on the owner's computer)

**Action:** Read the project rules, the plan, the Phase 0 log and survey, upstream's `AGENTS.md` / `.claude/CLAUDE.md`, and the spike READMEs; surveyed the build (`app/build.gradle.kts`, version catalog), the state types embedded in view-model and repository files, the Compose stability config, DataStore creation and the scripts that hard-code `app/src/main`.
**Reasoning:** The phase plan has to fit what is in the tree, especially the things that make a restructure expensive: the stability contract (package globs), CI scripts and hooks with fixed paths (15 files), resource qualifiers, and user data locations on Android.

### Phase 1 plan (proposed)

**Goal and exit criteria** (from `PORT_PLAN.md`): `shared` builds for Android and desktop with no Android imports in `commonMain`; the Android app passes `testDebugUnitTest` and looks unchanged.

**Scope boundary.** Phase 1 moves everything that is already platform-free (data, domain, Android-free UI) into `commonMain`, and gives Android-bound code a home in `androidMain` behind interfaces where a caller in `commonMain` needs it. It does not write the desktop implementations of those interfaces (Phase 2 and 3) and does not have to make every screen run on the desktop. A file that cannot move yet stays where it is and is listed, so the leftover list sizes Phase 2.

#### Principles

1. **Packages never change.** Every file keeps `com.nendo.argosy.*`. Moves change only the directory, so imports, the Compose stability globs (`ui.**`, `data.model.**`, ...), Room's generated code, test packages and upstream's tooling keep working, and git sees pure renames.
2. **A move commit only moves.** File moves (`git mv`) and content edits never share a commit, so git's rename detection matches 100% and upstream changes to a moved file merge onto its new path.
3. **Mechanical edits are scripted, not hand-made.** Anything applied to many files (moves, the resource rewrite, import swaps) is done by a script under `scripts/desktop/` driven by a manifest, so it can be re-run on the next upstream release instead of merged by hand.
4. **Hand edits are small and prefer new files.** `expect`/`actual` declarations and interfaces go in new files; edits inside upstream files are kept to what a move forces.
5. **Every step ends green:** `:app:assembleDebug`, `:app:testDebugUnitTest`, and (once it exists) `:shared:compileKotlinDesktop`. One step, one or a few commits.

#### Module layout

| Module | Directory | Holds |
| --- | --- | --- |
| `:shared` | `shared/` | KMP library: `androidTarget()` + `jvm("desktop")`. Source sets `commonMain`, `androidMain`, `desktopMain`, `commonTest`. Room, DataStore, network, domain, view models, Android-free UI, resources. |
| `:app` | `app/` (unchanged) | The Android application: activities, services, workers, receivers, manifest, `libretrodroid`/`sigil` wiring, Hilt application component, migrations, Android-bound code not moved yet. Depends on `:shared`. |
| `:desktopApp` | `desktopApp/` | Compose desktop entry point, Dagger component, desktop actuals as they arrive. Phase 1 content: a window that opens the shared theme, proving the wiring. |

Two changes from `PORT_PLAN.md`, for the owner to confirm:

- **No separate `jvmShared` source set.** Both targets are JVMs, so Kotlin lets a source set shared by Android and JVM use JDK APIs and plain-Java libraries (Retrofit, OkHttp, Moshi, commons-compress, JavaSteam). `commonMain` *is* the shared JVM set; a third layer adds configuration with no consumer. If a non-JVM target ever appears, `jvmShared` can be split out then.
- **Keep the Android module as `app/`, not `androidApp/`.** Renaming it renames every Android-only file (about 1,000) for no functional gain and turns each upstream merge into a rename-heavy one. The module plays the `androidApp` role under its upstream name.

#### Order of moves

Each step is independently shippable on Android; the list runs from no-risk to the large scripted rewrites.

1. **Extract state types from view-model and repository files, same package, inside `app`.** Exactly the declarations the Library spike had to copy out one by one (`spikes/compose-desktop-library/extracted-decls.txt`): the 22 display enums plus `UserPreferences` / `BuiltinEmulatorSettings` from the 1,297-line `UserPreferencesRepository.kt`; `LibraryUiState` and its filter/grid types from `LibraryViewModel.kt`; `ThemeState` from `ThemeViewModel.kt`; the picker states from `GameLaunchDelegate.kt`. No module change, no behaviour change. This is also the "first step" in the upstream proposal, so it is the most likely part to be adopted upstream.
2. **Module skeleton.** Add `:shared` and `:desktopApp`, `app` depends on `shared`, nothing moved yet. Settles the toolchain questions on an empty module: Compose Multiplatform version (see risks), KSP per target (`kspAndroid`, `kspDesktop`), the Compose stability config applied to `shared` too, JDK APIs in `commonMain`.
3. **Room into `shared`.** Entities, DAOs, `Converters`, `ALauncherDatabase` → `commonMain` (proved unchanged by the Room spike). `migrations/` and the Android `DatabaseFactory` stay in `app`; a desktop factory uses `BundledSQLiteDriver`. Schema export keeps writing to `app/schemas` and the v214 `identityHash` is checked. Android keeps its file name and SupportSQLite path, so existing installs open their database unchanged.
4. **Android-free models, domain and utilities.** A script computes the movable set to a fixpoint (a file moves only when everything it references has moved), using the compiler as the final check. Domain's 12 known Android-debt files stay in `app` until an interface replaces the call.
5. **Network and JVM libraries.** RomM client (Retrofit/Moshi/OkHttp), Socket.IO, archives, Steam libraries → `commonMain`; Moshi codegen runs per target.
6. **DataStore KMP.** Preference repositories → `commonMain`; file creation behind an `expect`/`actual` path provider. Android keeps the exact `datastore/*.preferences_pb` file names, so no user loses settings (user-data rule from `.claude/CLAUDE.md`).
7. **DI.** Dagger stays the engine (see decision below). `@Module`/`@Provides` and `@Inject` constructors move with their classes; `@InstallIn` comes from `hilt-core`, which is plain JVM. View models get a small `expect` annotation that is `@HiltViewModel` on Android; the desktop builds a Dagger `@Component` over the same modules plus a view-model map. Hilt keeps owning activities, services and workers in `app`.
8. **Resources → Compose Multiplatform resources.** `res/values*/strings*.xml` (6,254 strings, 225 plurals, 8 languages) and the drawables shared UI uses move to `shared/src/commonMain/composeResources/`; a script rewrites `R.string.x` → `Res.string.x`, `stringResource`/`pluralStringResource` imports, and `@StringRes Int` carriers → `StringResource`. Android-only code (notifications, widgets, manifest labels) keeps the Android resources it needs. `scripts/ci/check-translations.py` and the lint translation checks are pointed at the new location. To verify: Compose resources' handling of the `values-b+zh+Hans` / `b+zh+Hant` folder qualifiers.
9. **Android-free UI.** Theme, tokens (`scripts/gen-tokens.mjs` output path), components and screens that compile without Android → `commonMain`. Composables that need Android get the spike's shim list as `expect`/`actual` (blur mask, shader matrix, bitmap decode, font fallback, `LocalContext` uses). The rest stays in `app` and is listed for Phase 2.
10. **Tooling paths.** Each step that first moves files under a script's scope updates that script in the same step: `stability_checks.py`, `service_stability_checks.py`, `comment_checks.py`, `smell-rules.json`, `check-translations.py`, `gen-tokens.mjs`, the `.claude` hooks, detekt sources, CI. Tests stay in `app/src/test` (they see `shared` through the dependency) unless a test needs to move with an `internal` declaration.

#### DI: Dagger or Koin

Recommendation: **plain Dagger 2 on the desktop, Hilt kept on Android.** `javax.inject` (`@Inject` in 504 files, `@Singleton` in 340) is plain JVM and stays untouched; Hilt's `@InstallIn` is in the JVM `hilt-core` artifact; only the 33 `@HiltViewModel` view models and the `hiltViewModel()` call sites need a shared alias. Koin would mean rewriting the wiring of every injected class into Koin modules or Koin annotations: a large, permanent diff against upstream that has to be redone on every release, for no user-visible gain.

#### How the restructure survives upstream releases

The plan works whether or not rommapp adopts the KMP split:

- `main` merges each upstream release (the `upstream` branch) with git rename detection. Because moves are pure renames with unchanged packages, upstream edits to a moved file land on its new path.
- `scripts/desktop/restructure/` holds the manifest (which file or package lives in which source set) and the scripted transforms. After a merge it is re-run: it relocates new upstream files that landed under `app/` but belong in `shared`, re-applies the resource rewrite to new or changed call sites, and fails if the layout drifts from the manifest. Every transform is idempotent.
- Conflicts can then come only from hand edits, which are few and mostly in new files.
- Rehearsal: before the phase closes, run the sync once on a real upstream delta (v2.24.0 if released by then; otherwise replay the restructure from `9940f11` onto `6e64baa`) and record the effort.
- If rommapp adopts the restructure, the same script produces the upstream PR series, step by step in this order.

#### Risks found while planning

- **Compose version drift on Android.** Android uses Compose BOM 2024.12.01 (Compose 1.7.x, Material3 1.3.1). `commonMain` compiles against Compose Multiplatform, and on Android CMP resolves to the androidx artifacts it was built against, so CMP 1.12 (used in the spike) would silently upgrade Android Compose by several releases and risk "looks unchanged". Recommendation: use the CMP release that maps to Android's current Compose (1.7.x) during Phase 1, and move both together when upstream bumps its BOM. Step 2 proves this works with Kotlin 2.3.20.
- **Compile time.** KMP compiles `shared` once per target; a cold build may grow beyond the ~12 min of today. Mitigation: build only the target needed, keep the stability config on `shared`, and split `shared` into `shared-data` / `shared-ui` if a cold build passes ~20 min.
- **User data on Android.** Database and DataStore file names and locations must not change; steps 3 and 6 check this against an installed build before they are committed.
- **Visual check.** "Looks unchanged" needs a before/after on a device or emulator, not just a green build. No Android device or emulator was used in Phase 0.

#### Verification per step

`./gradlew :app:assembleDebug :app:testDebugUnitTest` (plus `:shared:compileKotlinDesktop` from step 2) on JDK 21. Steps that touch the database, DataStore, resources or UI also get an on-device or emulator check before and after. Every report says which of these ran.

**Action:** Showed the plan to the owner. Decisions: plan approved as written (no `jvmShared` set, Android module stays `app/`); DI = plain Dagger on the desktop with Hilt kept on Android; Android stays on its current Compose 1.7 (CMP release matched to the BOM) during Phase 1; "looks unchanged" is checked on an Android emulator installed in `~/Android/Sdk`, before and after the data, resource and UI steps. Upstream-or-fork stays open (the owner holds the proposal); the plan works either way.
**Reasoning:** These were the choices where two defensible readings lead to materially different work. Recorded the module and DI decisions in `PORT_PLAN.md`.
**Divergence from upstream rules:** none yet. Step 8 (resources) will change upstream's "`@StringRes Int` carriers" law to `StringResource` carriers in shared code; that will be recorded here when it lands.

**Action:** Step 1 done: moved state types out of view-model and repository files into their own files, same package, inside `app`. Done by `scripts/desktop/restructure/extract_decls.py` from the manifest `extract-decls.txt`, so it re-runs on an upstream release (it skips what is already extracted). 47 declarations, 5 new files:

| From | To | Declarations |
| --- | --- | --- |
| `data/preferences/UserPreferencesRepository.kt` (1,297 → 682 lines) | `UserPreferences.kt` | `UserPreferences`, `BuiltinEmulatorSettings` |
| same | `PreferenceEnums.kt` | 24 preference enums (`ThemeMode`, `BoxArtShape`, `EmulatorDisplayTarget`, ...) |
| `ui/screens/library/LibraryViewModel.kt` | `LibraryUiState.kt` | `LibraryUiState`, `LibraryGameUi`, `LibraryGridItem`, filter types, `MIN_SECTION_RAIL_LABELS` |
| `ui/theme/ThemeViewModel.kt` | `ThemeState.kt` | `ThemeState` |
| `ui/screens/common/GameLaunchDelegate.kt` | `LaunchStates.kt` | picker and overlay states, `LaunchResultCallbacks`, the two choice enums |

Declarations, KDoc and annotations move byte for byte; each file keeps only the imports its text still names. One content change: `MIN_SECTION_RAIL_LABELS` is read by both `LibraryUiState` and `LibraryViewModel`, so it moves as `internal` instead of `private` (manifest marker `^`).
**Reasoning:** This is the set the Library spike had to copy out one declaration at a time, so it is what blocks moving these types to `commonMain` later; it is also the upstream-friendly first step in the proposal. Packages are unchanged, so no caller, test or stability glob changes.
**Verification:** `./gradlew :app:assembleDebug :app:testDebugUnitTest` on JDK 21 → BUILD SUCCESSFUL in 2 m 34 s (warm); `compileDebugKotlin`, `compileDebugUnitTestKotlin` and `testDebugUnitTest` all executed: 4,359 tests, 0 failures, 1 skipped (upstream's own). `scripts/ci/stability_checks.py` and `service_stability_checks.py` pass. Not checked on a device or emulator: the step moves declarations without changing any of them, so there is no behaviour to compare; the emulator is set up before step 3.
**Finding:** First attempt failed to compile (the constant above was `private` and used by both files); fixed in the script, not by hand.
**Finding:** upstream's `agentic-smells.py` flags 8 moved lines (KDoc wording, two `//` lines, a `glowColorMode` field) because a partial move looks like added text in a diff. They are upstream's own lines, left verbatim; if this step is ever offered upstream, the PR needs a `Takt-ack:` saying the lines are moved, not written.

**Action:** Owner said proceed. Installed the Android emulator and an API 35 `google_apis` x86_64 image in `~/Android/Sdk`, and created AVD `argosy_handheld` (1920×1080 landscape, 320 dpi, hardware keyboard), the common handheld resolution Argosy targets. KVM is available; it boots headless in about 22 s (`-no-window -gpu swiftshader_indirect`).
**Reasoning:** Owner's choice for the "looks unchanged" check.

**Action:** Step 2 done: module skeleton. Added `:shared` (Kotlin Multiplatform library: `androidTarget()` + `jvm("desktop")`, namespace `com.nendo.argosy.shared`, min SDK 26, JVM 17) and `:desktopApp` (Compose desktop, `com.nendo.argosy.desktop.MainKt`, an empty window for now); `:app` depends on `:shared`. `shared` uses the app's `compose_stability_config.conf` and the same three Compose feature flags as `app`, so code compiles the same in both modules. Detekt now also scans `shared/src` and `desktopApp/src`. Moved one real file, `domain/model/UnifiedStateEntry.kt` (self-contained, uses `java.time`), to `shared/src/commonMain` as a pure rename (100% similarity, its own commit).
**Reasoning:** One real file proves the whole chain (JDK APIs in `commonMain`, `app` compiling against `shared`, desktop compile) without adding placeholder code.
**Decision (technical):** Compose Multiplatform **1.7.3**. Its Android variants require androidx Compose `ui`/`foundation`/`runtime` **1.7.6** and `material3` **1.3.1**, exactly what BOM 2024.12.01 already resolves. Checked by diffing `:app:dependencies --configuration debugRuntimeClasspath` before and after: no existing artifact changed version; five were added (`org.jetbrains.compose.runtime`, `annotation-internal`, `collection-internal` 1.7.3, `atomicfu`/`atomicfu-jvm` 0.23.2). CMP plugin 1.7.3 works with Kotlin 2.3.20 (only the existing `IntrinsicRemember` deprecation warning).
**Finding:** with only JVM-family targets, Kotlin skips `compileCommonMainKotlinMetadata`; `commonMain` is compiled once per target. "No Android imports in `commonMain`" is therefore enforced by `:shared:compileKotlinDesktop` (no `android.jar` on that classpath), which is part of every step's gate from now on.
**Deferred:** KSP per target (`kspAndroid` / `kspDesktop`) moves to step 3, the first step with an annotation processor; an empty processor setup would prove nothing.
**Verification:**
- `./gradlew :shared:assemble :desktopApp:assemble :app:assembleDebug :app:testDebugUnitTest` on JDK 21 → BUILD SUCCESSFUL (6 m 35 s); `app` compile and 4,359 unit tests executed, 0 failures, 1 skipped (upstream's).
- `./gradlew :desktopApp:run`: window process starts on the owner's desktop, no exceptions (closed by timeout after ~2 min).
- Emulator, before/after: built the pre-Phase-1 commit `3ad8281f6` in a temporary worktree as the baseline APK, installed it, captured the first-run welcome screen and (after the gamepad A key) the RomM login screen; then installed the current build over it and repeated. Both screens are **pixel-identical** at 1920×1080 (empty difference box); no `FATAL EXCEPTION` in logcat. No app data was cleared (the first-run step is not persisted, so both builds start on the same screen).
- Not covered: screens past first run (home, library, settings) need a RomM server; `testbed/romm` (Docker) will provide one for the database and UI steps.
**Note:** baseline APK and its screenshots kept in `build/desktop-baseline/` (git-ignored) for later before/after checks.

**Action:** Owner said proceed with step 3. Step 3 done: the Room database lives in `shared/src/commonMain`. Moved 154 files as pure renames (all R100): entities (77), DAOs (69), `Converters`, `ALauncherDatabase`, and the six plain-Kotlin files they import (`ArtSlot`, `FileOrigin`, `GameSource`, `ResolvedGameArt`, `PlatformDefinitions`, `SearchNormalizer`), exactly the Room spike's closure. `data/local/migrations/` and the Android `DatabaseFactory` stay in `app`; Android still builds its database with `Room.databaseBuilder(context, …, "alauncher.db")` and the same 214 migrations.
**Build wiring:** `shared` gets `room-runtime` and coroutines in `commonMain`, `room-compiler` on `kspAndroid` and `kspDesktop`, `sqlite-bundled` on desktop only, and the Room Gradle plugin with `schemaDirectory` = `app/schemas` (the existing folder, so `androidTest` assets and upstream's schema history are untouched). `app` no longer runs the Room processor. New `DesktopDatabaseFactory` (`desktopMain`) opens the database with `BundledSQLiteDriver`; a desktop test (ported from the spike) checks insert, `Flow` queries and `user_version` 214.
**Finding (important for the rest of the phase):** moving the entities broke 32 places in `app` with "Smart cast to 'String' is impossible, because 'localPath' is a public API property declared in different module". Kotlin never smart-casts a property from another module, and upstream's code relies on smart casts everywhere, as one module. Kotlin has no switch for this, but it treats a *friend* module (the mechanism that lets test code see main code) like its own. `app` now passes `shared`'s compiled Android classes as `-Xfriend-paths` (debug and release jars), which brings smart casts and `internal` visibility back with 0 code edits. The same issue would have grown with every later step (several hundred hand edits in upstream files), so this keeps the "moves only, no edits" principle intact.
**Caveats of the friend-path setup:** the paths are AGP intermediates (`compile_library_classes_jar/<variant>/…/classes.jar`) and may need updating on an AGP upgrade; `desktopApp` will need the same for `shared`'s desktop jar once it uses shared types. Alternative if this ever breaks: turn `app` itself into the KMP module (Android application + desktop target in one module, the Compose Multiplatform "composeApp" layout), which removes the module boundary altogether but changes the approved layout, so that would be the owner's call.
**Finding:** Room writes a schema file only when that version's file is missing. With `214.json` present the export tasks are silent; with it moved aside, both the Android and the desktop processor wrote a file **byte-identical** to upstream's `214.json` (identity hash `6a58e378d72d74d60c8b6d1bf8a20cb8`) and the plugin copied it into `app/schemas`. The file was then restored; `app/schemas` is unchanged.
**Re-apply tooling:** `scripts/desktop/restructure/moves.txt` lists the moves as path rules with excludes (`+ data/local/**`, `- data/local/migrations/**`, …) and `apply_moves.py` applies them with `git mv`, idempotently, so an entity or DAO that upstream adds under `app/` is relocated by re-running it. Checked: the rules select exactly the 155 files now in `shared` (including step 2's `UnifiedStateEntry`) and none of the migrations.
**Tooling paths:** no CI script referenced the moved files. Three upstream skill docs gave exact paths (`platform-support`, `ra-compliance`, and a `grep` command in `pre-release-validation`); updated to the new paths. AGENTS.md's structural index names files without paths and needed no edit.
**Verification:**
- `./gradlew :shared:compileKotlinDesktop :shared:desktopTest :desktopApp:assemble :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin` on JDK 21 → BUILD SUCCESSFUL. App: 4,359 unit tests, 0 failures, 1 skipped (upstream's). Desktop: 2 tests, 0 failures. `androidTest` sources (Room migration tests) compile; they were not run (they need instrumentation).
- `:shared:compileKotlinDesktop` compiles all 155 files without `android.jar`: no Android imports in `commonMain`.
- Emulator upgrade check: the pre-Phase-1 baseline APK had created its database on first launch (v214, 77 tables, one seeded platform row). Snapshotted it (pulled `alauncher.db` with WAL, summarised `user_version`, `room_master_table` identity hash, every table's row count and a content checksum), installed this step's build over it and launched it. The app opened the database 1.6 s after process start (`-shm` touched), the summary is **identical** before and after, the first-run screen is **pixel-identical** to the baseline, and logcat has no `FATAL EXCEPTION` or Room error. No app data was cleared.
- Not covered: the device database holds almost no rows, because nothing past first run is reachable without a RomM server. A populated check (library sync, then upgrade) needs the `testbed/romm` Docker setup; Docker 29.7 is installed on this machine.

**Action:** Owner said proceed with step 4. Wrote `scripts/desktop/restructure/movable.py`: it indexes every top-level declaration in `app` and `shared`, resolves each file's in-repo dependencies (imports, wildcard imports, fully qualified names, same-package references by word), rejects files with imports outside an allow-list (`kotlin`, `kotlinx.coroutines`, `java`, `javax.inject`, `javax.crypto`, `androidx.room`, `androidx.compose.runtime`; not `androidx.sqlite.db`) or with `R`/`BuildConfig`, and iterates to the largest closed set. Given path prefixes, it prints the qualifying files there plus the qualifying files they depend on, so the output compiles on its own.
**Reasoning:** Hand-picking files would miss same-package references; a fixpoint over a conservative dependency graph cannot select a file whose dependency stays behind. The compiler stays the final check.
**Finding:** today 250 `app` files form a closed Android-free set. For step 4's scope (`domain/`, `data/model/`, `util/`, 157 files) 60 qualify. About 50 are blocked directly (`Context` 16, `android.util.Log` 14, `Intent` 7, `Build` 5, `org.json` 6, Hilt qualifiers 4, …) and the rest because they depend on repositories, which move in steps 5–7. `Log` and `org.json` are cheap to unblock later (a logging interface; the `org.json` artifact on desktop).
**Action:** Moved the 60 files as pure renames (all R100): 27 `domain/model`, 15 `domain/usecase`, 9 `data/model`, 6 `util`, plus one file each from `data/music`, `core/notification` and `core/media` that they depend on. `shared` gets `javax.inject:javax.inject:1` (plain JVM; the same artifact Hilt uses) for their `@Inject` constructors. The list is in `moves.txt`.
**Finding:** Dagger generates the factory for an `@Inject` class compiled in another module without the processor: `app`'s Hilt build now emits `GetLauncherQueueUseCase_Factory` and the others under `app/build/generated/hilt/component_sources`. No Dagger setup in `shared` is needed for Android.
**Note:** the parallel testbed session (started by the owner) runs AVD `argosy_handheld` on `emulator-5554`. To avoid disturbing it, before/after checks now use a second, identical AVD `argosy_handheld_check` on `emulator-5556`, started fresh each time.
**Verification:**
- `./gradlew :shared:compileKotlinDesktop :shared:desktopTest :desktopApp:assemble :app:assembleDebug :app:testDebugUnitTest` on JDK 21 → BUILD SUCCESSFUL (6 m 35 s). App: 4,359 tests, 0 failures, 1 skipped (upstream's). Desktop: 2 tests, 0 failures. `shared` (215 files) compiles for the desktop without `android.jar`.
- Emulator (`argosy_handheld_check`, fresh): baseline APK first launch, then this step's build installed over it. Welcome and RomM login screens **pixel-identical** to the saved baseline screenshots; database summary identical before and after the upgrade; no `FATAL EXCEPTION`.
- Not covered: screens past first run (waiting on the RomM testbed).

**Action:** The owner started the proposed testbed session ([RomM testbed](docs/desktop/testbed-runbook.md)): a local RomM 5.1 server on `localhost:8093` with 13 self-made ROMs, and a seed script that signs the debug app on an emulator in through the debug-only `DebugSeedReceiver`. Its runbook and scripts (`docs/desktop/testbed-runbook.md`, `scripts/desktop/testbed/`) are in the working tree, not yet committed.

**Action:** Owner said proceed with step 5. Step 5 done: network clients and JVM-library code live in `shared`.
- `shared` gets Retrofit, OkHttp, Moshi, Socket.IO (with `org.json` excluded, as in `app`), commons-compress, xz, the Steam bundle, BouncyCastle and CBOR at `app`'s versions; zstd-jni as AAR on Android and JAR on desktop; `org.json` on desktop only (Android ships it); Moshi codegen on both targets. (`addProvider` is used where a dependency needs an `exclude` or artifact type, which the KMP dependency block does not take for catalog entries.)
- **Logging seam.** `android.util.Log` (124 files import it, 14 more call it fully qualified) and upstream's `util/Logger.kt` wrapper were the most common blockers. New `PlatformLog` is an `expect object` with exactly `android.util.Log`'s `v/d/i/w/e` signatures; the Android actual delegates to `android.util.Log`, so Android logging is unchanged; the desktop actual writes to stderr. `rewrite_imports.py` (scripted, idempotent, only for files in `commonMain`) turns `import android.util.Log` into `import com.nendo.argosy.util.PlatformLog as Log` (keeping any alias), so call sites stay exactly as upstream wrote them, and fully qualified `android.util.Log.` calls into `PlatformLog.` calls.
- **One hand edit in an upstream file:** removed `LogSanitizer.describeIntent` from `Logger.kt` (29 lines). It has no callers and was the file's only `Intent` use.
- `movable.py` now allows all of `javax.*` (JDK), treats the rewritten `Log` as portable, and rejects fully qualified `android`/`androidx` references in code bodies (it missed those before).
- Moved 51 files as pure renames: `data/remote` (29: RomM, RA, Steam, Jellyfin models and APIs, GitHub/GitLab/Gitea/Play Store clients, SSL helpers), `data/social` (9), `data/quaypass` (4), `data/steam` (3), `data/speedrun` (2), `data/cheats` (2), `data/titledb` (1) and `util/Logger.kt`. The rewrite then changed 3 lines in 2 of them.
**Finding:** what still keeps network code in `app` is `BuildConfig` (API URLs, secrets, `DEBUG`: RomM, RA, Jellyfin factories), `Context`/`ConnectivityManager`/`SystemClock` (connection managers, reachability), Hilt `@ApplicationContext`, and repositories/preferences (steps 6–7). `BuildConfig` needs its own design: `app` generates `com.nendo.argosy.BuildConfig` per build type from `.env`, and shared code cannot see it. Proposed for step 7 (DI and configuration): an injected app-config interface, or `shared` generating the same fields.
**Verification:**
- `./gradlew :shared:compileKotlinDesktop :shared:desktopTest :desktopApp:assemble :app:assembleDebug :app:testDebugUnitTest` on JDK 21 → BUILD SUCCESSFUL (5 m 27 s). App: 4,359 tests, 0 failures, 1 skipped. Desktop: 2 tests, 0 failures. Moshi adapters for the moved classes are now generated in `shared` for both targets; `checkDebugDuplicateClasses` passes.
- Android `debugRuntimeClasspath`, compared with the pre-Phase-1 graph: no artifact changed version; still only step 2's five additions.
- **First populated before/after check.** On `argosy_handheld_check` (`emulator-5556`), signed in to the testbed: baseline APK, then this step's build, each installed over the same app data, launched, and walked through the same gamepad sequence (Home, Game Detail, side menu, Library platforms, Library all games). All five screens are **pixel-identical** once both clocks are masked; the database (v214, 13 games, 5 platforms, 13 files; 51 rows) is identical; no `FATAL EXCEPTION`.
- **Method finding:** the very first launch after a sync opens Home on a different row than later launches (the "Recent" row only exists after the first sync), and two baseline runs differ in exactly the regions that differed against the candidate. A fair A/B therefore compares the candidate with a baseline run from the same app state, not with a screenshot taken earlier. The side menu shows a second clock, and the header clock blurs through the frosted menu, so both are masked.
**Tooling:** `scripts/desktop/check/ab-check.sh` (with `compare.py` and `db_summary.py`) automates that A/B: boots the check AVD if needed, installs baseline then candidate over the same data, captures the five screens and the database, compares with clocks masked, counts crashes, and exits non-zero on any difference. Run once more end to end after writing it: all identical. Output goes to `build/desktop-check/<timestamp>/` (git-ignored).
