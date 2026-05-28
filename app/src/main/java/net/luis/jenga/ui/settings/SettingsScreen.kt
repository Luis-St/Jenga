package net.luis.jenga.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.luis.jenga.BuildConfig
import net.luis.jenga.JengaApp
import net.luis.jenga.R
import net.luis.jenga.data.export.DataExporter
import net.luis.jenga.data.export.DataImporter
import net.luis.jenga.data.repository.SettingsRepository
import net.luis.jenga.domain.model.AppLanguage
import net.luis.jenga.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    app: JengaApp,
    onNavigateBack: () -> Unit
) {
    val factory = remember { SettingsViewModel.Factory(SettingsRepository(app)) }
    val viewModel: SettingsViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    var blockCountText by remember { mutableStateOf(uiState.defaultBlockCount.toString()) }
    LaunchedEffect(uiState.defaultBlockCount) {
        blockCountText = uiState.defaultBlockCount.toString()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportSuccessMsg = stringResource(R.string.export_success)
    val exportErrorMsg = stringResource(R.string.export_error)
    val importSuccessMsg = stringResource(R.string.import_success)
    val importErrorMsg = stringResource(R.string.import_error)
    val restoreSuccessMsg = stringResource(R.string.restore_success)
    val restoreErrorMsg = stringResource(R.string.restore_error)

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val hasBackup by app.importBackup.collectAsState()
    var showRestoreDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            DataExporter.export(app.database, outputStream)
                        }
                    }
                    snackbarHostState.showSnackbar(exportSuccessMsg)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(exportErrorMsg)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) pendingImportUri = uri
    }

    pendingImportUri?.let { uriToImport ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.import_confirm_title)) },
            text = { Text(stringResource(R.string.import_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    pendingImportUri = null
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                app.importBackup.value = DataExporter.snapshot(app.database)
                                context.contentResolver.openInputStream(uriToImport)?.use { inputStream ->
                                    DataImporter.importData(app.database, inputStream)
                                }
                            }
                            snackbarHostState.showSnackbar(importSuccessMsg)
                        } catch (e: Exception) {
                            app.importBackup.value = null
                            snackbarHostState.showSnackbar(importErrorMsg)
                        }
                    }
                }) { Text(stringResource(R.string.import_data)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    showRestoreDialog = false
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val backup = app.importBackup.value ?: return@withContext
                                DataImporter.importFromData(app.database, backup)
                            }
                            app.importBackup.value = null
                            snackbarHostState.showSnackbar(restoreSuccessMsg)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(restoreErrorMsg)
                        }
                    }
                }) { Text(stringResource(R.string.restore_data)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SettingsSectionHeader(stringResource(R.string.appearance))

            Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            val themeModes = ThemeMode.entries
            val themeLabels = listOf(
                stringResource(R.string.light),
                stringResource(R.string.dark),
                stringResource(R.string.system_default)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeModes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeModes.size)
                    ) { Text(themeLabels[index]) }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            val languages = AppLanguage.entries
            val langLabels = listOf(stringResource(R.string.system_default), "English", "Deutsch")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                languages.forEachIndexed { index, lang ->
                    SegmentedButton(
                        selected = uiState.appLanguage == lang,
                        onClick = { viewModel.setAppLanguage(lang) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = languages.size)
                    ) { Text(langLabels[index]) }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dynamic_colors), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.dynamic_colors_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = uiState.dynamicColors, onCheckedChange = { viewModel.setDynamicColors(it) })
            }

            Spacer(Modifier.height(24.dp))
            SettingsSectionHeader(stringResource(R.string.game))

            Text(stringResource(R.string.default_block_count), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.default_block_count_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = blockCountText,
                onValueChange = { input ->
                    blockCountText = input
                    input.toIntOrNull()?.let { if (it in 1..999) viewModel.setDefaultBlockCount(it) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            SettingsSectionHeader(stringResource(R.string.data))

            FilledTonalButton(
                onClick = { exportLauncher.launch("jenga_backup.json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.export_data))
            }

            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.import_data))
            }

            if (hasBackup != null) {
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = { showRestoreDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.restore_data))
                }
            }

            Spacer(Modifier.height(24.dp))
            SettingsSectionHeader(stringResource(R.string.about))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.app_version), style = MaterialTheme.typography.bodyLarge)
                Text(
                    BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
