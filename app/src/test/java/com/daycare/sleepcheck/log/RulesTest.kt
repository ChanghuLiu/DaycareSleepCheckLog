package com.daycare.sleepcheck.log

import com.daycare.sleepcheck.log.data.JurisdictionProfile
import com.daycare.sleepcheck.log.domain.CheckCompletion
import com.daycare.sleepcheck.log.domain.CheckScheduling
import com.daycare.sleepcheck.log.domain.DirectVisualCheckRequired
import com.daycare.sleepcheck.log.domain.JurisdictionDefaults
import org.junit.Assert.*
import org.junit.Test

class RulesTest {
    @Test fun intervalSchedulingUsesSessionStartAndCompletedCount() {
        assertEquals(1_000L, CheckScheduling.nextScheduledAt(1_000L, 15, 0))
        assertEquals(901_000L, CheckScheduling.nextScheduledAt(1_000L, 15, 1))
    }

    @Test fun lateDetectionUsesObservedTimestamp() {
        assertFalse(CheckScheduling.isLate(1_000, 1_000))
        assertTrue(CheckScheduling.isLate(1_000, 1_001))
    }

    @Test fun directVisualConfirmationIsRequired() {
        assertThrows(DirectVisualCheckRequired::class.java) { CheckCompletion.validate(false) }
        CheckCompletion.validate(true)
    }

    @Test fun ontarioHasNoPredefinedRegulatoryInterval() {
        assertEquals(null, JurisdictionDefaults.intervalFor(JurisdictionProfile.ONTARIO))
        assertTrue(JurisdictionDefaults.requiresFacilityPolicyInterval(JurisdictionProfile.ONTARIO))
    }

    @Test fun ontarioSetupRequiresASelectedInterval() {
        assertThrows(IllegalArgumentException::class.java) { JurisdictionDefaults.validateFacilityPolicyInterval(null) }
        assertEquals(20, JurisdictionDefaults.validateFacilityPolicyInterval(20))
    }

    @Test fun californiaDefaultsToFifteenMinutes() {
        assertEquals(15, JurisdictionDefaults.intervalFor(JurisdictionProfile.CALIFORNIA))
        assertFalse(JurisdictionDefaults.requiresFacilityPolicyInterval(JurisdictionProfile.CALIFORNIA))
    }

    @Test fun customRequiresASelectedInterval() {
        assertEquals(null, JurisdictionDefaults.intervalFor(JurisdictionProfile.CUSTOM))
        assertThrows(IllegalArgumentException::class.java) { JurisdictionDefaults.validateFacilityPolicyInterval(0) }
    }
}
