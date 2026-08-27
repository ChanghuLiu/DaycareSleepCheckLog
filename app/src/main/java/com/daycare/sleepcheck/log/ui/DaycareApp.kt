package com.daycare.sleepcheck.log.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.daycare.sleepcheck.log.R
import com.daycare.sleepcheck.log.SleepUiState
import com.daycare.sleepcheck.log.SleepViewModel
import com.daycare.sleepcheck.log.UiMessage
import com.daycare.sleepcheck.log.UiMessageType
import com.daycare.sleepcheck.log.billing.BillingMessage
import com.daycare.sleepcheck.log.billing.ProAccessPolicy
import com.daycare.sleepcheck.log.billing.ProEntitlement
import com.daycare.sleepcheck.log.billing.ProFeature
import com.daycare.sleepcheck.log.data.*
import com.daycare.sleepcheck.log.domain.ReminderScheduling
import java.text.DateFormat
import java.util.Date

private enum class Screen { HOME, SESSION, HISTORY, PEOPLE, SETTINGS, CHECKLIST, PRO }

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
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources
    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            if (message.type == UiMessageType.CHECK_SAVED || message.type == UiMessageType.CHECK_SAVED_REMINDER_FAILED) {
                sessionId = null
                sessionOpenedFromReminder = false
                screen = Screen.HOME
            }
            snackbarHostState.showSnackbar(uiMessageText(resources, message))
        }
    }
    LaunchedEffect(state.requestedSessionId, state.facility?.id) {
        state.requestedSessionId?.takeIf { state.facility != null }?.let {
            sessionId = it
            sessionOpenedFromReminder = state.requestedSessionFromReminder
            screen = Screen.SESSION
            vm.clearRequestedSession()
        }
    }
    if (state.facility == null) SetupScreen(vm)
    else {
        LaunchedEffect(state.facility.id) { if (screen == Screen.SETTINGS) screen = Screen.HOME }
        Scaffold(
            topBar = { TopAppBar(title = { Text(titleFor(screen)) }, navigationIcon = { if (screen != Screen.HOME) TextButton(onClick = { screen = Screen.HOME }) { Text(stringResource(R.string.back)) } }) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (screen) {
                    Screen.HOME -> HomeScreen(state, vm, onSession = { id -> sessionId = id; sessionOpenedFromReminder = false; screen = Screen.SESSION }, onNavigate = { target ->
                        if (target == Screen.CHECKLIST && !ProAccessPolicy.canAccess(ProFeature.PLAYGROUND_SAFETY_LOG, state.proEntitlement, state.rooms.size)) screen = Screen.PRO else screen = target
                    })
                    Screen.SESSION -> SessionScreen(state, vm, sessionId ?: "", sessionOpenedFromReminder, state.sessionFormResetVersion, onHistory = { screen = Screen.HISTORY })
                    Screen.HISTORY -> HistoryScreen(state, vm, onPdf = { if (ProAccessPolicy.canAccess(ProFeature.PDF_EXPORT, state.proEntitlement, state.rooms.size)) pdf(it) else screen = Screen.PRO }, onRequestPro = { screen = Screen.PRO })
                    Screen.PEOPLE -> PeopleScreen(state, vm, onRequestPro = { screen = Screen.PRO })
                    Screen.SETTINGS -> SettingsScreen(state, vm, backup, restore, onReminderToggle, onRequestPreciseReminders, onRequestPro = { screen = Screen.PRO })
                    Screen.CHECKLIST -> ChecklistScreen(state, vm)
                    Screen.PRO -> ProScreen(state, onPurchasePro, onRestorePro)
                }
            }
        }
    }
}

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
}

@Composable private fun titleFor(screen: Screen): String = when (screen) {
    Screen.HOME -> stringResource(R.string.home_title)
    Screen.SESSION -> stringResource(R.string.whole_room_check)
    Screen.HISTORY -> stringResource(R.string.history)
    Screen.PEOPLE -> stringResource(R.string.people)
    Screen.SETTINGS -> stringResource(R.string.settings)
    Screen.CHECKLIST -> stringResource(R.string.playground_checklist)
    Screen.PRO -> stringResource(R.string.pro_screen_title)
}

