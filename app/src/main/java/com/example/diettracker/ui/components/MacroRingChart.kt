package com.example.diettracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.diettracker.data.model.Macros
import com.example.diettracker.ui.theme.MacroColors

/**
 * A ring chart showing how the day's calories split across carbs / protein / fat,
 * with the consumed-vs-goal percentage in the middle.
 *
 * The ring is segmented by *calorie contribution*, which is the conventional way
 * to read a macro ring, while the centre number is the plain macro completion
 * ratio (sum of grams consumed / sum of grams targeted).
 */
@Composable
fun MacroRingChart(
    consumed: Macros,
    goal: Macros,
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp,
    strokeWidth: Dp = 18.dp,
    centerTitle: String,
    centerSubtitle: String
) {
    val percent = overallPercent(consumed, goal).coerceIn(0.0, 1.0).toFloat()
    val animated by animateFloatAsState(
        targetValue = percent,
        animationSpec = tween(durationMillis = 700),
        label = "ringProgress"
    )

    val totalKcal = consumed.calories
    val slices = listOf(
        MacroColors.Carbs to consumed.carbs * 4.0,
        MacroColors.Protein to consumed.protein * 4.0,
        MacroColors.Fat to consumed.fat * 9.0
    ).filter { it.second > 0.0 }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val empty = totalKcal <= 0.0

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)

            // Track.
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            if (empty) return@Canvas

            // Macro segments, scaled by the animated completion ratio so that the
            // ring fills up as the user eats.
            var start = -90f
            slices.forEach { (color, kcal) ->
                val sweep = (kcal / totalKcal * 360.0).toFloat() * animated
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
                start += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = centerSubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Three concentric rings, one per macro, each showing `consumed / goal`.
 * Used on the goal screen and as a compact visual on the home screen.
 */
@Composable
fun TripleProgressRings(
    consumed: Macros,
    goal: Macros,
    modifier: Modifier = Modifier,
    diameter: Dp = 132.dp
) {
    val ringStroke = 12.dp
    val ratios = listOf(
        MacroColors.Carbs to ratioOf(consumed.carbs, goal.carbs),
        MacroColors.Protein to ratioOf(consumed.protein, goal.protein),
        MacroColors.Fat to ratioOf(consumed.fat, goal.fat)
    )
    val animatedRatios = ratios.map { (color, value) ->
        val animated by animateFloatAsState(
            targetValue = value.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 700),
            label = "tripleRing"
        )
        color to animated
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.size(diameter)) {
        val strokePx = ringStroke.toPx()
        animatedRatios.forEachIndexed { index, (color, ratio) ->
            val inset = strokePx / 2f + strokePx * index
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (ratio > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * ratio,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/** "碳水 120/250 g" style legend, stacked. */
@Composable
fun MacroLegend(
    consumed: Macros,
    goal: Macros,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MacroLegendRow(
            label = "碳水",
            value = "${Math.round(consumed.carbs)} / ${Math.round(goal.carbs)} g",
            color = MacroColors.Carbs
        )
        MacroLegendRow(
            label = "蛋白质",
            value = "${Math.round(consumed.protein)} / ${Math.round(goal.protein)} g",
            color = MacroColors.Protein
        )
        MacroLegendRow(
            label = "脂肪",
            value = "${Math.round(consumed.fat)} / ${Math.round(goal.fat)} g",
            color = MacroColors.Fat
        )
    }
}

/** Sum-of-grams completion across all three macros, used as the ring's centre number. */
fun overallPercent(consumed: Macros, goal: Macros): Double {
    val goalSum = goal.carbs + goal.protein + goal.fat
    if (goalSum <= 0.0) return 0.0
    val consumedSum = consumed.carbs + consumed.protein + consumed.fat
    return consumedSum / goalSum
}

private fun ratioOf(consumed: Double, goal: Double): Float =
    if (goal > 0.0) (consumed / goal).toFloat() else 0f

/** Small inline "chip" showing one macro's grams, used in list rows. */
@Composable
fun MacroChip(label: String, grams: Double, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(6.dp)) {
            drawCircle(color = color)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$label ${Math.round(grams)}g",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
