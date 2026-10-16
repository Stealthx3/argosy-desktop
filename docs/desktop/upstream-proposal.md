# Draft: proposal to rommapp for a shared Kotlin Multiplatform codebase

Status: **draft, not posted.** Posting is the project owner's decision. Suggested venue: a GitHub issue on `rommapp/argosy-launcher` or the RomM Discord.

---

**Title:** Proposal: Linux desktop build of Argosy via Compose Multiplatform (shared codebase)

Hi! I'm working on a Linux desktop port of Argosy, shipped as a Flatpak, with the goal that it looks and behaves exactly like the Android app. The cleanest way to get there is to run Argosy's own Compose UI on the desktop through Compose Multiplatform, rather than rebuilding anything.

I surveyed the current `main` (v2.23.0) to size this:

- 919 of 1,695 Kotlin files (54%) have no Android, Hilt or resource dependency and could move to a shared module unchanged.
- 474 files (28%) call real Android APIs; they cluster in launching, package management, WorkManager, media, device controls and libretrodroid.
- The input layer is already platform-neutral above the key-code mapping, and the Room DAOs are Room-KMP-ready apart from the `SupportSQLiteDatabase` migrations.

I then ran three small proofs of concept against that same `main`, without changing any upstream file:

- **Database:** the entities, all DAOs and `ALauncherDatabase` compile for the desktop JVM under Room KMP with the bundled SQLite driver. The exported schema is identical to `app/schemas/.../214.json` (same identity hash), and every DAO function is already `suspend` or `Flow`.
- **UI:** `LibraryGameList` inside `ALauncherTheme` renders in a Compose Multiplatform desktop window, with 71 Argosy files (theme, tokens, list rows, footers, badges, titles) compiled unchanged. The Android pieces it touched were thin: `R`/`stringResource`, `LocalContext`, Coil's `AsyncImage`, `hiltViewModel`, a few `android.graphics` helpers, and one `BlurMaskFilter` glow.
- **Sandbox:** a test Flatpak starts the RetroArch Flatpak on the host with a ROM from a shared folder and gets the exit status back, which is what play sessions need.

A cheap first step that helps either way: a few state types (`LibraryUiState`, `ThemeState`, the preference enums in `UserPreferencesRepository`) are declared inside view-model or repository files. Moving them into their own files is a small, behaviour-free change.

What I'd like to ask:

1. Would you be open to the app being restructured into Kotlin Multiplatform modules (`shared` + `androidApp` + `desktopApp`) upstream, done in small reviewable PRs that keep the Android app unchanged for users? That avoids a long-lived fork drifting behind.
2. If not, are you OK with a fork named "Argosy Desktop" using the Argosy name and icon, with clear credit and a link back here?

Everything stays GPL-3.0. Happy to share the full plan and survey.
