package com.daycare.sleepcheck.log.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.daycare.sleepcheck.log.R
import com.daycare.sleepcheck.log.SleepUiState
import com.daycare.sleepcheck.log.SleepViewModel
import com.daycare.sleepcheck.log.UiMessage
import com.daycare.sleepcheck.log.data.*
import java.text.DateFormat
import java.util.Date

private enum class Screen { HOME, SESSION, HISTORY, PEOPLE, SETTINGS, CHECKLIST }

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
) {
    var screen by rememberSaveable { mutableStateOf(if (state.facility == null) Screen.SETTINGS else Screen.HOME) }
    var sessionId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.requestedSessionId, state.facility?.id) {
        state.requestedSessionId?.takeIf { state.facility != null }?.let {
            sessionId = it
            screen = Screen.SESSION
            vm.clearRequestedSession()
        }
    }
    if (state.facility == null) SetupScreen(vm)
    else {
        LaunchedEffect(state.facility.id) { if (screen == Screen.SETTINGS) screen = Screen.HOME }
        Scaffold(topBar = { TopAppBar(title = { Text(titleFor(screen)) }, navigationIcon = { if (screen != Screen.HOME) TextButton(onClick = { screen = Screen.HOME }) { Text(stringResource(R.string.back)) } }) }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (screen) {
                    Screen.HOME -> HomeScreen(state, vm, onSession = { id -> sessionId = id; screen = Screen.SESSION }, onNavigate = { screen = it })
                    Screen.SESSION -> SessionScreen(state, vm, sessionId ?: "", onHistory = { screen = Screen.HISTORY })
                    Screen.HISTORY -> HistoryScreen(state, vm, onPdf = pdf)
                    Screen.PEOPLE -> PeopleScreen(state, vm)
                    Screen.SETTINGS -> SettingsScreen(state, vm, backup, restore, onReminderToggle, onRequestPreciseReminders)
                    Screen.CHECKLIST -> ChecklistScreen(state, vm)
                }
            }
        }
    }
    state.message?.let { message -> LaunchedEffect(message) { vm.clearMessage() } }
}

@Composable private fun titleFor(screen: Screen): String = when (screen) {
    Screen.HOME -> stringResource(R.string.home_title)
    Screen.SESSION -> stringResource(R.string.whole_room_check)
    Screen.HISTORY -> stringResource(R.string.history)
    Screen.PEOPLE -> stringResource(R.string.people)
    Screen.SETTINGS -> stringResource(R.string.settings)
    Screen.CHECKLIST -> stringResource(R.string.playground_checklist)
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

@Composable private fun SessionScreen(state: SleepUiState, vm: SleepViewModel, id: String, onHistory: () -> Unit) {
    var confirmed by rememberSaveable(id) { mutableStateOf(false) }; var exception by rememberSaveable(id) { mutableStateOf(false) }; var notes by rememberSaveable(id) { mutableStateOf("") }
    val session = state.activeSessions.firstOrNull { it.id == id }
    val confirmationLabel = stringResource(R.string.direct_visual_confirmation)
    val exceptionLabel = stringResource(R.string.exception_observation)
    val sleepingChildren = session?.let { current -> state.children.count { it.roomId == current.roomId } } ?: 0
    FormColumn(stringResource(R.string.due_check), session?.let { stringResource(R.string.interval_summary, it.intervalMinutes) }.orEmpty()) {
        Text(stringResource(R.string.whole_room_prompt))
        Text(stringResource(R.string.children_count, sleepingChildren), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it }, modifier = Modifier.semantics { contentDescription = confirmationLabel })
            Text(stringResource(R.string.direct_visual_confirmation))
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = exception, onCheckedChange = { exception = it }, modifier = Modifier.semantics { contentDescription = exceptionLabel }); Text(exceptionLabel) }
        if (!exception) Text(stringResource(R.string.normal_observation))
        Field(R.string.notes_label, notes, singleLine = false) { notes = it }
        Button(onClick = { vm.completeCheck(id, exception, notes, confirmed) }, enabled = confirmed, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.complete_check)) }
        if (!confirmed) Text(stringResource(R.string.confirmation_required), color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_history)) }
        if (session != null) OutlinedButton(onClick = { vm.closeSession(id) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close_session)) }
    }
}

