# Phase 0 survey: how much of Argosy can run on the desktop

Surveyed 2026-10-16 against upstream commit `6e64baa` (v2.23.0, versionCode 357) on `Stealthx3/argosy-desktop` main.

**Bottom line:** 919 of 1,695 Kotlin files (54%) import nothing Android-specific and can move to `commonMain` as they are. Most of the rest is mechanical work (Hilt annotations, `R.*` resources, `LocalContext`), not real Android behaviour. Only 474 files (28%) call true Android framework APIs, and they cluster in a few areas that already map to the plan's platform interfaces.

## Size

| Item | Count |
| --- | --- |
| Kotlin files in `app/src/main` | 1,695 |
| Lines of Kotlin | 386,679 |
| Files with no Android, Hilt, `R.*` or Compose-Android-bridge use | 919 |
| Files calling real Android framework APIs | 474 |
| Files using Hilt (`dagger.hilt`, `androidx.hilt`) | 221 |
| Files using `R.*` resources | 428 |
| Files using Compose-to-Android bridges (`LocalContext`, `LocalView`, `ui.res`) | 301 (103 use `LocalContext`) |
| `@HiltViewModel` view models | 33 |
| WorkManager workers | 8 |
| String resources | 6,479 across 8 languages (en, de, es, fr, hi, ru, zh-Hans, zh-Hant) |
| `res/` + `assets/` | 6.0 MB + 1.8 MB |
| `libretrodroid` native code | 8,580 lines of C++, 8 files tied to GLES/EGL/Android audio |

The `javax.inject` annotations (`@Inject` in 504 files, `@Singleton` in 340) are plain JVM, not Android, so they do not count against portability.

## Where the Android calls are

| Area | Files | Real Android API files | Main Android dependencies |
| --- | --- | --- | --- |
| `ui/` | 721 | 187 | `Context` and `LocalContext`, Media3 in a few components, `Bitmap` |
| `data/` | 624 | 184 | `Context`, Hilt, `Intent`/`ComponentName` (launcher), `PackageManager`, `WorkManager`, BLE |
| `libretro/` | 144 | 36 | `InputDevice`, `KeyEvent`, EGL, `Log` |
| `domain/` | 120 | 12 | `Log`, one `Intent` (upstream lists these as known debt in AGENTS.md) |
| `hardware/` | 27 | 22 | `Build`, device-specific performance and fan controls |
| `util/` | 30 | 20 | `Context`, `Intent` |
| `core/` | 21 | 8 | `InputDevice`, `MediaCodec` |

Top Android imports by file count: `android.content.Context` 265, `android.util.Log` 126, `android.content.Intent` 75, `android.os.Build` 48, `android.net.Uri` 33, `android.content.pm` 32, `android.graphics.Bitmap` 31, `android.view.KeyEvent` 29, `android.provider.Settings` 29, `androidx.media3` 27.

## Findings that change the plan

1. **TV Compose is gone upstream.** No file imports `androidx.tv`. The plan's TV Compose replacement step and its focus-feel risk mostly disappear.
2. **The input path is already platform-neutral above the key-code layer.** `ui/input/GamepadInputHandler.kt` turns Android key codes into a `GamepadEvent`, and every screen consumes the Android-free `InputHandler` interface (`onUp`, `onConfirm`, `onLeftStickClick`, ...). The desktop only needs SDL3 to emit the same key-code integers into `mapKeycodeToGamepadEvent`; the A/B, X/Y and Start/Select swap logic is reused as is. `ArgosyActivity.dispatchKeyEvent` / `dispatchGenericMotionEvent` and `TriggerAxisKeyEmitter` are the Android entry points to mirror.
3. **The database ports cleanly except for migrations.** Room schema is version 214, with 219 exported schema JSON files. DAOs use `Flow` and `suspend`; no `LiveData`, Paging or raw cursors outside migrations. All 214 migrations (4,406 lines, 742 `execSQL` calls) are written against Android's `SupportSQLiteDatabase`. A fresh desktop install is created directly at the current schema, so these migrations can stay Android-only; future migrations must use Room KMP's `SQLiteConnection` form.
4. **DI can stay closer to upstream than planned.** Plain Dagger 2 runs on the JVM, so desktop could use Dagger components and keep every `@Inject` constructor unchanged, instead of rewriting to Koin. Decide in Phase 1; Dagger is the lower-churn option.
5. **String resources convert mechanically.** Compose Multiplatform resources read the same `strings.xml` format, so the 428 `R.*` call sites become `Res.*` with a scripted rewrite.
6. **Camera and Bluetooth are narrow.** CameraX is used only by `ui/components/QrScanner.kt`. BLE is used only by QuayPass (`data/quaypass/ble/`, 7 files). Both stay deferred to 1.1 as planned.
7. **A RomM test server exists.** `testbed/romm` has Docker Compose files for RomM 4.9.2, 5.0.0 and 5.1.0 on one MariaDB, mounting a read-only sample library. It needs a RomM-shaped library (`romm_mock` from a RomM checkout).
8. **Build cost is high.** `gradle.properties` budgets 11 GB of heap, and AGENTS.md notes a cold `:app:compileDebugKotlin` takes about 12 minutes even with the Compose stability config. The development machine needs at least 16 GB of RAM.

## Method

`scripts` used: a Python pass over `app/src/main/kotlin` classifying each `import` as internal, real Android (`android.*`, `androidx.core/activity/work/media3/camera/palette`, `com.google.android.*`), Hilt (`dagger.hilt`, `androidx.hilt`), Compose-Android bridge (`LocalContext`, `LocalView`, `androidx.compose.ui.res`) or portable, plus regex counts for `R.<type>.` references. Counts are per file, not per call site.
