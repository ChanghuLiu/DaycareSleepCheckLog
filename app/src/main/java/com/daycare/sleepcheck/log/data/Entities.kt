package com.daycare.sleepcheck.log.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class JurisdictionProfile { ONTARIO, CALIFORNIA, CUSTOM }
enum class ObservationType { NORMAL, EXCEPTION }

const val UNSET_INTERVAL_MINUTES = 0

@Entity(tableName = "facilities")
data class FacilityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val jurisdiction: String,
    val intervalMinutes: Int,
    val createdAt: Long,
)

@Entity(tableName = "rooms", indices = [Index("facilityId")])
data class RoomEntity(@PrimaryKey val id: String, val facilityId: String, val name: String)

@Entity(tableName = "staff", indices = [Index("facilityId")])
data class StaffEntity(@PrimaryKey val id: String, val facilityId: String, val name: String)

@Entity(tableName = "children", indices = [Index("roomId")])
data class ChildEntity(@PrimaryKey val id: String, val roomId: String, val name: String)

@Entity(tableName = "sleep_sessions", indices = [Index("roomId"), Index("active")])
data class SleepSessionEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val startedAt: Long,
    val intervalMinutes: Int,
    val active: Boolean,
)

@Entity(tableName = "check_records", indices = [Index("sessionId"), Index("recordedAt")])
data class CheckRecordEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val roomId: String,
    val staffId: String,
    val scheduledAt: Long,
    val observedAt: Long,
    val recordedAt: Long,
    val observationType: String,
    val notes: String,
    val directVisualCheckConfirmed: Boolean,
    val isLate: Boolean,
)

@Entity(tableName = "correction_audit", indices = [Index("originalRecordId"), Index("createdAt")])
data class CorrectionAuditEntity(
    @PrimaryKey val id: String,
    val originalRecordId: String,
    val correctionReason: String,
    val correctedScheduledAt: Long?,
    val correctedObservedAt: Long?,
    val correctedObservationType: String?,
    val correctedNotes: String?,
    val createdAt: Long,
    val staffId: String,
)

@Entity(tableName = "playground_checklist", indices = [Index("checkDate")])
data class PlaygroundChecklistEntity(
    @PrimaryKey val id: String,
    val checkDate: String,
    val surfaceSafe: Boolean,
    val equipmentSafe: Boolean,
    val gateSafe: Boolean,
    val recordedAt: Long,
    val staffId: String,
)
