package com.daycare.sleepcheck.log.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daycare.sleepcheck.log.R
import com.daycare.sleepcheck.log.SleepUiState
import com.daycare.sleepcheck.log.SleepViewModel
import com.daycare.sleepcheck.log.UiMessage
import com.daycare.sleepcheck.log.UiMessageType
import com.daycare.sleepcheck.log.billing.BillingMessage
import com.daycare.sleepcheck.log.billing.ProAccessPolicy
import com.daycare.sleepcheck.log.billing.ProEntitlement
import com.daycare.sleepcheck.log.billing.ProFeature
import com.daycare.sleepcheck.log.data.CheckRecordEntity
import com.daycare.sleepcheck.log.data.JurisdictionProfile
import com.daycare.sleepcheck.log.data.ObservationType
import com.daycare.sleepcheck.log.data.SleepSessionEntity
import com.daycare.sleepcheck.log.domain.ReminderScheduling
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.collectLatest

private enum class Screen { HOME, SESSIONS, CHECKS, SESSION, HISTORY, PEOPLE, SETTINGS, CHECKLIST, PRO, MORE }
private data class FeedbackDialogData(val title: String, val body: String)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DaycareApp(
    state: SleepUiState,
    vm: SleepViewModel,
    backup: (String) -> Unit,
    restore: (String) -> Unit,
    pdf: (String) -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onRequestPreciseReminders: () -> Unit,
    onPurchasePro: () -> Unit,
    onRestorePro: () -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(if (state.facility == null) Screen.SETTINGS else Screen.HOME) }
    var sessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var sessionOpenedFromReminder by rememberSaveable { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<FeedbackDialogData?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources

    fun openSession(id: String, fromReminder: Boolean = false) {
        sessionId = id
        sessionOpenedFromReminder = fromReminder
        screen = Screen.SESSION
    }

    LaunchedEffect(Unit) {
        vm.messages.collectLatest { message ->
            if (message.type == UiMessageType.CHECK_SAVED || message.type == UiMessageType.CHECK_SAVED_REMINDER_FAILED) {
                sessionId = null
                sessionOpenedFromReminder = false
                screen = Screen.HOME
            }
            when (message.type) {
                UiMessageType.BACKUP_CREATED -> dialog = FeedbackDialogData(resources.getString(R.string.backup_complete_title), resources.getString(R.string.backup_complete_body))
                UiMessageType.RESTORED -> dialog = FeedbackDialogData(resources.getString(R.string.restore_complete_title), resources.getString(R.string.restore_complete_body))
                UiMessageType.PDF_EXPORTED -> dialog = FeedbackDialogData(resources.getString(R.string.pdf_exported_title), resources.getString(R.string.pdf_exported_body))
                else -> snackbarHostState.showSnackbar(DaycareSnackbarVisuals(uiMessageText(resources, message), if (message.type.isFailure()) StatusTone.Error else StatusTone.Success))
            }
        }
    }
    LaunchedEffect(state.requestedSessionId, state.facility?.id) {
        state.requestedSessionId?.takeIf { state.facility != null }?.let {
            openSession(it, state.requestedSessionFromReminder)
            vm.clearRequestedSession()
        }
    }

    if (state.facility == null) {
        SetupScreen(vm)
    } else {
        LaunchedEffect(state.facility.id) { if (screen == Screen.SETTINGS) screen = Screen.HOME }
        Scaffold(
            topBar = { DaycareTopBar(screen, { screen = Screen.HOME }, { screen = Screen.SETTINGS }) },
            bottomBar = {
                if (screen != Screen.SESSION && screen != Screen.PRO && screen != Screen.SETTINGS && screen != Screen.CHECKLIST && screen != Screen.PEOPLE) DaycareBottomNavigation(screen) { screen = it }
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    val visuals = data.visuals as? DaycareSnackbarVisuals
                    if (visuals?.tone == StatusTone.Error) Snackbar(data, modifier = Modifier.padding(12.dp), shape = DaycareShapes.SmallCard, containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                    else SuccessSnackbar(data)
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    Screen.HOME -> HomeScreen(state, vm, ::openSession) { screen = it }
                    Screen.SESSIONS -> SessionsScreen(state, vm, ::openSession)
                    Screen.CHECKS -> ChecksScreen(state, ::openSession)
                    Screen.SESSION -> SessionScreen(state, vm, sessionId.orEmpty(), sessionOpenedFromReminder, onHistory = { screen = Screen.HISTORY })
                    Screen.HISTORY -> HistoryScreen(state, vm, { if (ProAccessPolicy.canAccess(ProFeature.PDF_EXPORT, state.proEntitlement, state.rooms.size)) pdf(it) else screen = Screen.PRO }) { screen = Screen.PRO }
                    Screen.PEOPLE -> PeopleScreen(state, vm) { screen = Screen.PRO }
                    Screen.SETTINGS -> SettingsScreen(state, vm, backup, restore, onReminderToggle, onRequestPreciseReminders) { screen = Screen.PRO }
                    Screen.CHECKLIST -> ChecklistScreen(state, vm)
                    Screen.PRO -> ProScreen(state, onPurchasePro, onRestorePro)
                    Screen.MORE -> MoreScreen(state) { screen = it }
                }
            }
        }
    }
    dialog?.let { value -> SuccessDialog(value.title, value.body, { dialog = null }, stringResource(R.string.done)) }
}

