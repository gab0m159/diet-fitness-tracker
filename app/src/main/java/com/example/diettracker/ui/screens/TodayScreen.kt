package com.example.diettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diettracker.data.db.ExerciseLogEntity
import com.example.diettracker.data.model.DiaryEntry
import com.example.diettracker.data.model.MealType
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.domain.SportLibrary
import com.example.diettracker.ui.components.BurnOverrideDialog
import com.example.diettracker.ui.components.ConfirmDialog
import com.example.diettracker.ui.components.DateNavigator
import com.example.diettracker.ui.components.Dot
import com.example.diettracker.ui.components.DurationDialog
import com.example.diettracker.ui.components.EditEntrySheet
import com.example.diettracker.ui.components.ExerciseEditorDialog
import com.example.diettracker.ui.components.IntakeSummaryCard
import com.example.diettracker.ui.components.MacroChip
import com.example.diettracker.ui.components.SectionHeader
import com.example.diettracker.ui.components.SportActivityCard
import com.example.diettracker.ui.components.SportPickerDialog
import com.example.diettracker.ui.components.StrengthActivityCard
import com.example.diettracker.ui.theme.AppColors
import com.example.diettracker.ui.theme.MacroColors
import com.example.diettracker.ui.theme.Spacing
import com.example.diettracker.ui.viewmodel.ActivityCard
import com.example.diettracker.ui.viewmodel.ActivityViewModel
import com.example.diettracker.ui.viewmodel.DiaryViewModel
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.launch

