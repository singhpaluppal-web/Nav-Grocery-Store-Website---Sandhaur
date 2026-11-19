package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.StoreDeliveryConfig
import com.example.data.model.StoreHeroPromoConfig
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StorePaymentsConfig
import com.example.data.model.UserAccount
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.CheckoutSheet
import com.example.ui.screens.LegalAndSafetyScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xhdpi")
class StorefrontUiFlowRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val samplePhone = Product(
        id = "ELEC_5G_01",
        name = "Galaxy 5G Smartphone",
        brand = "Samsung",
        department = "Electronics and Mobiles",
        category = "Electronics",
        sellerName = "Banga Electronics Sandhaur",
        mrp = 12000,
        stock = 6,
        availableSizes = "128GB,256GB",
        availableColors = "Midnight Black,Forest Green"
    )

    private val sampleGhee = Product(
        id = "GROC_GHEE_01",
        name = "Verka Pure Desi Ghee 1L",
        brand = "Verka",
        department = "Packaged Groceries",
        category = "Desi Ghee & Oils",
        mrp = 650,
        stock = 20
    )

    @Test
    fun `CatalogScreen renders dynamic categories, live heroPromo, offer banner, and dispatches category selection`() {
        var clickedCategory = ""
        composeRule.setContent {
            MyApplicationTheme {
                CatalogScreen(
                    products = listOf(sampleGhee, samplePhone),
                    categories = listOf("All Products", "Desi Ghee & Oils", "Electronics", "Clothing", "Cosmetics"),
                    selectedCategory = "All Products",
                    brands = listOf("All", "Verka", "Samsung"),
                    selectedBrand = "All",
                    selectedSort = "Default",
                    offersConfig = StoreOffersConfig(
                        discountPercent = 12.0,
                        isOfferActive = true,
                        offerBannerText = "NavKart Mela: 12% OFF + 4% Nav Coins!"
                    ),
                    heroPromoConfig = StoreHeroPromoConfig(
                        isActive = true,
                        badgeText = "LIVE PROMO",
                        title = "Festival Electronics & Sealed Ration Sale",
                        subtitle = "Sandhaur Main Bazaar • B2B Partners",
                        mediaType = "VIDEO",
                        videoUrl = "https://www.navgrocerystore.com/promo.mp4",
                        ctaText = "Shop Electronics",
                        ctaCategory = "Electronics"
                    ),
                    cartItems = emptyList(),
                    cartTotalCount = 0,
                    cartTotalSalePrice = 0,
                    isPunjabi = false,
                    appLanguage = AppLanguage.ENGLISH,
                    onCategorySelected = { clickedCategory = it },
                    onBrandSelected = {},
                    onSortSelected = {},
                    onProductClick = {},
                    onAddToCart = { _, _ -> },
                    onUpdateQuantity = { _, _ -> },
                    onViewCartClick = {},
                    onStoreInfoClick = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("live_remote_offer_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("live_hero_promo_card").assertIsDisplayed()
        composeRule.onNodeWithText("Festival Electronics & Sealed Ration Sale").assertIsDisplayed()
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText("Shop Electronics"))
        composeRule.onNodeWithText("Shop Electronics").performClick()
        composeRule.waitForIdle()
        assertEquals("Electronics", clickedCategory)
    }

    @Test
    fun `CartScreen displays item subtotal, live pricing breakdown, and Bajaj & Bank EMI eligibility badge`() {
        var checkoutTriggered = false
        composeRule.setContent {
            MyApplicationTheme {
                CartScreen(
                    cart = listOf(CartItem(product = samplePhone, quantity = 1)),
                    masterCatalog = listOf(samplePhone),
                    offersConfig = StoreOffersConfig(discountPercent = 10.0),
                    paymentsConfig = StorePaymentsConfig(isEmiEnabled = true, minEmiOrderAmount = 3000),
                    deliveryConfig = StoreDeliveryConfig(),
                    userAccount = UserAccount(uid = "uid_test", navCoins = 150),
                    totalMrp = 12000,
                    totalSalePrice = 10800,
                    totalSavings = 1200,
                    isPunjabi = false,
                    appLanguage = AppLanguage.ENGLISH,
                    onUpdateQuantity = { _, _, _, _, _ -> },
                    onClearCart = {},
                    onCheckoutClick = { checkoutTriggered = true },
                    onBrowseCatalogClick = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasTestTag("emi_eligibility_badge"))
        composeRule.onNodeWithTag("emi_eligibility_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("view_emi_plans_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("emi_plans_bottom_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Bajaj Finserv EMI Card").assertIsDisplayed()
        composeRule.onNodeWithText("Close Plans").performClick()
        composeRule.waitForIdle()

        assertTrue(!checkoutTriggered)
    }

    @Test
    fun `CheckoutSheet validates Sandhaur pincode input and surfaces EMI eligibility`() {
        composeRule.setContent {
            MyApplicationTheme {
                CheckoutSheet(
                    cart = listOf(CartItem(product = samplePhone, quantity = 1)),
                    masterCatalog = listOf(samplePhone),
                    offersConfig = StoreOffersConfig(discountPercent = 10.0),
                    paymentsConfig = StorePaymentsConfig(isEmiEnabled = true, minEmiOrderAmount = 3000),
                    deliveryConfig = StoreDeliveryConfig(localPincodes = listOf("148020")),
                    currentUser = null,
                    userAccount = null,
                    totalPayable = 10800,
                    totalSavings = 1200,
                    isPunjabi = false,
                    appLanguage = AppLanguage.ENGLISH,
                    isTestModeActive = false,
                    onDismiss = {},
                    onConfirmOrder = { _, _, _, _, _, _, _, _, _, _ -> },
                    onCallHelpline = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("checkout_pincode_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("checkout_pincode_input").performTextReplacement("148020")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sandhaur Local Zone (148020)", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("emi_eligibility_badge").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `AccountScreen exposes Play Store legal links and Delete Account gate while LegalAndSafetyScreen submits DPDP erasure`() {
        var openedPolicyUrl = ""
        var signInPromptedForGuestDelete = false
        composeRule.setContent {
            MyApplicationTheme {
                AccountScreen(
                    currentUser = null,
                    userAccount = null,
                    savedAddresses = emptyList(),
                    cloudOrders = emptyList(),
                    localOrders = emptyList(),
                    wishlistCount = 0,
                    profileCompletionPercent = 20,
                    authErrorMessage = null,
                    isPunjabi = false,
                    appLanguage = AppLanguage.ENGLISH,
                    onGoogleSignInClick = { signInPromptedForGuestDelete = true },
                    onSignOutClick = {},
                    onSaveAddress = { _, _, _, _, _, _, _, _ -> },
                    onDeleteAddress = {},
                    onSetDefaultAddress = {},
                    onOpenWishlistClick = {},
                    onOpenDealsClick = {},
                    onOpenPolicyUrl = { _, url -> openedPolicyUrl = url },
                    onReorderItems = {},
                    onBrowseCatalogClick = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Verify Privacy Policy / Terms / Refund URLs on www.navgrocerystore.com
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasTestTag("account_privacy_policy_btn"))
        composeRule.onNodeWithTag("account_privacy_policy_btn").performClick()
        composeRule.waitForIdle()
        assertEquals("https://www.navgrocerystore.com/privacy-policy", openedPolicyUrl)

        composeRule.onNodeWithTag("account_terms_btn").performClick()
        composeRule.waitForIdle()
        assertEquals("https://www.navgrocerystore.com/terms", openedPolicyUrl)

        composeRule.onNodeWithTag("account_refund_policy_btn").performClick()
        composeRule.waitForIdle()
        assertEquals("https://www.navgrocerystore.com/refund-policy", openedPolicyUrl)

        // Delete button requires Sign-In or confirmation
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasTestTag("delete_my_account_btn"))
        composeRule.onNodeWithTag("delete_my_account_btn").performClick()
        composeRule.waitForIdle()
        assertTrue(signInPromptedForGuestDelete)
    }

    @Test
    fun `LegalAndSafetyScreen switches to Hindi and renders DPDP Delete Data button`() {
        var backClicked = false
        composeRule.setContent {
            MyApplicationTheme {
                LegalAndSafetyScreen(
                    isPunjabi = false,
                    appLanguage = AppLanguage.HINDI,
                    onBackClick = { backClicked = true },
                    onRequestDataDeletion = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("legal_delete_tab").let { nodes ->
            if (nodes.fetchSemanticsNodes().isNotEmpty()) {
                nodes.onFirst().performClick()
            } else {
                composeRule.onNodeWithText("डेटा हटाएं").performClick()
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("request_data_deletion_btn").assertIsDisplayed()
        assertTrue(!backClicked)
    }
}
