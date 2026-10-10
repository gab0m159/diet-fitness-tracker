package com.example.diettracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diettracker.data.db.PersonalRecordEntity
import com.example.diettracker.ui.components.Dot
import com.example.diettracker.ui.components.TripleWheelPicker
import com.example.diettracker.ui.theme.AppColors
import com.example.diettracker.ui.theme.Spacing
import com.example.diettracker.ui.viewmodel.PersonalRecordViewModel
import com.example.diettracker.ui.viewmodel.RecordGroup
import com.example.diettracker.util.DateUtils

/**
 * 「我的 PR」。
 *
 * 每个动作只显示**当前最好成绩**（重量优先、同重量比次数），点一下展开可以看到
 * 这个动作的历史纪录。动作从动作库里选，记录 `动作 + 重量 + 次数 + 日期`。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRecordScreen(
    viewModel: PersonalRecordViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAdd by remember { mutableStateOf(false) }
    var expandedGroup by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<PersonalRecordEntity?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的 PR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Spacing.screenHorizontal,
                end = Spacing.screenHorizontal,
                top = 6.dp,
                bottom = Spacing.listBottom
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
        ) {
            item(key = "hint") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.StrengthSoft.copy(alpha = 0.7f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.cardPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = AppColors.Strength,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(Spacing.itemGap))
                        Text(
                            text = if (state.groups.isEmpty()) {
                                "还没有记录。点下面的「添加 PR」，从动作库选一个动作，" +
                                    "填上你的最大重量。"
                            } else {
                                "共 ${state.groups.size} 个动作。" +
                                    "点某一项可以展开看这个动作的历史纪录。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item(key = "add") {
                Button(
                    onClick = { showAdd = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("添加 PR")
                }
            }

            items(items = state.groups, key = { it.exerciseName }) { group ->
                RecordGroupCard(
                    group = group,
                    expanded = expandedGroup == group.exerciseName,
                    onToggle = {
                        expandedGroup =
                            if (expandedGroup == group.exerciseName) null else group.exerciseName
                    },
                    onDelete = { pendingDelete = it }
                )
            }

            if (state.groups.isNotEmpty()) {
                item(key = "pr_explain") {
                    Text(
                        text = "排序方式：重量优先，同重量比次数。" +
                            "展开后的「≈1RM」是按 Epley 公式估算的极限重量，方便跨次数比较。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddRecordDialog(
            exerciseNames = state.exerciseNames,
            onDismiss = { showAdd = false },
            onConfirm = { name, weight, reps ->
                viewModel.addRecord(name, weight, reps, DateUtils.today())
                showAdd = false
            }
        )
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条纪录？") },
            text = {
                Text(
                    "${record.exerciseName}　${record.summary}　${record.date}" +
                        "\n\n删除后该动作的历史最好成绩会重新计算。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(record.id)
                        pendingDelete = null
                    }
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

/** 一个动作的 PR 卡片：显示（当前最好的）成绩，展开后列出历史。 */
@Composable
private fun RecordGroupCard(
    group: RecordGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: (PersonalRecordEntity) -> Unit
) {
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
                .clickable(onClick = onToggle)
                .padding(Spacing.cardPadding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(color = AppColors.Strength, size = 8)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${group.best.date} · 共 ${group.history.size} 条" +
                            if (group.history.size > 1) "（点开看历史）" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${trimNumber(group.best.weightKg)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Strength
                    )
                    Text(
                        text = "kg × ${group.best.reps} 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(Spacing.itemGap))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(Modifier.height(Spacing.itemGap))

                if (group.best.reps > 1) {
                    Text(
                        text = "估算 1RM ≈ ${trimNumber(group.best.estimatedOneRm)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Strength
                    )
                    Spacer(Modifier.height(6.dp))
                }

                group.history.forEach { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.date,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(96.dp)
                        )
                        Text(
                            text = record.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (record.note.isNotBlank()) {
                            Text(
                                text = record.note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDelete(record) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除",
                                modifier = Modifier.size(17.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 添加一条 PR：选动作 + 三个滚轮（重量 / 组数→次数）。 */
@Composable
private fun AddRecordDialog(
    exerciseNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf(20.0) }
    var reps by remember { mutableIntStateOf(1) }

    val filtered = remember(exerciseNames, query) {
        val q = query.trim()
        if (q.isEmpty()) exerciseNames else exerciseNames.filter { it.contains(q, true) }
    }

    if (step == 0) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("选择动作") },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("搜索动作") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(360.dp)) {
                        items(items = filtered, key = { it }) { name ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        picked = name
                                        step = 1
                                    }
                                    .padding(vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "选择",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppColors.Strength
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(picked.orEmpty()) },
            text = {
                Column {
                    Text(
                        text = "拖动滚轮记下你能做起来的最大重量和次数。" +
                            "默认是 1 次（冲极限）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.itemGap))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.Strength.copy(alpha = 0.08f))
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${trimNumber(weight)} kg × $reps 次",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Strength
                        )
                    }
                    Spacer(Modifier.height(Spacing.itemGap))
                    // 复用三个滚轮组件，组数固定 1、只取「重量」和「次数」
                    TripleWheelPicker(
                        weightKg = weight,
                        sets = 1,
                        reps = reps,
                        accent = AppColors.Strength,
                        onWeightChange = { weight = it },
                        onSetsChange = { },
                        onRepsChange = { reps = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(picked.orEmpty(), weight, reps) },
                    enabled = picked != null && weight > 0
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
    }
}

private fun trimNumber(value: Double): String {
    val rounded = Math.round(value * 100.0) / 100.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
    else rounded.toString()
}
