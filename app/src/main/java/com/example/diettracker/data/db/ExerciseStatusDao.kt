package com.example.diettracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 动作卡片结果表。
 *
 * 往期记录（[observeHistory] / [getHistory]）刻意只查 [ExerciseOutcome.recordedInHistory]，
 * 也就是只有成功和失败会出现在历史里，跳过的不显示。
 */
@Dao
interface ExerciseStatusDao {

    @Query("SELECT * FROM exercise_status WHERE date = :date ORDER BY position ASC, id ASC")
    fun observeByDate(date: String): Flow<List<ExerciseStatusEntity>>

    @Query("SELECT * FROM exercise_status WHERE date = :date ORDER BY position ASC, id ASC")
    suspend fun getByDate(date: String): List<ExerciseStatusEntity>

    @Query(
        """
        SELECT * FROM exercise_status
        WHERE date = :date AND source_kind = :sourceKind AND source_id = :sourceId
        LIMIT 1
        """
    )
    suspend fun find(date: String, sourceKind: String, sourceId: Long): ExerciseStatusEntity?

    /**
     * 往期记录：只含成功与失败，最新的排在前面。
     */
    @Query(
        """
        SELECT * FROM exercise_status
        WHERE outcome IN ('SUCCESS', 'FAILURE')
        ORDER BY date DESC, id DESC LIMIT :limit
        """
    )
    fun observeHistory(limit: Int = 500): Flow<List<ExerciseStatusEntity>>

    @Query(
        """
        SELECT * FROM exercise_status
        WHERE outcome IN ('SUCCESS', 'FAILURE')
        ORDER BY date DESC, id DESC LIMIT :limit
        """
    )
    suspend fun getHistory(limit: Int = 500): List<ExerciseStatusEntity>

    /**
     * 某个动作最近的成绩（成功/失败），用来在卡片上显示「上次 60kg」。
     */
    @Query(
        """
        SELECT * FROM exercise_status
        WHERE exercise_name = :exerciseName AND outcome IN ('SUCCESS', 'FAILURE')
        ORDER BY date DESC, id DESC LIMIT :limit
        """
    )
    suspend fun recentForExercise(
        exerciseName: String,
        limit: Int = 5
    ): List<ExerciseStatusEntity>

    /** 某个动作最近一次的成绩。 */
    @Query(
        """
        SELECT * FROM exercise_status
        WHERE exercise_name = :exerciseName AND outcome IN ('SUCCESS', 'FAILURE')
        ORDER BY date DESC, id DESC LIMIT 1
        """
    )
    suspend fun latestForExercise(exerciseName: String): ExerciseStatusEntity?

    /**
     * 一批动作各自的成绩行（新的在前）。调用方按动作名分组取第一条，就得到每个
     * 动作的「上次」成绩，避免为每个卡片单独查一次。
     */
    @Query(
        """
        SELECT * FROM exercise_status
        WHERE exercise_name IN (:exerciseNames) AND outcome IN ('SUCCESS', 'FAILURE')
        ORDER BY date DESC, id DESC
        """
    )
    suspend fun recentForNames(exerciseNames: List<String>): List<ExerciseStatusEntity>

    /** 某个动作连续失败的次数（最新的排前面，遇到成功即停）。 */
    @Query(
        """
        SELECT * FROM exercise_status
        WHERE exercise_name = :exerciseName AND outcome IN ('SUCCESS', 'FAILURE')
        ORDER BY date DESC, id DESC LIMIT :limit
        """
    )
    suspend fun recentOutcomesForFailureCount(
        exerciseName: String,
        limit: Int = 6
    ): List<ExerciseStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExerciseStatusEntity): Long

    @Query(
        """
        DELETE FROM exercise_status
        WHERE date = :date AND source_kind = :sourceKind AND source_id = :sourceId
        """
    )
    suspend fun deleteFor(date: String, sourceKind: String, sourceId: Long)

    @Query("DELETE FROM exercise_status WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM exercise_status")
    suspend fun deleteAll()
}
