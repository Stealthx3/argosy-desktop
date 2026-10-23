package com.nendo.argosy.data.platform

import com.nendo.argosy.platform.PlatformContext
import java.io.File

/**
 * Where downloads go while the user has not chosen a ROM folder. Android keeps them in the app's
 * external files directory; the desktop uses a host folder that emulators can also open.
 */
expect fun defaultRomStorageBase(context: PlatformContext): File
