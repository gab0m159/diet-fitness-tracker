package com.example.diettracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id")
    fun observeById(id: Long): Flow<FoodEntity?>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): FoodEntity?

    /** Free-text search across name, brand, tags and note. */
    @Query(
        """
        SELECT * FROM foods
        WHERE name LIKE '%' || :query || '%'
           OR note LIKE '%' || :query || '%'
           OR brand_tag LIKE '%' || :query || '%'
           OR nutrition_tags LIKE '%' || :query || '%'
        ORDER BY
          CASE category WHEN 'USER' THEN 0 WHEN 'RECOMMENDED' THEN 1 ELSE 2 END,
          name COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<FoodEntity>>

    /** Browse one library tab. */
    @Query(
        """
        SELECT * FROM foods WHERE category = :category
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeByCategory(category: String): Flow<List<FoodEntity>>

    /** Everything carrying a given brand label, e.g. all 麦当劳 items. */
    @Query(
        """
        SELECT * FROM foods WHERE brand_tag = :brand
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeByBrand(brand: String): Flow<List<FoodEntity>>

    /** Everything carrying a quality tag, e.g. all 优质碳水 foods. */
    @Query(
        """
        SELECT * FROM foods WHERE nutrition_tags LIKE '%' || :tag || '%'
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeByNutritionTag(tag: String): Flow<List<FoodEntity>>

    /** Distinct brand labels present in the library, for the filter chips. */
    @Query(
        """
        SELECT DISTINCT brand_tag FROM foods
        WHERE brand_tag != '' ORDER BY brand_tag ASC
        """
    )
    fun observeBrands(): Flow<List<String>>

    /**
     * Foods that need seeding: the bundled rows are inserted only once, guarded
     * by checking whether any row of that category already exists.
     */
    @Query("SELECT COUNT(*) FROM foods WHERE category = :category")
    suspend fun countInCategory(category: String): Int

    /** Used to warn about duplicate names before inserting. */
    @Query("SELECT COUNT(*) FROM foods WHERE name = :name COLLATE NOCASE AND id != :excludeId")
    suspend fun countWithName(name: String, excludeId: Long = 0L): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(food: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(foods: List<FoodEntity>): List<Long>

    @Update
    suspend fun update(food: FoodEntity)

    @Delete
    suspend fun delete(food: FoodEntity)

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int
}
