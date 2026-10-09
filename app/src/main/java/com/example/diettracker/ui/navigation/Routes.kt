package com.example.diettracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector

/** Every destination in the app. */
object Routes {
    /** The three bottom-bar tabs. */
    const val LIBRARY = "library"
    const val TODAY = "today"
    const val MINE = "mine"

    /** Secondary screens, pushed on top of a tab. */
    const val GOALS = "goals"

    /** 训练日管理：创建 / 编辑 / 删除训练日，套用预设计划。 */
    const val TRAINING_DAYS = "training_days"

    /** 单个训练日的编辑器（名称、频率、动作与每个动作的目标）。 */
    const val TRAINING_DAY_EDITOR = "training_day_editor?splitId={splitId}"

    fun trainingDayEditor(splitId: Long = 0L): String = "training_day_editor?splitId=$splitId"

    /** 把训练日或单个动作加进某一天的日程。 */
    const val ADD_TO_TODAY = "add_to_today?date={date}"

    fun addToToday(date: String): String = "add_to_today?date=$date"

    /** 往期训练记录（只有成功 / 失败）。 */
    const val TRAINING_HISTORY = "training_history"

    /** 详细记录页：时长、强度、热量与每组次数、练后拉伸。 */
    const val WORKOUT_LOG = "workout_log"

    const val PROFILE = "profile"

    /** Create a new food. */
    const val FOOD_EDITOR_NEW = "food_editor?foodId=0"

    /** Edit an existing food. */
    const val FOOD_EDITOR = "food_editor?foodId={foodId}"

    fun foodEditor(foodId: Long = 0L): String = "food_editor?foodId=$foodId"

    /** Add-to-diary screen for a given day. */
    const val ADD_ENTRY = "add_entry?date={date}"

    fun addEntry(date: String): String = "add_entry?date=$date"

    /** Brand / recommended food browser. `category` is a FoodCategory name. */
    const val FOOD_BROWSE = "food_browse?category={category}"

    fun foodBrowse(category: String): String = "food_browse?category=$category"

    /**
     * The library tab, optionally opened straight on the stretch section with one
     * stretch highlighted (used by an exercise's "拉伸 ↗" jump).
     */
    const val LIBRARY_WITH_STRETCH = "library?stretch={stretch}"

    fun libraryStretch(stretchName: String): String =
        "library?stretch=${android.net.Uri.encode(stretchName)}"

    /**
     * Bottom bar: three items — the library, today, and personal settings.
     *
     * 「我的」hosts the training-day management, the daily macro goals and the
     * body-metrics screen.
     */
    val bottomBarItems = listOf(
        BottomBarItem(LIBRARY, "库", Icons.Filled.Storage),
        BottomBarItem(TODAY, "今日", Icons.Filled.CalendarToday),
        BottomBarItem(MINE, "我的", Icons.Filled.Person)
    )

    /** Which tab the app opens on. */
    const val START = TODAY
}

data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
