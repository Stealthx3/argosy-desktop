package com.nendo.argosy.data.platform

import com.nendo.argosy.platform.PlatformContext
import java.io.File

actual fun defaultRomStorageBase(context: PlatformContext): File =
    File(System.getProperty("user.home"), "Games/Argosy")
