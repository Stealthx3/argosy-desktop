package com.nendo.argosy.data.storage

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect class StorageAttributionRepository {
    val snapshot: StateFlow<StorageSnapshot?>

    val walkProgress: StateFlow<Map<StorageCategory, WalkState>>

    val volumes: StateFlow<List<StorageVolumeInfo>>

    val isRefreshing: StateFlow<Boolean>

    val dirtyCategories: StateFlow<Set<StorageCategory>>

    fun markDirty(category: StorageCategory)

    fun refresh(force: Boolean = false, deep: Boolean = false): Job

    fun refreshOnOpen(): Job

    fun isPathAvailable(path: String): Boolean

    fun refreshVolumes(): List<StorageVolumeInfo>
}
