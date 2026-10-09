package com.example.diettracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diettracker.domain.StretchGuide

/**
 * Stretch guidance card shown after a workout.
 *
 * Text-only by design. The [StretchGuide.mediaUrl] field is honoured if present,
 * and otherwise a dashed placeholder keeps the layout slot reserved so a GIF or
 * video link can be dropped in later without changing the composition.
 */
@Composable
fun StretchCard(
    guide: StretchGuide,
    modifier: Modifier = Modifier,
    onOpenInLibrary: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = guide.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "目标肌群：${guide.targetMuscle}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${guide.holdSecondsMin}-${guide.holdSecondsMax} 秒",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Reserved media area. Renders a placeholder until mediaUrl is set.
            StretchMediaSlot(mediaUrl = guide.mediaUrl)

            Spacer(Modifier.height(10.dp))
            Text(
                text = guide.howTo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "建议：${guide.prescription}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (guide.source.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "出处：${guide.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            onOpenInLibrary?.let { open ->
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = open) {
                    Text("在拉伸库中查看", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Placeholder for future stretch media.
 *
 * When [mediaUrl] is blank (always, today) this draws a subdued empty box so the
 * slot is visible and the layout does not reflow once media is added.
 */
@Composable
fun StretchMediaSlot(
    mediaUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 7f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (mediaUrl.isNullOrBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "示范图 / 视频位置（暂未提供）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            // Future: render the GIF or a video link preview here.
            Text(
                text = mediaUrl,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Header + list used on the post-workout summary. */
@Composable
fun StretchSection(
    guides: List<StretchGuide>,
    modifier: Modifier = Modifier,
    onOpenStretch: (String) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                Modifier
                    .size(4.dp, 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "练后拉伸",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { onOpenStretch(guides.firstOrNull()?.name.orEmpty()) }
            ) {
                Text("打开拉伸库", style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(
            text = StretchGuide.METHODOLOGY_NOTE,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        guides.forEach { guide ->
            StretchCard(
                guide = guide,
                onOpenInLibrary = { onOpenStretch(guide.name) }
            )
        }
    }
}
