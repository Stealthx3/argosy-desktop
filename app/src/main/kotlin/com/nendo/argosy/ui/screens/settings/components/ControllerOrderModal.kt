package com.nendo.argosy.ui.screens.settings.components

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.nendo.argosy.R
import com.nendo.argosy.core.input.isPhysicalGamepad
import com.nendo.argosy.ui.components.InputButton
import com.nendo.argosy.ui.components.Modal
import com.nendo.argosy.ui.input.LocalGamepadInputHandler
import com.nendo.argosy.ui.primitives.FocusIndicators
import com.nendo.argosy.ui.primitives.argosyFocusIndicators
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.util.clickableNoFocus

@Composable
fun ControllerOrderModal(
    controller: ControllerSeatController,
    onDismiss: () -> Unit
) {
    val state by controller.state.collectAsState()
    val gamepadInputHandler = LocalGamepadInputHandler.current
    val currentOnDismiss = rememberUpdatedState(onDismiss)

    DisposableEffect(gamepadInputHandler, controller) {
        val listener: (KeyEvent) -> Boolean = { event ->
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val pressingPad = event.device?.takeIf { it.isPhysicalGamepad() }
                val gamepadEvent = gamepadInputHandler?.mapKeyToEvent(event.keyCode)
                if (controller.onPadPress(gamepadEvent, pressingPad)) currentOnDismiss.value()
            }
            true
        }
        gamepadInputHandler?.setRawKeyEventListener(listener)
        onDispose { gamepadInputHandler?.setRawKeyEventListener(null) }
    }

    val defaultName = stringResource(R.string.settings_controller_order_default_name)

    Modal(
        title = stringResource(R.string.settings_controller_order_title),
        subtitle = stringResource(R.string.settings_controller_order_claim_subtitle),
        baseWidth = Dimens.modalWidthLg,
        onDismiss = onDismiss,
        footerHints = listOf(
            InputButton.Y to stringResource(R.string.settings_controller_order_hint_clear_all)
        ),
        inlineFooterHints = true,
        onFooterHintClick = { button ->
            if (button == InputButton.Y) controller.clearAll()
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
            state.seats.forEachIndexed { port, holder ->
                SeatRow(
                    seatLabel = stringResource(R.string.settings_controller_order_slot_label, port + 1),
                    occupant = holder?.let { it.name ?: defaultName },
                    isWaiting = port == state.nextOpenSeat,
                    onRelease = { controller.releaseSeat(port) }
                )
            }
        }
    }
}

@Composable
private fun SeatRow(
    seatLabel: String,
    occupant: String?,
    isWaiting: Boolean,
    onRelease: () -> Unit
) {
    val emptyText = if (isWaiting) {
        stringResource(R.string.settings_controller_order_seat_waiting)
    } else {
        stringResource(R.string.settings_controller_order_seat_open)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.menuRowHeightLg)
            .argosyFocusIndicators(
                focused = isWaiting,
                indicators = FocusIndicators.ListRow,
                shape = RoundedCornerShape(Dimens.radiusMd)
            )
            .clickableNoFocus(enabled = occupant != null, onClick = onRelease)
            .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        Text(
            text = seatLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = occupant ?: emptyText,
            style = MaterialTheme.typography.bodyLarge,
            color = if (occupant != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
