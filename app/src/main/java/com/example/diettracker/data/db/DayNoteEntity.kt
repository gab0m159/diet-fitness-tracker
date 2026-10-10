package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 某一天的备注名，例如「减脂日」「练胸日」。
 *
 * 单独一张表而不是塞进饮食/运动表：它跟饮食和运动都无关，只跟"日期"有关，
 * 而且大多数日子是没有名字的，不该给每一天都建一行占位数据。
 */
@Entity(
    tableName = "day_notes",
    indices = [Index(value = ["date"], unique = true)]
)
data class DayNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** ISO `yyyy-MM-dd`。 */
    @ColumnInfo(name = "date")
    val date: String,

    /** 用户起的名字，例如「减脂日」。空字符串表示没起名字。 */
    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
