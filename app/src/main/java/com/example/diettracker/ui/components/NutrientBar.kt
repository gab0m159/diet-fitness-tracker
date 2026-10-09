package com.example.diettracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diettracker.ui.theme.MacroColors
import com.example.diettracker.ui.theme.Spacing

/**
 * 一根营养进度条。
 *
 * 配色规则（产品要求）：
 *  - 未超标 → 该指标的固定色（碳橙 / 蛋蓝 / 脂粉 / 热量绿）
 *  - 达到目标 90%-100% → 黄色（接近目标）
 *  - 超出 100% → 红色
 */
@Composable
fun NutrientBar(
    label: String,
    current: Double,
    target: Double,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val ratio = if (target > 0) (current / target).toFloat() else 0f
    val barColor = when {
        target <= 0 -> accent
        ratio > 1.0f -> MaterialTheme.colorScheme.error
        ratio >= 0.9f -> MacroColors.Warning
        else -> accent
    }
    val percent = (ratio * 100).toInt()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${fmt(current)} / ${fmt(target)} $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = barColor
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(barColor.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = ratio.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * 「饮食摄入 / 运动消耗 / 净热量」三格。
 *
 * 刻意把运动消耗标成蓝色、净热量标成主色，避免和饮食摄入混在一起。
 */
@Composable
fun EnergySummaryRow(
    intakeKcal: Double,
    burnedKcal: Double,
    netKcal: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        EnergyCell(
            label = "饮食摄入",
            value = intakeKcal,
            color = MacroColors.Calories,
            modifier = Modifier.weight(1f)
        )
        EnergyCell(
            label = "运动消耗",
            value = burnedKcal,
            color = com.example.diettracker.ui.theme.AppColors.Sport,
            modifier = Modifier.weight(1f)
        )
        EnergyCell(
            label = "净热量",
            value = netKcal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EnergyCell(
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
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
            text = "${Math.round(value)}",
            style = MaterialTheme.typography.titleLarge,
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

/** 去掉无意义的小数尾巴。 */
internal fun fmt(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
    else rounded.toString()
}
