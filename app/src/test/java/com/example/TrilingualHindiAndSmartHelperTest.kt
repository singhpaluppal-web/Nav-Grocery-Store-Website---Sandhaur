package com.example

import com.example.data.ai.SmartHelperAiService
import com.example.data.model.AppLanguage
import com.example.data.model.CoinRewardConfig
import com.example.data.model.FirestoreOrderRecord
import com.example.data.model.MarketplaceCategory
import com.example.data.model.Order
import com.example.data.model.PriceFilterRange
import com.example.data.model.Product
import com.example.data.model.StoreOffersConfig
import com.example.data.model.UserAccount
import com.example.data.security.OrderStatusChange
import com.example.ui.components.formatOrderStatusNotificationText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrilingualHindiAndSmartHelperTest {

    private val sampleCatalog = listOf(
        Product(
            id = "NAV001",
            name = "Aashirvaad Shudh Chakki Atta 10kg",
            brand = "Aashirvaad",
            category = "Atta & Dals",
            department = "Groceries",
            mrp = 500,
            sale = 440,
            stock = 20
        ),
        Product(
            id = "NAV002",
            name = "Verka Pure Desi Ghee 1L",
            brand = "Verka",
            category = "Desi Ghee & Oils",
            department = "Groceries",
            mrp = 650,
            sale = 572,
            stock = 14
        ),
        Product(
            id = "NAV201",
            name = "Dove Cream Beauty Bathing Bar Pack",
            brand = "Dove",
            category = "Soaps & Household",
            department = "Household",
            mrp = 240,
            sale = 211,
            stock = 18
        ),
        Product(
            id = "NAV301",
            name = "Redmi Note 5G Smartphone 128GB",
            brand = "Xiaomi",
            category = "Electronics & Mobiles",
            department = "Electronics",
            mrp = 15999,
            sale = 14079,
            stock = 5
        ),
        Product(
            id = "NAV401",
            name = "Patiala Cotton Salwar Suit Set",
            brand = "Navkart Fashion",
            category = "Clothing & Apparel",
            department = "Fashion",
            mrp = 1499,
            sale = 1319,
            stock = 9
        )
    )

    @Test
    fun appLanguageLocalesLabelsAndCyclingWorkCorrectly() {
        assertEquals("en-IN", AppLanguage.ENGLISH.speechLocaleTag)
        assertEquals("pa-IN", AppLanguage.PUNJABI.speechLocaleTag)
        assertEquals("hi-IN", AppLanguage.HINDI.speechLocaleTag)

        assertEquals(AppLanguage.PUNJABI, AppLanguage.ENGLISH.next())
        assertEquals(AppLanguage.HINDI, AppLanguage.PUNJABI.next())
        assertEquals(AppLanguage.ENGLISH, AppLanguage.HINDI.next())

        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("hi"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromCode("hi-IN"))
        assertEquals(AppLanguage.PUNJABI, AppLanguage.fromCode("pa-IN"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))

        assertEquals(
            "खोजें",
            AppLanguage.HINDI.tr(english = "Search", punjabi = "ਖੋਜੋ", hindi = "खोजें")
        )
    }

    @Test
    fun appLanguageDetectsDevanagariGurmukhiHinglishAndTtsLocales() {
        assertEquals(
            AppLanguage.HINDI,
            AppLanguage.detectFromText("मुझे 10 किलो आटा और देसी घी चाहिए", AppLanguage.ENGLISH)
        )
        assertEquals(
            AppLanguage.PUNJABI,
            AppLanguage.detectFromText("ਮੈਨੂੰ 10 ਕਿਲੋ ਆਟਾ ਚਾਹੀਦਾ ਹੈ", AppLanguage.ENGLISH)
        )
        assertEquals(
            AppLanguage.HINDI,
            AppLanguage.detectFromText("bhaiya aata ka rate batao kya hai", AppLanguage.ENGLISH)
        )
        assertEquals(
            AppLanguage.ENGLISH,
            AppLanguage.detectFromText("Show me best atta deals", AppLanguage.ENGLISH)
        )

        assertEquals(
            AppLanguage.HINDI,
            AppLanguage.detectTtsLanguage("आशीर्वाद आटा ₹440 में उपलब्ध है", AppLanguage.ENGLISH)
        )
        assertEquals(
            AppLanguage.PUNJABI,
            AppLanguage.detectTtsLanguage("ਆਸ਼ੀਰਵਾਦ ਆਟਾ ₹440 ਵਿੱਚ ਉਪਲਬਧ ਹੈ", AppLanguage.ENGLISH)
        )
        assertEquals(
            AppLanguage.ENGLISH,
            AppLanguage.detectTtsLanguage("Aashirvaad Atta is ₹440", AppLanguage.HINDI)
        )
    }

    @Test
    fun smartHelperSystemInstructionEnforcesTrilingualCustomerOnlyGuardrails() {
        val instruction = SmartHelperAiService.SYSTEM_INSTRUCTION
        assertTrue(instruction.contains("Hindi"))
        assertTrue(instruction.contains("Devanagari"))
        assertTrue(instruction.contains("Punjabi"))
        assertTrue(instruction.contains("Gurmukhi"))
        assertTrue(instruction.contains("CUSTOMER_OWN_ORDERS"))
        assertTrue(instruction.contains("NEVER mention, reveal, or invent any admin panel, POS billing tools, Digital Khata, Navo Hub"))
        assertFalse(instruction.contains("AIza"))
        assertFalse(instruction.contains("GEMINI_API_KEY"))
    }

    @Test
    fun buildCustomerContextPromptIncludesDynamicOffersCoinsAndCustomerOrdersOnly() {
        val offersConfig = StoreOffersConfig(
            discountPercent = 15.0,
            offerBannerText = "Diwali Mega Sale 15% OFF",
            isOfferActive = true,
            coinConfig = CoinRewardConfig(
                isCoinsEnabled = true,
                earnRatePercent = 5.0,
                coinValueInr = 1.0,
                maxRedeemPercent = 20.0
            )
        )
        val account = UserAccount(uid = "cust_1", displayName = "Harpreet", navCoins = 125L)
        val cloudOrders = listOf(
            FirestoreOrderRecord(
                id = "NAV-5001",
                orderId = "NAV-5001",
                status = "Out for Delivery",
                finalTotal = 850,
                deliveryTier = "15-Min Express",
                coinsEarned = 42
            )
        )

        val prompt = SmartHelperAiService.buildCustomerContextPrompt(
            userPrompt = "मेरा ऑर्डर कहाँ है?",
            targetLanguage = AppLanguage.HINDI,
            matchedProducts = sampleCatalog.take(2),
            offersConfig = offersConfig,
            categoryDiscounts = mapOf("Atta & Dals" to 18.0),
            userAccount = account,
            localOrders = emptyList<Order>(),
            cloudOrders = cloudOrders
        )

        assertTrue(prompt.contains("TARGET_LANGUAGE_HINT: HINDI (हिंदी / hi-IN)"))
        assertTrue(prompt.contains("Storewide Discount: 15% OFF MRP"))
        assertTrue(prompt.contains("Active Promo Banner: Diwali Mega Sale 15% OFF"))
        assertTrue(prompt.contains("earn=5%, 1_coin_value=₹1, max_redeem_cap=20%_per_order"))
        assertTrue(prompt.contains("Customer Coin Balance: 125 Nav Coins | Signed In: true"))
        assertTrue(prompt.contains("NAV-5001: status=Out for Delivery, total=₹850"))
        assertTrue(prompt.contains("ID=NAV001 | Aashirvaad Shudh Chakki Atta 10kg"))
        assertTrue(prompt.contains("18% OFF"))
    }

    @Test
    fun hindiKeywordSearchMatchesRelevantMultiCategoryProducts() {
        val gheeAndAtta = SmartHelperAiService.findRelevantProducts("देसी घी और आटा दिखाओ", sampleCatalog)
        assertTrue(gheeAndAtta.any { it.id == "NAV001" })
        assertTrue(gheeAndAtta.any { it.id == "NAV002" })

        val soap = SmartHelperAiService.findRelevantProducts("नहाने का साबुन", sampleCatalog)
        assertEquals("NAV201", soap.first().id)

        val mobile = SmartHelperAiService.findRelevantProducts("सस्ता 5G मोबाइल फोन", sampleCatalog)
        assertEquals("NAV301", mobile.first().id)

        val suit = SmartHelperAiService.findRelevantProducts("पटियाला सूट दिखाएं", sampleCatalog)
        assertEquals("NAV401", suit.first().id)
    }

    @Test
    fun parseSkuTagsAndSanitizeTtsWorkForHindiPunjabiEnglish() {
        val raw = "**नमस्ते!** यह रहा आटा ₹440 में। [SKU:NAV001, NAV002, NAV001]"
        val (cleanDisplay, skus) = SmartHelperAiService.parseRecommendedSkuIds(raw)
        assertEquals("**नमस्ते!** यह रहा आटा ₹440 में।", cleanDisplay)
        assertEquals(listOf("NAV001", "NAV002"), skus)

        val hindiTts = SmartHelperAiService.sanitizeForTts(cleanDisplay, AppLanguage.HINDI)
        assertFalse(hindiTts.contains("**"))
        assertFalse(hindiTts.contains("₹"))
        assertTrue(hindiTts.contains("रुपये 440"))
    }

    @Test
    fun orderNotificationAndMarketplaceModelsSupportHindi() {
        val change = OrderStatusChange(
            id = "c1",
            orderId = "NAV-777",
            previousStatus = "Packing",
            newStatus = "Out for Delivery"
        )
        assertEquals(
            "ऑर्डर NAV-777: Packing → Out for Delivery",
            formatOrderStatusNotificationText(change, AppLanguage.HINDI)
        )
        assertEquals("₹100 से कम", PriceFilterRange.UNDER_100.labelFor(AppLanguage.HINDI))

        val cat = MarketplaceCategory(
            id = "cat_1",
            name = "Electronics",
            punjabiName = "ਇਲੈਕਟ੍ਰਾਨਿਕਸ",
            hindiName = "इलेक्ट्रॉनिक्स"
        )
        assertEquals("इलेक्ट्रॉनिक्स", cat.displayName(AppLanguage.HINDI))
    }
}
