package com.daycare.sleepcheck.log

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daycare.sleepcheck.log.ui.DaycareApp
import com.daycare.sleepcheck.log.ui.SleepCheckTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SleepViewModel by viewModels()
    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::backupTo) }
    private val restoreBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::restoreFrom) }
    private val createPdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uri?.let(viewModel::exportPdfTo) }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refreshReminderStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.openSessionFromReminder(intent.getStringExtra(EXTRA_OPEN_SESSION_ID))
        viewModel.refreshReminderStatus()
        viewModel.reconcileReminders()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SleepCheckTheme {
                DaycareApp(
                    state,
                    viewModel,
                    createBackup::launch,
                    { restoreBackup.launch(arrayOf(it)) },
                    createPdf::launch,
                    ::setRemindersEnabled,
                    ::openPreciseReminderSettings,
                    { viewModel.purchasePro(this@MainActivity) },
                    viewModel::refreshProEntitlement,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.openSessionFromReminder(intent.getStringExtra(EXTRA_OPEN_SESSION_ID))
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshReminderStatus()
        viewModel.reconcileReminders()
        viewModel.refreshProEntitlement()
    }

    private fun setRemindersEnabled(enabled: Boolean) {
        viewModel.setRemindersEnabled(enabled)
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openPreciseReminderSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
        }
    }

    companion object {
        const val EXTRA_OPEN_SESSION_ID = "extra_open_session_id"
    }
}
