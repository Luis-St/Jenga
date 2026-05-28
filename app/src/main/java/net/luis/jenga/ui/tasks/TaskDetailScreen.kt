package net.luis.jenga.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.luis.jenga.JengaApp
import net.luis.jenga.R
import net.luis.jenga.data.repository.TaskRepository
import net.luis.jenga.domain.model.Category
import net.luis.jenga.domain.model.Task

private data class TaskDetailUiState(
    val title: String = "",
    val description: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    val allCategories: List<Category> = emptyList(),
    val titleError: Boolean = false,
    val isLoading: Boolean = true,
    val isExistingTask: Boolean = false
)

private class TaskDetailViewModel(
    private val repository: TaskRepository,
    private val taskId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailUiState())
    val state: StateFlow<TaskDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val allCategories = repository.getAllCategoriesOnce()
            if (taskId != -1L) {
                val task = repository.getTaskById(taskId)
                _state.value = TaskDetailUiState(
                    title = task?.title ?: "",
                    description = task?.description ?: "",
                    selectedCategoryIds = task?.categories?.map { it.id }?.toSet() ?: emptySet(),
                    allCategories = allCategories,
                    isLoading = false,
                    isExistingTask = task != null
                )
            } else {
                _state.value = TaskDetailUiState(allCategories = allCategories, isLoading = false)
            }
        }
    }

    fun setTitle(value: String) {
        _state.value = _state.value.copy(title = value, titleError = false)
    }

    fun setDescription(value: String) {
        _state.value = _state.value.copy(description = value)
    }

    fun toggleCategory(categoryId: Long) {
        val current = _state.value.selectedCategoryIds
        _state.value = _state.value.copy(
            selectedCategoryIds = if (categoryId in current) current - categoryId else current + categoryId
        )
    }

    suspend fun save(): Boolean {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.value = s.copy(titleError = true)
            return false
        }
        repository.saveTask(
            Task(id = if (taskId == -1L) 0 else taskId, title = s.title.trim(), description = s.description.trim()),
            s.selectedCategoryIds.toList()
        )
        return true
    }

    suspend fun delete() {
        if (taskId != -1L) {
            val s = _state.value
            repository.deleteTask(Task(id = taskId, title = s.title))
        }
    }

    class Factory(private val repository: TaskRepository, private val taskId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TaskDetailViewModel(repository, taskId) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailScreen(
    app: JengaApp,
    taskId: Long,
    onNavigateBack: () -> Unit
) {
    val viewModel: TaskDetailViewModel = viewModel(
        factory = TaskDetailViewModel.Factory(TaskRepository(app.database), taskId)
    )
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_task)) },
            text = { Text(stringResource(R.string.delete_task_confirm)) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        viewModel.delete()
                        onNavigateBack()
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isExistingTask) stringResource(R.string.edit_task) else stringResource(R.string.add_task))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.isExistingTask) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_task), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            if (viewModel.save()) onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.setTitle(it) },
                label = { Text(stringResource(R.string.task_title)) },
                isError = state.titleError,
                supportingText = if (state.titleError) {
                    { Text(stringResource(R.string.task_title_empty)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.setDescription(it) },
                label = { Text(stringResource(R.string.task_description)) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.allCategories.isNotEmpty()) {
                Text(stringResource(R.string.categories), style = MaterialTheme.typography.bodyLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.allCategories.forEach { category ->
                        FilterChip(
                            selected = category.id in state.selectedCategoryIds,
                            onClick = { viewModel.toggleCategory(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.save()) onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
