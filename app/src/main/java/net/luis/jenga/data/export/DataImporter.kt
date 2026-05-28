package net.luis.jenga.data.export

import kotlinx.serialization.json.Json
import net.luis.jenga.data.local.AppDatabase
import net.luis.jenga.data.local.entity.CategoryEntity
import net.luis.jenga.data.local.entity.DistributionEntity
import net.luis.jenga.data.local.entity.TaskCategoryCrossRef
import net.luis.jenga.data.local.entity.TaskEntity
import net.luis.jenga.domain.model.DistributionGroup
import java.io.InputStream

object DataImporter {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun importData(database: AppDatabase, inputStream: InputStream) {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        importFromData(database, json.decodeFromString(ExportData.serializer(), jsonString))
    }

    suspend fun importFromData(database: AppDatabase, exportData: ExportData) {
        database.clearAllTables()

        val categoryIdMap = mutableMapOf<Long, Long>()
        for (cat in exportData.categories) {
            val newId = database.categoryDao().insert(CategoryEntity(name = cat.name))
            categoryIdMap[cat.id] = newId
        }

        val taskIdMap = mutableMapOf<Long, Long>()
        for (task in exportData.tasks) {
            val newId = database.taskDao().insert(TaskEntity(title = task.title, description = task.description))
            taskIdMap[task.id] = newId
        }

        for (ref in exportData.taskCategories) {
            val newTaskId = taskIdMap[ref.taskId] ?: continue
            val newCatId = categoryIdMap[ref.categoryId] ?: continue
            database.taskDao().insertCrossRef(TaskCategoryCrossRef(newTaskId, newCatId))
        }

        for (dist in exportData.distributions) {
            val groupsJson = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(DistributionGroup.serializer()),
                dist.groups
            )
            database.distributionDao().insert(DistributionEntity(name = dist.name, groupsJson = groupsJson))
        }
    }
}
