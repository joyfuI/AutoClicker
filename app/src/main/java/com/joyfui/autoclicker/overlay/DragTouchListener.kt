package com.joyfui.autoclicker.overlay

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.joyfui.autoclicker.data.OverlayPosition

class DragTouchListener(
    private val targetView: View,
    private val layoutParams: WindowManager.LayoutParams,
    private val windowManager: WindowManager,
    private val clampPosition: (x: Int, y: Int, width: Int, height: Int) -> OverlayPosition,
    private val onDragEnd: (OverlayPosition) -> Unit,
    private val onTap: (() -> Unit)? = null
) : View.OnTouchListener {

    private val touchSlop = ViewConfiguration.get(targetView.context).scaledTouchSlop
    private var downRawX = 0f
    private var downRawY = 0f
    private var downX = 0
    private var downY = 0
    private var dragged = false

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                downX = layoutParams.x
                downY = layoutParams.y
                dragged = false
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - downRawX).toInt()
                val dy = (event.rawY - downRawY).toInt()
                if (!dragged) {
                    val distanceSquared = dx * dx + dy * dy
                    dragged = distanceSquared > touchSlop * touchSlop
                }

                val width = maxOf(targetView.width, targetView.measuredWidth, 1)
                val height = maxOf(targetView.height, targetView.measuredHeight, 1)
                val clampedPosition = clampPosition(
                    downX + dx,
                    downY + dy,
                    width,
                    height
                )
                if (layoutParams.x != clampedPosition.x || layoutParams.y != clampedPosition.y) {
                    layoutParams.x = clampedPosition.x
                    layoutParams.y = clampedPosition.y
                    windowManager.updateViewLayout(targetView, layoutParams)
                }
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val position = OverlayPosition(layoutParams.x, layoutParams.y)
                onDragEnd(position)
                if (!dragged && onTap != null && event.actionMasked == MotionEvent.ACTION_UP) {
                    targetView.performClick()
                    onTap.invoke()
                }
                true
            }

            else -> false
        }
    }
}
