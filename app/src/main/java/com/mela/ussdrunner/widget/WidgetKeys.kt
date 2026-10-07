package com.mela.ussdrunner.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.mela.ussdrunner.MelaApplication
import com.mela.ussdrunner.domain.model.Preset
import kotlinx.coroutines.flow.first

object WidgetKeys {
    const val PRESET_ID_EXTRA = "preset_id"
    private const val PREFS = "mela_widget_slots"

    fun loadGridIds(context: Context, appWidgetId: Int): List<String?> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (0 until 6).map { index -> prefs.getString(gridKey(appWidgetId, index), null) }
    }

    fun saveGridIds(context: Context, appWidgetId: Int, ids: List<String>) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        repeat(6) { index ->
            val id = ids.getOrNull(index)
            if (id.isNullOrBlank()) {
                editor.remove(gridKey(appWidgetId, index))
            } else {
                editor.putString(gridKey(appWidgetId, index), id)
            }
        }
        editor.apply()
    }

    fun loadSingleId(context: Context, appWidgetId: Int): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(singleKey(appWidgetId), null)

    fun saveSingleId(context: Context, appWidgetId: Int, id: String?) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (id.isNullOrBlank()) editor.remove(singleKey(appWidgetId))
        else editor.putString(singleKey(appWidgetId), id)
        editor.apply()
    }

    fun delete(context: Context, appWidgetIds: IntArray) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach { appWidgetId ->
            editor.remove(singleKey(appWidgetId))
            repeat(6) { index -> editor.remove(gridKey(appWidgetId, index)) }
        }
        editor.apply()
    }

    fun resolveGrid(savedIds: List<String?>, all: List<Preset>): List<Preset?> {
        val byId = all.associateBy { it.id }
        return fillGridCells(savedIds.map { id -> id?.let { byId[it] } }, all)
    }

    fun resolveSingle(savedId: String?, all: List<Preset>): Preset? {
        val byId = all.associateBy { it.id }
        return savedId?.let { byId[it] } ?: all.firstOrNull()
    }

    private fun gridKey(appWidgetId: Int, index: Int) = "grid_${appWidgetId}_$index"
    private fun singleKey(appWidgetId: Int) = "single_$appWidgetId"
}

class WidgetUpdater(private val context: Context) {
    suspend fun refreshAll() {
        val app = context.applicationContext as MelaApplication
        val presets = app.container.presetRepository.observeAll().first()
        val manager = AppWidgetManager.getInstance(context)
        val gridIds = manager.getAppWidgetIds(
            ComponentName(context, GridPresetWidgetReceiver::class.java),
        )
        gridIds.forEach { id -> GridWidgetRenderer.render(context, manager, id, presets) }
        val singleIds = manager.getAppWidgetIds(
            ComponentName(context, SinglePresetWidgetReceiver::class.java),
        )
        singleIds.forEach { id -> SingleWidgetRenderer.render(context, manager, id, presets) }
    }
}
