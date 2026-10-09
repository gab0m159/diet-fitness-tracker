package com.example.diettracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 训练日（`TrainingSplitEntity`）。
 *
 * `position` 上没有唯一索引：训练日的增删由仓库统一重排，唯一约束只会让
 * `@Insert(REPLACE)` 在插入重排结果时误删相邻行。
 */
@Dao
interface TrainingSplitDao {

    @Query("SELECT * FROM training_splits ORDER BY position ASC, id ASC")
    fun observeAll(): Flow<List<TrainingSplitEntity>>

    @Query("SELECT * FROM training_splits ORDER BY position ASC, id ASC")
    suspend fun getAll(): List<TrainingSplitEntity>

    @Query("SELECT * FROM training_splits WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TrainingSplitEntity?

    @Query("SELECT COUNT(*) FROM training_splits")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(split: TrainingSplitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(splits: List<TrainingSplitEntity>)

    @Update
    suspend fun update(split: TrainingSplitEntity)

    @Query("DELETE FROM training_splits WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM training_splits")
    suspend fun deleteAll()
}

/** 训练日里的动作行（目标组数 / 次数 / 重量 / 步进）。 */
@Dao
interface TrainingDayExerciseDao {

    @Query(
        """
        SELECT * FROM training_day_exercises
        WHERE split_id = :splitId ORDER BY position ASC, id ASC
        """
    )
    fun observeBySplit(splitId: Long): Flow<List<TrainingDayExerciseEntity>>

    @Query("SELECT * FROM training_day_exercises ORDER BY split_id ASC, position ASC, id ASC")
    fun observeAll(): Flow<List<TrainingDayExerciseEntity>>

    @Query(
        """
        SELECT * FROM training_day_exercises
        WHERE split_id = :splitId ORDER BY position ASC, id ASC
        """
    )
    suspend fun getBySplit(splitId: Long): List<TrainingDayExerciseEntity>

    @Query("SELECT * FROM training_day_exercises ORDER BY split_id ASC, position ASC, id ASC")
    suspend fun getAll(): List<TrainingDayExerciseEntity>

    @Query("SELECT * FROM training_day_exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TrainingDayExerciseEntity?

    @Query("SELECT COUNT(*) FROM training_day_exercises WHERE split_id = :splitId")
    suspend fun countForSplit(splitId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TrainingDayExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TrainingDayExerciseEntity>)

    @Update
    suspend fun update(entity: TrainingDayExerciseEntity)

    @Query("DELETE FROM training_day_exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM training_day_exercises WHERE split_id = :splitId")
    suspend fun deleteForSplit(splitId: Long)

    @Query("DELETE FROM training_day_exercises")
    suspend fun deleteAll()

    /** 整份替换某个训练日的动作清单，顺序由列表下标决定。 */
    @Transaction
    suspend fun replaceForSplit(splitId: Long, entities: List<TrainingDayExerciseEntity>) {
        deleteForSplit(splitId)
        insertAll(entities.mapIndexed { index, entity -> entity.copy(position = index) })
    }
}

/** 今日日程里手动添加的项。 */
@Dao
interface DayScheduleDao {

    @Query("SELECT * FROM day_schedule WHERE date = :date ORDER BY position ASC, id ASC")
    fun observeByDate(date: String): Flow<List<DayScheduleEntity>>

    @Query("SELECT * FROM day_schedule WHERE date = :date ORDER BY position ASC, id ASC")
    suspend fun getByDate(date: String): List<DayScheduleEntity>

    @Query("SELECT COUNT(*) FROM day_schedule WHERE date = :date")
    suspend fun countByDate(date: String): Int

    @Query("SELECT * FROM day_schedule WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DayScheduleEntity?

    @Query("SELECT * FROM day_schedule WHERE date = :date AND split_id = :splitId LIMIT 1")
    suspend fun findDayForSplit(date: String, splitId: Long): DayScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DayScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DayScheduleEntity>)

    @Update
    suspend fun update(entity: DayScheduleEntity)

    @Delete
    suspend fun delete(entity: DayScheduleEntity)

    @Query("DELETE FROM day_schedule WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM day_schedule WHERE date = :date AND split_id = :splitId")
    suspend fun deleteForSplit(date: String, splitId: Long)

    /** 「延期」：把某一天手动添加的整份日程挪到另一天。 */
    @Query("UPDATE day_schedule SET date = :toDate WHERE date = :fromDate")
    suspend fun moveDate(fromDate: String, toDate: String)

    @Query("DELETE FROM day_schedule WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
