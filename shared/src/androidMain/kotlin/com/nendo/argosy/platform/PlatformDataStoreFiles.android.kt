package com.nendo.argosy.platform

import androidx.datastore.preferences.preferencesDataStoreFile as androidPreferencesDataStoreFile
import java.io.File

actual fun PlatformContext.preferencesDataStoreFile(name: String): File = androidPreferencesDataStoreFile(name)
