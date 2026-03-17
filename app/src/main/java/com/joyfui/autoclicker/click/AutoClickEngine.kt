package com.joyfui.autoclicker.click

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import com.joyfui.autoclicker.data.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AutoClickEngine(
    private val service: AccessibilityService,
    private val scope: CoroutineScope,
    private val pointProvider: (pointCount: Int) -> List<PointF>
) {

    var onRunningChanged: ((Boolean) -> Unit)? = null

    private var currentSettings: AppSettings = AppSettings()
    private var loopJob: Job? = null
    private var running: Boolean = false

    val isRunning: Boolean
        get() = running

    fun updateSettings(settings: AppSettings) {
        currentSettings = settings.normalized()
    }

    fun start() {
        if (loopJob?.isActive == true) {
            return
        }

        loopJob = scope.launch {
            var pointIndex = 0
            while (isActive) {
                val settings = currentSettings.normalized()
                val points = pointProvider(settings.pointCount).take(settings.pointCount)
                if (points.isEmpty()) {
                    pointIndex = 0
                    delay(IDLE_WAIT_MS)
                    continue
                }

                if (pointIndex >= points.size) {
                    pointIndex = 0
                }

                val tapTarget = points[pointIndex]
                dispatchTap(tapTarget)

                if (!isActive) {
                    break
                }

                delay(settings.intervalMs)
                pointIndex = (pointIndex + 1) % points.size
            }
        }.also { job ->
            setRunning(true)
            job.invokeOnCompletion {
                setRunning(false)
                loopJob = null
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        setRunning(false)
    }

    private suspend fun dispatchTap(point: PointF): Boolean {
        val path = Path().apply { moveTo(point.x, point.y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, TAP_DURATION_MS)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val dispatched = service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                },
                null
            )

            if (!dispatched && continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    private fun setRunning(isRunning: Boolean) {
        if (running == isRunning) {
            return
        }
        running = isRunning
        onRunningChanged?.invoke(isRunning)
    }

    private companion object {
        const val TAP_DURATION_MS = 40L
        const val IDLE_WAIT_MS = 50L
    }
}
