package com.nendo.argosy.ui.theme.backdrop

import com.nendo.argosy.platform.PlatformContext

expect class BackdropStampProvider

expect fun backdropStampProvider(context: PlatformContext): BackdropStampProvider
