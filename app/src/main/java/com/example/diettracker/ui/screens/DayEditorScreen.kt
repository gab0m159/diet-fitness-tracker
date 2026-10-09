package com.example.diettracker.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diettracker.data.db.TrainingDayExerciseEntity
import com.example.diettracker.data.db.TrainingSplitEntity
import com.example.diettracker.domain.ExerciseLibrary
import com.example.diettracker.domain.FrequencyScheduler
import com.example.diettracker.domain.ProgressionEngine
import com.example.diettracker.ui.viewmodel.TrainingDayViewModel

/**
 * 训练日编辑器。
 *
 * 一个训练日由三部分组成，全部在这里设置：
 *
 *  1. 名称与部位；
 *  2. **频率**——「几天一次」，决定它什么时候自动出现在今日页；
 *  3. **动作清单**——每个动作有自己的目标组数、每组次数、目标重量和步进。
 *
 * 后两条是 v6 的关键变化：目标重量和步进属于「这个训练日里的这个动作」，不再有
 * 按动作名全局共享的一份；失败不降重，成功才按这里的步进往上加。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorScreen(
    viewModel: TrainingDayViewModel,
    splitId: Long,
    onBack: () -> Unit
) {
    val state by viewModel.editor.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var pickingExercise by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<TrainingDayExerciseEntity?>(null) }

    LaunchedEffect(splitId) { viewModel.loadEditor(splitId) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearEditorMessages()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearEditorMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "新建训练日" else state.name.ifBlank { "训练日" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------------------------------------------- 名称 / 部位 / 频率
            item(key = "basics") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = viewModel::onNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("训练日名称") },
                            placeholder = { Text("例如：胸 + 三头") },
                            singleLine = true
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("主要部位", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(6.dp))
                        val parts = state.bodyPartChoices
                        parts.chunked(4).forEachIndexed { index, rowParts ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (index == 0) 0.dp else 6.dp)
                            ) {
                                rowParts.forEach { part ->
                                    FilterChip(
                                        selected = state.bodyPart == part,
                                        onClick = { viewModel.onBodyPartChange(part) },
                                        label = { Text(part.label) }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("频率：几天练一次", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "到期的训练日会自动出现在今日页。现在的设置：" +
                                FrequencyScheduler.describeInterval(state.intervalDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 3, 4, 5, 6, 7).forEach { days ->
                                FilterChip(
                                    selected = state.intervalDays == days,
                                    onClick = { viewModel.onIntervalChange(days) },
                                    label = { Text("$days 天") }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.intervalDays.toString(),
                            onValueChange = { raw ->
                                raw.filter { it.isDigit() }
                                    .take(2)
                                    .toIntOrNull()
                                    ?.let(viewModel::onIntervalChange)
                            },
                            label = { Text("自定义天数") },
                            supportingText = {
                                Text(
                                    "可选 ${TrainingSplitEntity.MIN_INTERVAL_DAYS}-" +
                                        "${TrainingSplitEntity.MAX_INTERVAL_DAYS} 天"
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // ---------------------------------------------------------- 动作清单
            item(key = "exercise_header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "动作清单（${state.rows.size}）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { pickingExercise = true }) { Text("添加动作") }
                }
            }

            if (state.rows.isEmpty()) {
                item(key = "exercise_empty") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "还没有动作",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "从动作库里挑动作加进来，再给每个动作设目标组数、" +
                                    "每组次数、目标重量和步进。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { pickingExercise = true }) { Text("添加动作") }
                        }
                    }
                }
            }

            items(items = state.rows, key = { it.id }) { row ->
                ExerciseTargetCard(
                    row = row,
                    canMoveUp = state.rows.firstOrNull()?.id != row.id,
                    canMoveDown = state.rows.lastOrNull()?.id != row.id,
                    onMoveUp = { viewModel.moveExercise(row.id, -1) },
                    onMoveDown = { viewModel.moveExercise(row.id, 1) },
                    onEdit = { editingRow = row },
                    onDelete = { viewModel.removeExercise(row.id) }
                )
            }

            item(key = "save") {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        val wasNew = state.isNew
                        viewModel.saveDayBasics()
                        if (wasNew) onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.saving && state.name.isNotBlank()
                ) {
                    Text(if (state.isNew) "创建训练日" else "保存")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "提示：名称、部位和频率要点一下保存；" +
                        "动作的目标组数 / 次数 / 重量 / 步进是点「保存」即时生效的。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (pickingExercise) {
        ExercisePickerDialog(
            candidates = state.allExercises,
            alreadyPicked = state.rows.map { it.exerciseName }.toSet(),
            onDismiss = { pickingExercise = false },
            onPick = { name ->
                viewModel.addExercise(name)
                pickingExercise = false
            }
        )
    }

    editingRow?.let { row ->
        ExerciseTargetDialog(
            row = row,
            onDismiss = { editingRow = null },
            onConfirm = { sets, reps, weight, increment ->
                viewModel.updateTarget(row.id, sets, reps, weight, increment)
                editingRow = null
            }
        )
    }
}

/** 一行动作：名称 + 目标摘要 + 排序 / 编辑 / 移除。 */
@Composable
private fun ExerciseTargetCard(
    row: TrainingDayExerciseEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "目标 ${row.targetSets} 组 × " +
                        ProgressionEngine.repsLabel(row.targetReps) + " · " +
                        weightLabelOf(row.targetWeightKg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "步进 " + incrementLabelOf(row.incrementKg),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = "上移",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(
                    Icons.Filled.ArrowDownward,
                    contentDescription = "下移",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "移除",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** 编辑一个动作的目标组数 / 次数 / 重量 / 步进。 */
@Composable
private fun ExerciseTargetDialog(
    row: TrainingDayExerciseEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Double, Double?) -> Unit
) {
    var setsText by remember { mutableStateOf(row.targetSets.toString()) }
    var repsText by remember { mutableStateOf(row.targetReps) }
    var weightText by remember {
        mutableStateOf(if (row.targetWeightKg > 0.0) trimNumber(row.targetWeightKg) else "")
    }
    var incrementText by remember {
        mutableStateOf(trimNumber(row.incrementKg ?: 0.0))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.exerciseName) },
        text = {
            Column {
                Text(
                    text = "这些目标只属于这个训练日。成功时目标重量按步进值加，" +
                        "失败和跳过都不变。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = setsText,
                    onValueChange = { raw -> setsText = raw.filter { it.isDigit() }.take(2) },
                    label = { Text("目标组数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it.take(20) },
                    label = { Text("每组次数") },
                    supportingText = { Text("可以填 8、8-12 或 12,12,10") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { raw ->
                        weightText = raw.filter { ch -> ch.isDigit() || ch == '.' }.take(6)
                    },
                    label = { Text("目标重量") },
                    suffix = { Text("kg") },
                    supportingText = { Text("留空表示还没定") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = incrementText,
                    onValueChange = { raw ->
                        incrementText = raw.filter { ch -> ch.isDigit() || ch == '.' }.take(6)
                    },
                    label = { Text("步进（成功一次加多少）") },
                    suffix = { Text("kg") },
                    supportingText = { Text("默认 0，表示不自动加重；要加重就自己填，例如 2.5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        setsText.toIntOrNull() ?: TrainingDayExerciseEntity.DEFAULT_SETS,
                        repsText.trim().ifBlank { TrainingDayExerciseEntity.DEFAULT_REPS },
                        weightText.toDoubleOrNull() ?: 0.0,
                        incrementText.toDoubleOrNull() ?: 0.0
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 从动作库里挑一个动作加进训练日。 */
@Composable
private fun ExercisePickerDialog(
    candidates: List<String>,
    alreadyPicked: Set<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(candidates, query, alreadyPicked) {
        val q = query.trim()
        val base = if (q.isEmpty()) candidates else candidates.filter { it.contains(q, true) }
        base.filterNot { it in alreadyPicked }
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
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("搜索动作") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text(
                        text = "没有可选的动作（已经在清单里的不会重复出现）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(320.dp)) {
                        items(items = filtered, key = { it }) { name ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(name) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "添加",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
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

private fun weightLabelOf(weightKg: Double): String =
    if (weightKg > 0.0) "${trimNumber(weightKg)}kg" else "目标重量未设定"

private fun incrementLabelOf(incrementKg: Double?): String {
    val value = incrementKg ?: 0.0
    return if (value <= 0.0) "0kg（不自动加重）" else "${trimNumber(value)}kg"
}

private fun trimNumber(value: Double): String {
    val rounded = Math.round(value * 100.0) / 100.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
    else rounded.toString()
}
