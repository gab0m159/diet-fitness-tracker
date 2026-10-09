package com.example.diettracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodVariantDao {

    @Query("SELECT * FROM food_variants WHERE food_id = :foodId ORDER BY position ASC")
    fun observeForFood(foodId: Long): Flow<List<FoodVariantEntity>>

    @Query("SELECT * FROM food_variants WHERE food_id = :foodId ORDER BY position ASC")
    suspend fun getForFood(foodId: Long): List<FoodVariantEntity>

    /**
     * Variants for several foods at once, so a list screen can render "一份 xxx"
     * without an N+1 query.
     */
    @Query("SELECT * FROM food_variants ORDER BY food_id ASC, position ASC")
    fun observeAll(): Flow<List<FoodVariantEntity>>

    @Query("SELECT * FROM food_variants ORDER BY food_id ASC, position ASC")
    suspend fun getAll(): List<FoodVariantEntity>

    @Query("SELECT COUNT(*) FROM food_variants")
    suspend fun count(): Int

    @Query("SELECT id FROM food_variants WHERE food_id = :foodId")
    suspend fun idsForFood(foodId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(variant: FoodVariantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(variants: List<FoodVariantEntity>): List<Long>

    @Query("DELETE FROM food_variants WHERE food_id = :foodId")
    suspend fun deleteForFood(foodId: Long)

    @Query("DELETE FROM food_variants WHERE id = :id")
    suspend fun deleteById(id: Long)
}
