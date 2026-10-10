package com.example.diettracker.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diettracker.data.db.CustomSportEntity
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.domain.SportCategory
import com.example.diettracker.domain.SportInfo
import com.example.diettracker.domain.SportLibrary
import com.example.diettracker.ui.theme.AppColors
import com.example.diettracker.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * 运动项目清单（库 → 运动 → 运动）。
 *
 * 列出内置的 32 项运动，每项显示：默认时长 · MET 值 · **按你当前体重估算的消耗**。
 * 这里只是「参考资料」，真正添加运动是在今日页点「+」。
 */
@Composable
fun SportLibraryScreen(
    activityRepository: ActivityRepository
) {
    var bodyWeight by remember { mutableStateOf(70.0) }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CustomSportEntity?>(null) }
    var customSports by remember { mutableStateOf<List<CustomSportEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        bodyWeight = activityRepository.currentWeightKg()
        customSports = activityRepository.getCustomSports()
    }

    val groups = remember(customSports) {
        SportLibrary.groupedWithCustom(customSports)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screenHorizontal,
            end = Spacing.screenHorizontal,
            top = 4.dp,
            bottom = Spacing.listBottom
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        item(key = "hint") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.SportSoft.copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                    Text(
                        text = "运动热量怎么算",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Sport
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "消耗 ≈ MET × 体重(${trimNumber(bodyWeight)}kg) × 时长(小时)。" +
                            "下表是按默认时长估算的参考值，加入后可以自己改。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "add") {
            Button(
                onClick = { showAdd = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("新建运动")
            }
        }

        groups.forEach { (category, sports) ->
            if (sports.isEmpty()) return@forEach
            item(key = "g_${category.name}") {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (category == SportCategory.CUSTOM) {
                        AppColors.Stretch
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
            items(items = sports, key = { it.key }) { sport ->
                val isCustom = SportCategory.CUSTOM == sport.category
                SportInfoCard(
                    sport = sport,
                    bodyWeightKg = bodyWeight,
                    onDelete = if (isCustom) {
                        {
                            pendingDelete = customSports.firstOrNull { it.key == sport.key }
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }

    if (showAdd) {
        AddSportDialog(
            bodyWeightKg = bodyWeight,
            onDismiss = { showAdd = false },
            onConfirm = { name, met, minutes ->
                // 直接用仓库写，写完重新读一次列表（同页面内立刻可见）。
                // 这里不走 ViewModel：这个页面本身是库的一个子页，没有自己的 VM。
                kotlinx.coroutines.MainScope().launch {
                    activityRepository.addCustomSport(name, met, minutes)
                    customSports = activityRepository.getCustomSports()
                }
                showAdd = false
            }
        )
    }

    pendingDelete?.let { entity ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除「${entity.name}」？") },
            text = { Text("只会从运动库里移除；已经记录过的运动不受影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        kotlinx.coroutines.MainScope().launch {
                            activityRepository.deleteCustomSport(entity.id)
                            customSports = activityRepository.getCustomSports()
                        }
                        pendingDelete = null
                    }
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

/** 新建自建运动：名称 + MET + 默认时长。 */
@Composable
private fun AddSportDialog(
    bodyWeightKg: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var metText by remember { mutableStateOf("5.0") }
    var minutesText by remember { mutableStateOf("30") }

    val met = metText.toDoubleOrNull()
    val minutes = minutesText.toIntOrNull()
    val preview = if (met != null && minutes != null) {
        SportLibrary.estimateKcal(met, bodyWeightKg, minutes)
    } else {
        0.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建运动") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(12) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("运动名称") },
                    placeholder = { Text("例如：划船机") },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = metText,
                    onValueChange = { raw ->
                        metText = raw.filter { ch -> ch.isDigit() || ch == '.' }.take(4)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("MET（强度）") },
                    placeholder = { Text("5.0") },
                    singleLine = true,
                    supportingText = { Text("1.0-25.0，越大越累") }
                )
                Spacer(Modifier.height(6.dp))
                Text("常用档位", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                CustomSportEntity.MET_PRESETS.chunked(3).forEach { rowPresets ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        rowPresets.forEach { (value, label) ->
                            FilterChip(
                                selected = met == value,
                                onClick = { metText = value.toString() },
                                label = {
                                    Text(
                                        text = trimNumber(value),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
                Text(
                    text = CustomSportEntity.MET_PRESETS.joinToString("　") { (v, l) ->
                        "${trimNumber(v)} $l"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { raw ->
                        minutesText = raw.filter { it.isDigit() }.take(3)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("默认时长（分钟）") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "按你的体重 ${trimNumber(bodyWeightKg)}kg 估算：" +
                        "${trimNumber(minutes?.toDouble() ?: 0.0)} 分钟 ≈ " +
                        "${Math.round(preview)} kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Sport
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        met ?: 0.0,
                        minutes ?: 0
                    )
                },
                enabled = name.isNotBlank() &&
                    met != null && met in CustomSportEntity.MIN_MET..CustomSportEntity.MAX_MET &&
                    minutes != null && minutes > 0
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SportInfoCard(
    sport: SportInfo,
    bodyWeightKg: Double,
    onDelete: (() -> Unit)? = null
) {
    val accent = when {
        sport.strength -> AppColors.Strength
        sport.category == SportCategory.CUSTOM -> AppColors.Stretch
        else -> AppColors.Sport
    }
    val kcal = SportLibrary.estimateKcal(sport.met, bodyWeightKg, sport.defaultMinutes)

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
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(Spacing.itemGap))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sport.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (sport.strength) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "可加动作",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Strength
                        )
                    }
                    if (sport.category == SportCategory.CUSTOM) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "自建",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Stretch
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append("默认 ${sport.defaultMinutes} 分钟")
                        append(" · MET ${trimNumber(sport.met)}")
                        if (sport.note.isNotBlank() && sport.note != "自建") {
                            append(" · ${sport.note}")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${Math.round(kcal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
                Text(
                    text = "kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
