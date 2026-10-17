package com.mela.ussdrunner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.mela.ussdrunner.MainActivity
import com.mela.ussdrunner.MelaApplication
import com.mela.ussdrunner.R
import com.mela.ussdrunner.domain.model.Preset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GridPresetWidgetReceiver : AppWidgetProvider() {
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
                    try {
                        GridWidgetRenderer.render(context, appWidgetManager, id, presets)
                    } catch (_: Throwable) {
                        GridWidgetRenderer.renderFallback(context, appWidgetManager, id)
                    }
                }
            } catch (_: Throwable) {
                appWidgetIds.forEach { id ->
                    GridWidgetRenderer.renderFallback(context, appWidgetManager, id)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

internal object GridWidgetRenderer {
    private val tileIds = intArrayOf(
        R.id.tile_0, R.id.tile_1, R.id.tile_2, R.id.tile_3, R.id.tile_4, R.id.tile_5,
    )
    private val nameIds = intArrayOf(
        R.id.tile_0_name, R.id.tile_1_name, R.id.tile_2_name, R.id.tile_3_name,
    )
    private val codeIds = intArrayOf(
        R.id.tile_0_code, R.id.tile_1_code, R.id.tile_2_code, R.id.tile_3_code,
    )
    private val badgeIds = intArrayOf(
        R.id.tile_0_badge, R.id.tile_1_badge, R.id.tile_2_badge, R.id.tile_3_badge,
    )

    fun render(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        presets: List<Preset>,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_grid)
        val cells = WidgetKeys.resolveGrid(WidgetKeys.loadGridIds(context, appWidgetId), presets)
        for (index in 0 until 4) {
            val preset = cells.getOrNull(index)
            val style = WidgetPalette.forIndex(index)
            views.setTextViewText(
                nameIds[index],
                preset?.name ?: context.getString(R.string.widget_empty_name),
            )
            views.setTextViewText(
                codeIds[index],
                preset?.ussdCode ?: context.getString(R.string.widget_empty_code),
            )
            views.setInt(tileIds[index], "setBackgroundResource", style.tile)
            views.setImageViewBitmap(
                badgeIds[index],
                WidgetIcons.circled(context, style.badge, style.color),
            )
            views.setOnClickPendingIntent(
                tileIds[index],
                if (preset == null) {
                    WidgetIntents.configureGrid(context, appWidgetId, index + 10)
                } else {
                    WidgetIntents.runPreset(context, preset.id, appWidgetId * 10 + index)
                },
            )
        }
        views.setImageViewBitmap(
            R.id.widget_settings,
            WidgetIcons.plain(context, R.drawable.ic_widget_settings, 0xFF374151.toInt()),
        )
        views.setImageViewBitmap(
            R.id.widget_add,
            WidgetIcons.plain(context, R.drawable.ic_widget_add, 0xFF374151.toInt()),
        )
        views.setOnClickPendingIntent(
            R.id.widget_settings,
            WidgetIntents.configureGrid(context, appWidgetId, requestCode = 1),
        )
        views.setOnClickPendingIntent(
            R.id.widget_add,
            WidgetIntents.openEditor(context, requestCode = 2),
        )
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun renderFallback(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_loading)
        views.setOnClickPendingIntent(
            R.id.widget_loading_root,
            WidgetIntents.configureGrid(context, appWidgetId, requestCode = 1),
        )
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

internal object WidgetIntents {
    fun runPreset(context: Context, presetId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, WidgetRunActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WidgetKeys.PRESET_ID_EXTRA, presetId)
            data = Uri.parse("mela://widget/run/$presetId/$requestCode")
        }
        return pendingActivity(context, requestCode, intent)
    }

    fun configureGrid(context: Context, appWidgetId: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, GridWidgetConfigActivity::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("mela://widget/grid-config/$appWidgetId")
        }
        return pendingActivity(context, requestCode, intent)
    }

    fun configureSingle(context: Context, appWidgetId: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, SingleWidgetConfigActivity::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse("mela://widget/single-config/$appWidgetId")
        }
        return pendingActivity(context, requestCode, intent)
    }

    fun openEditor(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_EDITOR, true)
            data = Uri.parse("mela://widget/add")
        }
        return pendingActivity(context, requestCode, intent)
    }

    private fun pendingActivity(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

internal fun fillGridCells(saved: List<Preset?>, all: List<Preset>): List<Preset?> {
    if (saved.any { it != null }) return saved
    val fallback = all.take(6)
    return List(6) { index -> fallback.getOrNull(index) }
}

/** Tap order becomes widget order. Deselect removes that shortcut and closes the gap. */
internal fun toggleWidgetSelection(current: List<String>, id: String, max: Int): List<String> {
    if (id in current) return current.filterNot { it == id }
    if (max <= 1) return listOf(id)
    if (current.size >= max) return current
    return current + id
}

internal fun orderedWidgetPresets(chosenIds: List<String>, presets: List<Preset>, max: Int): List<Preset> {
    val byId = presets.associateBy { it.id }
    return chosenIds.mapNotNull { byId[it] }.distinctBy { it.id }.take(max)
}

internal object WidgetPalette {
    data class Style(val tile: Int, val badge: Int, val color: Int)

    private val styles = listOf(
        Style(R.drawable.widget_tile_green, R.drawable.ic_widget_dollar, 0xFF22C55E.toInt()),
        Style(R.drawable.widget_tile_blue, R.drawable.ic_widget_wifi, 0xFF3B82F6.toInt()),
        Style(R.drawable.widget_tile_orange, R.drawable.ic_widget_doc, 0xFFF97316.toInt()),
        Style(R.drawable.widget_tile_purple, R.drawable.ic_widget_card, 0xFFA855F7.toInt()),
        Style(R.drawable.widget_tile_teal, R.drawable.ic_widget_close, 0xFF14B8A6.toInt()),
        Style(R.drawable.widget_tile_pink, R.drawable.ic_widget_phone, 0xFFEC4899.toInt()),
    )

    fun forIndex(index: Int): Style = styles[index.mod(styles.size)]
}
