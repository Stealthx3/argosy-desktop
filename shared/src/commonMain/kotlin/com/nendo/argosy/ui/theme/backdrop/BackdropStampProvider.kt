package com.nendo.argosy.ui.theme.backdrop

import com.nendo.argosy.platform.PlatformContext
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources

expect class BackdropStampProvider

expect fun backdropStampProvider(context: PlatformContext): BackdropStampProvider
