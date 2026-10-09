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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.diettracker.domain.ProgressionEngine
import com.example.diettracker.ui.viewmodel.TrainingDaySummary
import com.example.diettracker.ui.viewmodel.TrainingDayViewModel
import com.example.diettracker.ui.viewmodel.WorkoutViewModel
import com.example.diettracker.util.DateUtils

/**
 * 「添加到今天」：把训练或动作加进某一天的日程。
 *
 * 两种添加方式（产品需求里的两条）：
 *
 *  1. **添加整个训练日**——从已创建的训练日里挑一个，加入今天，它的动作清单和
 *     目标（组数 / 次数 / 目标重量）会一起显示在今日页；
 *  2. **单独添加一个动作**——直接从动作库挑一个动作加入今天，不依赖任何训练日，
 *     组数 / 次数 / 目标重量 / 步进当场填。
 *
 * 注意「频率到期」的训练日不需要来这里加：它们会自动出现在今日页。这个页面是给
 * 「今天想临时练点什么」准备的。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToTodayScreen(
    date: String,
    dayViewModel: TrainingDayViewModel,
    workoutViewModel: WorkoutViewModel,
    onDone: () -> Unit,
    onManageDays: () -> Unit
) {
    val listState by dayViewModel.list.collectAsStateWithLifecycle()
    val exerciseNames by dayViewModel.exerciseNames.collectAsStateWithLifecycle()
    val workoutState by workoutViewModel.home.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var segment by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var pendingExercise by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(workoutState.message) {
        workoutState.message?.let {
            snackbarHostState.showSnackbar(it)
            workoutViewModel.clearMessage()
            onDone()
        }
    }
    LaunchedEffect(workoutState.error) {
        workoutState.error?.let {
            snackbarHostState.showSnackbar(it)
            workoutViewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加到${DateUtils.friendlyLabel(date)}") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                listOf("添加训练日", "添加单个动作").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = segment == index,
                        onClick = { segment = index },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        label = { Text(label, style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }

            if (segment == 0) {
                TrainingDayPicker(
                    days = listState.days,
                    loading = listState.loading,
                    onPick = { splitId -> workoutViewModel.addTrainingDayToDate(date, splitId) },
                    onManageDays = onManageDays
                )
            } else {
                ExercisePicker(
                    query = query,
                    onQueryChange = { query = it },
                    names = exerciseNames,
                    onPick = { pendingExercise = it }
                )
            }
        }
    }

    pendingExercise?.let { name ->
        SingleExerciseDialog(
            exerciseName = name,
            defaultReps = ProgressionEngine.defaultRepsSpec(listState.objective),
            onDismiss = { pendingExercise = null },
            onConfirm = { sets, reps, weight, increment ->
                workoutViewModel.addSingleExerciseToDate(
                    date = date,
                    exerciseName = name,
                    targetSets = sets,
                    targetReps = reps,
                    targetWeightKg = weight,
                    incrementKg = increment
                )
                pendingExercise = null
            }
        )
    }
}

/** 方式一：从已创建的训练日里挑一个加进今天。 */
@Composable
private fun TrainingDayPicker(
    days: List<TrainingDaySummary>,
    loading: Boolean,
    onPick: (Long) -> Unit,
    onManageDays: () -> Unit
) {
    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (days.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "还没有训练日",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "先创建训练日，再把它加进今天",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onManageDays) { Text("创建训练日") }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "挑一个训练日加进今天。它的动作清单和每个动作的目标重量会一起显示在今日页。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(items = days, key = { it.id }) { day ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = day.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${day.bodyPart.label} · ${day.exerciseCount} 个动作 · " +
                                    day.intervalLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (day.exercisePreview.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = day.exercisePreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = day.statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onPick(day.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("加入今天") }
                }
            }
        }
    }
}

/** 方式二：从动作库挑一个动作。 */
@Composable
private fun ExercisePicker(
    query: String,
    onQueryChange: (String) -> Unit,
    names: List<String>,
    onPick: (String) -> Unit
) {
    val filtered = remember(names, query) {
        val q = query.trim()
        if (q.isEmpty()) names else names.filter { it.contains(q, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("搜索动作") },
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "单独加进来的动作不挂在任何训练日上，目标重量和步进只在今天这张卡片上生效。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "没有匹配的动作",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
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

/** 单独添加动作时填目标组数 / 次数 / 重量 / 步进。 */
@Composable
private fun SingleExerciseDialog(
    exerciseName: String,
    defaultReps: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Double, Double?) -> Unit
) {
    var setsText by remember { mutableStateOf(TrainingDayExerciseEntity.DEFAULT_SETS.toString()) }
    var repsText by remember { mutableStateOf(defaultReps) }
    var weightText by remember { mutableStateOf("") }
    var incrementText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            Column {
                Text(
                    text = "填今天这个动作的目标。目标重量留空表示还没定，成功后会按步进加重。",
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
                    supportingText = { Text("默认 0，表示不自动加重") },
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
                        repsText.trim().ifBlank { defaultReps },
                        weightText.toDoubleOrNull() ?: 0.0,
                        incrementText.toDoubleOrNull() ?: 0.0
                    )
                }
            ) { Text("加入今天") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
