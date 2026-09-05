package com.jwilgeroth.sleeptimertile

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.jwilgeroth.sleeptimertile.ui.theme.SleepTimerTileTheme
import kotlinx.coroutines.launch

/**
 * The app's only screen. It's for configuration + permissions — never for
 * starting a real timer (the tile does that). It DOES have a "Test now" button
 * that runs the expiry action in 10 seconds so you can verify things quickly.
 */
class MainActivity : ComponentActivity() {

    // Settings mirror (loaded once, updated as the user edits; also written to
    // DataStore). This activity is the only writer, so simple local state is safe.
    private val durationMinutes = mutableStateOf(Settings.DEFAULT_DURATION_MINUTES)
    private val pauseMedia = mutableStateOf(Settings.DEFAULT_PAUSE_MEDIA)
    private val lockScreen = mutableStateOf(Settings.DEFAULT_LOCK_SCREEN)

    // Permission / reliability states, refreshed in onResume.
    private val adminActive = mutableStateOf(false)
    private val notificationsGranted = mutableStateOf(false)
    private val mediaListenerEnabled = mutableStateOf(false)
    private val batteryExempt = mutableStateOf(false)

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsGranted.value = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(DevicePolicyManager::class.java)
        adminComponent = ComponentName(this, SleepAdminReceiver::class.java)

        durationMinutes.value = Settings.durationMinutes(this)
        pauseMedia.value = Settings.pauseMedia(this)
        lockScreen.value = Settings.lockScreen(this)

        enableEdgeToEdge()
        setContent {
            SleepTimerTileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsScreen(
                        durationMinutes = durationMinutes.value,
                        onDurationChange = ::updateDuration,
                        pauseMedia = pauseMedia.value,
                        onPauseMediaChange = ::updatePauseMedia,
                        lockScreen = lockScreen.value,
                        onLockScreenChange = ::updateLockScreen,
                        onRunTest = ::runTest,
                        adminActive = adminActive.value,
                        onEnableAdmin = ::requestDeviceAdmin,
                        onDisableAdmin = ::disableDeviceAdmin,
                        notificationsGranted = notificationsGranted.value,
                        onEnableNotifications = ::requestNotificationPermission,
                        mediaListenerEnabled = mediaListenerEnabled.value,
                        onOpenMediaListenerSettings = ::openMediaListenerSettings,
                        batteryExempt = batteryExempt.value,
                        onRequestBattery = ::requestBatteryExemption,
                        onOpenAppInfo = ::openAppInfo,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adminActive.value = dpm.isAdminActive(adminComponent)
        notificationsGranted.value = hasNotificationPermission()
        mediaListenerEnabled.value = MediaAccess.isListenerEnabled(this)
        batteryExempt.value = BatteryHelper.isIgnoringOptimizations(this)
    }

    // --- Settings writes ----------------------------------------------------
    private fun updateDuration(min: Int) {
        durationMinutes.value = min
        lifecycleScope.launch { Settings.setDurationMinutes(this@MainActivity, min) }
    }

    private fun updatePauseMedia(value: Boolean) {
        pauseMedia.value = value
        lifecycleScope.launch { Settings.setPauseMedia(this@MainActivity, value) }
    }

    private fun updateLockScreen(value: Boolean) {
        lockScreen.value = value
        lifecycleScope.launch { Settings.setLockScreen(this@MainActivity, value) }
    }

    /** Runs the real expiry action (per current toggles) in 10 seconds. */
    private fun runTest() {
        SleepTimer.start(this, TEST_DURATION_MS)
    }

    // --- Permission / reliability actions -----------------------------------
    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_explanation))
        }
        startActivity(intent)
    }

    private fun disableDeviceAdmin() {
        dpm.removeActiveAdmin(adminComponent)
        adminActive.value = dpm.isAdminActive(adminComponent)
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openMediaListenerSettings() {
        startActivity(MediaAccess.listenerSettingsIntent())
    }

    private fun requestBatteryExemption() {
        startActivity(BatteryHelper.requestIgnoreIntent(this))
    }

    private fun openAppInfo() {
        startActivity(BatteryHelper.appDetailsIntent(this))
    }
}

