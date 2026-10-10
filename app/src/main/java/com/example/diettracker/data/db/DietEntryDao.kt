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

    /**
     * 月历用：一段日期区间内，每天记录了几条、合计多少热量。
     *
     * 注意列名要加**反引号并显式起别名**：`date` 是 SQLite 的内建函数名，
     * 裸写 `SELECT date` 会被当成函数调用，Room 拿不到列就映射失败、运行期崩溃。
     */
    @Query(
        """
        SELECT `date` AS date,
               COUNT(*) AS entryCount,
               COALESCE(SUM(
                   CASE
                     WHEN variant_kcal_per_serving > 0
                       THEN variant_kcal_per_serving * (CASE WHEN servings > 0 THEN servings ELSE 1 END)
                     ELSE (carbs_per_100g * 4 + protein_per_100g * 4 + fat_per_100g * 9) * grams / 100.0
                   END
               ), 0.0) AS kcal
        FROM diet_entries
        WHERE `date` BETWEEN :from AND :to
        GROUP BY `date`
        """
    )
    suspend fun dailyTotalsBetween(from: String, to: String): List<DailyEntryTotal>
}

/** 某一天的饮食汇总（月历格子用）。 */
data class DailyEntryTotal(
    val date: String,
    val entryCount: Int,
    val kcal: Double
)
