package com.nendo.argosy.platform

import android.content.ComponentName
import android.content.Intent

actual object PlatformIntent {
    actual val ACTION_BATTERY_CHANGED: String get() = Intent.ACTION_BATTERY_CHANGED
    actual val ACTION_BOOT_COMPLETED: String get() = Intent.ACTION_BOOT_COMPLETED
    actual val ACTION_DELETE: String get() = Intent.ACTION_DELETE
    actual val ACTION_MAIN: String get() = Intent.ACTION_MAIN
    actual val ACTION_MY_PACKAGE_REPLACED: String get() = Intent.ACTION_MY_PACKAGE_REPLACED
    actual val ACTION_PACKAGE_ADDED: String get() = Intent.ACTION_PACKAGE_ADDED
    actual val ACTION_PACKAGE_REMOVED: String get() = Intent.ACTION_PACKAGE_REMOVED
    actual val ACTION_PACKAGE_REPLACED: String get() = Intent.ACTION_PACKAGE_REPLACED
    actual val ACTION_SCREEN_OFF: String get() = Intent.ACTION_SCREEN_OFF
    actual val ACTION_SCREEN_ON: String get() = Intent.ACTION_SCREEN_ON
    actual val ACTION_USER_PRESENT: String get() = Intent.ACTION_USER_PRESENT
    actual val ACTION_VIEW: String get() = Intent.ACTION_VIEW
    actual val CATEGORY_DEFAULT: String get() = Intent.CATEGORY_DEFAULT
    actual val CATEGORY_HOME: String get() = Intent.CATEGORY_HOME
    actual val CATEGORY_LAUNCHER: String get() = Intent.CATEGORY_LAUNCHER
    actual val FLAG_ACTIVITY_CLEAR_TASK: Int get() = Intent.FLAG_ACTIVITY_CLEAR_TASK
    actual val FLAG_ACTIVITY_CLEAR_TOP: Int get() = Intent.FLAG_ACTIVITY_CLEAR_TOP
    actual val FLAG_ACTIVITY_MULTIPLE_TASK: Int get() = Intent.FLAG_ACTIVITY_MULTIPLE_TASK
    actual val FLAG_ACTIVITY_NEW_TASK: Int get() = Intent.FLAG_ACTIVITY_NEW_TASK
    actual val FLAG_ACTIVITY_NO_ANIMATION: Int get() = Intent.FLAG_ACTIVITY_NO_ANIMATION
    actual val FLAG_ACTIVITY_NO_HISTORY: Int get() = Intent.FLAG_ACTIVITY_NO_HISTORY
    actual val FLAG_ACTIVITY_REORDER_TO_FRONT: Int get() = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    actual val FLAG_ACTIVITY_SINGLE_TOP: Int get() = Intent.FLAG_ACTIVITY_SINGLE_TOP
    actual val FLAG_GRANT_READ_URI_PERMISSION: Int get() = Intent.FLAG_GRANT_READ_URI_PERMISSION
}

actual typealias PlatformIntentType = Intent

actual typealias PlatformComponentName = ComponentName

actual val PlatformComponentName.packageName: String get() = packageName

actual val PlatformIntentType.component: PlatformComponentName? get() = component

actual val PlatformIntentType.`package`: String? get() = `package`

actual fun PlatformIntentType.getStringExtra(name: String): String? = getStringExtra(name)

actual val PlatformIntentType.action: String? get() = action

actual var PlatformIntentType.data: PlatformUri?
    get() = data
    set(value) {
        data = value
    }

actual var PlatformIntentType.flags: Int
    get() = flags
    set(value) {
        flags = value
    }

actual fun PlatformIntentType.addFlags(flags: Int): PlatformIntentType = addFlags(flags)

actual fun PlatformIntentType.putExtra(name: String, value: String?): PlatformIntentType = putExtra(name, value)

actual fun PlatformIntentType.putExtra(name: String, value: Int): PlatformIntentType = putExtra(name, value)

actual fun PlatformIntentType.putExtra(name: String, value: Long): PlatformIntentType = putExtra(name, value)

actual fun PlatformIntentType.putExtra(name: String, value: Boolean): PlatformIntentType = putExtra(name, value)
