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
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.data.repository.TrainingRepository
import com.example.diettracker.ui.viewmodel.LibraryViewModel

/** The three sections of the "库" tab. */
private val segments = listOf("训练动作", "拉伸", "食物")

/**
 * The "库" tab: one segmented control switching between the exercise library, the
 * stretch library and the food library.
 *
 * The screens are composed directly rather than nested in their own NavHost, so
 * the bottom bar stays visible and the selected segment survives recomposition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    dietRepository: DietRepository,
    trainingRepository: TrainingRepository,
    onCreateFood: () -> Unit,
    onEditFood: (Long) -> Unit,
    onBrowseBundled: (FoodCategory) -> Unit,
    /** Set when navigated here from an exercise's "拉伸 ↗" button. */
    initialStretch: String? = null
) {
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.factory(trainingRepository)
    )

    var selected by remember(initialStretch) {
        mutableIntStateOf(if (initialStretch.isNullOrBlank()) 0 else 1)
    }
    var highlightStretch by remember(initialStretch) { mutableStateOf(initialStretch) }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            segments.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selected == index,
                    onClick = {
                        selected = index
                        // Clear the highlight once the user navigates manually.
                        if (index != 1) highlightStretch = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, segments.size),
                    label = { Text(label, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }

        when (selected) {
            0 -> ExerciseLibraryScreen(
                viewModel = viewModel,
                onOpenStretch = { stretchName ->
                    highlightStretch = stretchName.ifBlank { null }
                    selected = 1
                }
            )

            1 -> StretchLibraryScreen(
                viewModel = viewModel,
                highlightStretch = highlightStretch
            )

            else -> FoodLibraryScreen(
                repository = dietRepository,
                onCreateFood = onCreateFood,
                onEditFood = onEditFood,
                onBrowseBundled = onBrowseBundled
            )
        }
    }
}
