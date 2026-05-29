package net.luis.jenga.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.jenga.JengaApp
import net.luis.jenga.R
import net.luis.jenga.domain.model.Category
import net.luis.jenga.domain.model.Task

private sealed interface FolderSelection {
    data class Named(val category: Category) : FolderSelection
    data object Uncategorized : FolderSelection
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    app: JengaApp,
    onNavigateBack: () -> Unit,
    onNavigateToTask: (Long) -> Unit,
    onAddTask: () -> Unit
) {
    val viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory(app.database))
    val uiState by viewModel.uiState.collectAsState()

    var showCategoryDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var openFolder by remember { mutableStateOf<FolderSelection?>(null) }

    val searching = uiState.searchQuery.isNotBlank()
    val grouped = uiState.tasksByCategory
    val canGoUp = !searching && openFolder != null

    BackHandler(enabled = canGoUp) { openFolder = null }

    if (showCategoryDialog) {
        CategoryManagementDialog(
            categories = uiState.categories,
            onAddCategory = { viewModel.saveCategory(it) },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDismiss = { showCategoryDialog = false }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(stringResource(R.string.delete_task)) },
            text = { Text(stringResource(R.string.delete_task_confirm)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteTask(task)
                    taskToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val f = openFolder) {
                        is FolderSelection.Named -> if (searching) stringResource(R.string.tasks) else f.category.name
                        FolderSelection.Uncategorized -> if (searching) stringResource(R.string.tasks) else stringResource(R.string.uncategorized)
                        null -> stringResource(R.string.tasks)
                    }
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = { if (canGoUp) openFolder = null else onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showCategoryDialog = true }) {
                        Icon(Icons.Default.Category, contentDescription = stringResource(R.string.manage_categories))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task))
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(stringResource(R.string.search_tasks)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searching) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            when {
                searching -> TaskList(
                    tasks = uiState.filteredTasks,
                    onNavigateToTask = onNavigateToTask,
                    onDeleteTask = { taskToDelete = it }
                )

                openFolder == null -> {
                    if (grouped.isEmpty()) {
                        EmptyMessage(stringResource(R.string.no_tasks))
                    } else {
                        LazyColumn {
                            items(grouped.entries.toList(), key = { it.key?.id ?: -1L }) { (category, tasks) ->
                                FolderItem(
                                    name = category?.name ?: stringResource(R.string.uncategorized),
                                    count = tasks.size,
                                    onClick = {
                                        openFolder = if (category == null) FolderSelection.Uncategorized
                                        else FolderSelection.Named(category)
                                    }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }

                else -> TaskList(
                    tasks = tasksForSelection(grouped, openFolder!!),
                    onNavigateToTask = onNavigateToTask,
                    onDeleteTask = { taskToDelete = it }
                )
            }
        }
    }
}

private fun tasksForSelection(grouped: Map<Category?, List<Task>>, selection: FolderSelection): List<Task> =
    when (selection) {
        is FolderSelection.Named -> grouped.entries.firstOrNull { it.key?.id == selection.category.id }?.value ?: emptyList()
        FolderSelection.Uncategorized -> grouped[null] ?: emptyList()
    }

@Composable
private fun TaskList(
    tasks: List<Task>,
    onNavigateToTask: (Long) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    if (tasks.isEmpty()) {
        EmptyMessage(stringResource(R.string.no_tasks))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskListItem(
                task = task,
                onClick = { onNavigateToTask(task.id) },
                onDelete = { onDeleteTask(task) }
            )
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FolderItem(name: String, count: Int, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
        headlineContent = { Text(name) },
        trailingContent = { Text(pluralStringResource(R.plurals.task_count, count, count)) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@Composable
private fun TaskListItem(task: Task, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(task.title) },
            supportingContent = {
                if (task.categories.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        task.categories.forEach { cat ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(cat.name) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
                            )
                        }
                    }
                }
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_task),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryManagementDialog(
    categories: List<Category>,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(stringResource(R.string.delete_category_confirm)) },
            confirmButton = {
                Button(onClick = {
                    onDeleteCategory(cat)
                    categoryToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_categories)) },
        text = {
            Column {
                if (categories.isEmpty()) {
                    Text(stringResource(R.string.no_categories), style = MaterialTheme.typography.bodyMedium)
                } else {
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { categoryToDelete = cat }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onAddCategory(newName.trim())
                                newName = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_category))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
