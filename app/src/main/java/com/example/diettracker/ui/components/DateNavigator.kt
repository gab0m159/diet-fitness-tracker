package com.example.diettracker.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.diettracker.ui.theme.AppColors
import com.example.diettracker.util.DateUtils

/**
 * 日记页顶部的日期条：前一天 / 后一天、可点的日期（打开月历）、回到今天。
 *
 * [dayLabel] 是用户给这天起的名字（「减脂日」），有名字时显示在日期下方；
 * [onEditLabel] 让用户给这天起名或改名（点名字区域触发）。
 */
@Composable
fun DateNavigator(
    date: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
    dayLabel: String = "",
    onEditLabel: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "前一天"
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onPickDate)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = DateUtils.friendlyLabel(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${DateUtils.displayDate(date)} · ${DateUtils.weekday(date)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 名字按钮：有名字显示名字，没名字显示「起名」提示
            TextButton(
                onClick = onEditLabel,
                modifier = Modifier.widthIn(min = 62.dp)
            ) {
                Text(
                    text = dayLabel.ifBlank { "起名" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (dayLabel.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (dayLabel.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        AppColors.Stretch
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (DateUtils.isToday(date)) {
                // 与下面的按钮同宽，保证日期居中。
                Box(Modifier.width(52.dp)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            } else {
                TextButton(onClick = onToday, modifier = Modifier.width(52.dp)) {
                    Text("今天", style = MaterialTheme.typography.labelMedium)
                }
            }

            IconButton(onClick = onNextDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "后一天"
                )
            }
        }
    }
}

/** Thin spacer helper so screens read consistently. */
@Composable
fun Gap(height: Int) {
    Spacer(Modifier.height(height.dp))
}
