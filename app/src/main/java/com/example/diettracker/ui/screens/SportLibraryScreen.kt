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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.domain.SportInfo
import com.example.diettracker.domain.SportLibrary
import com.example.diettracker.ui.theme.AppColors
import com.example.diettracker.ui.theme.Spacing

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
    LaunchedEffect(Unit) { bodyWeight = activityRepository.currentWeightKg() }

    val groups = remember { SportLibrary.grouped() }

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
                            "下表是按默认时长估算的参考值，实际消耗因人而异，加入后可以自己改。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        groups.forEach { (category, sports) ->
            if (sports.isEmpty()) return@forEach
            item(key = "g_${category.name}") {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
            items(items = sports, key = { it.key }) { sport ->
                SportInfoCard(sport = sport, bodyWeightKg = bodyWeight)
            }
        }
    }
}

@Composable
private fun SportInfoCard(sport: SportInfo, bodyWeightKg: Double) {
    val accent = if (sport.strength) AppColors.Strength else AppColors.Sport
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
                .padding(Spacing.cardPadding),
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
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append("默认 ${sport.defaultMinutes} 分钟")
                        append(" · MET ${trimNumber(sport.met)}")
                        if (sport.note.isNotBlank()) append(" · ${sport.note}")
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
        }
    }
}

private fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
