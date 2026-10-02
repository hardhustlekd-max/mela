package com.mela.ussdrunner.ui.screens.execution

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mela.ussdrunner.LocalAppContainer
import com.mela.ussdrunner.telephony.PermissionHelper

@Composable
fun ExecutionScreen(
    presetId: String,
    onClose: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm: ExecutionViewModel = viewModel(
        factory = ExecutionViewModel.factory(
            context.applicationContext as Application,
            presetId,
            container.presetRepository,
            container.settingsRepository,
            container.ussdExecutor,
            container.widgetUpdater,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { vm.onPermissionsUpdated() }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val preset = state.preset
            Text(
                text = preset?.name ?: "USSD",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = preset?.ussdCode.orEmpty(),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(28.dp))

            when (val phase = state.phase) {
                RunPhase.Loading, RunPhase.Connecting -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Connecting…")
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel")
                    }
                }
                RunPhase.NeedPermission -> {
                    Text(
                        "Mela needs phone permission to send USSD codes.",
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    state.missingPermissions.forEach { perm ->
                        Text(
                            PermissionHelper.rationaleFor(perm),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            permissionLauncher.launch(PermissionHelper.runtimePermissions.toTypedArray())
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) { Text("Grant permission") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { context.startActivity(PermissionHelper.appSettingsIntent(context)) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) { Text("Open Android settings") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel")
                    }
                }
                is RunPhase.Success -> {
                    Text("USSD response", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        phase.body.ifBlank { "(Empty response from the network)" },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("Close")
                    }
                }
                is RunPhase.Dialer -> {
                    Text("Opened the system USSD interface", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        phase.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("Close")
                    }
                }
                is RunPhase.Error -> {
                    Text(phase.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    if (phase.canFallback) {
                        Text(
                            "Check that:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    phase.hints.forEach { hint ->
                        Text("• $hint", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(20.dp))
                    if (phase.canFallback) {
                        Button(onClick = vm::fallback, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                            Text("Use dialer fallback")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedButton(onClick = vm::start, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("Try again")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
                }
                RunPhase.Missing -> {
                    Text("That preset is no longer saved.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onClose) { Text("Close") }
                }
            }
        }
    }
}