@Composable private fun SetupScreen(vm: SleepViewModel) {
    var facility by rememberSaveable { mutableStateOf("") }; var room by rememberSaveable { mutableStateOf("") }
    var staff by rememberSaveable { mutableStateOf("") }; var child by rememberSaveable { mutableStateOf("") }
    var profile by rememberSaveable { mutableStateOf(JurisdictionProfile.ONTARIO) }
    var interval by rememberSaveable { mutableStateOf("") }
    FormColumn(stringResource(R.string.setup_title), stringResource(R.string.setup_subtitle)) {
        Field(R.string.facility_name_label, facility) { facility = it }; Field(R.string.room_name_label, room) { room = it }
        Field(R.string.staff_name_label, staff) { staff = it }; Field(R.string.child_name_label, child) { child = it }
        Text(stringResource(R.string.jurisdiction_profile), style = MaterialTheme.typography.titleMedium)
        ProfileChips(profile) { selected -> profile = selected; interval = vm.defaultInterval(selected)?.toString().orEmpty() }
        Text(profileNote(profile))
        Field(R.string.interval_minutes, interval) { interval = it.filter(Char::isDigit) }
        if (interval.toIntOrNull()?.let { it > 0 } != true) Text(stringResource(R.string.policy_interval_required), color = MaterialTheme.colorScheme.error)
        val canSave = facility.isNotBlank() && room.isNotBlank() && staff.isNotBlank() && interval.toIntOrNull()?.let { it > 0 } == true
        Button(onClick = { if (canSave) vm.saveSetup(facility, room, staff, child, profile, interval.toInt()) }, enabled = canSave, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_setup)) }
        Text(stringResource(R.string.app_privacy), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun HomeScreen(state: SleepUiState, vm: SleepViewModel, onSession: (String) -> Unit, onNavigate: (Screen) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(state.facility?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall); if ((state.facility?.intervalMinutes ?: 0) > 0) Text(stringResource(R.string.interval_summary, state.facility?.intervalMinutes ?: 0)) else Text(stringResource(R.string.interval_not_set), color = MaterialTheme.colorScheme.error) }
        item { Button(onClick = { state.rooms.firstOrNull()?.let { vm.startSession(it.id, onSession) } }, enabled = state.rooms.isNotEmpty() && state.children.isNotEmpty() && (state.facility?.intervalMinutes ?: 0) > 0, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_session)) } }
        if (state.children.isEmpty()) item { Text(stringResource(R.string.no_children)) }
        item { Text(stringResource(R.string.active_sessions), style = MaterialTheme.typography.titleMedium) }
        if (state.activeSessions.isEmpty()) item { Text(stringResource(R.string.no_active_sessions)) }
        items(state.activeSessions) { session ->
            val room = state.rooms.firstOrNull { it.id == session.roomId }?.name.orEmpty()
            OutlinedButton(onClick = { onSession(session.id) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.room_label, room)) }
        }
        item { HorizontalDivider() }
        item { NavButton(R.string.open_history) { onNavigate(Screen.HISTORY) } }
        item { NavButton(R.string.open_checklist) { onNavigate(Screen.CHECKLIST) } }
        item { NavButton(R.string.open_people) { onNavigate(Screen.PEOPLE) } }
        item { NavButton(R.string.open_settings) { onNavigate(Screen.SETTINGS) } }
    }
}

@Composable private fun SessionScreen(state: SleepUiState, vm: SleepViewModel, id: String, openedFromReminder: Boolean, formResetKey: Long, onHistory: () -> Unit) {
    var confirmed by rememberSaveable(id, formResetKey) { mutableStateOf(false) }; var exception by rememberSaveable(id, formResetKey) { mutableStateOf(false) }; var notes by rememberSaveable(id, formResetKey) { mutableStateOf("") }
    val session = state.activeSessions.firstOrNull { it.id == id }
    val completing = id in state.completingSessionIds
    val confirmationLabel = stringResource(R.string.direct_visual_confirmation)
    val exceptionLabel = stringResource(R.string.exception_observation)
    val sleepingChildren = session?.let { current -> state.children.count { it.roomId == current.roomId } } ?: 0
    val roomName = session?.let { current -> state.rooms.firstOrNull { it.id == current.roomId }?.name }.orEmpty()
    val nextScheduledAt = session?.let { current ->
        ReminderScheduling.nextReminderAt(current.startedAt, current.intervalMinutes, state.history.count { it.sessionId == current.id })
    }
    val isDue = openedFromReminder || (nextScheduledAt?.let { it <= System.currentTimeMillis() } == true)
    FormColumn(stringResource(R.string.whole_room_check), session?.let { stringResource(R.string.interval_summary, it.intervalMinutes) }.orEmpty()) {
        if (roomName.isNotBlank()) Text(stringResource(R.string.room_label, roomName), style = MaterialTheme.typography.titleMedium)
        nextScheduledAt?.let { Text(stringResource(R.string.next_check_due_at, time(it)), style = MaterialTheme.typography.titleMedium) }
        if (isDue) Text(stringResource(R.string.check_due_now), color = MaterialTheme.colorScheme.error)
        Text(stringResource(R.string.whole_room_prompt))
        Text(stringResource(R.string.children_count, sleepingChildren), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it }, modifier = Modifier.semantics { contentDescription = confirmationLabel })
            Text(stringResource(R.string.direct_visual_confirmation), Modifier.weight(1f).padding(top = 12.dp))
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = exception, onCheckedChange = { exception = it }, modifier = Modifier.semantics { contentDescription = exceptionLabel })
            Text(exceptionLabel, Modifier.weight(1f).padding(top = 12.dp))
        }
        if (!exception) Text(stringResource(R.string.normal_observation))
        Field(R.string.notes_label, notes, singleLine = false) { notes = it }
        Button(onClick = { vm.completeCheck(id, exception, notes, confirmed) }, enabled = confirmed && !completing && session != null, modifier = Modifier.fillMaxWidth()) {
            if (completing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.check_saving))
            } else {
                Text(stringResource(R.string.complete_check))
            }
        }
        if (!confirmed) Text(stringResource(R.string.confirmation_required), color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_history)) }
        if (session != null) {
            HorizontalDivider()
            OutlinedButton(onClick = { vm.closeSession(id) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close_session)) }
        }
    }
}

