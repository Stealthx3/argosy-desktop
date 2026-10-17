package com.nendo.argosy.ui

import androidx.annotation.StringRes

data class DrawerItem(
    val route: String,
    @StringRes val labelRes: Int
)
