package com.nendo.argosy.ui.primitives

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.nendo.argosy.ui.components.InputButton
import com.nendo.argosy.ui.components.toPainter
import com.nendo.argosy.ui.components.physicalKeyCode
import com.nendo.argosy.ui.components.ControllerButtonGlyph
import com.nendo.argosy.ui.util.isTvLayout
import androidx.compose.material3.MaterialTheme
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalArgosyTheme

@Composable
fun InputGlyph(
    button: InputButton,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.iconSm,
    tint: Color? = null,
) {
    val tvKey = if (isTvLayout) button.physicalKeyCode() else null
    if (tvKey != null && ControllerButtonGlyph(tvKey, size, MaterialTheme.typography.labelSmall.fontSize, modifier)) return
    val painter = button.toPainter() ?: return
    val effectiveTint = tint ?: LocalArgosyTheme.current.textDim
    Icon(
        painter = painter,
        contentDescription = button.name,
        tint = effectiveTint,
        modifier = modifier.size(size),
    )
}
