package com.nendo.argosy.platform

import java.io.File

actual fun PlatformContext.preferencesDataStoreFile(name: String): File =
    filesDir.resolveSibling("datastore").resolve("$name.preferences_pb")
