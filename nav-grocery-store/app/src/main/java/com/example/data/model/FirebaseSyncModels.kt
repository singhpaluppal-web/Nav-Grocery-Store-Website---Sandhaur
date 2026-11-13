package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents the 9 synchronized Firebase Realtime Database nodes
 * shared across Public App, Admin Controller App, public website, and admin console.
 */
object FirebaseNodes {
    const val PRODUCTS = "products"       // Node 1: 308 locked SKUs + approved vendor items
    const val SELLERS = "sellers"         // Node 2: Shopkeeper registrations & KYC
    const val ORDERS = "orders"           // Node 3: Real-time customer orders & cart
    const val COMMISSIONS = "commissions" // Node 4: Category-wise platform commission rates
    const val PAYOUTS = "payouts"         // Node 5: Central merchant escrow & settled payouts
    const val LOGISTICS = "logistics"     // Node 6: Hyperlocal delivery riders & AWB courier tracking
    const val CAMPAIGNS = "campaigns"     // Node 7: Home carousel banners, flash deals & push logs
    const val STAFF = "staff"             // Node 8: Restricted Cashier & Delivery partner logins
    const val SYSTEM_LOCKS = "system_locks" // Node 9: Catalog, price, policy, & emergency storefront freeze
}

@JsonClass(generateAdapter = true)
data class SystemLocks(
    @Json(name = "catalogLocked") val catalogLocked: Boolean = true,
    @Json(name = "pricingIntegrityLocked") val pricingIntegrityLocked: Boolean = true, // Flat 12% OFF rule
    @Json(name = "strictPolicyGuardLocked") val strictPolicyGuardLocked: Boolean = true, // No fresh produce
    @Json(name = "emergencyStorefrontFreeze") val emergencyStorefrontFreeze: Boolean = false,
    @Json(name = "lastGoldenSnapshotTimestamp") val lastGoldenSnapshotTimestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class LogisticsRecord(
    @Json(name = "id") val id: String,
    @Json(name = "orderId") val orderId: String,
    @Json(name = "riderName") val riderName: String = "Sandhaur Express Rider",
    @Json(name = "riderPhone") val riderPhone: String = "8847370905",
    @Json(name = "carrier") val carrier: String = "HYPERLOCAL_SANDHAUR", // or SHIPROCKET, DELHIVERY, BLUEDART, INDIA_POST
    @Json(name = "awbNumber") val awbNumber: String = "",
    @Json(name = "deliveryOtp") val deliveryOtp: String = "1480",
    @Json(name = "status") val status: String = "ASSIGNED" // ASSIGNED, OUT_FOR_DELIVERY, DELIVERED
)

@JsonClass(generateAdapter = true)
data class FlashCampaign(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "subtitle") val subtitle: String,
    @Json(name = "bannerUrl") val bannerUrl: String = "",
    @Json(name = "discountPercent") val discountPercent: Int = 12,
    @Json(name = "expiresAtMillis") val expiresAtMillis: Long = System.currentTimeMillis() + 86400000L,
    @Json(name = "isActive") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class StaffMember(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String, // CASHIER, PACKER, DELIVERY_RIDER
    @Json(name = "phone") val phone: String,
    @Json(name = "accessPin") val accessPin: String,
    @Json(name = "isActive") val isActive: Boolean = true
)