private fun UiMessageType.isFailure(): Boolean = this in setOf(UiMessageType.INVALID_BACKUP, UiMessageType.MISSING_STAFF, UiMessageType.SESSION_MISSING, UiMessageType.CONFIRMATION_REQUIRED, UiMessageType.CHECK_SAVE_FAILED, UiMessageType.PDF_EXPORT_FAILED, UiMessageType.CHECK_SAVED_REMINDER_FAILED)

private fun uiMessageText(resources: android.content.res.Resources, message: UiMessage): String = when (message.type) {
    UiMessageType.SETUP_SAVED -> resources.getString(R.string.session_started)
    UiMessageType.CHECK_SAVED -> resources.getString(R.string.check_saved_next_due, time(message.nextScheduledAt ?: System.currentTimeMillis()))
    UiMessageType.CHECK_SAVED_REMINDER_FAILED -> resources.getString(R.string.check_saved_reminder_failed, time(message.nextScheduledAt ?: System.currentTimeMillis()))
    UiMessageType.CORRECTION_SAVED -> resources.getString(R.string.correction_saved)
    UiMessageType.CHECKLIST_SAVED -> resources.getString(R.string.checklist_saved)
    UiMessageType.BACKUP_CREATED -> resources.getString(R.string.backup_created)
    UiMessageType.RESTORED -> resources.getString(R.string.restore_complete)
    UiMessageType.INVALID_BACKUP -> resources.getString(R.string.invalid_backup)
    UiMessageType.MISSING_STAFF -> resources.getString(R.string.check_error_missing_staff)
    UiMessageType.SESSION_MISSING -> resources.getString(R.string.check_error_session_missing)
    UiMessageType.CONFIRMATION_REQUIRED -> resources.getString(R.string.check_error_confirmation)
    UiMessageType.CHECK_SAVE_FAILED -> resources.getString(R.string.check_error_save)
    UiMessageType.PDF_EXPORTED -> resources.getString(R.string.pdf_exported_body)
    UiMessageType.PDF_EXPORT_FAILED -> resources.getString(R.string.pdf_export_failed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun DaycareTopBar(screen: Screen, onBack: () -> Unit, onSettings: () -> Unit) {
    TopAppBar(title = { Column { Text(titleFor(screen), style = MaterialTheme.typography.titleLarge); if (screen == Screen.HOME) Text(stringResource(R.string.home_header_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, navigationIcon = { if (screen != Screen.HOME) IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }, actions = { if (screen == Screen.HOME) { IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Settings, stringResource(R.string.open_settings)) }; IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.NotificationsNone, stringResource(R.string.reminders)) } } })
}

@Composable
private fun titleFor(screen: Screen): String = when (screen) {
    Screen.HOME -> stringResource(R.string.home)
    Screen.SESSIONS -> stringResource(R.string.sessions)
    Screen.CHECKS -> stringResource(R.string.checks)
    Screen.SESSION -> stringResource(R.string.checks)
    Screen.HISTORY -> stringResource(R.string.nav_history)
    Screen.PEOPLE -> stringResource(R.string.people)
    Screen.SETTINGS -> stringResource(R.string.settings)
    Screen.CHECKLIST -> stringResource(R.string.playground_checklist)
    Screen.PRO -> stringResource(R.string.open_daycare_pro)
    Screen.MORE -> stringResource(R.string.nav_more)
}

@Composable private fun DaycareBottomNavigation(screen: Screen, onSelected: (Screen) -> Unit) {
    val tabs = listOf(Triple(Screen.HOME, stringResource(R.string.nav_home), Icons.Default.Home), Triple(Screen.SESSIONS, stringResource(R.string.nav_sessions), Icons.Default.Bedtime), Triple(Screen.CHECKS, stringResource(R.string.nav_checks), Icons.Default.FactCheck), Triple(Screen.HISTORY, stringResource(R.string.nav_history), Icons.Default.History), Triple(Screen.MORE, stringResource(R.string.nav_more), Icons.Default.MoreHoriz))
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) { tabs.forEach { (target, label, icon) -> val selected = when (target) { Screen.SESSIONS -> screen == Screen.SESSIONS || screen == Screen.SESSION; Screen.MORE -> screen == Screen.MORE || screen == Screen.PEOPLE || screen == Screen.SETTINGS || screen == Screen.CHECKLIST || screen == Screen.PRO; else -> screen == target }; val navigationLabel = if (target == Screen.CHECKS) stringResource(R.string.nav_checks_compact) else label; NavigationBarItem(selected, { onSelected(target) }, icon = { Icon(icon, null) }, label = { Text(navigationLabel, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)) }) } }
}

@Composable private fun SetupScreen(vm: SleepViewModel) {
    var facility by rememberSaveable { mutableStateOf("") }; var room by rememberSaveable { mutableStateOf("") }; var staff by rememberSaveable { mutableStateOf("") }; var child by rememberSaveable { mutableStateOf("") }; var profile by rememberSaveable { mutableStateOf(JurisdictionProfile.ONTARIO) }; var interval by rememberSaveable { mutableStateOf("") }
    val validInterval = interval.toIntOrNull()?.let { it > 0 } == true
    LazyColumn(contentPadding = PaddingValues(DaycareSpacing.Page), verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) {
        item { ScreenIntro(stringResource(R.string.setup_title), stringResource(R.string.setup_subtitle)) }
        item { DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) { SectionHeader(stringResource(R.string.facility_information)); DaycareTextField(stringResource(R.string.facility_name_label), facility, { facility = it }); DaycareTextField(stringResource(R.string.room_name_label), room, { room = it }); DaycareTextField(stringResource(R.string.staff_name_label), staff, { staff = it }); DaycareTextField(stringResource(R.string.child_name_label), child, { child = it }) } } }
        item { DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) { SectionHeader(stringResource(R.string.jurisdiction_profile)); ProfileChips(profile) { selected -> profile = selected; interval = vm.defaultInterval(selected)?.toString().orEmpty() }; Text(profileNote(profile), color = MaterialTheme.colorScheme.onSurfaceVariant); DaycareTextField(stringResource(R.string.interval_minutes), interval, { interval = it.filter(Char::isDigit) }, isError = !validInterval, supportingText = if (!validInterval) stringResource(R.string.policy_interval_required) else null) } } }
        item { PrimaryActionButton(stringResource(R.string.save_setup), { vm.saveSetup(facility, room, staff, child, profile, interval.toInt()) }, Modifier.fillMaxWidth(), facility.isNotBlank() && room.isNotBlank() && staff.isNotBlank() && validInterval, Icons.Default.Check) }
        item { InfoCard(stringResource(R.string.local_records_title), stringResource(R.string.setup_privacy_reassurance), StatusTone.Success) }
    }
}

