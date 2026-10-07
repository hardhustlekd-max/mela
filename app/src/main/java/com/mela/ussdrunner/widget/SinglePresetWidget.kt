package com.mela.ussdrunner.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.mela.ussdrunner.MelaApplication
import com.mela.ussdrunner.R
import com.mela.ussdrunner.domain.model.Preset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SinglePresetWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        refresh(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        refresh(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetKeys.delete(context, appWidgetIds)
    }

    private fun refresh(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MelaApplication
                val presets = app.container.presetRepository.observeAll().first()
                appWidgetIds.forEach { id ->
                    SingleWidgetRenderer.render(context, appWidgetManager, id, presets)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

internal object SingleWidgetRenderer {
    fun render(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        presets: List<Preset>,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_single)
        val preset = WidgetKeys.resolveSingle(WidgetKeys.loadSingleId(context, appWidgetId), presets)
        val style = WidgetPalette.forIndex(0)
        views.setTextViewText(
            R.id.widget_single_name,
            preset?.name ?: context.getString(R.string.widget_empty_name),
        )
        views.setTextViewText(
            R.id.widget_single_code,
            preset?.ussdCode ?: context.getString(R.string.widget_empty_code),
        )
        views.setImageViewResource(R.id.widget_single_badge, style.badge)
        views.setOnClickPendingIntent(
            R.id.widget_single_root,
            if (preset == null) {
                WidgetIntents.configureSingle(context, appWidgetId, requestCode = 3)
            } else {
                WidgetIntents.runPreset(context, preset.id, requestCode = 1000 + appWidgetId)
            },
        )
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
