package com.example.diettracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diettracker.data.db.FoodVariantEntity

/**
 * Size selector for a branded food, e.g. 薯条 迷你 / 小 / 中 / 大.
 *
 * Sits at the same level as the amount input: the user picks a size and then a
 * serving count. Each size carries its own officially published per-serving
 * nutrition, so switching size changes the numbers 1:1 with the source.
 *
 * A food with a single variant still renders (as one selected chip) so the user
 * can see which form the numbers refer to — e.g. "1个" for 巨无霸.
 */
@Composable
fun VariantSelector(
    variants: List<FoodVariantEntity>,
    selectedId: Long?,
    onSelect: (FoodVariantEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (variants.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "规格",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            val current = variants.firstOrNull { it.id == selectedId } ?: variants.first()
            Text(
                text = "${trimNumber(current.kcal)} kcal / 份",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            variants.forEach { variant ->
                val selected = variant.id == (selectedId ?: variants.first().id)
                AssistChip(
                    onClick = { onSelect(variant) },
                    label = {
                        Text(
                            text = if (selected) {
                                "✓ ${variant.displaySpec}"
                            } else {
                                variant.displaySpec
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }
    }
}

/**
 * Per-serving nutrition of the currently selected variant, scaled by a serving
 * count. Rendered as the "这条记录将摄入" preview.
 */
@Composable
fun VariantNutritionRow(
    variant: FoodVariantEntity,
    servings: Double,
    modifier: Modifier = Modifier
) {
    val scaled = variant.scaledBy(servings)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NutrientCell("碳水", scaled.carbsG, "g")
            NutrientCell("蛋白质", scaled.proteinG, "g")
            NutrientCell("脂肪", scaled.fatG, "g")
            NutrientCell("热量", scaled.kcal, "kcal")
        }
        if (scaled.hasSodium || scaled.hasCalcium) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (scaled.hasSodium) {
                    NutrientCell("钠", scaled.sodiumMg, "mg")
                }
                if (scaled.hasCalcium) {
                    NutrientCell("钙", scaled.calciumMg, "mg")
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "按官方公布的单份数值 × ${trimNumber(servings)} 份计算，未做换算",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NutrientCell(label: String, value: Double, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${trimNumber(value)} $unit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Shared number formatting: trims trailing zeros. */
internal fun trimNumber(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
    else rounded.toString()
}

/** Small caption used for source / credibility notes. */
@Composable
fun SourceNote(
    source: String,
    credibility: String,
    note: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (source.isNotBlank()) {
            Text(
                text = "数据来源：$source（$credibility）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        if (note.isNotBlank()) {
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
