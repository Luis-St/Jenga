package net.luis.jenga.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.luis.jenga.data.local.entity.DistributionEntity

@Dao
interface DistributionDao {
    @Query("SELECT * FROM distributions ORDER BY name ASC")
    fun getAll(): Flow<List<DistributionEntity>>

    @Query("SELECT * FROM distributions ORDER BY name ASC")
    suspend fun getAllOnce(): List<DistributionEntity>

    @Query("SELECT * FROM distributions WHERE id = :id")
    suspend fun getById(id: Long): DistributionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(distribution: DistributionEntity): Long

    @Update
    suspend fun update(distribution: DistributionEntity)

    @Delete
    suspend fun delete(distribution: DistributionEntity)
}
