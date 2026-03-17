package com.joyfui.autoclicker.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.PointF
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import com.joyfui.autoclicker.R
import com.joyfui.autoclicker.data.AppSettings
import com.joyfui.autoclicker.data.OverlayPosition
import com.joyfui.autoclicker.data.SettingsRepository
import com.joyfui.autoclicker.util.DisplayBoundsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OverlayController(
    context: Context,
    private val repository: SettingsRepository,
    private val scope: CoroutineScope,
    private val onControlTapped: () -> Unit
) {

    private val inflater = LayoutInflater.from(context)
    private val windowManager = checkNotNull(context.getSystemService(WindowManager::class.java))
    private val density = context.resources.displayMetrics.density

    private var currentSettings: AppSettings = AppSettings()
    private var isRunning = false
    private var controlOverlay: OverlayItem? = null
    private val pointOverlays = mutableListOf<OverlayItem>()

    fun show(settings: AppSettings) {
        currentSettings = settings.normalized()
        if (controlOverlay == null) {
            controlOverlay = createControlOverlay(currentSettings)
        }
        if (pointOverlays.isEmpty()) {
            repeat(3) { pointOverlays += createPointOverlay(index = it + 1, settings = currentSettings) }
        }
        updatePointVisibility(currentSettings.pointCount)
        updateControlIcon()
    }

    fun applySettings(settings: AppSettings) {
        currentSettings = settings.normalized()
        updatePointVisibility(currentSettings.pointCount)
    }

    fun setRunning(running: Boolean) {
        isRunning = running
        updateControlIcon()
    }

    fun removeAll() {
        controlOverlay?.let(::removeOverlayItem)
        controlOverlay = null

        pointOverlays.forEach(::removeOverlayItem)
        pointOverlays.clear()
    }

    fun getTapPoints(pointCount: Int): List<PointF> {
        val activeCount = pointCount.coerceIn(1, 3)
        return pointOverlays
            .take(activeCount)
            .mapNotNull { item ->
                if (!item.view.isVisible) {
                    return@mapNotNull null
                }
                val width = maxOf(item.view.width, item.view.measuredWidth, dp(56))
                val height = maxOf(item.view.height, item.view.measuredHeight, dp(56))
                PointF(
                    item.params.x + width / 2f,
                    item.params.y + height / 2f
                )
            }
    }

    private fun createControlOverlay(settings: AppSettings): OverlayItem {
        val view = inflater.inflate(R.layout.view_overlay_control, FrameLayout(inflater.context), false)
        val initial = settings.controlPosition ?: defaultControlPosition()
        val params = createBaseLayoutParams(initial)

        windowManager.addView(view, params)
        clampAfterAttach(view, params)

        view.setOnTouchListener(
            DragTouchListener(
                targetView = view,
                layoutParams = params,
                windowManager = windowManager,
                clampPosition = ::clampPosition,
                onDragEnd = { position ->
                    scope.launch { repository.updateControlPosition(position) }
                },
                onTap = onControlTapped
            )
        )
        return OverlayItem(view, params)
    }

    private fun createPointOverlay(index: Int, settings: AppSettings): OverlayItem {
        val view = inflater.inflate(R.layout.view_overlay_point, FrameLayout(inflater.context), false)
        val initial = getSavedPointPosition(settings, index) ?: defaultPointPosition(index)
        val params = createBaseLayoutParams(initial)

        windowManager.addView(view, params)
        clampAfterAttach(view, params)

        view.setOnTouchListener(
            DragTouchListener(
                targetView = view,
                layoutParams = params,
                windowManager = windowManager,
                clampPosition = ::clampPosition,
                onDragEnd = { position ->
                    scope.launch { repository.updatePointPosition(index, position) }
                }
            )
        )
        return OverlayItem(view, params)
    }

    private fun createBaseLayoutParams(position: OverlayPosition): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = position.x
            y = position.y
        }
    }

    private fun clampAfterAttach(view: View, params: WindowManager.LayoutParams) {
        view.post {
            val width = maxOf(view.width, view.measuredWidth, dp(56))
            val height = maxOf(view.height, view.measuredHeight, dp(56))
            val clamped = clampPosition(params.x, params.y, width, height)
            if (params.x != clamped.x || params.y != clamped.y) {
                params.x = clamped.x
                params.y = clamped.y
                runCatching { windowManager.updateViewLayout(view, params) }
            }
        }
    }

    private fun updatePointVisibility(pointCount: Int) {
        val normalizedCount = pointCount.coerceIn(1, 3)
        pointOverlays.forEachIndexed { index, item ->
            item.view.isVisible = index < normalizedCount
        }
    }

    private fun updateControlIcon() {
        val imageView = controlOverlay?.view?.findViewById<ImageView>(R.id.image_control_icon) ?: return
        imageView.setImageResource(if (isRunning) R.drawable.ic_stop else R.drawable.ic_play)
    }

    private fun getSavedPointPosition(settings: AppSettings, index: Int): OverlayPosition? {
        return when (index) {
            1 -> settings.point1Position
            2 -> settings.point2Position
            3 -> settings.point3Position
            else -> null
        }
    }

    private fun defaultControlPosition(): OverlayPosition {
        return OverlayPosition(x = dp(16), y = dp(24))
    }

    private fun defaultPointPosition(index: Int): OverlayPosition {
        val bounds = DisplayBoundsHelper.getDisplayBounds(windowManager)
        val baseX = bounds.centerX() - dp(28)
        val baseY = bounds.centerY() - dp(28)
        val spacing = dp(88)

        val position = when (index) {
            1 -> OverlayPosition(baseX, baseY)
            2 -> OverlayPosition(baseX + spacing, baseY)
            3 -> OverlayPosition(baseX, baseY + spacing)
            else -> OverlayPosition(baseX, baseY)
        }
        return DisplayBoundsHelper.clampToBounds(
            x = position.x,
            y = position.y,
            viewWidth = dp(56),
            viewHeight = dp(56),
            bounds = bounds
        )
    }

    private fun clampPosition(x: Int, y: Int, width: Int, height: Int): OverlayPosition {
        val bounds = DisplayBoundsHelper.getDisplayBounds(windowManager)
        return DisplayBoundsHelper.clampToBounds(
            x = x,
            y = y,
            viewWidth = width,
            viewHeight = height,
            bounds = bounds
        )
    }

    private fun removeOverlayItem(item: OverlayItem) {
        runCatching { windowManager.removeViewImmediate(item.view) }
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    private data class OverlayItem(
        val view: View,
        val params: WindowManager.LayoutParams
    )
}
