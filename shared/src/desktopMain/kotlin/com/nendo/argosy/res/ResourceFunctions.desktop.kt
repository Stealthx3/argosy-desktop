package com.nendo.argosy.res

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.nendo.argosy.shared.resources.AndroidResourceIds

@Composable
actual fun stringResource(id: Int): String =
    org.jetbrains.compose.resources.stringResource(AndroidResourceIds.string(id))

@Composable
actual fun stringResource(id: Int, vararg formatArgs: Any): String =
    String.format(stringResource(id), *formatArgs)

@Composable
actual fun pluralStringResource(id: Int, count: Int): String =
    org.jetbrains.compose.resources.pluralStringResource(AndroidResourceIds.plurals(id), count)

@Composable
actual fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String =
    String.format(pluralStringResource(id, count), *formatArgs)

@Composable
actual fun painterResource(id: Int): Painter =
    org.jetbrains.compose.resources.painterResource(AndroidResourceIds.drawable(id))
