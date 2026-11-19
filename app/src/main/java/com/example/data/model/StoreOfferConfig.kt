package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Live coin loyalty configuration fetched from Firebase RTDB `/storeConfig/offers/coinConfig`.
 * Zero coin rules are hardcoded; falls back gracefully when offline.
 */
@JsonClass(generateAdapter = true)
data class CoinRewardConfig(
    @param:Json(name = "isCoinsEnabled") val isCoinsEnabled: Boolean = true,
    @param:Json(name = "earnRatePercent") val earnRatePercent: Double = 4.0,
    @param:Json(name = "coinValueInr") val coinValueInr: Double = 1.0,
    @param:Json(name = "maxRedeemPercent") val maxRedeemPercent: Double = 25.0
)

/**
 * Live store-wide pricing, promotional banner, and coin loyalty rules
 * synced in real time from Firebase RTDB `/storeConfig/offers`.
 */
@JsonClass(generateAdapter = true)
data class StoreOffersConfig(
    @param:Json(name = "discountPercent") val discountPercent: Double = DEFAULT_DISCOUNT_PERCENT,
    @param:Json(name = "offerBannerText") val offerBannerText: String = "",
    @param:Json(name = "isOfferActive") val isOfferActive: Boolean = false,
    @param:Json(name = "petiExtraDiscountPercent") val petiExtraDiscountPercent: Double = DEFAULT_PETI_EXTRA_DISCOUNT_PERCENT,
    @param:Json(name = "coinConfig") val coinConfig: CoinRewardConfig = CoinRewardConfig()
) {
    val sanitizedDiscountPercent: Double
        get() = discountPercent.coerceIn(0.0, 90.0)

    val sanitizedPetiExtraDiscountPercent: Double
        get() = petiExtraDiscountPercent.coerceIn(0.0, 50.0)

    val discountLabelPercent: String
        get() = formatPercent(sanitizedDiscountPercent)

    val petiLabelPercent: String
        get() = formatPercent(sanitizedPetiExtraDiscountPercent)

    val petiSlabFactor: Double
        get() = (1.0 - sanitizedPetiExtraDiscountPercent / 100.0).coerceIn(0.1, 1.0)

    fun sellingPrice(mrp: Int, effectiveDiscountPercent: Double = sanitizedDiscountPercent): Int {
        if (mrp <= 0) return 0
        val pct = effectiveDiscountPercent.coerceIn(0.0, 90.0)
        return (mrp * (1.0 - pct / 100.0)).roundToInt().coerceAtLeast(0)
    }

    fun maxRedeemableCoins(afterCouponRupees: Int, availableCoins: Int): Int {
        if (!coinConfig.isCoinsEnabled || coinConfig.coinValueInr <= 0.0 || afterCouponRupees <= 0 || availableCoins <= 0) {
            return 0
        }
        val maxRupeeCap = afterCouponRupees * (coinConfig.maxRedeemPercent.coerceIn(0.0, 100.0) / 100.0)
        val coinsAllowedByPercent = floor(maxRupeeCap / coinConfig.coinValueInr).toInt().coerceAtLeast(0)
        return min(availableCoins, coinsAllowedByPercent)
    }

    fun coinDiscountRupees(coinsRedeemed: Int): Int {
        if (!coinConfig.isCoinsEnabled || coinsRedeemed <= 0 || coinConfig.coinValueInr <= 0.0) return 0
        return (coinsRedeemed * coinConfig.coinValueInr).roundToInt().coerceAtLeast(0)
    }

    fun coinsToEarn(finalPayableTotal: Int): Int {
        if (!coinConfig.isCoinsEnabled || finalPayableTotal <= 0 || coinConfig.coinValueInr <= 0.0 || coinConfig.earnRatePercent <= 0.0) {
            return 0
        }
        val earnInr = finalPayableTotal * (coinConfig.earnRatePercent.coerceIn(0.0, 50.0) / 100.0)
        return (earnInr / coinConfig.coinValueInr).roundToInt().coerceIn(0, 2000)
    }

    companion object {
        const val DEFAULT_DISCOUNT_PERCENT = 12.0
        const val DEFAULT_PETI_EXTRA_DISCOUNT_PERCENT = 3.0

        fun formatPercent(value: Double): String {
            val clamped = value.coerceIn(0.0, 99.9)
            return if (clamped % 1.0 == 0.0) {
                clamped.toInt().toString()
            } else {
                String.format(java.util.Locale.US, "%.1f", clamped)
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class EmiTenurePlan(
    val months: Int,
    val monthlyAmount: Int,
    val annualInterestPercent: Double,
    val totalPayable: Int,
    val isNoCost: Boolean
)

/**
 * Live EMI & payment gateway configuration synced from Firebase RTDB `/storeConfig/payments`.
 * Read-only on client; actual EMI verification & card entry runs strictly inside the gateway checkout session.
 */
@JsonClass(generateAdapter = true)
data class StorePaymentsConfig(
    @param:Json(name = "isEmiEnabled") val isEmiEnabled: Boolean = true,
    @param:Json(name = "minEmiOrderAmount") val minEmiOrderAmount: Int = DEFAULT_MIN_EMI_ORDER_AMOUNT,
    @param:Json(name = "supportedEmiProviders") val supportedEmiProviders: List<String> = DEFAULT_EMI_PROVIDERS
) {
    val sanitizedMinEmiOrderAmount: Int
        get() = minEmiOrderAmount.coerceAtLeast(1)

    val effectiveProviders: List<String>
        get() = supportedEmiProviders.filter { it.isNotBlank() }.ifEmpty { DEFAULT_EMI_PROVIDERS }

    fun isEligibleForEmi(amountInr: Int): Boolean =
        isEmiEnabled && amountInr >= sanitizedMinEmiOrderAmount

    fun emiStartingPerMonth(amountInr: Int): Int {
        if (amountInr <= 0) return 0
        return (amountInr.toDouble() / 12.0).roundToInt().coerceAtLeast(1)
    }

    fun buildEmiPlans(amountInr: Int): List<EmiTenurePlan> {
        if (amountInr <= 0) return emptyList()
        val tenures = listOf(
            3 to 0.0,
            6 to 0.0,
            9 to 12.0,
            12 to 14.0
        )
        return tenures.map { (months, annualRate) ->
            val interestFactor = 1.0 + (annualRate / 100.0) * (months.toDouble() / 12.0) * 0.55
            val totalPayable = if (annualRate == 0.0) amountInr else (amountInr * interestFactor).roundToInt()
            val monthly = (totalPayable.toDouble() / months.toDouble()).roundToInt().coerceAtLeast(1)
            EmiTenurePlan(
                months = months,
                monthlyAmount = monthly,
                annualInterestPercent = annualRate,
                totalPayable = monthly * months,
                isNoCost = annualRate == 0.0
            )
        }
    }

    companion object {
        const val DEFAULT_MIN_EMI_ORDER_AMOUNT = 3000
        val DEFAULT_EMI_PROVIDERS = listOf(
            "Bajaj Finserv EMI Card",
            "HDFC Bank",
            "SBI Card",
            "ICICI Bank",
            "Axis Bank",
            "Kotak Bank"
        )
    }
}

/**
 * Live Delivery Zone, Pincode & Store Pickup rules synced from Firebase RTDB `/storeConfig/delivery`.
 */
@JsonClass(generateAdapter = true)
data class StoreDeliveryConfig(
    @param:Json(name = "localPincodes") val localPincodes: List<String> = DEFAULT_LOCAL_PINCODES,
    @param:Json(name = "freeDeliveryMinOrder") val freeDeliveryMinOrder: Int = DEFAULT_FREE_DELIVERY_MIN_ORDER,
    @param:Json(name = "localDeliveryFee") val localDeliveryFee: Int = DEFAULT_LOCAL_DELIVERY_FEE,
    @param:Json(name = "standardDeliveryFee") val standardDeliveryFee: Int = DEFAULT_STANDARD_DELIVERY_FEE,
    @param:Json(name = "expressDeliveryFee") val expressDeliveryFee: Int = DEFAULT_EXPRESS_DELIVERY_FEE,
    @param:Json(name = "storePickupEnabled") val storePickupEnabled: Boolean = true,
    @param:Json(name = "pickupAddress") val pickupAddress: String = StoreProfile.LOCATION
) {
    val sanitizedLocalPincodes: List<String>
        get() = localPincodes.map { it.filter { ch -> ch.isDigit() } }.filter { it.length == 6 }.ifEmpty { DEFAULT_LOCAL_PINCODES }

    fun isLocalPincode(pinCode: String): Boolean {
        val clean = pinCode.filter { it.isDigit() }
        if (clean.length != 6) return false
        return sanitizedLocalPincodes.any { it == clean }
    }

    fun computeDeliveryFee(
        deliveryOption: DeliveryOption,
        pinCode: String,
        subtotalAfterDiscounts: Int,
        freeExpressPromo: Boolean = false,
        freeLocalPromo: Boolean = false
    ): Int {
        return when (deliveryOption) {
            DeliveryOption.STORE_PICKUP -> 0
            DeliveryOption.EXPRESS_15_MIN -> if (freeExpressPromo) 0 else expressDeliveryFee.coerceIn(0, 200)
            DeliveryOption.LOCAL_2_HR,
            DeliveryOption.HOME_DELIVERY,
            DeliveryOption.PIND_SCHEDULED -> {
                if (subtotalAfterDiscounts >= freeDeliveryMinOrder.coerceAtLeast(0)) {
                    0
                } else if (freeLocalPromo && isLocalPincode(pinCode.ifBlank { StoreProfile.DEFAULT_PIN_CODE })) {
                    0
                } else if (isLocalPincode(pinCode.ifBlank { StoreProfile.DEFAULT_PIN_CODE })) {
                    localDeliveryFee.coerceIn(0, 200)
                } else {
                    standardDeliveryFee.coerceIn(0, 200)
                }
            }
        }
    }

    companion object {
        const val DEFAULT_FREE_DELIVERY_MIN_ORDER = 499
        const val DEFAULT_LOCAL_DELIVERY_FEE = 25
        const val DEFAULT_STANDARD_DELIVERY_FEE = 60
        const val DEFAULT_EXPRESS_DELIVERY_FEE = 25
        val DEFAULT_LOCAL_PINCODES = listOf("148020", "148019", "148023", "148022", "148021")
    }
}

/**
 * Live website-controlled promo banner / video configuration synced from `/storeConfig/heroPromo`.
 */
@JsonClass(generateAdapter = true)
data class StoreHeroPromoConfig(
    @param:Json(name = "isActive") val isActive: Boolean = false,
    @param:Json(name = "badgeText") val badgeText: String = "FEATURED PROMO",
    @param:Json(name = "title") val title: String = "",
    @param:Json(name = "subtitle") val subtitle: String = "",
    @param:Json(name = "mediaType") val mediaType: String = "BANNER",
    @param:Json(name = "bannerImageUrl") val bannerImageUrl: String = "",
    @param:Json(name = "videoUrl") val videoUrl: String = "",
    @param:Json(name = "ctaText") val ctaText: String = "Shop Promo",
    @param:Json(name = "ctaCategory") val ctaCategory: String = "All Products"
)

/**
 * Dynamic marketplace category loaded from Firebase RTDB `/categories`.
 * Supports any A-to-Z retail category added remotely without app updates.
 */
@JsonClass(generateAdapter = true)
data class MarketplaceCategory(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "punjabiName") val punjabiName: String = "",
    @param:Json(name = "hindiName") val hindiName: String = "",
    @param:Json(name = "department") val department: String = "",
    @param:Json(name = "imageUrl") val imageUrl: String = "",
    @param:Json(name = "discountPercent") val discountPercent: Double? = null,
    @param:Json(name = "returnPolicy") val returnPolicy: String = "",
    @param:Json(name = "sortOrder") val sortOrder: Int = 0,
    @param:Json(name = "isActive") val isActive: Boolean = true
) {
    fun displayName(isPunjabi: Boolean): String =
        if (isPunjabi && punjabiName.isNotBlank()) punjabiName else name

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.PUNJABI -> punjabiName.ifBlank { name }
        AppLanguage.HINDI -> hindiName.ifBlank { name }
        AppLanguage.ENGLISH -> name
    }

    fun effectiveDepartment(): String = department.ifBlank { name }
}

/**
 * Noon-style price range buckets for rapid catalog filtering.
 */
enum class PriceFilterRange(
    val labelEn: String,
    val labelPa: String,
    val labelHi: String,
    val minInclusive: Int,
    val maxInclusive: Int?
) {
    ALL("All Prices", "ਸਾਰੀਆਂ ਕੀਮਤਾਂ", "सभी कीमतें", 0, null),
    UNDER_100("Under ₹100", "₹100 ਤੋਂ ਘੱਟ", "₹100 से कम", 0, 99),
    FROM_100_TO_499("₹100 – ₹499", "₹100 – ₹499", "₹100 – ₹499", 100, 499),
    FROM_500_TO_1999("₹500 – ₹1,999", "₹500 – ₹1,999", "₹500 – ₹1,999", 500, 1999),
    ABOVE_2000("₹2,000+", "₹2,000+", "₹2,000+", 2000, null);

    fun labelFor(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> labelEn
        AppLanguage.PUNJABI -> labelPa
        AppLanguage.HINDI -> labelHi
    }

    fun matches(sellingPrice: Int): Boolean =
        sellingPrice >= minInclusive && (maxInclusive == null || sellingPrice <= maxInclusive)
}
