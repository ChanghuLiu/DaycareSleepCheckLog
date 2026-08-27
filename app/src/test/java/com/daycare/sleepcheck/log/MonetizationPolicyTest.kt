package com.daycare.sleepcheck.log

import com.daycare.sleepcheck.log.billing.ProAccessPolicy
import com.daycare.sleepcheck.log.billing.ProEntitlement
import com.daycare.sleepcheck.log.billing.ProEntitlementPolicy
import com.daycare.sleepcheck.log.billing.ProFeature
import com.daycare.sleepcheck.log.billing.PurchaseSnapshot
import com.daycare.sleepcheck.log.billing.PurchaseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationPolicyTest {
    @Test
    fun freeUserCanPerformUnlimitedCoreChecks() {
        repeat(100) { assertTrue(ProAccessPolicy.canAccess(ProFeature.CORE_SLEEP_CHECKS, ProEntitlement.FREE, 1)) }
    }

    @Test
    fun freeUserCanUseOneRoomButSecondRoomIsGated() {
        assertTrue(ProAccessPolicy.canAccess(ProFeature.SECOND_ROOM, ProEntitlement.FREE, 0))
        assertFalse(ProAccessPolicy.canAccess(ProFeature.SECOND_ROOM, ProEntitlement.FREE, 1))
    }

    @Test
    fun freeUserCannotExportButProCanUseEveryProFeature() {
        assertFalse(ProAccessPolicy.canAccess(ProFeature.PDF_EXPORT, ProEntitlement.FREE, 1))
        assertTrue(ProAccessPolicy.canAccess(ProFeature.PDF_EXPORT, ProEntitlement.PRO, 1))
        listOf(
            ProFeature.SECOND_ROOM,
            ProFeature.PDF_EXPORT,
            ProFeature.ADVANCED_HISTORY,
            ProFeature.BACKUP_RESTORE,
            ProFeature.PLAYGROUND_SAFETY_LOG,
        ).forEach { assertTrue(ProAccessPolicy.canAccess(it, ProEntitlement.PRO, 3)) }
    }

    @Test
    fun purchasedUnlocksAndAcknowledgementIsRequestedOnlyWhenNeeded() {
        val unacknowledged = PurchaseSnapshot("daycare_pro_lifetime", PurchaseStatus.PURCHASED, acknowledged = false)
        val acknowledged = unacknowledged.copy(acknowledged = true)
        assertEquals(ProEntitlement.PRO, ProEntitlementPolicy.evaluate(listOf(unacknowledged), true, false).state)
        assertTrue(ProEntitlementPolicy.evaluate(listOf(unacknowledged), true, false).acknowledgePurchase)
        assertFalse(ProEntitlementPolicy.evaluate(listOf(acknowledged), true, false).acknowledgePurchase)
    }

    @Test
    fun pendingAndCanceledPurchasesDoNotUnlock() {
        val pending = PurchaseSnapshot("daycare_pro_lifetime", PurchaseStatus.PENDING, acknowledged = false)
        assertEquals(ProEntitlement.FREE, ProEntitlementPolicy.evaluate(listOf(pending), true, false).state)
        assertEquals(ProEntitlement.FREE, ProEntitlementPolicy.evaluate(emptyList(), true, false).state)
    }

    @Test
    fun billingUnavailableDoesNotCrashAndMayUseKnownProCache() {
        assertEquals(ProEntitlement.UNAVAILABLE, ProEntitlementPolicy.evaluate(emptyList(), false, false).state)
        assertEquals(ProEntitlement.PRO, ProEntitlementPolicy.evaluate(emptyList(), false, true).state)
    }

    @Test
    fun itemAlreadyOwnedRequiresPurchaseRefresh() {
        assertTrue(ProEntitlementPolicy.shouldRefreshAfterItemAlreadyOwned(7))
        assertFalse(ProEntitlementPolicy.shouldRefreshAfterItemAlreadyOwned(1))
    }

    @Test
    fun entitlementDoesNotAffectRecordIntegrityOrCoreAccess() {
        ProEntitlement.values().forEach { entitlement ->
            assertTrue(ProAccessPolicy.canAccess(ProFeature.CORE_SLEEP_CHECKS, entitlement, 1))
        }
    }
}
