package net.luis.jenga.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.luis.jenga.R
import net.luis.jenga.domain.model.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit,
    onStartGame: () -> Unit,
    onManageDistributions: () -> Unit
) {
    val state by viewModel.setupState.collectAsState()

    var showDistributionMenu by remember { mutableStateOf(false) }
    var slotPickerIndex by remember { mutableStateOf<Int?>(null) }
    var taskSearchQuery by remember { mutableStateOf("") }

    slotPickerIndex?.let { slotIdx ->
        TaskPickerDialog(
            tasks = state.allTasks,
            searchQuery = taskSearchQuery,
            onSearchChange = { taskSearchQuery = it },
            onSelect = { task ->
                viewModel.assignTaskToSlot(slotIdx, task)
                slotPickerIndex = null
                taskSearchQuery = ""
            },
            onDismiss = {
                slotPickerIndex = null
                taskSearchQuery = ""
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.game_setup)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.block_count), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.blockCountText,
                        onValueChange = { viewModel.setBlockCountText(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(onClick = { viewModel.setBlockCount(52) }) {
                        Text(stringResource(R.string.default_52))
                    }
                }
            }

            item {
                Text(stringResource(R.string.distribution), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showDistributionMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.selectedDistribution?.name
                                    ?: stringResource(R.string.no_distribution),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showDistributionMenu,
                            onDismissRequest = { showDistributionMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.no_distribution)) },
                                onClick = {
                                    viewModel.selectDistribution(null)
                                    showDistributionMenu = false
                                }
                            )
                            state.allDistributions.forEach { dist ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(dist.name)
                                            Text(
                                                stringResource(R.string.distribution_total, dist.totalBlocks, dist.tasksNeeded),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectDistribution(dist)
                                        showDistributionMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onManageDistributions) {
                        Icon(Icons.Default.EditNote, contentDescription = stringResource(R.string.manage_distributions))
                    }
                }

                if (state.selectedDistribution != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.tasks_needed, state.tasksNeeded),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.taskSlots.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.task_slots), style = MaterialTheme.typography.bodyLarge)
                }
                itemsIndexed(state.taskSlots, key = { idx, _ -> idx }) { index, slot ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (slot.assignedTask != null)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth().clickable { slotPickerIndex = index }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.slot_label, index + 1, slot.blockCount),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    slot.assignedTask?.title ?: stringResource(R.string.pick_task),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (slot.assignedTask != null)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.startGame()
                        onStartGame()
                    },
                    enabled = state.canStart,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.start_game), style = MaterialTheme.typography.titleMedium)
                }
                if (!state.canStart && state.taskSlots.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.all_slots_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskPickerDialog(
    tasks: List<Task>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelect: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    val filtered = if (searchQuery.isBlank()) tasks
    else tasks.filter { it.title.contains(searchQuery, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_task)) },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(stringResource(R.string.search_tasks)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(filtered.size) { idx ->
                        val task = filtered[idx]
                        ListItem(
                            headlineContent = { Text(task.title) },
                            supportingContent = if (task.description.isNotBlank()) {
                                { Text(task.description, maxLines = 1) }
                            } else null,
                            modifier = Modifier.clickable { onSelect(task) }
                        )
                        if (idx < filtered.lastIndex) HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
