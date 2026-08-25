package com.daycare.sleepcheck.log.data

import android.content.ContentResolver
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object BackupManifestValidator {
    const val PACKAGE = "com.daycare.sleepcheck.log"
    const val FORMAT = "daycare-sleep-check-log"
    const val VERSION = 1

    fun isValid(text: String): Boolean =
        Regex("\\\"format\\\"\\s*:\\s*\\\"$FORMAT\\\"").containsMatchIn(text) &&
            Regex("\\\"formatVersion\\\"\\s*:\\s*$VERSION(?:\\s*[,}])").containsMatchIn(text) &&
            Regex("\\\"package\\\"\\s*:\\s*\\\"$PACKAGE\\\"").containsMatchIn(text) &&
            Regex("\\\"data\\\"\\s*:").containsMatchIn(text)
}

class BackupManager(private val resolver: ContentResolver, private val db: AppDatabase) {
    suspend fun write(uri: Uri) {
        val root = JSONObject().apply {
            put("format", BackupManifestValidator.FORMAT)
            put("formatVersion", BackupManifestValidator.VERSION)
            put("package", BackupManifestValidator.PACKAGE)
            put("createdAt", System.currentTimeMillis())
            put("data", collect())
        }
        resolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(root.toString()) }
            ?: error("Unable to open backup destination")
    }

    suspend fun restore(uri: Uri) {
        val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Unable to read backup")
        require(BackupManifestValidator.isValid(text))
        val data = JSONObject(text).getJSONObject("data")
        db.clearAllTables()
        data.array("facilities").forEach { db.facilityDao().insert(it.facility()) }
        data.array("rooms").forEach { db.peopleDao().insertRoom(it.room()) }
        data.array("staff").forEach { db.peopleDao().insertStaff(it.staff()) }
        data.array("children").forEach { db.peopleDao().insertChild(it.child()) }
        data.array("sessions").forEach { db.sleepDao().insertSessionForRestore(it.session()) }
        data.array("records").forEach { db.sleepDao().insertRecordForRestore(it.record()) }
        data.array("corrections").forEach { db.auditDao().insertForRestore(it.correction()) }
        data.array("checklists").forEach { db.checklistDao().insertForRestore(it.checklist()) }
    }

    private suspend fun collect(): JSONObject = JSONObject().apply {
        put("facilities", JSONArray(db.facilityDao().all().map { it.json() }))
        put("rooms", JSONArray(db.peopleDao().allRooms().map { it.json() }))
        put("staff", JSONArray(db.peopleDao().allStaff().map { it.json() }))
        put("children", JSONArray(db.peopleDao().allChildren().map { it.json() }))
        put("sessions", JSONArray(db.sleepDao().allSessions().map { it.json() }))
        put("records", JSONArray(db.sleepDao().allRecords().map { it.json() }))
        put("corrections", JSONArray(db.auditDao().all().map { it.json() }))
        put("checklists", JSONArray(db.checklistDao().all().map { it.json() }))
    }
}

private fun JSONObject.array(key: String): List<JSONObject> = getJSONArray(key).let { array -> List(array.length()) { array.getJSONObject(it) } }
private fun JSONObject.facility() = FacilityEntity(getString("id"), getString("name"), getString("jurisdiction"), getInt("intervalMinutes"), getLong("createdAt"))
private fun JSONObject.room() = RoomEntity(getString("id"), getString("facilityId"), getString("name"))
private fun JSONObject.staff() = StaffEntity(getString("id"), getString("facilityId"), getString("name"))
private fun JSONObject.child() = ChildEntity(getString("id"), getString("roomId"), getString("name"))
private fun JSONObject.session() = SleepSessionEntity(getString("id"), getString("roomId"), getLong("startedAt"), getInt("intervalMinutes"), getBoolean("active"))
private fun JSONObject.record() = CheckRecordEntity(getString("id"), getString("sessionId"), getString("roomId"), getString("staffId"), getLong("scheduledAt"), getLong("observedAt"), getLong("recordedAt"), getString("observationType"), getString("notes"), getBoolean("directVisualCheckConfirmed"), getBoolean("isLate"))
private fun JSONObject.correction() = CorrectionAuditEntity(getString("id"), getString("originalRecordId"), getString("correctionReason"), optLongOrNull("correctedScheduledAt"), optLongOrNull("correctedObservedAt"), optStringOrNull("correctedObservationType"), optStringOrNull("correctedNotes"), getLong("createdAt"), getString("staffId"))
private fun JSONObject.checklist() = PlaygroundChecklistEntity(getString("id"), getString("checkDate"), getBoolean("surfaceSafe"), getBoolean("equipmentSafe"), getBoolean("gateSafe"), getLong("recordedAt"), getString("staffId"))
private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)

private fun FacilityEntity.json() = JSONObject().put("id", id).put("name", name).put("jurisdiction", jurisdiction).put("intervalMinutes", intervalMinutes).put("createdAt", createdAt)
private fun RoomEntity.json() = JSONObject().put("id", id).put("facilityId", facilityId).put("name", name)
private fun StaffEntity.json() = JSONObject().put("id", id).put("facilityId", facilityId).put("name", name)
private fun ChildEntity.json() = JSONObject().put("id", id).put("roomId", roomId).put("name", name)
private fun SleepSessionEntity.json() = JSONObject().put("id", id).put("roomId", roomId).put("startedAt", startedAt).put("intervalMinutes", intervalMinutes).put("active", active)
private fun CheckRecordEntity.json() = JSONObject().put("id", id).put("sessionId", sessionId).put("roomId", roomId).put("staffId", staffId).put("scheduledAt", scheduledAt).put("observedAt", observedAt).put("recordedAt", recordedAt).put("observationType", observationType).put("notes", notes).put("directVisualCheckConfirmed", directVisualCheckConfirmed).put("isLate", isLate)
private fun CorrectionAuditEntity.json() = JSONObject().put("id", id).put("originalRecordId", originalRecordId).put("correctionReason", correctionReason).put("correctedScheduledAt", correctedScheduledAt).put("correctedObservedAt", correctedObservedAt).put("correctedObservationType", correctedObservationType).put("correctedNotes", correctedNotes).put("createdAt", createdAt).put("staffId", staffId)
private fun PlaygroundChecklistEntity.json() = JSONObject().put("id", id).put("checkDate", checkDate).put("surfaceSafe", surfaceSafe).put("equipmentSafe", equipmentSafe).put("gateSafe", gateSafe).put("recordedAt", recordedAt).put("staffId", staffId)
