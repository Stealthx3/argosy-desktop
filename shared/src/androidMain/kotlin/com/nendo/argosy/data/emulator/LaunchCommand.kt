package com.nendo.argosy.data.emulator

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.nendo.argosy.util.Logger

private const val TAG = "LaunchCommand"

fun EffectiveLaunchCommand.toIntent(context: Context): Intent {
    val ownFileProviderAuthority = "${context.packageName}.fileprovider"
    for (uri in grantReadUriTo) {
        if (uri.scheme != "content") {
            Logger.warn(TAG, "Skipping URI grant for non-content scheme: $uri")
            continue
        }
        if (uri.authority != ownFileProviderAuthority) {
            Logger.warn(TAG, "Refusing URI grant to $packageName for foreign authority '${uri.authority}' - only $ownFileProviderAuthority can be granted")
            continue
        }
        try {
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            Logger.warn(TAG, "Failed to grant URI permission to $packageName for $uri", e)
        }
    }

    val cmdCategories = categories
    val cmdExtras = extras
    val cmdClipDataUri = clipDataUri
    val cmdFlags = intentFlags

    val intent = Intent(action)
    if (activityClass != null) {
        intent.component = ComponentName(packageName, activityClass)
    } else {
        intent.setPackage(packageName)
    }
    for (category in cmdCategories) {
        intent.addCategory(category)
    }
    if (dataUri != null) {
        if (mimeType != null) {
            intent.setDataAndType(dataUri, mimeType)
        } else {
            intent.data = dataUri
        }
    }
    for (extra in cmdExtras) {
        when (extra) {
            is ResolvedExtra.StringExtra -> intent.putExtra(extra.key, extra.value)
            is ResolvedExtra.UriExtra -> intent.putExtra(extra.key, extra.uri)
            is ResolvedExtra.BoolExtra -> intent.putExtra(extra.key, extra.value)
            is ResolvedExtra.StringArrayExtra -> intent.putExtra(extra.key, extra.values)
        }
    }
    if (cmdClipDataUri != null) {
        intent.clipData = ClipData.newRawUri(null, cmdClipDataUri)
    }
    if (cmdFlags != 0) {
        intent.addFlags(cmdFlags)
    }

    val hasContentUri = dataUri?.scheme == "content" ||
        cmdExtras.any { it is ResolvedExtra.UriExtra && it.uri.scheme == "content" } ||
        cmdClipDataUri?.scheme == "content" ||
        grantReadUriTo.isNotEmpty()
    if (hasContentUri) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return intent
}
