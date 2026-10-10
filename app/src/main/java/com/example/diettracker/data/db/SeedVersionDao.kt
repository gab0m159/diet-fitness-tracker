package com.example.diettracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 内置数据的版本戳读写。 */
@Dao
interface SeedVersionDao {

    @Query("SELECT * FROM seed_versions WHERE seed_key = :key LIMIT 1")
    suspend fun get(key: String): SeedVersionEntity?

    @Query("SELECT * FROM seed_versions")
    suspend fun getAll(): List<SeedVersionEntity>

    @Query("SELECT * FROM seed_versions")
    fun observeAll(): Flow<List<SeedVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SeedVersionEntity)

    @Query("DELETE FROM seed_versions")
    suspend fun clear()

    /** 已经装到第几版；没记录过返回 0（表示从没装过）。 */
    suspend fun installedVersion(key: String): Int = get(key)?.version ?: 0
}
