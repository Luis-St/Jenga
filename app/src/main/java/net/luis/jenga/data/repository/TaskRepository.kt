package net.luis.jenga.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.luis.jenga.data.local.AppDatabase
import net.luis.jenga.data.local.entity.CategoryEntity
import net.luis.jenga.data.local.entity.TaskCategoryCrossRef
import net.luis.jenga.data.local.entity.TaskEntity
import net.luis.jenga.domain.model.Category
import net.luis.jenga.domain.model.Task

class TaskRepository(private val database: AppDatabase) {

    val allTasks: Flow<List<Task>> = database.taskDao().getAllWithCategories().map { list ->
        list.map { it.toTask() }
    }

    val allCategories: Flow<List<Category>> = database.categoryDao().getAll().map { list ->
        list.map { Category(it.id, it.name) }
    }

    suspend fun getTaskById(id: Long): Task? =
        database.taskDao().getByIdWithCategories(id)?.toTask()

    suspend fun saveTask(task: Task, categoryIds: List<Long>) {
        val entity = TaskEntity(id = task.id, title = task.title, description = task.description)
        val taskId = if (task.id == 0L) {
            database.taskDao().insert(entity)
        } else {
            database.taskDao().update(entity)
            task.id
        }
        database.taskDao().deleteCrossRefsForTask(taskId)
        categoryIds.forEach { catId ->
            database.taskDao().insertCrossRef(TaskCategoryCrossRef(taskId, catId))
        }
    }

    suspend fun deleteTask(task: Task) {
        database.taskDao().delete(TaskEntity(id = task.id, title = task.title, description = task.description))
    }

    suspend fun saveCategory(category: Category): Long =
        database.categoryDao().insert(CategoryEntity(id = category.id, name = category.name))

    suspend fun updateCategory(category: Category) =
        database.categoryDao().update(CategoryEntity(id = category.id, name = category.name))

    suspend fun deleteCategory(category: Category) =
        database.categoryDao().delete(CategoryEntity(id = category.id, name = category.name))

    suspend fun getAllTasksOnce(): List<Task> =
        database.taskDao().getAllWithCategoriesOnce().map { it.toTask() }

    suspend fun getAllCategoriesOnce(): List<Category> =
        database.categoryDao().getAllOnce().map { Category(it.id, it.name) }
}

private fun net.luis.jenga.data.local.relation.TaskWithCategories.toTask() = Task(
    id = task.id,
    title = task.title,
    description = task.description,
    categories = categories.map { Category(it.id, it.name) }
)
