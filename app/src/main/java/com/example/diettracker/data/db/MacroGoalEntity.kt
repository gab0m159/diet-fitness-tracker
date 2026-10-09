package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The single-row table holding the daily macro goals.
 *
 * There is exactly one row, whose primary key is always [SINGLETON_ID] = 1.
 * Grams are stored as Double so goals like 122.5 g round-trip exactly.
 */
@Entity(tableName = "macro_goals")
data class MacroGoalEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLETON_ID,

    @ColumnInfo(name = "carbs_grams")
    val carbsGrams: Double,

    @ColumnInfo(name = "protein_grams")
    val proteinGrams: Double,

    @ColumnInfo(name = "fat_grams")
    val fatGrams: Double,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 1

        /** Defaults roughly matching a 2000 kcal split of 50 / 25 / 25. */
        fun default() = MacroGoalEntity(
            carbsGrams = 250.0,
            proteinGrams = 125.0,
            fatGrams = 55.0
        )
    }
}
