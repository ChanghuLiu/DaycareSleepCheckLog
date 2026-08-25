package com.daycare.sleepcheck.log.domain

enum class ReminderAccuracy { PRECISE, INEXACT }

data class ReminderPlan(val sessionId: String, val scheduledAt: Long)

object ReminderScheduling {
    fun planFor(active: Boolean, sessionId: String, sessionStartedAt: Long, intervalMinutes: Int, completedCount: Int): ReminderPlan? =
        if (active) ReminderPlan(sessionId, nextReminderAt(sessionStartedAt, intervalMinutes, completedCount)) else null

    fun cancelOnSessionEnd(): ReminderPlan? = null

    fun nextReminderAt(sessionStartedAt: Long, intervalMinutes: Int, completedCount: Int): Long =
        CheckScheduling.nextScheduledAt(sessionStartedAt, intervalMinutes, completedCount)

    fun afterCompletedCheck(sessionStartedAt: Long, intervalMinutes: Int, completedCountAfterSave: Int): Long =
        nextReminderAt(sessionStartedAt, intervalMinutes, completedCountAfterSave)

    fun afterIntervalChange(sessionStartedAt: Long, newIntervalMinutes: Int, completedCount: Int): Long =
        nextReminderAt(sessionStartedAt, newIntervalMinutes, completedCount)

    fun accuracy(apiLevel: Int, canScheduleExactAlarms: Boolean): ReminderAccuracy =
        if (apiLevel < 31 || canScheduleExactAlarms) ReminderAccuracy.PRECISE else ReminderAccuracy.INEXACT
}
