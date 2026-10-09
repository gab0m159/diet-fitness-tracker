package com.example.diettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diettracker.data.model.ActivityLevel
import com.example.diettracker.data.model.GoalMode
import com.example.diettracker.data.model.Sex
import com.example.diettracker.domain.MacroEstimate
import com.example.diettracker.ui.components.ErrorBanner
import com.example.diettracker.ui.components.HintText
import com.example.diettracker.ui.components.NumericField
import com.example.diettracker.ui.theme.MacroColors
import com.example.diettracker.ui.viewmodel.ProfileViewModel

/**
 * Body metrics and the macro-target estimate.
 *
 * The estimate is shown as a suggestion with its own "应用" button; it never
 * silently replaces a hand-set goal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onOpenGoals: () -> Unit = {},
    onOpenPlan: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenLibrary: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ------------------------------------------- settings entries
            EntryCard(
                title = "每日目标",
                subtitle = "当前：碳水 ${round(state.currentGoalCarbs)}g / " +
                    "蛋白 ${round(state.currentGoalProtein)}g / " +
                    "脂肪 ${round(state.currentGoalFat)}g",
                onClick = onOpenGoals
            )
            Spacer(Modifier.height(8.dp))
            EntryCard(
                title = "训练日",
                subtitle = "创建训练日、排动作与目标重量、设置几天一次",
                onClick = onOpenPlan
            )
            Spacer(Modifier.height(8.dp))
            EntryCard(
                title = "往期训练记录",
                subtitle = "成功 / 失败的动作记录（跳过的不显示）",
                onClick = onOpenHistory
            )
            Spacer(Modifier.height(8.dp))
            EntryCard(
                title = "动作与拉伸库",
                subtitle = "训练动作、拉伸动作与食物",
                onClick = onOpenLibrary
            )

            Spacer(Modifier.height(16.dp))

            // ------------------------------------------------- basic data
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "身体数据",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(
                            value = state.height,
                            onValueChange = viewModel::onHeightChange,
                            label = "身高",
                            suffix = "cm",
                            modifier = Modifier.weight(1f)
                        )
                        NumericField(
                            value = state.weight,
                            onValueChange = viewModel::onWeightChange,
                            label = "体重",
                            suffix = "kg",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericField(
                            value = state.age,
                            onValueChange = viewModel::onAgeChange,
                            label = "年龄",
                            suffix = "岁",
                            modifier = Modifier.weight(1f)
                        )
                        NumericField(
                            value = state.bodyFat,
                            onValueChange = viewModel::onBodyFatChange,
                            label = "体脂率（可选）",
                            suffix = "%",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HintText("体脂率不填也能计算；填了会额外显示 Katch-McArdle 参考值")
                        if (state.hasBodyFat) {
                            OutlinedButton(
                                onClick = viewModel::clearBodyFat,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("清除", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("性别", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Sex.entries.forEach { option ->
                            FilterChip(
                                selected = state.sex == option,
                                onClick = { viewModel.onSexChange(option) },
                                label = { Text(option.label) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ------------------------------------------------ activity
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "活动系数",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActivityLevel.entries.forEach { level ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = state.activity == level,
                                    onClick = { viewModel.onActivityChange(level) },
                                    label = { Text("${level.label} ×${level.multiplier}") }
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = level.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---------------------------------------------------- goal
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "目标模式",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GoalMode.entries.forEach { mode ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = state.goalMode == mode,
                                    onClick = { viewModel.onGoalModeChange(mode) },
                                    label = { Text(mode.label) }
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "${mode.description} · 碳水 " +
                                        "${(mode.carbsPct * 100).toInt()}% / 蛋白 " +
                                        "${(mode.proteinPct * 100).toInt()}% / 脂肪 " +
                                        "${(mode.fatPct * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ------------------------------------------------ estimate
            state.estimate?.let { estimate ->
                EstimateCard(
                    estimate = estimate,
                    currentCarbs = state.currentGoalCarbs,
                    currentProtein = state.currentGoalProtein,
                    currentGoalFat = state.currentGoalFat,
                    hasBodyFat = state.hasBodyFat,
                    onApply = viewModel::applyEstimateToGoals
                )
            } ?: MissingInputCard()

            state.error?.let { message ->
                Spacer(Modifier.height(12.dp))
                ErrorBanner(message)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = viewModel::saveProfile,
                    enabled = !state.saving,
                    modifier = Modifier.weight(1f)
                ) { Text("保存身体数据") }
                Button(
                    onClick = viewModel::saveProfileAndApply,
                    enabled = !state.saving && state.estimate != null,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("保存并应用目标")
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "计算公式：Mifflin-St Jeor 估算 BMR（男 10×体重 + 6.25×身高 − 5×年龄 + 5；" +
                    "女 … − 161），乘以活动系数得到 TDEE，再按目标模式调整" +
                    "（增肌 +15% / 保持 0% / 减脂 −20%），" +
                    "最后按 4/4/9 kcal per g 拆成碳蛋脂克数。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EntryCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun EstimateCard(
    estimate: MacroEstimate,
    currentCarbs: Double,
    currentProtein: Double,
    currentGoalFat: Double,
    hasBodyFat: Boolean,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "估算结果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(10.dp))

            EstimateRow("基础代谢 BMR（Mifflin）", "${round(estimate.bmrMifflin)} kcal")
            EstimateRow("每日总消耗 TDEE", "${round(estimate.tdee)} kcal")
            EstimateRow("目标热量", "${round(estimate.targetCalories)} kcal")

            if (hasBodyFat && estimate.bmrKatch != null) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(6.dp))
                EstimateRow(
                    "BMR（Katch-McArdle 参考）",
                    "${round(estimate.bmrKatch)} kcal"
                )
                Text(
                    text = "Katch 仅作参考，默认 TDEE 仍使用 Mifflin 结果。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(10.dp))

            Text(
                text = "建议每日目标",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(6.dp))
            EstimateRow("碳水", "${round(estimate.targets.carbs)} g")
            EstimateRow("蛋白质", "${round(estimate.targets.protein)} g")
            EstimateRow("脂肪", "${round(estimate.targets.fat)} g")

            Spacer(Modifier.height(10.dp))
            Text(
                text = "当前目标：碳水 ${round(currentCarbs)}g / " +
                    "蛋白 ${round(currentProtein)}g / 脂肪 ${round(currentGoalFat)}g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(Modifier.height(10.dp))
            Button(onClick = onApply) { Text("应用为每日目标") }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "建议值不会自动覆盖你手动的设置，需要点上面的按钮才会写入。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun EstimateRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun MissingInputCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "填好身高、体重、年龄后这里会显示估算结果",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun round(value: Double): String = Math.round(value).toString()
