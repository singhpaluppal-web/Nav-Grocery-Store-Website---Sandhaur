package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Order(
    val orderId: String,
    val timestamp: Long,
    val customerName: String,
    val customerPhone: String,
    val deliveryType: String, // "HOME_DELIVERY" or "STORE_PICKUP"
    val address: String,
    val items: List<CartItem>,
    val totalMrp: Int,
    val totalDiscount: Int,
    val finalTotal: Int,
    val status: String = "Dispatched via WhatsApp",
    val paymentMethod: String = PaymentMethod.UPI_INTENT.name,
    val isPaid: Boolean = false,
    val isTestModeOrder: Boolean = false,
    val merchantVpa: String = OwnerSecureLedger.CENTRAL_MERCHANT_VPA,
    val platformCommission: Int = 0,
    val netSellerAmount: Int = 0,
    val sellerName: String = "Nav Grocery Store",
    val paymentStatus: String = "SUCCESS",
    val escrowStatus: String = "HELD_IN_ESCROW"
) {
    val orderStatus: String
        get() = status
}

enum class DeliveryOption(val title: String, val punjabiTitle: String, val badge: String) {
    HOME_DELIVERY(
        "Home Delivery",
        "ਘਰੇ ਡਿਲਿਵਰੀ",
        "~2 hr in Sandhaur & nearby villages"
    ),
    STORE_PICKUP(
        "Store Pickup",
        "ਸਟੋਰ ਪਿਕਅੱਪ",
        "30-min at Main Bazaar"
    )
}
