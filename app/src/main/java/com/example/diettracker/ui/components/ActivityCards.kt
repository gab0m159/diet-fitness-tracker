package com.example.diettracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.diettracker.data.db.ExerciseLogEntity
import com.example.diettracker.domain.SportLibrary
import com.example.diettracker.ui.theme.AppColors
import com.example.diettracker.ui.theme.Spacing
import com.example.diettracker.ui.viewmodel.ActivityCard
import com.example.diettracker.ui.viewmodel.SportRow

/**
 * 有氧 / 操课这一类运动项目的卡片。
 *
 * 显示：运动名 · 时长 · 消耗热量；点「改时长」或「改热量」都能编辑，
 * 点删除移除这一项。
 */
@Composable
fun SportActivityCard(
    card: ActivityCard,
    onEditDuration: () -> Unit,
    onEditBurn: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.SportSoft.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = card.durationLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = AppColors.Sport
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = card.kcalLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Sport
                    )
                    if (card.isOverridden) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "已手改",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onEditDuration) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "改时长",
                    modifier = Modifier.size(19.dp),
                    tint = AppColors.Sport
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "删除",
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 撸铁卡片：上面是「撸铁 + 时长 + 热量」，下面挂动作清单。
 *
 * [highlighted] 为真时加一圈橙色边框并把底色加重——今日运动里撸铁会被置顶并高亮，
 * 让人一眼看到今天的主力训练。
 */
@Composable
fun StrengthActivityCard(
    card: ActivityCard,
    onEditDuration: () -> Unit,
    onEditBurn: () -> Unit,
    onDelete: () -> Unit,
    onAddExercise: () -> Unit,
    onEditExercise: (ExerciseLogEntity) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier.border(
                        width = 2.dp,
                        color = AppColors.Strength.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                AppColors.StrengthSoft
            } else {
                AppColors.StrengthSoft.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Strength.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = AppColors.Strength
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${card.durationLabel} · ${card.kcalLabel}" +
                            if (card.isOverridden) "（已手改）" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.Strength
                    )
                }
                IconButton(onClick = onEditDuration) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "改时长",
                        modifier = Modifier.size(19.dp),
                        tint = AppColors.Strength
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "删除",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(Spacing.itemGap))
            ThinDivider()
            Spacer(Modifier.height(Spacing.itemGap))

            if (card.exercises.isEmpty()) {
                Text(
                    text = "还没有动作，点下面的「加动作」开始记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                card.exercises.forEach { exercise ->
                    ExerciseLogRow(
                        exercise = exercise,
                        onEdit = { onEditExercise(exercise) },
                        onDelete = { onDeleteExercise(exercise.id) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (card.totalVolume > 0) {
                    Text(
                        text = "总容量 ${Math.round(card.totalVolume)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            OutlinedButton(
                onClick = onAddExercise,
                modifier = Modifier.fillMaxWidth()
            ) { Text("加动作") }
        }
    }
}

/** 一行动作：`动作名 · 60kg × 4 组 × 8 次`。 */
@Composable
private fun ExerciseLogRow(
    exercise: ExerciseLogEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .padding(start = 12.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = exercise.summary,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "编辑",
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "删除",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * 选运动项目的弹窗。
 *
 * **撸铁单独橙色置顶**：它是最常用的项目，而且能往下挂动作卡片，所以不做成
 * 分组里的一项，而是提到最上面、整行用橙色高亮。其余项目按分组排在后面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportPickerDialog(
    title: String,
    groups: List<Pair<com.example.diettracker.domain.SportCategory, List<SportRow>>>,
    onPick: (SportRow) -> Unit,
    onDismiss: () -> Unit,
    hasStrengthAlready: Boolean = false
) {
    // 把撸铁从分组里摘出来，单独置顶。
    val strengthRow = groups.asSequence()
        .flatMap { it.second.asSequence() }
        .firstOrNull { it.key == SportLibrary.STRENGTH_KEY }
    val restGroups = groups.map { (category, rows) ->
        category to rows.filterNot { it.key == SportLibrary.STRENGTH_KEY }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier.height(430.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // ---- 撸铁：置顶 + 橙色高亮 ----
                if (strengthRow != null) {
                    item(key = "strength_pinned") {
                        StrengthPinnedRow(
                            row = strengthRow,
                            disabled = hasStrengthAlready,
                            onClick = { onPick(strengthRow) }
                        )
                    }
                    item(key = "divider") {
                        Spacer(Modifier.height(8.dp))
                    }
                }

                restGroups.forEach { (category, rows) ->
                    if (rows.isEmpty()) return@forEach
                    item(key = "h_${category.name}") {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(items = rows, key = { it.key }) { row ->
                        SportPickerRow(row = row, disabled = false, onClick = { onPick(row) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 置顶的撸铁行：橙色底 + 橙色边框，一眼能认出来。 */
@Composable
private fun StrengthPinnedRow(
    row: SportRow,
    disabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (disabled) {
                    AppColors.StrengthSoft.copy(alpha = 0.45f)
                } else {
                    AppColors.StrengthSoft
                }
            )
            .border(
                width = 1.5.dp,
                color = AppColors.Strength.copy(alpha = if (disabled) 0.3f else 0.6f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !disabled, onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.Strength.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = AppColors.Strength,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Strength
            )
            Text(
                text = buildString {
                    append("可加动作 · ")
                    append(row.defaultLabel)
                    append(" · ")
                    append(row.metLabel)
                    if (disabled) append("（今天已有）")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = row.kcalLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.Strength
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "添加",
            style = MaterialTheme.typography.labelLarge,
            color = if (disabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                AppColors.Strength
            }
        )
    }
}

@Composable
private fun SportPickerRow(
    row: SportRow,
    disabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (disabled) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                } else {
                    Color.Transparent
                }
            )
            .padding(vertical = 9.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (disabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = buildString {
                    append(row.defaultLabel)
                    append(" · ")
                    append(row.metLabel)
                    if (row.info.note.isNotBlank()) {
                        append(" · ")
                        append(row.info.note)
                    }
                    if (disabled) append("（今天已有撸铁）")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = row.kcalLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.Sport
        )
        Spacer(Modifier.width(10.dp))
        TextButton(onClick = onClick, enabled = !disabled) { Text("添加") }
    }
}

/** 改时长（分钟）。 */
@Composable
fun DurationDialog(
    activityName: String,
    currentMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentMinutes.toString()) }
    val quick = listOf(15, 20, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$activityName 时长") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { raw -> text = raw.filter { it.isDigit() }.take(3) },
                    label = { Text("分钟") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("常用时长", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(96.dp)
                ) {
                    items(quick) { minutes ->
                        OutlinedButton(
                            onClick = { text = minutes.toString() },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                        ) {
                            Text("$minutes", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { text.toIntOrNull()?.let(onConfirm) },
                enabled = (text.toIntOrNull() ?: 0) > 0
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 手改消耗热量。 */
@Composable
fun BurnOverrideDialog(
    activityName: String,
    currentKcal: Double,
    estimatedKcal: Double,
    onConfirm: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(Math.round(currentKcal).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$activityName 消耗热量") },
        text = {
            Column {
                Text(
                    text = "按 MET 估算约为 ${Math.round(estimatedKcal)} kcal，" +
                        "你可以直接改成实测值。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { raw ->
                        text = raw.filter { ch -> ch.isDigit() || ch == '.' }.take(6)
                    },
                    label = { Text("热量") },
                    suffix = { Text("kcal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { text.toDoubleOrNull()?.let(onConfirm) },
                enabled = text.toDoubleOrNull() != null
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null) }) { Text("恢复估算") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}