@Composable private fun HomeScreen(state: SleepUiState, vm: SleepViewModel, onSession: (String, Boolean) -> Unit, onNavigate: (Screen) -> Unit) {
    val first = state.activeSessions.firstOrNull(); val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis; val today = state.history.filter { it.recordedAt >= todayStart }; val next = state.activeSessions.map { session -> ReminderScheduling.nextReminderAt(session.startedAt, session.intervalMinutes, state.history.count { it.sessionId == session.id }) }.minOrNull()
    LazyColumn(contentPadding = PaddingValues(DaycareSpacing.Page), verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) {
        item { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(greetingText(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary); Text(state.facility?.name.orEmpty(), style = DaycareTypography.PageTitle); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Text(stringResource(R.string.local_records_short), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        item { if (first != null) ActiveSessionHero(state, first) { onSession(first.id, false) } else DaycareCard(hero = true, modifier = Modifier.fillMaxWidth()) { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { SurfaceIcon(Icons.Default.Bedtime, true); Column(Modifier.weight(1f)) { Text(stringResource(R.string.no_active_sessions), style = DaycareTypography.CardTitle); Text(stringResource(R.string.start_session_prompt), color = MaterialTheme.colorScheme.onSurfaceVariant) } }; PrimaryActionButton(stringResource(R.string.start_session), { startFirstSession(state, vm, onSession) }, Modifier.fillMaxWidth(), canStart(state), Icons.Default.PlayArrow) } } }
        if (first != null) item { PrimaryActionButton(stringResource(R.string.start_session), { startFirstSession(state, vm, onSession) }, Modifier.fillMaxWidth(), canStart(state), Icons.Default.Add) }
        if (state.activeSessions.size > 1) { item { SectionHeader(stringResource(R.string.other_active_sessions), state.activeSessions.size - 1) }; items(state.activeSessions.drop(1), key = { it.id }) { session -> SessionListCard(state, session) { onSession(session.id, false) } } }
        item { SectionHeader(stringResource(R.string.quick_actions)) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact), modifier = Modifier.fillMaxWidth()) { IconActionCard(Icons.Default.FactCheck, stringResource(R.string.quick_check), stringResource(R.string.quick_check_description), { onNavigate(Screen.CHECKS) }, Modifier.weight(1f)); IconActionCard(Icons.Default.History, stringResource(R.string.quick_history), stringResource(R.string.quick_history_description), { onNavigate(Screen.HISTORY) }, Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact), modifier = Modifier.fillMaxWidth()) { IconActionCard(Icons.Default.SportsSoccer, stringResource(R.string.quick_playground), stringResource(R.string.quick_playground_description), { onNavigate(Screen.CHECKLIST) }, Modifier.weight(1f)); IconActionCard(Icons.Default.Groups, stringResource(R.string.quick_people), stringResource(R.string.quick_people_description), { onNavigate(Screen.PEOPLE) }, Modifier.weight(1f)) } }
        item { DaycareCard { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { SurfaceIcon(Icons.Default.NotificationsNone); Column(Modifier.weight(1f)) { Text(stringResource(R.string.next_reminder), style = DaycareTypography.CardTitle); Text(next?.let { stringResource(R.string.next_check_due_at, time(it)) } ?: stringResource(if (state.remindersEnabled) R.string.no_active_sessions else R.string.reminders_off), color = MaterialTheme.colorScheme.onSurfaceVariant) }; next?.let { StatusPill(timeUntil(it), if (it <= System.currentTimeMillis()) StatusTone.Warning else StatusTone.Success) } } } }
        item { SectionHeader(stringResource(R.string.today_summary)) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { SummaryMetric(today.size.toString(), stringResource(R.string.checks_today), Modifier.weight(1f)); SummaryMetric(today.count { it.isLate }.toString(), stringResource(R.string.late_checks_today), Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { SummaryMetric(state.activeSessions.size.toString(), stringResource(R.string.active_rooms_today), Modifier.weight(1f)); SummaryMetric(today.count { it.observationType == ObservationType.EXCEPTION.name }.toString(), stringResource(R.string.exceptions_today), Modifier.weight(1f)) } }
        item { SectionHeader(stringResource(R.string.quick_access)) }
        item { AccessRow(Icons.Default.Settings, stringResource(R.string.open_settings), stringResource(R.string.jurisdiction_short)) { onNavigate(Screen.SETTINGS) } }
        item { AccessRow(Icons.Default.Backup, stringResource(R.string.backup_restore), stringResource(R.string.backup_short)) { onNavigate(Screen.SETTINGS) } }
    }
}

private fun canStart(state: SleepUiState) = state.rooms.isNotEmpty() && state.children.isNotEmpty() && (state.facility?.intervalMinutes ?: 0) > 0
private fun startFirstSession(state: SleepUiState, vm: SleepViewModel, onSession: (String, Boolean) -> Unit) { state.rooms.firstOrNull()?.let { vm.startSession(it.id) { onSession(it, false) } } }

@Composable private fun ActiveSessionHero(state: SleepUiState, session: SleepSessionEntity, onClick: () -> Unit) {
    val room = state.rooms.firstOrNull { it.id == session.roomId }?.name.orEmpty(); val last = state.history.filter { it.sessionId == session.id }.maxByOrNull { it.recordedAt }; val next = ReminderScheduling.nextReminderAt(session.startedAt, session.intervalMinutes, state.history.count { it.sessionId == session.id })
    DaycareCard(hero = true, modifier = Modifier.fillMaxWidth()) { Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.clickable(onClick = onClick)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { SurfaceIcon(Icons.Default.Bedtime, true); Column(Modifier.weight(1f)) { Text(stringResource(R.string.active_sleep_session), style = DaycareTypography.CardTitle); Text(room, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } }; StatusPill(stringResource(R.string.active_status), StatusTone.Success); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { MetricText(stringResource(R.string.started_at), time(session.startedAt), Modifier.weight(1f)); MetricText(stringResource(R.string.interval_short), stringResource(R.string.interval_summary, session.intervalMinutes), Modifier.weight(1f)) }; Divider(color = MaterialTheme.colorScheme.outlineVariant); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { MetricText(stringResource(R.string.last_check), last?.let { time(it.observedAt) } ?: stringResource(R.string.not_recorded), Modifier.weight(1f)); MetricText(stringResource(R.string.next_check), time(next), Modifier.weight(1f)) }; Row(verticalAlignment = Alignment.CenterVertically) { Text(timeUntil(next), color = if (next <= System.currentTimeMillis()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, stringResource(R.string.open_session), tint = MaterialTheme.colorScheme.primary) } } }
}

@Composable private fun SessionsScreen(state: SleepUiState, vm: SleepViewModel, onSession: (String, Boolean) -> Unit) { LazyColumn(contentPadding = PaddingValues(DaycareSpacing.Page), verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) { item { ScreenIntro(stringResource(R.string.nav_sessions), stringResource(R.string.sessions_subtitle)) }; item { PrimaryActionButton(stringResource(R.string.start_session), { startFirstSession(state, vm, onSession) }, Modifier.fillMaxWidth(), canStart(state), Icons.Default.Add) }; if (state.activeSessions.isEmpty()) item { EmptyState(Icons.Default.Bedtime, stringResource(R.string.no_active_sessions), stringResource(R.string.start_session_prompt)) }; items(state.activeSessions, key = { it.id }) { session -> SessionListCard(state, session) { onSession(session.id, false) } } } }
@Composable private fun ChecksScreen(state: SleepUiState, onSession: (String, Boolean) -> Unit) { LazyColumn(contentPadding = PaddingValues(DaycareSpacing.Page), verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) { item { ScreenIntro(stringResource(R.string.nav_checks), stringResource(R.string.checks_subtitle)) }; if (state.activeSessions.isEmpty()) item { EmptyState(Icons.Default.FactCheck, stringResource(R.string.no_active_sessions), stringResource(R.string.start_session_prompt)) }; items(state.activeSessions, key = { it.id }) { session -> SessionListCard(state, session) { onSession(session.id, false) } } } }

@Composable private fun SessionListCard(state: SleepUiState, session: SleepSessionEntity, onClick: () -> Unit) { val room = state.rooms.firstOrNull { it.id == session.roomId }?.name.orEmpty(); val next = ReminderScheduling.nextReminderAt(session.startedAt, session.intervalMinutes, state.history.count { it.sessionId == session.id }); DaycareCard(modifier = Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.clickable(onClick = onClick)) { SurfaceIcon(Icons.Default.Room); Column(Modifier.weight(1f)) { Text(room, style = DaycareTypography.CardTitle); Text(stringResource(R.string.next_check_due_at, time(next)), color = MaterialTheme.colorScheme.onSurfaceVariant) }; StatusPill(stringResource(R.string.active_status), StatusTone.Success); Icon(Icons.Default.ChevronRight, stringResource(R.string.open_session), tint = MaterialTheme.colorScheme.primary) } } }

@Composable private fun SessionScreen(state: SleepUiState, vm: SleepViewModel, id: String, openedFromReminder: Boolean, onHistory: () -> Unit) {
    var confirmed by rememberSaveable(id, state.sessionFormResetVersion) { mutableStateOf(false) }; var exception by rememberSaveable(id, state.sessionFormResetVersion) { mutableStateOf(false) }; var notes by rememberSaveable(id, state.sessionFormResetVersion) { mutableStateOf("") }
    val session = state.activeSessions.firstOrNull { it.id == id }; val completing = id in state.completingSessionIds; val room = session?.let { current -> state.rooms.firstOrNull { it.id == current.roomId }?.name }.orEmpty(); val children = session?.let { current -> state.children.count { it.roomId == current.roomId } } ?: 0; val next = session?.let { current -> ReminderScheduling.nextReminderAt(current.startedAt, current.intervalMinutes, state.history.count { it.sessionId == current.id }) }; val due = openedFromReminder || (next?.let { it <= System.currentTimeMillis() } == true)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = DaycareSpacing.Page, top = DaycareSpacing.Page, end = DaycareSpacing.Page, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) {
        item { DaycareCard(hero = true) { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.whole_room_check), style = DaycareTypography.PageTitle)
            Text(room, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StatusPill(if (due) stringResource(R.string.check_due_now) else stringResource(R.string.active_status), if (due) StatusTone.Warning else StatusTone.Success)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { MetricText(stringResource(R.string.interval_short), session?.let { stringResource(R.string.interval_summary, it.intervalMinutes) }.orEmpty(), Modifier.weight(1f)); MetricText(stringResource(R.string.children_count_label), children.toString(), Modifier.weight(1f)) }
            next?.let { InfoCard(stringResource(R.string.next_check), stringResource(R.string.next_check_due_at, time(it)), if (due) StatusTone.Warning else StatusTone.Success) }
        } } }
        item { DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(stringResource(R.string.check_prompt_title), style = DaycareTypography.SectionTitle); Text(stringResource(R.string.whole_room_prompt), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(stringResource(R.string.children_count, children), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) } } }
        item { DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { CheckChoiceRow(confirmed, { confirmed = it }, stringResource(R.string.direct_visual_confirmation), stringResource(R.string.direct_visual_confirmation_support), Icons.Default.Security); CheckChoiceRow(exception, { exception = it }, stringResource(R.string.exception_observation), stringResource(R.string.exception_observation_support), Icons.Default.WarningAmber) } } }
        item { DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(stringResource(R.string.observation), style = DaycareTypography.SectionTitle); StatusPill(if (exception) stringResource(R.string.exception_observation) else stringResource(R.string.normal_observation), if (exception) StatusTone.Warning else StatusTone.Success); DaycareTextField(stringResource(R.string.notes_label), notes, { notes = it }, singleLine = false) } } }
        item { SecondaryActionButton(stringResource(R.string.open_history), onHistory, Modifier.fillMaxWidth(), icon = Icons.Default.History) }
        if (session != null) item { SecondaryActionButton(stringResource(R.string.close_session), { vm.closeSession(id) }, Modifier.fillMaxWidth(), icon = Icons.Default.Close) }
        if (!confirmed) item { WarningBanner(stringResource(R.string.confirmation_required), stringResource(R.string.check_confirmation_hint), StatusTone.Warning) }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding().padding(horizontal = DaycareSpacing.Page, vertical = 12.dp)) {
            PrimaryActionButton(if (completing) stringResource(R.string.check_saving) else stringResource(R.string.complete_check), { vm.completeCheck(id, exception, notes, confirmed) }, Modifier.fillMaxWidth(), confirmed && !completing && session != null, if (completing) null else Icons.Default.Check)
        }
    }
}

