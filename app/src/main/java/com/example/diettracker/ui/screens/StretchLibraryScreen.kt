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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diettracker.domain.StretchGuide
import com.example.diettracker.ui.components.ConfirmDialog
import com.example.diettracker.ui.components.ErrorBanner
import com.example.diettracker.ui.components.StretchMediaSlot
import com.example.diettracker.ui.viewmodel.LibraryViewModel

/**
 * Stand-alone stretch library, grouped by target muscle.
 *
 * Every entry cites its source, and custom entries can carry a description and a
 * photo/video link. The methodology note at the top explains where the hold times
 * and set counts come from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StretchLibraryScreen(
    viewModel: LibraryViewModel,
    highlightStretch: String?
) {
    val state by viewModel.stretchState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<StretchGuide?>(null) }
    var editing by remember { mutableStateOf<StretchGuide?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<String?>(highlightStretch) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Jump straight to the requested stretch when navigated from an exercise.
    LaunchedEffect(highlightStretch) {
        if (!highlightStretch.isNullOrBlank()) expanded = highlightStretch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("拉伸") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "新增拉伸")
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
                placeholder = { Text("搜索拉伸名称或目标肌群") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            val filtered = remember(state.groups, query) {
                if (query.isBlank()) {
                    state.groups
                } else {
                    state.groups.mapNotNull { (muscle, list) ->
                        val hits = list.filter {
                            it.name.contains(query, true) ||
                                it.targetMuscle.contains(query, true)
                        }
                        if (hits.isEmpty()) null else muscle to hits
                    }
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
                item { MethodologyCard() }

                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = "没有匹配的拉伸",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                filtered.forEach { (muscle, list) ->
                    item(key = "h_$muscle") {
                        Text(
                            text = muscle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(items = list, key = { it.name }) { guide ->
                        StretchDetailCard(
                            guide = guide,
                            expanded = expanded == guide.name,
                            onToggle = {
                                expanded = if (expanded == guide.name) null else guide.name
                            },
                            onEdit = { editing = guide },
                            onDelete = { pendingDelete = guide }
                        )
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        StretchEditorDialog(
            initial = editing,
            onDismiss = {
                showAdd = false
                editing = null
            },
            onConfirm = { name, muscle, min, max, sets, howTo, description, media, source ->
                val entity = com.example.diettracker.data.db.CustomStretchEntity(
                    name = name,
                    targetMuscle = muscle,
                    holdSecondsMin = min,
                    holdSecondsMax = max,
                    sets = sets,
                    howTo = howTo,
                    description = description,
                    mediaUrl = media,
                    source = source.ifBlank { "用户自建" }
                )
                viewModel.saveStretch(entity)
                showAdd = false
                editing = null
            }
        )
    }

    pendingDelete?.let { guide ->
        ConfirmDialog(
            title = "删除「${guide.name}」？",
            message = "只有自建拉伸可以删除。内置拉伸删除后会恢复到默认内容。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteStretch(guide.name)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun MethodologyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "方法与出处",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = StretchGuide.METHODOLOGY_NOTE,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "参考：ACSM 柔韧性训练指南；Harvard Health《The ideal stretching routine》；" +
                    "UC Davis Sports Medicine 拉伸手册；NHS 拉伸建议。" +
                    "每条动作的描述与注意点由本应用根据通用运动解剖学整理。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun StretchDetailCard(
    guide: StretchGuide,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCustom = guide.source.contains("用户自建")
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
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = "${guide.holdSecondsMin}-${guide.holdSecondsMax}s × ${guide.sets}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess
                        else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开"
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                StretchMediaSlot(mediaUrl = guide.mediaUrl)
                Spacer(Modifier.height(10.dp))

                Text(
                    text = "做法",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = guide.howTo,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (guide.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "说明 / 注意",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = guide.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "建议：${guide.prescription}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (guide.source.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "出处：${guide.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    if (isCustom) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "编辑",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Text(
                            text = "内置拉伸（不可编辑）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

/** Create / edit a custom stretch, including its media link. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StretchEditorDialog(
    initial: StretchGuide?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        muscle: String,
        min: Int,
        max: Int,
        sets: Int,
        howTo: String,
        description: String,
        media: String,
        source: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var muscle by remember { mutableStateOf(initial?.targetMuscle.orEmpty()) }
    var holdText by remember {
        mutableStateOf(initial?.holdSecondsMin?.toString() ?: "20")
    }
    var holdMaxText by remember {
        mutableStateOf(initial?.holdSecondsMax?.toString() ?: "30")
    }
    var setsText by remember { mutableStateOf(initial?.sets?.toString() ?: "3") }
    var howTo by remember { mutableStateOf(initial?.howTo.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var media by remember { mutableStateOf(initial?.mediaUrl.orEmpty()) }
    var source by remember {
        mutableStateOf(initial?.source?.takeIf { it != "用户自建" }.orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "新增拉伸" else "编辑拉伸",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("拉伸名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = muscle,
                    onValueChange = { muscle = it; error = null },
                    label = { Text("目标肌群") },
                    placeholder = { Text("例如：胸大肌") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = holdText,
                        onValueChange = { holdText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("保持最短") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = holdMaxText,
                        onValueChange = { holdMaxText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("保持最长") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = setsText,
                        onValueChange = { setsText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("组数") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp),
                        shape = MaterialTheme.shapes.medium
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = howTo,
                    onValueChange = { howTo = it },
                    label = { Text("做法") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("说明 / 注意（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = media,
                    onValueChange = { media = it },
                    label = { Text("照片 / 视频链接（可选）") },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("出处（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    ErrorBanner(it)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val min = holdText.toIntOrNull()
                val max = holdMaxText.toIntOrNull()
                val sets = setsText.toIntOrNull()
                when {
                    name.isBlank() -> error = "请填写拉伸名称"
                    muscle.isBlank() -> error = "请填写目标肌群"
                    min == null || min <= 0 -> error = "保持时间请填正整数"
                    max == null || max < min -> error = "最长保持时间不能小于最短"
                    sets == null || sets <= 0 -> error = "组数请填正整数"
                    howTo.isBlank() -> error = "请填写做法"
                    else -> onConfirm(
                        name, muscle, min, max, sets, howTo, description, media, source
                    )
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
