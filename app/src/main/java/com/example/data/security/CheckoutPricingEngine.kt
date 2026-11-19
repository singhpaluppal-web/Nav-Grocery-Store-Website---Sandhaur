package com.example.data.security

import com.example.data.model.CartItem
import com.example.data.model.DeliveryOption
import com.example.data.model.Product
import com.example.data.model.StoreDeliveryConfig
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StoreProfile
import com.example.data.model.UserAccount
import kotlin.math.min
import kotlin.math.roundToInt

data class VerifiedCheckoutSummary(
    val totalMrp: Int,
    val wholesaleDiscount: Int,
    val petiDiscountRupees: Int,
    val subtotalAfterWholesale: Int,
    val spinCouponDiscountRupees: Int,
    val spinCouponCode: String,
    val coinsRedeemed: Int,
    val coinDiscountRupees: Int,
    val deliveryFee: Int,
    val freeExpressApplied: Boolean,
    val walletDeductedRupees: Int,
    val finalPayableTotal: Int,
    val netGatewayPayable: Int,
    val coinsToEarn: Int,
    val effectiveStoreDiscountPercent: Double,
    val integrityDigest: String
)

object CheckoutPricingEngine {
    const val SUBMISSION_COOLDOWN_MS = 5_000L

    fun verifyAndCompute(
        cartItems: List<CartItem>,
        masterCatalog: List<Product>,
        deliveryOption: DeliveryOption,
        userAccount: UserAccount?,
        applySpinCoupon: Boolean,
        requestedCoinsToRedeem: Int,
        applyWalletBalance: Boolean = false,
        offersConfig: StoreOffersConfig = StoreOffersConfig(),
        categoryDiscounts: Map<String, Double> = emptyMap(),
        deliveryConfig: StoreDeliveryConfig = StoreDeliveryConfig(),
        pinCode: String = StoreProfile.DEFAULT_PIN_CODE
    ): Result<VerifiedCheckoutSummary> {
        if (cartItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cart cannot be empty"))
        }

        val catalogById = masterCatalog.associateBy { it.id }
        var sumMrp = 0
        var sumBaseWholesale = 0
        var sumPetiDiscount = 0

        for (item in cartItems) {
            if (item.quantity <= 0 || item.quantity > 50) {
                return Result.failure(IllegalArgumentException("Invalid quantity for ${item.product.id}"))
            }
            val authoritative = catalogById[item.product.id] ?: item.product
            if (authoritative.stock <= 0) {
                return Result.failure(IllegalArgumentException("${authoritative.name} is currently out of stock."))
            }
            val isCarton = item.isPetiCarton && authoritative.isBulkEligible
            val unitsPerPack = if (isCarton) authoritative.petiUnits.coerceAtLeast(1) else 1
            val totalRequestedUnits = item.quantity * unitsPerPack
            if (item.quantity > authoritative.stock && totalRequestedUnits > authoritative.stock) {
                return Result.failure(
                    IllegalArgumentException("Only ${authoritative.stock} unit(s) of ${authoritative.name} left in stock.")
                )
            }
            if (authoritative.mrp <= 0) {
                return Result.failure(IllegalArgumentException("Invalid MRP for ${authoritative.id}"))
            }
            val catPct = categoryDiscounts[authoritative.category]
            val unitSale = authoritative.sellingPrice(catPct, offersConfig)
            val lineMrp = authoritative.mrp * unitsPerPack * item.quantity
            val lineBaseWholesale = unitSale * unitsPerPack * item.quantity

            sumMrp += lineMrp
            sumBaseWholesale += lineBaseWholesale

            if (isCarton && unitsPerPack > 1) {
                val petiPackPrice = (unitSale * unitsPerPack * offersConfig.petiSlabFactor).roundToInt()
                val linePetiTotal = petiPackPrice * item.quantity
                sumPetiDiscount += (lineBaseWholesale - linePetiTotal).coerceAtLeast(0)
            }
        }

        val wholesaleDiscount = (sumMrp - sumBaseWholesale).coerceAtLeast(0)
        val afterPetiSubtotal = (sumBaseWholesale - sumPetiDiscount).coerceAtLeast(0)

        var couponDiscount = 0
        var appliedCode = ""
        var freeExpress = false
        var freeLocal = false

        if (applySpinCoupon && userAccount != null && userAccount.hasValidSpinCoupon()) {
            appliedCode = userAccount.activeSpinCouponCode
            if (userAccount.activeSpinFreeExpress) {
                freeExpress = true
            }
            if (userAccount.activeSpinFreeLocal) {
                freeLocal = true
            }
            if (userAccount.activeSpinCouponDiscountRupees > 0L) {
                couponDiscount = min(
                    userAccount.activeSpinCouponDiscountRupees.toInt(),
                    afterPetiSubtotal
                ).coerceAtLeast(0)
            }
        }

        val afterCoupon = (afterPetiSubtotal - couponDiscount).coerceAtLeast(0)
        val availCoins = (userAccount?.navCoins ?: 0L).toInt().coerceAtLeast(0)
        val maxRedeemable = offersConfig.maxRedeemableCoins(afterCoupon, availCoins)
        val coinsRedeemed = requestedCoinsToRedeem.coerceIn(0, maxRedeemable)
        val coinDiscountRupees = min(afterCoupon, offersConfig.coinDiscountRupees(coinsRedeemed))
        val subtotalAfterAllDiscounts = (afterCoupon - coinDiscountRupees).coerceAtLeast(0)

        val deliveryFee = deliveryConfig.computeDeliveryFee(
            deliveryOption = deliveryOption,
            pinCode = pinCode,
            subtotalAfterDiscounts = subtotalAfterAllDiscounts,
            freeExpressPromo = freeExpress,
            freeLocalPromo = freeLocal
        )

        val finalPayableTotal = (subtotalAfterAllDiscounts + deliveryFee).coerceAtLeast(0)
        val walletAvail = (userAccount?.walletBalanceRupees ?: 0L).toInt().coerceAtLeast(0)
        val walletDeducted = if (applyWalletBalance) min(walletAvail, finalPayableTotal) else 0
        val netGatewayPayable = (finalPayableTotal - walletDeducted).coerceAtLeast(0)
        val coinsEarned = offersConfig.coinsToEarn(finalPayableTotal)
        val payloadToHash = "$sumMrp|$wholesaleDiscount|$sumPetiDiscount|$couponDiscount|$coinsRedeemed|$coinDiscountRupees|$walletDeducted|$deliveryFee|$finalPayableTotal|${offersConfig.sanitizedDiscountPercent}|${pinCode.trim()}|${cartItems.joinToString(",") { "${it.product.id}:${it.quantity}:${it.isPetiCarton}" }}"
        val digest = InputSanitizer.sha256Hex(payloadToHash)

        return Result.success(
            VerifiedCheckoutSummary(
                totalMrp = sumMrp,
                wholesaleDiscount = wholesaleDiscount,
                petiDiscountRupees = sumPetiDiscount,
                subtotalAfterWholesale = afterPetiSubtotal,
                spinCouponDiscountRupees = couponDiscount,
                spinCouponCode = appliedCode,
                coinsRedeemed = coinsRedeemed,
                coinDiscountRupees = coinDiscountRupees,
                deliveryFee = deliveryFee,
                freeExpressApplied = freeExpress,
                walletDeductedRupees = walletDeducted,
                finalPayableTotal = finalPayableTotal,
                netGatewayPayable = netGatewayPayable,
                coinsToEarn = coinsEarned,
                effectiveStoreDiscountPercent = offersConfig.sanitizedDiscountPercent,
                integrityDigest = digest
            )
        )
    }

    fun canSubmitNow(lastSubmittedMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean =
        lastSubmittedMs <= 0L || (nowMs - lastSubmittedMs) >= SUBMISSION_COOLDOWN_MS
}
