package com.example.diettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.diettracker.data.db.AppDatabase
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.data.repository.TrainingRepository
import com.example.diettracker.ui.DietTrackerRoot
import com.example.diettracker.ui.theme.DietTrackerTheme

/**
 * The single Activity of the app. All screens are Compose destinations hosted by
 * `DietTrackerRoot`, which owns the NavHost.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // `android:name=".DietTrackerApp"` in the manifest makes this cast succeed.
        // Fall back to building the graph here (rather than crashing) if that
        // attribute is ever missing, e.g. when merging manifests in a variant.
        val app = application
        val database = AppDatabase.getInstance(this)
        val repository = if (app is DietTrackerApp) app.repository else DietRepository(database)
        val trainingRepository =
            if (app is DietTrackerApp) app.trainingRepository else TrainingRepository(database)

        setContent {
            DietTrackerTheme {
                DietTrackerRoot(
                    repository = repository,
                    trainingRepository = trainingRepository
                )
            }
        }
    }
}
