package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户自己添加的运动项目。
 *
 * 内置的 33 项在 `SportLibrary` 里是代码常量（不可改），用户新增的走这张表。
 * 展示时两者合并：内置在前、自建在后，用 [SportLibrary.STRENGTH_KEY] 之外的自定义
 * key 区分（见 [key]）。
 *
 * ## 为什么要存 MET
 *
 * 热量按 `MET × 体重 × 时长` 估算，所以 MET 是这个项目的核心参数。界面上会给出
 * 常用档位参考，但最终由用户填——不理解 MET 的人可以按界面提示挑一个相近的。
 */
@Entity(tableName = "custom_sports")
data class CustomSportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** 显示名，例如「划船机（自家）」。 */
    @ColumnInfo(name = "name")
    val name: String,

    /** 代谢当量，用于热量估算。 */
    @ColumnInfo(name = "met")
    val met: Double,

    /** 加进来时预填的时长（分钟）。 */
    @ColumnInfo(name = "default_minutes")
    val defaultMinutes: Int = 30,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 存进 activity_logs 时用的稳定标识，与内置运动区分开。 */
    val key: String get() = "$KEY_PREFIX$id"

    companion object {
        /** 自建运动 key 的前缀；内置运动的 key 都是纯大写字，不会冲突。 */
        const val KEY_PREFIX = "CUSTOM_"

        /** MET 可选档位（界面用）。 */
        val MET_PRESETS = listOf(
            2.5 to "很轻（散步、拉伸）",
            3.5 to "轻（慢走、太极）",
            5.0 to "中等（快走、跳舞）",
            6.5 to "偏重（篮球、健美操）",
            8.0 to "重（跑步、游泳）",
            10.0 to "很重（跳绳、HIIT）"
        )

        fun isCustomKey(key: String): Boolean = key.startsWith(KEY_PREFIX)

        fun idFromKey(key: String): Long? =
            key.removePrefix(KEY_PREFIX).toLongOrNull()

        const val MIN_MET = 1.0
        const val MAX_MET = 25.0
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 600
    }
}
