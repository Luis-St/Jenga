package net.luis.jenga.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import net.luis.jenga.data.local.dao.CategoryDao
import net.luis.jenga.data.local.dao.DistributionDao
import net.luis.jenga.data.local.dao.TaskDao
import net.luis.jenga.data.local.entity.CategoryEntity
import net.luis.jenga.data.local.entity.DistributionEntity
import net.luis.jenga.data.local.entity.TaskCategoryCrossRef
import net.luis.jenga.data.local.entity.TaskEntity

@Database(
    entities = [
        CategoryEntity::class,
        TaskEntity::class,
        TaskCategoryCrossRef::class,
        DistributionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun distributionDao(): DistributionDao
}
