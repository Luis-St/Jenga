package net.luis.jenga.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import net.luis.jenga.data.local.AppDatabase
import net.luis.jenga.data.local.entity.DistributionEntity
import net.luis.jenga.domain.model.Distribution
import net.luis.jenga.domain.model.DistributionGroup

class DistributionRepository(private val database: AppDatabase) {

    private val json = Json { ignoreUnknownKeys = true }

    val allDistributions: Flow<List<Distribution>> = database.distributionDao().getAll().map { list ->
        list.map { it.toDistribution() }
    }

    suspend fun getDistributionById(id: Long): Distribution? =
        database.distributionDao().getById(id)?.toDistribution()

    suspend fun saveDistribution(distribution: Distribution): Long {
        val groupsJson = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(DistributionGroup.serializer()),
            distribution.groups
        )
        val entity = DistributionEntity(
            id = distribution.id,
            name = distribution.name,
            groupsJson = groupsJson
        )
        return if (distribution.id == 0L) {
            database.distributionDao().insert(entity)
        } else {
            database.distributionDao().update(entity)
            distribution.id
        }
    }

    suspend fun deleteDistribution(distribution: Distribution) {
        val groupsJson = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(DistributionGroup.serializer()),
            distribution.groups
        )
        database.distributionDao().delete(
            DistributionEntity(id = distribution.id, name = distribution.name, groupsJson = groupsJson)
        )
    }

    suspend fun getAllDistributionsOnce(): List<Distribution> =
        database.distributionDao().getAllOnce().map { it.toDistribution() }

    private fun DistributionEntity.toDistribution(): Distribution {
        val groups = try {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(DistributionGroup.serializer()),
                groupsJson
            )
        } catch (e: Exception) {
            emptyList()
        }
        return Distribution(id = id, name = name, groups = groups)
    }
}
