@file:JvmName("ArgosyAndroidResourceShims")

package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.nendo.argosy.shared.resources.AndroidResourceIds

@Composable
fun stringResource(id: Int): String =
    org.jetbrains.compose.resources.stringResource(AndroidResourceIds.string(id))

@Composable
fun stringResource(id: Int, vararg formatArgs: Any): String =
    String.format(stringResource(id), *formatArgs)

@Composable
fun pluralStringResource(id: Int, count: Int): String =
    org.jetbrains.compose.resources.pluralStringResource(AndroidResourceIds.plurals(id), count)

@Composable
fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String =
    String.format(pluralStringResource(id, count), *formatArgs)

@Composable
fun painterResource(id: Int): Painter =
    org.jetbrains.compose.resources.painterResource(AndroidResourceIds.drawable(id))
