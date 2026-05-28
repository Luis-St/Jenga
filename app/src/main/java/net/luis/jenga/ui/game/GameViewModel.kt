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
import net.luis.jenga.domain.model.Distribution
import net.luis.jenga.domain.model.Task

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
