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
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diettracker.data.db.FoodEntity
import com.example.diettracker.data.db.FoodVariantEntity
import com.example.diettracker.data.model.AmountMode
import com.example.diettracker.data.model.FoodCredibility
import com.example.diettracker.data.model.MealType
import com.example.diettracker.ui.components.NumericField
import com.example.diettracker.ui.components.SourceNote
import com.example.diettracker.ui.components.VariantNutritionRow
import com.example.diettracker.ui.components.VariantSelector
import com.example.diettracker.ui.viewmodel.DiaryViewModel
import com.example.diettracker.util.DateUtils
import kotlinx.coroutines.launch

/**
 * Pick a food and log it against a day. Tapping a food opens a bottom sheet where
 * the amount can be entered either in grams or in servings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    viewModel: DiaryViewModel,
    initialDate: String,
    onDone: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val variantsByFood by viewModel.variantsByFood.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<FoodEntity?>(null) }
    var selectedVariants by remember { mutableStateOf<List<FoodVariantEntity>>(emptyList()) }
    var showEmptyHint by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialDate) {
        if (initialDate.isNotBlank()) viewModel.setDate(initialDate)
    }

    /** Loads a food's sizes before opening the amount sheet. */
    fun openFood(food: FoodEntity) {
        selectedFood = food
        scope.launch {
            selectedVariants = viewModel.variantsFor(food.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加到 ${DateUtils.friendlyLabel(state.date)}") },
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
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onFoodQueryChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索食物名称") },
                leadingIcon = {
                    Icon(Icons.Filled.RestaurantMenu, contentDescription = null)
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            Text(
                text = "点一个食物，然后选「按克数」或「按份数」",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (foods.isEmpty()) {
                EmptyPicker(onExplain = { showEmptyHint = true })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = foods, key = { it.id }) { food ->
                        FoodPickRow(
                            food = food,
                            variants = variantsByFood[food.id].orEmpty(),
                            onClick = { openFood(food) }
                        )
                    }
                }
            }
        }
    }

    selectedFood?.let { food ->
        ModalBottomSheet(
            onDismissRequest = { selectedFood = null },
            sheetState = sheetState
        ) {
            AddEntrySheetContent(
                food = food,
                variants = selectedVariants,
                onCancel = { selectedFood = null },
                onConfirm = { amount, mode, meal, variantId ->
                    viewModel.addEntry(food.id, amount, mode, meal, variantId)
                    selectedFood = null
                    onDone()
                }
            )
        }
    }

    if (showEmptyHint) {
        AlertDialog(
            onDismissRequest = { showEmptyHint = false },
            title = { Text("没有可选的食物") },
            text = { Text("请先切到「食物库」标签页创建一个食物，再回来记录。") },
            confirmButton = {
                TextButton(onClick = { showEmptyHint = false }) { Text("知道了") }
            }
        )
    }
}

