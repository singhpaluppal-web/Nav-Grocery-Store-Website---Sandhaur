package com.example

import com.example.data.model.CartItem
import com.example.data.model.CoinRewardConfig
import com.example.data.model.DeliveryOption
import com.example.data.model.Product
import com.example.data.model.StoreOffersConfig
import com.example.data.model.UserAccount
import com.example.data.security.CheckoutPricingEngine
import com.example.data.security.InputSanitizer
import com.example.ui.components.WebShieldValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class SecurityAndCheckoutVerificationTest {

    private fun sampleGrocery(
        id: String = "NAV001",
        name: String = "Aashirvaad Shudh Chakki Atta 5kg",
        mrp: Int = 300
    ) = Product(
        id = id,
        name = name,
        department = "Packaged Groceries",
        category = "Atta & Dals",
        mrp = mrp
    )

    private fun sampleTshirt(
        id: String = "NAV560",
        name: String = "Cotton Polo T-Shirt",
        mrp: Int = 1000
    ) = Product(
        id = id,
        name = name,
        department = "Clothing and Apparel",
        category = "Clothing",
        mrp = mrp
    )

    @Test
    fun `enforces flat 12 percent wholesale off MRP and Peti extra 3 percent slab`() {
        val atta = sampleGrocery(mrp = 300)
        // Single retail unit: round(300 * 0.88) = 264
        assertEquals(264, atta.wholesalePrice)
        // 5kg product -> petiUnits = 4
        assertTrue(atta.isBulkEligible)
        assertEquals(4, atta.petiUnits)
        // Peti price: round(264 * 4 * 0.97) = round(1024.32) = 1024 vs 1056 base wholesale -> 32 rebate
        val expectedPetiTotal = (264 * 4 * 0.97).roundToInt()
        assertEquals(expectedPetiTotal, atta.petiTotalPrice)

        // Apparel should not be Peti bulk eligible even if client sends isPetiCarton=true
        val shirt = sampleTshirt(mrp = 1000)
        assertFalse(shirt.isBulkEligible)

        val tamperedCart = listOf(
            // Deliberately pass tampered zero MRP / sale price on CartItem product, verify engine uses catalog MRP
            CartItem(product = atta.copy(mrp = 1, sale = 1), quantity = 1, isPetiCarton = true, petiUnits = 4),
            CartItem(product = shirt.copy(mrp = 10, sale = 1), quantity = 1, isPetiCarton = true, petiUnits = 12)
        )

        val summary = CheckoutPricingEngine.verifyAndCompute(
            cartItems = tamperedCart,
            masterCatalog = listOf(atta, shirt),
            deliveryOption = DeliveryOption.PIND_SCHEDULED,
            userAccount = null,
            applySpinCoupon = false,
            requestedCoinsToRedeem = 0,
            applyWalletBalance = false
        ).getOrThrow()

        val expectedMrp = (300 * 4) + 1000
        val expectedBaseWholesale = (264 * 4) + 880
        val expectedPetiDiscount = (264 * 4) - expectedPetiTotal

        assertEquals(expectedMrp, summary.totalMrp)
        assertEquals(expectedMrp - expectedBaseWholesale, summary.wholesaleDiscount)
        assertEquals(expectedPetiDiscount, summary.petiDiscountRupees)
        assertEquals(0, summary.deliveryFee)
        assertEquals(expectedBaseWholesale - expectedPetiDiscount, summary.finalPayableTotal)
    }

    @Test
    fun `express delivery charges 25 rupees unless spin free express coupon applies`() {
        val grocery = sampleGrocery(mrp = 200) // wholesale 176
        val cart = listOf(CartItem(product = grocery, quantity = 1))

        val normalExpress = CheckoutPricingEngine.verifyAndCompute(
            cartItems = cart,
            masterCatalog = listOf(grocery),
            deliveryOption = DeliveryOption.EXPRESS_15_MIN,
            userAccount = UserAccount(uid = "u1", navCoins = 0L),
            applySpinCoupon = false,
            requestedCoinsToRedeem = 0
        ).getOrThrow()
        assertEquals(25, normalExpress.deliveryFee)
        assertEquals(176 + 25, normalExpress.finalPayableTotal)

        val spinExpressAccount = UserAccount(
            uid = "u1",
            activeSpinCouponCode = "SPINEXP",
            activeSpinFreeExpress = true,
            activeSpinUsed = false
        )
        val freeExpress = CheckoutPricingEngine.verifyAndCompute(
            cartItems = cart,
            masterCatalog = listOf(grocery),
            deliveryOption = DeliveryOption.EXPRESS_15_MIN,
            userAccount = spinExpressAccount,
            applySpinCoupon = true,
            requestedCoinsToRedeem = 0
        ).getOrThrow()
        assertEquals(0, freeExpress.deliveryFee)
        assertTrue(freeExpress.freeExpressApplied)
        assertEquals(176, freeExpress.finalPayableTotal)
    }

    @Test
    fun `caps coin redemption by dynamic maxRedeemPercent and deducts family wallet`() {
        val atta = sampleGrocery(mrp = 1000) // wholesale = 880
        val cart = listOf(CartItem(product = atta, quantity = 1))
        val account = UserAccount(
            uid = "uid123456",
            navCoins = 500L,
            walletBalanceRupees = 300L
        )

        val summary = CheckoutPricingEngine.verifyAndCompute(
            cartItems = cart,
            masterCatalog = listOf(atta),
            deliveryOption = DeliveryOption.LOCAL_2_HR,
            userAccount = account,
            applySpinCoupon = false,
            requestedCoinsToRedeem = 500, // cap clamps to dynamic 25% of 880 = 220 coins
            applyWalletBalance = true
        ).getOrThrow()

        assertEquals(220, summary.coinsRedeemed)
        assertEquals(660, summary.finalPayableTotal)
        assertEquals(300, summary.walletDeductedRupees)
        assertEquals(360, summary.netGatewayPayable)
    }

    @Test
    fun `referral code formats PIND prefix from uid or saved code`() {
        val defaultUser = UserAccount(uid = "abc12345xyz")
        assertEquals("PIND-ABC123", defaultUser.effectiveReferralCode())

        val customCodeUser = defaultUser.copy(referralCode = "PIND-SANDHAUR")
        assertEquals("PIND-SANDHAUR", customCodeUser.effectiveReferralCode())
    }

    @Test
    fun `supports remote StoreOffersConfig and category overrides dynamically`() {
        val grocery = sampleGrocery(mrp = 200)
        val liveConfig = StoreOffersConfig(
            discountPercent = 15.0,
            offerBannerText = "Diwali Mega Sale 15% OFF",
            isOfferActive = true,
            petiExtraDiscountPercent = 5.0,
            coinConfig = CoinRewardConfig(
                isCoinsEnabled = true,
                earnRatePercent = 6.0,
                coinValueInr = 2.0,
                maxRedeemPercent = 20.0
            )
        )
        // Product fallback rate -> 15% => round(200 * 0.85) = 170
        assertEquals(170, grocery.sellingPrice(null, liveConfig))
        // Category override -> 20% => round(200 * 0.80) = 160
        assertEquals(160, grocery.sellingPrice(20.0, liveConfig))

        val cart = listOf(CartItem(product = grocery, quantity = 10)) // 160 * 10 = 1600
        val account = UserAccount(uid = "u_dyn", navCoins = 500L)
        val summary = CheckoutPricingEngine.verifyAndCompute(
            cartItems = cart,
            masterCatalog = listOf(grocery),
            deliveryOption = DeliveryOption.LOCAL_2_HR,
            userAccount = account,
            applySpinCoupon = false,
            requestedCoinsToRedeem = 300,
            applyWalletBalance = false,
            offersConfig = liveConfig,
            categoryDiscounts = mapOf("Atta & Dals" to 20.0)
        ).getOrThrow()

        // Max 20% of 1600 = ₹320; at ₹2/coin -> max 160 coins = ₹320 off
        assertEquals(160, summary.coinsRedeemed)
        assertEquals(320, summary.coinDiscountRupees)
        assertEquals(1280, summary.finalPayableTotal)

        // When admin disables coins remotely, coin redemption drops cleanly to 0
        val pausedCoinsSummary = CheckoutPricingEngine.verifyAndCompute(
            cartItems = cart,
            masterCatalog = listOf(grocery),
            deliveryOption = DeliveryOption.LOCAL_2_HR,
            userAccount = account,
            applySpinCoupon = false,
            requestedCoinsToRedeem = 100,
            applyWalletBalance = false,
            offersConfig = liveConfig.copy(coinConfig = liveConfig.coinConfig.copy(isCoinsEnabled = false)),
            categoryDiscounts = mapOf("Atta & Dals" to 20.0)
        ).getOrThrow()
        assertEquals(0, pausedCoinsSummary.coinsRedeemed)
        assertEquals(0, pausedCoinsSummary.coinDiscountRupees)
        assertEquals(0, pausedCoinsSummary.coinsToEarn)
    }

    @Test
    fun `upgrades gallery additionalImages to HTTPS and enforces WebShieldValidator`() {
        val gadget = Product(
            id = "ELEC01",
            name = "Boat Earbuds",
            department = "Electronics and Mobiles",
            category = "Electronics",
            mrp = 2000,
            image = "https://www.navgrocerystore.com/assets/elec01.jpg",
            additionalImages = "http://www.navgrocerystore.com/assets/elec01_side.jpg, assets/elec01_box.jpg"
        )
        val gallery = gadget.galleryImageUrls
        assertEquals(3, gallery.size)
        assertTrue(gallery.all { it.startsWith("https://") })

        assertTrue(WebShieldValidator.isTrustedHttpsUrl("https://www.navgrocerystore.com/privacy"))
        assertTrue(WebShieldValidator.isTrustedHttpsUrl("https://checkout.razorpay.com/v1/checkout.js"))
        assertFalse(WebShieldValidator.isTrustedHttpsUrl("http://www.navgrocerystore.com"))
        assertFalse(WebShieldValidator.isTrustedHttpsUrl("javascript:alert(1)"))
        assertFalse(WebShieldValidator.isTrustedHttpsUrl("file:///etc/passwd"))
        assertFalse(WebShieldValidator.isTrustedHttpsUrl("https://evil-phishing-site.com"))
    }

    @Test
    fun `enforces SHA256 determinism, 5 second checkout cooldown, and input sanitization`() {
        val atta = sampleGrocery(mrp = 500)
        val cart = listOf(CartItem(product = atta, quantity = 2))
        val first = CheckoutPricingEngine.verifyAndCompute(
            cartItems = cart,
            masterCatalog = listOf(atta),
            deliveryOption = DeliveryOption.STORE_PICKUP,
            userAccount = null,
            applySpinCoupon = false,
            requestedCoinsToRedeem = 0
        ).getOrThrow()
        val second = CheckoutPricingEngine.verifyAndCompute(
            cartItems = cart,
            masterCatalog = listOf(atta),
            deliveryOption = DeliveryOption.STORE_PICKUP,
            userAccount = null,
            applySpinCoupon = false,
            requestedCoinsToRedeem = 0
        ).getOrThrow()

        assertEquals(64, first.integrityDigest.length)
        assertEquals(first.integrityDigest, second.integrityDigest)

        assertFalse(CheckoutPricingEngine.canSubmitNow(lastSubmittedMs = 10_000L, nowMs = 13_000L))
        assertTrue(CheckoutPricingEngine.canSubmitNow(lastSubmittedMs = 10_000L, nowMs = 15_000L))

        assertEquals("Gurpreet Singh", InputSanitizer.sanitizeName("<b>Gurpreet</b>   <script/>Singh"))
        assertEquals("8847370905", InputSanitizer.sanitizePhone10("+91 88473-70905"))
        assertTrue(InputSanitizer.isValidIndianMobile("8847370905"))
        assertFalse(InputSanitizer.isValidIndianMobile("1234567890"))
        assertTrue(InputSanitizer.isValidPin6("148020"))
        assertNull(
            InputSanitizer.validateAddressFields(
                fullName = "Harjinder Kaur",
                mobile = "9915752905",
                street = "Main Bazaar Near Gurdwara Sahib",
                villageOrCity = "Sandhaur, Malerkotla",
                pinCode = "148020"
            )
        )
        assertNotNull(
            InputSanitizer.validateAddressFields(
                fullName = "A",
                mobile = "9915752905",
                street = "Main Bazaar",
                villageOrCity = "Sandhaur",
                pinCode = "148020"
            )
        )
    }
}
