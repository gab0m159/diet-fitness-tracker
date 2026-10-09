package com.example.diettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.diettracker.data.db.AppDatabase
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.ui.DietTrackerRoot
import com.example.diettracker.ui.theme.DietTrackerTheme

/**
 * 唯一的 Activity。所有界面都是 `DietTrackerRoot` 里的 Compose 目的地。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 清单里的 android:name=".DietTrackerApp" 保证这个转换成立；万一缺失
        // （例如某个变体合并清单时），就地兜底构建，而不是崩溃。
        val app = application
        val database = AppDatabase.getInstance(this)
        val dietRepository =
            if (app is DietTrackerApp) app.dietRepository else DietRepository(database)
        val activityRepository =
            if (app is DietTrackerApp) {
                app.activityRepository
            } else {
                ActivityRepository(database)
            }

        setContent {
            DietTrackerTheme {
                DietTrackerRoot(
                    dietRepository = dietRepository,
                    activityRepository = activityRepository
                )
            }
        }
    }
}
