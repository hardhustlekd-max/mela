package com.mela.ussdrunner.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mela.ussdrunner.MelaApplication
import com.mela.ussdrunner.domain.model.Preset
import com.mela.ussdrunner.ui.theme.MelaTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SingleWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.appWidgetId()
        setResult(Activity.RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()
        val app = application as MelaApplication
        setContent {
            MelaTheme {
                WidgetPickerScreen(
                    title = "Choose a USSD shortcut",
                    presets = app.container.presetRepository.observeAll(),
                    maxSelection = 1,
                    onConfirm = { selected ->
                        bindWidget(
                            activity = this@SingleWidgetConfigActivity,
                            appWidgetId = appWidgetId,
                            widget = SinglePresetWidget(),
                        ) { prefs ->
                            prefs[WidgetKeys.presetId] = selected.first().id
                        }
                    },
                )
            }
        }
    }
}

class GridWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.appWidgetId()
        setResult(Activity.RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()
        val app = application as MelaApplication
        setContent {
            MelaTheme {
                WidgetPickerScreen(
                    title = "Choose up to 6 shortcuts",
                    presets = app.container.presetRepository.observeAll(),
                    maxSelection = WidgetKeys.gridSlots.size,
                    onConfirm = { selected ->
                        bindWidget(
                            activity = this@GridWidgetConfigActivity,
                            appWidgetId = appWidgetId,
                            widget = GridPresetWidget(),
                        ) { prefs ->
                            WidgetKeys.gridSlots.forEachIndexed { index, key ->
                                val id = selected.getOrNull(index)?.id
                                if (id.isNullOrBlank()) prefs.remove(key) else prefs[key] = id
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun Intent.appWidgetId(): Int =
    getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

private suspend fun bindWidget(
    activity: ComponentActivity,
    appWidgetId: Int,
    widget: GlanceAppWidget,
    write: (MutablePreferences) -> Unit,
) {
    val manager = GlanceAppWidgetManager(activity)
    val glanceId = manager.getGlanceIdBy(appWidgetId)
    updateAppWidgetState(activity, glanceId, write)
    widget.update(activity, glanceId)
    val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    activity.setResult(Activity.RESULT_OK, result)
    activity.finish()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetPickerScreen(
    title: String,
    presets: Flow<List<Preset>>,
    maxSelection: Int,
    onConfirm: suspend (List<Preset>) -> Unit,
) {
    val list by presets.collectAsStateWithLifecycle(initialValue = emptyList())
    val selected = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            if (list.isEmpty()) {
                Text("Save a preset in Mela first, then come back to this widget.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(list, key = { it.id }) { preset ->
                        val isOn = selected.contains(preset.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable {
                                    if (isOn) {
                                        selected.remove(preset.id)
                                    } else if (maxSelection == 1) {
                                        selected.clear()
                                        selected.add(preset.id)
                                    } else if (selected.size < maxSelection) {
                                        selected.add(preset.id)
                                    }
                                },
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    if (isOn) "✓  ${preset.name}" else preset.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    preset.ussdCode,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val chosen = selected.mapNotNull { id -> list.find { it.id == id } }
                        if (chosen.isNotEmpty()) {
                            scope.launch { onConfirm(chosen) }
                        }
                    },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(if (maxSelection == 1) "Use this shortcut" else "Add ${selected.size} shortcuts")
                }
            }
        }
    }
}
