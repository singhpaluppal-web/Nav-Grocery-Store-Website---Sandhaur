package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Order(
    val orderId: String,
    val timestamp: Long,
    val customerName: String,
    val customerPhone: String,
    val deliveryType: String,
    val address: String,
    val items: List<CartItem>,
    val totalMrp: Int,
    val totalDiscount: Int,
    val finalTotal: Int,
    val status: String = "Order Placed",
    val paymentMethod: String = PaymentMethod.UPI_INTENT.name,
    val isPaid: Boolean = false,
    val isTestModeOrder: Boolean = false,
    val merchantVpa: String = StoreProfile.MERCHANT_UPI_VPA,
    val platformCommission: Int = 0,
    val netSellerAmount: Int = 0,
    val sellerName: String = "Nav Grocery Store",
    val paymentStatus: String = "SUCCESS",
    val escrowStatus: String = "HELD_IN_ESCROW",
    val deliveryFee: Int = 0,
    val spinCouponRupees: Int = 0,
    val spinCouponCode: String = "",
    val coinsRedeemed: Int = 0,
    val petiDiscountRupees: Int = 0,
    val walletDeductedRupees: Int = 0,
    val dispatchSlot: String = "",
    val isNriOrder: Boolean = false,
    val deliveryProofUrl: String = "",
    val integrityDigest: String = ""
) {
    val orderStatus: String
        get() = status

    val totalItemsCount: Int
        get() = items.sumOf { it.quantity }
}

enum class DeliveryOption(val title: String, val punjabiTitle: String, val badge: String) {
    EXPRESS_15_MIN(
        "15-Min Express",
        "15-ਮਿੰਟ ਐਕਸਪ੍ਰੈੱਸ",
        "₹25 in Sandhaur 148020 (or Free via Spin)"
    ),
    STORE_PICKUP(
        "Store Pickup",
        "ਸਟੋਰ ਪਿਕਅੱਪ",
        "30-min at Main Bazaar (FREE)"
    ),
    PIND_SCHEDULED(
        "Pind-to-Pind Route",
        "ਪਿੰਡ-ਤੋਂ-ਪਿੰਡ ਰੂਟ",
        "11 AM / 6 PM Van • Sherwani Kot, Kasba Bharal, Kalyan, Matoi, Khurd"
    ),
    LOCAL_2_HR(
        "2-Hr Local Delivery",
        "2-ਘੰਟੇ ਲੋਕਲ ਡਿਲਿਵਰੀ",
        "FREE across Sandhaur & nearby villages"
    ),
    HOME_DELIVERY(
        "Home Delivery",
        "ਘਰੇ ਡਿਲਿਵਰੀ",
        "~2 hr in Sandhaur & nearby villages"
    )
}
