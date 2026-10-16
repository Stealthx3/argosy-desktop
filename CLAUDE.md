# Argosy Desktop — instructions for Claude Code

This folder is the development workspace for **Argosy Desktop**: a faithful Linux desktop port of the Argosy Android launcher (`rommapp/argosy-launcher`), shipped as a Flatpak. Goal: a user of Argosy Desktop cannot tell it apart from the Android app by using it.

Read these first, in order:

1. `PORT_PLAN.md` — the plan you are executing (phases, architecture, parity matrix, Flatpak design).
2. `DEVLOG.md` — what has been done so far and why.
3. `docs/desktop/phase-0-survey.md` — codebase measurements and findings that refine the plan.
4. Upstream's own agent rules, once the repo is checked out: `AGENTS.md` and `.claude/CLAUDE.md`. Follow them for code that stays shared with Android; where the desktop port must diverge, say so in `DEVLOG.md`.

## Standing rules from the project owner

- **This project is separate** from any other RomM/Argosy project (e.g. FreightCar, IceRom). Do not mix them.
- **Git workspace:** this folder. **Remote:** `https://github.com/Stealthx3/argosy-desktop` (fork of `rommapp/argosy-launcher`, default branch `main`).
- **Branches:** `main` is the Argosy Desktop workspace; all work lands there. `upstream` is a read-only reference copy of `rommapp/argosy-launcher` at its newest stable release. Never commit to `upstream`; `.github/workflows/sync-upstream-release.yml` fast-forwards it to each new stable release tag and does nothing between releases.
- **Push only after a phase is complete.** Commit locally as you go; push to `origin` once, at the end of each phase.
- **Between phases, stop and ask** the owner whether to proceed to the next phase or schedule the continuation for later. Never roll into the next phase on your own.
- **`README.md` must contain a copy of the port plan.** At each phase end, rebuild `README.md` as: a short Argosy Desktop intro and status, then the full contents of `PORT_PLAN.md`, then a link to upstream's original README (keep it as `README.upstream.md`). The same README is pushed to GitHub, so both the local and remote repositories carry the plan.
- **Keep `DEVLOG.md` current:** for every meaningful step, record the action and the reasoning, under the current phase's heading. Each phase gets its own clearly separated section.
- Never post to upstream (issues, PRs, Discord) without the owner's explicit go-ahead. `docs/desktop/upstream-proposal.md` is a draft for the owner.
- Commit messages end with the attribution lines your harness specifies.

## One-time setup (this folder is not yet a git checkout)

This folder already contains the handoff files (this file, `PORT_PLAN.md`, `DEVLOG.md`, `docs/desktop/`, `spikes/`) and the plan PDF. None of these names exist upstream, so check the fork out in place:

```sh
git init -b main
git remote add origin https://github.com/Stealthx3/argosy-desktop.git
git fetch origin main
git checkout -t origin/main          # handoff files stay as untracked files
git submodule update --init --recursive
```

Then add the handoff files in the first commit of Phase 0 (leave the PDF out of git: add `*.pdf` at the repo root to `.git/info/exclude`).

Check the toolchain before building: JDK 17+ (21 fine), Android SDK with platform 35 and build-tools (`ANDROID_HOME` or `local.properties`), and at least 16 GB RAM (upstream budgets an 11 GB Gradle/Kotlin heap; a cold `:app:compileDebugKotlin` takes about 12 minutes). For the Flatpak spike: `flatpak`, `flatpak-builder`, Flathub remote, `org.freedesktop.Platform//24.08` and `org.freedesktop.Sdk//24.08`, and the `org.libretro.RetroArch` Flatpak with at least one core downloaded.

## Phase 0: remaining work

Done in session 1 (see `DEVLOG.md`): fork exists, codebase survey, input-path trace, Room/migration analysis, CameraX/BLE/testbed check, Flatpak spike files, upstream proposal draft.

Still to do, in this order:

1. **Android baseline build.** `./gradlew :app:assembleDebug` unchanged. Record build time and any setup needed in `DEVLOG.md`. This proves the toolchain before spikes.
2. **Spike: Room KMP on the JVM** → `spikes/room-kmp-jvm/` (standalone Gradle build, not wired into the root build).
   - Kotlin JVM project with Room KMP (`androidx.room` 2.7.x runtime + KSP compiler) and `androidx.sqlite:sqlite-bundled`.
   - Copy (do not move) the entities, DAOs, `Converters` and `ALauncherDatabase` from `app/src/main/kotlin/com/nendo/argosy/data/local/`, without `migrations/`. Strip Android-only imports; note each one stripped.
   - Pass: the database builds and opens on the JVM with `BundledSQLiteDriver`, a DAO insert and a `Flow` query work in a test, and the generated schema's `identityHash` equals the one in `app/schemas/com.nendo.argosy.data.local.ALauncherDatabase/214.json`.
3. **Spike: upstream Library UI in a Compose desktop window** → `spikes/compose-desktop-library/` (standalone Gradle build).
   - Compose Multiplatform desktop (current stable, 1.11.x or newer).
   - Bring in upstream's theme (`ui/theme/`) and the smallest real Library composables that render a game list (start from `ui/screens/library/LibraryGameList.kt` and its `components/`); work out the dependency closure first and stub only what is Android-bound (`LocalContext`, `R.*`, Coil, Hilt view models) with fake data.
   - Pass: renders headless to a PNG with `ImageComposeScene` (commit it as `spikes/compose-desktop-library/library.png`) and opens in a window with `./gradlew run`. Note every stub needed: that list sizes Phase 2.
4. **Spike: Flatpak host launch** → `spikes/flatpak-launch/` (files already written).
   - Put a legally redistributable homebrew ROM in `~/Games/Argosy/`, then run `spikes/flatpak-launch/run-spike.sh <rom-file> <core>`.
   - Pass: the ROM is visible inside the sandbox, RetroArch starts on the host with it, and the script reports the exit status when RetroArch closes. Record the outcome; if `--talk-name=org.freedesktop.Flatpak` or the override step is a problem, record exactly what failed.
5. **Upstream proposal.** Show the owner `docs/desktop/upstream-proposal.md` and ask whether to post it. Do not post it yourself.
6. **Close the phase.** Update `DEVLOG.md` (results of every spike, gate pass/fail), tick the Phase 0 boxes in `PORT_PLAN.md`, rebuild `README.md` per the rule above, commit, push `main` to `origin`, then ask the owner: proceed to Phase 1, or schedule it for later?

If a spike fails, do not paper over it: record the failure, what it means for the plan, and a proposed change, and raise it with the owner at the phase gate.
