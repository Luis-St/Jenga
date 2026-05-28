package net.luis.jenga.data.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.luis.jenga.data.local.AppDatabase
import net.luis.jenga.domain.model.DistributionGroup
import java.io.OutputStream

@Serializable
data class ExportData(
    val categories: List<ExportCategory>,
    val tasks: List<ExportTask>,
    val taskCategories: List<ExportTaskCategory>,
    val distributions: List<ExportDistribution>
)

@Serializable
data class ExportCategory(val id: Long, val name: String)

@Serializable
data class ExportTask(val id: Long, val title: String, val description: String)

@Serializable
data class ExportTaskCategory(val taskId: Long, val categoryId: Long)

@Serializable
data class ExportDistribution(val id: Long, val name: String, val groups: List<DistributionGroup>)

object DataExporter {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    suspend fun snapshot(database: AppDatabase): ExportData = buildExportData(database)

    suspend fun export(database: AppDatabase, outputStream: OutputStream) {
        val data = buildExportData(database)
        val jsonString = json.encodeToString(ExportData.serializer(), data)
        outputStream.bufferedWriter().use { it.write(jsonString) }
    }

    private suspend fun buildExportData(database: AppDatabase): ExportData {
        val categories = database.categoryDao().getAllOnce()
            .map { ExportCategory(it.id, it.name) }

        val tasks = database.taskDao().getAllWithCategoriesOnce()
        val exportTasks = tasks.map { ExportTask(it.task.id, it.task.title, it.task.description) }
        val taskCategories = tasks.flatMap { twc ->
            twc.categories.map { ExportTaskCategory(twc.task.id, it.id) }
        }

        val distributions = database.distributionDao().getAllOnce().map { entity ->
            val groups = try {
                json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(DistributionGroup.serializer()),
                    entity.groupsJson
                )
            } catch (e: Exception) { emptyList() }
            ExportDistribution(entity.id, entity.name, groups)
        }

        return ExportData(categories, exportTasks, taskCategories, distributions)
    }
}
