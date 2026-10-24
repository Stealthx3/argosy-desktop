# Owner test, Phase 4 (Linux adaptation)

Install or update the alpha (`flatpak update --user io.github.stealthx3.ArgosyDesktop`, or the `.flatpakref` in the README). Each item: what to do, what to expect. Report pass / fail / skipped per number.

## Controllers (steps 1–2)

1. Steam Controller with Steam running, Argosy started normally: Settings → Navigation → Input Mode shows Auto; the pad navigates (SDL3 path; no rumble expected). Note which Steam Controller model.
2. Argosy added to Steam and started from Steam: Input Mode offers Steam Input; navigation works and rumble comes through Steam.
3. `grep -i DesktopGamepad ~/argosy-run.log` lines from the two runs (what Argosy saw).
4. Settings → Navigation → Button Mapping on a real pad: map Confirm to X, check the footer icons follow, then Reset to Default.
5. A Nintendo-layout pad (Switch Pro or 8BitDo in Switch mode) with one remap: footer icons show the right face buttons.

## Window and display (steps 4–5)

6. F11 toggles full screen; Interface → Start in Full Screen on, restart: opens full screen; off again.
7. On a handheld (Steam Deck / Ally / Legion Go): the first-run Display step or Interface → Display Profile recommends Handheld; under Game Mode the window is full screen.
8. On a TV (≥ 40"): TV is recommended; TV applies UI Scale 125 %, the 3 % margin; edges are not cut off.
9. Handheld or TV profile: the mouse cursor hides while using a controller and returns on mouse movement.

## Flatpak screens (steps 6–8)

10. Settings → Permissions shows ten rows, all granted. Optional grant test: `flatpak override --user --nofilesystem=~/.var/app/net.rpcs3.RPCS3 io.github.stealthx3.ArgosyDesktop`, restart, Emulator save folders → A → Allow, restart: granted again (`flatpak override --user --reset io.github.stealthx3.ArgosyDesktop` undoes all of Argosy's overrides).
11. Settings → GPU Drivers shows your GPU, driver, VRAM, Vulkan and OpenGL versions and displays.
12. A platform without its emulator (e.g. Game Boy Advance without mGBA): Change Emulator → mGBA (Flathub) installs it and it becomes selectable. Remove with `flatpak uninstall --user io.mgba.mGBA`.
13. Settings → Managed Installers: an AppImage emulator you don't have (e.g. mGBA) → A downloads it into `~/Applications` and Argosy detects it; a Flatpak row with a newer Flathub version updates on A.

## Video (step 9)

14. A local video on a Home custom-grid tile or as a page backdrop plays (muted until engaged); a page theme song plays with a fade-in. YouTube trailers are deliberately not shown on desktop.

## Removals (step 3)

15. No Steam, Jellyfin/Media, Social, Check-In or Android Settings entries anywhere (Settings, drawer, Quick Settings); the first-run wizard has no core download steps.
