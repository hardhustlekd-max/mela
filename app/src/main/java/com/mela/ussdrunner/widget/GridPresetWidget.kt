package com.mela.ussdrunner.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import com.mela.ussdrunner.MelaApplication
import com.mela.ussdrunner.domain.model.Preset
import kotlinx.coroutines.flow.first

class GridPresetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GridPresetWidget()
}

class GridPresetWidget : GlanceAppWidget() {
    // Exact size lets the layout shrink gracefully when the widget is short.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as MelaApplication
        val presets = app.container.presetRepository.observeAll().first().associateBy { it.id }
        provideContent {
            val prefs = currentState<Preferences>()
            val cells = WidgetKeys.gridSlots.map { key -> prefs[key]?.let { presets[it] } }
            GlanceTheme(colors = MelaWidgetColors) { GridWidgetContent(cells) }
        }
    }
}

@Composable
private fun GridWidgetContent(cells: List<Preset?>) {
    val rows = cells.chunked(2)
    val gap = 8.dp
    // Title bar + padding take roughly 64dp; whatever is left is shared by the rows.
    val tileHeight = (LocalSize.current.height - 64.dp - gap * (rows.size - 1)) / rows.size
    val compact = tileHeight < 52.dp

    Scaffold(
        backgroundColor = GlanceTheme.colors.widgetBackground,
        titleBar = { WidgetTitleBar() },
        horizontalPadding = 12.dp,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            rows.forEachIndexed { index, pair ->
                if (index > 0) Spacer(GlanceModifier.height(gap))
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    ShortcutTile(pair.getOrNull(0), GlanceModifier.defaultWeight().fillMaxHeight(), compact)
                    Spacer(GlanceModifier.width(gap))
                    ShortcutTile(pair.getOrNull(1), GlanceModifier.defaultWeight().fillMaxHeight(), compact)
                }
            }
        }
    }
}
