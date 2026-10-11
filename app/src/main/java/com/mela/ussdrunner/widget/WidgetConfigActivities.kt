package com.mela.ussdrunner.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mela.ussdrunner.MelaApplication
import com.mela.ussdrunner.domain.model.Preset
import com.mela.ussdrunner.ui.theme.MelaTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        val initial = listOfNotNull(WidgetKeys.loadSingleId(this, appWidgetId))
        setContent {
            MelaTheme {
                WidgetPickerScreen(
                    title = "Choose a shortcut",
                    subtitle = "This widget runs one code. Tap a different shortcut to replace it.",
                    presets = app.container.presetRepository.observeAll(),
                    maxSelection = 1,
                    initialIds = initial,
                    onConfirm = { selected ->
                        WidgetKeys.saveSingleId(this, appWidgetId, selected.first().id)
                        val presets = app.container.presetRepository.observeAll().first()
                        SingleWidgetRenderer.render(
                            this,
                            AppWidgetManager.getInstance(this),
                            appWidgetId,
                            presets,
                        )
                        finishConfigured(appWidgetId)
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
        val initial = WidgetKeys.loadGridIds(this, appWidgetId).filterNotNull()
        setContent {
            MelaTheme {
                WidgetPickerScreen(
                    title = "Choose shortcuts",
                    subtitle = "Tap up to 6. The order you tap is the widget order, left to right, top to bottom.",
                    presets = app.container.presetRepository.observeAll(),
                    maxSelection = 6,
                    initialIds = initial,
                    onConfirm = { selected ->
                        WidgetKeys.saveGridIds(this, appWidgetId, selected.map { it.id })
                        val presets = app.container.presetRepository.observeAll().first()
                        GridWidgetRenderer.render(
                            this,
                            AppWidgetManager.getInstance(this),
                            appWidgetId,
                            presets,
                        )
                        finishConfigured(appWidgetId)
                    },
                )
            }
        }
    }
}

private fun Intent.appWidgetId(): Int =
    getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

private fun ComponentActivity.finishConfigured(appWidgetId: Int) {
    val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    setResult(Activity.RESULT_OK, result)
    finish()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetPickerScreen(
    title: String,
    subtitle: String,
    presets: Flow<List<Preset>>,
    maxSelection: Int,
    initialIds: List<String>,
    onConfirm: suspend (List<Preset>) -> Unit,
) {
    val list by presets.collectAsStateWithLifecycle(initialValue = emptyList())
    val selected = remember { mutableStateListOf<String>() }
    var seeded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(list, initialIds) {
        if (!seeded && list.isNotEmpty()) {
            selected.clear()
            selected.addAll(
                initialIds.filter { id -> list.any { it.id == id } }.distinct().take(maxSelection),
            )
            seeded = true
        }
    }
    val chosen = orderedWidgetPresets(selected, list, maxSelection)
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            if (maxSelection > 1) {
                Spacer(Modifier.height(12.dp))
                SlotPreview(chosen = chosen, slots = maxSelection)
            }
            Spacer(Modifier.height(12.dp))
            if (list.isEmpty()) {
                Text("Save a preset in Mela first, then come back to this widget.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(list, key = { it.id }) { preset ->
                        val index = selected.indexOf(preset.id)
                        val isOn = index >= 0
                        val atCap = !isOn && selected.size >= maxSelection
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable(enabled = isOn || !atCap) {
                                    val next = toggleWidgetSelection(selected.toList(), preset.id, maxSelection)
                                    selected.clear()
                                    selected.addAll(next)
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SelectionBadge(number = if (isOn) index + 1 else null)
                                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                    Text(preset.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        preset.ussdCode,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "${selected.size} of $maxSelection selected",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (chosen.isNotEmpty()) scope.launch { onConfirm(chosen) }
                    },
                    enabled = chosen.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(
                        when {
                            maxSelection == 1 -> "Use this shortcut"
                            chosen.size == 1 -> "Add 1 shortcut"
                            else -> "Add ${chosen.size} shortcuts"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotPreview(chosen: List<Preset>, slots: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0 until slots / 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0..1) {
                    val index = row * 2 + col
                    val preset = chosen.getOrNull(index)
                    Card(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset?.name ?: "Empty",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (preset == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionBadge(number: Int?) {
    val selected = number != null
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number?.toString() ?: "",
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}