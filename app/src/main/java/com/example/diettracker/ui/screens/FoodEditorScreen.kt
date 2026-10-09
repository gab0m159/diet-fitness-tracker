package com.example.diettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.ui.components.ErrorBanner
import com.example.diettracker.ui.components.HintText
import com.example.diettracker.ui.components.NumericField
import com.example.diettracker.ui.viewmodel.FoodEditorViewModel

/**
 * Create / edit a food. `foodId == 0` means "new".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditorScreen(
    repository: DietRepository,
    foodId: Long,
    onDone: () -> Unit
) {
    val viewModel: FoodEditorViewModel = viewModel(
        key = "food_editor_$foodId",
        factory = FoodEditorViewModel.factory(repository, foodId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "编辑食物" else "新建食物") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("食物名称") },
                placeholder = { Text("例如：鸡蛋") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            HintText("名称不能和已有食物重复")

            Spacer(Modifier.height(20.dp))
            Text(
                text = "每 100 克的营养含量",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumericField(
                    value = state.carbs,
                    onValueChange = viewModel::onCarbsChange,
                    label = "碳水",
                    suffix = "g",
                    modifier = Modifier.weight(1f)
                )
                NumericField(
                    value = state.protein,
                    onValueChange = viewModel::onProteinChange,
                    label = "蛋白质",
                    suffix = "g",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            NumericField(
                value = state.fat,
                onValueChange = viewModel::onFatChange,
                label = "脂肪",
                suffix = "g / 100g",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = "通常一份有多大",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            NumericField(
                value = state.servingSize,
                onValueChange = viewModel::onServingChange,
                label = "一份大小",
                suffix = "g",
                modifier = Modifier.fillMaxWidth(),
                supportingText = "按份数记录时会用这个值换算成克数"
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                QuickServingChip("鸡蛋 50g") { viewModel.onServingChange("50") }
                QuickServingChip("米饭 200g") { viewModel.onServingChange("200") }
                QuickServingChip("牛奶 250g") { viewModel.onServingChange("250") }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("备注（可选）") },
                placeholder = { Text("品牌、做法、数据来源等") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = MaterialTheme.shapes.medium
            )

            state.servingPreview?.let { preview ->
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "一份（${state.servingSize} g）大约是",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PreviewValue("碳水", preview.carbs)
                            PreviewValue("蛋白质", preview.protein)
                            PreviewValue("脂肪", preview.fat)
                            PreviewValue("热量", preview.calories, unit = "kcal")
                        }
                    }
                }
            }

            state.error?.let { error ->
                Spacer(Modifier.height(16.dp))
                ErrorBanner(error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::save,
                enabled = !state.saving && state.loaded,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (state.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp).height(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (state.isEditing) "保存修改" else "创建食物")
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "小提示：按份数记录时用的是这里的「一份大小」，所以记得填得符合你的习惯。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PreviewValue(
    label: String,
    value: Double,
    unit: String = "g"
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = "${trim(value)} $unit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * A small tap-to-fill helper for common serving sizes.
 */
@Composable
private fun QuickServingChip(
    label: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
    )
}
