package com.example.diettracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 当天运动项目（跑步、撸铁…）。 */
@Dao
interface ActivityLogDao {

    @Query("SELECT * FROM activity_logs WHERE date = :date ORDER BY position ASC, id ASC")
    fun observeByDate(date: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE date = :date ORDER BY position ASC, id ASC")
    suspend fun getByDate(date: String): List<ActivityLogEntity>

    @Query("SELECT * FROM activity_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ActivityLogEntity?

    @Query("SELECT COUNT(*) FROM activity_logs WHERE date = :date")
    suspend fun countByDate(date: String): Int

    /**
     * 当天消耗合计。**只有填了时长的才算**（避免刚加进来还没填就计 0 干扰）。
     */
    @Query("SELECT COALESCE(SUM(burned_kcal), 0.0) FROM activity_logs WHERE date = :date")
    fun observeBurnedKcal(date: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(burned_kcal), 0.0) FROM activity_logs WHERE date = :date")
    suspend fun getBurnedKcal(date: String): Double

    /** 最近 [days] 天的消耗合计，用于首页小结。 */
    @Query(
        """
        SELECT COALESCE(SUM(burned_kcal), 0.0) FROM activity_logs
        WHERE date >= :fromDate
        """
    )
    fun observeBurnedKcalSince(fromDate: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ActivityLogEntity): Long

    @Update
    suspend fun update(entity: ActivityLogEntity)

    @Query("DELETE FROM activity_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM activity_logs WHERE date = :date")
    suspend fun deleteByDate(date: String)
}

/** 撸铁里的动作卡片：动作名 + 重量 + 组数 + 次数。 */
@Dao
interface ExerciseLogDao {

    @Query(
        """
        SELECT * FROM exercise_logs
        WHERE activity_id = :activityId ORDER BY position ASC, id ASC
        """
    )
    fun observeByActivity(activityId: Long): Flow<List<ExerciseLogEntity>>

    @Query(
        """
        SELECT * FROM exercise_logs
        WHERE activity_id = :activityId ORDER BY position ASC, id ASC
        """
    )
    suspend fun getByActivity(activityId: Long): List<ExerciseLogEntity>

    /** 「某一天练了哪些动作」一次查出来。 */
    @Query("SELECT * FROM exercise_logs WHERE date = :date ORDER BY position ASC, id ASC")
    suspend fun getByDate(date: String): List<ExerciseLogEntity>

    /** 同上的订阅版：动作卡片增删改时能实时刷新当天列表。 */
    @Query("SELECT * FROM exercise_logs WHERE date = :date ORDER BY position ASC, id ASC")
    fun observeByDate(date: String): Flow<List<ExerciseLogEntity>>

    @Query(
        """
        SELECT * FROM exercise_logs
        WHERE date = :date AND exercise_name = :exerciseName
        ORDER BY id DESC LIMIT 1
        """
    )
    suspend fun lastForExerciseOnDate(date: String, exerciseName: String): ExerciseLogEntity?

    /** 某个动作最近一次的记录，用于预填「上次的重量 / 组数 / 次数」。 */
    @Query(
        """
        SELECT * FROM exercise_logs
        WHERE exercise_name = :exerciseName AND date < :beforeDate
        ORDER BY date DESC, id DESC LIMIT 1
        """
    )
    suspend fun previousForExercise(
        exerciseName: String,
        beforeDate: String
    ): ExerciseLogEntity?

    @Query("SELECT COUNT(*) FROM exercise_logs WHERE activity_id = :activityId")
    suspend fun countForActivity(activityId: Long): Int

    @Query("SELECT * FROM exercise_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ExerciseLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExerciseLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExerciseLogEntity>)

    @Update
    suspend fun update(entity: ExerciseLogEntity)

    @Query("DELETE FROM exercise_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM exercise_logs WHERE activity_id = :activityId")
    suspend fun deleteForActivity(activityId: Long)

    /** 替换某个活动下的全部动作，顺序按列表下标。 */
    @Transaction
    suspend fun replaceForActivity(activityId: Long, entities: List<ExerciseLogEntity>) {
        deleteForActivity(activityId)
        insertAll(entities.mapIndexed { index, entity -> entity.copy(position = index) })
    }
}
