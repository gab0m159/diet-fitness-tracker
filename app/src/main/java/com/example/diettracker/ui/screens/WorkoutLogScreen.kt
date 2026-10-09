package com.example.diettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.diettracker.data.model.TrainingIntensity
import com.example.diettracker.ui.components.ErrorBanner
import com.example.diettracker.ui.components.MacroProgressBar
import com.example.diettracker.ui.components.StretchSection
import com.example.diettracker.ui.viewmodel.ExerciseEntry
import com.example.diettracker.ui.viewmodel.WorkoutViewModel

/**
 * Workout log: one editable row per exercise, plus the session metrics.
 *
 * 每行是今天日程里的一张卡片：重量 / 组数 / 每组次数可以逐组填。目标重量和步进
 * 不在这里改——它们属于训练日里的那个动作，在「我的 → 训练日」里设置；这里填的
 * 是实际做成的重量和次数，完成后按 MET 估算当天的消耗热量。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLogScreen(
    viewModel: WorkoutViewModel,
    onDone: () -> Unit,
    onOpenStretch: (String) -> Unit = {}
) {
    val state by viewModel.log.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { "训练记录" }) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SessionMetricsCard(
                    durationText = state.durationText,
                    intensity = state.intensity,
                    burnText = state.burnText,
                    burnOverridden = state.burnOverridden,
                    estimatedBurn = state.estimatedBurn,
                    bodyWeightKg = state.bodyWeightKg,
                    onDurationChange = viewModel::onDurationChange,
                    onIntensityChange = viewModel::onIntensityChange,
                    onBurnChange = viewModel::onBurnChange,
                    onResetBurn = viewModel::resetBurnToEstimate
                )
            }

            item {
                Text(
                    text = "动作记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            itemsIndexedEntries(
                entries = state.entries,
                onWeightChange = viewModel::onWeightChange,
                onSetsChange = viewModel::onSetsChange,
                onRepsChange = viewModel::onRepsChange
            )

            item {
                state.error?.let { ErrorBanner(it) }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.save(complete = false) },
                        enabled = !state.saving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("保存草稿")
                    }
                    Button(
                        onClick = { viewModel.save(complete = true) },
                        enabled = !state.saving,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("完成训练")
                    }
                }
            }

            item {
                SummaryLine(
                    entryCount = state.completedEntries,
                    totalVolume = state.totalVolume,
                    restLabel = state.restLabel
                )
            }

            if (state.showStretches && state.stretches.isNotEmpty()) {
                item {
                    Column {
                        StretchSection(
                            guides = state.stretches,
                            onOpenStretch = onOpenStretch
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = viewModel::dismissStretches) {
                            Text("收起拉伸指导")
                        }
                    }
                }
            }
        }
    }
}

/** Lazy list items for the exercise rows. */
private fun LazyListScope.itemsIndexedEntries(
    entries: List<ExerciseEntry>,
    onWeightChange: (Int, String) -> Unit,
    onSetsChange: (Int, String) -> Unit,
    onRepsChange: (Int, String) -> Unit
) {
    items(
        count = entries.size,
        // 用卡片身份而不是动作名做 key：同一天可能出现两张同名卡片。
        key = { index -> entries[index].cardKey }
    ) { index ->
        val entry = entries[index]
        ExerciseRow(
            index = index,
            entry = entry,
            onWeightChange = onWeightChange,
            onSetsChange = onSetsChange,
            onRepsChange = onRepsChange
        )
    }
}

@Composable
private fun ExerciseRow(
    index: Int,
    entry: ExerciseEntry,
    onWeightChange: (Int, String) -> Unit,
    onSetsChange: (Int, String) -> Unit,
    onRepsChange: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${entry.repsMin}-${entry.repsMax} 次",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            entry.lastSummary?.let { last ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "上次：$last" +
                        if (entry.personalBest > 0.0) {
                            "　最好：${trimKg(entry.personalBest)}kg"
                        } else {
                            ""
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = entry.weightText,
                    onValueChange = { onWeightChange(index, it) },
                    label = { Text("重量") },
                    suffix = { Text("kg") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = entry.setsText,
                    onValueChange = { onSetsChange(index, it) },
                    label = { Text("组数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(90.dp),
                    shape = MaterialTheme.shapes.medium
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = entry.repsText,
                onValueChange = { onRepsChange(index, it) },
                label = { Text("每组次数") },
                placeholder = { Text("例如 12,12,10") },
                supportingText = {
                    Text(
                        text = if (entry.reps.isEmpty()) {
                            "用逗号分隔，例如 12,12,10"
                        } else {
                            "共 ${entry.reps.size} 组 / ${entry.totalReps} 次" +
                                "　容量 ${Math.round(entry.volume)}kg"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

/** Duration, intensity and the MET-based burn with manual override. */
@Composable
private fun SessionMetricsCard(
    durationText: String,
    intensity: TrainingIntensity,
    burnText: String,
    burnOverridden: Boolean,
    estimatedBurn: Double,
    bodyWeightKg: Double,
    onDurationChange: (String) -> Unit,
    onIntensityChange: (TrainingIntensity) -> Unit,
    onBurnChange: (String) -> Unit,
    onResetBurn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "训练时长与消耗",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = durationText,
                onValueChange = onDurationChange,
                label = { Text("时长") },
                suffix = { Text("分钟") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "强度档位（用于估算）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainingIntensity.entries.forEach { option ->
                    AssistChip(
                        onClick = { onIntensityChange(option) },
                        label = {
                            Text(
                                if (intensity == option) {
                                    "✓ ${option.label}"
                                } else {
                                    option.label
                                }
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = burnText,
                onValueChange = onBurnChange,
                label = { Text("消耗热量") },
                suffix = { Text("kcal") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                supportingText = {
                    Text(
                        text = if (burnOverridden) {
                            "已手动填写（估算值约 ${Math.round(estimatedBurn)} kcal）"
                        } else {
                            "按 MET ${intensity.met} × 体重 ${trimKg(bodyWeightKg)}kg × 时长估算"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )

            if (burnOverridden) {
                TextButton(onClick = onResetBurn) { Text("用估算值") }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "该消耗只作为当天饮食汇总里的「运动消耗」显示，" +
                    "不会自动增加可摄入额度。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryLine(entryCount: Int, totalVolume: Double, restLabel: String) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(Modifier.height(8.dp))
        Text(
            text = "已填 $entryCount 个动作 · 总容量 ${Math.round(totalVolume)}kg · " +
                "组间休息 $restLabel",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun trimKg(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
    else rounded.toString()
}
