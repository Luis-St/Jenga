package net.luis.jenga.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.luis.jenga.data.local.entity.TaskCategoryCrossRef
import net.luis.jenga.data.local.entity.TaskEntity
import net.luis.jenga.data.local.relation.TaskWithCategories

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY title ASC")
    fun getAllWithCategories(): Flow<List<TaskWithCategories>>

    @Transaction
    @Query("SELECT * FROM tasks ORDER BY title ASC")
    suspend fun getAllWithCategoriesOnce(): List<TaskWithCategories>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getByIdWithCategories(id: Long): TaskWithCategories?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: TaskCategoryCrossRef)

    @Query("DELETE FROM task_category_cross_ref WHERE taskId = :taskId")
    suspend fun deleteCrossRefsForTask(taskId: Long)
}