@Composable private fun HistoryScreen(state: SleepUiState, vm: SleepViewModel, onPdf: (String) -> Unit) {
    var correctionFor by remember { mutableStateOf<CheckRecordEntity?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Button(onClick = { onPdf("sleep-check-history.pdf") }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.export_pdf)) } }
        if (state.history.isEmpty()) item { Text(stringResource(R.string.no_history)) }
        items(state.history) { record ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (record.observationType == ObservationType.EXCEPTION.name) stringResource(R.string.exception_observation) else stringResource(R.string.normal_observation), style = MaterialTheme.typography.titleMedium)
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

@Composable private fun PeopleScreen(state: SleepUiState, vm: SleepViewModel) {
    var room by rememberSaveable { mutableStateOf("") }; var staff by rememberSaveable { mutableStateOf("") }; var child by rememberSaveable { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Field(R.string.add_room, room) { room = it }; Button(onClick = { if (room.isNotBlank()) { vm.addRoom(room); room = "" } }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_room)) } }
        items(state.rooms) { Text(it.name) }
        item { Field(R.string.add_staff, staff) { staff = it }; Button(onClick = { if (staff.isNotBlank()) { vm.addStaff(staff); staff = "" } }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_staff)) } }
        items(state.staff) { Text(it.name) }
        item { Field(R.string.add_child, child) { child = it }; Button(onClick = { state.rooms.firstOrNull()?.let { roomEntity -> if (child.isNotBlank()) { vm.addChild(roomEntity.id, child); child = "" } } }, enabled = state.rooms.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_child)) } }
        items(state.children) { Text(it.name) }
    }
}

@Composable private fun SettingsScreen(state: SleepUiState, vm: SleepViewModel, backup: (String) -> Unit, restore: (String) -> Unit, onReminderToggle: (Boolean) -> Unit, onRequestPreciseReminders: () -> Unit) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = state.remindersEnabled, onCheckedChange = onReminderToggle)
            Text(stringResource(R.string.enable_reminders))
        }
        Text(if (state.preciseRemindersAvailable) stringResource(R.string.precise_reminders_available) else stringResource(R.string.precise_reminders_unavailable))
        if (!state.preciseRemindersAvailable) OutlinedButton(onClick = onRequestPreciseReminders, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_precise_settings)) }
        if (state.remindersEnabled && !state.notificationsAllowed) Text(stringResource(R.string.notifications_unavailable), color = MaterialTheme.colorScheme.error)
        HorizontalDivider()
        Text(stringResource(R.string.backup_restore), style = MaterialTheme.typography.titleMedium)
        Button(onClick = { backup("daycare-sleep-check-log.json") }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.backup_data)) }
        OutlinedButton(onClick = { restore("application/json") }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.restore_data)) }
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

@Composable private fun FormColumn(title: String, subtitle: String?, content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxSize().padding(20.dp).safeDrawingPadding(), verticalArrangement = Arrangement.spacedBy(12.dp), content = { Text(title, style = MaterialTheme.typography.headlineSmall); subtitle?.takeIf { it.isNotBlank() }?.let { Text(it) }; content() }) }
@Composable private fun Field(label: Int, value: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) { OutlinedTextField(value, onValueChange, label = { Text(stringResource(label)) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth()) }
@Composable private fun CheckRow(label: Int, checked: Boolean, onChecked: (Boolean) -> Unit) { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, onChecked); Text(stringResource(label)) } }
@Composable private fun NavButton(label: Int, onClick: () -> Unit) { OutlinedButton(onClick, Modifier.fillMaxWidth()) { Text(stringResource(label)) } }
@Composable private fun ProfileChips(selected: JurisdictionProfile, onSelected: (JurisdictionProfile) -> Unit) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { JurisdictionProfile.values().forEach { item -> FilterChip(selected = selected == item, onClick = { onSelected(item) }, label = { Text(profileLabel(item)) }) } } }
@Composable private fun profileNote(profile: JurisdictionProfile): String = when (profile) { JurisdictionProfile.ONTARIO -> stringResource(R.string.ontario_policy_note); JurisdictionProfile.CALIFORNIA -> stringResource(R.string.california_policy_note); JurisdictionProfile.CUSTOM -> stringResource(R.string.custom_policy_note) }
@Composable private fun profileLabel(profile: JurisdictionProfile): String = when (profile) { JurisdictionProfile.ONTARIO -> stringResource(R.string.ontario); JurisdictionProfile.CALIFORNIA -> stringResource(R.string.california); JurisdictionProfile.CUSTOM -> stringResource(R.string.custom) }
private fun time(value: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(value))