@Composable
private fun FoodPickRow(
    food: FoodEntity,
    variants: List<FoodVariantEntity>,
    onClick: () -> Unit
) {
    val first = variants.firstOrNull()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                if (food.hasVariants && first != null) {
                    // Branded food: published per serving, no weight given.
                    Text(
                        text = "每份：${trim(first.kcal)} kcal · " +
                            "碳 ${trim(first.carbsG)}g · " +
                            "蛋 ${trim(first.proteinG)}g · " +
                            "脂 ${trim(first.fatG)}g",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "每 100g：碳 ${trim(food.carbsPer100g)}g · " +
                            "蛋 ${trim(food.proteinPer100g)}g · " +
                            "脂 ${trim(food.fatPer100g)}g",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (food.hasVariants && variants.size > 1) {
                    "${variants.size} 种规格"
                } else if (food.hasVariants) {
                    first?.displaySpec.orEmpty()
                } else {
                    "一份 ${trim(food.servingSizeGrams)}g"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyPicker(onExplain: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.RestaurantMenu,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "食物库里没有匹配的食物",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "先创建一个食物，再回来记录",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onExplain) { Text("怎么办？") }
    }
}

/**
 * The amount editor sheet.
 *
 * Two shapes:
 *
 *  - **Branded / variant food** (e.g. 麦当劳): a size selector sits next to the
 *    serving count, and the preview uses the published per-serving numbers for
 *    the chosen size. Grams entry is not offered because the official data gives
 *    no weight.
 *  - **Per-100g food**: the original grams-or-servings toggle.
 */
@Composable
private fun AddEntrySheetContent(
    food: FoodEntity,
    variants: List<FoodVariantEntity>,
    onCancel: () -> Unit,
    onConfirm: (amount: Double, mode: AmountMode, meal: MealType, variantId: Long?) -> Unit
) {
    val isVariantFood = food.hasVariants && variants.isNotEmpty()
    var mode by remember { mutableStateOf(if (isVariantFood) AmountMode.SERVING else AmountMode.GRAMS) }
    var amountText by remember { mutableStateOf(if (isVariantFood) "1" else "100") }
    var meal by remember { mutableStateOf(MealType.OTHER) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedVariantId by remember {
        mutableStateOf(variants.firstOrNull()?.id)
    }

    val parsed = amountText.toDoubleOrNull()
    val servings = if (parsed != null && parsed > 0.0) parsed else 0.0
    val grams = when {
        !isVariantFood && mode == AmountMode.SERVING -> servings * food.servingSizeGrams
        !isVariantFood -> servings
        else -> 0.0
    }
    val factor = grams / 100.0
    val selectedVariant = variants.firstOrNull { it.id == selectedVariantId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = food.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        if (isVariantFood) {
            Text(
                text = "品牌食品 · 按官方单份营养计量",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "每 100g：碳 ${trim(food.carbsPer100g)}g · " +
                    "蛋 ${trim(food.proteinPer100g)}g · 脂 ${trim(food.fatPer100g)}g",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ------------------------------------------------ size selector
        if (isVariantFood) {
            Spacer(Modifier.height(16.dp))
            VariantSelector(
                variants = variants,
                selectedId = selectedVariantId,
                onSelect = { selectedVariantId = it.id }
            )
        }

        // ------------------------------------------------ amount input
        Spacer(Modifier.height(16.dp))
        if (!isVariantFood) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip(
                    selected = mode == AmountMode.GRAMS,
                    label = "按克数",
                    onClick = {
                        mode = AmountMode.GRAMS
                        amountText = "100"
                        error = null
                    }
                )
                ModeChip(
                    selected = mode == AmountMode.SERVING,
                    label = "按份数",
                    onClick = {
                        mode = AmountMode.SERVING
                        amountText = "1"
                        error = null
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        NumericField(
            value = amountText,
            onValueChange = {
                amountText = it
                error = null
            },
            label = when {
                isVariantFood -> "份数"
                mode == AmountMode.GRAMS -> "克数"
                else -> "份数"
            },
            suffix = if (!isVariantFood && mode == AmountMode.GRAMS) "g" else "份",
            modifier = Modifier.fillMaxWidth(),
            supportingText = when {
                isVariantFood -> "按份计量（官方未公布份量克重）"
                mode == AmountMode.SERVING ->
                    "一份 = ${trim(food.servingSizeGrams)} g，换算后共 ${trim(grams)} g"
                else -> null
            }
        )

        Spacer(Modifier.height(16.dp))
        Text(text = "记到哪一餐", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MealType.entries.forEach { option ->
                ModeChip(
                    selected = meal == option,
                    label = option.label,
                    onClick = { meal = option }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        Spacer(Modifier.height(12.dp))

        Text(
            text = "这条记录将摄入",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))

        if (isVariantFood && selectedVariant != null) {
            VariantNutritionRow(variant = selectedVariant, servings = servings)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SheetValue("碳水", food.carbsPer100g * factor)
                SheetValue("蛋白质", food.proteinPer100g * factor)
                SheetValue("脂肪", food.fatPer100g * factor)
                SheetValue(
                    "热量",
                    food.carbsPer100g * factor * 4.0 +
                        food.proteinPer100g * factor * 4.0 +
                        food.fatPer100g * factor * 9.0,
                    unit = "kcal"
                )
            }
        }

        if (isVariantFood) {
            Spacer(Modifier.height(10.dp))
            SourceNote(
                source = food.dataSource,
                credibility = FoodCredibility.fromStorage(food.credibility).label,
                note = food.sourceNote
            )
        }

        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) { Text("取消") }
            Button(
                onClick = {
                    val value = amountText.toDoubleOrNull()
                    when {
                        value == null || value <= 0.0 -> error = "请输入大于 0 的数量"
                        isVariantFood && selectedVariant == null -> error = "请选择规格"
                        !isVariantFood && mode == AmountMode.SERVING &&
                            food.servingSizeGrams <= 0.0 ->
                            error = "该食物没有设置一份大小，请先在食物库里补上"
                        else -> onConfirm(value, mode, meal, selectedVariantId)
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("添加") }
        }
    }
}

/** Selectable pill used for both the amount mode and the meal selector. */
@Composable
private fun ModeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(if (selected) "✓ $label" else label) }
    )
}

@Composable
private fun SheetValue(label: String, value: Double, unit: String = "g") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${trim(value)} $unit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
