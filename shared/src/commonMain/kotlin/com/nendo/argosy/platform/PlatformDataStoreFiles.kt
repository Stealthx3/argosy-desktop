package com.nendo.argosy.platform

import java.io.File

expect fun PlatformContext.preferencesDataStoreFile(name: String): File
