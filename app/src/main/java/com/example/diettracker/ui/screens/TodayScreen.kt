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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diettracker.data.db.ExerciseOutcome
import com.example.diettracker.data.model.MealType
import com.example.diettracker.data.repository.TodayExerciseCard
import com.example.diettracker.data.repository.TodayGroupKind
import com.example.diettracker.data.repository.TodayPlan
import com.example.diettracker.data.repository.TodayTrainingGroup
import com.example.diettracker.data.repository.TrainingRepository
import com.example.diettracker.ui.components.ConfirmDialog
import com.example.diettracker.ui.components.DateNavigator
import com.example.diettracker.ui.components.EditEntrySheet
import com.example.diettracker.ui.components.IntakeSummaryCard
import com.example.diettracker.ui.components.MacroChip
import com.example.diettracker.ui.theme.MacroColors
import com.example.diettracker.ui.viewmodel.DiaryViewModel
import com.example.diettracker.ui.viewmodel.WorkoutViewModel
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.launch

/**
 * 首页。
 *
 * 三段竖排：当天摄入 / 今日训练 / 今日饮食，每段自己的加号做自己的事。
 *
 * 训练段 v6 起直接列出**训练日分组 + 动作卡片**：到期的训练日自动出现，也可以
 * 手动加进来；卡片上按成功 / 失败 / 跳过，训练日层面还有整体跳过和整体延期。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: DiaryViewModel,
    trainingRepository: TrainingRepository,
    onAddFood: (String) -> Unit,
    onAddTraining: (String) -> Unit,
    onOpenLog: () -> Unit,
    onOpenGoals: () -> Unit,
    onManageDays: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 训练块和详细记录页共用同一个 ViewModel，标记完首页立刻同步。
    val workoutViewModel: WorkoutViewModel = viewModel(
        factory = WorkoutViewModel.factory(trainingRepository)
    )
    val workoutState by workoutViewModel.home.collectAsStateWithLifecycle()

    var pendingDelete by remember { mutableStateOf<com.example.diettracker.data.model.DiaryEntry?>(null) }
    var confirmClearDay by remember { mutableStateOf(false) }
    var skipDayGroup by remember { mutableStateOf<TodayTrainingGroup?>(null) }
    var confirmPostpone by remember { mutableStateOf(false) }
    var removeEntryId by remember { mutableStateOf<Long?>(null) }
    var editingEntry by remember { mutableStateOf<com.example.diettracker.data.model.DiaryEntry?>(null) }
    var editingVariants by remember {
        mutableStateOf<List<com.example.diettracker.data.db.FoodVariantEntity>>(emptyList())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    /** 打开编辑面板前先把该食物的规格读出来。 */
    fun startEditing(entry: com.example.diettracker.data.model.DiaryEntry) {
        editingEntry = entry
        editingVariants = emptyList()
        scope.launch {
            editingVariants = viewModel.variantsFor(entry.foodId)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(workoutState.message) {
        workoutState.message?.let {
            snackbarHostState.showSnackbar(it)
            workoutViewModel.clearMessage()
        }
    }
    LaunchedEffect(workoutState.error) {
        workoutState.error?.let {
            snackbarHostState.showSnackbar(it)
            workoutViewModel.clearMessage()
        }
    }

    // 训练块跟着日期走，翻历史能看到那天练了什么。
    LaunchedEffect(state.date) {
        workoutViewModel.setDate(state.date)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今日") },
                actions = {
                    TextButton(onClick = { confirmClearDay = true }) {
                        Text("清空当天")
                    }
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
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DateNavigator(
                    date = state.date,
                    onPreviousDay = viewModel::previousDay,
                    onNextDay = viewModel::nextDay,
                    onPickDate = { showDatePicker = true },
                    onToday = viewModel::goToToday
                )
            }

            // ---------------------------------------------------- 1. 当天摄入
            item(key = "intake") {
                IntakeSummaryCard(
                    state = state,
                    onOpenGoals = onOpenGoals
                )
            }

            // ------------------------------------------------------ 2. 今日训练
            item(key = "training_header") {
                BlockHeader(
                    icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
                    title = "今日训练",
                    onAdd = { onAddTraining(state.date) },
                    addDescription = "添加训练"
                )
            }
            item(key = "training_body") {
                TodayTrainingBlock(
                    plan = workoutState.plan,
                    date = state.date,
                    loading = workoutState.loading,
                    busy = workoutState.saving,
                    onAddTraining = { onAddTraining(state.date) },
                    onManageDays = onManageDays,
                    onOpenLog = onOpenLog,
                    onMarkSuccess = workoutViewModel::markSuccess,
                    onMarkFailure = workoutViewModel::markFailure,
                    onMarkSkip = workoutViewModel::markSkip,
                    onReset = workoutViewModel::resetOutcome,
                    onRemoveEntry = { removeEntryId = it },
                    onSkipDay = { skipDayGroup = it }
                )
            }

            // ------------------------------------------------------ 3. 今日饮食
            item(key = "food_header") {
                BlockHeader(
                    icon = { Icon(Icons.Filled.Restaurant, contentDescription = null) },
                    title = "今日饮食",
                    onAdd = { onAddFood(state.date) },
                    addDescription = "添加食物"
                )
            }

            if (state.entries.isEmpty() && !state.loading) {
                item(key = "food_empty") {
                    EmptyFoodCard(onAddFood = { onAddFood(state.date) })
                }
            }

            state.groupedByMeal.forEach { (meal, entries) ->
                item(key = "meal_${meal.name}") {
                    MealHeader(meal = meal, totalKcal = entries.sumOf { it.macros.calories })
                }
                items(items = entries, key = { it.id }) { entry ->
                    DiaryEntryCard(
                        entry = entry,
                        onEdit = { startEditing(entry) },
                        onDelete = { pendingDelete = entry }
                    )
                }
            }
        }
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
            message = "${DateUtils.displayDate(state.date)} 的 ${state.entries.size} 条记录" +
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

    skipDayGroup?.let { group ->
        ConfirmDialog(
            title = "跳过「${group.title}」？",
            message = "该训练日下的 ${group.pendingCount} 个动作会被全部标记为跳过，" +
                "从今天移除，也不会进入往期记录。",
            confirmLabel = "跳过",
            destructive = true,
            onConfirm = {
                workoutViewModel.skipDay(group.key)
                skipDayGroup = null
            },
            onDismiss = { skipDayGroup = null }
        )
    }

    removeEntryId?.let { entryId ->
        ConfirmDialog(
            title = "从今天移除？",
            message = "只会把它从今天的日程里拿走，不记录成功或失败，目标重量也不变。",
            confirmLabel = "移除",
            onConfirm = {
                workoutViewModel.removeScheduleEntry(entryId)
                removeEntryId = null
            },
            onDismiss = { removeEntryId = null }
        )
    }

    if (confirmPostpone) {
        ConfirmDialog(
            title = "延期到明天？",
            message = "所有训练日统一往后顺延一天：今天的内容整体挪到明天，后面的训练日一起顺延。" +
                "不会记录成失败或跳过。",
            confirmLabel = "延期",
            onConfirm = {
                workoutViewModel.postponeAll()
                confirmPostpone = false
            },
            onDismiss = { confirmPostpone = false }
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

/** 段落标题 + 尾部加号。 */
@Composable
private fun BlockHeader(
    icon: @Composable () -> Unit,
    title: String,
    onAdd: () -> Unit,
    addDescription: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onAdd) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = addDescription,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 今日训练栏。
 *
 * 只显示「今天该练的东西」：到期的训练日、手动加进来的训练日和单独动作，每个
 * 训练日一组，组里是动作卡片（动作名 + 目标组数 × 次数 + 目标重量 + 三个按钮）。
 */
@Composable
private fun TodayTrainingBlock(
    plan: TodayPlan?,
    date: String,
    loading: Boolean,
    busy: Boolean,
    onAddTraining: () -> Unit,
    onManageDays: () -> Unit,
    onOpenLog: () -> Unit,
    onMarkSuccess: (String, String) -> Unit,
    onMarkFailure: (String, String) -> Unit,
    onMarkSkip: (String, String) -> Unit,
    onReset: (String, String) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onSkipDay: (TodayTrainingGroup) -> Unit
) {
    val isToday = DateUtils.isToday(date)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "正在计算今天该练什么…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Card
            }

            if (plan == null || !plan.hasTrainingDays) {
                Text(
                    text = "还没有训练日",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "先创建训练日（比如「胸 + 三头」），给它排好动作目标和频率，" +
                        "到期的训练日会自动出现在这里。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onManageDays) { Text("创建训练日") }
                    OutlinedButton(onClick = onAddTraining) { Text("添加到今天") }
                }
                return@Card
            }

            if (!plan.hasAnything) {
                Text(
                    text = if (plan.isToday) "今天不用练" else "这一天没有训练安排",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "没有到期的训练日。想练的话可以从下面手动加一个进来。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAddTraining) { Text("添加到今天") }
                    OutlinedButton(onClick = onManageDays) { Text("管理训练日") }
                }
                return@Card
            }

            // 汇总行
            Text(
                text = "共 ${plan.totalCards} 个动作 · 已完成 ${plan.doneCards}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            plan.groups.forEachIndexed { index, group ->
                if (index > 0) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
                Spacer(Modifier.height(10.dp))
                TrainingGroupBlock(
                    group = group,
                    isToday = isToday,
                    busy = busy,
                    onMarkSuccess = { cardKey -> onMarkSuccess(group.key, cardKey) },
                    onMarkFailure = { cardKey -> onMarkFailure(group.key, cardKey) },
                    onMarkSkip = { cardKey -> onMarkSkip(group.key, cardKey) },
                    onReset = { cardKey -> onReset(group.key, cardKey) },
                    onRemoveEntry = onRemoveEntry,
                    onSkipDay = { onSkipDay(group) }
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenLog,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("时长/热量")
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "「时长/热量」里填今天的训练时长，系统按 MET 估算消耗，" +
                    "结果只用于饮食页的「运动消耗」显示。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 一个训练日（或「单独动作」）的卡片组。 */
@Composable
private fun TrainingGroupBlock(
    group: TodayTrainingGroup,
    isToday: Boolean,
    busy: Boolean,
    onMarkSuccess: (String) -> Unit,
    onMarkFailure: (String) -> Unit,
    onMarkSkip: (String) -> Unit,
    onReset: (String) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onSkipDay: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            val subtitle = when {
                group.subtitle.isNotBlank() -> group.subtitle
                group.kind == TodayGroupKind.SINGLE -> "单独加进来的动作"
                else -> ""
            }
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (group.cards.isNotEmpty()) {
            Text(
                text = group.progressLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (group.allDone) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }

    if (group.cards.isEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "这个训练日还没有动作，去「我的 → 训练日」里给它加动作。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Spacer(Modifier.height(6.dp))
    group.cards.forEach { card ->
        ExerciseCardRow(
            card = card,
            enabled = !busy,
            onSuccess = { onMarkSuccess(card.key) },
            onFailure = { onMarkFailure(card.key) },
            onSkip = { onMarkSkip(card.key) },
            onReset = { onReset(card.key) }
        )
        Spacer(Modifier.height(8.dp))
    }

    if (isToday && group.canSkipWholeDay) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onSkipDay,
                modifier = Modifier.weight(1f)
            ) { Text("跳过训练日") }
            group.manualEntryId?.let { entryId ->
                OutlinedButton(
                    onClick = { onRemoveEntry(entryId) },
                    modifier = Modifier.weight(1f)
                ) { Text("从今天移除") }
            }
        }
    }
}

/** 一张动作卡片：动作名、目标组数 × 次数、目标重量，以及成功 / 失败 / 跳过。 */
@Composable
private fun ExerciseCardRow(
    card: TodayExerciseCard,
    enabled: Boolean,
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                card.outcome?.let { outcome ->
                    AssistChip(
                        onClick = { if (enabled) onReset() },
                        label = { Text(outcome.label) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "目标 ${card.targetLabel} · ${card.weightProgressLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            card.lastSummary?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "上次 $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (card.resultText.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = card.resultText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = onSuccess,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) { Text("成功") }
                OutlinedButton(
                    onClick = onFailure,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) { Text("失败") }
                OutlinedButton(
                    onClick = onSkip,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) { Text("跳过") }
            }
            if (card.outcome != null) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onReset, enabled = enabled) { Text("重新选择") }
                }
            }
        }
    }
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
            Text(
                text = "这一天还没有记录",
                style = MaterialTheme.typography.titleMedium
            )
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
    entry: com.example.diettracker.data.model.DiaryEntry,
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
                .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.foodName,
                    style = MaterialTheme.typography.titleMedium,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(20.dp)
                )
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
