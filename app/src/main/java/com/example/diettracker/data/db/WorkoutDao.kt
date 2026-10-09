package com.example.diettracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY created_at ASC")
    fun observeByDate(date: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY created_at ASC")
    suspend fun getByDate(date: String): List<WorkoutSessionEntity>

    /**
     * The day's session. v6 keeps one session per date, so this is a plain date
     * lookup; the ORDER BY + LIMIT only guards against duplicate rows written by
     * an older build that has not been migrated yet.
     */
    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE date = :date
        ORDER BY created_at DESC, id DESC LIMIT 1
        """
    )
    suspend fun findForDate(date: String): WorkoutSessionEntity?

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE status = 'COMPLETED'
        ORDER BY date DESC, created_at DESC LIMIT :limit
        """
    )
    fun observeRecentCompleted(limit: Int = 20): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutSessionEntity?>

    /**
     * Total burn for a day, counting only finished sessions — a postponed or
     * skipped slot did not burn anything.
     */
    @Query(
        """
        SELECT COALESCE(SUM(burned_kcal), 0.0) FROM workout_sessions
        WHERE date = :date AND status = 'COMPLETED'
        """
    )
    fun observeBurnedKcal(date: String): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(burned_kcal), 0.0) FROM workout_sessions
        WHERE date = :date AND status = 'COMPLETED'
        """
    )
    suspend fun getBurnedKcal(date: String): Double

    /** Burn totals for the last [days] days, for the training summary. */
    @Query(
        """
        SELECT COALESCE(SUM(burned_kcal), 0.0) FROM workout_sessions
        WHERE status = 'COMPLETED' AND date >= :fromDate
        """
    )
    fun observeBurnedKcalSince(fromDate: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSessionEntity): Long

    @Update
    suspend fun update(session: WorkoutSessionEntity)

    @Delete
    suspend fun delete(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** How many completed sessions exist; used to decide "first ever". */
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE status = 'COMPLETED'")
    suspend fun completedCount(): Int
}

@Dao
interface ExerciseRecordDao {

    @Query("SELECT * FROM exercise_records WHERE session_id = :sessionId ORDER BY position ASC")
    fun observeBySession(sessionId: Long): Flow<List<ExerciseRecordEntity>>

    @Query("SELECT * FROM exercise_records WHERE session_id = :sessionId ORDER BY position ASC")
    suspend fun getBySession(sessionId: Long): List<ExerciseRecordEntity>

    /**
     * The most recent completed performance of an exercise, which is what the
     * progression engine compares against.
     */
    @Query(
        """
        SELECT r.* FROM exercise_records r
        INNER JOIN workout_sessions s ON r.session_id = s.id
        WHERE r.exercise_name = :exerciseName AND s.status = 'COMPLETED'
        ORDER BY r.date DESC, r.created_at DESC, r.id DESC LIMIT 1
        """
    )
    suspend fun latestForExercise(exerciseName: String): ExerciseRecordEntity?

    @Query(
        """
        SELECT r.* FROM exercise_records r
        INNER JOIN workout_sessions s ON r.session_id = s.id
        WHERE r.exercise_name = :exerciseName AND s.status = 'COMPLETED'
        ORDER BY r.date DESC, r.created_at DESC, r.id DESC LIMIT :limit
        """
    )
    suspend fun historyForExercise(exerciseName: String, limit: Int = 10): List<ExerciseRecordEntity>

    /**
     * Sets performed for a muscle group since a date, used by the volume-first
     * tier of the fallback ladder.
     */
    @Query(
        """
        SELECT COALESCE(SUM(r.sets), 0) FROM exercise_records r
        INNER JOIN workout_sessions s ON r.session_id = s.id
        WHERE r.exercise_name IN (:exerciseNames)
          AND s.status = 'COMPLETED'
          AND r.date >= :fromDate
        """
    )
    suspend fun setsSince(exerciseNames: List<String>, fromDate: String): Int

    /** All-time best load for an exercise, shown as a reference. */
    @Query(
        """
        SELECT COALESCE(MAX(r.weight_kg), 0.0) FROM exercise_records r
        INNER JOIN workout_sessions s ON r.session_id = s.id
        WHERE r.exercise_name = :exerciseName AND s.status = 'COMPLETED'
        """
    )
    suspend fun personalBest(exerciseName: String): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ExerciseRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ExerciseRecordEntity>)

    @Update
    suspend fun update(record: ExerciseRecordEntity)

    @Query("DELETE FROM exercise_records WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("DELETE FROM exercise_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Replaces every exercise record of a session in one transaction, which is
     * how the workout log screen saves an edited session.
     */
    @Transaction
    suspend fun replaceSessionRecords(sessionId: Long, records: List<ExerciseRecordEntity>) {
        deleteBySession(sessionId)
        insertAll(records)
    }
}
