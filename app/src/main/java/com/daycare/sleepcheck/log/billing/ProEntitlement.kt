package com.daycare.sleepcheck.log.billing

/** The product is a one-time, non-consumable purchase. */
const val PRO_PRODUCT_ID = "daycare_pro_lifetime"

enum class ProEntitlement {
    CHECKING,
    FREE,
    PRO,
    UNAVAILABLE,
}

enum class ProFeature {
    CORE_SLEEP_CHECKS,
    SECOND_ROOM,
    PDF_EXPORT,
    ADVANCED_HISTORY,
    BACKUP_RESTORE,
    PLAYGROUND_SAFETY_LOG,
}

/** Centralizes gates so core records and sleep checks are never entitlement-gated. */
object ProAccessPolicy {
    fun canAccess(feature: ProFeature, entitlement: ProEntitlement, roomCount: Int): Boolean = when (feature) {
        ProFeature.CORE_SLEEP_CHECKS -> true
        ProFeature.SECOND_ROOM -> roomCount < 1 || entitlement == ProEntitlement.PRO
        ProFeature.PDF_EXPORT,
        ProFeature.ADVANCED_HISTORY,
        ProFeature.BACKUP_RESTORE,
        ProFeature.PLAYGROUND_SAFETY_LOG -> entitlement == ProEntitlement.PRO
    }
}

enum class PurchaseStatus {
    PURCHASED,
    PENDING,
}

data class PurchaseSnapshot(
    val productId: String,
    val status: PurchaseStatus,
    val acknowledged: Boolean,
)

data class EntitlementDecision(
    val state: ProEntitlement,
    val acknowledgePurchase: Boolean,
)

/** Pure purchase rules used by the BillingClient adapter and unit tests. */
object ProEntitlementPolicy {
    fun evaluate(
        purchases: List<PurchaseSnapshot>,
        billingAvailable: Boolean,
        cachedKnownPro: Boolean,
    ): EntitlementDecision {
        val productPurchases = purchases.filter { it.productId == PRO_PRODUCT_ID }
        val purchased = productPurchases.firstOrNull { it.status == PurchaseStatus.PURCHASED }
        if (purchased != null) {
            return EntitlementDecision(ProEntitlement.PRO, !purchased.acknowledged)
        }
        if (!billingAvailable) {
            return EntitlementDecision(
                if (cachedKnownPro) ProEntitlement.PRO else ProEntitlement.UNAVAILABLE,
                acknowledgePurchase = false,
            )
        }
        return EntitlementDecision(ProEntitlement.FREE, acknowledgePurchase = false)
    }

    fun shouldRefreshAfterItemAlreadyOwned(responseCode: Int): Boolean = responseCode == ITEM_ALREADY_OWNED

    private const val ITEM_ALREADY_OWNED = 7
}
