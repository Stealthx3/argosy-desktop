package com.nendo.argosy.ui.screens.apps

import com.nendo.argosy.platform.PlatformIntentType as Intent
import com.nendo.argosy.platform.action
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.data
import com.nendo.argosy.platform.flags
import com.nendo.argosy.platform.putExtra
import com.nendo.argosy.platform.component
import com.nendo.argosy.platform.`package`
import com.nendo.argosy.platform.getStringExtra
import com.nendo.argosy.platform.className
import com.nendo.argosy.platform.PlatformIntent
import com.nendo.argosy.platform.PlatformUri as Uri
import com.nendo.argosy.platform.authority
import com.nendo.argosy.platform.encodedPath
import com.nendo.argosy.platform.getQueryParameter
import com.nendo.argosy.platform.host
import com.nendo.argosy.platform.lastPathSegment
import com.nendo.argosy.platform.path
import com.nendo.argosy.platform.pathSegments
import com.nendo.argosy.platform.queryParameterNames
import com.nendo.argosy.platform.scheme
import com.nendo.argosy.platform.PlatformUris
import com.nendo.argosy.platform.PlatformSettings as Settings
import com.nendo.argosy.util.PlatformLog as Log
import com.nendo.argosy.platform.PlatformToasts as Toast
import com.nendo.argosy.platform.show
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.nendo.argosy.ui.util.clickableNoFocus
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nendo.argosy.platform.LocalPlatformContext as LocalContext
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nendo.argosy.di.hiltViewModel
import com.nendo.argosy.platform.image.SubcomposeAsyncImage
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.ui.coil.AppIconData
import com.nendo.argosy.ui.input.LocalInputDispatcher
import com.nendo.argosy.ui.input.ModalPresenceEffect
import com.nendo.argosy.ui.navigation.Screen
import com.nendo.argosy.ui.components.AppContextMenuItem
import com.nendo.argosy.ui.components.AppMenuRow
import com.nendo.argosy.ui.components.FooterHints
import com.nendo.argosy.ui.components.FooterSpacer
import com.nendo.argosy.ui.components.InputButton
import com.nendo.argosy.ui.components.liftedReorderHints
import com.nendo.argosy.ui.primitives.InputGlyph
import androidx.compose.ui.graphics.lerp
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import com.nendo.argosy.ui.theme.Motion
import com.nendo.argosy.ui.theme.generated.ColorTokens
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.blur
import com.nendo.argosy.platform.LocalPlatformConfiguration as LocalConfiguration
import com.nendo.argosy.platform.get
import com.nendo.argosy.platform.locales
import com.nendo.argosy.platform.orientation
import com.nendo.argosy.platform.screenHeightDp
import com.nendo.argosy.platform.screenWidthDp
import kotlinx.coroutines.launch

