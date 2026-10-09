package com.example.diettracker

import android.app.Application
import com.example.diettracker.data.db.AppDatabase
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.data.repository.TrainingRepository

/**
 * Application entry point.
 *
 * The database and repositories are created here manually (no dependency-
 * injection framework) so that the whole object graph stays explicit and small.
 */
class DietTrackerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repository: DietRepository by lazy { DietRepository(database) }

    val trainingRepository: TrainingRepository by lazy { TrainingRepository(database) }
}