@Composable private fun HistoryScreen(state: SleepUiState, vm: SleepViewModel, onPdf: (String) -> Unit, onRequestPro: () -> Unit) {
    var correctionFor by remember { mutableStateOf<CheckRecordEntity?>(null) }; var search by rememberSaveable { mutableStateOf("") }; var exceptionsOnly by rememberSaveable { mutableStateOf(false) }; val pdfFilename = stringResource(R.string.pdf_default_filename); val advanced = ProAccessPolicy.canAccess(ProFeature.ADVANCED_HISTORY, state.proEntitlement, state.rooms.size); val visible = if (!advanced) state.history else state.history.filter { record -> (!exceptionsOnly || record.observationType == ObservationType.EXCEPTION.name) && (search.isBlank() || record.notes.contains(search, true) || state.rooms.firstOrNull { it.id == record.roomId }?.name?.contains(search, true) == true) }
    LazyColumn(contentPadding = PaddingValues(DaycareSpacing.Page), verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) {
        item { ScreenIntro(stringResource(R.string.history), stringResource(R.string.history_subtitle)) }
        item { PrimaryActionButton(stringResource(R.string.export_pdf), { onPdf(pdfFilename) }, Modifier.fillMaxWidth(), icon = Icons.Default.PictureAsPdf) }
        item { if (advanced) { DaycareTextField(stringResource(R.string.pro_history_search), search, { search = it }); Spacer(Modifier.height(8.dp)); FilterChip(exceptionsOnly, { exceptionsOnly = !exceptionsOnly }, label = { Text(stringResource(R.string.pro_history_exceptions_only)) }, leadingIcon = { Icon(Icons.Default.WarningAmber, null) }) } else { InfoCard(stringResource(R.string.pro_history_tools), stringResource(R.string.pro_history_tools_description)); SecondaryActionButton(stringResource(R.string.open_daycare_pro), onRequestPro, Modifier.fillMaxWidth()) } }
        if (visible.isEmpty()) item { EmptyState(Icons.Default.History, stringResource(R.string.no_history), stringResource(R.string.history_empty_subtitle)) }
        items(visible, key = { it.id }) { record -> HistoryRecordCard(state, record) { correctionFor = record } }
        if (state.corrections.isNotEmpty()) { item { SectionHeader(stringResource(R.string.correction_history)) }; items(state.corrections, key = { it.id }) { Text(stringResource(R.string.correction_entry, it.correctionReason, time(it.createdAt)), modifier = Modifier.padding(horizontal = 4.dp)) } }
    }
    correctionFor?.let { record -> CorrectionDialog(record, { correctionFor = null }) { reason, notes -> vm.addCorrection(record, reason, notes); correctionFor = null } }
}

