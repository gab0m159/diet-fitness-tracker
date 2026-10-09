package com.example.diettracker

import android.app.Application
import com.example.diettracker.data.db.AppDatabase
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.data.repository.DietRepository

/**
 * 应用入口。数据库与仓库都在这里手工构建（不用依赖注入框架），
 * 让整个对象图保持显式且小。
 *
 * v7 起原来的 `TrainingRepository` 换成了 [ActivityRepository]
 * （运动项目 + 时长 + MET 热量）；训练日 / 频率 / 步进那一整套已经移除。
 */
class DietTrackerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val dietRepository: DietRepository by lazy { DietRepository(database) }

    val activityRepository: ActivityRepository by lazy { ActivityRepository(database) }
}
