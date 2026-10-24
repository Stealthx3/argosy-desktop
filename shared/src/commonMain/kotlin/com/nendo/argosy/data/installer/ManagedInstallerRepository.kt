package com.nendo.argosy.data.installer

import com.nendo.argosy.data.local.dao.ManagedInstallerDao
import com.nendo.argosy.data.local.entity.ManagedInstallerEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class GitHubRepoRef(val owner: String, val name: String)

@Singleton
class ManagedInstallerRepository @Inject constructor(
    private val dao: ManagedInstallerDao
) {
    fun observeAll(): Flow<List<ManagedInstallerEntity>> = dao.observeAll()

    suspend fun getAll(): List<ManagedInstallerEntity> = dao.getAll()

    suspend fun getById(id: Long): ManagedInstallerEntity? = dao.getById(id)

    suspend fun ensureSeeded() {
        val seeds = InstallerSeeds.all
        if (InstallerSeeds.replacesUpstreamSeeds) {
            val wanted = seeds.map { it.repoOwner to it.repoName }.toSet()
            dao.getAll()
                .filter { it.locked && it.repoOwner != FLATHUB_OWNER && (it.repoOwner to it.repoName) !in wanted }
                .forEach { dao.delete(it.id) }
            seeds.forEach { seed ->
                val existing = dao.getByRepo(seed.repoOwner, seed.repoName)
                if (existing != null && existing.tagAtInstall == null && existing.packageName != seed.packageName) {
                    dao.upsert(existing.copy(packageName = seed.packageName))
                }
            }
        }
        seeds.forEach { seed ->
            val existing = dao.getByRepo(seed.repoOwner, seed.repoName)
            if (existing == null) {
                dao.upsert(seed)
                return@forEach
            }
            if (!existing.locked || existing.displayName != seed.displayName) {
                dao.upsert(
                    existing.copy(
                        displayName = seed.displayName,
                        packageName = existing.packageName ?: seed.packageName,
                        locked = true,
                        sortOrder = seed.sortOrder
                    )
                )
            }
        }
    }

    suspend fun add(ref: GitHubRepoRef, displayName: String): Long =
        dao.upsert(
            ManagedInstallerEntity(
                repoOwner = ref.owner,
                repoName = ref.name,
                displayName = displayName
            )
        )

    suspend fun remove(id: Long): Boolean = dao.deleteUnlocked(id) > 0

    suspend fun syncPlatformRows(owner: String, rows: List<ManagedInstallerEntity>) {
        val wanted = rows.map { it.repoName }.toSet()
        dao.getAll().filter { it.repoOwner == owner && it.repoName !in wanted }.forEach { dao.delete(it.id) }
        rows.forEach { row ->
            val existing = dao.getByRepo(owner, row.repoName)
            when {
                existing == null -> dao.upsert(row)
                existing.packageName != row.packageName -> dao.upsert(existing.copy(packageName = row.packageName))
            }
        }
    }

    suspend fun recordCheck(id: Long, latestSeenTag: String?) {
        val row = dao.getById(id) ?: return
        dao.upsert(row.copy(latestSeenTag = latestSeenTag, lastCheckedAt = Instant.now()))
    }

    suspend fun recordInstall(id: Long, tag: String, packageName: String?, assetVariant: String?) {
        val row = dao.getById(id) ?: return
        dao.upsert(
            row.copy(
                tagAtInstall = tag,
                latestSeenTag = tag,
                packageName = packageName ?: row.packageName,
                assetVariant = assetVariant ?: row.assetVariant,
                lastCheckedAt = Instant.now()
            )
        )
    }

    suspend fun clearVariant(id: Long) {
        val row = dao.getById(id) ?: return
        dao.upsert(row.copy(assetVariant = null))
    }
}
