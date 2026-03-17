package com.joyfui.autoclicker

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.joyfui.autoclicker.data.AppSettings
import com.joyfui.autoclicker.data.DEFAULT_INTERVAL_MS
import com.joyfui.autoclicker.data.MAX_INTERVAL_MS
import com.joyfui.autoclicker.data.MIN_INTERVAL_MS
import com.joyfui.autoclicker.data.SettingsRepository
import com.joyfui.autoclicker.service.AutoClickAccessibilityService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var repository: SettingsRepository

    private lateinit var pointCountGroup: RadioGroup
    private lateinit var intervalInput: EditText

    private var currentSettings: AppSettings = AppSettings()
    private var rendering = false
    private var statusBarScrim: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_main)
        applySystemBarStyle()
        ensureStatusBarScrim()

        repository = SettingsRepository(applicationContext)

        pointCountGroup = findViewById(R.id.radio_group_point_count)
        intervalInput = findViewById(R.id.edit_interval_ms)

        fixActionBarOverlapIfNeeded()
        bindListeners()
        observeSettings()
    }

    private fun fixActionBarOverlapIfNeeded() {
        val scrollContent = findViewById<ScrollView>(R.id.scroll_content)
        val baseTopPadding = scrollContent.paddingTop

        scrollContent.post {
            val actionBarHeight = supportActionBar?.height?.takeIf { it > 0 } ?: resolveActionBarHeight()
            if (actionBarHeight <= 0) {
                return@post
            }

            val location = IntArray(2)
            scrollContent.getLocationInWindow(location)

            val statusBarInset = ViewCompat.getRootWindowInsets(scrollContent)
                ?.getInsets(WindowInsetsCompat.Type.statusBars())
                ?.top ?: 0

            val expectedTop = statusBarInset + actionBarHeight
            val overlap = expectedTop - location[1]
            if (overlap > 0) {
                scrollContent.setPadding(
                    scrollContent.paddingLeft,
                    baseTopPadding + overlap,
                    scrollContent.paddingRight,
                    scrollContent.paddingBottom
                )
            }
        }
    }

    private fun resolveActionBarHeight(): Int {
        val typedValue = TypedValue()
        val resolved = theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, typedValue, true)
        if (!resolved) {
            return 0
        }
        return TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
    }

    private fun applySystemBarStyle() {
        window.statusBarColor = getColor(R.color.app_bar_background)
        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }
    }

    private fun ensureStatusBarScrim() {
        val decorView = window.decorView as? ViewGroup ?: return
        if (statusBarScrim == null) {
            statusBarScrim = View(this).apply {
                setBackgroundColor(getColor(R.color.app_bar_background))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    0,
                    Gravity.TOP
                )
                isClickable = false
                isFocusable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            decorView.addView(statusBarScrim)
        }

        statusBarScrim?.let { scrim ->
            ViewCompat.setOnApplyWindowInsetsListener(scrim) { view, insets ->
                val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                view.updateLayoutParams<FrameLayout.LayoutParams> {
                    height = statusBarTop
                }
                insets
            }
            ViewCompat.requestApplyInsets(scrim)
        }
    }

    override fun onResume() {
        super.onResume()
        applySystemBarStyle()
    }

    private fun bindListeners() {
        findViewById<View>(R.id.button_open_accessibility_settings).setOnClickListener {
            openAccessibilitySettings()
        }

        pointCountGroup.setOnCheckedChangeListener { _, checkedId ->
            if (rendering) {
                return@setOnCheckedChangeListener
            }
            val selectedCount = when (checkedId) {
                R.id.radio_point_count_1 -> 1
                R.id.radio_point_count_2 -> 2
                R.id.radio_point_count_3 -> 3
                else -> currentSettings.pointCount
            }
            if (selectedCount != currentSettings.pointCount) {
                lifecycleScope.launch {
                    repository.updatePointCount(selectedCount)
                }
            }
        }

        intervalInput.doAfterTextChanged { editable ->
            if (rendering) {
                return@doAfterTextChanged
            }
            val rawText = editable?.toString().orEmpty().trim()
            if (rawText.isEmpty()) {
                return@doAfterTextChanged
            }

            val parsed = rawText.toLongOrNull() ?: DEFAULT_INTERVAL_MS
            val clamped = parsed.coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
            val clampedText = clamped.toString()

            if (rawText != clampedText) {
                rendering = true
                intervalInput.setText(clampedText)
                intervalInput.setSelection(clampedText.length)
                rendering = false
            }

            if (clamped != currentSettings.intervalMs) {
                lifecycleScope.launch {
                    repository.updateIntervalMs(clamped)
                }
            }
        }

        intervalInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                return@setOnFocusChangeListener
            }
            if (intervalInput.text?.toString()?.trim().isNullOrEmpty()) {
                val normalizedText = currentSettings.intervalMs.toString()
                rendering = true
                intervalInput.setText(normalizedText)
                intervalInput.setSelection(normalizedText.length)
                rendering = false
            }
        }
    }

    private fun openAccessibilitySettings() {
        val serviceComponent = ComponentName(this, AutoClickAccessibilityService::class.java)
        val detailsIntent = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, serviceComponent)
        }

        val openedDetails = runCatching {
            startActivity(detailsIntent)
            true
        }.getOrElse { throwable ->
            when (throwable) {
                is ActivityNotFoundException,
                is SecurityException -> false
                else -> throw throwable
            }
        }

        if (!openedDetails) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.settingsFlow.collect { settings ->
                    currentSettings = settings.normalized()
                    render(currentSettings)
                }
            }
        }
    }

    private fun render(settings: AppSettings) {
        rendering = true

        val checkedId = when (settings.pointCount) {
            1 -> R.id.radio_point_count_1
            2 -> R.id.radio_point_count_2
            else -> R.id.radio_point_count_3
        }
        if (pointCountGroup.checkedRadioButtonId != checkedId) {
            pointCountGroup.check(checkedId)
        }

        val intervalText = settings.intervalMs.toString()
        if (intervalInput.text?.toString() != intervalText) {
            intervalInput.setText(intervalText)
            intervalInput.setSelection(intervalText.length)
        }

        rendering = false
    }
}
