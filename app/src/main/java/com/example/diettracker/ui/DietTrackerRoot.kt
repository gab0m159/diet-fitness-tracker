package com.example.diettracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.ui.navigation.Routes
import com.example.diettracker.ui.screens.AddEntryScreen
import com.example.diettracker.ui.screens.FoodBrowseScreen
import com.example.diettracker.ui.screens.FoodEditorScreen
import com.example.diettracker.ui.screens.GoalScreen
import com.example.diettracker.ui.screens.LibraryScreen
import com.example.diettracker.ui.screens.PersonalRecordScreen
import com.example.diettracker.ui.screens.ProfileScreen
import com.example.diettracker.ui.screens.TodayScreen
import com.example.diettracker.ui.viewmodel.DiaryViewModel
import com.example.diettracker.ui.viewmodel.PersonalRecordViewModel
import com.example.diettracker.ui.viewmodel.ProfileViewModel

/**
 * 根 Composable：持有 NavController、底部栏与全部目的地。
 *
 * 三个标签页（库 / 今日 / 我的）+ 目标设置、食物编辑等二级页面。
 *
 * v7 精简后，今日页的运动块与「添加食物」共用今日这个返回栈条目上的 ViewModel，
 * 所以标记完回到首页时数据已经是新的。
 */
@Composable
fun DietTrackerRoot(
    dietRepository: DietRepository,
    activityRepository: ActivityRepository
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.START

    val showBottomBar = Routes.bottomBarItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Routes.bottomBarItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { navController.navigateToTab(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.START,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ------------------------------------------------------- 今日
            composable(Routes.TODAY) {
                val vm: DiaryViewModel = viewModel(
                    factory = DiaryViewModel.factory(dietRepository)
                )
                TodayScreen(
                    viewModel = vm,
                    activityRepository = activityRepository,
                    dietRepository = dietRepository,
                    onAddFood = { date -> navController.navigate(Routes.addEntry(date)) },
                    onOpenGoals = { navController.navigate(Routes.GOALS) }
                )
            }

            // --------------------------------------------------------- 库
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    dietRepository = dietRepository,
                    activityRepository = activityRepository,
                    onCreateFood = { navController.navigate(Routes.foodEditor(0L)) },
                    onEditFood = { id -> navController.navigate(Routes.foodEditor(id)) },
                    onBrowseBundled = { category ->
                        navController.navigate(Routes.foodBrowse(category.name))
                    },
                    initialStretch = null
                )
            }

            composable(
                route = Routes.LIBRARY_WITH_STRETCH,
                arguments = listOf(
                    navArgument("stretch") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                val stretch = entry.arguments?.getString("stretch").orEmpty()
                LibraryScreen(
                    dietRepository = dietRepository,
                    activityRepository = activityRepository,
                    onCreateFood = { navController.navigate(Routes.foodEditor(0L)) },
                    onEditFood = { id -> navController.navigate(Routes.foodEditor(id)) },
                    onBrowseBundled = { category ->
                        navController.navigate(Routes.foodBrowse(category.name))
                    },
                    initialStretch = stretch.ifBlank { null }
                )
            }

            // -------------------------------------------------------- 我的
            composable(Routes.MINE) {
                ProfileScreen(
                    viewModel = viewModel(
                        factory = ProfileViewModel.factory(dietRepository, activityRepository)
                    ),
                    onOpenGoals = { navController.navigate(Routes.GOALS) },
                    onOpenRecords = { navController.navigate(Routes.RECORDS) }
                )
            }

            // ---------------------------------------------------- 我的 PR
            composable(Routes.RECORDS) {
                PersonalRecordScreen(
                    viewModel = viewModel(
                        factory = PersonalRecordViewModel.factory(activityRepository)
                    ),
                    onBack = { navController.popBackStack() }
                )
            }

            // ---------------------------------------------------- 目标设置
            composable(Routes.GOALS) {
                GoalScreen(
                    repository = dietRepository,
                    onBack = { navController.popBackStack() }
                )
            }

            // ---------------------------------------------------- 食物相关
            composable(
                route = Routes.FOOD_BROWSE,
                arguments = listOf(
                    navArgument("category") {
                        type = NavType.StringType
                        defaultValue = FoodCategory.BRAND.name
                    }
                )
            ) { entry ->
                val raw = entry.arguments?.getString("category") ?: FoodCategory.BRAND.name
                FoodBrowseScreen(
                    repository = dietRepository,
                    initialCategory = FoodCategory.fromStorage(raw),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.FOOD_EDITOR,
                arguments = listOf(
                    navArgument("foodId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                val foodId = entry.arguments?.getLong("foodId") ?: 0L
                FoodEditorScreen(
                    repository = dietRepository,
                    foodId = foodId,
                    onDone = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ADD_ENTRY,
                arguments = listOf(
                    navArgument("date") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                val date = entry.arguments?.getString("date").orEmpty()
                // 复用「今日」那个返回栈条目上的 DiaryViewModel，加完记录首页立刻更新。
                val todayEntry = remember(entry) {
                    navController.getBackStackEntry(Routes.TODAY)
                }
                val vm: DiaryViewModel = viewModel(
                    viewModelStoreOwner = todayEntry,
                    factory = DiaryViewModel.factory(dietRepository)
                )
                AddEntryScreen(
                    viewModel = vm,
                    initialDate = date,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}

/** 切到底部标签页，不堆叠重复实例。 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
