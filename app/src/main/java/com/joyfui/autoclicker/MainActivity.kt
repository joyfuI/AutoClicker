package com.joyfui.autoclicker

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.joyfui.autoclicker.data.AppSettings
import com.joyfui.autoclicker.data.DEFAULT_INTERVAL_MS
import com.joyfui.autoclicker.data.MAX_INTERVAL_MS
import com.joyfui.autoclicker.data.MIN_INTERVAL_MS
import com.joyfui.autoclicker.data.SettingsRepository
import com.joyfui.autoclicker.service.AutoClickAccessibilityService
import kotlinx.coroutines.launch

private val AutoClickerLightColorScheme = lightColorScheme(
    primary = Color(0xFF203456),
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF2A2A2A),
    surface = Color.White,
    onSurface = Color(0xFF2A2A2A)
)

class MainActivity : ComponentActivity() {

    private lateinit var repository: SettingsRepository

    private var statusBarScrim: View? = null

    private companion object {
        const val PRIVACY_POLICY_URL = "https://joyfui.com/privacy/autoclicker.html"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        applySystemBarStyle()
        ensureStatusBarScrim()

        repository = SettingsRepository(applicationContext)

        setContent {
            AutoClickerTheme {
                val settings by repository.settingsFlow.collectAsState(initial = AppSettings())
                AutoClickerSettingsScreen(
                    settings = settings.normalized(),
                    onPointCountChanged = { count ->
                        lifecycleScope.launch {
                            repository.updatePointCount(count)
                        }
                    },
                    onIntervalChanged = { intervalMs ->
                        lifecycleScope.launch {
                            repository.updateIntervalMs(intervalMs)
                        }
                    },
                    onOpenAccessibilitySettings = ::openAccessibilitySettings,
                    onOpenPrivacyPolicy = ::openPrivacyPolicy
                )
            }
        }
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
        ensureStatusBarScrim()
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

    private fun openPrivacyPolicy() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
    }
}

@Composable
private fun AutoClickerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AutoClickerLightColorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoClickerSettingsScreen(
    settings: AppSettings,
    onPointCountChanged: (Int) -> Unit,
    onIntervalChanged: (Long) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    var intervalText by rememberSaveable { mutableStateOf(settings.intervalMs.toString()) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(settings.intervalMs) {
        val persistedValue = settings.intervalMs.toString()
        if (intervalText != persistedValue) {
            intervalText = persistedValue
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.desc_more_options)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.menu_privacy_policy)) },
                            onClick = {
                                menuExpanded = false
                                onOpenPrivacyPolicy()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.label_point_count),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PointCountOption(
                    value = 1,
                    selected = settings.pointCount == 1,
                    onSelect = onPointCountChanged
                )
                Spacer(modifier = Modifier.width(16.dp))
                PointCountOption(
                    value = 2,
                    selected = settings.pointCount == 2,
                    onSelect = onPointCountChanged
                )
                Spacer(modifier = Modifier.width(16.dp))
                PointCountOption(
                    value = 3,
                    selected = settings.pointCount == 3,
                    onSelect = onPointCountChanged
                )
            }

            Text(
                text = stringResource(id = R.string.label_interval),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 28.dp)
            )

            OutlinedTextField(
                value = intervalText,
                onValueChange = { rawInput ->
                    val digitsOnly = rawInput.filter { it.isDigit() }
                    intervalText = digitsOnly

                    if (digitsOnly.isEmpty()) {
                        return@OutlinedTextField
                    }

                    val parsed = digitsOnly.toLongOrNull() ?: DEFAULT_INTERVAL_MS
                    val clamped = parsed.coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
                    if (clamped != settings.intervalMs) {
                        onIntervalChanged(clamped)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && intervalText.isBlank()) {
                            intervalText = settings.intervalMs.toString()
                        }
                    },
                singleLine = true,
                placeholder = { Text(text = stringResource(id = R.string.hint_interval)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
                    .heightIn(min = 46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD3D6DC),
                    contentColor = Color(0xFF30323A)
                )
            ) {
                Text(
                    text = stringResource(id = R.string.action_open_accessibility_settings),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PointCountOption(
    value: Int,
    selected: Boolean,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = { onSelect(value) }
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(text = value.toString())
    }
}
