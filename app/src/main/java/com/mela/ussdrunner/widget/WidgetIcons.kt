package com.mela.ussdrunner.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.core.content.ContextCompat

/** Rasterize icons in the app so the launcher cannot mirror vector drawables. */
internal object WidgetIcons {
    fun circled(context: Context, icon: Int, color: Int): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        drawIcon(context, icon, canvas, size, 0xFFFFFFFF.toInt(), 0.22f)
        return bitmap
    }

    fun plain(context: Context, icon: Int, color: Int): Bitmap {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawIcon(context, icon, Canvas(bitmap), size, color, 0.04f)
        return bitmap
    }

    private fun drawIcon(
        context: Context,
        icon: Int,
        canvas: Canvas,
        size: Int,
        color: Int,
        insetFraction: Float,
    ) {
        val drawable = ContextCompat.getDrawable(context, icon)?.mutate() ?: return
        drawable.setTint(color)
        drawable.isAutoMirrored = false
        drawable.layoutDirection = View.LAYOUT_DIRECTION_LTR
        val inset = (size * insetFraction).toInt()
        drawable.setBounds(inset, inset, size - inset, size - inset)
        drawable.draw(canvas)
    }
}
