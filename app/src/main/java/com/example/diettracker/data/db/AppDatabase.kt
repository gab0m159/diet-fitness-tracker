package com.example.diettracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        // --- 饮食（结构未变）------------------------------------------------
        FoodEntity::class,
        FoodVariantEntity::class,
        DietEntryEntity::class,
        MacroGoalEntity::class,
        // --- 身体数据 -------------------------------------------------------
        UserProfileEntity::class,
        // --- 运动（v7 新模型）----------------------------------------------
        ActivityLogEntity::class,
        ExerciseLogEntity::class,
        // --- 每日备注名与个人纪录（v8 新增）--------------------------------
        DayNoteEntity::class,
        PersonalRecordEntity::class,
        // --- 用户自建运动（v8 新增）----------------------------------------
        CustomSportEntity::class,
        // --- 内置数据版本戳（v9 新增）--------------------------------------
        SeedVersionEntity::class,
        // --- 用户自建内容 ---------------------------------------------------
        CustomExerciseEntity::class,
        CustomStretchEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun foodVariantDao(): FoodVariantDao
    abstract fun dietEntryDao(): DietEntryDao
    abstract fun macroGoalDao(): MacroGoalDao

    abstract fun userProfileDao(): UserProfileDao

    abstract fun activityLogDao(): ActivityLogDao
    abstract fun exerciseLogDao(): ExerciseLogDao

    abstract fun dayNoteDao(): DayNoteDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun customSportDao(): CustomSportDao
    abstract fun seedVersionDao(): SeedVersionDao

    abstract fun customExerciseDao(): CustomExerciseDao
    abstract fun customStretchDao(): CustomStretchDao

    companion object {
        private const val DB_NAME = "diet_tracker.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                // 从 v8 起不再破坏式迁移：每个版本都有真迁移，用户数据不会因为
                // 升级而丢失（见 Migrations）。
                //
                // v8 之后**不要**再加 fallbackToDestructiveMigration()：它只在版本
                // 号变化时兜底，一旦出现「版本号相同但结构不同」的库（开发期装中间
                // 版本很容易造成），Room 会直接打开旧库，然后在取列时崩溃。
                .addMigrations(*Migrations.ALL)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seed(INSTANCE, freshInstall = true)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // 每次打开都跑一次增量补齐：代码里给某批内置数据升了版本，
                        // 这里会把新增的条目补进去，用户改过/删过的不受影响。
                        seed(INSTANCE, freshInstall = false)
                    }
                })
                .build()

        /**
         * 安装 / 补齐全套内置数据。
         *
         * [freshInstall] 为真表示刚建库（全装）；为假表示常规启动或迁移后
         * （只补版本号落后的批次）。
         */
        private fun seed(database: AppDatabase?, freshInstall: Boolean) {
            val db = database ?: return
            CoroutineScope(Dispatchers.IO).launch {
                com.example.diettracker.data.seed.FoodSeed.install(
                    foodDao = db.foodDao(),
                    variantDao = db.foodVariantDao(),
                    seedVersionDao = db.seedVersionDao(),
                    freshInstall = freshInstall
                )
                db.userProfileDao().get() ?: db.userProfileDao()
                    .upsert(UserProfileEntity.default())
            }
        }
    }
}
