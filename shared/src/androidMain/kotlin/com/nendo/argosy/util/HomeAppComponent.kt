package com.nendo.argosy.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.nendo.argosy.ArgosyActivity

actual object HomeAppComponent {

    actual const val ALIAS_CLASS = "com.nendo.argosy.MainActivity"

    actual fun setEnabled(context: Context, enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        val component = ComponentName(context.packageName, ALIAS_CLASS)
        if (context.packageManager.getComponentEnabledSetting(component) == state) return
        context.packageManager.setComponentEnabledSetting(
            component, state, PackageManager.DONT_KILL_APP
        )
    }

    actual fun isLauncherActivity(className: String?): Boolean =
        className == ArgosyActivity::class.java.name || className == ALIAS_CLASS

    actual fun componentEnabled(available: Boolean, isDefaultHome: Boolean): Boolean =
        available || isDefaultHome

    actual fun startupAvailability(
        stored: Boolean,
        defaultApplied: Boolean,
        desktopModeSupported: Boolean,
        isDefaultHome: Boolean
    ): HomeAppStartup {
        if (defaultApplied) return HomeAppStartup(available = stored, persistDefault = false)
        return HomeAppStartup(
            available = !desktopModeSupported || isDefaultHome,
            persistDefault = true
        )
    }
}
