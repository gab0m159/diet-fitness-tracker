package com.example.diettracker.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diettracker.data.repository.DietRepository
import com.example.diettracker.ui.components.ErrorBanner
import com.example.diettracker.ui.components.HintText
import com.example.diettracker.ui.components.NumericField
import com.example.diettracker.ui.theme.MacroColors
import com.example.diettracker.ui.viewmodel.GoalViewModel

/**
 * Daily macro goal editor. The values persist in Room, so the home summary and
 * this screen always agree.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    repository: DietRepository,
    onBack: () -> Unit
) {
    val viewModel: GoalViewModel = viewModel(
        factory = GoalViewModel.factory(repository)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSavedMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("每日目标") },
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
            Text(
                text = "设定每天想摄入的碳蛋脂克数，首页会按这个目标显示进度和剩余额度。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            GoalField(
                label = "碳水目标",
                value = state.carbs,
                onValueChange = viewModel::onCarbsChange,
                accent = MacroColors.Carbs
            )
            Spacer(Modifier.height(12.dp))
            GoalField(
                label = "蛋白质目标",
                value = state.protein,
                onValueChange = viewModel::onProteinChange,
                accent = MacroColors.Protein
            )
            Spacer(Modifier.height(12.dp))
            GoalField(
                label = "脂肪目标",
                value = state.fat,
                onValueChange = viewModel::onFatChange,
                accent = MacroColors.Fat
            )

            state.preview?.let { preview ->
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "按这个目标，每天约 ${Math.round(preview.calories)} kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "碳水 ${Math.round(preview.carbs * 4)} + " +
                                "蛋白 ${Math.round(preview.protein * 4)} + " +
                                "脂肪 ${Math.round(preview.fat * 9)} kcal" +
                                "（按 4/4/9 kcal 每克换算）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            state.error?.let { error ->
                Spacer(Modifier.height(16.dp))
                ErrorBanner(error)
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = viewModel::restoreDefaults,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("恢复默认")
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saving && !state.loading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(16.dp)
                                .height(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("保存目标")
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "关于默认值",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "默认目标（碳水 250g / 蛋白质 125g / 脂肪 55g）大致对应 2000 kcal，" +
                    "只是初始值，请按自己的需要修改。目标保存在本地数据库里，重启应用后依然生效。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HintText("提示：某项不想跟踪可以填 0，该项进度会显示为「—」。")

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GoalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.width(6.dp).height(38.dp)) {
            drawRoundRect(
                color = accent,
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
        Spacer(Modifier.width(10.dp))
        NumericField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            suffix = "g",
            modifier = Modifier.weight(1f)
        )
    }
}