@Composable
fun SettingsScreen(
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    pauseMedia: Boolean,
    onPauseMediaChange: (Boolean) -> Unit,
    lockScreen: Boolean,
    onLockScreenChange: (Boolean) -> Unit,
    onRunTest: () -> Unit,
    adminActive: Boolean,
    onEnableAdmin: () -> Unit,
    onDisableAdmin: () -> Unit,
    notificationsGranted: Boolean,
    onEnableNotifications: () -> Unit,
    mediaListenerEnabled: Boolean,
    onOpenMediaListenerSettings: () -> Unit,
    batteryExempt: Boolean,
    onRequestBattery: () -> Unit,
    onOpenAppInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Outer column scrolls; inner column is width-capped and centered so the
    // screen stays readable on the Fold's wide inner display.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally)
                .padding(24.dp)
        ) {
            Text("Sleep Timer Tile", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Start timers from the Quick Settings tile (tap = default, long-press " +
                    "= pick a length). This screen sets how they behave.",
                style = MaterialTheme.typography.bodySmall
            )

            // --- Reliability warning (shown until exempt) ------------------
            if (!batteryExempt) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "⚠️ Timer may not fire reliably",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Samsung can freeze this app overnight and delay the alarm. " +
                                "Fix it with the two steps in Reliability below.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // --- Duration --------------------------------------------------
            Spacer(Modifier.height(24.dp))
            Text("Timer length", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { preset ->
                    FilterChip(
                        selected = durationMinutes == preset,
                        onClick = { onDurationChange(preset) },
                        label = { Text("$preset min") }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            var customText by remember(durationMinutes) { mutableStateOf(durationMinutes.toString()) }
            OutlinedTextField(
                value = customText,
                onValueChange = { input ->
                    customText = input.filter { it.isDigit() }.take(3)
                    customText.toIntOrNull()?.let { if (it in 1..600) onDurationChange(it) }
                },
                label = { Text("Custom (minutes)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // --- Behavior toggles ------------------------------------------
            Spacer(Modifier.height(24.dp))
            Text("When the timer ends", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            ToggleRow("Pause media", pauseMedia, onPauseMediaChange)
            ToggleRow("Lock / turn screen off", lockScreen, onLockScreenChange)

            // --- Test ------------------------------------------------------
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRunTest) { Text("Test now (runs in 10 seconds)") }
            Spacer(Modifier.height(4.dp))
            Text(
                "Runs the real end-of-timer action after 10 seconds using the settings " +
                    "above — a fast way to check it works.",
                style = MaterialTheme.typography.bodySmall
            )

            // --- Reliability (Samsung) -------------------------------------
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))
            Text("Reliability (Samsung)", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Step 1 — Battery optimization", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (batteryExempt) {
                    "✅ This app is exempt from battery optimization."
                } else {
                    "❌ Not exempt. Tap below and choose \"Allow\" / \"Don't optimize\" " +
                        "so Android doesn't defer the alarm."
                }
            )
            Spacer(Modifier.height(12.dp))
            if (!batteryExempt) {
                Button(onClick = onRequestBattery) { Text("Allow (ignore battery optimization)") }
            }

            Spacer(Modifier.height(20.dp))
            Text("Step 2 — Sleeping apps (manual)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Android has no API for this one, so you must set it by hand: open App " +
                    "info → Battery → set to \"Unrestricted.\" Also check Settings → " +
                    "Battery → Background usage limits and make sure Sleep Timer is NOT " +
                    "in \"Sleeping\" or \"Deep sleeping\" apps.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenAppInfo) { Text("Open App info → Battery") }

            // --- Permissions -----------------------------------------------
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))
            Text("Permissions", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(16.dp))
            Text("Turn the screen off", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (adminActive) {
                    "✅ Enabled. The tile can lock your screen."
                } else {
                    "❌ Not enabled. The timer can't turn the screen off until you grant this."
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Uses \"Device Admin\" — the only no-root way for an app to lock the " +
                    "screen. Its consent screen sounds scary, but this app requests only " +
                    "the lock-screen power, nothing else.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            if (adminActive) {
                OutlinedButton(onClick = onDisableAdmin) { Text("Disable screen-off permission") }
            } else {
                Button(onClick = onEnableAdmin) { Text("Enable screen-off permission") }
            }

            Spacer(Modifier.height(24.dp))
            Text("Show a countdown", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (notificationsGranted) {
                    "✅ Enabled. A ticking countdown notification shows while a timer runs."
                } else {
                    "❌ Not enabled. A timer still works but shows no countdown."
                }
            )
            Spacer(Modifier.height(12.dp))
            if (!notificationsGranted) {
                Button(onClick = onEnableNotifications) { Text("Enable countdown notification") }
            }

            Spacer(Modifier.height(24.dp))
            Text("Pause stubborn apps", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (mediaListenerEnabled) {
                    "✅ Notification access on — pauses the exact playing app, plus the " +
                        "backup pause key."
                } else {
                    "☑️ Basic pause is always on (a \"pause\" key that stops most apps). " +
                        "Only enable notification access if some app won't pause — it's a " +
                        "broad permission (reads all notifications); this app uses it just " +
                        "to find the playing media."
                },
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            if (mediaListenerEnabled) {
                OutlinedButton(onClick = onOpenMediaListenerSettings) { Text("Manage notification access") }
            } else {
                Button(onClick = onOpenMediaListenerSettings) { Text("Turn on notification access (optional)") }
            }

            // --- Uninstall reminder ----------------------------------------
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))
            Text(
                "⚠️ To uninstall this app later, FIRST disable its device admin permission " +
                    "(button above, or Settings → Security and privacy → More security " +
                    "settings → Device admin apps). Android blocks uninstalling an active " +
                    "device admin.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
