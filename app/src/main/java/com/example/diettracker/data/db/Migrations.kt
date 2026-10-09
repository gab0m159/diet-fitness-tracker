package com.example.diettracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v5 -> v6：训练模块从「轮转序列」改成「训练日 + 频率」模型。
 *
 * ## 保留什么（这是本迁移存在的全部理由）
 *
 * 饮食侧的表结构一个字都没改，所以它们**原样保留，一条数据都不丢**：
 *
 * | 表 | 内容 |
 * |---|---|
 * | `foods` | 食物库，含用户自建食物、品牌标签、推荐标签、数据来源 |
 * | `food_variants` | 品牌食品的按份规格 |
 * | `diet_entries` | 每天的饮食记录（营养值是快照，不依赖食物库） |
 * | `macro_goals` | 单行宏量目标 |
 * | `user_profile` | 身高 / 体重 / 年龄 / 性别 / 体脂率 / 活动系数 / 目标模式 |
 * | `custom_exercises` / `custom_stretches` | 用户自建动作与拉伸 |
 *
 * 也就是说**不再使用破坏式迁移**：不会先删库再重播种子数据，用户的食物库和
 * 饮食记录完整保留。
 *
 * ## 重建什么
 *
 * 训练侧的表结构变化太大（去掉了轮转槽位、按动作名全局共享的设置表，新增了
 * 训练日动作行和今日日程行），旧行没有可映射的语义，因此直接重建：
 *
 *  - `training_splits` / `training_day_exercises`：训练日与动作目标（会丢，需要重新创建或套用预设）
 *  - `day_schedule`：今日手动日程（新表）
 *  - `workout_sessions` / `exercise_records` / `exercise_status`：训练历史与每组次数明细（会丢）
 *  - `user_training_goal`：去掉 `schedule_mode` / `rest_after` 两列
 *  - 删除 `training_plan`（轮转状态）、`exercise_settings`（按动作名全局的步进与上次重量）
 *
 * ## 为什么 DDL 要手写
 *
 * Room 打开数据库后会校验实际表结构和实体定义是否一致，所以下面的建表语句必须
 * 和 Room 生成的完全等价。它们与 `app/schemas/com.example.diettracker.data.db.AppDatabase/6.json`
 * 里的 `createSql` 一一对应（列顺序、类型、NOT NULL、索引名、外键都按 Room 的约定写）。
 */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {

    override fun migrate(db: SupportSQLiteDatabase) {
        // ---------------------------------------------------------- 1. 拆表
        // 先删子表再删父表（外键 CASCADE 会跟随删除，顺序上更安全）。
        db.execSQL("DROP TABLE IF EXISTS `exercise_status`")
        db.execSQL("DROP TABLE IF EXISTS `exercise_records`")
        db.execSQL("DROP TABLE IF EXISTS `workout_sessions`")
        db.execSQL("DROP TABLE IF EXISTS `day_schedule`")
        db.execSQL("DROP TABLE IF EXISTS `training_day_exercises`")
        db.execSQL("DROP TABLE IF EXISTS `training_splits`")
        db.execSQL("DROP TABLE IF EXISTS `user_training_goal`")
        db.execSQL("DROP TABLE IF EXISTS `training_plan`")
        db.execSQL("DROP TABLE IF EXISTS `exercise_settings`")

        // -------------------------------------------------- 2. 按 v6 结构重建

        // 训练日：多出频率与到期日。
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `training_splits` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`body_part` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "`interval_days` INTEGER NOT NULL, " +
                "`next_due_date` TEXT, " +
                "`last_done_date` TEXT, " +
                "`updated_at` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_training_splits_position` " +
                "ON `training_splits` (`position`)"
        )

        // 训练日里的动作行：每个动作独立的目标组数 / 次数 / 重量 / 步进。
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `training_day_exercises` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`split_id` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "`exercise_name` TEXT NOT NULL, " +
                "`target_sets` INTEGER NOT NULL, " +
                "`target_reps` TEXT NOT NULL, " +
                "`target_weight_kg` REAL NOT NULL, " +
                "`increment_kg` REAL, " +
                "`updated_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`split_id`) REFERENCES `training_splits`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_training_day_exercises_split_id` " +
                "ON `training_day_exercises` (`split_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_training_day_exercises_split_id_position` " +
                "ON `training_day_exercises` (`split_id`, `position`)"
        )

        // 今日日程里手动添加的项（整个训练日 / 单个动作）。
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `day_schedule` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "`kind` TEXT NOT NULL, " +
                "`split_id` INTEGER, " +
                "`split_name` TEXT NOT NULL, " +
                "`exercise_name` TEXT NOT NULL, " +
                "`target_sets` INTEGER NOT NULL, " +
                "`target_reps` TEXT NOT NULL, " +
                "`target_weight_kg` REAL NOT NULL, " +
                "`increment_kg` REAL, " +
                "`created_at` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_day_schedule_date` " +
                "ON `day_schedule` (`date`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_day_schedule_date_position` " +
                "ON `day_schedule` (`date`, `position`)"
        )

        // 运动记录：改成按天一条，去掉轮转槽位，加一个标题快照。
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_sessions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "`duration_minutes` INTEGER NOT NULL, " +
                "`intensity` TEXT NOT NULL, " +
                "`burned_kcal` REAL NOT NULL, " +
                "`burn_overridden` INTEGER NOT NULL, " +
                "`note` TEXT NOT NULL, " +
                "`created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_date` " +
                "ON `workout_sessions` (`date`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_date_status` " +
                "ON `workout_sessions` (`date`, `status`)"
        )

        // 每组次数明细（结构没变，但父表被重建，所以一并重建避免留下孤儿行）。
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `exercise_records` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`session_id` INTEGER NOT NULL, " +
                "`date` TEXT NOT NULL, " +
                "`exercise_name` TEXT NOT NULL, " +
                "`weight_kg` REAL NOT NULL, " +
                "`sets` INTEGER NOT NULL, " +
                "`reps_per_set` TEXT NOT NULL, " +
                "`reps_min` INTEGER NOT NULL, " +
                "`reps_max` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_records_session_id` " +
                "ON `exercise_records` (`session_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_records_exercise_name` " +
                "ON `exercise_records` (`exercise_name`)"
        )

        // 卡片结果：身份从「动作名」改成「(日期, 来源类型, 来源行 id)」，
        // 并带上目标值快照供往期记录显示。
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `exercise_status` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`session_id` INTEGER, " +
                "`date` TEXT NOT NULL, " +
                "`exercise_name` TEXT NOT NULL, " +
                "`outcome` TEXT NOT NULL, " +
                "`source_kind` TEXT NOT NULL, " +
                "`source_id` INTEGER NOT NULL, " +
                "`weight_before_kg` REAL NOT NULL, " +
                "`weight_after_kg` REAL NOT NULL, " +
                "`target_sets` INTEGER NOT NULL, " +
                "`target_reps` TEXT NOT NULL, " +
                "`target_weight_kg` REAL NOT NULL, " +
                "`source_label` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_status_date` " +
                "ON `exercise_status` (`date`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_status_exercise_name` " +
                "ON `exercise_status` (`exercise_name`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_exercise_status_date_source_kind_source_id` " +
                "ON `exercise_status` (`date`, `source_kind`, `source_id`)"
        )

        // 训练偏好：去掉排程模式与休息间隔。
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_training_goal` (" +
                "`id` INTEGER NOT NULL, " +
                "`split_type` TEXT NOT NULL, " +
                "`objective` TEXT NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        // 单行表被重建了，补回默认行，否则首屏要等用户保存才有值。
        // 时间戳直接用 SQL 取，保持整条语句自包含（便于离线校验迁移脚本）。
        db.execSQL(
            "INSERT OR REPLACE INTO `user_training_goal` " +
                "(`id`, `split_type`, `objective`, `updated_at`) " +
                "VALUES (1, 'THREE', 'HYPERTROPHY', CAST(strftime('%s','now') AS INTEGER) * 1000)"
        )
    }
}

/** 所有已注册的迁移。 */
val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_5_6)
