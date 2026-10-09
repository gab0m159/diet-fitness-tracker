package com.example.diettracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.diettracker.ui.theme.AppColors
import com.example.diettracker.ui.theme.Spacing

/**
 * 一个滑轮（滚轮选择器）。
 *
 * 实现方式：`LazyColumn` + `snapFlingBehavior`，中间那一条高亮。刻意不引第三方
 * 滚轮库，保持依赖干净。
 *
 * @param items 候选值。
 * @param selectedIndex 当前选中下标。
 * @param onSelect 选中变化回调。
 * @param format 显示格式化，例如把 60.0 显示成 "60"。
 */
@OptIn(ExperimentalFoundationApi::class)
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
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 0)

    // 把选中项滚到中间：可见 3 条时，中间那一条的下标就是 firstVisibleItemIndex。
    val targetFirst = (safeIndex - visibleCount / 2).coerceAtLeast(0)
    LaunchedEffect(safeIndex, items.size) {
        runCatching { listState.animateScrollToItem(targetFirst) }
    }

    // 滚动停止后，把「停在中间的那一项」回传给调用方。
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                val center = listState.firstVisibleItemIndex + visibleCount / 2
                val clamped = center.coerceIn(0, items.lastIndex)
                if (clamped != safeIndex) onSelect(clamped)
            }
        }
    }

    val rowHeight = 38.dp
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                state = listState,
                flingBehavior = fling,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight * visibleCount),
                contentPadding = PaddingValues(vertical = rowHeight),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(items.size) { index ->
                    val distance = kotlin.math.abs(index - safeIndex)
                    Text(
                        text = format(items[index]),
                        style = if (distance == 0) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = when (distance) {
                            0 -> accent
                            1 -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .padding(top = 7.dp)
                    )
                }
            }
        }
        // 中间高亮条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.08f))
        )
    }
}

/**
 * 三个滑轮：**重量 / 组数 / 次数**。
 *
 * 产品要求：不要「每组次数不一样」那种输入，就是三个值各选一个。
 * 重量按 2.5kg 递增；组数 1-20；次数 1-100。
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
    val weights = remember { (0..120).map { it * 2.5 } }        // 0 - 300kg
    val setOptions = remember { (1..20).toList() }               // 1 - 20 组
    val repOptions = remember { (1..100).toList() }              // 1 - 100 次

    val weightIndex = weights.indexOfFirst { kotlin.math.abs(it - weightKg) < 0.01 }
        .takeIf { it >= 0 } ?: 0
    val setIndex = (sets - 1).coerceIn(0, setOptions.lastIndex)
    val repIndex = (reps - 1).coerceIn(0, repOptions.lastIndex)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        WheelColumn(
            label = "重量 (kg)",
            modifier = Modifier.weight(1.15f)
        ) {
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

/** 修改一个动作卡片：动作名 + 三个滑轮。 */
@Composable
fun ExerciseEditorDialog(
    exerciseName: String,
    initialWeight: Double,
    initialSets: Int,
    initialReps: Int,
    onRename: ((String) -> Unit)? = null,
    onConfirm: (Double, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var weight by remember { androidx.compose.runtime.mutableStateOf(initialWeight) }
    var sets by remember { mutableIntStateOf(initialSets) }
    var reps by remember { mutableIntStateOf(initialReps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            Column {
                Text(
                    text = "选重量、组数和次数就行。重量按 2.5kg 递增。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.itemGap))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.Strength.copy(alpha = 0.08f))
                        .padding(vertical = 8.dp),
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
