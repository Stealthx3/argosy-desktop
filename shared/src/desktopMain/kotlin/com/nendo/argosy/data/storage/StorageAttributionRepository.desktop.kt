package com.nendo.argosy.data.storage

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class StorageAttributionRepository @Inject constructor() {
    actual val snapshot: StateFlow<StorageSnapshot?> = kotlinx.coroutines.flow.MutableStateFlow(null)

    actual val walkProgress: StateFlow<Map<StorageCategory, WalkState>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())

    actual val volumes: StateFlow<List<StorageVolumeInfo>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    actual val isRefreshing: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

    actual val dirtyCategories: StateFlow<Set<StorageCategory>> = kotlinx.coroutines.flow.MutableStateFlow(emptySet())

    actual fun markDirty(category: StorageCategory) {
    }

    actual fun refresh(force: Boolean, deep: Boolean): Job = kotlinx.coroutines.Job().apply { complete() }

    actual fun refreshOnOpen(): Job = kotlinx.coroutines.Job().apply { complete() }

    actual fun isPathAvailable(path: String): Boolean = false

    actual fun refreshVolumes(): List<StorageVolumeInfo> = emptyList()
}
