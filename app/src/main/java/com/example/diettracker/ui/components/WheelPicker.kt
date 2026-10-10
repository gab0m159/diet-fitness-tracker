package com.example.diettracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diettracker.ui.theme.AppColors
import com.example.diettracker.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 滚轮选择器（不用 LazyColumn）。
 *
 * ## 为什么自己画
 *
 * 之前用 `LazyColumn + snapFlingBehavior`，选中项靠「滚到第 N 项 + 偏移」再反推
 * 中心项，结果**一直往下一格偏**：列表上下 padding 与 `firstVisibleItemIndex`
 * 的语义对不齐，改了两次公式都没根治。
 *
 * 现在改成「固定行高 + 手指拖拽 + 自己维护一个浮点偏移量」：
 *
 *  - `offsetRows` 是以「行」为单位的滚动量（0 = 第 0 项正好在中间）；
 *  - 手指每拖 1 行的高度，`offsetRows` 就加 1；
 *  - 松手后按四舍五入吸附到整数行，并回调 [onSelect]。
 *
 * 位置由我自己算，不再依赖任何列表的索引语义，所以不可能再错位。
 */
@Composable
fun WheelPicker(
    items: List<Any>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    visibleCount: Int = 3,
    format: (Any) -> String = { it.toString() }
) {
    if (items.isEmpty()) return

    val rowHeight = 40.dp
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    val half = visibleCount / 2

    // 当前偏移（单位：行）。0 表示第 0 项在中间。
    var offsetRows by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 松手后平滑吸附到最近一格（弹簧动画），而不是瞬间跳过去。
    fun snapTo(target: Int) {
        val start = offsetRows
        scope.launch {
            Animatable(start).animateTo(
                targetValue = target.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) {
                offsetRows = value
            }
        }
    }

    // 外部值变化时把滚轮移过去（用户拖动期间不打扰）。
    LaunchedEffect(selectedIndex, items.size) {
        if (!dragging) {
            val target = selectedIndex.coerceIn(0, items.lastIndex)
            if (kotlin.math.abs(offsetRows - target) > 0.01f) snapTo(target)
        }
    }

    val dragState = rememberDraggableState { delta ->
        // 手指向上拖（delta < 0）→ 内容向上走 → 偏移增大。
        val next = offsetRows - delta / rowPx
        offsetRows = next.coerceIn(0f, items.lastIndex.toFloat())
    }

    // 逐行的浮点位移：让内容跟着手指连续滚动，而不是整数行跳变。
    // offsetRows 的小数部分决定整体平移多少像素。
    val fractional = offsetRows - offsetRows.roundToInt()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight * visibleCount)
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = { dragging = true },
                onDragStopped = {
                    dragging = false
                    val snapped = offsetRows.roundToInt().coerceIn(0, items.lastIndex)
                    snapTo(snapped)
                    if (snapped != selectedIndex) onSelect(snapped)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // 中间高亮条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.09f))
        )

        // 多画一行（half*2+1 行），再用整数像素的垂直偏移整体平移，
        // 这样滚动是连续的，不会一格一格跳。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = -fractional * rowPx },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val center = offsetRows.roundToInt()
            for (slot in -half..half) {
                val itemIndex = center + slot
                val rowDistance = kotlin.math.abs(itemIndex - offsetRows)

                if (itemIndex !in items.indices) {
                    Spacer(Modifier.height(rowHeight))
                    continue
                }

                val focused = rowDistance < 0.5f
                // 透明度按距离连续插值：越靠中间越清晰，拖动时不会突变。
                val alpha = (1f - (rowDistance / (half + 1)).coerceIn(0f, 1f))
                    .coerceIn(0.25f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(items[itemIndex]),
                        // 字号同样做插值：中心 17sp，两侧缩小，滚动看起来是渐变的。
                        fontSize = (17f - rowDistance * 2.2f).coerceIn(12f, 17f).sp,
                        fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (focused) {
                            accent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 三个滚轮：**重量 / 组数 / 次数**。
 *
 * 按产品要求就是三个值各选一个，不支持「每组次数不一样」。
 * 重量 0-300kg（2.5kg 一档）、组数 1-20、次数 1-100。
 */
@Composable
fun TripleWheelPicker(
    weightKg: Double,
    sets: Int,
    reps: Int,
    accent: Color,
    onWeightChange: (Double) -> Unit,
    onSetsChange: (Int) -> Unit,
    onRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val weights = remember { (0..120).map { it * 2.5 } }
    val setOptions = remember { (1..20).toList() }
    val repOptions = remember { (1..100).toList() }

    val weightIndex = weights.indexOfFirst { kotlin.math.abs(it - weightKg) < 0.01 }
        .takeIf { it >= 0 } ?: 0
    val setIndex = (sets - 1).coerceIn(0, setOptions.lastIndex)
    val repIndex = (reps - 1).coerceIn(0, repOptions.lastIndex)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        WheelColumn(label = "重量 (kg)", modifier = Modifier.weight(1.15f)) {
            WheelPicker(
                items = weights,
                selectedIndex = weightIndex,
                onSelect = { onWeightChange(weights[it]) },
                accent = accent,
                format = { fmtNumber(it as Double) }
            )
        }
        WheelColumn(label = "组数", modifier = Modifier.weight(1f)) {
            WheelPicker(
                items = setOptions,
                selectedIndex = setIndex,
                onSelect = { onSetsChange(setOptions[it]) },
                accent = accent
            )
        }
        WheelColumn(label = "次数", modifier = Modifier.weight(1f)) {
            WheelPicker(
                items = repOptions,
                selectedIndex = repIndex,
                onSelect = { onRepsChange(repOptions[it]) },
                accent = accent
            )
        }
    }
}

@Composable
private fun WheelColumn(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

/** 修改一个动作卡片：动作名 + 三个滚轮。 */
@Composable
fun ExerciseEditorDialog(
    exerciseName: String,
    initialWeight: Double,
    initialSets: Int,
    initialReps: Int,
    onConfirm: (Double, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var weight by remember { mutableStateOf(initialWeight) }
    var sets by remember { mutableIntStateOf(initialSets) }
    var reps by remember { mutableIntStateOf(initialReps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            Column {
                Text(
                    text = "上下滑动滚轮选重量、组数和次数。重量按 2.5kg 递增。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.itemGap))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.Strength.copy(alpha = 0.08f))
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${fmtNumber(weight)} kg × $sets 组 × $reps 次",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Strength
                    )
                }
                Spacer(Modifier.height(Spacing.itemGap))
                TripleWheelPicker(
                    weightKg = weight,
                    sets = sets,
                    reps = reps,
                    accent = AppColors.Strength,
                    onWeightChange = { weight = it },
                    onSetsChange = { sets = it },
                    onRepsChange = { reps = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(weight, sets, reps) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 去掉小数尾巴：60.0 → "60"，62.5 → "62.5"。 */
internal fun fmtNumber(value: Double): String {
    val rounded = Math.round(value * 100.0) / 100.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
    else rounded.toString()
}