@Composable private fun HistoryScreen(state: SleepUiState, vm: SleepViewModel, onPdf: (String) -> Unit, onRequestPro: () -> Unit) {
    var correctionFor by remember { mutableStateOf<CheckRecordEntity?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var exceptionsOnly by rememberSaveable { mutableStateOf(false) }
    val defaultPdfFilename = stringResource(R.string.pdf_default_filename)
    val advancedAvailable = ProAccessPolicy.canAccess(ProFeature.ADVANCED_HISTORY, state.proEntitlement, state.rooms.size)
    val visibleHistory = if (!advancedAvailable) state.history else state.history.filter { record ->
        (!exceptionsOnly || record.observationType == ObservationType.EXCEPTION.name) &&
            (search.isBlank() || record.notes.contains(search, ignoreCase = true) || state.rooms.firstOrNull { it.id == record.roomId }?.name?.contains(search, ignoreCase = true) == true)
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Button(onClick = { onPdf(defaultPdfFilename) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.export_pdf)) } }
        item {
            if (advancedAvailable) {
                OutlinedTextField(search, { search = it }, label = { Text(stringResource(R.string.pro_history_search)) }, modifier = Modifier.fillMaxWidth())
                FilterChip(selected = exceptionsOnly, onClick = { exceptionsOnly = !exceptionsOnly }, label = { Text(stringResource(R.string.pro_history_exceptions_only)) })
            } else {
                OutlinedButton(onClick = onRequestPro, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.pro_history_tools)) }
            }
        }
        if (visibleHistory.isEmpty()) item { Text(stringResource(R.string.no_history)) }
        items(visibleHistory) { record ->
            val roomName = state.rooms.firstOrNull { it.id == record.roomId }?.name.orEmpty()
            val staffName = state.staff.firstOrNull { it.id == record.staffId }?.name.orEmpty()
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (record.observationType == ObservationType.EXCEPTION.name) stringResource(R.string.exception_observation) else stringResource(R.string.normal_observation), style = MaterialTheme.typography.titleMedium)
                if (roomName.isNotBlank()) Text(stringResource(R.string.room_label, roomName))
                if (staffName.isNotBlank()) Text(stringResource(R.string.observed_by, staffName))
                Text(if (record.isLate) stringResource(R.string.late_check) else stringResource(R.string.on_time_check))
                Text(stringResource(R.string.recorded_times, time(record.scheduledAt), time(record.observedAt), time(record.recordedAt)), style = MaterialTheme.typography.bodySmall)
                if (record.notes.isNotBlank()) Text(record.notes)
                TextButton(onClick = { correctionFor = record }) { Text(stringResource(R.string.add_correction)) }
            } }
        }
        item { Text(stringResource(R.string.correction_history), style = MaterialTheme.typography.titleMedium) }
        items(state.corrections) { Text(stringResource(R.string.correction_entry, it.correctionReason, time(it.createdAt))) }
    }
    correctionFor?.let { record -> CorrectionDialog(record, onDismiss = { correctionFor = null }) { reason, notes -> vm.addCorrection(record, reason, notes); correctionFor = null } }
}

