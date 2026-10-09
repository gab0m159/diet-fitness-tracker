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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diettracker.data.db.CustomExerciseEntity
import com.example.diettracker.data.model.BodyPart
import com.example.diettracker.ui.components.ConfirmDialog
import com.example.diettracker.ui.components.SectionHeader
import com.example.diettracker.ui.components.ThinDivider
import com.example.diettracker.ui.theme.AppColors
import com.example.diettracker.ui.theme.Spacing
import com.example.diettracker.ui.viewmodel.ExerciseLibraryViewModel
import com.example.diettracker.ui.viewmodel.ExerciseRow

/**
 * 撸铁的动作库（库 → 运动 → 撸铁）。
 *
 * 这里只**管理动作本身**：名字、部位、主要肌群、要点。动作的重量 / 组数 / 次数
 * 属于当天的记录，在「今日 → 撸铁」里用三个滑轮填。
 *
 * 内置动作不可删（但可以看），自建动作可以增删改。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrengthLibraryScreen(
    viewModel: ExerciseLibraryViewModel,
    embedded: Boolean = false
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CustomExerciseEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<ExerciseRow?>(null) }

    LaunchedEffect(state.message, state.error) {
        (state.message ?: state.error)?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val matches: (String) -> Boolean = { name ->
        query.isBlank() || name.contains(query.trim(), ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = "搜索动作"
        )

        // 顶部的「新建动作」按钮：这是撸铁库里唯一能创建自建动作的入口，
        // 之前只留了弹窗状态却没接按钮，导致点不进来。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "共 ${state.totalCount} 个动作（自建 ${state.customRows.size}）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { editing = null; showAdd = true },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("新建动作", style = MaterialTheme.typography.labelLarge)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screenHorizontal,
                end = Spacing.screenHorizontal,
                top = 4.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
        ) {
            val customRows = state.customRows.filter { matches(it.name) }
            if (customRows.isNotEmpty()) {
                item(key = "h_custom") {
                    Text(
                        text = "我创建的动作（${customRows.size}）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Strength,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                items(items = customRows, key = { "c_${it.id}" }) { row ->
                    ExerciseLibraryRow(
                        row = row,
                        editable = true,
                        onEdit = {
                            editing = CustomExerciseEntity(
                                id = row.id,
                                name = row.name,
                                bodyPart = row.bodyPart.name,
                                primaryMuscle = row.primaryMuscle,
                                cue = row.cue,
                                stretchNames = CustomExerciseEntity.encode(row.stretchNames)
                            )
                        },
                        onDelete = { pendingDelete = row }
                    )
                }
            }

            state.builtInGroups.forEach { (part, list) ->
                val rows = list.filter { matches(it.name) }
                if (rows.isEmpty()) return@forEach
                item(key = "h_${part.name}") {
                    Text(
                        text = part.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(items = rows, key = { "b_${it.name}" }) { row ->
                    ExerciseLibraryRow(
                        row = row,
                        editable = false,
                        onEdit = null,
                        onDelete = null
                    )
                }
            }

            if (state.customRows.none { matches(it.name) } &&
                state.builtInGroups.none { (_, list) -> list.any { matches(it.name) } }
            ) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "没有匹配的动作",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        ExerciseEditorDialog(
            initial = editing,
            onDismiss = {
                showAdd = false
                editing = null
            },
            onConfirm = { entity ->
                viewModel.saveCustomExercise(entity)
                showAdd = false
                editing = null
            }
        )
    }

    pendingDelete?.let { row ->
        ConfirmDialog(
            title = "删除「${row.name}」？",
            message = "只会从动作库移除，已有的训练记录不受影响。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteCustomExercise(
                    CustomExerciseEntity(
                        id = row.id,
                        name = row.name,
                        bodyPart = row.bodyPart.name,
                        primaryMuscle = row.primaryMuscle
                    )
                )
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

/** 搜索输入框，样式与食物库保持一致。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutlinedSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = 6.dp),
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
}

@Composable
private fun ExerciseLibraryRow(
    row: ExerciseRow,
    editable: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.cardPadding, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(row.bodyPart.label)
                        if (row.primaryMuscle.isNotBlank()) append(" · ${row.primaryMuscle}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (row.cue.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "要点：${row.cue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (editable && onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            if (editable && onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/** 新建 / 编辑自建动作的弹窗，字段与数据模型一致（不含步进）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseEditorDialog(
    initial: CustomExerciseEntity?,
    onDismiss: () -> Unit,
    onConfirm: (CustomExerciseEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var part by remember {
        mutableStateOf(BodyPart.fromStorage(initial?.bodyPart ?: BodyPart.FULL_BODY.name))
    }
    var muscle by remember { mutableStateOf(initial?.primaryMuscle.orEmpty()) }
    var cue by remember { mutableStateOf(initial?.cue.orEmpty()) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建动作" else "编辑动作") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("动作名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("部位", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                BodyPart.entries.filter { it != BodyPart.REST }.chunked(4).forEach { rowParts ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        rowParts.forEach { p ->
                            androidx.compose.material3.FilterChip(
                                selected = part == p,
                                onClick = { part = p },
                                label = { Text(p.label) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = muscle,
                    onValueChange = { muscle = it },
                    label = { Text("主要肌群（可选）") },
                    placeholder = { Text("例如：胸大肌上部") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = cue,
                    onValueChange = { cue = it },
                    label = { Text("要点（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    onConfirm(
                        (initial ?: CustomExerciseEntity(name = "", bodyPart = part.name))
                            .copy(
                                name = name.trim(),
                                bodyPart = part.name,
                                primaryMuscle = muscle.trim(),
                                cue = cue.trim()
                            )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
