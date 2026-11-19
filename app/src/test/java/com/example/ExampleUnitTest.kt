package com.example

import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class ExampleUnitTest {

    @Test
    fun testWholesalePricingRule_flat12PercentOff() {
        val mrp = 120
        val expectedSale = (mrp * 0.88).roundToInt() // 106
        val product = Product(
            id = "NAV001",
            name = "Test Basmati Rice 1kg",
            mrp = mrp,
            sale = expectedSale
        )

        assertEquals(106, product.wholesalePrice)
        assertEquals(14, product.savingsAmount)
        assertEquals(12, product.savingsPercent)
    }

    @Test
    fun testWholesalePricingRule_autoComputesIfZero() {
        val product = Product(
            id = "NAV003",
            name = "Tata Urad Dal 1kg",
            mrp = 185,
            sale = 0
        )
        // 185 * 0.88 = 162.8 -> 163
        assertEquals(163, product.wholesalePrice)
        assertEquals(22, product.savingsAmount)
    }

    @Test
    fun testColdChainDetection() {
        val frozenItem = Product(
            id = "NAV002",
            name = "Premium Frozen Green Peas 500g",
            category = "Frozen Foods",
            mrp = 120,
            sale = 106
        )
        val regularItem = Product(
            id = "NAV004",
            name = "Black Pepper 100g",
            category = "Spices",
            mrp = 120,
            sale = 106
        )

        assertTrue(frozenItem.isColdChain)
        assertFalse(regularItem.isColdChain)
    }

    @Test
    fun testCartItemCalculations() {
        val product = Product(
            id = "NAV001",
            name = "Maharaja Spices 100g",
            mrp = 100,
            sale = 88
        )
        val cartItem = CartItem(product = product, quantity = 3)

        assertEquals(300, cartItem.totalMrp)
        assertEquals(264, cartItem.totalSalePrice)
        assertEquals(36, cartItem.totalSavings)
    }

    @Test
    fun testProductDescriptionAndPrices() {
        val customDesc = "Aromatic stone ground whole wheat flour milled from MP Sharbati wheat."
        val product = Product(
            id = "NAV005",
            name = "Aashirvaad Sharbati Atta 5kg",
            description = customDesc,
            category = "Atta & Dals",
            mrp = 350,
            sale = 308
        )

        assertEquals(customDesc, product.description)
        assertEquals(customDesc, product.displayDescription)
        assertEquals(308, product.wholesalePrice)
        assertEquals(42, product.savingsAmount)
    }

    @Test
    fun testProductDefaultDescriptionFallback() {
        val productWithoutDesc = Product(
            id = "NAV006",
            name = "Verka Pure Desi Ghee 1L",
            description = "",
            category = "Desi Ghee & Oils",
            mrp = 650
        )

        assertTrue(productWithoutDesc.displayDescription.contains("pure desi ghee"))
        assertEquals(572, productWithoutDesc.wholesalePrice) // 650 * 0.88 = 572
    }

    @Test
    fun testStoreProfilePublicShieldConstants() {
        assertEquals("NavKart", StoreProfile.APP_NAME)
        assertEquals("Nav Grocery Store", StoreProfile.STORE_NAME)
        assertEquals("+91 8847370905", StoreProfile.HELPLINE_PHONE)
        assertEquals("@nav_grocery_store", StoreProfile.INSTAGRAM_HANDLE)
        assertEquals("https://www.navgrocerystore.com", StoreProfile.WEB_URL)
        assertEquals("8847370905@upi", StoreProfile.MERCHANT_UPI_VPA)
    }

    @Test
    fun testCheckoutPricingEngine_antiTamperAndRewards() {
        val authoritative = Product(id = "NAV010", name = "Aashirvaad Atta 10kg", mrp = 500, sale = 440)
        val tamperedClient = authoritative.copy(mrp = 10, sale = 1)
        val cart = listOf(CartItem(product = tamperedClient, quantity = 2))
        val account = com.example.data.model.UserAccount(
            uid = "u1",
            navCoins = 120L,
            activeSpinCouponCode = "SPIN20",
            activeSpinCouponDiscountRupees = 20L,
            activeSpinFreeExpress = false,
            activeSpinUsed = false
        )

        val result = com.example.data.security.CheckoutPricingEngine.verifyAndCompute(
            cartItems = cart,
            masterCatalog = listOf(authoritative),
            deliveryOption = com.example.data.model.DeliveryOption.EXPRESS_15_MIN,
            userAccount = account,
            applySpinCoupon = true,
            requestedCoinsToRedeem = 500
        )

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(1000, summary.totalMrp)
        assertEquals(880, summary.subtotalAfterWholesale)
        assertEquals(20, summary.spinCouponDiscountRupees)
        // Avail coins = 120, cap = 200 & 25% of 860 (215), so max redeemable = 120
        assertEquals(120, summary.coinsRedeemed)
        assertEquals(25, summary.deliveryFee)
        assertEquals(880 - 20 - 120 + 25, summary.finalPayableTotal)
    }

    @Test
    fun testInputSanitizerAndSubmissionCooldown() {
        val dirty = "<script>alert(1)</script>  Harpreet   Singh  "
        assertEquals("alert(1) Harpreet Singh", com.example.data.security.InputSanitizer.sanitizeName(dirty))
        assertEquals("9876543210", com.example.data.security.InputSanitizer.sanitizePhone10("+91 98765-43210"))
        assertTrue(com.example.data.security.InputSanitizer.isValidIndianMobile("9876543210"))
        assertFalse(com.example.data.security.InputSanitizer.isValidIndianMobile("1234567890"))
        assertTrue(com.example.data.security.InputSanitizer.isValidPin6("148020"))
        assertFalse(com.example.data.security.CheckoutPricingEngine.canSubmitNow(10_000L, 12_000L))
        assertTrue(com.example.data.security.CheckoutPricingEngine.canSubmitNow(10_000L, 16_000L))
    }

    @Test
    fun testUserAccount24hSpinCooldown() {
        val now = 200_000_000L
        val freshAccount = com.example.data.model.UserAccount(uid = "u1", lastSpinAtMs = 0L)
        assertTrue(freshAccount.canSpinNow(now))

        val spunRecently = freshAccount.copy(lastSpinAtMs = now - 3_600_000L)
        assertFalse(spunRecently.canSpinNow(now))

        val spunYesterday = freshAccount.copy(lastSpinAtMs = now - 86_400_000L)
        assertTrue(spunYesterday.canSpinNow(now))
    }
}
