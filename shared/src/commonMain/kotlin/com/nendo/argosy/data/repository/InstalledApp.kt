package com.nendo.argosy.data.repository

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val declaresGameCategory: Boolean = false
)