@Composable private fun HistoryRecordCard(state: SleepUiState, record: CheckRecordEntity, onCorrection: () -> Unit) { val room = state.rooms.firstOrNull { it.id == record.roomId }?.name.orEmpty(); val staff = state.staff.firstOrNull { it.id == record.staffId }?.name.orEmpty(); val exception = record.observationType == ObservationType.EXCEPTION.name; DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(if (exception) Icons.Default.WarningAmber else Icons.Default.CheckCircle, null, tint = if (exception) DaycareColors.Amber else MaterialTheme.colorScheme.primary); Text(if (exception) stringResource(R.string.exception_observation) else stringResource(R.string.normal_observation), style = DaycareTypography.CardTitle) }; StatusPill(if (record.isLate) stringResource(R.string.late_check) else stringResource(R.string.on_time_check), if (record.isLate) StatusTone.Warning else StatusTone.Success) }; Text(stringResource(R.string.room_label, room), fontWeight = FontWeight.Medium); if (staff.isNotBlank()) Text(stringResource(R.string.observed_by, staff), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(stringResource(R.string.recorded_times, time(record.scheduledAt), time(record.observedAt), time(record.recordedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (record.notes.isNotBlank()) InfoCard(stringResource(R.string.notes_label), record.notes); TextButton(onClick = onCorrection, modifier = Modifier.heightIn(min = 48.dp)) { Icon(Icons.Default.Tune, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.add_correction)) } } } }

