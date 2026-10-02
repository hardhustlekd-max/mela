package com.mela.ussdrunner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mela.ussdrunner.di.AppContainer
import com.mela.ussdrunner.ui.navigation.MelaNavHost
import com.mela.ussdrunner.ui.theme.MelaTheme

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startPreset = intent.getStringExtra(EXTRA_PRESET_ID)
        setContent {
            val app = application as MelaApplication
            MelaRoot(app.container, startPreset)
        }
    }

    companion object {
        const val EXTRA_PRESET_ID = "preset_id"
    }
}

@Composable
fun MelaRoot(container: AppContainer, startPresetId: String?) {
    val settings by container.settingsRepository.settings.collectAsState(
        initial = com.mela.ussdrunner.domain.model.AppSettings(),
    )
    CompositionLocalProvider(LocalAppContainer provides container) {
        MelaTheme(themeMode = settings.themeMode) {
            val start = remember(startPresetId) {
                if (startPresetId.isNullOrBlank()) "home" else "run/$startPresetId"
            }
            MelaNavHost(startDestination = start)
        }
    }
}
