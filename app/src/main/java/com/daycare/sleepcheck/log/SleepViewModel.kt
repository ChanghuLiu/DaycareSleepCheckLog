package com.daycare.sleepcheck.log

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daycare.sleepcheck.log.data.*
import com.daycare.sleepcheck.log.billing.BillingMessage
import com.daycare.sleepcheck.log.billing.ProEntitlement
import com.daycare.sleepcheck.log.billing.ProEntitlementRepository
import com.daycare.sleepcheck.log.domain.JurisdictionDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SleepUiState(
    val facility: FacilityEntity? = null,
    val rooms: List<RoomEntity> = emptyList(),
    val staff: List<StaffEntity> = emptyList(),
    val children: List<ChildEntity> = emptyList(),
    val activeSessions: List<SleepSessionEntity> = emptyList(),
    val history: List<CheckRecordEntity> = emptyList(),
    val corrections: List<CorrectionAuditEntity> = emptyList(),
    val checklist: PlaygroundChecklistEntity? = null,
    val requestedSessionId: String? = null,
    val remindersEnabled: Boolean = false,
    val preciseRemindersAvailable: Boolean = false,
    val notificationsAllowed: Boolean = true,
    val message: UiMessage? = null,
    val proEntitlement: ProEntitlement = ProEntitlement.CHECKING,
    val proPrice: String? = null,
    val billingMessage: BillingMessage? = null,
)

enum class UiMessage { SETUP_SAVED, CHECK_SAVED, CORRECTION_SAVED, CHECKLIST_SAVED, BACKUP_CREATED, RESTORED, INVALID_BACKUP }

class SleepViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.create(app)
    private val repo = SleepRepository(db)
    private val reminderScheduler = ReminderScheduler(app)
    private val proEntitlementRepository = ProEntitlementRepository(app)
    private val _uiState = MutableStateFlow(SleepUiState())
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init {
        refreshReminderStatus()
        viewModelScope.launch { repo.facility.collect { update { copy(facility = it) } } }
        viewModelScope.launch { repo.rooms.collect { update { copy(rooms = it) } } }
        viewModelScope.launch { repo.staff.collect { update { copy(staff = it) } } }
        viewModelScope.launch { repo.children.collect { update { copy(children = it) } } }
        viewModelScope.launch { repo.activeSessions.collect { update { copy(activeSessions = it) } } }
        viewModelScope.launch { repo.history.collect { update { copy(history = it) } } }
        viewModelScope.launch { repo.corrections.collect { update { copy(corrections = it) } } }
        viewModelScope.launch { repo.checklist.collect { update { copy(checklist = it) } } }
        viewModelScope.launch { proEntitlementRepository.entitlement.collect { value -> update { copy(proEntitlement = value) } } }
        viewModelScope.launch { proEntitlementRepository.localizedPrice.collect { value -> update { copy(proPrice = value) } } }
        viewModelScope.launch { proEntitlementRepository.message.collect { value -> update { copy(billingMessage = value) } } }
        proEntitlementRepository.connect()
    }

    private fun update(change: SleepUiState.() -> SleepUiState) { _uiState.value = _uiState.value.change() }
    fun clearMessage() = update { copy(message = null) }
    fun saveSetup(facility: String, room: String, staff: String, child: String, profile: JurisdictionProfile, interval: Int) = launch { repo.saveSetup(facility, room, staff, child, profile, interval); update { copy(message = UiMessage.SETUP_SAVED) } }
    fun saveSettings(profile: JurisdictionProfile, interval: Int?) = launch { repo.saveFacilitySettings(profile, interval); reminderScheduler.rescheduleAll(db); refreshReminderStatus() }
    fun addRoom(name: String) = launch { repo.addRoom(name) }
    fun addStaff(name: String) = launch { repo.addStaff(name) }
    fun addChild(roomId: String, name: String) = launch { repo.addChild(roomId, name) }
    fun startSession(roomId: String, onStarted: (String) -> Unit) = launch { val id = repo.startSession(roomId); reminderScheduler.rescheduleSession(db, id); onStarted(id) }
    fun completeCheck(sessionId: String, exception: Boolean, notes: String, confirmed: Boolean) = launch {
        val staff = _uiState.value.staff.firstOrNull()?.id ?: return@launch
        try { repo.completeCheck(sessionId, staff, exception, notes, confirmed); reminderScheduler.rescheduleSession(db, sessionId); update { copy(message = UiMessage.CHECK_SAVED) } } catch (_: IllegalArgumentException) { }
    }
    fun addCorrection(record: CheckRecordEntity, reason: String, notes: String) = launch {
        val staff = _uiState.value.staff.firstOrNull()?.id ?: return@launch
        repo.addCorrection(record, reason, staff, null, notes); update { copy(message = UiMessage.CORRECTION_SAVED) }
    }
    fun closeSession(id: String) = launch { repo.closeSession(id); reminderScheduler.cancel(id) }
    fun saveChecklist(surface: Boolean, equipment: Boolean, gate: Boolean) = launch { _uiState.value.staff.firstOrNull()?.id?.let { repo.saveChecklist(surface, equipment, gate, it); update { copy(message = UiMessage.CHECKLIST_SAVED) } } }
    fun backupTo(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        BackupManager(getApplication<Application>().contentResolver, db).write(uri)
        update { copy(message = UiMessage.BACKUP_CREATED) }
    }
    fun restoreFrom(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            BackupManager(getApplication<Application>().contentResolver, db).restore(uri)
            update { copy(message = UiMessage.RESTORED) }
        } catch (_: Exception) {
            update { copy(message = UiMessage.INVALID_BACKUP) }
        }
    }
    fun exportPdfTo(uri: Uri) = launch {
        val state = _uiState.value
        PdfExporter(getApplication(), getApplication<Application>().contentResolver).write(
            uri,
            state.facility,
            state.rooms,
            state.staff,
            state.history,
        )
    }
    fun defaultInterval(profile: JurisdictionProfile): Int? = JurisdictionDefaults.intervalFor(profile)
    fun openSessionFromReminder(sessionId: String?) { if (!sessionId.isNullOrBlank()) update { copy(requestedSessionId = sessionId) } }
    fun clearRequestedSession() = update { copy(requestedSessionId = null) }
    fun setRemindersEnabled(enabled: Boolean) = launch {
        if (!enabled) _uiState.value.activeSessions.forEach { reminderScheduler.cancel(it.id) }
        reminderScheduler.setEnabled(enabled)
        if (enabled) reminderScheduler.rescheduleAll(db)
        refreshReminderStatus()
    }
    fun refreshReminderStatus() {
        update { copy(remindersEnabled = reminderScheduler.enabled, preciseRemindersAvailable = reminderScheduler.preciseAvailable(), notificationsAllowed = reminderScheduler.notificationsAllowed()) }
    }
    fun refreshProEntitlement() = proEntitlementRepository.refresh()
    fun purchasePro(activity: android.app.Activity) = proEntitlementRepository.launchPurchase(activity)
    fun clearBillingMessage() = proEntitlementRepository.clearMessage()
    fun reconcileReminders() = launch { reminderScheduler.rescheduleAll(db) }
    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }
    override fun onCleared() { proEntitlementRepository.close(); db.close(); super.onCleared() }
}
