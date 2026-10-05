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
    val presetId4 = stringPreferencesKey("preset_id_4")
    val presetId5 = stringPreferencesKey("preset_id_5")

    /** Grid widget slots in reading order (2 columns x 3 rows). */
    val gridSlots = listOf(presetId0, presetId1, presetId2, presetId3, presetId4, presetId5)

    val presetIdParam = ActionParameters.Key<String>("preset_id")
    val routeParam = ActionParameters.Key<String>("route")
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
