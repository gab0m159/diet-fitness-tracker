package com.example.diettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.diettracker.ui.components.ErrorBanner
import com.example.diettracker.ui.viewmodel.ExerciseRow
import com.example.diettracker.ui.viewmodel.LibraryViewModel
import com.example.diettracker.domain.ProgressionEngine

/**
 * Exercise library: search, per-exercise load step, and a "拉伸 ↗" jump into the
 * stretch library.
 *
 * Custom exercises can be created, edited and deleted here; built-in ones are
 * read-only except for their increment, which the user is allowed to override.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    viewModel: LibraryViewModel,
    onOpenStretch: (String) -> Unit
) {
    val state by viewModel.exerciseState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<ExerciseRow?>(null) }
    var editingRow by remember { mutableStateOf<CustomExerciseEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(state.message, state.error) {
        (state.message ?: state.error)?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("训练动作") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "新增动作")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索动作名称或目标肌群") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            val groups = remember(state.builtInGroups, query) {
                if (query.isBlank()) {
                    state.builtInGroups
                } else {
                    state.builtInGroups.mapNotNull { (part, list) ->
                        val hits = list.filter {
                            it.name.contains(query, true) ||
                                it.primaryMuscle.contains(query, true)
                        }
                        if (hits.isEmpty()) null else part to hits
                    }
                }
            }
            val custom = remember(state.customRows, query) {
                if (query.isBlank()) state.customRows
                else state.customRows.filter {
                    it.name.contains(query, true) ||
                        it.primaryMuscle.contains(query, true)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (custom.isNotEmpty()) {
                    item(key = "h_custom") {
                        Text(
                            text = "我创建的动作",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(items = custom, key = { "c_${it.name}" }) { row ->
                        ExerciseCard(
                            row = row,
                            onOpenStretch = onOpenStretch,
                            onEdit = {
                                editingRow = CustomExerciseEntity(
                                    id = row.id,
                                    name = row.name,
                                    bodyPart = row.bodyPart.name,
                                    primaryMuscle = row.primaryMuscle,
                                    stretchNames = CustomExerciseEntity.encode(row.stretchNames),
                                    cue = row.cue
                                )
                            },
                            onDelete = { pendingDelete = row }
                        )
                    }
                }

                groups.forEach { (part, list) ->
                    item(key = "h_${part.name}") {
                        Text(
                            text = part.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(items = list, key = { "b_${it.name}" }) { row ->
                        ExerciseCard(
                            row = row,
                            onOpenStretch = onOpenStretch,
                            onEdit = null,
                            onDelete = null
                        )
                    }
                }

                if (groups.isEmpty() && custom.isEmpty()) {
                    item {
                        Text(
                            text = "没有匹配的动作",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAdd || editingRow != null) {
        ExerciseEditorDialog(
            initial = editingRow,
            availableStretches = state.builtInGroups
                .flatMap { it.second }
                .flatMap { it.stretchNames }
                .distinct()
                .sorted(),
            onDismiss = {
                showAdd = false
                editingRow = null
            },
            onConfirm = { entity ->
                viewModel.saveCustomExercise(entity)
                showAdd = false
                editingRow = null
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

@Composable
private fun ExerciseCard(
    row: ExerciseRow,
    onOpenStretch: (String) -> Unit,
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
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(row.bodyPart.label)
                            if (row.primaryMuscle.isNotBlank()) {
                                append(" · ${row.primaryMuscle}")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "编辑",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (row.cue.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "要点：${row.cue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (row.source.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "出处：${row.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/** Create / edit a custom exercise. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseEditorDialog(
    initial: CustomExerciseEntity?,
    availableStretches: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (CustomExerciseEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var part by remember {
        mutableStateOf(BodyPart.fromStorage(initial?.bodyPart ?: BodyPart.FULL_BODY.name))
    }
    var muscle by remember { mutableStateOf(initial?.primaryMuscle.orEmpty()) }
    var cue by remember { mutableStateOf(initial?.cue.orEmpty()) }
    var increment by remember { mutableStateOf(initial?.incrementKg ?: 2.5) }
    var selectedStretches by remember {
        mutableStateOf(
            CustomExerciseEntity
                .decodeStretches(initial?.stretchNames.orEmpty())
                .toSet()
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "新增动作" else "编辑动作",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("动作名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(8.dp))
                Text("部位", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BodyPart.entries.take(4).forEach { option ->
                        AssistChip(
                            onClick = { part = option },
                            label = {
                                Text(if (part == option) "✓${option.label}" else option.label)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BodyPart.entries.drop(4).take(5).forEach { option ->
                        AssistChip(
                            onClick = { part = option },
                            label = {
                                Text(if (part == option) "✓${option.label}" else option.label)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = muscle,
                    onValueChange = { muscle = it },
                    label = { Text("目标肌群（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cue,
                    onValueChange = { cue = it },
                    label = { Text("动作要点（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "加重步进：${trimNumber(increment)}kg",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProgressionEngine.incrementChoices.take(5).forEach { option ->
                        AssistChip(
                            onClick = { increment = option },
                            label = {
                                Text(
                                    if (increment == option) "✓ ${trimNumber(option)}"
                                    else trimNumber(option)
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProgressionEngine.incrementChoices.drop(4).forEach { option ->
                        AssistChip(
                            onClick = { increment = option },
                            label = {
                                Text(
                                    if (increment == option) "✓ ${trimNumber(option)}"
                                    else trimNumber(option)
                                )
                            }
                        )
                    }
                }

                if (availableStretches.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "关联拉伸（可选，可多选）",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.height(120.dp)
                    ) {
                        items(items = availableStretches, key = { it }) { stretch ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedStretches.contains(stretch),
                                    onCheckedChange = { checked ->
                                        val next = selectedStretches.toMutableSet()
                                        if (checked) next.add(stretch) else next.remove(stretch)
                                        selectedStretches = next
                                    }
                                )
                                Text(
                                    text = stretch,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                error?.let {
                    Spacer(Modifier.height(6.dp))
                    ErrorBanner(it)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    error = "请填写动作名称"
                } else {
                    onConfirm(
                        CustomExerciseEntity(
                            id = initial?.id ?: 0L,
                            name = name,
                            bodyPart = part.name,
                            primaryMuscle = muscle,
                            incrementKg = increment,
                            stretchNames = CustomExerciseEntity.encode(
                                selectedStretches.toList()
                            ),
                            cue = cue
                        )
                    )
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun trimNumber(value: Double): String {
    val rounded = Math.round(value * 100.0) / 100.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
    else rounded.toString()
}