@Composable
fun AppsScreen(
    onBack: () -> Unit,
    onDrawerToggle: () -> Unit,
    viewModel: AppsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppsEvent.Launch -> {
                    try {
                        if (event.options != null) {
                            context.startActivity(event.intent, event.options)
                        } else {
                            context.startActivity(event.intent)
                        }
                    } catch (error: Exception) {
                        Log.e("AppsScreen", "Failed to launch ${event.intent.component}", error)
                        Toast.makeText(context, R.string.library_apps_launch_failed, Toast.LENGTH_SHORT).show()
                    }
                }
                is AppsEvent.OpenAppInfo -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = PlatformUris.parse("package:${event.packageName}")
                    }
                    context.startActivity(intent)
                }
                is AppsEvent.RequestUninstall -> {
                    val intent = Intent(PlatformIntent.ACTION_DELETE).apply {
                        data = PlatformUris.parse("package:${event.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    LaunchedEffect(uiState.focusedIndex) {
        if (uiState.apps.isNotEmpty()) {
            val cols = uiState.columnsCount
            val focusedRow = uiState.focusedIndex / cols
            val targetIndex = (focusedRow * cols).coerceIn(0, uiState.apps.lastIndex)
            isProgrammaticScroll = true
            scope.launch {
                gridState.animateScrollToItem(targetIndex)
                isProgrammaticScroll = false
            }
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling && !isProgrammaticScroll) {
                    viewModel.enterTouchMode()
                }
            }
    }

    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration.screenWidthDp) {
        viewModel.updateScreenWidth(configuration.screenWidthDp)
    }

    val inputDispatcher = LocalInputDispatcher.current
    val inputHandler = remember(onBack, onDrawerToggle) {
        viewModel.createInputHandler(
            onDrawerToggle = onDrawerToggle,
            onBack = onBack
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, inputHandler) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                inputDispatcher.subscribeView(inputHandler, forRoute = Screen.ROUTE_APPS)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        inputDispatcher.subscribeView(inputHandler, forRoute = Screen.ROUTE_APPS)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val modalBlur by animateDpAsState(
        targetValue = if (uiState.showContextMenu) Motion.blurRadiusModal else 0.dp,
        animationSpec = Motion.focusSpringDp,
        label = "contextMenuBlur"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().blur(modalBlur)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingLg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            uiState.isReorderMode -> stringResource(R.string.library_apps_title_reorder)
                            uiState.showHiddenApps -> stringResource(R.string.library_apps_title_hidden)
                            else -> stringResource(R.string.library_apps_title)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.isReorderMode) {
                        Spacer(modifier = Modifier.width(Dimens.spacingMd))
                        InputGlyph(button = InputButton.A)
                        Spacer(modifier = Modifier.width(Dimens.spacingXs))
                        Text(
                            text = stringResource(R.string.library_apps_reorder_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.spacingXxl),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                uiState.apps.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.library_apps_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(uiState.columnsCount),
                        state = gridState,
                        contentPadding = PaddingValues(horizontal = Dimens.spacingMd, vertical = Dimens.spacingMd),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(
                            items = uiState.apps,
                            key = { _, app -> app.packageName }
                        ) { index, app ->
                            AppCard(
                                packageName = app.packageName,
                                label = app.label,
                                isFocused = index == uiState.focusedIndex,
                                showFocus = !uiState.isTouchMode || uiState.hasSelectedApp,
                                isReorderMode = uiState.isReorderMode,
                                onClick = { viewModel.handleAppTap(index) },
                                onLongClick = { viewModel.handleAppLongPress(index) }
                            )
                        }
                    }
                }
            }

            val selectSwapMode = com.nendo.argosy.ui.dualscreen.selectSwapModeState()
            val selectSwapsRoles = selectSwapMode == com.nendo.argosy.data.preferences.SelectSwapMode.TAP
            FooterHints(
                hints = when {
                    uiState.isReorderMode -> liftedReorderHints(
                        move = stringResource(R.string.library_apps_hint_move),
                        cancel = stringResource(R.string.library_apps_hint_cancel),
                        moveButton = InputButton.DPAD
                    )
                    else -> listOf(
                        InputButton.A to stringResource(
                            if (selectSwapsRoles) R.string.library_apps_hint_open_hold_options else R.string.library_apps_hint_open
                        ),
                        InputButton.B to stringResource(R.string.library_apps_hint_back),
                        InputButton.Y to stringResource(R.string.library_apps_hint_reorder),
                        InputButton.SELECT to stringResource(
                            when (selectSwapMode) {
                                com.nendo.argosy.data.preferences.SelectSwapMode.TAP ->
                                    R.string.library_apps_hint_swap_screens
                                com.nendo.argosy.data.preferences.SelectSwapMode.HOLD ->
                                    R.string.library_apps_hint_options_hold_swap_screens
                                null -> R.string.library_apps_hint_options
                            }
                        ),
                        InputButton.X to if (uiState.showHiddenApps) {
                            stringResource(R.string.library_apps_hint_show_apps)
                        } else {
                            stringResource(R.string.library_apps_hint_show_hidden)
                        }
                    )
                },
                onHintClick = { button ->
                    if (uiState.isReorderMode) {
                        when (button) {
                            InputButton.A -> viewModel.saveReorderAndExit()
                            InputButton.B -> viewModel.cancelReorderAndExit()
                            else -> {}
                        }
                    } else {
                        when (button) {
                            InputButton.A -> uiState.focusedApp?.let { viewModel.launchAppAt(uiState.focusedIndex) }
                            InputButton.B -> onBack()
                            InputButton.Y -> viewModel.handleSecondaryAction()
                            InputButton.SELECT -> if (selectSwapsRoles) {
                                com.nendo.argosy.DualScreenManagerHolder.instance?.swapRoles()
                            } else {
                                viewModel.showContextMenuAt(uiState.focusedIndex)
                            }
                            InputButton.X -> viewModel.toggleShowHidden()
                            else -> {}
                        }
                    }
                }
            )
            FooterSpacer()
        }

        if (uiState.showContextMenu) {
            ModalPresenceEffect()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickableNoFocus { viewModel.dismissContextMenu() },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(Dimens.radiusPanel),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = Dimens.elevationLg,
                    modifier = Modifier.clickableNoFocus(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .padding(vertical = Dimens.spacingSm)
                    ) {
                        Text(
                            text = uiState.focusedApp?.label ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = Dimens.spacingMd, vertical = Dimens.radiusLg)
                        )

                        uiState.contextMenuItems.forEachIndexed { index, row ->
                            if (row is AppMenuRow.Action && row.item == AppContextMenuItem.UNINSTALL) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = Dimens.spacingSm),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                            com.nendo.argosy.ui.components.AppMenuItemRow(
                                row = row,
                                isFocused = index == uiState.contextMenuFocusIndex,
                                isAppHidden = uiState.focusedApp?.isHidden ?: false,
                                isOnHome = uiState.focusedApp?.isOnHome ?: false,
                                isOnSecondaryHome = uiState.focusedApp?.isOnSecondaryHome ?: false,
                                reorderLabel = R.string.library_apps_menu_reorder,
                                onClick = {
                                    viewModel.selectContextMenuItem(index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCard(
    packageName: String,
    label: String,
    isFocused: Boolean,
    showFocus: Boolean = true,
    modifier: Modifier = Modifier,
    isReorderMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val effectiveFocused = isFocused && showFocus
    val backgroundColor = if (effectiveFocused) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    val borderModifier = if (effectiveFocused && isReorderMode) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(Dimens.radiusMd)
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .then(borderModifier)
            .clip(RoundedCornerShape(Dimens.radiusMd))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .background(backgroundColor)
            .padding(Dimens.radiusLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubcomposeAsyncImage(
            model = AppIconData(packageName),
            contentDescription = label,
            modifier = Modifier.size(64.dp),
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(Dimens.spacingSm))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
