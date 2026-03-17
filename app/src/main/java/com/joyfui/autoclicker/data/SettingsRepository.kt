package com.joyfui.autoclicker.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auto_clicker_settings")

class SettingsRepository(
    private val context: Context
) {
    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .map { preferences ->
            AppSettings(
                pointCount = (preferences[Keys.POINT_COUNT] ?: DEFAULT_POINT_COUNT)
                    .coerceIn(MIN_POINT_COUNT, MAX_POINT_COUNT),
                intervalMs = (preferences[Keys.INTERVAL_MS] ?: DEFAULT_INTERVAL_MS)
                    .coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS),
                controlPosition = readPosition(preferences, Keys.CONTROL_X, Keys.CONTROL_Y),
                point1Position = readPosition(preferences, Keys.POINT1_X, Keys.POINT1_Y),
                point2Position = readPosition(preferences, Keys.POINT2_X, Keys.POINT2_Y),
                point3Position = readPosition(preferences, Keys.POINT3_X, Keys.POINT3_Y)
            )
        }
        .distinctUntilChanged()

    suspend fun updatePointCount(pointCount: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.POINT_COUNT] = pointCount.coerceIn(MIN_POINT_COUNT, MAX_POINT_COUNT)
        }
    }

    suspend fun updateIntervalMs(intervalMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.INTERVAL_MS] = intervalMs.coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
        }
    }

    suspend fun updateControlPosition(position: OverlayPosition) {
        savePosition(
            xKey = Keys.CONTROL_X,
            yKey = Keys.CONTROL_Y,
            position = position
        )
    }

    suspend fun updatePointPosition(pointIndex: Int, position: OverlayPosition) {
        when (pointIndex) {
            1 -> savePosition(Keys.POINT1_X, Keys.POINT1_Y, position)
            2 -> savePosition(Keys.POINT2_X, Keys.POINT2_Y, position)
            3 -> savePosition(Keys.POINT3_X, Keys.POINT3_Y, position)
        }
    }

    private suspend fun savePosition(
        xKey: Preferences.Key<Int>,
        yKey: Preferences.Key<Int>,
        position: OverlayPosition
    ) {
        context.dataStore.edit { preferences ->
            preferences[xKey] = position.x
            preferences[yKey] = position.y
        }
    }

    private fun readPosition(
        preferences: Preferences,
        xKey: Preferences.Key<Int>,
        yKey: Preferences.Key<Int>
    ): OverlayPosition? {
        val x = preferences[xKey]
        val y = preferences[yKey]
        return if (x != null && y != null) OverlayPosition(x, y) else null
    }

    private object Keys {
        val POINT_COUNT = intPreferencesKey("point_count")
        val INTERVAL_MS = longPreferencesKey("interval_ms")
        val CONTROL_X = intPreferencesKey("control_x")
        val CONTROL_Y = intPreferencesKey("control_y")
        val POINT1_X = intPreferencesKey("point_1_x")
        val POINT1_Y = intPreferencesKey("point_1_y")
        val POINT2_X = intPreferencesKey("point_2_x")
        val POINT2_Y = intPreferencesKey("point_2_y")
        val POINT3_X = intPreferencesKey("point_3_x")
        val POINT3_Y = intPreferencesKey("point_3_y")
    }
}
