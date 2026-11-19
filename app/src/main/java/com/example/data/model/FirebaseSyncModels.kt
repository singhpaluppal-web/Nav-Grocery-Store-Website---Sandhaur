package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents the 9 synchronized Firebase Realtime Database nodes
 * shared across Public App, Admin Controller App, public website, and admin console.
 */
object FirebaseNodes {
    const val PRODUCTS = "products"         // Node 1: Dynamic A-to-Z marketplace catalog + verified packaged/frozen groceries
    const val CATEGORIES = "categories"     // Node 2: Dynamic A-to-Z categories & category-level discount overrides
    const val STORE_CONFIG = "storeConfig"  // Node 3: Remote offers, banner text, discountPercent & coinConfig
    const val ORDERS = "orders"             // Node 4: Real-time customer orders & cart
    const val LOGISTICS = "logistics"       // Node 5: Hyperlocal delivery riders & AWB courier tracking
    const val CAMPAIGNS = "campaigns"       // Node 6: Home carousel banners & flash deals
    const val SYSTEM_LOCKS = "system_locks" // Node 7: Catalog, price, policy, & emergency storefront freeze
}

@JsonClass(generateAdapter = true)
data class SystemLocks(
    @Json(name = "catalogLocked") val catalogLocked: Boolean = true,
    @Json(name = "pricingIntegrityLocked") val pricingIntegrityLocked: Boolean = true,
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
    @Json(name = "deliveryOtp") val deliveryOtp: String = "",
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
data class HeroBannerSlide(
    @Json(name = "id") val id: String,
    @Json(name = "badge") val badge: String,
    @Json(name = "headline") val headline: String,
    @Json(name = "subtitle") val subtitle: String,
    @Json(name = "ctaText") val ctaText: String = "Shop Now",
    @Json(name = "ctaCategory") val ctaCategory: String = "All Products",
    @Json(name = "imageUrl") val imageUrl: String = ""
) {
    companion object {
        val DEFAULT_SLIDES = listOf(
            HeroBannerSlide(
                id = "HERO_1",
                badge = "MEGA MARKETPLACE LAUNCH",
                headline = "Everything You Need, Delivered Fast from Sandhaur Hub",
                subtitle = "Fashion, 5G Mobiles, Footwear, Kitchen & 308+ Sealed Groceries",
                ctaText = "Shop Now",
                ctaCategory = "All Products",
                imageUrl = "images/hero_slide_1.jpg"
            ),
            HeroBannerSlide(
                id = "HERO_2",
                badge = "FASHION & FOOTWEAR FEST",
                headline = "Trendsetting Clothing & Branded Shoes — Up to 40% Off",
                subtitle = "Punjabi Suits, Kurtas, Denim & Puma / Nike Sneakers (S–XXL, UK 6–11)",
                ctaText = "Shop Fashion",
                ctaCategory = "Clothing and Apparel",
                imageUrl = "images/hero_slide_2.jpg"
            ),
            HeroBannerSlide(
                id = "HERO_3",
                badge = "SMART ELECTRONICS & MOBILES UPGRADE",
                headline = "Latest 5G Phones, Audio & Home Tech with Brand Warranty",
                subtitle = "iQOO, Samsung, Sony Headphones, JBL & Appliances • 1-Yr Warranty",
                ctaText = "Shop Tech",
                ctaCategory = "Electronics and Mobiles",
                imageUrl = "images/hero_slide_3.jpg"
            ),
            HeroBannerSlide(
                id = "HERO_4",
                badge = "NAV GROCERY STORE WHOLESALE SAVINGS",
                headline = "Live Wholesale Savings on Verified Packaged & Frozen Groceries",
                subtitle = "100% Sealed Atta, Rice, Ghee, Spices & Cold-Chain Frozen • Zero Fresh Produce",
                ctaText = "Shop Groceries",
                ctaCategory = "Packaged Groceries",
                imageUrl = "images/hero_slide_4.jpg"
            )
        )
    }
}
