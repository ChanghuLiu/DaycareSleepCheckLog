package com.daycare.sleepcheck.log.domain

import com.daycare.sleepcheck.log.data.JurisdictionProfile

object JurisdictionDefaults {
    const val CALIFORNIA_MINUTES = 15

    fun intervalFor(profile: JurisdictionProfile): Int? = when (profile) {
        JurisdictionProfile.ONTARIO -> null
        JurisdictionProfile.CALIFORNIA -> CALIFORNIA_MINUTES
        JurisdictionProfile.CUSTOM -> null
    }

    fun requiresFacilityPolicyInterval(profile: JurisdictionProfile): Boolean =
        profile == JurisdictionProfile.ONTARIO || profile == JurisdictionProfile.CUSTOM

    fun validateFacilityPolicyInterval(intervalMinutes: Int?): Int =
        intervalMinutes?.takeIf { it > 0 } ?: throw IllegalArgumentException("A facility policy interval is required")
}

object CheckScheduling {
    fun nextScheduledAt(sessionStartedAt: Long, intervalMinutes: Int, completedCount: Int): Long =
        sessionStartedAt + completedCount * intervalMinutes.coerceAtLeast(1) * 60_000L

    fun isLate(scheduledAt: Long, observedAt: Long): Boolean = observedAt > scheduledAt
}

class DirectVisualCheckRequired : IllegalArgumentException()

object CheckCompletion {
    fun validate(directVisualCheckConfirmed: Boolean) {
        if (!directVisualCheckConfirmed) throw DirectVisualCheckRequired()
    }
}
