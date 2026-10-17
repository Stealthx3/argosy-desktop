package com.nendo.argosy.platform

actual object PlatformIntent {
    actual val ACTION_BATTERY_CHANGED: String = "android.intent.action.BATTERY_CHANGED"
    actual val ACTION_BOOT_COMPLETED: String = "android.intent.action.BOOT_COMPLETED"
    actual val ACTION_DELETE: String = "android.intent.action.DELETE"
    actual val ACTION_MAIN: String = "android.intent.action.MAIN"
    actual val ACTION_MY_PACKAGE_REPLACED: String = "android.intent.action.MY_PACKAGE_REPLACED"
    actual val ACTION_PACKAGE_ADDED: String = "android.intent.action.PACKAGE_ADDED"
    actual val ACTION_PACKAGE_REMOVED: String = "android.intent.action.PACKAGE_REMOVED"
    actual val ACTION_PACKAGE_REPLACED: String = "android.intent.action.PACKAGE_REPLACED"
    actual val ACTION_SCREEN_OFF: String = "android.intent.action.SCREEN_OFF"
    actual val ACTION_SCREEN_ON: String = "android.intent.action.SCREEN_ON"
    actual val ACTION_USER_PRESENT: String = "android.intent.action.USER_PRESENT"
    actual val ACTION_VIEW: String = "android.intent.action.VIEW"
    actual val CATEGORY_DEFAULT: String = "android.intent.category.DEFAULT"
    actual val CATEGORY_HOME: String = "android.intent.category.HOME"
    actual val CATEGORY_LAUNCHER: String = "android.intent.category.LAUNCHER"
    actual val FLAG_ACTIVITY_CLEAR_TASK: Int = 32768
    actual val FLAG_ACTIVITY_CLEAR_TOP: Int = 67108864
    actual val FLAG_ACTIVITY_MULTIPLE_TASK: Int = 134217728
    actual val FLAG_ACTIVITY_NEW_TASK: Int = 268435456
    actual val FLAG_ACTIVITY_NO_ANIMATION: Int = 65536
    actual val FLAG_ACTIVITY_NO_HISTORY: Int = 1073741824
    actual val FLAG_ACTIVITY_REORDER_TO_FRONT: Int = 131072
    actual val FLAG_ACTIVITY_SINGLE_TOP: Int = 536870912
    actual val FLAG_GRANT_READ_URI_PERMISSION: Int = 1
}

actual open class PlatformIntentType actual constructor() {
    internal var intentAction: String? = null
    internal var intentData: PlatformUri? = null
    internal var intentFlags: Int = 0
    internal val extras = LinkedHashMap<String, Any?>()

    actual constructor(action: String?) : this() {
        intentAction = action
    }

    actual constructor(action: String?, uri: PlatformUri?) : this(action) {
        intentData = uri
    }
}

actual val PlatformIntentType.action: String? get() = intentAction

actual var PlatformIntentType.data: PlatformUri?
    get() = intentData
    set(value) {
        intentData = value
    }

actual var PlatformIntentType.flags: Int
    get() = intentFlags
    set(value) {
        intentFlags = value
    }

actual fun PlatformIntentType.addFlags(flags: Int): PlatformIntentType = apply { intentFlags = intentFlags or flags }

actual fun PlatformIntentType.putExtra(name: String, value: String?): PlatformIntentType = apply { extras[name] = value }

actual fun PlatformIntentType.putExtra(name: String, value: Int): PlatformIntentType = apply { extras[name] = value }

actual fun PlatformIntentType.putExtra(name: String, value: Long): PlatformIntentType = apply { extras[name] = value }

actual fun PlatformIntentType.putExtra(name: String, value: Boolean): PlatformIntentType = apply { extras[name] = value }
