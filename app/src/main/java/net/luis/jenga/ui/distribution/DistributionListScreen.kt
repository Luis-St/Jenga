package net.luis.jenga.ui.distribution

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.jenga.JengaApp
import net.luis.jenga.R
import net.luis.jenga.domain.model.Distribution

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributionListScreen(
    app: JengaApp,
    onNavigateBack: () -> Unit,
    onNavigateToDistribution: (Long) -> Unit,
    onAddDistribution: () -> Unit
) {
    val viewModel: DistributionViewModel = viewModel(factory = DistributionViewModel.Factory(app.database))
    val state by viewModel.listState.collectAsState()
    var distributionToDelete by remember { mutableStateOf<Distribution?>(null) }

    distributionToDelete?.let { dist ->
        AlertDialog(
            onDismissRequest = { distributionToDelete = null },
            title = { Text(stringResource(R.string.delete_distribution)) },
            text = { Text(stringResource(R.string.delete_distribution_confirm)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteDistribution(dist)
                    distributionToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { distributionToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.distributions)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddDistribution) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_distribution))
            }
        }
    ) { innerPadding ->
        if (state.distributions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.no_distributions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(state.distributions, key = { it.id }) { dist ->
                    ListItem(
                        headlineContent = { Text(dist.name) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.distribution_total, dist.totalBlocks, dist.tasksNeeded),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { distributionToDelete = dist }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_distribution),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.clickable { onNavigateToDistribution(dist.id) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
