package com.nendo.argosy.platform

expect object PlatformIntent {
    val ACTION_BATTERY_CHANGED: String
    val ACTION_BOOT_COMPLETED: String
    val ACTION_DELETE: String
    val ACTION_MAIN: String
    val ACTION_MY_PACKAGE_REPLACED: String
    val ACTION_PACKAGE_ADDED: String
    val ACTION_PACKAGE_REMOVED: String
    val ACTION_PACKAGE_REPLACED: String
    val ACTION_SCREEN_OFF: String
    val ACTION_SCREEN_ON: String
    val ACTION_USER_PRESENT: String
    val ACTION_VIEW: String
    val CATEGORY_DEFAULT: String
    val CATEGORY_HOME: String
    val CATEGORY_LAUNCHER: String
    val FLAG_ACTIVITY_CLEAR_TASK: Int
    val FLAG_ACTIVITY_CLEAR_TOP: Int
    val FLAG_ACTIVITY_MULTIPLE_TASK: Int
    val FLAG_ACTIVITY_NEW_TASK: Int
    val FLAG_ACTIVITY_NO_ANIMATION: Int
    val FLAG_ACTIVITY_NO_HISTORY: Int
    val FLAG_ACTIVITY_REORDER_TO_FRONT: Int
    val FLAG_ACTIVITY_SINGLE_TOP: Int
    val FLAG_GRANT_READ_URI_PERMISSION: Int
}

expect open class PlatformIntentType {
    constructor()
    constructor(action: String?)
    constructor(action: String?, uri: PlatformUri?)
    constructor(original: PlatformIntentType)
}

expect class PlatformComponentName

expect val PlatformComponentName.packageName: String

expect val PlatformIntentType.component: PlatformComponentName?

expect val PlatformIntentType.`package`: String?

expect fun PlatformIntentType.getStringExtra(name: String): String?

expect val PlatformIntentType.action: String?

expect var PlatformIntentType.data: PlatformUri?

expect var PlatformIntentType.flags: Int

expect fun PlatformIntentType.addFlags(flags: Int): PlatformIntentType

expect fun PlatformIntentType.putExtra(name: String, value: String?): PlatformIntentType

expect fun PlatformIntentType.putExtra(name: String, value: Int): PlatformIntentType

expect fun PlatformIntentType.putExtra(name: String, value: Long): PlatformIntentType

expect fun PlatformIntentType.putExtra(name: String, value: Boolean): PlatformIntentType