/**
 * 首页（今日）。
 *
 * 三段：
 *
 *  1. **当天摄入** —— 四项进度条 + 摄入 / 消耗 / 净热量
 *  2. **今日运动** —— 上段是有氧等运动项目，下段是撸铁（里面是动作卡片）
 *  3. **今日饮食** —— 当天食物，按餐次分组
 *
 * 运动消耗**只显示**，不加回可摄入额度。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: DiaryViewModel,
    activityRepository: ActivityRepository,
    onAddFood: (String) -> Unit,
    onOpenGoals: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val activityViewModel: ActivityViewModel = viewModel(
        factory = ActivityViewModel.factory(activityRepository)
    )
    val activityState by activityViewModel.ui.collectAsStateWithLifecycle()
    val bodyWeightKg = activityState.bodyWeightKg

    var pendingDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    var confirmClearDay by remember { mutableStateOf(false) }
    var confirmClearSport by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }
    var editingVariants by remember {
        mutableStateOf<List<com.example.diettracker.data.db.FoodVariantEntity>>(emptyList())
    }
    var showSportPicker by remember { mutableStateOf(false) }
    var durationTarget by remember { mutableStateOf<ActivityCard?>(null) }
    var burnTarget by remember { mutableStateOf<ActivityCard?>(null) }
    var deleteActivityTarget by remember { mutableStateOf<ActivityCard?>(null) }
    var exerciseTarget by remember { mutableStateOf<ExerciseLogEntity?>(null) }
    var exercisePickerFor by remember { mutableStateOf<ActivityCard?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun startEditing(entry: DiaryEntry) {
        editingEntry = entry
        editingVariants = emptyList()
        scope.launch { editingVariants = viewModel.variantsFor(entry.foodId) }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(activityState.message) {
        activityState.message?.let {
            snackbarHostState.showSnackbar(it)
            activityViewModel.clearMessage()
        }
    }
    LaunchedEffect(activityState.error) {
        activityState.error?.let {
            snackbarHostState.showSnackbar(it)
            activityViewModel.clearMessage()
        }
    }
    LaunchedEffect(state.date) { activityViewModel.setDate(state.date) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今日") },
                actions = {
                    TextButton(onClick = { confirmClearDay = true }) { Text("清空当天") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Spacing.screenHorizontal,
                end = Spacing.screenHorizontal,
                top = 6.dp,
                bottom = Spacing.listBottom
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)
        ) {
            item(key = "date") {
                DateNavigator(
                    date = state.date,
                    onPreviousDay = viewModel::previousDay,
                    onNextDay = viewModel::nextDay,
                    onPickDate = { showDatePicker = true },
                    onToday = viewModel::goToToday
                )
            }

            item(key = "intake") {
                IntakeSummaryCard(state = state, onOpenGoals = onOpenGoals)
            }

            // ---------------------------------------------------- 2. 今日饮食
            item(key = "food_header") {
                SectionHeader(
                    title = "今日饮食",
                    icon = Icons.Filled.Restaurant,
                    accent = AppColors.Diet,
                    trailing = {
                        IconButton(onClick = { onAddFood(state.date) }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "添加食物",
                                tint = AppColors.Diet
                            )
                        }
                    }
                )
            }

            if (state.entries.isEmpty() && !state.loading) {
                item(key = "food_empty") { EmptyFoodCard { onAddFood(state.date) } }
            }

            state.groupedByMeal.forEach { (meal, entries) ->
                item(key = "meal_${meal.name}") {
                    MealHeader(meal = meal, totalKcal = entries.sumOf { it.macros.calories })
                }
                items(items = entries, key = { entry -> "food_${entry.id}" }) { entry ->
                    DiaryEntryCard(
                        entry = entry,
                        onEdit = { startEditing(entry) },
                        onDelete = { pendingDelete = entry }
                    )
                }
            }

            // ---------------------------------------------------- 3. 今日运动
            item(key = "sport_header") {
                SectionHeader(
                    title = "今日运动",
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    accent = AppColors.Sport,
                    subtitle = if (activityState.isEmpty) {
                        null
                    } else {
                        "${activityState.totalMinutes} 分钟 · 消耗 ${activityState.totalKcalLabel}"
                    },
                    trailing = {
                        IconButton(onClick = { showSportPicker = true }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "添加运动",
                                tint = AppColors.Sport
                            )
                        }
                    }
                )
            }

            item(key = "sport_body") {
                TodayActivityBlock(
                    cards = activityState.cards,
                    loading = activityState.loading,
                    onAdd = { showSportPicker = true },
                    onEditDuration = { durationTarget = it },
                    onEditBurn = { burnTarget = it },
                    onDelete = { deleteActivityTarget = it },
                    onAddExercise = { exercisePickerFor = it },
                    onEditExercise = { exerciseTarget = it },
                    onDeleteExercise = { activityViewModel.deleteExercise(it) },
                    onClearDay = { confirmClearSport = true }
                )
            }
        }
    }

    // --------------------------------------------------------------- 弹窗

    if (showSportPicker) {
        SportPickerDialog(
            title = "添加运动",
            groups = activityState.sportGroups,
            hasStrengthAlready = activityState.strengthCards.isNotEmpty(),
            onPick = { row ->
                activityViewModel.addSport(row.key)
                showSportPicker = false
            },
            onDismiss = { showSportPicker = false }
        )
    }

    durationTarget?.let { card ->
        DurationDialog(
            activityName = card.name,
            currentMinutes = card.activity.durationMinutes,
            onConfirm = { minutes ->
                activityViewModel.setDuration(card.id, minutes)
                durationTarget = null
            },
            onDismiss = { durationTarget = null }
        )
    }

    burnTarget?.let { card ->
        BurnOverrideDialog(
            activityName = card.name,
            currentKcal = card.activity.burnedKcal,
            estimatedKcal = SportLibrary.estimateKcal(
                card.activity.met,
                bodyWeightKg,
                card.activity.durationMinutes
            ),
            onConfirm = { kcal ->
                activityViewModel.overrideBurn(card.id, kcal)
                burnTarget = null
            },
            onDismiss = { burnTarget = null }
        )
    }

    deleteActivityTarget?.let { card ->
        ConfirmDialog(
            title = "删除「${card.name}」？",
            message = "这一项运动记录会被移除，当天消耗合计随之减少。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                activityViewModel.deleteActivity(card.id)
                deleteActivityTarget = null
            },
            onDismiss = { deleteActivityTarget = null }
        )
    }

    exercisePickerFor?.let { card ->
        ExerciseNamePicker(
            names = activityState.exerciseNames,
            onPick = { name ->
                activityViewModel.addExercise(card.id, name)
                exercisePickerFor = null
            },
            onDismiss = { exercisePickerFor = null }
        )
    }

    exerciseTarget?.let { exercise ->
        ExerciseEditorDialog(
            exerciseName = exercise.exerciseName,
            initialWeight = exercise.weightKg,
            initialSets = exercise.sets,
            initialReps = exercise.reps,
            onConfirm = { weight, sets, reps ->
                activityViewModel.updateExercise(exercise.id, weight, sets, reps)
                exerciseTarget = null
            },
            onDismiss = { exerciseTarget = null }
        )
    }

    if (confirmClearSport) {
        ConfirmDialog(
            title = "清空当天运动？",
            message = "今天记录的所有运动与动作都会被删除。",
            confirmLabel = "清空",
            destructive = true,
            onConfirm = {
                activityViewModel.clearDay()
                confirmClearSport = false
            },
            onDismiss = { confirmClearSport = false }
        )
    }

    pendingDelete?.let { entry ->
        ConfirmDialog(
            title = "删除这条记录？",
            message = "「${entry.foodName}」${entry.amountLabel} 将从当天记录中移除。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteEntry(entry.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    if (confirmClearDay) {
        ConfirmDialog(
            title = "清空这一天的记录？",
            message = "${DateUtils.displayDate(state.date)} 的 ${state.entries.size} 条饮食记录" +
                "将被全部删除，此操作不可撤销。",
            confirmLabel = "清空",
            destructive = true,
            onConfirm = {
                viewModel.clearDay()
                confirmClearDay = false
            },
            onDismiss = { confirmClearDay = false }
        )
    }

    editingEntry?.let { entry ->
        EditEntrySheet(
            entry = entry,
            availableVariants = editingVariants,
            onDismiss = {
                editingEntry = null
                editingVariants = emptyList()
            },
            onConfirm = { amount, mode, meal, variantId ->
                viewModel.updateEntry(entry.id, amount, mode, meal, variantId)
                editingEntry = null
                editingVariants = emptyList()
            }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.toEpochMillis(state.date)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            viewModel.setDate(DateUtils.fromEpochMillis(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** 今日运动主体：上段运动项目，下段撸铁。 */
