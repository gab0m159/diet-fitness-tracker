package com.example.diettracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diettracker.data.model.DailyEnergy
import com.example.diettracker.domain.NutritionCalculator
import com.example.diettracker.domain.NutritionCalculator.ProgressBand
import com.example.diettracker.ui.theme.MacroColors

/**
 * Colours for the three progress bands.
 *
 * The band is decided by [NutritionCalculator.progressBand] so the thresholds
 * live in one place: >100% over, 90-100% near, below that on track.
 */
object ProgressColors {
    val OnTrack = Color(0xFF2E7D32) // green
    val Near = Color(0xFFF2A93B)    // amber
    val Over = Color(0xFFD32F2F)    // red
    val Neutral = Color(0xFF9E9E9E)

    fun forBand(band: ProgressBand): Color = when (band) {
        ProgressBand.ON_TRACK -> OnTrack
        ProgressBand.NEAR -> Near
        ProgressBand.OVER -> Over
        ProgressBand.NO_GOAL -> Neutral
    }
}

/**
 * 首页的「当天摄入」卡片。
 *
 * 内容（用户要求精简后）：
 *  - 三条宏量进度条：碳水 / 蛋白质 / 脂肪
 *  - 三个数字：饮食摄入 / 运动消耗 / 总热量
 *
 * 去掉了大圆环与图例（太占地方）。运动消耗**只作参考**，永远不会减掉可摄入额度。
 */
@Composable
fun IntakeSummaryCard(
    state: com.example.diettracker.ui.viewmodel.DiaryUiState,
    onOpenGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "当天摄入",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "目标 ${Math.round(state.goalCalories)} kcal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenGoals) { Text("修改目标") }
            }

            Spacer(Modifier.height(12.dp))

            MacroProgressBar(
                label = "碳水",
                consumed = state.consumed.carbs,
                goal = state.goal.carbs,
                color = MacroColors.Carbs
            )
            Spacer(Modifier.height(10.dp))
            MacroProgressBar(
                label = "蛋白质",
                consumed = state.consumed.protein,
                goal = state.goal.protein,
                color = MacroColors.Protein
            )
            Spacer(Modifier.height(10.dp))
            MacroProgressBar(
                label = "脂肪",
                consumed = state.consumed.fat,
                goal = state.goal.fat,
                color = MacroColors.Fat
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IntakeMetric(
                    label = "饮食摄入",
                    value = Math.round(state.energy.intakeCalories).toString(),
                    color = MacroColors.Calories
                )
                IntakeMetric(
                    label = "运动消耗",
                    value = Math.round(state.energy.burnedCalories).toString(),
                    color = com.example.diettracker.ui.theme.AppColors.Sport
                )
                IntakeMetric(
                    label = "总热量",
                    value = Math.round(state.energy.netCalories).toString(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** 三个数字之一：标签 + 大号数值 + kcal。 */
@Composable
private fun IntakeMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = "kcal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 「饮食摄入 / 运动消耗 / 净热量」三格。
 *
 * 从摄入卡片里挪出来，现在放在「今日运动」栏里：消耗与净热量跟运动放一起更合理。
 * 运动消耗**只作参考**，永远不会减掉可摄入额度。
 */
@Composable
fun EnergyLedgerRow(
    energy: DailyEnergy,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EnergyValue(
            label = "饮食摄入",
            value = Math.round(energy.intakeCalories).toString(),
            color = MaterialTheme.colorScheme.onSurface
        )
        EnergyValue(
            label = "运动消耗",
            value = Math.round(energy.burnedCalories).toString(),
            color = ProgressColors.OnTrack
        )
        EnergyValue(
            label = "净热量",
            value = Math.round(energy.netCalories).toString(),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** "剩 120g" / "超 30g" chip for one macro. */
@Composable
private fun RemainingChip(
    label: String,
    remaining: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val over = remaining < 0
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (over) "超 ${Math.round(-remaining)}g" else "剩 ${Math.round(remaining)}g",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (over) MaterialTheme.colorScheme.error else color
            )
        }
    }
}

/**
 * A horizontal progress bar whose colour reflects how close the metric is to
 * its goal: green, amber near the target, red once over.
 *
 * [color] is used only when the metric is still on track, so a macro can keep its
 * own identity colour until it needs to warn.
 */
@Composable
fun MacroProgressBar(
    label: String,
    consumed: Double,
    goal: Double,
    color: Color,
    modifier: Modifier = Modifier,
    unit: String = "g"
) {
    val band = NutritionCalculator.progressBand(consumed, goal)
    val animatedFraction by animateFloatAsState(
        targetValue = NutritionCalculator.progressFraction(consumed, goal),
        animationSpec = tween(durationMillis = 600),
        label = "macroProgress"
    )
    val barColor by animateColorAsState(
        targetValue = when (band) {
            ProgressBand.ON_TRACK -> color
            else -> ProgressColors.forBand(band)
        },
        animationSpec = tween(durationMillis = 400),
        label = "macroColor"
    )
    val percent = if (goal > 0.0) consumed / goal * 100.0 else 0.0
    val bandLabel = when (band) {
        ProgressBand.OVER -> "已超出"
        ProgressBand.NEAR -> "接近目标"
        ProgressBand.ON_TRACK -> "进行中"
        ProgressBand.NO_GOAL -> "未设置"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (band == ProgressBand.NEAR || band == ProgressBand.OVER) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = bandLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ProgressColors.forBand(band)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${round(consumed)} / ${round(goal)} $unit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (goal > 0.0) "${round(percent)}%" else "—",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = barColor
            )
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics {
                    contentDescription =
                        "$label 已完成 ${round(consumed)}$unit，目标 ${round(goal)}$unit，$bandLabel"
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * A single energy figure: label, big value and unit.
 */
@Composable
private fun EnergyValue(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = "kcal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun round(value: Double): String = Math.round(value).toString()

/** A legend row like "碳水 120 / 250 g" with a colour swatch. */
@Composable
fun MacroLegendRow(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Small grey caption used for hint text under form fields. */
@Composable
fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 4.dp)
    )
}
