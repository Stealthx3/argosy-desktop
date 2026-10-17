package com.nendo.argosy.platform

expect class PlatformBundle

expect fun PlatformContext.startActivity(intent: PlatformIntentType)

expect fun PlatformContext.startActivity(intent: PlatformIntentType, options: PlatformBundle?)
