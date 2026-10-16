# Draft: proposal to rommapp for a shared Kotlin Multiplatform codebase

Status: **draft, not posted.** Posting is the project owner's decision. Suggested venue: a GitHub issue on `rommapp/argosy-launcher` or the RomM Discord.

---

**Title:** Proposal: Linux desktop build of Argosy via Compose Multiplatform (shared codebase)

Hi! I'm working on a Linux desktop port of Argosy, shipped as a Flatpak, with the goal that it looks and behaves exactly like the Android app. The cleanest way to get there is to run Argosy's own Compose UI on the desktop through Compose Multiplatform, rather than rebuilding anything.

I surveyed the current `main` (v2.23.0) to size this:

- 919 of 1,695 Kotlin files (54%) have no Android, Hilt or resource dependency and could move to a shared module unchanged.
- 474 files (28%) call real Android APIs; they cluster in launching, package management, WorkManager, media, device controls and libretrodroid.
- The input layer is already platform-neutral above the key-code mapping, and the Room DAOs are Room-KMP-ready apart from the `SupportSQLiteDatabase` migrations.

What I'd like to ask:

1. Would you be open to the app being restructured into Kotlin Multiplatform modules (`shared` + `androidApp` + `desktopApp`) upstream, done in small reviewable PRs that keep the Android app unchanged for users? That avoids a long-lived fork drifting behind.
2. If not, are you OK with a fork named "Argosy Desktop" using the Argosy name and icon, with clear credit and a link back here?

Everything stays GPL-3.0. Happy to share the full plan and survey.
