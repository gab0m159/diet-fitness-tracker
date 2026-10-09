package com.example.diettracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroGoalDao {

    @Query("SELECT * FROM macro_goals WHERE id = ${MacroGoalEntity.SINGLETON_ID}")
    fun observe(): Flow<MacroGoalEntity?>

    @Query("SELECT * FROM macro_goals WHERE id = ${MacroGoalEntity.SINGLETON_ID}")
    suspend fun get(): MacroGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: MacroGoalEntity)
}
