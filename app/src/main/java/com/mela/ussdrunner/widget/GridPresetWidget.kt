package com.mela.ussdrunner.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.mela.ussdrunner.MelaApplication
import com.mela.ussdrunner.R
import com.mela.ussdrunner.domain.model.Preset
import kotlinx.coroutines.flow.first

class GridPresetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GridPresetWidget()
}

class GridPresetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as MelaApplication
        val presets = app.container.presetRepository.observeAll().first().associateBy { it.id }
        provideContent {
            val prefs = currentState<Preferences>()
            val cells = WidgetKeys.gridSlots.map { key -> prefs[key]?.let { presets[it] } }
            GlanceTheme { GridWidgetContent(cells) }
        }
    }
}

@Composable
private fun GridWidgetContent(cells: List<Preset?>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_card_bg))
            .padding(12.dp),
    ) {
        WidgetHeader()
        Spacer(GlanceModifier.height(8.dp))
        cells.chunked(2).forEachIndexed { rowIndex, pair ->
            if (rowIndex > 0) Spacer(GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                ShortcutTile(pair.getOrNull(0), GlanceModifier.defaultWeight().fillMaxHeight())
                Spacer(GlanceModifier.width(8.dp))
                ShortcutTile(pair.getOrNull(1), GlanceModifier.defaultWeight().fillMaxHeight())
            }
        }
    }
}
