package com.daycare.sleepcheck.log

import com.daycare.sleepcheck.log.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AuditTrailTest {
    @Test fun correctionReferencesOriginalAndDoesNotReplaceIt() {
        val original = CheckRecordEntity("record", "session", "room", "staff", 1, 2, 3, ObservationType.NORMAL.name, "", true, false)
        val correction = CorrectionAuditEntity("correction", original.id, "Wrong note", null, 4, null, "Updated note", 5, "staff")
        assertEquals(original.id, correction.originalRecordId)
        assertNotEquals(original.observedAt, correction.correctedObservedAt)
        assertEquals(2L, original.observedAt)
    }
}
