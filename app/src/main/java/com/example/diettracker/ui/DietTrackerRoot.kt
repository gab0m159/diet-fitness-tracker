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
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.data.repository.TrainingRepository
import com.example.diettracker.ui.navigation.Routes
import com.example.diettracker.ui.screens.AddEntryScreen
import com.example.diettracker.ui.screens.AddToTodayScreen
import com.example.diettracker.ui.screens.DayEditorScreen
import com.example.diettracker.ui.screens.FoodBrowseScreen
import com.example.diettracker.ui.screens.FoodEditorScreen
import com.example.diettracker.ui.screens.GoalScreen
import com.example.diettracker.ui.screens.LibraryScreen
import com.example.diettracker.ui.screens.ProfileScreen
import com.example.diettracker.ui.screens.TodayScreen
import com.example.diettracker.ui.screens.TrainingDayListScreen
import com.example.diettracker.ui.screens.TrainingHistoryScreen
import com.example.diettracker.ui.screens.WorkoutLogScreen
import com.example.diettracker.ui.viewmodel.DiaryViewModel
import com.example.diettracker.ui.viewmodel.ProfileViewModel
import com.example.diettracker.ui.viewmodel.TrainingDayViewModel
import com.example.diettracker.ui.viewmodel.TrainingHistoryViewModel
import com.example.diettracker.ui.viewmodel.WorkoutViewModel

/**
 * Root composable: owns the NavController, the bottom bar and all destinations.
 *
 * Three tabs (库 / 今日 / 我的) plus secondary screens pushed on top: the macro
 * goal editor, training-day management, the day editor, add-to-today, the workout
 * log, the training history, food editing and body metrics.
 *
 * 今日页和「时长/热量」「添加到今天」共用**同一个** WorkoutViewModel 实例（都挂在
 * 今日这个返回栈条目上），所以标记成功 / 失败之后返回首页，界面已经是新的。
 */
@Composable
fun DietTrackerRoot(
    repository: DietRepository,
    trainingRepository: TrainingRepository
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
                    factory = DiaryViewModel.factory(repository, trainingRepository)
                )
                TodayScreen(
                    viewModel = vm,
                    trainingRepository = trainingRepository,
                    onAddFood = { date -> navController.navigate(Routes.addEntry(date)) },
                    onAddTraining = { date ->
                        navController.navigate(Routes.addToToday(date))
                    },
                    onOpenLog = { navController.navigate(Routes.WORKOUT_LOG) },
                    onOpenGoals = { navController.navigate(Routes.GOALS) },
                    onManageDays = { navController.navigate(Routes.TRAINING_DAYS) }
                )
            }

            // --------------------------------------------------------- 库
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    dietRepository = repository,
                    trainingRepository = trainingRepository,
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
                    dietRepository = repository,
                    trainingRepository = trainingRepository,
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
                        factory = ProfileViewModel.factory(repository, trainingRepository)
                    ),
                    onOpenGoals = { navController.navigate(Routes.GOALS) },
                    onOpenPlan = { navController.navigate(Routes.TRAINING_DAYS) },
                    onOpenHistory = { navController.navigate(Routes.TRAINING_HISTORY) },
                    onOpenLibrary = { navController.navigateToTab(Routes.LIBRARY) }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = viewModel(
                        factory = ProfileViewModel.factory(repository, trainingRepository)
                    ),
                    onOpenGoals = { navController.navigate(Routes.GOALS) },
                    onOpenPlan = { navController.navigate(Routes.TRAINING_DAYS) },
                    onOpenHistory = { navController.navigate(Routes.TRAINING_HISTORY) },
                    onOpenLibrary = { navController.navigateToTab(Routes.LIBRARY) }
                )
            }

            // ----------------------------------------- 训练日管理与编辑器
            composable(Routes.TRAINING_DAYS) {
                TrainingDayListScreen(
                    viewModel = viewModel(
                        factory = TrainingDayViewModel.factory(trainingRepository)
                    ),
                    onBack = { navController.popBackStack() },
                    onCreateDay = {
                        navController.navigate(Routes.trainingDayEditor(0L))
                    },
                    onEditDay = { splitId ->
                        navController.navigate(Routes.trainingDayEditor(splitId))
                    }
                )
            }

            composable(
                route = Routes.TRAINING_DAY_EDITOR,
                arguments = listOf(
                    navArgument("splitId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                val splitId = entry.arguments?.getLong("splitId") ?: 0L
                DayEditorScreen(
                    viewModel = viewModel(
                        factory = TrainingDayViewModel.factory(trainingRepository)
                    ),
                    splitId = splitId,
                    onBack = { navController.popBackStack() }
                )
            }

            // ------------------------------------------------ 添加到今天
            composable(
                route = Routes.ADD_TO_TODAY,
                arguments = listOf(
                    navArgument("date") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                val date = entry.arguments?.getString("date").orEmpty()
                AddToTodayScreen(
                    date = date.ifBlank { com.example.diettracker.util.DateUtils.today() },
                    dayViewModel = viewModel(
                        factory = TrainingDayViewModel.factory(trainingRepository)
                    ),
                    workoutViewModel = todayScopedWorkoutViewModel(
                        navController,
                        trainingRepository
                    ),
                    onDone = { navController.popBackStack() },
                    onManageDays = { navController.navigate(Routes.TRAINING_DAYS) }
                )
            }

            // ------------------------------------------------ 往期记录
            composable(Routes.TRAINING_HISTORY) {
                TrainingHistoryScreen(
                    viewModel = viewModel(
                        factory = TrainingHistoryViewModel.factory(trainingRepository)
                    ),
                    onBack = { navController.popBackStack() }
                )
            }

            // -------------------------------- 详细记录（时长 / 热量 / 拉伸）
            composable(Routes.WORKOUT_LOG) {
                WorkoutLogScreen(
                    viewModel = todayScopedWorkoutViewModel(navController, trainingRepository),
                    onDone = { navController.popBackStack() },
                    onOpenStretch = { stretchName ->
                        navController.navigate(Routes.libraryStretch(stretchName))
                    }
                )
            }

            // ---------------------------------------------- 饮食相关页面
            composable(Routes.GOALS) {
                GoalScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

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
                    repository = repository,
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
                    repository = repository,
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
                    factory = DiaryViewModel.factory(repository, trainingRepository)
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

/**
 * 「今日」条目上的 WorkoutViewModel 实例。
 *
 * 今日页、添加训练、详细记录页三处必须是同一个实例，否则标记结果之后首页不会刷新。
 * 从别的入口进来（今日不在返回栈里）时退回普通作用域。
 */
@Composable
private fun todayScopedWorkoutViewModel(
    navController: NavHostController,
    trainingRepository: TrainingRepository
): WorkoutViewModel {
    val todayEntry = remember {
        runCatching { navController.getBackStackEntry(Routes.TODAY) }.getOrNull()
    }
    return if (todayEntry != null) {
        viewModel(
            viewModelStoreOwner = todayEntry,
            factory = WorkoutViewModel.factory(trainingRepository)
        )
    } else {
        viewModel(factory = WorkoutViewModel.factory(trainingRepository))
    }
}

/** Navigates to a bottom-bar tab without stacking duplicates. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