@Composable private fun CorrectionDialog(record: CheckRecordEntity, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var reason by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.correction)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Field(R.string.correction_reason, reason) { reason = it }; Field(R.string.notes_label, notes) { notes = it } } }, confirmButton = { TextButton(onClick = { onSave(reason, notes) }, enabled = reason.isNotBlank()) { Text(stringResource(R.string.save_correction)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable private fun PeopleScreen(state: SleepUiState, vm: SleepViewModel, onRequestPro: () -> Unit) {
    var room by rememberSaveable { mutableStateOf("") }; var staff by rememberSaveable { mutableStateOf("") }; var child by rememberSaveable { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Field(R.string.add_room, room) { room = it }; Button(onClick = { if (room.isNotBlank()) { if (ProAccessPolicy.canAccess(ProFeature.SECOND_ROOM, state.proEntitlement, state.rooms.size)) { vm.addRoom(room); room = "" } else onRequestPro() } }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_room)) } }
        items(state.rooms) { Text(it.name) }
        item { Field(R.string.add_staff, staff) { staff = it }; Button(onClick = { if (staff.isNotBlank()) { vm.addStaff(staff); staff = "" } }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_staff)) } }
        items(state.staff) { Text(it.name) }
        item { Field(R.string.add_child, child) { child = it }; Button(onClick = { state.rooms.firstOrNull()?.let { roomEntity -> if (child.isNotBlank()) { vm.addChild(roomEntity.id, child); child = "" } } }, enabled = state.rooms.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_child)) } }
        items(state.children) { Text(it.name) }
    }
}

@Composable private fun SettingsScreen(state: SleepUiState, vm: SleepViewModel, backup: (String) -> Unit, restore: (String) -> Unit, onReminderToggle: (Boolean) -> Unit, onRequestPreciseReminders: () -> Unit, onRequestPro: () -> Unit) {
    val current = state.facility ?: return
    var profile by rememberSaveable(current.jurisdiction) { mutableStateOf(runCatching { JurisdictionProfile.valueOf(current.jurisdiction) }.getOrDefault(JurisdictionProfile.CUSTOM)) }
    var interval by rememberSaveable(current.intervalMinutes) { mutableStateOf(current.intervalMinutes.takeIf { it > 0 }?.toString().orEmpty()) }
    FormColumn(stringResource(R.string.jurisdiction_profile), stringResource(R.string.profile_note)) {
        ProfileChips(profile) { selected -> profile = selected; interval = vm.defaultInterval(selected)?.toString().orEmpty() }
        Text(profileNote(profile))
        Field(R.string.interval_minutes, interval) { interval = it.filter(Char::isDigit) }
        if (interval.toIntOrNull()?.let { it > 0 } != true) Text(stringResource(R.string.policy_interval_required), color = MaterialTheme.colorScheme.error)
        Button(onClick = { vm.saveSettings(profile, interval.toIntOrNull()) }, enabled = interval.toIntOrNull()?.let { it > 0 } == true, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_settings)) }
        HorizontalDivider()
        Text(stringResource(R.string.reminders), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = state.remindersEnabled, onCheckedChange = onReminderToggle)
            Text(stringResource(R.string.enable_reminders), Modifier.weight(1f).padding(start = 12.dp))
        }
        Text(if (state.preciseRemindersAvailable) stringResource(R.string.precise_reminders_available) else stringResource(R.string.precise_reminders_unavailable))
        if (!state.preciseRemindersAvailable) OutlinedButton(onClick = onRequestPreciseReminders, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_precise_settings)) }
        if (state.remindersEnabled && !state.notificationsAllowed) Text(stringResource(R.string.notifications_unavailable), color = MaterialTheme.colorScheme.error)
        HorizontalDivider()
        Button(onClick = onRequestPro, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_daycare_pro)) }
        HorizontalDivider()
        Text(stringResource(R.string.backup_restore), style = MaterialTheme.typography.titleMedium)
        val backupFilename = stringResource(R.string.backup_default_filename)
        Button(onClick = { if (state.proEntitlement == ProEntitlement.PRO) backup(backupFilename) else onRequestPro() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.backup_data)) }
        OutlinedButton(onClick = { if (state.proEntitlement == ProEntitlement.PRO) restore("application/json") else onRequestPro() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.restore_data)) }
    }
}

