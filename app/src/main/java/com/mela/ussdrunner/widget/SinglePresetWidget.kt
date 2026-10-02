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
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mela.ussdrunner.MelaApplication
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
            GlanceTheme {
                SingleWidgetContent(preset)
            }
        }
    }
}

@Composable
private fun SingleWidgetContent(preset: Preset?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF0F6E68)))
            .padding(16.dp)
            .clickable(runAction(preset?.id)),
        verticalAlignment = Alignment.Vertical.Top,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = preset?.name ?: "Preset removed",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = preset?.ussdCode ?: "Open the app to pick another",
            style = TextStyle(color = ColorProvider(Color(0xFFD4EFEB)), fontSize = 13.sp),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(12.dp))
        Text(
            text = if (preset == null) "OPEN" else "RUN",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.fillMaxWidth(),
        )
    }
}

internal fun runAction(presetId: String?) =
    actionStartActivity<WidgetRunActivity>(
        actionParametersOf(WidgetKeys.presetIdParam to (presetId ?: "")),
    )
