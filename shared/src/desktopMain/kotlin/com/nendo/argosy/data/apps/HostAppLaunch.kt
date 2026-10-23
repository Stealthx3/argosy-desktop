package com.nendo.argosy.data.apps

import com.nendo.argosy.platform.HostShell
import com.nendo.argosy.platform.PlatformIntent
import com.nendo.argosy.platform.PlatformSettings
import com.nendo.argosy.platform.PlatformIntentType as Intent

object HostAppLaunch {
    private const val EXTRA_APP_COMMAND = "argosy.desktop.app_command"
    private const val EXTRA_APP_DIRECTORY = "argosy.desktop.app_directory"

    fun intent(app: HostApplication): Intent = Intent(PlatformIntent.ACTION_MAIN).apply {
        intentPackage = app.id
        extras[EXTRA_APP_COMMAND] = app.exec
        extras[EXTRA_APP_DIRECTORY] = app.workingDirectory
    }

    @Suppress("UNCHECKED_CAST")
    fun launch(intent: Intent): Boolean {
        val command = intent.extras[EXTRA_APP_COMMAND] as? List<String> ?: return false
        HostShell.spawn(command, intent.extras[EXTRA_APP_DIRECTORY] as? String)
        return true
    }

    fun openStorePage(intent: Intent): Boolean {
        if (intent.intentAction != PlatformIntent.ACTION_DELETE &&
            intent.intentAction != PlatformSettings.ACTION_APPLICATION_DETAILS_SETTINGS
        ) return false
        val id = intent.intentData?.toString()?.removePrefix("package:")?.takeIf { it.isNotBlank() } ?: return false
        HostShell.spawn(listOf("xdg-open", "appstream://$id"), null)
        return true
    }
}
