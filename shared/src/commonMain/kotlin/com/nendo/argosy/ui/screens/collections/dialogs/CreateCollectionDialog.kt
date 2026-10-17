package com.nendo.argosy.ui.screens.collections.dialogs

import androidx.compose.runtime.Composable
import com.nendo.argosy.res.stringResource
import com.nendo.argosy.res.ArgosyR as R

@Composable
fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    gamepadInput: Boolean = true
) {
    CollectionNameDialog(
        title = stringResource(R.string.collections_create_dialog_title),
        label = stringResource(R.string.collections_create_dialog_name_label),
        confirmLabel = stringResource(R.string.collections_create_dialog_confirm),
        initialName = "",
        gamepadInput = gamepadInput,
        onDismiss = onDismiss,
        onSubmit = onCreate
    )
}
