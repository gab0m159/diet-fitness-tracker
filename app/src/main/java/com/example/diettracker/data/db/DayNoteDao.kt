package com.example.diettracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 每日备注名（「减脂日」「练胸日」）。 */
@Dao
interface DayNoteDao {

    @Query("SELECT * FROM day_notes WHERE date = :date LIMIT 1")
    fun observe(date: String): Flow<DayNoteEntity?>

    @Query("SELECT * FROM day_notes WHERE date = :date LIMIT 1")
    suspend fun get(date: String): DayNoteEntity?

    /** 整月一次性读出来，月历渲染用。 */
    @Query("SELECT * FROM day_notes WHERE date BETWEEN :from AND :to")
    fun observeBetween(from: String, to: String): Flow<List<DayNoteEntity>>

    @Query("SELECT * FROM day_notes WHERE date BETWEEN :from AND :to")
    suspend fun getBetween(from: String, to: String): List<DayNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DayNoteEntity)

    /** 改名时按日期写入；没有行就插入。 */
    @Query(
        """
        INSERT INTO day_notes (date, label, updated_at)
        VALUES (:date, :label, :updatedAt)
        ON CONFLICT(date) DO UPDATE SET label = :label, updated_at = :updatedAt
        """
    )
    suspend fun setLabel(date: String, label: String, updatedAt: Long)

    @Query("DELETE FROM day_notes WHERE date = :date")
    suspend fun delete(date: String)
}

/** 用户自建的运动项目。 */
@Dao
interface CustomSportDao {

    @Query("SELECT * FROM custom_sports ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<CustomSportEntity>>

    @Query("SELECT * FROM custom_sports ORDER BY created_at ASC, id ASC")
    suspend fun getAll(): List<CustomSportEntity>

    @Query("SELECT * FROM custom_sports WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomSportEntity?

    @Query("SELECT COUNT(*) FROM custom_sports WHERE name = :name COLLATE NOCASE AND id != :excludeId")
    suspend fun countWithName(name: String, excludeId: Long = 0L): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomSportEntity): Long

    @Query("DELETE FROM custom_sports WHERE id = :id")
    suspend fun deleteById(id: Long)
}

/** 个人纪录（PR）。 */
@Dao
interface PersonalRecordDao {

    /** 全部记录，新的在前。 */
    @Query("SELECT * FROM personal_records ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<PersonalRecordEntity>>

    @Query("SELECT * FROM personal_records ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<PersonalRecordEntity>

    /** 某个动作的全部记录（含历史），用于「点进去看历史」。 */
    @Query(
        """
        SELECT * FROM personal_records
        WHERE exercise_name = :exerciseName
        ORDER BY weight_kg DESC, reps DESC, date DESC
        """
    )
    fun observeForExercise(exerciseName: String): Flow<List<PersonalRecordEntity>>

    /**
     * 某个动作的历史最好成绩（按重量，同重量比次数）。
     * 用于「练到更重时提示要不要更新 PR」。
     */
    @Query(
        """
        SELECT * FROM personal_records
        WHERE exercise_name = :exerciseName
        ORDER BY weight_kg DESC, reps DESC LIMIT 1
        """
    )
    suspend fun bestFor(exerciseName: String): PersonalRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PersonalRecordEntity): Long

    @Delete
    suspend fun delete(entity: PersonalRecordEntity)

    @Query("DELETE FROM personal_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