@Composable private fun ChecklistScreen(state: SleepUiState, vm: SleepViewModel) {
    val saved = state.checklist
    var surface by rememberSaveable(saved?.id) { mutableStateOf(saved?.surfaceSafe ?: false) }
    var equipment by rememberSaveable(saved?.id) { mutableStateOf(saved?.equipmentSafe ?: false) }
    var gate by rememberSaveable(saved?.id) { mutableStateOf(saved?.gateSafe ?: false) }
    FormColumn(stringResource(R.string.playground_checklist), null) {
        if (saved != null) Text(stringResource(R.string.checklist_status_saved), style = MaterialTheme.typography.bodyMedium)
        CheckRow(R.string.checklist_item_surface, surface) { surface = it }; CheckRow(R.string.checklist_item_equipment, equipment) { equipment = it }; CheckRow(R.string.checklist_item_gate, gate) { gate = it }
        Button(onClick = { vm.saveChecklist(surface, equipment, gate) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_checklist)) }
    }
}

@Composable
private fun ProScreen(state: SleepUiState, onPurchase: () -> Unit, onRestore: () -> Unit) {
    val price = state.proPrice ?: stringResource(R.string.pro_price_unavailable)
    val billingMessage = state.billingMessage
    FormColumn(stringResource(R.string.pro_screen_title), stringResource(R.string.pro_screen_subtitle)) {
        ProBenefit(R.string.pro_benefit_unlimited_rooms, R.string.pro_benefit_unlimited_rooms_description)
        ProBenefit(R.string.pro_benefit_pdf, R.string.pro_benefit_pdf_description)
        ProBenefit(R.string.pro_benefit_history, R.string.pro_benefit_history_description)
        ProBenefit(R.string.pro_benefit_backup, R.string.pro_benefit_backup_description)
        ProBenefit(R.string.pro_benefit_playground, R.string.pro_benefit_playground_description)
        HorizontalDivider()
        Text(stringResource(R.string.pro_price_once, price), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.pro_lifetime_access))
        Button(onClick = onPurchase, enabled = state.proEntitlement != ProEntitlement.PRO && state.proPrice != null, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pro_unlock_cta, price))
        }
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.pro_restore_purchase)) }
        Text(stringResource(R.string.pro_footer), style = MaterialTheme.typography.bodySmall)
        billingMessage?.let { message ->
            Text(
                when (message) {
                    BillingMessage.PURCHASE_PENDING -> stringResource(R.string.pro_purchase_pending)
                    BillingMessage.PURCHASE_CANCELED -> stringResource(R.string.pro_purchase_canceled)
                    BillingMessage.BILLING_UNAVAILABLE -> stringResource(R.string.pro_billing_unavailable)
                    BillingMessage.PURCHASE_FAILED -> stringResource(R.string.pro_purchase_failed)
                    BillingMessage.PURCHASE_RESTORED -> stringResource(R.string.pro_purchase_restored)
                },
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable private fun ProBenefit(title: Int, description: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(description))
    }
}

@Composable private fun FormColumn(title: String, subtitle: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = { Text(title, style = MaterialTheme.typography.headlineSmall); subtitle?.takeIf { it.isNotBlank() }?.let { Text(it) }; content() },
    )
}
@Composable private fun Field(label: Int, value: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) { OutlinedTextField(value, onValueChange, label = { Text(stringResource(label)) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth()) }
@Composable private fun CheckRow(label: Int, checked: Boolean, onChecked: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.Top) { Checkbox(checked, onChecked); Text(stringResource(label), Modifier.weight(1f).padding(top = 12.dp)) } }
@Composable private fun NavButton(label: Int, onClick: () -> Unit) { OutlinedButton(onClick, Modifier.fillMaxWidth()) { Text(stringResource(label)) } }
@Composable private fun ProfileChips(selected: JurisdictionProfile, onSelected: (JurisdictionProfile) -> Unit) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { JurisdictionProfile.values().forEach { item -> FilterChip(selected = selected == item, onClick = { onSelected(item) }, label = { Text(profileLabel(item)) }) } } }
@Composable private fun profileNote(profile: JurisdictionProfile): String = when (profile) { JurisdictionProfile.ONTARIO -> stringResource(R.string.ontario_policy_note); JurisdictionProfile.CALIFORNIA -> stringResource(R.string.california_policy_note); JurisdictionProfile.CUSTOM -> stringResource(R.string.custom_policy_note) }
@Composable private fun profileLabel(profile: JurisdictionProfile): String = when (profile) { JurisdictionProfile.ONTARIO -> stringResource(R.string.ontario); JurisdictionProfile.CALIFORNIA -> stringResource(R.string.california); JurisdictionProfile.CUSTOM -> stringResource(R.string.custom) }
private fun time(value: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(value))
