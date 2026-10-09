package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table with the user's body metrics.
 *
 * Body-fat percentage is **optional**: everything still computes with just
 * height / weight / age / sex. There is deliberately no muscle-mass field.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLETON_ID,

    @ColumnInfo(name = "height_cm")
    val heightCm: Double,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,

    @ColumnInfo(name = "age")
    val age: Int,

    /** `Sex.name`: MALE / FEMALE. */
    @ColumnInfo(name = "sex")
    val sex: String,

    /** Optional body-fat percentage (0-70). Null when not entered. */
    @ColumnInfo(name = "body_fat_percent")
    val bodyFatPercent: Double? = null,

    /** `ActivityLevel.name`; drives the TDEE multiplier. */
    @ColumnInfo(name = "activity_level")
    val activityLevel: String = "MODERATE",

    /** `GoalMode.name`; drives the calorie adjustment and macro split. */
    @ColumnInfo(name = "goal_mode")
    val goalMode: String = "MAINTAIN",

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 1

        /** Neutral starting point so the estimate screen is never empty. */
        fun default() = UserProfileEntity(
            heightCm = 175.0,
            weightKg = 70.0,
            age = 25,
            sex = "MALE",
            bodyFatPercent = null,
            activityLevel = "MODERATE",
            goalMode = "MAINTAIN"
        )
    }
}
