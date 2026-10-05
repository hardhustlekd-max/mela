package com.mela.ussdrunner.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
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
            GlanceTheme(colors = MelaWidgetColors) { SingleWidgetContent(preset) }
        }
    }
}

/** One shortcut as a Material 3 tonal card. */
@Composable
private fun SingleWidgetContent(preset: Preset?) {
    val c = GlanceTheme.colors
    val kind = preset?.let(::tileKindFor)
    val colors = kind?.colors() ?: TileColors(c.surfaceVariant, c.onSurfaceVariant, c.outline, c.surface)
    val icon = kind?.icon ?: R.drawable.ic_w_add
    val action = if (preset == null) routeAction("home") else runAction(preset.id)

    Scaffold(
        backgroundColor = colors.container,
        horizontalPadding = 14.dp,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize().clickable(action),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(icon, 44.dp, colors.accent, colors.onAccent)
            Spacer(GlanceModifier.width(12.dp))
            Column(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = preset?.name ?: "Shortcut removed",
                    style = TextStyle(
                        color = colors.onContainer,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = preset?.ussdCode ?: "Tap to open the app",
                    style = TextStyle(color = colors.onContainer, fontSize = 13.sp),
                    maxLines = 1,
                )
            }
        }
    }
}
