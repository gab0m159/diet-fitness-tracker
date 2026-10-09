package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One exercise performed inside a [WorkoutSessionEntity].
 *
 * The rep range is **snapshotted** ([repsMin]/[repsMax] at the time of training)
 * so that changing the training objective later does not rewrite history or
 * break the progression comparison against the previous session.
 *
 * [repsPerSet] is a comma-separated list such as "12,12,10" — chosen over a
 * child table because it is only ever read as a whole.
 */
@Entity(
    tableName = "exercise_records",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id"), Index("exercise_name")]
)
data class ExerciseRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    /** Date snapshot, so "last time I did this" is a single indexed lookup. */
    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,

    /** Load used, in kg. */
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,

    /** Number of working sets. */
    @ColumnInfo(name = "sets")
    val sets: Int,

    /** Comma-separated reps per set, e.g. "12,12,10". */
    @ColumnInfo(name = "reps_per_set")
    val repsPerSet: String,

    /** Rep range targeted at the time of this session. */
    @ColumnInfo(name = "reps_min")
    val repsMin: Int,

    @ColumnInfo(name = "reps_max")
    val repsMax: Int,

    /** Order within the session. */
    @ColumnInfo(name = "position")
    val position: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Parsed reps, ignoring blanks. */
    val reps: List<Int>
        get() = repsPerSet.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }

    /** Total reps across all sets. */
    val totalReps: Int get() = reps.sum()

    /** Rough volume load, kg x reps. */
    val volume: Double get() = weightKg * totalReps

    /** "60kg · 3 组 × 12,12,10" */
    val summary: String
        get() = "${formatKg(weightKg)}kg · $sets 组 × ${repsPerSet.ifBlank { "-" }}"

    private fun formatKg(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else String.format(java.util.Locale.US, "%.1f", value)

    companion object {
        fun encodeReps(reps: List<Int>): String =
            reps.filter { it > 0 }.joinToString(",")

        fun decodeReps(raw: String): List<Int> =
            raw.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
    }
}
