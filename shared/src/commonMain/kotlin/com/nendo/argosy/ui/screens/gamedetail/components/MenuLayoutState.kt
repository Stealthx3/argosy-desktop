package com.nendo.argosy.ui.screens.gamedetail.components

import androidx.compose.runtime.getValue

data class MenuLayoutState(
    val hasDescription: Boolean = false,
    val hasScreenshots: Boolean = false,
    val hasAchievements: Boolean = false,
    val hasSocialAccount: Boolean = false,
    val hasSaveSync: Boolean = false,
    val hasRelated: Boolean = false,
    val hasPerGameSettings: Boolean = false,
    val hasDocuments: Boolean = false
)
