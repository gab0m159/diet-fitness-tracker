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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diettracker.data.db.ExerciseOutcome
import com.example.diettracker.data.db.ExerciseStatusEntity
import com.example.diettracker.ui.viewmodel.HistoryDay
import com.example.diettracker.ui.viewmodel.TrainingHistoryUiState
import com.example.diettracker.ui.viewmodel.TrainingHistoryViewModel

/**
 * 往期训练记录页。
 *
 * 这里**只显示标记为「成功」或「失败」的动作，跳过的一律不出现**。
 * 原因是跳过的动作只是从当天待办里划掉的一种操作（数据库里仍然会留一行，
 * 值是 SKIPPED，否则今日页无法判断这张卡片已经被处理过），它并不代表练过，
 * 也不是一次训练结果；把它列进历史会让「我今天到底练了什么」变得不可信，
 * 也会虚增记录条数。因此过滤在 [TrainingHistoryViewModel] 里就完成了，
 * 这个页面只负责展示，不再按 outcome 二次筛选，避免两处规则漂移。
 *
 * 每条记录显示日期分组、动作名、当时的目标组数/次数/重量快照、来源，
 * 以及结果徽标。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingHistoryScreen(
    viewModel: TrainingHistoryViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("往期训练记录") },
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
            HistoryStatsBar(state = state)

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索动作名称或来源") },
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

            when {
                // 加载完成前不知道有没有记录，先转圈，避免先闪一下空态再跳出列表。
                state.loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.isEmpty -> EmptyHistory(query = state.query)

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.days.forEach { day ->
                            item(key = "day-${day.date}") {
                                HistoryDayHeader(day = day)
                            }
                            items(items = day.entries, key = { it.id }) { entry ->
                                HistoryExerciseCard(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 顶部统计条：「12 条记录 · 成功 9 · 失败 3」。 */
@Composable
private fun HistoryStatsBar(state: TrainingHistoryUiState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${state.totalEntries} 条记录",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "· 成功 ${state.successCount} · 失败 ${state.failureCount}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 日期分组头：日期、星期、当天小结。 */
@Composable
private fun HistoryDayHeader(day: HistoryDay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = day.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = day.weekday,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = day.summary,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 一条动作记录。
 *
 * 目标组数/次数与重量都是**当时的快照**（[ExerciseStatusEntity] 里存的就是快照），
 * 所以之后改训练日不会篡改这里显示的内容。
 */
@Composable
private fun HistoryExerciseCard(entry: ExerciseStatusEntity) {
    val outcome = entry.outcomeValue
    val success = outcome == ExerciseOutcome.SUCCESS

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                OutcomeBadge(outcome = outcome, success = success)
            }

            Text(
                text = "目标 ${entry.targetLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "重量 ${entry.targetWeightLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 只有成功才会调整目标重量，所以「+5kg → 65kg」只对成功行有意义；
                // 失败行的 changeLabel 恒为「保持 xx」，显示出来只是噪音。
                if (success) {
                    Text(
                        text = entry.changeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (entry.sourceLabel.isNotBlank()) {
                Text(
                    text = entry.sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/** 结果徽标：成功用主色（绿色系），失败用错误色。 */
@Composable
private fun OutcomeBadge(outcome: ExerciseOutcome, success: Boolean) {
    val tint = if (success) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = outcome.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}

/**
 * 空态。
 *
 * 搜不到和一条记录都没有是两种情况：前者要提示换关键词，后者要告诉用户
 * 记录是怎么产生的——尤其是「跳过的动作不会出现在这里」，否则用户会以为
 * 自己标记过却看不到是 bug。
 */
@Composable
private fun EmptyHistory(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (query.isBlank()) "还没有往期训练记录" else "没有找到「$query」",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (query.isBlank()) {
                    "练完在今日页把动作标记为成功或失败，这里就会有记录；跳过的动作不会出现在这里。"
                } else {
                    "换个动作名或来源试试。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
