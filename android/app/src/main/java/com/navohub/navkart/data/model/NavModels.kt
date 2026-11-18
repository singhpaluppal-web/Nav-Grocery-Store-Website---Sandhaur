package com.navohub.navkart.data.model

import kotlin.math.roundToInt

enum class ServiceVertical(val label: String) {
    STORE("Store (Grocery)"),
    SUPERMALL("Nav Kart Supermall"),
    EXPRESS("Express 15m"),
    ESSENTIALS("Essentials")
}

enum class PaymentChannel(val label: String) {
    UPI("UPI App / QR"),
    RAZORPAY("Razorpay Cards/NetBanking"),
    BANK_TRANSFER("NEFT / IMPS Bank Transfer"),
    COD("Cash on Delivery")
}

data class PublishingChannels(
    val navGroceryWeb: Boolean = true,
    val navKartWeb: Boolean = true,
    val androidApps: Boolean = true
)

data class NavProduct(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val category: String = "",
    val department: String = "",
    val mrp: Int = 0,
    val sale: Int = 0,
    val stock: Int = 0,
    val gst: String = "5%",
    val barcode: String? = null,
    val image: String = "",
    val customImage: String? = null,
    val channels: PublishingChannels = PublishingChannels()
) {
    val effectiveSale: Int
        get() = if (mrp > 0) (mrp * 0.88).roundToInt() else sale

    val isFrozen: Boolean
        get() = department.contains("frozen", ignoreCase = true) ||
            category.contains("frozen", ignoreCase = true)

    val isElectronicsOrHome: Boolean
        get() = department.contains("electronic", ignoreCase = true) ||
            department.contains("mobile", ignoreCase = true) ||
            department.contains("kitchen", ignoreCase = true)

    val isApparel: Boolean
        get() = department.contains("clothing", ignoreCase = true) ||
            department.contains("apparel", ignoreCase = true)

    val isGeneralRetail: Boolean
        get() = department.contains("general retail", ignoreCase = true) ||
            department.contains("stationery", ignoreCase = true) ||
            department.contains("luggage", ignoreCase = true)

    val isGroceryCore: Boolean
        get() = !isElectronicsOrHome && !isApparel && !isGeneralRetail

    fun resolvedImageUrl(webBase: String = "https://www.navgrocerystore.com"): String {
        val candidate = customImage?.takeIf { it.isNotBlank() } ?: image
        return when {
            candidate.startsWith("http://") || candidate.startsWith("https://") -> candidate
            candidate.startsWith("/") -> "$webBase$candidate"
            id.startsWith("NAV", ignoreCase = true) -> {
                val stem = id.removePrefix("NAV").padStart(3, '0')
                "$webBase/products/$stem.jpg"
            }
            else -> ""
        }
    }

    fun isSafeRetailBoundary(): Boolean {
        val text = "$name $category $department".lowercase()
        val bannedFresh = listOf("fresh fruit", "fresh vegetable", "sabzi", "fresh produce")
        return bannedFresh.none { text.contains(it) }
    }
}

data class AndroidFeedResponse(
    val store: String = "",
    val updatedAt: String = "",
    val totalItems: Int = 0,
    val items: List<NavProduct> = emptyList()
)

data class CartEntry(
    val product: NavProduct,
    val quantity: Int
)

data class OrderItemPayload(
    val id: String,
    val name: String,
    val brand: String,
    val qty: Int,
    val mrp: Int,
    val sale: Int,
    val gst: String
)

data class OrderPayload(
    val orderId: String,
    val createdAt: String,
    val customerName: String,
    val customerPhone: String,
    val address: String,
    val fulfillment: String,
    val slot: String,
    val paymentMethod: String,
    val paymentStatus: String,
    val utrRef: String? = null,
    val subtotalMrp: Int,
    val subtotalSale: Int,
    val gstTotal: Int,
    val savings: Int,
    val deliveryFee: Int,
    val totalPayable: Int,
    val status: String = "Pending",
    val items: List<OrderItemPayload>
)
