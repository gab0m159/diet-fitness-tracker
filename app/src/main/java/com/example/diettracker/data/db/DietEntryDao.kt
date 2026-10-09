package com.example.diettracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DietEntryDao {

    @Query("SELECT * FROM diet_entries WHERE date = :date ORDER BY created_at ASC, id ASC")
    fun observeByDate(date: String): Flow<List<DietEntryEntity>>

    @Query("SELECT * FROM diet_entries WHERE id = :id")
    suspend fun getById(id: Long): DietEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: DietEntryEntity): Long

    @Update
    suspend fun update(entry: DietEntryEntity)

    @Delete
    suspend fun delete(entry: DietEntryEntity)

    @Query("DELETE FROM diet_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM diet_entries WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query(
        """
        SELECT COUNT(*) FROM diet_entries
        WHERE food_id = :foodId
        """
    )
    suspend fun countByFood(foodId: Long): Int
}
