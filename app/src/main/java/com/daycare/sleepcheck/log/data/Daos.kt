package com.daycare.sleepcheck.log.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FacilityDao {
    @Query("SELECT * FROM facilities LIMIT 1") fun observe(): Flow<FacilityEntity?>
    @Query("SELECT * FROM facilities LIMIT 1") suspend fun get(): FacilityEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(item: FacilityEntity)
    @Query("SELECT * FROM facilities") suspend fun all(): List<FacilityEntity>
}

@Dao
interface PeopleDao {
    @Query("SELECT * FROM rooms ORDER BY name") fun rooms(): Flow<List<RoomEntity>>
    @Query("SELECT * FROM staff ORDER BY name") fun staff(): Flow<List<StaffEntity>>
    @Query("SELECT * FROM children ORDER BY name") fun children(): Flow<List<ChildEntity>>
    @Query("SELECT * FROM children WHERE roomId = :roomId ORDER BY name") suspend fun childrenForRoom(roomId: String): List<ChildEntity>
    @Query("SELECT * FROM rooms WHERE id = :roomId LIMIT 1") suspend fun room(roomId: String): RoomEntity?
    @Insert suspend fun insertRoom(item: RoomEntity)
    @Insert suspend fun insertStaff(item: StaffEntity)
    @Insert suspend fun insertChild(item: ChildEntity)
    @Query("SELECT * FROM rooms") suspend fun allRooms(): List<RoomEntity>
    @Query("SELECT * FROM staff") suspend fun allStaff(): List<StaffEntity>
    @Query("SELECT * FROM children") suspend fun allChildren(): List<ChildEntity>
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_sessions WHERE active = 1 ORDER BY startedAt DESC") fun activeSessions(): Flow<List<SleepSessionEntity>>
    @Query("SELECT * FROM sleep_sessions WHERE active = 1 ORDER BY startedAt DESC") suspend fun activeSessionsOnce(): List<SleepSessionEntity>
    @Query("SELECT * FROM sleep_sessions WHERE id = :id LIMIT 1") suspend fun session(id: String): SleepSessionEntity?
    @Query("SELECT * FROM check_records ORDER BY recordedAt DESC") fun history(): Flow<List<CheckRecordEntity>>
    @Query("SELECT * FROM check_records WHERE sessionId = :sessionId ORDER BY scheduledAt") suspend fun recordsForSession(sessionId: String): List<CheckRecordEntity>
    @Insert suspend fun insertSession(item: SleepSessionEntity)
    @Insert suspend fun insertRecord(item: CheckRecordEntity)
    @Query("UPDATE sleep_sessions SET active = 0 WHERE id = :id") suspend fun closeSession(id: String)
    @Query("UPDATE sleep_sessions SET intervalMinutes = :intervalMinutes WHERE active = 1") suspend fun updateActiveIntervals(intervalMinutes: Int)
    @Query("SELECT * FROM sleep_sessions") suspend fun allSessions(): List<SleepSessionEntity>
    @Query("SELECT * FROM check_records") suspend fun allRecords(): List<CheckRecordEntity>
    @Insert suspend fun insertSessionForRestore(item: SleepSessionEntity)
    @Insert suspend fun insertRecordForRestore(item: CheckRecordEntity)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM correction_audit ORDER BY createdAt DESC") fun corrections(): Flow<List<CorrectionAuditEntity>>
    @Insert suspend fun insert(item: CorrectionAuditEntity)
    @Query("SELECT * FROM correction_audit") suspend fun all(): List<CorrectionAuditEntity>
    @Insert suspend fun insertForRestore(item: CorrectionAuditEntity)
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM playground_checklist WHERE checkDate = :date ORDER BY recordedAt DESC LIMIT 1") fun observe(date: String): Flow<PlaygroundChecklistEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(item: PlaygroundChecklistEntity)
    @Query("SELECT * FROM playground_checklist") suspend fun all(): List<PlaygroundChecklistEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertForRestore(item: PlaygroundChecklistEntity)
}
