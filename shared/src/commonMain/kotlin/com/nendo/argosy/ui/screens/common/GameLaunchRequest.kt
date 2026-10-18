package com.nendo.argosy.ui.screens.common

import com.nendo.argosy.platform.PlatformBundle as Bundle
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.data
import com.nendo.argosy.platform.flags
import com.nendo.argosy.platform.putExtra
import com.nendo.argosy.platform.component
import com.nendo.argosy.platform.`package`
import com.nendo.argosy.platform.getStringExtra
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.className

/**
 * A game intent with the placement its display target resolved to.
 */
data class GameLaunchRequest(
    val intent: com.nendo.argosy.platform.PlatformIntentType,
    val options: Bundle? = null
)
