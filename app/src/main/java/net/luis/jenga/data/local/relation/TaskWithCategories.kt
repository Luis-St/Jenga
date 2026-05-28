package net.luis.jenga.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import net.luis.jenga.data.local.entity.CategoryEntity
import net.luis.jenga.data.local.entity.TaskCategoryCrossRef
import net.luis.jenga.data.local.entity.TaskEntity

data class TaskWithCategories(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskCategoryCrossRef::class,
            parentColumn = "taskId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<CategoryEntity>
)
