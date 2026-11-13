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
    }

    @Test
    fun testOwnerSecureLedgerPrivateConstants() {
        assertEquals("Amrit Pal Singh Uppal", com.example.data.model.OwnerSecureLedger.OWNER_LEGAL_NAME)
        assertEquals("singhpaluppal@gmail.com", com.example.data.model.OwnerSecureLedger.OWNER_ADMIN_EMAIL)
        assertEquals("03AHPPU6185N1ZF", com.example.data.model.OwnerSecureLedger.GSTIN)
        assertEquals("148020", com.example.data.model.OwnerSecureLedger.SUPER_ADMIN_PIN)
        assertEquals("8847370905@upi", com.example.data.model.OwnerSecureLedger.CENTRAL_MERCHANT_VPA)
    }
}
