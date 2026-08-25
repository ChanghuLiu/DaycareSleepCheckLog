package com.daycare.sleepcheck.log

import com.daycare.sleepcheck.log.domain.ReminderAccuracy
import com.daycare.sleepcheck.log.domain.ReminderScheduling
import org.junit.Assert.*
import org.junit.Test

class ReminderSchedulingTest {
    @Test fun nextReminderUsesTheNextUncompletedCheck() {
        assertEquals(901_000L, ReminderScheduling.nextReminderAt(1_000L, 15, 1))
    }

    @Test fun completingACheckSchedulesTheFollowingReminder() {
        assertEquals(1_801_000L, ReminderScheduling.afterCompletedCheck(1_000L, 15, 2))
    }

    @Test fun intervalChangeRecalculatesNextReminder() {
        assertEquals(1_201_000L, ReminderScheduling.afterIntervalChange(1_000L, 20, 1))
    }

    @Test fun endingASessionHasNoPendingReminderPlan() {
        assertNull(ReminderScheduling.cancelOnSessionEnd())
        assertNull(ReminderScheduling.planFor(false, "session", 1_000L, 15, 0))
    }

    @Test fun deniedPreciseAlarmCapabilityFallsBackToInexact() {
        assertEquals(ReminderAccuracy.INEXACT, ReminderScheduling.accuracy(36, false))
        assertEquals(ReminderAccuracy.PRECISE, ReminderScheduling.accuracy(36, true))
        assertEquals(ReminderAccuracy.PRECISE, ReminderScheduling.accuracy(30, false))
    }
}
