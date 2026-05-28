package net.luis.jenga.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.luis.jenga.JengaApp
import net.luis.jenga.data.repository.DistributionRepository
import net.luis.jenga.data.repository.SettingsRepository
import net.luis.jenga.data.repository.TaskRepository
import net.luis.jenga.domain.model.Category
import net.luis.jenga.domain.model.Distribution
import net.luis.jenga.domain.model.Task
import net.luis.jenga.util.naturalOrder

data class TaskSlot(
    val slotIndex: Int,
    val blockCount: Int,
    val assignedTask: Task? = null
)

data class GameSetupUiState(
    val blockCount: Int = 52,
    val blockCountText: String = "52",
    val selectedDistribution: Distribution? = null,
    val taskSlots: List<TaskSlot> = emptyList(),
    val allTasks: List<Task> = emptyList(),
    val allDistributions: List<Distribution> = emptyList(),
    val availableDistributions: List<Distribution> = emptyList(),
    val isLoaded: Boolean = false
) {
    val canStart: Boolean
        get() = selectedDistribution != null && taskSlots.isNotEmpty() && taskSlots.all { it.assignedTask != null }
    val tasksNeeded: Int get() = taskSlots.size

    /** Distinct categories used by the current tasks, naturally ordered by name. */
    val availableCategories: List<Category>
        get() = allTasks.flatMap { it.categories }
            .distinctBy { it.id }
            .sortedWith(compareBy(naturalOrder) { it.name })
}

data class GamePlayUiState(
    val blockCount: Int = 52,
    val blockToTask: Map<Int, Task> = emptyMap(),
    val inputText: String = "",
    val currentTask: Task? = null,
    val inputError: String? = null
)

class GameViewModel(
    private val taskRepository: TaskRepository,
    private val distributionRepository: DistributionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _setupState = MutableStateFlow(GameSetupUiState())
    val setupState: StateFlow<GameSetupUiState> = _setupState.asStateFlow()

    private val _playState = MutableStateFlow(GamePlayUiState())
    val playState: StateFlow<GamePlayUiState> = _playState.asStateFlow()

    init {
        viewModelScope.launch {
            val defaultBlocks = settingsRepository.defaultBlockCount.first()
            _setupState.value = _setupState.value.copy(
                blockCount = defaultBlocks,
                blockCountText = defaultBlocks.toString()
            )
            combine(taskRepository.allTasks, distributionRepository.allDistributions) { tasks, distributions ->
                val current = _setupState.value
                val selected = current.selectedDistribution?.let { sel ->
                    distributions.find { it.id == sel.id && it.totalBlocks == current.blockCount }
                }
                current.copy(
                    allTasks = tasks,
                    allDistributions = distributions,
                    availableDistributions = distributions.filter { it.totalBlocks == current.blockCount },
                    selectedDistribution = selected,
                    taskSlots = rebuildSlots(selected, current.taskSlots),
                    isLoaded = true
                )
            }.collect { _setupState.value = it }
        }
    }

    fun setBlockCountText(text: String) {
        val current = _setupState.value
        val parsed = text.toIntOrNull()
        if (parsed != null && parsed in 1..999) {
            val selected = current.selectedDistribution?.takeIf { it.totalBlocks == parsed }
            _setupState.value = current.copy(
                blockCount = parsed,
                blockCountText = text,
                availableDistributions = current.allDistributions.filter { it.totalBlocks == parsed },
                selectedDistribution = selected,
                taskSlots = rebuildSlots(selected, current.taskSlots)
            )
        } else {
            _setupState.value = current.copy(blockCountText = text)
        }
    }

    fun selectDistribution(distribution: Distribution) {
        val current = _setupState.value
        _setupState.value = current.copy(
            selectedDistribution = distribution,
            taskSlots = rebuildSlots(distribution, current.taskSlots)
        )
    }

    fun assignTaskToSlot(slotIndex: Int, task: Task?) {
        val current = _setupState.value
        val updated = current.taskSlots.toMutableList()
        if (slotIndex in updated.indices) {
            updated[slotIndex] = updated[slotIndex].copy(assignedTask = task)
        }
        _setupState.value = current.copy(taskSlots = updated)
    }

    /** Assigns a random task (optionally restricted to [category]) to a single slot. */
    fun assignRandomToSlot(slotIndex: Int, category: Category?) {
        val pool = tasksForCategory(category)
        if (pool.isEmpty()) return
        assignTaskToSlot(slotIndex, pool.random())
    }

    /**
     * Fills every still-empty slot with a random task (optionally restricted to [category]),
     * avoiding repeats while the pool lasts before drawing a fresh shuffled bag.
     */
    fun fillUnassignedRandomly(category: Category?) {
        val current = _setupState.value
        val pool = tasksForCategory(category)
        if (pool.isEmpty()) return
        val updated = current.taskSlots.toMutableList()
        var bag = emptyList<Task>()
        updated.forEachIndexed { index, slot ->
            if (slot.assignedTask == null) {
                if (bag.isEmpty()) bag = pool.shuffled()
                updated[index] = slot.copy(assignedTask = bag.first())
                bag = bag.drop(1)
            }
        }
        _setupState.value = current.copy(taskSlots = updated)
    }

    private fun tasksForCategory(category: Category?): List<Task> {
        val tasks = _setupState.value.allTasks
        return if (category == null) tasks
        else tasks.filter { task -> task.categories.any { it.id == category.id } }
    }

    fun startGame() {
        val setup = _setupState.value
        val slots = setup.taskSlots

        val allBlockNumbers = (1..setup.blockCount).toMutableList()
        allBlockNumbers.shuffle()

        val blockToTask = mutableMapOf<Int, Task>()
        var blockIdx = 0
        slots.forEach { slot ->
            val task = slot.assignedTask ?: return@forEach
            repeat(slot.blockCount) {
                if (blockIdx < allBlockNumbers.size) {
                    blockToTask[allBlockNumbers[blockIdx]] = task
                    blockIdx++
                }
            }
        }

        _playState.value = GamePlayUiState(
            blockCount = setup.blockCount,
            blockToTask = blockToTask
        )
    }

    fun setInputText(text: String) {
        _playState.value = _playState.value.copy(inputText = text, inputError = null, currentTask = null)
    }

    fun lookupBlock() {
        val state = _playState.value
        val num = state.inputText.toIntOrNull()
        if (num == null || num !in 1..state.blockCount) {
            _playState.value = state.copy(inputError = num.toString())
            return
        }
        _playState.value = state.copy(currentTask = state.blockToTask[num], inputError = null)
    }

    private fun rebuildSlots(
        distribution: Distribution?,
        previousSlots: List<TaskSlot>
    ): List<TaskSlot> {
        val groups = distribution?.groups ?: return emptyList()

        val expanded = groups.flatMap { group ->
            List(group.groupCount) { group.blockCount }
        }

        return expanded.mapIndexed { index, bc ->
            val prevTask = previousSlots.getOrNull(index)?.assignedTask
            TaskSlot(slotIndex = index, blockCount = bc, assignedTask = prevTask)
        }
    }

    class Factory(private val app: JengaApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GameViewModel(
                TaskRepository(app.database),
                DistributionRepository(app.database),
                SettingsRepository(app)
            ) as T
    }
}
