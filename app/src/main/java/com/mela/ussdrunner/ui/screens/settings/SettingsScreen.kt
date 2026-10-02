package com.mela.ussdrunner.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.mela.ussdrunner.domain.model.SimPreference
import com.mela.ussdrunner.domain.model.ThemeMode
import com.mela.ussdrunner.telephony.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(container.settingsRepository),
    )
    val settings by vm.settings.collectAsStateWithLifecycle()
    var dualSim by remember { mutableStateOf(container.simManager.isDualSim()) }
    var canReadSims by remember { mutableStateOf(container.simManager.canReadSubscriptions()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        canReadSims = container.simManager.canReadSubscriptions()
        dualSim = container.simManager.isDualSim()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (dualSim) {
                Text("Default SIM", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    FilterChip(
                        selected = settings.defaultSim == SimPreference.DEFAULT,
                        onClick = { vm.setSim(SimPreference.DEFAULT) },
                        label = { Text("Default") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = settings.defaultSim == SimPreference.SLOT_0,
                        onClick = { vm.setSim(SimPreference.SLOT_0) },
                        label = { Text("SIM 1") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = settings.defaultSim == SimPreference.SLOT_1,
                        onClick = { vm.setSim(SimPreference.SLOT_1) },
                        label = { Text("SIM 2") },
                    )
                }
            } else if (!canReadSims) {
                Text("Default SIM", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Grant phone-state permission to list SIM cards on dual-SIM phones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        permissionLauncher.launch(PermissionHelper.runtimePermissions.toTypedArray())
                    },
                ) { Text("Allow phone permission") }
                Spacer(Modifier.height(12.dp))
            }
            Text("Theme", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                FilterChip(
                    selected = settings.themeMode == ThemeMode.SYSTEM,
                    onClick = { vm.setTheme(ThemeMode.SYSTEM) },
                    label = { Text("System") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = settings.themeMode == ThemeMode.LIGHT,
                    onClick = { vm.setTheme(ThemeMode.LIGHT) },
                    label = { Text("Light") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = settings.themeMode == ThemeMode.DARK,
                    onClick = { vm.setTheme(ThemeMode.DARK) },
                    label = { Text("Dark") },
                )
            }
            SettingSwitch(
                title = "Confirm before running USSD",
                checked = settings.confirmBeforeRun,
                onChange = vm::setConfirm,
            )
            SettingSwitch(
                title = "Show USSD code on cards",
                checked = settings.showUssdOnCards,
                onChange = vm::setShowCode,
            )
            LinkRow("Help", onHelp)
            LinkRow("About", onAbout)
            LinkRow("Privacy", onPrivacy)
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
