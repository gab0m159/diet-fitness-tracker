package com.example.diettracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.repository.ActivityRepository
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.ui.theme.Spacing
import com.example.diettracker.ui.viewmodel.LibraryViewModel

/** 库页的一级菜单。 */
private val topLevelSegments = listOf("运动", "食物")

/** 「运动」下面的二级菜单。 */
private val sportSegments = listOf("运动", "拉伸")

/**
 * 「库」这一栏。
 *
 * 一级菜单只有两项：**运动** 和 **食物**。运动下面再分 **运动**（项目清单）与
 * **拉伸**（按肌群分组的拉伸指导）。
 *
 * 一级 / 二级都用分段控件，不做嵌套导航——底部栏始终可见，切换也不会丢状态。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    dietRepository: DietRepository,
    activityRepository: ActivityRepository,
    onCreateFood: () -> Unit,
    onEditFood: (Long) -> Unit,
    onBrowseBundled: (FoodCategory) -> Unit,
    /** 从别处跳进来时直接落在拉伸上（保留参数，方便以后加「练后拉伸」入口）。 */
    initialStretch: String? = null
) {
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.factory(activityRepository)
    )

    var topLevel by remember { mutableIntStateOf(0) }
    var sportLevel by remember(initialStretch) {
        mutableIntStateOf(if (initialStretch.isNullOrBlank()) 0 else 1)
    }
    var highlightStretch by remember(initialStretch) { mutableStateOf(initialStretch) }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = 8.dp)
        ) {
            topLevelSegments.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = topLevel == index,
                    onClick = { topLevel = index },
                    shape = SegmentedButtonDefaults.itemShape(index, topLevelSegments.size),
                    label = { Text(label, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }

        if (topLevel == 0) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal)
                    .padding(bottom = 6.dp)
            ) {
                sportSegments.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = sportLevel == index,
                        onClick = {
                            sportLevel = index
                            if (index != 1) highlightStretch = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, sportSegments.size),
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            if (sportLevel == 0) {
                SportLibraryScreen(activityRepository = activityRepository)
            } else {
                StretchLibraryScreen(
                    viewModel = viewModel,
                    highlightStretch = highlightStretch
                )
            }
        } else {
            FoodLibraryScreen(
                repository = dietRepository,
                onCreateFood = onCreateFood,
                onEditFood = onEditFood,
                onBrowseBundled = onBrowseBundled
            )
        }
    }
}