@Composable private fun CorrectionDialog(record: CheckRecordEntity, onDismiss: () -> Unit, onSave: (String, String) -> Unit) { var reason by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.correction)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { DaycareTextField(stringResource(R.string.correction_reason), reason, { reason = it }); DaycareTextField(stringResource(R.string.notes_label), notes, { notes = it }, singleLine = false) } }, confirmButton = { TextButton(onClick = { onSave(reason, notes) }, enabled = reason.isNotBlank()) { Text(stringResource(R.string.save_correction)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }) }

@Composable private fun PeopleScreen(state: SleepUiState, vm: SleepViewModel, onRequestPro: () -> Unit) { var room by rememberSaveable { mutableStateOf("") }; var staff by rememberSaveable { mutableStateOf("") }; var child by rememberSaveable { mutableStateOf("") }; LazyColumn(contentPadding = PaddingValues(DaycareSpacing.Page), verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) { item { ScreenIntro(stringResource(R.string.people), stringResource(R.string.people_subtitle)) }; item { PeopleSection(Icons.Default.Room, stringResource(R.string.rooms_section), state.rooms.size, stringResource(R.string.add_room), room, { room = it }, { if (room.isNotBlank()) { if (ProAccessPolicy.canAccess(ProFeature.SECOND_ROOM, state.proEntitlement, state.rooms.size)) { vm.addRoom(room); room = "" } else onRequestPro() } }, state.rooms.map { it.name }) }; item { PeopleSection(Icons.Default.Person, stringResource(R.string.staff_section), state.staff.size, stringResource(R.string.add_staff), staff, { staff = it }, { if (staff.isNotBlank()) { vm.addStaff(staff); staff = "" } }, state.staff.map { it.name }) }; item { PeopleSection(Icons.Default.Groups, stringResource(R.string.children_section), state.children.size, stringResource(R.string.add_child), child, { child = it }, { state.rooms.firstOrNull()?.let { roomEntity -> if (child.isNotBlank()) { vm.addChild(roomEntity.id, child); child = "" } } }, state.children.map { it.name }, state.rooms.isNotEmpty()) } } }
@Composable private fun PeopleSection(icon: ImageVector, title: String, count: Int, fieldLabel: String, value: String, onValue: (String) -> Unit, onAdd: () -> Unit, rows: List<String>, enabled: Boolean = true) { DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); SectionHeader(title, count) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { DaycareTextField(fieldLabel, value, onValue, Modifier.weight(1f)); IconButton(onClick = onAdd, enabled = enabled && value.isNotBlank(), modifier = Modifier.size(52.dp)) { Icon(Icons.Default.Add, fieldLabel) } }; if (rows.isEmpty()) Text(stringResource(R.string.none_added_yet), color = MaterialTheme.colorScheme.onSurfaceVariant); rows.forEach { name -> DaycareListRow(name, icon = icon) } } } }

