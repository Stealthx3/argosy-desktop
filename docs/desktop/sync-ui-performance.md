# UI slowdown during a large library sync

Status: analysis only, nothing changed (2026-11-10). The owner reported that the UI becomes very slow while a large RomM library syncs. The causes below were found by reading the code. No profile has been taken yet. Fixes that are not clear bug fixes are planned for the next release, through the owner's wishlist.

## Likely causes, most likely first

### 1. Cover prefetch decodes every image into memory (desktop only)

- **Where:** `shared/src/desktopMain/kotlin/com/nendo/argosy/data/cache/ImageCacheManager.desktop.kt`, `prefetch()`.
- **What happens:** during sync, `queueCoverCache`, `queueBackgroundCache` and `queueArtIfStale` all call `prefetch()`. It enqueues one Coil `ImageRequest` per URL, with no concurrency limit. Each request downloads the image and also decodes it at full size into Coil's memory cache.
- **Effect:** with thousands of games, the CPU and the garbage collector are kept busy, and the covers on screen are evicted from the memory cache and decoded again.
- **Also:** `pauseBackgroundCaching()` and `resumeBackgroundCaching()` exist but do nothing on desktop.
- **Proposed fix:**
  - Write prefetched images straight into Coil's disk cache, without decoding. The client is the OkHttp client in `platform/image/DesktopImageLoader.kt`.
  - Limit it to a few downloads at a time.
  - Wire up pause and resume.

### 2. The full art table reloads every second (desktop only)

- **Where:** same file, the `gameArt` StateFlow.
- **What happens:** every change in the `game_art` row count runs `gameArtDao.getAll()` and rebuilds a map of every game, at most once a second (`ART_MODEL_RELOAD_INTERVAL_MS`). During a sync the row count changes continuously, so this runs every second. Every tile's `observeArt(gameId)` maps over the whole result.
- **Proposed fix:** do one of these:
  - reload only the games whose art changed;
  - reload much less often while a sync is running, and once when it ends.

### 3. Several separate database commits per game (shared with Android)

- **Where:** `shared/src/commonMain/kotlin/com/nendo/argosy/data/remote/romm/RomMLibrarySyncService.kt`, in `syncRom` / `syncRomInBatch` / `syncPlatformRoms`.
- **What happens:** each ROM's game row (`gameDao.insert`), art rows (`writeArtSources` / `recordArtSource`) and disc rows are separate commits. Each commit triggers Room's invalidation, and Home's tile flows (`homeTileRepository.observeTiles`, among others) then re-query and recompose.
- **Proposed fix:** do one of these:
  - Wrap each fetched page in one transaction. First check that `syncRom` makes no network calls inside it.
  - Throttle the UI-side flows while a sync runs, to at most one refresh every ~500 ms.
- **Note:** this is shared code, so the divergence needs a `DEVLOG.md` note. It is library sync, not save sync, but it still needs a before-and-after check on a large library.

### 4. Sync can use every core, and progress updates once per game

- **Cores:** sync runs on `Dispatchers.IO` and can take all cores, which hurts most on a Steam Deck. Proposed fix: limit sync to a couple of threads (`limitedParallelism`).
- **Progress:** `publishGameProgress` updates `_syncProgress` once per ROM and rebuilds the platform list each time. Proposed fix: limit it to about 10 updates a second.

## Suggested order

1. Profile one large sync against the testbed (RomM on `localhost:8093`; see `testbed-runbook.md`) with Java Flight Recorder, to confirm this order.
2. Fix 1 and 2. They are desktop-only and low risk.
3. Fix 3 and 4.
4. Profile again with the same library and compare.
