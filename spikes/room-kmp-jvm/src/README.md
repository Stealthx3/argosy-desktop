# Spike: Room KMP on the JVM

Phase 0 spike. Standalone Gradle build, not wired into the root build.

Compiles upstream's Room database (`ALauncherDatabase`, schema v214) for the JVM with Room KMP
2.7.1 + KSP and opens it with `androidx.sqlite:sqlite-bundled` (`BundledSQLiteDriver`).

```sh
./copy-sources.sh        # copy (never move) entities, DAOs, Converters, ALauncherDatabase from app/
./gradlew test           # JDK 17+
```

`test` depends on `verifySchemaMatchesUpstream`, which fails unless the schema Room exports here
(`schemas/.../214.json`) is identical to `app/schemas/.../214.json`.

Excluded from the copy: `migrations/` (written against `SupportSQLiteDatabase`) and
`DatabaseFactory.kt` (Android `Context` and `Room.databaseBuilder(context, ...)`). No other Android
import exists in the copied code, so no source line was edited.
