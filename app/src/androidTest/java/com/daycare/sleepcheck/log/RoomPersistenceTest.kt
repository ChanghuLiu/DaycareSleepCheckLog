package com.daycare.sleepcheck.log

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daycare.sleepcheck.log.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPersistenceTest {
    private lateinit var db: AppDatabase
    @Before fun setUp() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build() }
    @After fun tearDown() { db.close() }
    @Test fun facilityPeopleSessionAndRecordPersist() = runBlocking {
        val repo = SleepRepository(db)
        repo.saveSetup("Test Daycare", "Nap Room", "A. Staff", "Child One", JurisdictionProfile.ONTARIO, 15)
        val room = db.peopleDao().allRooms().single()
        val staff = db.peopleDao().allStaff().single()
        val session = repo.startSession(room.id)
        repo.completeCheck(session, staff.id, false, "", true, 1000)
        assertEquals(1, db.sleepDao().allRecords().size)
        assertEquals("Test Daycare", db.facilityDao().get()?.name)
    }
}
