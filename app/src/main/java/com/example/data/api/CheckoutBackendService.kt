package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Minimal client checkout line item: strictly identifiers and quantity.
 * Selling price, MRP, discounts, GST, and Peti slabs are NEVER sent from client;
 * the backend recomputes them authoritatively from live Firebase product & storeConfig data.
 */
@JsonClass(generateAdapter = true)
data class SecureCheckoutItemPayload(
    @Json(name = "productId") val productId: String,
    @Json(name = "variantId") val variantId: String,
    @Json(name = "quantity") val quantity: Int
)

/**
 * Minimal client-to-backend order verification & creation payload.
 * Includes an idempotency key to prevent duplicate submissions / replay attacks.
 */
@JsonClass(generateAdapter = true)
data class SecureOrderSubmissionRequest(
    @Json(name = "idempotencyKey") val idempotencyKey: String,
    @Json(name = "items") val items: List<SecureCheckoutItemPayload>,
    @Json(name = "redeemCoinsRequested") val redeemCoinsRequested: Int,
    @Json(name = "applySpinCoupon") val applySpinCoupon: Boolean,
    @Json(name = "applyWalletBalance") val applyWalletBalance: Boolean,
    @Json(name = "deliveryTier") val deliveryTier: String,
    @Json(name = "paymentMethod") val paymentMethod: String,
    @Json(name = "addressSnapshot") val addressSnapshot: String,
    @Json(name = "pinCode") val pinCode: String = "",
    @Json(name = "dispatchSlot") val dispatchSlot: String,
    @Json(name = "isNriOrder") val isNriOrder: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ServerVerifiedOrderResponse(
    @Json(name = "orderId") val orderId: String? = null,
    @Json(name = "verifiedFinalTotal") val verifiedFinalTotal: Int? = null,
    @Json(name = "verifiedTotalMrp") val verifiedTotalMrp: Int? = null,
    @Json(name = "verifiedWholesaleDiscount") val verifiedWholesaleDiscount: Int? = null,
    @Json(name = "verifiedPetiDiscount") val verifiedPetiDiscount: Int? = null,
    @Json(name = "verifiedCoinDiscountRupees") val verifiedCoinDiscountRupees: Int? = null,
    @Json(name = "verifiedCoinsRedeemed") val verifiedCoinsRedeemed: Int? = null,
    @Json(name = "verifiedDeliveryFee") val verifiedDeliveryFee: Int? = null,
    @Json(name = "verifiedCoinsEarned") val verifiedCoinsEarned: Int? = null,
    @Json(name = "paymentSessionUrl") val paymentSessionUrl: String? = null,
    @Json(name = "status") val status: String? = null
)

interface CheckoutBackendService {
    @POST("api/orders/verify-and-create")
    suspend fun verifyAndCreateOrder(
        @Header("Authorization") bearerIdToken: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body payload: SecureOrderSubmissionRequest
    ): Response<ServerVerifiedOrderResponse>
}
