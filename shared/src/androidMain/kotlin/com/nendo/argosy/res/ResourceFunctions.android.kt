package com.nendo.argosy.res

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@Composable
actual fun stringResource(id: Int): String = androidx.compose.ui.res.stringResource(id)

@Composable
actual fun stringResource(id: Int, vararg formatArgs: Any): String =
    androidx.compose.ui.res.stringResource(id, *formatArgs)

@Composable
actual fun pluralStringResource(id: Int, count: Int): String =
    androidx.compose.ui.res.pluralStringResource(id, count)

@Composable
actual fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String =
    androidx.compose.ui.res.pluralStringResource(id, count, *formatArgs)

@Composable
actual fun painterResource(id: Int): Painter = androidx.compose.ui.res.painterResource(id)
