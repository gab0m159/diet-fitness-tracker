package com.example.diettracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diettracker.ui.theme.AppColors
import com.example.diettracker.ui.theme.MacroColors
import com.example.diettracker.ui.theme.Spacing
import java.time.LocalDate
import java.time.YearMonth

/** 月历里一天的汇总数据。 */
data class DayOverview(
    val date: String,
    /** 用户起的名字（「减脂日」），没名字时为空。 */
    val label: String = "",
    /** 当天饮食摄入热量。 */
    val intakeKcal: Double = 0.0,
    /** 当天目标热量。 */
    val goalKcal: Double = 0.0,
    /** 当天运动消耗。 */
    val burnedKcal: Double = 0.0,
    /** 当天记录了几项运动。 */
    val activityCount: Int = 0,
    /** 当天记录了几条饮食。 */
    val foodCount: Int = 0
) {
    val hasFood: Boolean get() = foodCount > 0
    val hasSport: Boolean get() = activityCount > 0
    val isEmpty: Boolean get() = !hasFood && !hasSport

    /** 是否超出目标热量。 */
    val isOver: Boolean get() = goalKcal > 0 && intakeKcal > goalKcal

    /** 是否接近目标（90%-100%）。 */
    val isNear: Boolean get() = goalKcal > 0 && intakeKcal >= goalKcal * 0.9 && !isOver
}

/**
 * 月历总览。
 *
 * 每个格子显示 **日期 + 名字 + 热量 + 训练标记**：
 *  - 名字是用户给这天起的（「减脂日」），没起就不显示；
 *  - 热量按达标情况染色：超出红、接近黄、正常绿；
 *  - 底部两个小点表示「有饮食」和「有运动」。
 *
 * 可以翻月看历史，也可以点任意一天跳过去。今天是实心圆底。
 */
@Composable
fun MonthCalendarDialog(
    month: YearMonth,
    today: String,
    selectedDate: String,
    overviews: Map<String, DayOverview>,
    onMonthChange: (YearMonth) -> Unit,
    onPickDate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上个月"
                    )
                }
                Text(
                    text = "${month.year} 年 ${month.monthValue} 月",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onMonthChange(month.plusMonths(1)) },
                    enabled = month < YearMonth.now().plusMonths(1)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下个月"
                    )
                }
            }
        },
        text = {
            Column {
                WeekdayHeader()
                Spacer(Modifier.height(4.dp))
                MonthGrid(
                    month = month,
                    today = today,
                    selectedDate = selectedDate,
                    overviews = overviews,
                    onPickDate = onPickDate
                )
                Spacer(Modifier.height(Spacing.itemGap))
                Legend()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: String,
    selectedDate: String,
    overviews: Map<String, DayOverview>,
    onPickDate: (String) -> Unit
) {
    val firstDay = month.atDay(1)
    // 周一为一周的第一天
    val leadingBlanks = (firstDay.dayOfWeek.value - 1)
    val daysInMonth = month.lengthOfMonth()
    val totalCells = leadingBlanks + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - leadingBlanks + 1
                    Box(modifier = Modifier.weight(1f)) {
                        if (dayNumber in 1..daysInMonth) {
                            val date = month.atDay(dayNumber)
                            val iso = date.toString()
                            DayCell(
                                dayNumber = dayNumber,
                                overview = overviews[iso],
                                isToday = iso == today,
                                isSelected = iso == selectedDate,
                                onClick = { onPickDate(iso) }
                            )
                        } else {
                            Spacer(Modifier.aspectRatio(0.78f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    overview: DayOverview?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val kcalColor = when {
        overview == null || !overview.hasFood -> MaterialTheme.colorScheme.onSurfaceVariant
        overview.isOver -> MaterialTheme.colorScheme.error
        overview.isNear -> MacroColors.Warning
        else -> MacroColors.Calories
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .padding(1.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 日期（今天用圆底强调）
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isToday) borderColor else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isToday) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Spacer(Modifier.height(2.dp))

        // 名字（最多一行，超出省略）
        if (overview != null && overview.label.isNotBlank()) {
            Text(
                text = overview.label,
                fontSize = 8.5.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.Stretch,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Spacer(Modifier.height(10.dp))
        }

        // 热量
        if (overview != null && overview.hasFood) {
            Text(
                text = Math.round(overview.intakeKcal).toString(),
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = kcalColor,
                maxLines = 1
            )
        } else {
            Spacer(Modifier.height(11.dp))
        }

        // 训练 / 饮食标记
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (overview?.hasSport == true) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(AppColors.Sport)
                )
            }
            if (overview?.hasFood == true) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MacroColors.Calories)
                )
            }
            if (overview == null || overview.isEmpty) {
                Spacer(Modifier.size(5.dp))
            }
        }
    }
}

@Composable
private fun Legend() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(MacroColors.Calories, "饮食正常")
            Spacer(Modifier.width(10.dp))
            LegendDot(MacroColors.Warning, "接近目标")
            Spacer(Modifier.width(10.dp))
            LegendDot(MaterialTheme.colorScheme.error, "超出目标")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(AppColors.Sport, "有运动")
            Spacer(Modifier.width(10.dp))
            Text(
                text = "数字是当天摄入热量；点任意一天可以跳过去",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 把 ISO 日期转成 YearMonth。 */
fun isoToYearMonth(iso: String): YearMonth =
    runCatching { YearMonth.from(LocalDate.parse(iso)) }.getOrDefault(YearMonth.now())
