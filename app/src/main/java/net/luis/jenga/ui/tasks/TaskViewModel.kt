package net.luis.jenga.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.luis.jenga.data.local.AppDatabase
import net.luis.jenga.data.repository.TaskRepository
import net.luis.jenga.domain.model.Category
import net.luis.jenga.domain.model.Task
import net.luis.jenga.util.groupedByCategory

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = ""
) {
    val filteredTasks: List<Task>
        get() = if (searchQuery.isBlank()) tasks
        else tasks.filter { it.title.contains(searchQuery, ignoreCase = true) }

    val tasksByCategory: Map<Category?, List<Task>>
        get() = filteredTasks.groupedByCategory()
}

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.allTasks, repository.allCategories) { tasks, categories ->
                _uiState.value.copy(tasks = tasks, categories = categories)
            }.collect { _uiState.value = it }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun saveCategory(name: String) {
        viewModelScope.launch { repository.saveCategory(Category(name = name)) }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TaskViewModel(TaskRepository(database)) as T
    }
}
