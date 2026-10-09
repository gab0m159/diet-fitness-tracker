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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.diettracker.data.model.BrandTags
import com.example.diettracker.data.model.FoodCategory
import com.example.diettracker.data.model.FoodCredibility
import com.example.diettracker.data.model.NutritionTags
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.ui.theme.MacroColors
import com.example.diettracker.ui.viewmodel.FoodBrowseViewModel

/**
 * Brand and recommended foods, with tag filters.
 *
 * Searching "麦当劳" lists that brand's items; searching "优质碳水" lists every
 * food carrying that tag — both work because the DAO matches name, brand and
 * tags in one query.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodBrowseScreen(
    repository: DietRepository,
    initialCategory: FoodCategory,
    onBack: () -> Unit
) {
    val viewModel: FoodBrowseViewModel = viewModel(
        factory = FoodBrowseViewModel.factory(repository, initialCategory)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("食品库") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索名称、品牌（如 麦当劳）或标签（如 优质碳水）") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            // Category tabs.
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(FoodCategory.BRAND, FoodCategory.RECOMMENDED).forEach { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { viewModel.onCategoryChange(category) },
                        label = { Text(category.label) }
                    )
                }
            }

            // Brand filter chips.
            if (state.category == FoodCategory.BRAND && state.brands.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = state.brandFilter == null,
                        onClick = { viewModel.onBrandFilterChange(null) },
                        label = { Text("全部") }
                    )
                    BrandTags.all.filter { state.brands.contains(it) }.forEach { brand ->
                        FilterChip(
                            selected = state.brandFilter == brand,
                            onClick = { viewModel.onBrandFilterChange(brand) },
                            label = { Text(brand) }
                        )
                    }
                }
            }

            // Nutrition tag filter chips.
            if (state.category == FoodCategory.RECOMMENDED) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = state.tagFilter == null,
                        onClick = { viewModel.onTagFilterChange(null) },
                        label = { Text("全部") }
                    )
                    NutritionTags.all.forEach { tag ->
                        FilterChip(
                            selected = state.tagFilter == tag,
                            onClick = { viewModel.onTagFilterChange(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (state.foods.isEmpty() && !state.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "没有匹配的食品",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = state.foods, key = { it.id }) { food ->
                        FoodTagCard(food = food)
                    }
                }
            }
        }
    }
}

/** Card showing nutrition, tags, source and the plain-text daily advice. */
@Composable
private fun FoodTagCard(food: FoodEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (food.brandLabel.isNotBlank()) {
                    Box {
                        AssistChip(
                            onClick = {},
                            label = { Text(food.brandLabel) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "每 100g：${round1(food.kcalPer100g)} kcal · " +
                    "碳 ${round1(food.carbsPer100g)}g · " +
                    "蛋 ${round1(food.proteinPer100g)}g · " +
                    "脂 ${round1(food.fatPer100g)}g",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "一份 ≈ ${round1(food.servingSizeGrams)} g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (food.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    food.tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (tag) {
                                        NutritionTags.QUALITY_CARBS -> MacroColors.Carbs
                                        NutritionTags.QUALITY_PROTEIN -> MacroColors.Protein
                                        else -> MacroColors.Fat
                                    }
                                )
                            }
                        )
                    }
                }
            }

            if (food.dailyRecommendation.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "建议摄入：${food.dailyRecommendation}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(6.dp))
            val credibility = FoodCredibility.fromStorage(food.credibility)
            Text(
                text = buildString {
                    append("数据来源：${food.dataSource.ifBlank { "用户录入" }}")
                    append("（${credibility.label}）")
                    if (credibility == FoodCredibility.ESTIMATED) {
                        append(" · 仅供参考")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            if (food.note.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = food.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun round1(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
    else rounded.toString()
}
