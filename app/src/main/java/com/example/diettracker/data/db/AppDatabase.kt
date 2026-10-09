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
        // --- 用户自建内容 ---------------------------------------------------
        CustomExerciseEntity::class,
        CustomStretchEntity::class
    ],
    version = 7,
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
                // v6 -> v7 把「训练日 + 频率 + 步进」整套模型换成了「运动项目 + 时长 +
                // MET」。旧训练表语义上无法映射到新模型（没有训练日、没有成功失败、
                // 没有步进），所以 v7 走破坏式迁移并在重建后重新播种食物库。
                // 这一步已与用户确认：升级会清空本地数据。
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seed(INSTANCE)
                    }

                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        super.onDestructiveMigration(db)
                        // 表被重建过，重新播种内置内容，避免库是空的。
                        seed(INSTANCE)
                    }
                })
                .build()

        /** 写入内置食物库（用户自建食物不动）+ 身体数据默认行。 */
        private fun seed(database: AppDatabase?) {
            val db = database ?: return
            CoroutineScope(Dispatchers.IO).launch {
                com.example.diettracker.data.seed.FoodSeed.install(
                    db.foodDao(),
                    db.foodVariantDao()
                )
                db.userProfileDao().get() ?: db.userProfileDao()
                    .upsert(UserProfileEntity.default())
            }
        }
    }
}
