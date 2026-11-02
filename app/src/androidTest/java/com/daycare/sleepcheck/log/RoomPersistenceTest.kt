package com.daycare.sleepcheck.log

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.daycare.sleepcheck.log.data.*
import com.daycare.sleepcheck.log.domain.ReminderScheduling
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomPersistenceTest {
    private lateinit var db: AppDatabase
    @Before fun setUp() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build() }
    @After fun tearDown() { db.close() }
    @Test fun facilityPeopleSessionAndRecordPersist() = runBlocking {
        val repo = SleepRepository(db)
        repo.saveSetup(MockTestFixtures.facility, MockTestFixtures.room, MockTestFixtures.staff, MockTestFixtures.firstChild, JurisdictionProfile.ONTARIO, MockTestFixtures.intervalMinutes.toInt())
        val room = db.peopleDao().allRooms().single()
        val staff = db.peopleDao().allStaff().single()
        val session = repo.startSession(room.id)
        val sessionEntity = db.sleepDao().session(session)!!
        val completion = repo.completeCheck(session, staff.id, false, "Observed sleeping normally", true, sessionEntity.startedAt + 1)
        assertEquals(1, db.sleepDao().allRecords().size)
        assertEquals(sessionEntity.startedAt, completion.record.scheduledAt)
        assertEquals(sessionEntity.startedAt + 1, completion.record.observedAt)
        assertEquals("Observed sleeping normally", completion.record.notes)
        assertTrue(completion.record.directVisualCheckConfirmed)
        assertTrue(completion.record.isLate)
        assertEquals(ReminderScheduling.afterCompletedCheck(sessionEntity.startedAt, sessionEntity.intervalMinutes, 1), completion.nextScheduledAt)
        assertTrue(db.sleepDao().session(session)!!.active)
        assertEquals(MockTestFixtures.facility, db.facilityDao().get()?.name)
    }

    @Test fun malformedBackupRollsBackExistingLocalData() = runBlocking {
        db.facilityDao().insert(FacilityEntity("existing", "Existing Facility", "CUSTOM", 10, 1L))
        val context = ApplicationProvider.getApplicationContext<Context>()
        val data = org.json.JSONObject().apply {
            put("facilities", org.json.JSONArray().put(org.json.JSONObject().apply {
                put("id", "new")
                put("name", "New")
                put("jurisdiction", "CUSTOM")
                put("intervalMinutes", "bad")
                put("createdAt", 2L)
            }))
            put("rooms", org.json.JSONArray())
            put("staff", org.json.JSONArray())
            put("children", org.json.JSONArray())
            put("sessions", org.json.JSONArray())
            put("records", org.json.JSONArray())
            put("corrections", org.json.JSONArray())
            put("checklists", org.json.JSONArray())
        }
        try {
            BackupManager(context.contentResolver, db).restoreData(data)
            throw AssertionError("Malformed backup unexpectedly restored")
        } catch (_: org.json.JSONException) {
            // Expected: malformed value fails after clear, and Room rolls back the transaction.
        }
        assertEquals(listOf("Existing Facility"), db.facilityDao().all().map { it.name })
    }
}
