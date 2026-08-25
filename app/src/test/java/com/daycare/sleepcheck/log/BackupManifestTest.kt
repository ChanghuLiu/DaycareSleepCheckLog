package com.daycare.sleepcheck.log

import com.daycare.sleepcheck.log.data.BackupManifestValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManifestTest {
    @Test fun validManifestIsAccepted() {
        assertTrue(BackupManifestValidator.isValid("""{"format":"daycare-sleep-check-log","formatVersion":1,"package":"com.daycare.sleepcheck.log","data":{}}"""))
    }

    @Test fun wrongPackageOrVersionIsRejected() {
        assertFalse(BackupManifestValidator.isValid("""{"format":"daycare-sleep-check-log","formatVersion":2,"package":"com.daycare.sleepcheck.log","data":{}}"""))
        assertFalse(BackupManifestValidator.isValid("not json"))
    }
}
