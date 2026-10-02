package com.mela.ussdrunner.ui.screens.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mela.ussdrunner.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(page: String, onBack: () -> Unit) {
    val title = when (page) {
        "about" -> "About"
        "privacy" -> "Privacy"
        else -> "Help"
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
            when (page) {
                "about" -> AboutBody()
                "privacy" -> PrivacyBody()
                else -> HelpBody()
            }
        }
    }
}

@Composable
private fun HelpBody() {
    Paragraph("Mela USSD Runner saves your carrier codes on this phone and sends them through Android telephony.")
    Heading("Running a code")
    Paragraph("Tap RUN on a preset. When the device supports it, Mela uses TelephonyManager.sendUssdRequest() and shows the reply. If the manufacturer or carrier blocks that API, Mela opens the system dialer instead — Android then shows the usual USSD dialog, which this app cannot read.")
    Heading("SIM cards")
    Paragraph("On dual-SIM phones you can pick Default, SIM 1, or SIM 2 per preset. Single-SIM phones hide that control. Slot names follow Android’s SIM order, which can differ by manufacturer.")
    Heading("Home screen widgets")
    Paragraph("Long-press the home screen, choose Widgets, then Mela USSD Runner. The single widget runs one preset. The grid widget holds up to four shortcuts. If you rename, change, or delete a preset, widgets refresh on the next update.")
    Heading("Device and carrier limits")
    Paragraph("USSD is a carrier feature. Some phones never return the reply to third-party apps. Some dual-SIM firmware ignores the selected slot. Tablets without telephony cannot send codes. Airplane mode, no SIM, a locked PIN, or no mobile network will all fail. Sample codes are examples only — confirm them with your operator.")
}

@Composable
private fun AboutBody() {
    Heading("Mela USSD Runner")
    Paragraph("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    Paragraph("Offline USSD shortcuts for Android. Presets stay on this device. There is no account and no server.")
    Paragraph("Minimum Android 8.0 (API 26). Native sendUssdRequest() is used when the OS exposes it; otherwise the encoded tel: dialer fallback is used.")
}

@Composable
private fun PrivacyBody() {
    Heading("Privacy")
    Paragraph("Saved names, USSD codes, descriptions, and settings are stored only in this app’s local database on your phone. They are not uploaded, synced, or sent to any server.")
    Paragraph("Mela does not collect contacts, SMS, call history, location, or analytics. Phone permissions are used solely to send USSD requests and to list SIM cards for dual-SIM selection.")
    Paragraph("Uninstalling the app removes its local data. Android backups, if you have them enabled, may include the local database on this device.")
}

@Composable
private fun Heading(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Paragraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
}
