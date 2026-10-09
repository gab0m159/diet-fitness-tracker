package com.example.diettracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = ${UserProfileEntity.SINGLETON_ID}")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = ${UserProfileEntity.SINGLETON_ID}")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

/** 用户自建动作（只有名字、部位、肌群等；不再有「步进」概念）。 */
@Dao
interface CustomExerciseDao {

    @Query("SELECT * FROM custom_exercises ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CustomExerciseEntity>>

    @Query("SELECT * FROM custom_exercises ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CustomExerciseEntity>

    @Query("SELECT * FROM custom_exercises WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): CustomExerciseEntity?

    @Query(
        "SELECT COUNT(*) FROM custom_exercises " +
            "WHERE name = :name COLLATE NOCASE AND id != :excludeId"
    )
    suspend fun countWithName(name: String, excludeId: Long = 0L): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomExerciseEntity): Long

    @Update
    suspend fun update(entity: CustomExerciseEntity)

    @Delete
    suspend fun delete(entity: CustomExerciseEntity)

    @Query("DELETE FROM custom_exercises WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CustomStretchDao {

    @Query("SELECT * FROM custom_stretches ORDER BY target_muscle ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CustomStretchEntity>>

    @Query("SELECT * FROM custom_stretches ORDER BY target_muscle ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CustomStretchEntity>

    @Query(
        "SELECT COUNT(*) FROM custom_stretches " +
            "WHERE name = :name COLLATE NOCASE AND id != :excludeId"
    )
    suspend fun countWithName(name: String, excludeId: Long = 0L): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomStretchEntity): Long

    @Update
    suspend fun update(entity: CustomStretchEntity)

    @Delete
    suspend fun delete(entity: CustomStretchEntity)

    @Query("DELETE FROM custom_stretches WHERE id = :id")
    suspend fun deleteById(id: Long)
}
