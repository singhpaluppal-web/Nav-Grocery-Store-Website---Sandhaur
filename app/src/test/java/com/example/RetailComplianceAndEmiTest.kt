package com.example

import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.DeliveryOption
import com.example.data.model.FirestoreOrderRecord
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StoreDeliveryConfig
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StorePaymentsConfig
import com.example.data.model.StoreProfile
import com.example.data.model.UserAccount
import com.example.data.security.CheckoutPricingEngine
import com.example.data.security.OrderStatusTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetailComplianceAndEmiTest {

    private fun electronicsItem(stock: Int = 5, mrp: Int = 5000) = Product(
        id = "ELEC_901",
        name = "Samsung Microwave Oven 28L",
        department = "Electronics and Mobiles",
        category = "Electronics",
        mrp = mrp,
        stock = stock,
        warranty = "1 Year Samsung India Warranty"
    )

    private fun apparelItem(stock: Int = 8, mrp: Int = 1200) = Product(
        id = "APRL_202",
        name = "Punjabi Cotton Kurta Pajama Set",
        department = "Clothing and Apparel",
        category = "Clothing",
        mrp = mrp,
        stock = stock
    )

    private fun coldChainItem(stock: Int = 12, mrp: Int = 250) = Product(
        id = "FRZ_301",
        name = "McCain Frozen French Fries 750g",
        department = "Cold Chain Frozen Foods",
        category = "Frozen Foods",
        mrp = mrp,
        stock = stock
    )

    private fun sealedGroceryItem(stock: Int = 10, mrp: Int = 300) = Product(
        id = "GROC_101",
        name = "Fortune Sunlite Refined Oil 1L",
        department = "Packaged Groceries",
        category = "Desi Ghee & Oils",
        mrp = mrp,
        stock = stock
    )

    @Test
    fun `StorePaymentsConfig gates EMI at minEmiOrderAmount, computes monthly installment plans, and exposes Bajaj & bank providers`() {
        val defaultConfig = StorePaymentsConfig()
        assertTrue(defaultConfig.isEmiEnabled)
        assertEquals(3000, defaultConfig.sanitizedMinEmiOrderAmount)
        assertTrue(defaultConfig.effectiveProviders.contains("Bajaj Finserv EMI Card"))
        assertTrue(defaultConfig.effectiveProviders.contains("HDFC Bank"))
        assertTrue(defaultConfig.effectiveProviders.contains("SBI Card"))
        assertTrue(defaultConfig.effectiveProviders.contains("ICICI Bank"))
        assertTrue(defaultConfig.effectiveProviders.contains("Axis Bank"))
        assertTrue(defaultConfig.effectiveProviders.contains("Kotak Bank"))

        // Boundary check at minEmiOrderAmount = 3000
        assertFalse(defaultConfig.isEligibleForEmi(2999))
        assertTrue(defaultConfig.isEligibleForEmi(3000))
        assertTrue(defaultConfig.isEligibleForEmi(4400))

        // Starting monthly estimate (rounded /12)
        assertEquals(300, defaultConfig.emiStartingPerMonth(3600))

        // Plans include 3, 6, 9, 12 months with No-Cost on 3 and 6 months
        val plans = defaultConfig.buildEmiPlans(6000)
        assertEquals(listOf(3, 6, 9, 12), plans.map { it.months })
        assertTrue(plans[0].isNoCost)
        assertEquals(2000, plans[0].monthlyAmount)
        assertTrue(plans[1].isNoCost)
        assertEquals(1000, plans[1].monthlyAmount)
        assertFalse(plans[2].isNoCost)
        assertFalse(plans[3].isNoCost)

        // Remote disable switches eligibility off even above minimum
        val disabledConfig = defaultConfig.copy(isEmiEnabled = false)
        assertFalse(disabledConfig.isEligibleForEmi(15000))

        // Gateway-hosted flag enforcement for PCI-DSS compliance
        assertTrue(PaymentMethod.EMI_INSTALLMENTS.isGatewayHosted)
        assertTrue(PaymentMethod.CREDIT_DEBIT_CARD.isGatewayHosted)
        assertFalse(PaymentMethod.CASH_ON_DELIVERY.isGatewayHosted)
    }

    @Test
    fun `StoreDeliveryConfig and CheckoutPricingEngine enforce zone delivery fees by pincode and atomic stock bounds`() {
        val deliveryConfig = StoreDeliveryConfig(
            localPincodes = listOf("148020", "148019"),
            freeDeliveryMinOrder = 499,
            localDeliveryFee = 25,
            standardDeliveryFee = 60,
            expressDeliveryFee = 25
        )
        val grocery = sealedGroceryItem(stock = 4, mrp = 300) // selling price = 264 at 12%

        // Local pincode 148020 below ₹499 -> ₹25 local delivery fee
        val localSmallOrder = CheckoutPricingEngine.verifyAndCompute(
            cartItems = listOf(CartItem(product = grocery, quantity = 1)),
            masterCatalog = listOf(grocery),
            deliveryOption = DeliveryOption.HOME_DELIVERY,
            userAccount = UserAccount(uid = "u_local"),
            applySpinCoupon = false,
            requestedCoinsToRedeem = 0,
            offersConfig = StoreOffersConfig(),
            deliveryConfig = deliveryConfig,
            pinCode = "148020"
        ).getOrThrow()
        assertEquals(25, localSmallOrder.deliveryFee)
        assertEquals(264 + 25, localSmallOrder.finalPayableTotal)

        // Local pincode 148020 at/above ₹499 (2 * 264 = ₹528) -> FREE ₹0 delivery
        val localFreeOrder = CheckoutPricingEngine.verifyAndCompute(
            cartItems = listOf(CartItem(product = grocery, quantity = 2)),
            masterCatalog = listOf(grocery),
            deliveryOption = DeliveryOption.LOCAL_2_HR,
            userAccount = UserAccount(uid = "u_local"),
            applySpinCoupon = false,
            requestedCoinsToRedeem = 0,
            offersConfig = StoreOffersConfig(),
            deliveryConfig = deliveryConfig,
            pinCode = "148020"
        ).getOrThrow()
        assertEquals(0, localFreeOrder.deliveryFee)
        assertEquals(528, localFreeOrder.finalPayableTotal)

        // Non-local pincode 141001 below ₹499 -> ₹60 standard delivery fee
        val outstationOrder = CheckoutPricingEngine.verifyAndCompute(
            cartItems = listOf(CartItem(product = grocery, quantity = 1)),
            masterCatalog = listOf(grocery),
            deliveryOption = DeliveryOption.HOME_DELIVERY,
            userAccount = UserAccount(uid = "u_out"),
            applySpinCoupon = false,
            requestedCoinsToRedeem = 0,
            offersConfig = StoreOffersConfig(),
            deliveryConfig = deliveryConfig,
            pinCode = "141001"
        ).getOrThrow()
        assertEquals(60, outstationOrder.deliveryFee)
        assertEquals(264 + 60, outstationOrder.finalPayableTotal)

        // Store Pickup is always ₹0 regardless of pincode or subtotal
        val pickupOrder = CheckoutPricingEngine.verifyAndCompute(
            cartItems = listOf(CartItem(product = grocery, quantity = 1)),
            masterCatalog = listOf(grocery),
            deliveryOption = DeliveryOption.STORE_PICKUP,
            userAccount = UserAccount(uid = "u_pickup"),
            applySpinCoupon = false,
            requestedCoinsToRedeem = 0,
            offersConfig = StoreOffersConfig(),
            deliveryConfig = deliveryConfig,
            pinCode = "141001"
        ).getOrThrow()
        assertEquals(0, pickupOrder.deliveryFee)

        // Out-of-stock rejection (stock <= 0)
        val oosProduct = grocery.copy(stock = 0)
        val oosResult = CheckoutPricingEngine.verifyAndCompute(
            cartItems = listOf(CartItem(product = oosProduct, quantity = 1)),
            masterCatalog = listOf(oosProduct),
            deliveryOption = DeliveryOption.LOCAL_2_HR,
            userAccount = null,
            applySpinCoupon = false,
            requestedCoinsToRedeem = 0,
            deliveryConfig = deliveryConfig
        )
        assertTrue(oosResult.isFailure)
        assertTrue(oosResult.exceptionOrNull()?.message?.contains("out of stock", ignoreCase = true) == true)

        // Exceeding stock limit rejection (qty > stock)
        val overQtyResult = CheckoutPricingEngine.verifyAndCompute(
            cartItems = listOf(CartItem(product = grocery, quantity = 5)),
            masterCatalog = listOf(grocery), // stock = 4
            deliveryOption = DeliveryOption.LOCAL_2_HR,
            userAccount = null,
            applySpinCoupon = false,
            requestedCoinsToRedeem = 0,
            deliveryConfig = deliveryConfig
        )
        assertTrue(overQtyResult.isFailure)
        assertTrue(overQtyResult.exceptionOrNull()?.message?.contains("4 unit", ignoreCase = true) == true)
    }

    @Test
    fun `Product effectiveReturnWindowBadge returns category-specific English, Punjabi, and Hindi return badges and respects remote override`() {
        val elec = electronicsItem()
        val apparel = apparelItem()
        val frozen = coldChainItem()
        val sealed = sealedGroceryItem()

        assertEquals("7-Day Replacement", elec.effectiveReturnWindowBadge(AppLanguage.ENGLISH))
        assertEquals("7-ਦਿਨ ਰਿਪਲੇਸਮੈਂਟ", elec.effectiveReturnWindowBadge(AppLanguage.PUNJABI))
        assertEquals("7-दिन रिप्लेसमेंट", elec.effectiveReturnWindowBadge(AppLanguage.HINDI))

        assertEquals("7-Day Size Exchange / Return", apparel.effectiveReturnWindowBadge(AppLanguage.ENGLISH))
        assertTrue(frozen.effectiveReturnWindowBadge(AppLanguage.ENGLISH).contains("cold-chain", ignoreCase = true))
        assertTrue(sealed.effectiveReturnWindowBadge(AppLanguage.ENGLISH).contains("Non-returnable sealed", ignoreCase = true))

        // Remote category return policy override takes precedence in English
        assertEquals(
            "10-Day Brand Service Replacement",
            elec.effectiveReturnWindowBadge(AppLanguage.ENGLISH, categoryReturnPolicy = "10-Day Brand Service Replacement")
        )
        assertFalse(elec.copy(stock = 0).isInStock)
        assertTrue(elec.isInStock)
    }

    @Test
    fun `OrderStatusTracker resolves merged lifecycle stages and StoreProfile exposes valid GSTIN and legal URLs`() {
        val tracker = OrderStatusTracker()
        val initialOrder = FirestoreOrderRecord(orderId = "NAV-501", uid = "uid_1", status = "Order Placed")

        // First baseline snapshot returns empty (prevents duplicate alert on sign-in)
        assertTrue(tracker.onOrdersSnapshot("uid_1", listOf(initialOrder)).isEmpty())

        // Transition Order Placed -> Packing emits 1 change event
        val packingChanges = tracker.onOrdersSnapshot(
            "uid_1",
            listOf(initialOrder.copy(status = "Packing"))
        )
        assertEquals(1, packingChanges.size)
        assertEquals("Order Placed", packingChanges.first().previousStatus)
        assertEquals("Packing", packingChanges.first().newStatus)

        // Merged status picks the further lifecycle stage
        assertEquals("Out for Delivery", OrderStatusTracker.resolveMergedStatus("Packing", "Out for Delivery"))
        assertEquals("Delivered", OrderStatusTracker.resolveMergedStatus("Delivered", "Packing"))
        assertEquals("Cancelled", OrderStatusTracker.resolveMergedStatus("Order Placed", "Cancelled"))

        // Mandatory Play Store GSTIN and legal links
        assertEquals("03AHPPU6185N1ZF", StoreProfile.GSTIN)
        assertEquals("https://www.navgrocerystore.com/privacy-policy", StoreProfile.PRIVACY_POLICY_URL)
        assertEquals("https://www.navgrocerystore.com/terms", StoreProfile.TERMS_URL)
        assertEquals("https://www.navgrocerystore.com/refund-policy", StoreProfile.REFUND_POLICY_URL)

        // Website-controlled heroPromo configuration model
        val heroPromo = com.example.data.model.StoreHeroPromoConfig(
            isActive = true,
            badgeText = "DIWALI MEGA PROMO",
            title = "5G Smartphones & Festive Dry Fruits",
            subtitle = "Synced live from NavKart storefront",
            mediaType = "VIDEO",
            bannerImageUrl = "https://www.navgrocerystore.com/banners/promo.webp",
            videoUrl = "https://www.navgrocerystore.com/videos/tour.mp4",
            ctaText = "Explore Electronics",
            ctaCategory = "Electronics"
        )
        assertTrue(heroPromo.isActive)
        assertEquals("VIDEO", heroPromo.mediaType)
        assertEquals("Electronics", heroPromo.ctaCategory)
    }
}
