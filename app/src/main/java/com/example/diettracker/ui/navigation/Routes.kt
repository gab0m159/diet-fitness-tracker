package com.example.diettracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 应用里的全部目的地。
 *
 * v7 起大幅精简：训练日管理、往期训练记录、详细记录页、添加训练页都已随训练计划
 * 功能一起删除。现在只剩三个标签页 + 目标设置 + 食物相关页面。
 */
object Routes {
    /** 三个底部标签页。 */
    const val LIBRARY = "library"
    const val TODAY = "today"
    const val MINE = "mine"

    /** 每日目标编辑。 */
    const val GOALS = "goals"

    /** 新建 / 编辑食物。 */
    const val FOOD_EDITOR = "food_editor?foodId={foodId}"

    fun foodEditor(foodId: Long = 0L): String = "food_editor?foodId=$foodId"

    /** 往某一天加食物。 */
    const val ADD_ENTRY = "add_entry?date={date}"

    fun addEntry(date: String): String = "add_entry?date=$date"

    /** 品牌 / 推荐食物的浏览页。`category` 是 FoodCategory 的名字。 */
    const val FOOD_BROWSE = "food_browse?category={category}"

    fun foodBrowse(category: String): String = "food_browse?category=$category"

    /** 库页，可指定直接落在拉伸上（用 `stretch` 高亮某一条）。 */
    const val LIBRARY_WITH_STRETCH = "library?stretch={stretch}"

    fun libraryStretch(stretchName: String): String =
        "library?stretch=${android.net.Uri.encode(stretchName)}"

    /**
     * 底部栏：库 / 今日 / 我的。
     *
     * 「我的」里只剩每日目标与身体数据。
     */
    val bottomBarItems = listOf(
        BottomBarItem(LIBRARY, "库", Icons.Filled.Storage),
        BottomBarItem(TODAY, "今日", Icons.Filled.CalendarToday),
        BottomBarItem(MINE, "我的", Icons.Filled.Person)
    )

    /** 启动落在哪一页。 */
    const val START = TODAY
}

data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
