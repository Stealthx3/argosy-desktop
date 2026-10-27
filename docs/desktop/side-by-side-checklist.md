# Side-by-side checklist (Phase 5, step 5)

Exit criterion: a tester using both apps cannot name a behavioural difference outside the approved desktop divergences. Run Argosy (Android) and Argosy Desktop (alpha Flatpak) against the **same RomM server and account**, with the same controller if you can. Go through each row in both apps. Write down anything that looks, sounds, or behaves differently, together with the screen and the buttons pressed.

## Not differences (approved divergences, `PORT_PLAN.md`)

- Missing on desktop: Steam, Jellyfin, Argosy Social and QuayPass / Check-In, the "Android Settings" row, built-in emulation (cores, netplay, RetroAchievements in-core), dual-screen, YouTube trailers (deferred).
- Linux versions of Android screens: emulator suggestions and installers (Flathub / AppImage), GPU drivers (host GPU info), permissions (Flatpak folder access).
- Desktop only:
  - Window and display: system title bar, windowed dark first launch, full-screen setting (F11).
  - Controls: controller input mode (Auto / Steam Input / SDL3), per-button navigation mapping.
  - Display: display profiles (Handheld / Desktop / TV) and the wizard's Display setup step.
  - Wizard: "Emulators Argosy should manage" step.
  - Close Argosy: the Close Argosy entry in Quick Settings.

## Rows to compare

| # | Area | What to do in both apps | Same? | Notes |
|---|------|-------------------------|-------|-------|
| 1 | First run | Wizard: language, server, sign-in, ROM folder, platform pick, finish |  |  |
| 2 | Home | Rows (Recent, Favourites, platforms), covers, NEW badges, background art, clock and battery |  |  |
| 3 | Home input | D-pad and shoulders between rows, A / X / Y actions, footer hints, sounds |  |  |
| 4 | Library | Platform cards, game grid, letter jump, filters, sort, quick menu |  |  |
| 5 | Game details | Art, metadata, achievements, saves, screenshots, video, actions |  |  |
| 6 | Download | Start, progress, pause / resume, cancel, queue, notifications |  |  |
| 7 | Launch | Launch a game in an external emulator, return, play time recorded |  |  |
| 8 | Save sync | Play, exit, and check the save on the server, then on the other device |  |  |
| 9 | Collections | Create, rename, add and remove games, browse by genre / game mode |  |  |
| 10 | Search | Text search, on-screen keyboard, results |  |  |
| 11 | Quick Settings | Every item, wrap-around, sounds |  |  |
| 12 | Settings | Every section: values, ranges, toggles, modals, Reset |  |  |
| 13 | Themes | Theme, accent, wallpaper (image and video), UI scale, fonts |  |  |
| 14 | Sounds and music | UI sounds, volume |  |  |
| 15 | Languages | Switch to German (or another language), check text and layout, switch back |  |  |
| 16 | Empty and error states | No server, server offline, empty platform, failed download |  |  |
| 17 | About | Version, links, changelog |  |  |

The automated part of this check is `scripts/desktop/check/side-by-side.sh <out> <fresh-walk-name>`. It walks the same screens on the check AVD and on fresh desktop data, then tiles each pair into `<out>/pairs-*.png`.
