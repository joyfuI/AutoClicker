package com.joyfui.autoclicker.data

const val MIN_POINT_COUNT = 1
const val MAX_POINT_COUNT = 3
const val DEFAULT_POINT_COUNT = 1

const val MIN_INTERVAL_MS = 10L
const val MAX_INTERVAL_MS = 5000L
const val DEFAULT_INTERVAL_MS = 100L

data class AppSettings(
    val pointCount: Int = DEFAULT_POINT_COUNT,
    val intervalMs: Long = DEFAULT_INTERVAL_MS,
    val controlPosition: OverlayPosition? = null,
    val point1Position: OverlayPosition? = null,
    val point2Position: OverlayPosition? = null,
    val point3Position: OverlayPosition? = null
) {
    fun normalized(): AppSettings = copy(
        pointCount = pointCount.coerceIn(MIN_POINT_COUNT, MAX_POINT_COUNT),
        intervalMs = intervalMs.coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
    )
}

data class OverlayPosition(
    val x: Int,
    val y: Int
)
