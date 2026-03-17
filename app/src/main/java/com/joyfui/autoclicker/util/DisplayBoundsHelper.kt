package com.joyfui.autoclicker.util

import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.WindowManager
import com.joyfui.autoclicker.data.OverlayPosition

object DisplayBoundsHelper {

    fun getDisplayBounds(windowManager: WindowManager): Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Rect(windowManager.currentWindowMetrics.bounds)
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val size = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(size)
            Rect(0, 0, size.x, size.y)
        }
    }

    fun clampToBounds(
        x: Int,
        y: Int,
        viewWidth: Int,
        viewHeight: Int,
        bounds: Rect
    ): OverlayPosition {
        val maxX = (bounds.width() - viewWidth).coerceAtLeast(0)
        val maxY = (bounds.height() - viewHeight).coerceAtLeast(0)
        return OverlayPosition(
            x = x.coerceIn(0, maxX),
            y = y.coerceIn(0, maxY)
        )
    }
}