@Composable private fun SettingsScreen(state: SleepUiState, vm: SleepViewModel, backup: (String) -> Unit, restore: (String) -> Unit, onReminderToggle: (Boolean) -> Unit, onRequestPreciseReminders: () -> Unit, onRequestPro: () -> Unit) { val current = state.facility ?: return; val backupFilename = stringResource(R.string.backup_default_filename); var profile by rememberSaveable(current.jurisdiction) { mutableStateOf(runCatching { JurisdictionProfile.valueOf(current.jurisdiction) }.getOrDefault(JurisdictionProfile.CUSTOM)) }; var interval by rememberSaveable(current.intervalMinutes) { mutableStateOf(current.intervalMinutes.takeIf { it > 0 }?.toString().orEmpty()) }; LazyColumn(contentPadding = PaddingValues(DaycareSpacing.Page), verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) { item { ScreenIntro(stringResource(R.string.settings), stringResource(R.string.settings_subtitle)) }; item { DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { SectionHeader(stringResource(R.string.jurisdiction_profile)); ProfileChips(profile) { selected -> profile = selected; interval = vm.defaultInterval(selected)?.toString().orEmpty() }; Text(profileNote(profile), color = MaterialTheme.colorScheme.onSurfaceVariant); DaycareTextField(stringResource(R.string.interval_minutes), interval, { interval = it.filter(Char::isDigit) }, isError = interval.toIntOrNull()?.let { it > 0 } != true); PrimaryActionButton(stringResource(R.string.save_settings), { vm.saveSettings(profile, interval.toIntOrNull()) }, Modifier.fillMaxWidth(), interval.toIntOrNull()?.let { it > 0 } == true, Icons.Default.Check) } } }; item { DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { SectionHeader(stringResource(R.string.reminders)); Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.NotificationsNone, null, tint = MaterialTheme.colorScheme.primary); Text(stringResource(R.string.enable_reminders), Modifier.weight(1f).padding(horizontal = 12.dp)); Switch(checked = state.remindersEnabled, onCheckedChange = onReminderToggle) }; InfoCard(stringResource(R.string.reminder_status), if (state.preciseRemindersAvailable) stringResource(R.string.precise_reminders_available) else stringResource(R.string.precise_reminders_unavailable), if (state.preciseRemindersAvailable) StatusTone.Success else StatusTone.Warning); if (!state.preciseRemindersAvailable) SecondaryActionButton(stringResource(R.string.open_precise_settings), onRequestPreciseReminders, Modifier.fillMaxWidth()); if (state.remindersEnabled && !state.notificationsAllowed) WarningBanner(stringResource(R.string.notifications_unavailable), stringResource(R.string.notifications_unavailable), StatusTone.Error) } } }; item { DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { SectionHeader(stringResource(R.string.backup_restore)); Text(stringResource(R.string.backup_restore_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant); PrimaryActionButton(stringResource(R.string.backup_data), { if (state.proEntitlement == ProEntitlement.PRO) backup(backupFilename) else onRequestPro() }, Modifier.fillMaxWidth(), icon = Icons.Default.Backup); SecondaryActionButton(stringResource(R.string.restore_data), { if (state.proEntitlement == ProEntitlement.PRO) restore("application/json") else onRequestPro() }, Modifier.fillMaxWidth(), icon = Icons.Default.Folder) } } }; item { DaycareCard(modifier = Modifier.fillMaxWidth()) { AccessRow(Icons.Default.Security, stringResource(R.string.open_daycare_pro), stringResource(R.string.pro_screen_subtitle), onRequestPro) } } } }

@Composable private fun ChecklistScreen(state: SleepUiState, vm: SleepViewModel) { val saved = state.checklist; var surface by rememberSaveable(saved?.id) { mutableStateOf(saved?.surfaceSafe ?: false) }; var equipment by rememberSaveable(saved?.id) { mutableStateOf(saved?.equipmentSafe ?: false) }; var gate by rememberSaveable(saved?.id) { mutableStateOf(saved?.gateSafe ?: false) }; LazyColumn(contentPadding = PaddingValues(DaycareSpacing.Page), verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) { item { ScreenIntro(stringResource(R.string.playground_checklist), stringResource(R.string.playground_subtitle)) }; item { DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { if (saved != null) InfoCard(stringResource(R.string.checklist_status_saved), stringResource(R.string.checklist_status_saved), StatusTone.Success); CheckChoiceRow(surface, { surface = it }, stringResource(R.string.checklist_item_surface), null, Icons.Default.SportsSoccer); CheckChoiceRow(equipment, { equipment = it }, stringResource(R.string.checklist_item_equipment), null, Icons.Default.Security); CheckChoiceRow(gate, { gate = it }, stringResource(R.string.checklist_item_gate), null, Icons.Default.Security) } } }; item { PrimaryActionButton(stringResource(R.string.save_checklist), { vm.saveChecklist(surface, equipment, gate) }, Modifier.fillMaxWidth(), icon = Icons.Default.Check) } } }

