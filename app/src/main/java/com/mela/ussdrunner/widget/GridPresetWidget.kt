package com.mela.ussdrunner.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mela.ussdrunner.MelaApplication
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
            val cells = listOf(
                prefs[WidgetKeys.presetId0],
                prefs[WidgetKeys.presetId1],
                prefs[WidgetKeys.presetId2],
                prefs[WidgetKeys.presetId3],
            ).map { idValue -> idValue?.let { presets[it] } }
            GlanceTheme { GridWidgetContent(cells) }
        }
    }
}

@Composable
private fun GridWidgetContent(cells: List<Preset?>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF10211F)))
            .padding(6.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            GridCell(cells.getOrNull(0), GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            GridCell(cells.getOrNull(1), GlanceModifier.defaultWeight())
        }
        Spacer(GlanceModifier.height(6.dp))
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            GridCell(cells.getOrNull(2), GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            GridCell(cells.getOrNull(3), GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun GridCell(preset: Preset?, modifier: GlanceModifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF0F6E68)))
            .padding(10.dp)
            .clickable(runAction(preset?.id)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = preset?.name ?: "Empty",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = preset?.ussdCode ?: "—",
            style = TextStyle(color = ColorProvider(Color(0xFFD4EFEB)), fontSize = 11.sp),
            maxLines = 1,
        )
    }
}