@Composable
private fun TodayActivityBlock(
    cards: List<ActivityCard>,
    loading: Boolean,
    onAdd: () -> Unit,
    onEditDuration: (ActivityCard) -> Unit,
    onEditBurn: (ActivityCard) -> Unit,
    onDelete: (ActivityCard) -> Unit,
    onAddExercise: (ActivityCard) -> Unit,
    onEditExercise: (ExerciseLogEntity) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onClearDay: () -> Unit
) {
    val sportCards = cards.filterNot { it.isStrength }
    val strengthCards = cards.filter { it.isStrength }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            if (loading) {                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "正在读取今天的运动…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Card
            }

            if (cards.isEmpty()) {
                Text(
                    text = "今天还没有运动记录",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "点右上角「+」选一项运动（跑步、游泳、撸铁…），" +
                        "填个时长就会按 MET 算出消耗。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.itemGap))
                TextButton(onClick = onAdd) { Text("添加运动") }
                return@Card
            }

            // 上段：运动
            SubSectionLabel(text = "运动", accent = AppColors.Sport)
            Spacer(Modifier.height(8.dp))
            if (sportCards.isEmpty()) {
                Text(
                    text = "还没有有氧 / 操课记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                sportCards.forEach { card ->
                    SportActivityCard(
                        card = card,
                        onEditDuration = { onEditDuration(card) },
                        onEditBurn = { onEditBurn(card) },
                        onDelete = { onDelete(card) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(6.dp))
            // 下段：撸铁
            SubSectionLabel(text = "撸铁", accent = AppColors.Strength)
            Spacer(Modifier.height(8.dp))
            if (strengthCards.isEmpty()) {
                Text(
                    text = "今天还没练力量。点右上角「+」选「撸铁」，就能往里加动作。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                strengthCards.forEach { card ->
                    StrengthActivityCard(
                        card = card,
                        onEditDuration = { onEditDuration(card) },
                        onEditBurn = { onEditBurn(card) },
                        onDelete = { onDelete(card) },
                        onAddExercise = { onAddExercise(card) },
                        onEditExercise = onEditExercise,
                        onDeleteExercise = onDeleteExercise
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "合计 ${Math.round(cards.sumOf { it.activity.burnedKcal })} kcal" +
                        "（只作显示）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onClearDay) { Text("清空") }
            }
        }
    }
}

@Composable
private fun SubSectionLabel(text: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color = accent, size = 7)
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
    }
}

/** 选动作名的弹窗。 */
@Composable
private fun ExerciseNamePicker(
    names: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(names, query) {
        val q = query.trim()
        if (q.isEmpty()) names else names.filter { it.contains(q, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加动作") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索动作") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text(
                        text = "没有匹配的动作",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(360.dp)) {
                        items(items = filtered, key = { it }) { name ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { onPick(name) }) { Text("添加") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
private fun EmptyFoodCard(onAddFood: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "这一天还没有记录", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "从食物库选一个食物，按克数或按份数记下来",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onAddFood) { Text("添加第一条记录") }
        }
    }
}

@Composable
private fun MealHeader(meal: MealType, totalKcal: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = meal.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${Math.round(totalKcal)} kcal",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DiaryEntryCard(
    entry: DiaryEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.cardPadding, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.foodName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.amountLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    MacroChip("碳", entry.macros.carbs, MacroColors.Carbs)
                    MacroChip("蛋", entry.macros.protein, MacroColors.Protein)
                    MacroChip("脂", entry.macros.fat, MacroColors.Fat)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${Math.round(entry.macros.calories)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MacroColors.Calories
                )
                Text(
                    text = "kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
