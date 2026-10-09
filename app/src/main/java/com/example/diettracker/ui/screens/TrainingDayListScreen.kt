package com.example.diettracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diettracker.data.db.TrainingSplitEntity
import com.example.diettracker.data.model.TrainingObjective
import com.example.diettracker.domain.PlanTemplate
import com.example.diettracker.domain.PlanTemplates
import com.example.diettracker.ui.components.ConfirmDialog
import com.example.diettracker.ui.viewmodel.TrainingDaySummary
import com.example.diettracker.ui.viewmodel.TrainingDayViewModel
import com.example.diettracker.util.DateUtils

/**
 * 「我的 → 训练日管理」。
 *
 * 这个页面取代了旧的「分化 + 轮转排程」设置页（`PlanListScreen` 那套）：那时候用户
 * 只能先选一个分化档位、再调轮转节奏，训练日是计划的副产物。现在训练日本身就是用户
 * 自定义的单位——想建几个就建几个，每个有自己的名字、动作清单和「几天一次」，到期
 * 的训练日会自动出现在今日页，预设计划退化成「一键起步」的模板。
 *
 * 页面分四段：训练日列表（改频率 / 删除 / 进入编辑）、预设计划、训练目标。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingDayListScreen(
    viewModel: TrainingDayViewModel,
    onBack: () -> Unit,
    onCreateDay: () -> Unit,
    onEditDay: (Long) -> Unit
) {
    val state by viewModel.list.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 弹窗只记「对谁操作」，具体内容交给各自的子 Composable，避免把一堆字段摊在主函数里。
    var intervalTarget by remember { mutableStateOf<TrainingDaySummary?>(null) }
    var deleteTarget by remember { mutableStateOf<TrainingDaySummary?>(null) }
    var adoptTarget by remember { mutableStateOf<PlanTemplate?>(null) }

    // message 与 error 共用一个 Snackbar：一次只提示一件事，读完就清空，否则重组会重复弹。
    LaunchedEffect(state.message, state.error) {
        (state.message ?: state.error)?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("训练日") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onCreateDay,
                        enabled = !state.saving
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "新建训练日")
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
                Text(
                    text = "训练日是你自己定的单位，比如「胸 + 三头」「腿」。" +
                        "每个训练日有一份动作清单和一个「几天一次」的频率，" +
                        "到期的训练日会自动出现在今日页。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.loading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if (!state.loading && state.days.isEmpty()) {
                item {
                    EmptyDaysCard(onCreateDay = onCreateDay)
                }
            }

            items(items = state.days, key = { it.id }) { summary ->
                TrainingDayCard(
                    summary = summary,
                    enabled = !state.saving,
                    onEdit = { onEditDay(summary.id) },
                    onEditInterval = { intervalTarget = summary },
                    onDelete = { deleteTarget = summary }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "预设计划",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "不想从零开始就套一个现成的；套用会覆盖上面所有训练日。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(items = state.templates, key = { it.type.name }) { template ->
                PlanTemplateCard(
                    template = template,
                    isCurrent = state.currentTemplateType == template.type && state.days.isNotEmpty(),
                    enabled = !state.saving,
                    onAdopt = { adoptTarget = template }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                ObjectiveCard(
                    objective = state.objective,
                    enabled = !state.saving,
                    onSelect = viewModel::setObjective
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    intervalTarget?.let { summary ->
        IntervalDialog(
            summary = summary,
            enabled = !state.saving,
            onConfirm = { days ->
                viewModel.setInterval(summary.id, days)
                intervalTarget = null
            },
            onAddToday = {
                viewModel.addDayToDate(DateUtils.today(), summary.id)
                intervalTarget = null
            },
            onDismiss = { intervalTarget = null }
        )
    }

    deleteTarget?.let { summary ->
        ConfirmDialog(
            title = "删除「${summary.name}」？",
            message = "这个训练日和它的 ${summary.exerciseCount} 个动作会被一起删掉。" +
                "已经练过的记录不受影响。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteDay(summary.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    adoptTarget?.let { template ->
        ConfirmDialog(
            title = "套用「${template.title}」？",
            message = "会用它的 ${template.dayCount} 个训练日替换掉现有的 " +
                "${state.days.size} 个训练日，当前训练日和动作清单会被覆盖，" +
                "之后仍然可以随意修改。",
            confirmLabel = "套用",
            destructive = true,
            onConfirm = {
                viewModel.adoptTemplate(template)
                adoptTarget = null
            },
            onDismiss = { adoptTarget = null }
        )
    }
}

/** 空态：没有训练日时给一条明确出路，同时把「套用预设计划」指出来。 */
@Composable
private fun EmptyDaysCard(onCreateDay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "还没有训练日",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "先建一个自己常练的组合，比如「胸 + 三头」，再加进动作清单和频率。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onCreateDay, modifier = Modifier.fillMaxWidth()) {
                Text("新建训练日")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "也可以直接在下面套用一个预设计划，一键生成整套训练日。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 一个训练日：信息在上，三个操作在下；点整行进编辑器。 */
@Composable
private fun TrainingDayCard(
    summary: TrainingDaySummary,
    enabled: Boolean,
    onEdit: () -> Unit,
    onEditInterval: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = summary.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = summary.bodyPart.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${summary.exerciseCount} 个动作",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = summary.exercisePreview.ifBlank { "还没有动作，点进去加几个" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = summary.statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onEditInterval, enabled = enabled) {
                    Text("改频率")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete, enabled = enabled) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * 改频率弹窗。
 *
 * 只给 1..10 的固定档位而不是自由输入：常用频率就那么几个，点一下比敲数字快，
 * 也顺手挡住了 0 或 99 这类没意义的输入。
 */
@Composable
private fun IntervalDialog(
    summary: TrainingDaySummary,
    enabled: Boolean,
    onConfirm: (Int) -> Unit,
    onAddToday: () -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember(summary.id) {
        mutableStateOf(
            TrainingSplitEntity.sanitizeInterval(summary.split.intervalDays)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("「${summary.name}」的频率", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    text = "每 $selected 天一次",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                // 分两排摆，窄屏也不会把数字挤成换行。
                (TrainingSplitEntity.MIN_INTERVAL_DAYS..10).chunked(5).forEach { rowValues ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowValues.forEach { value ->
                            FilterChip(
                                selected = value == selected,
                                onClick = { selected = value },
                                enabled = enabled,
                                label = { Text("$value") }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "保存后从下一次完成训练开始生效。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected) },
                enabled = enabled
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onAddToday, enabled = enabled) {
                    Text("加进今天")
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

/** 一张预设计划卡片：看一眼内容再决定套不套。 */
@Composable
private fun PlanTemplateCard(
    template: PlanTemplate,
    isCurrent: Boolean,
    enabled: Boolean,
    onAdopt: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (isCurrent) {
                    Text(
                        text = "当前使用",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = "${template.headline} · 共 ${template.exerciseCount} 个动作",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))

            // 「训练日 — 动作、动作…」：名字和动作各占一份宽度，长动作名不会把名字挤没。
            PlanTemplates.previewLines(template).forEach { (dayName, exercises) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(0.45f)
                    )
                    Text(
                        text = exercises.ifBlank { "暂无动作" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.55f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAdopt,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCurrent) "重新套用这个计划" else "套用这个计划")
            }
        }
    }
}

/** 训练目标：决定新加动作时的默认次数区间和组间休息建议。 */
@Composable
private fun ObjectiveCard(
    objective: TrainingObjective,
    enabled: Boolean,
    onSelect: (TrainingObjective) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "训练目标",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrainingObjective.entries.forEach { option ->
                    FilterChip(
                        selected = option == objective,
                        onClick = { onSelect(option) },
                        enabled = enabled,
                        label = { Text(option.label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = objective.description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "训练目标决定新加动作时的默认次数区间和组间休息建议。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