@Composable private fun ProScreen(state: SleepUiState, onPurchase: () -> Unit, onRestore: () -> Unit) { val price = state.proPrice ?: stringResource(R.string.pro_price_unavailable); LazyColumn(contentPadding = PaddingValues(DaycareSpacing.Page), verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) { item { androidx.compose.material3.Surface(color = DaycareColors.DeepGreen, shape = DaycareShapes.LargeCard, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.Security, null, tint = Color.White, modifier = Modifier.size(38.dp)); Text(stringResource(R.string.pro_screen_title), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold); Text(stringResource(R.string.pro_screen_subtitle), color = Color(0xFFDDEFE3)) } } }; item { Text(stringResource(R.string.pro_includes), style = DaycareTypography.SectionTitle) }; item { ProBenefit(Icons.Default.Room, R.string.pro_benefit_unlimited_rooms, R.string.pro_benefit_unlimited_rooms_description) }; item { ProBenefit(Icons.Default.PictureAsPdf, R.string.pro_benefit_pdf, R.string.pro_benefit_pdf_description) }; item { ProBenefit(Icons.Default.Search, R.string.pro_benefit_history, R.string.pro_benefit_history_description) }; item { ProBenefit(Icons.Default.Backup, R.string.pro_benefit_backup, R.string.pro_benefit_backup_description) }; item { ProBenefit(Icons.Default.SportsSoccer, R.string.pro_benefit_playground, R.string.pro_benefit_playground_description) }; item { DaycareCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(stringResource(R.string.pro_price_once, price), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(stringResource(R.string.pro_lifetime_access), color = MaterialTheme.colorScheme.onSurfaceVariant); PrimaryActionButton(stringResource(R.string.pro_unlock_cta, price), onPurchase, Modifier.fillMaxWidth(), state.proEntitlement != ProEntitlement.PRO && state.proPrice != null, Icons.Default.Security); SecondaryActionButton(stringResource(R.string.pro_restore_purchase), onRestore, Modifier.fillMaxWidth()) } } }; item { InfoCard(stringResource(R.string.pro_footer_title), stringResource(R.string.pro_footer), StatusTone.Neutral) }; state.billingMessage?.let { message -> item { InfoCard(stringResource(R.string.billing_status), billingMessageText(message), if (message == BillingMessage.BILLING_UNAVAILABLE || message == BillingMessage.PURCHASE_FAILED) StatusTone.Error else StatusTone.Warning) } } } }
@Composable private fun ProBenefit(icon: ImageVector, title: Int, description: Int) { DaycareCard { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { SurfaceIcon(icon); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(stringResource(title), style = DaycareTypography.CardTitle); Text(stringResource(description), color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
@Composable private fun MoreScreen(state: SleepUiState, onNavigate: (Screen) -> Unit) { LazyColumn(contentPadding = PaddingValues(DaycareSpacing.Page), verticalArrangement = Arrangement.spacedBy(DaycareSpacing.Compact)) { item { ScreenIntro(stringResource(R.string.nav_more), stringResource(R.string.more_subtitle)) }; item { AccessRow(Icons.Default.Groups, stringResource(R.string.people), stringResource(R.string.people_subtitle)) { onNavigate(Screen.PEOPLE) } }; item { AccessRow(Icons.Default.Settings, stringResource(R.string.settings), stringResource(R.string.settings_subtitle)) { onNavigate(Screen.SETTINGS) } }; item { AccessRow(Icons.Default.SportsSoccer, stringResource(R.string.playground_checklist), stringResource(R.string.playground_subtitle)) { if (ProAccessPolicy.canAccess(ProFeature.PLAYGROUND_SAFETY_LOG, state.proEntitlement, state.rooms.size)) onNavigate(Screen.CHECKLIST) else onNavigate(Screen.PRO) } }; item { AccessRow(Icons.Default.Security, stringResource(R.string.open_daycare_pro), stringResource(R.string.pro_screen_subtitle)) { onNavigate(Screen.PRO) } } } }

@Composable private fun ScreenIntro(title: String, subtitle: String) { Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = DaycareTypography.PageTitle); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun SurfaceIcon(icon: ImageVector, large: Boolean = false) { Box(Modifier.size(if (large) 58.dp else 44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(if (large) 32.dp else 24.dp)) } }
@Composable private fun SummaryMetric(value: String, label: String, modifier: Modifier = Modifier) { DaycareCard(modifier) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun MetricText(label: String, value: String, modifier: Modifier = Modifier) { Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) } }
@Composable private fun AccessRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) { DaycareCard(modifier = Modifier.fillMaxWidth()) { Row(Modifier.clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { SurfaceIcon(icon); Column(Modifier.weight(1f)) { Text(title, style = DaycareTypography.CardTitle); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }; Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary) } } }
@Composable private fun EmptyState(icon: ImageVector, title: String, body: String) { DaycareCard(modifier = Modifier.fillMaxWidth()) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { SurfaceIcon(icon, true); Text(title, style = DaycareTypography.CardTitle); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun CheckChoiceRow(checked: Boolean, onChecked: (Boolean) -> Unit, label: String, support: String?, icon: ImageVector) { Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onChecked(!checked) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(icon, null, tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant); Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium); support?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Checkbox(checked = checked, onCheckedChange = onChecked, modifier = Modifier.semantics { contentDescription = label }) } }
@Composable private fun ProfileChips(selected: JurisdictionProfile, onSelected: (JurisdictionProfile) -> Unit) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { JurisdictionProfile.values().forEach { item -> FilterChip(selected == item, { onSelected(item) }, label = { Text(profileLabel(item)) }) } } }
@Composable private fun profileNote(profile: JurisdictionProfile): String = when (profile) { JurisdictionProfile.ONTARIO -> stringResource(R.string.ontario_policy_note); JurisdictionProfile.CALIFORNIA -> stringResource(R.string.california_policy_note); JurisdictionProfile.CUSTOM -> stringResource(R.string.custom_policy_note) }
@Composable private fun profileLabel(profile: JurisdictionProfile): String = when (profile) { JurisdictionProfile.ONTARIO -> stringResource(R.string.ontario); JurisdictionProfile.CALIFORNIA -> stringResource(R.string.california); JurisdictionProfile.CUSTOM -> stringResource(R.string.custom) }
@Composable private fun billingMessageText(message: BillingMessage): String = when (message) { BillingMessage.PURCHASE_PENDING -> stringResource(R.string.pro_purchase_pending); BillingMessage.PURCHASE_CANCELED -> stringResource(R.string.pro_purchase_canceled); BillingMessage.BILLING_UNAVAILABLE -> stringResource(R.string.pro_billing_unavailable); BillingMessage.PURCHASE_FAILED -> stringResource(R.string.pro_purchase_failed); BillingMessage.PURCHASE_RESTORED -> stringResource(R.string.pro_purchase_restored) }
@Composable private fun greetingText(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) { in 5..11 -> stringResource(R.string.greeting_morning); in 12..17 -> stringResource(R.string.greeting_afternoon); else -> stringResource(R.string.greeting_evening) }
private fun time(value: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(value))
@Composable
private fun timeUntil(value: Long): String = if (value <= System.currentTimeMillis()) stringResource(R.string.due_now_short) else { val minutes = ((value - System.currentTimeMillis()) / 60_000L).coerceAtLeast(1); stringResource(R.string.due_in_minutes, minutes) }
