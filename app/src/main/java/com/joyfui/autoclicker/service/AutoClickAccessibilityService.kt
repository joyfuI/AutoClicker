package com.joyfui.autoclicker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.joyfui.autoclicker.click.AutoClickEngine
import com.joyfui.autoclicker.data.SettingsRepository
import com.joyfui.autoclicker.overlay.OverlayController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AutoClickAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var repository: SettingsRepository
    private var overlayController: OverlayController? = null
    private var clickEngine: AutoClickEngine? = null
    private var settingsJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        repository = SettingsRepository(applicationContext)
        overlayController = OverlayController(
            context = this,
            repository = repository,
            scope = serviceScope,
            onControlTapped = ::toggleAutoClick
        )
        clickEngine = AutoClickEngine(
            service = this,
            scope = serviceScope,
            pointProvider = { count -> overlayController?.getTapPoints(count).orEmpty() }
        ).also { engine ->
            engine.onRunningChanged = { isRunning ->
                overlayController?.setRunning(isRunning)
            }
        }

        settingsJob?.cancel()
        settingsJob = serviceScope.launch {
            repository.settingsFlow.collect { settings ->
                try {
                    overlayController?.show(settings)
                    overlayController?.applySettings(settings)
                    clickEngine?.updateSettings(settings)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (throwable: Throwable) {
                    Log.e(TAG, "Failed to initialize accessibility overlay", throwable)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        clickEngine?.stop()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun toggleAutoClick() {
        val engine = clickEngine ?: return
        if (engine.isRunning) {
            engine.stop()
        } else {
            engine.start()
        }
    }

    private fun cleanup() {
        settingsJob?.cancel()
        settingsJob = null
        clickEngine?.stop()
        clickEngine = null
        overlayController?.removeAll()
        overlayController = null
    }

    private companion object {
        const val TAG = "AutoClickService"
    }
}
