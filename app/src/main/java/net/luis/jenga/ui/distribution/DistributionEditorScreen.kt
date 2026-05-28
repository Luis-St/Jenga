package net.luis.jenga.ui.distribution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
import net.luis.jenga.data.repository.DistributionRepository
import net.luis.jenga.domain.model.Distribution
import net.luis.jenga.domain.model.DistributionGroup

private data class EditorUiState(
    val name: String = "",
    val nameError: Boolean = false,
    val isLoading: Boolean = true,
    val isExisting: Boolean = false
)

private class DistributionEditorViewModel(
    private val repository: DistributionRepository,
    private val distributionId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    val groups = mutableStateListOf<DistributionGroup>()

    init {
        viewModelScope.launch {
            if (distributionId != -1L) {
                val dist = repository.getDistributionById(distributionId)
                if (dist != null) {
                    _state.value = EditorUiState(name = dist.name, isLoading = false, isExisting = true)
                    groups.addAll(dist.groups)
                } else {
                    initDefault()
                }
            } else {
                initDefault()
            }
        }
    }

    private fun initDefault() {
        _state.value = EditorUiState(isLoading = false)
        groups.add(DistributionGroup(groupCount = 52, blockCount = 1))
    }

    fun setName(value: String) {
        _state.value = _state.value.copy(name = value, nameError = false)
    }

    fun addGroup() {
        groups.add(DistributionGroup(groupCount = 1, blockCount = 1))
    }

    fun removeGroup(index: Int) {
        if (groups.size > 1) groups.removeAt(index)
    }

    fun updateGroup(index: Int, groupCount: Int, blockCount: Int) {
        if (index in groups.indices) {
            groups[index] = DistributionGroup(
                groupCount = groupCount.coerceAtLeast(1),
                blockCount = blockCount.coerceAtLeast(1)
            )
        }
    }

    val totalBlocks: Int get() = groups.sumOf { it.groupCount * it.blockCount }
    val tasksNeeded: Int get() = groups.sumOf { it.groupCount }

    suspend fun save(): Boolean {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(nameError = true)
            return false
        }
        if (groups.any { it.groupCount < 1 || it.blockCount < 1 }) return false
        repository.saveDistribution(
            Distribution(
                id = if (distributionId == -1L) 0 else distributionId,
                name = s.name.trim(),
                groups = groups.toList()
            )
        )
        return true
    }

    class Factory(private val repository: DistributionRepository, private val id: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DistributionEditorViewModel(repository, id) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributionEditorScreen(
    app: JengaApp,
    distributionId: Long,
    onNavigateBack: () -> Unit
) {
    val viewModel: DistributionEditorViewModel = viewModel(
        factory = DistributionEditorViewModel.Factory(DistributionRepository(app.database), distributionId)
    )
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isExisting) stringResource(R.string.edit_distribution) else stringResource(R.string.add_distribution))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch { if (viewModel.save()) onNavigateBack() }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                    }
                }
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = { scope.launch { if (viewModel.save()) onNavigateBack() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
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
                value = state.name,
                onValueChange = { viewModel.setName(it) },
                label = { Text(stringResource(R.string.distribution_name)) },
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.distribution_name_empty)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.distribution_total, viewModel.totalBlocks, viewModel.tasksNeeded),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(stringResource(R.string.groups), style = MaterialTheme.typography.bodyLarge)

            viewModel.groups.forEachIndexed { index, group ->
                GroupRow(
                    group = group,
                    canRemove = viewModel.groups.size > 1,
                    onGroupChange = { gc, bc -> viewModel.updateGroup(index, gc, bc) },
                    onRemove = { viewModel.removeGroup(index) }
                )
            }

            FilledTonalButton(
                onClick = { viewModel.addGroup() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.add_group))
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: DistributionGroup,
    canRemove: Boolean,
    onGroupChange: (Int, Int) -> Unit,
    onRemove: () -> Unit
) {
    var groupCountText by remember(group.groupCount) { mutableStateOf(group.groupCount.toString()) }
    var blockCountText by remember(group.blockCount) { mutableStateOf(group.blockCount.toString()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = groupCountText,
            onValueChange = { input ->
                groupCountText = input
                val gc = input.toIntOrNull() ?: return@OutlinedTextField
                onGroupChange(gc, blockCountText.toIntOrNull() ?: group.blockCount)
            },
            label = { Text(stringResource(R.string.group_count_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Text("×", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = blockCountText,
            onValueChange = { input ->
                blockCountText = input
                val bc = input.toIntOrNull() ?: return@OutlinedTextField
                onGroupChange(groupCountText.toIntOrNull() ?: group.groupCount, bc)
            },
            label = { Text(stringResource(R.string.blocks_per_group_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}
