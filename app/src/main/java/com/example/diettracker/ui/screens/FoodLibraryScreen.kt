package com.example.diettracker.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.db.FoodVariantEntity
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.ui.components.ConfirmDialog
import com.example.diettracker.ui.viewmodel.FoodLibraryViewModel

/**
 * The food library: search, create, edit, delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLibraryScreen(
    repository: DietRepository,
    onCreateFood: () -> Unit,
    onEditFood: (Long) -> Unit,
    onBrowseBundled: (FoodCategory) -> Unit
) {
    val viewModel: FoodLibraryViewModel = viewModel(
        factory = FoodLibraryViewModel.factory(repository)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingDelete by remember { mutableStateOf<FoodEntity?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("食物库") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateFood) {
                Icon(Icons.Filled.Add, contentDescription = "新建食物")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Entry points to the bundled libraries.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onBrowseBundled(FoodCategory.BRAND) },
                    label = { Text("品牌食品") }
                )
                AssistChip(
                    onClick = { onBrowseBundled(FoodCategory.RECOMMENDED) },
                    label = { Text("推荐食品") }
                )
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索食物名称、品牌或标签") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            if (state.foods.isEmpty() && !state.loading) {
                EmptyLibrary(query = state.query)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = state.foods, key = { it.id }) { food ->
                        FoodCard(
                            food = food,
                            variants = state.variantsByFood[food.id].orEmpty(),
                            onEdit = { onEditFood(food.id) },
                            onDelete = { pendingDelete = food }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { food ->
        ConfirmDialog(
            title = "删除「${food.name}」？",
            message = "如果当天记录里用过这个食物，相关记录也会一并删除，此操作不可撤销。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteFood(food)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun FoodCard(
    food: FoodEntity,
    variants: List<FoodVariantEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                if (food.hasVariants) {
                    // Branded food: nutrition is published per serving, so show
                    // that plus the available sizes instead of per-100g numbers.
                    val first = variants.firstOrNull()
                    if (first != null) {
                        Text(
                            text = "每份：${trim(first.kcal)} kcal · " +
                                "碳 ${trim(first.carbsG)}g · " +
                                "蛋 ${trim(first.proteinG)}g · " +
                                "脂 ${trim(first.fatG)}g",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (variants.size > 1) {
                                "可选规格：" + variants.joinToString(" / ") { it.displaySpec }
                            } else {
                                "规格：${first.displaySpec}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = "每 100g：碳 ${trim(food.carbsPer100g)}g · " +
                            "蛋 ${trim(food.proteinPer100g)}g · " +
                            "脂 ${trim(food.fatPer100g)}g",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "一份 ≈ ${trim(food.servingSizeGrams)} g",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (food.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = food.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (food.brandLabel.isNotBlank() || food.sourceNote.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            if (food.brandLabel.isNotBlank()) {
                                append(food.brandLabel)
                            }
                            if (food.sourceNote.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(food.sourceNote)
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (query.isBlank()) "食物库还是空的" else "没有找到「$query」",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (query.isBlank()) "点右下角 + 新建食物" else "换个关键词试试，或新建这个食物",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal fun trim(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString() else rounded.toString()
}
