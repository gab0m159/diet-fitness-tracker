package com.example.diettracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diettracker.data.db.FoodVariantEntity
import com.example.diettracker.data.model.AmountMode
import com.example.diettracker.data.model.DiaryEntry
import com.example.diettracker.data.model.MealType
import com.example.diettracker.util.NumberParsing

/**
 * Bottom sheet used to change an existing diary entry's amount and meal.
 *
 * Pre-fills with the entry's original mode, so a "1.5 份" entry reopens as "1.5 份".
 * For a branded food the size selector is shown as well, seeded from the size
 * originally logged.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntrySheet(
    entry: DiaryEntry,
    availableVariants: List<FoodVariantEntity>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, mode: AmountMode, meal: MealType, variantId: Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isVariantEntry = entry.usesVariant

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        var mode by remember(entry.id) {
            mutableStateOf(if (isVariantEntry) AmountMode.SERVING else entry.amountMode)
        }
        var amountText by remember(entry.id) {
            mutableStateOf(
                NumberParsing.formatForInput(
                    if (mode == AmountMode.SERVING && entry.servings > 0.0) {
                        entry.servings
                    } else {
                        entry.grams
                    }
                )
            )
        }
        var meal by remember(entry.id) { mutableStateOf(entry.mealType) }
        var error by remember(entry.id) { mutableStateOf<String?>(null) }
        var selectedVariantId by remember(entry.id) {
            mutableStateOf(
                availableVariants
                    .firstOrNull { it.specName == entry.variantSpec }
                    ?.id
                    ?: availableVariants.firstOrNull()?.id
            )
        }

        val parsed = amountText.toDoubleOrNull()
        val servings = if (parsed != null && parsed > 0.0) parsed else 0.0
        val grams = when {
            isVariantEntry -> 0.0
            mode == AmountMode.GRAMS -> servings
            else -> servings * entry.servingSizeGrams
        }
        val factor = grams / 100.0
        val selectedVariant = availableVariants.firstOrNull { it.id == selectedVariantId }

        // The entry snapshots per-100g values, so undo the snapshot to preview.
        val per100Carbs = if (entry.grams > 0.0) entry.macros.carbs / entry.grams * 100.0 else 0.0
        val per100Protein =
            if (entry.grams > 0.0) entry.macros.protein / entry.grams * 100.0 else 0.0
        val per100Fat = if (entry.grams > 0.0) entry.macros.fat / entry.grams * 100.0 else 0.0

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "修改「${entry.foodName}」",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "当前记录：${entry.amountLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // -------------------------------------------- size selector
            if (isVariantEntry && availableVariants.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                VariantSelector(
                    variants = availableVariants,
                    selectedId = selectedVariantId,
                    onSelect = { selectedVariantId = it.id }
                )
            }

            // -------------------------------------------- amount toggle
            Spacer(Modifier.height(16.dp))
            if (!isVariantEntry) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            if (mode != AmountMode.GRAMS) {
                                // Convert the currently typed amount so nothing is lost.
                                val current = amountText.toDoubleOrNull()
                                amountText = if (current != null) {
                                    NumberParsing.formatForInput(
                                        current * entry.servingSizeGrams
                                    )
                                } else {
                                    NumberParsing.formatForInput(entry.grams)
                                }
                            }
                            mode = AmountMode.GRAMS
                            error = null
                        },
                        label = {
                            Text(if (mode == AmountMode.GRAMS) "✓ 按克数" else "按克数")
                        }
                    )
                    AssistChip(
                        onClick = {
                            if (mode != AmountMode.SERVING && entry.servingSizeGrams > 0.0) {
                                val current = amountText.toDoubleOrNull()
                                amountText = if (current != null) {
                                    NumberParsing.formatForInput(
                                        current / entry.servingSizeGrams
                                    )
                                } else {
                                    NumberParsing.formatForInput(1.0)
                                }
                            }
                            mode = AmountMode.SERVING
                            error = null
                        },
                        label = {
                            Text(if (mode == AmountMode.SERVING) "✓ 按份数" else "按份数")
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
                label = if (!isVariantEntry && mode == AmountMode.GRAMS) "克数" else "份数",
                suffix = if (!isVariantEntry && mode == AmountMode.GRAMS) "g" else "份",
                modifier = Modifier.fillMaxWidth(),
                supportingText = when {
                    isVariantEntry -> "按份计量（官方未公布份量克重）"
                    mode == AmountMode.SERVING ->
                        "一份 = ${Math.round(entry.servingSizeGrams)} g，换算后共 " +
                            "${Math.round(grams)} g"
                    else -> null
                }
            )

            Spacer(Modifier.height(16.dp))
            Text(text = "记到哪一餐", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealType.entries.forEach { option ->
                    AssistChip(
                        onClick = { meal = option },
                        label = {
                            Text(if (meal == option) "✓ ${option.label}" else option.label)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "修改后这条将摄入",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))

            if (isVariantEntry && selectedVariant != null) {
                VariantNutritionRow(variant = selectedVariant, servings = servings)
            } else {
                Text(
                    text = "碳水 ${Math.round(per100Carbs * factor)}g · " +
                        "蛋白质 ${Math.round(per100Protein * factor)}g · " +
                        "脂肪 ${Math.round(per100Fat * factor)}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
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
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("取消") }
                Button(
                    onClick = {
                        val value = amountText.toDoubleOrNull()
                        when {
                            value == null || value <= 0.0 -> error = "请输入大于 0 的数量"
                            isVariantEntry && selectedVariant == null -> error = "请选择规格"
                            !isVariantEntry && mode == AmountMode.SERVING &&
                                entry.servingSizeGrams <= 0.0 ->
                                error = "这个食物没有设置一份大小"
                            else -> onConfirm(value, mode, meal, selectedVariantId)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("保存") }
            }
        }
    }
}
