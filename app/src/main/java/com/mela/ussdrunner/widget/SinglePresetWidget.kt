package com.mela.ussdrunner.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.action.clickable
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mela.ussdrunner.MelaApplication
import com.mela.ussdrunner.R
import com.mela.ussdrunner.domain.model.Preset
import kotlinx.coroutines.flow.first

class SinglePresetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SinglePresetWidget()
}

class SinglePresetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as MelaApplication
        val presets = app.container.presetRepository.observeAll().first().associateBy { it.id }
        provideContent {
            val prefs = currentState<Preferences>()
            val preset = prefs[WidgetKeys.presetId]?.let { presets[it] }
            GlanceTheme { SingleWidgetContent(preset) }
        }
    }
}

/** A single shortcut in the same pastel-tile language as the grid widget. */
@Composable
private fun SingleWidgetContent(preset: Preset?) {
    if (preset == null) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_tile_empty_bg))
                .padding(14.dp)
                .clickable(routeAction("home")),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(R.drawable.widget_circle_empty, R.drawable.ic_w_add, 44.dp, 26.dp)
            Spacer(GlanceModifier.width(12.dp))
            Column {
                Text(
                    text = "Shortcut removed",
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text_primary),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = "Tap to open the app",
                    style = TextStyle(color = ColorProvider(R.color.widget_text_secondary), fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
        return
    }
    val kind = tileKindFor(preset)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(kind.tile))
            .padding(14.dp)
            .clickable(runAction(preset.id)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(kind.circle, kind.icon, 44.dp, 26.dp)
            Spacer(GlanceModifier.width(12.dp))
            Column {
                Text(
                    text = preset.name,
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text_primary),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = preset.ussdCode,
                    style = TextStyle(color = ColorProvider(R.color.widget_text_secondary), fontSize = 13.sp),
                    maxLines = 1,
                )
            }
        }
    }
}
