# Phase 0 · Discovery and spikes — closed

**Closed:** 2026-10-16. Gate passed (all three spikes). `main` pushed; `upstream` branch reset to rommapp v2.23.0.

## Outcome

- Android baseline builds unchanged (`:app:assembleDebug`, JDK 21, about 6 minutes).
- `spikes/room-kmp-jvm`: upstream's Room database (v214) runs on the JVM under Room KMP with no source edits; schema identical (identityHash `6a58e378…`); every DAO is `suspend` or `Flow`.
- `spikes/compose-desktop-library`: upstream's Library list and theme render on Compose Multiplatform desktop with 71 files unchanged; the README there lists the 15 Android APIs that needed shims.
- `spikes/flatpak-launch`: a sandboxed Flatpak starts the RetroArch Flatpak on a ROM in `~/Games/Argosy` and receives its exit status.

## Carried forward

- Build the Flatpak on `org.freedesktop.Platform` 25.08 (24.08 is end-of-life) → Phase 3 packaging.
- `docs/desktop/upstream-proposal.md`: updated with the spike results and held; posting is the owner's call.
- Toolchain on this machine: Android SDK `~/Android/Sdk`, JDK `~/.jdks/jdk-21.0.12.1+1`, `org.flatpak.Builder` (use `--disable-rofiles-fuse`). Git pushes over SSH via the repo-local `core.sshCommand` (the GNOME agent refuses to sign).
- Branch model: `main` is the workspace; `upstream` follows rommapp stable releases only (`.github/workflows/sync-upstream-release.yml`; needs Actions enabled on the fork).

## Details

`DEVLOG.md`, section "Phase 0 · Discovery and spikes"; `docs/desktop/phase-0-survey.md`; each spike's README.
