package com.example.diettracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移。
 *
 * ## 原则
 *
 * 从 v8 起**不再使用破坏式迁移**：每个版本都配一个真正的 `Migration`，保证用户
 * 的饮食记录、运动记录、自建内容在升级后都还在。
 *
 * ## 历史
 *
 * - **v6 → v7**：训练模型整体重写（训练日 + 频率 + 步进 → 运动项目 + 时长 + MET）。
 *   旧表语义上无法映射到新模型，是**唯一一次**允许丢数据的升级，因此没有迁移代码，
 *   直接走破坏式重建 + 重新播种。
 * - **v7 → v8**：新增 `day_notes`、`personal_records`、`custom_sports` 三张表。
 *   当时也走了破坏式（粒度是整个库），但结构上是无损的，这里补上真迁移，
 *   让仍停留在 v7 的用户升级时不会丢数据。
 * - **v8 → v9**：新增 `seed_versions` 表，用于内置数据的**增量更新**。
 *
 * ## 写迁移的注意事项
 *
 * 1. 建表语句必须和 Room 生成的一致（对照 `app/schemas/.../<version>.json`），
 *    否则启动时会抛 `IllegalStateException: Migration didn't properly handle ...`；
 * 2. `ALTER TABLE ... ADD COLUMN` 要带上 `NOT NULL DEFAULT`（如果实体字段非空）；
 * 3. 迁移只改结构，**不要**在这里插业务数据——内置内容的补齐交给 `FoodSeed`。
 */
object Migrations {

    /**
     * v7 → v8：新增每日备注名、个人纪录、自建运动三张表。
     *
     * 纯新增，不动任何已有表，所以是无损的。
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `day_notes` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `date` TEXT NOT NULL,
                    `label` TEXT NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_day_notes_date` ON `day_notes` (`date`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `personal_records` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `exercise_name` TEXT NOT NULL,
                    `weight_kg` REAL NOT NULL,
                    `reps` INTEGER NOT NULL,
                    `date` TEXT NOT NULL,
                    `note` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_personal_records_exercise_name`
                ON `personal_records` (`exercise_name`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_personal_records_date`
                ON `personal_records` (`date`)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `custom_sports` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `met` REAL NOT NULL,
                    `default_minutes` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    /**
     * v8 → v9：新增内置数据的版本戳表。
     *
     * 同样是无损的。建表后把各批次标记为**当前版本**——因为这些数据在 v8 里
     * 已经装过了，标记成"已安装到当前版"可以避免升级后重复插入一遍。
     */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `seed_versions` (
                    `seed_key` TEXT NOT NULL,
                    `version` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    PRIMARY KEY(`seed_key`)
                )
                """.trimIndent()
            )

            // 已有的内置数据算作"已安装到当前版本"，防止升级后整批重插。
            val now = System.currentTimeMillis()
            SeedVersionEntity.ALL.forEach { (key, version) ->
                db.execSQL(
                    "INSERT OR REPLACE INTO `seed_versions` " +
                        "(`seed_key`, `version`, `updated_at`) VALUES (?, ?, ?)",
                    arrayOf<Any>(key, version, now)
                )
            }
        }
    }

    /** 注册到 [AppDatabase] 的全部迁移，按版本顺序。 */
    val ALL: Array<Migration> = arrayOf(MIGRATION_7_8, MIGRATION_8_9)
}
