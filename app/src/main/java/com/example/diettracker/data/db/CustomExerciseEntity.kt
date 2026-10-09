package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created exercise.
 *
 * Deliberately has **no upper/lower-body field**: the built-in library assigns
 * increments by region, but a custom movement the user invented should not have
 * to be classified. It simply gets [incrementKg] (default 2.5) which the user can
 * change.
 *
 * [stretchNames] is a comma-separated list of stretch names the exercise links
 * to, so tapping "拉伸 ↗" on the exercise can jump to them.
 */
@Entity(tableName = "custom_exercises")
data class CustomExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    /** `BodyPart.name`, used only for grouping in the library. */
    @ColumnInfo(name = "body_part")
    val bodyPart: String,

    /** Free-text primary muscle, e.g. "胸大肌上部". */
    @ColumnInfo(name = "primary_muscle")
    val primaryMuscle: String = "",

    /** Load step used by the progression engine, in kg. */
    @ColumnInfo(name = "increment_kg")
    val incrementKg: Double = 2.5,

    /** Comma-separated stretch names linked from the stretch library. */
    @ColumnInfo(name = "stretch_names")
    val stretchNames: String = "",

    /** Optional coaching cue. */
    @ColumnInfo(name = "cue")
    val cue: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    val stretchList: List<String>
        get() = stretchNames.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    companion object {
        fun encode(items: List<String>): String =
            items.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")

        fun decodeStretches(raw: String): List<String> =
            raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
