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
        // --- diet（v5 -> v6 结构未变，迁移时原样保留）------------------------
        FoodEntity::class,
        FoodVariantEntity::class,
        DietEntryEntity::class,
        MacroGoalEntity::class,
        // --- body metrics -------------------------------------------------
        UserProfileEntity::class,
        // --- training -----------------------------------------------------
        UserTrainingGoalEntity::class,
        TrainingSplitEntity::class,
        TrainingDayExerciseEntity::class,
        DayScheduleEntity::class,
        WorkoutSessionEntity::class,
        ExerciseRecordEntity::class,
        // --- user-created library ------------------------------------------
        CustomExerciseEntity::class,
        CustomStretchEntity::class,
        ExerciseStatusEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun foodVariantDao(): FoodVariantDao
    abstract fun dietEntryDao(): DietEntryDao
    abstract fun macroGoalDao(): MacroGoalDao

    abstract fun userProfileDao(): UserProfileDao
    abstract fun userTrainingGoalDao(): UserTrainingGoalDao

    abstract fun trainingSplitDao(): TrainingSplitDao
    abstract fun trainingDayExerciseDao(): TrainingDayExerciseDao
    abstract fun dayScheduleDao(): DayScheduleDao

    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exerciseRecordDao(): ExerciseRecordDao

    abstract fun customExerciseDao(): CustomExerciseDao
    abstract fun customStretchDao(): CustomStretchDao
    abstract fun exerciseStatusDao(): ExerciseStatusDao

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
                // v5 -> v6 rewrote the training module (training days by interval,
                // per-plan exercise targets, per-day schedule entries). See
                // MIGRATIONS below: the food library, the diary and the body
                // profile are deliberately preserved, only the training tables are
                // rebuilt. The destructive fallback stays as a last resort for
                // paths we have no explicit migration for.
                .addMigrations(*MIGRATIONS)
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seed(INSTANCE)
                    }

                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        super.onDestructiveMigration(db)
                        // Tables were dropped and recreated; re-seed the bundled
                        // content so the library is not empty after an upgrade.
                        seed(INSTANCE)
                    }
                })
                .build()

        /**
         * Inserts the bundled food library (user foods start empty) plus the
         * single-row default profile/goal records.
         */
        private fun seed(database: AppDatabase?) {
            val db = database ?: return
            CoroutineScope(Dispatchers.IO).launch {
                com.example.diettracker.data.seed.FoodSeed.install(
                    db.foodDao(),
                    db.foodVariantDao()
                )
                // Default singletons so first launch has sane values.
                db.userProfileDao().get() ?: db.userProfileDao()
                    .upsert(UserProfileEntity.default())
                db.userTrainingGoalDao().get() ?: db.userTrainingGoalDao()
                    .upsert(UserTrainingGoalEntity.default())
            }
        }
    }
}
