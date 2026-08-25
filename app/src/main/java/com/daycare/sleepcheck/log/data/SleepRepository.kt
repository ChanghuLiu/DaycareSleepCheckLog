package com.daycare.sleepcheck.log.data

import com.daycare.sleepcheck.log.domain.CheckCompletion
import com.daycare.sleepcheck.log.domain.CheckScheduling
import com.daycare.sleepcheck.log.domain.JurisdictionDefaults
import java.util.UUID

class SleepRepository(private val db: AppDatabase) {
    val facility = db.facilityDao().observe()
    val rooms = db.peopleDao().rooms()
    val staff = db.peopleDao().staff()
    val children = db.peopleDao().children()
    val activeSessions = db.sleepDao().activeSessions()
    val history = db.sleepDao().history()
    val corrections = db.auditDao().corrections()
    val checklist = db.checklistDao().observe(currentDate())

    suspend fun saveSetup(
        facilityName: String,
        roomName: String,
        staffName: String,
        childName: String,
        jurisdiction: JurisdictionProfile,
        intervalMinutes: Int?,
    ) {
        val selectedInterval = JurisdictionDefaults.validateFacilityPolicyInterval(intervalMinutes)
        val facilityId = UUID.randomUUID().toString()
        val roomId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        db.facilityDao().insert(FacilityEntity(facilityId, facilityName, jurisdiction.name, selectedInterval, now))
        db.peopleDao().insertRoom(RoomEntity(roomId, facilityId, roomName))
        db.peopleDao().insertStaff(StaffEntity(UUID.randomUUID().toString(), facilityId, staffName))
        if (childName.isNotBlank()) db.peopleDao().insertChild(ChildEntity(UUID.randomUUID().toString(), roomId, childName))
    }

    suspend fun saveFacilitySettings(jurisdiction: JurisdictionProfile, intervalMinutes: Int?) {
        val current = db.facilityDao().get() ?: return
        val selectedInterval = JurisdictionDefaults.validateFacilityPolicyInterval(intervalMinutes)
        db.facilityDao().insert(current.copy(jurisdiction = jurisdiction.name, intervalMinutes = selectedInterval))
        db.sleepDao().updateActiveIntervals(selectedInterval)
    }

    suspend fun addRoom(name: String) { val f = db.facilityDao().get() ?: return; db.peopleDao().insertRoom(RoomEntity(UUID.randomUUID().toString(), f.id, name)) }
    suspend fun addStaff(name: String) { val f = db.facilityDao().get() ?: return; db.peopleDao().insertStaff(StaffEntity(UUID.randomUUID().toString(), f.id, name)) }
    suspend fun addChild(roomId: String, name: String) { db.peopleDao().insertChild(ChildEntity(UUID.randomUUID().toString(), roomId, name)) }

    suspend fun startSession(roomId: String): String {
        val f = db.facilityDao().get() ?: error("Facility is not configured")
        require(f.intervalMinutes > UNSET_INTERVAL_MINUTES) { "A facility policy interval is required" }
        val id = UUID.randomUUID().toString()
        db.sleepDao().insertSession(SleepSessionEntity(id, roomId, System.currentTimeMillis(), f.intervalMinutes, true))
        return id
    }

    suspend fun completeCheck(sessionId: String, staffId: String, exception: Boolean, notes: String, directVisualCheckConfirmed: Boolean, observedAt: Long = System.currentTimeMillis()): CheckRecordEntity {
        CheckCompletion.validate(directVisualCheckConfirmed)
        val session = db.sleepDao().session(sessionId) ?: error("Sleep session not found")
        val count = db.sleepDao().recordsForSession(sessionId).size
        val scheduled = CheckScheduling.nextScheduledAt(session.startedAt, session.intervalMinutes, count)
        return CheckRecordEntity(UUID.randomUUID().toString(), sessionId, session.roomId, staffId, scheduled, observedAt, System.currentTimeMillis(), if (exception) ObservationType.EXCEPTION.name else ObservationType.NORMAL.name, notes, true, CheckScheduling.isLate(scheduled, observedAt)).also { db.sleepDao().insertRecord(it) }
    }

    suspend fun addCorrection(record: CheckRecordEntity, reason: String, staffId: String, observedAt: Long?, notes: String?) {
        db.auditDao().insert(CorrectionAuditEntity(UUID.randomUUID().toString(), record.id, reason, null, observedAt, null, notes, System.currentTimeMillis(), staffId))
    }

    suspend fun closeSession(id: String) = db.sleepDao().closeSession(id)
    suspend fun saveChecklist(surface: Boolean, equipment: Boolean, gate: Boolean, staffId: String) {
        val date = currentDate()
        db.checklistDao().save(PlaygroundChecklistEntity(UUID.randomUUID().toString(), date, surface, equipment, gate, System.currentTimeMillis(), staffId))
    }

    private fun currentDate(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
}
