package com.nendo.argosy.platform

actual typealias PlatformBundle = android.os.Bundle

actual fun PlatformContext.startActivity(intent: PlatformIntentType) = startActivity(intent)

actual fun PlatformContext.startActivity(intent: PlatformIntentType, options: PlatformBundle?) = startActivity(intent, options)
