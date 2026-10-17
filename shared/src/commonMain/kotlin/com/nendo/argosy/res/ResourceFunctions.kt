package com.nendo.argosy.res

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@Composable
expect fun stringResource(id: Int): String

@Composable
expect fun stringResource(id: Int, vararg formatArgs: Any): String

@Composable
expect fun pluralStringResource(id: Int, count: Int): String

@Composable
expect fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String

@Composable
expect fun painterResource(id: Int): Painter
