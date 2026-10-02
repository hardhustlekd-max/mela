package com.mela.ussdrunner.widget

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import android.content.Context

object WidgetKeys {
    val presetId = stringPreferencesKey("preset_id")
    val presetId0 = stringPreferencesKey("preset_id_0")
    val presetId1 = stringPreferencesKey("preset_id_1")
    val presetId2 = stringPreferencesKey("preset_id_2")
    val presetId3 = stringPreferencesKey("preset_id_3")

    val presetIdParam = ActionParameters.Key<String>("preset_id")
}

class WidgetUpdater(private val context: Context) {
    suspend fun refreshAll() {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(SinglePresetWidget::class.java).forEach { id ->
            SinglePresetWidget().update(context, id)
        }
        manager.getGlanceIds(GridPresetWidget::class.java).forEach { id ->
            GridPresetWidget().update(context, id)
        }
    }
}
