package androidx.hilt.navigation.compose

import androidx.compose.runtime.Composable
import spike.FakeViewModels

/** Hilt's view-model lookup, answered from the spike's registry of fake view models. */
@Composable
inline fun <reified VM : Any> hiltViewModel(): VM = FakeViewModels.get(VM::class)
