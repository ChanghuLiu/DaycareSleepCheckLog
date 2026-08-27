package com.daycare.sleepcheck.log.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BillingMessage {
    PURCHASE_PENDING,
    PURCHASE_CANCELED,
    BILLING_UNAVAILABLE,
    PURCHASE_FAILED,
    PURCHASE_RESTORED,
}

/** One lifecycle owner for Play Billing and the local, non-sensitive Pro cache. */
class ProEntitlementRepository(context: Context) : PurchasesUpdatedListener {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val _entitlement = MutableStateFlow(ProEntitlement.CHECKING)
    val entitlement: StateFlow<ProEntitlement> = _entitlement.asStateFlow()
    private val _localizedPrice = MutableStateFlow<String?>(null)
    val localizedPrice: StateFlow<String?> = _localizedPrice.asStateFlow()
    private val _message = MutableStateFlow<BillingMessage?>(null)
    val message: StateFlow<BillingMessage?> = _message.asStateFlow()

    private var productDetails: ProductDetails? = null
    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        if (billingClient.isReady) {
            queryProductDetails()
            queryPurchases()
            return
        }
        _entitlement.value = ProEntitlement.CHECKING
        billingClient.startConnection(object : BillingClientStateListenerAdapter() {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryPurchases()
                } else {
                    markUnavailable()
                }
            }

            override fun onBillingServiceDisconnected() {
                markUnavailable()
            }
        })
    }

    fun clearMessage() {
        _message.value = null
    }

    fun launchPurchase(activity: Activity) {
        val details = productDetails
        if (details == null || !billingClient.isReady) {
            markUnavailable()
            connect()
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            queryPurchases()
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _message.value = BillingMessage.PURCHASE_CANCELED
        } else if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _message.value = BillingMessage.PURCHASE_FAILED
        }
    }

    fun refresh() {
        if (billingClient.isReady) queryPurchases() else connect()
    }

    fun close() {
        billingClient.endConnection()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> reconcile(purchases.orEmpty(), showRestored = true)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> queryPurchases()
            BillingClient.BillingResponseCode.USER_CANCELED -> _message.value = BillingMessage.PURCHASE_CANCELED
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> markUnavailable()
            else -> _message.value = BillingMessage.PURCHASE_FAILED
        }
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        ) { result, detailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsResult.productDetailsList.firstOrNull()
                _localizedPrice.value = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
            } else {
                _localizedPrice.value = null
            }
        }
    }

    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                reconcile(purchases, showRestored = false)
            } else {
                markUnavailable()
            }
        }
    }

    private fun reconcile(purchases: List<Purchase>, showRestored: Boolean) {
        val snapshots = purchases.flatMap { purchase ->
            purchase.products.map { productId ->
                PurchaseSnapshot(
                    productId = productId,
                    status = if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) PurchaseStatus.PURCHASED else PurchaseStatus.PENDING,
                    acknowledged = purchase.isAcknowledged,
                )
            }
        }
        val decision = ProEntitlementPolicy.evaluate(snapshots, billingAvailable = true, cachedKnownPro = false)
        _entitlement.value = decision.state
        if (decision.state == ProEntitlement.PRO) {
            preferences.edit().putBoolean(CACHED_PRO, true).apply()
            purchases.filter { it.products.contains(PRO_PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
                .forEach(::acknowledge)
            if (showRestored) _message.value = BillingMessage.PURCHASE_RESTORED
        } else if (snapshots.none { it.productId == PRO_PRODUCT_ID && it.status == PurchaseStatus.PENDING }) {
            preferences.edit().putBoolean(CACHED_PRO, false).apply()
        } else {
            _message.value = BillingMessage.PURCHASE_PENDING
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) _message.value = BillingMessage.PURCHASE_FAILED
        }
    }

    private fun markUnavailable() {
        val cached = preferences.getBoolean(CACHED_PRO, false)
        _entitlement.value = if (cached) ProEntitlement.PRO else ProEntitlement.UNAVAILABLE
        _message.value = BillingMessage.BILLING_UNAVAILABLE
    }

    private abstract class BillingClientStateListenerAdapter : BillingClientStateListener

    private companion object {
        const val PREFERENCES = "pro_entitlement"
        const val CACHED_PRO = "known_pro"
    }
}
