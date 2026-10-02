package com.mela.ussdrunner.ui.screens.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mela.ussdrunner.LocalAppContainer
import com.mela.ussdrunner.domain.model.Preset
import com.mela.ussdrunner.domain.model.PresetSort
import com.mela.ussdrunner.telephony.PermissionHelper
import com.mela.ussdrunner.ui.components.EmptyState
import com.mela.ussdrunner.ui.components.PresetCard
import com.mela.ussdrunner.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onRun: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            container.presetRepository,
            container.settingsRepository,
            container.widgetUpdater,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var missingPermissions by remember {
        mutableStateOf(PermissionHelper.missing(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        missingPermissions = PermissionHelper.missing(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mela USSD Runner") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add preset")
            }
        },
    ) { padding ->
        HomeContent(
            state = state,
            padding = padding,
            missingPermissions = missingPermissions,
            onGrantPermission = {
                permissionLauncher.launch(PermissionHelper.runtimePermissions.toTypedArray())
            },
            onOpenSettings = {
                val intent = PermissionHelper.appSettingsIntent(context)
                if (context !is Activity) intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
            onQuery = vm::setQuery,
            onAll = vm::showAll,
            onFavorites = vm::showFavorites,
            onRecent = vm::showRecent,
            onCategory = vm::setCategory,
            onSort = vm::setSort,
            onRun = { preset ->
                if (state.settings.confirmBeforeRun) vm.requestRun(preset) else onRun(preset.id)
            },
            onEdit = { onEdit(it.id) },
            onDuplicate = vm::duplicate,
            onDelete = vm::requestDelete,
            onFavorite = vm::toggleFavorite,
        )
    }

    state.pendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = vm::cancelDelete,
            title = { Text("Delete preset?") },
            text = { Text("“${preset.name}” will be removed from this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = vm::confirmDelete) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelDelete) { Text("Cancel") }
            },
        )
    }
    state.pendingRun?.let { preset ->
        AlertDialog(
            onDismissRequest = vm::cancelRun,
            title = { Text("Run ${preset.name}?") },
            text = { Text("This will send ${preset.ussdCode} through your phone’s telephony system.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = preset.id
                    vm.cancelRun()
                    onRun(id)
                }) { Text("Run") }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelRun) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    padding: PaddingValues,
    missingPermissions: List<String>,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuery: (String) -> Unit,
    onAll: () -> Unit,
    onFavorites: () -> Unit,
    onRecent: () -> Unit,
    onCategory: (String?) -> Unit,
    onSort: (PresetSort) -> Unit,
    onRun: (Preset) -> Unit,
    onEdit: (Preset) -> Unit,
    onDuplicate: (Preset) -> Unit,
    onDelete: (Preset) -> Unit,
    onFavorite: (Preset) -> Unit,
) {
    val filtering = state.query.isNotBlank() || state.favoritesOnly || state.recentOnly || state.category != null
    if (state.loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (missingPermissions.isNotEmpty()) {
            item {
                PermissionBanner(
                    missing = missingPermissions,
                    onGrant = onGrantPermission,
                    onSettings = onOpenSettings,
                )
            }
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search presets…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
            )
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = !filtering, onClick = onAll, label = { Text("All") })
                FilterChip(selected = state.favoritesOnly, onClick = onFavorites, label = { Text("Favorites") })
                FilterChip(selected = state.recentOnly, onClick = onRecent, label = { Text("Recent") })
                state.categories.forEach { cat ->
                    FilterChip(
                        selected = state.category == cat,
                        onClick = { onCategory(if (state.category == cat) null else cat) },
                        label = { Text(cat) },
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.sort == PresetSort.FAVORITES_FIRST,
                    onClick = { onSort(PresetSort.FAVORITES_FIRST) },
                    label = { Text("Favorites first") },
                )
                FilterChip(
                    selected = state.sort == PresetSort.NAME,
                    onClick = { onSort(PresetSort.NAME) },
                    label = { Text("Name") },
                )
                FilterChip(
                    selected = state.sort == PresetSort.RECENTLY_USED,
                    onClick = { onSort(PresetSort.RECENTLY_USED) },
                    label = { Text("Last used") },
                )
                FilterChip(
                    selected = state.sort == PresetSort.NEWEST,
                    onClick = { onSort(PresetSort.NEWEST) },
                    label = { Text("Newest") },
                )
            }
        }

        if (!filtering) {
            if (state.favorites.isNotEmpty()) {
                item { SectionHeader("Favorites") }
                items(state.favorites, key = { "fav-${it.id}" }) { preset ->
                    PresetCardItem(preset, state.settings.showUssdOnCards, onRun, onEdit, onDuplicate, onDelete, onFavorite)
                }
            }
            if (state.recent.isNotEmpty()) {
                item { SectionHeader("Recent") }
                items(state.recent, key = { "rec-${it.id}" }) { preset ->
                    PresetCardItem(preset, state.settings.showUssdOnCards, onRun, onEdit, onDuplicate, onDelete, onFavorite)
                }
            }
            if (state.others.isNotEmpty()) {
                item { SectionHeader(if (state.favorites.isEmpty() && state.recent.isEmpty()) "Presets" else "More") }
                items(state.others, key = { "all-${it.id}" }) { preset ->
                    PresetCardItem(preset, state.settings.showUssdOnCards, onRun, onEdit, onDuplicate, onDelete, onFavorite)
                }
            }
            if (state.favorites.isEmpty() && state.recent.isEmpty() && state.others.isEmpty()) {
                item {
                    EmptyState(
                        title = "No presets yet",
                        body = "Add a USSD code you use often. Sample codes are examples — verify them with your carrier.",
                    )
                }
            }
        } else if (state.presets.isEmpty()) {
            item {
                EmptyState(
                    title = "No matching presets",
                    body = "Try another name, code, or category.",
                )
            }
        } else {
            items(state.presets, key = { it.id }) { preset ->
                PresetCardItem(preset, state.settings.showUssdOnCards, onRun, onEdit, onDuplicate, onDelete, onFavorite)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PermissionBanner(
    missing: List<String>,
    onGrant: () -> Unit,
    onSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Phone permission", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                missing.firstOrNull()?.let { PermissionHelper.rationaleFor(it) }
                    ?: "Mela needs phone permission to send USSD codes and list SIM cards.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onGrant) { Text("Allow") }
                OutlinedButton(onClick = onSettings) { Text("Settings") }
            }
        }
    }
}

@Composable
private fun PresetCardItem(
    preset: Preset,
    showCode: Boolean,
    onRun: (Preset) -> Unit,
    onEdit: (Preset) -> Unit,
    onDuplicate: (Preset) -> Unit,
    onDelete: (Preset) -> Unit,
    onFavorite: (Preset) -> Unit,
) {
    PresetCard(
        preset = preset,
        showCode = showCode,
        onRun = { onRun(preset) },
        onEdit = { onEdit(preset) },
        onDuplicate = { onDuplicate(preset) },
        onDelete = { onDelete(preset) },
        onToggleFavorite = { onFavorite(preset) },
    )
}
