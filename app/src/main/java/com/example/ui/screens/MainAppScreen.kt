package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.StoreProfile
import com.example.data.notifications.NavKartFirebaseMessagingService
import com.example.ui.components.GroceryTopBar
import com.example.ui.components.OrderStatusSnackbarBanner
import com.example.ui.components.OrderStatusSnackbarVisuals
import com.example.ui.components.ParchiVoiceOrderDialog
import com.example.ui.components.ProductDetailSheet
import com.example.ui.components.SecureWebViewDialog
import com.example.ui.components.SuperAppDrawerSheet
import com.example.ui.components.WebShieldValidator
import com.example.ui.components.formatOrderStatusNotificationText
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmCreamBorder
import com.example.ui.theme.WholesaleRed
import com.example.ui.viewmodel.GroceryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(
    viewModel: GroceryViewModel
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeTab by viewModel.activeTab.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val currentAppLanguage by rememberUpdatedState(appLanguage)
    val isPunjabi by viewModel.isPunjabi.collectAsState()
    val isTestModeActive by viewModel.isTestModeActive.collectAsState()
    val smartHelperMessages by viewModel.smartHelperMessages.collectAsState()
    val smartHelperUiState by viewModel.smartHelperUiState.collectAsState()

    val currentUser by viewModel.currentUser.collectAsState()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.syncFcmNotifications(context)
        }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            viewModel.syncFcmNotifications(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.orderStatusEvents.collect { event ->
            val alertText = formatOrderStatusNotificationText(event, currentAppLanguage)
            NavKartFirebaseMessagingService.showOrderUpdateNotification(
                context = context,
                title = "Order ${event.orderId} Update",
                body = alertText,
                notificationId = event.orderId.hashCode()
            )
            snackbarHostState.currentSnackbarData?.dismiss()
            val visuals = OrderStatusSnackbarVisuals(
                orderId = event.orderId,
                previousStatus = event.previousStatus,
                newStatus = event.newStatus,
                message = alertText,
                actionLabel = currentAppLanguage.tr("View", "ਦੇਖੋ", "देखें"),
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            val result = snackbarHostState.showSnackbar(visuals)
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.selectProductForDetail(null)
                viewModel.setShowCheckoutSheet(false)
                viewModel.setActiveTab(3)
            }
        }
    }

    val userAccount by viewModel.userAccount.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()
    val cloudOrders by viewModel.cloudOrders.collectAsState()
    val shoppingQuests by viewModel.shoppingQuests.collectAsState()
    val profileCompletionPercent by viewModel.profileCompletionPercent.collectAsState()
    val authErrorMessage by viewModel.authErrorMessage.collectAsState()
    val lastWonSlice by viewModel.lastWonSlice.collectAsState()
    val spinStatusMessage by viewModel.spinStatusMessage.collectAsState()

    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val rawProducts by viewModel.rawProducts.collectAsState()
    val dynamicCategories by viewModel.dynamicCategories.collectAsState()
    val storeOffersConfig by viewModel.storeOffersConfig.collectAsState()
    val storeHeroPromoConfig by viewModel.storeHeroPromoConfig.collectAsState()
    val storePaymentsConfig by viewModel.storePaymentsConfig.collectAsState()
    val storeDeliveryConfig by viewModel.storeDeliveryConfig.collectAsState()
    val categoryDiscounts by viewModel.categoryDiscounts.collectAsState()
    val categoryReturnPolicies by viewModel.categoryReturnPolicies.collectAsState()
    val heroBanners by viewModel.heroBanners.collectAsState()
    val marqueeText by viewModel.marqueeText.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDepartmentScope by viewModel.selectedDepartmentScope.collectAsState()
    val brands by viewModel.availableBrands.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedPriceRange by viewModel.selectedPriceRange.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val wishlistIds by viewModel.wishlistIds.collectAsState()
    val recentlyViewedCategories by viewModel.recentlyViewedCategories.collectAsState()
    val selectedLocationLabel by viewModel.selectedLocationLabel.collectAsState()

    val cart by viewModel.cart.collectAsState()
    val cartTotalCount by viewModel.cartTotalCount.collectAsState()
    val cartTotalMrp by viewModel.cartTotalMrp.collectAsState()
    val cartTotalSalePrice by viewModel.cartTotalSalePrice.collectAsState()
    val cartTotalSavings by viewModel.cartTotalSavings.collectAsState()
    val hasColdChainInCart by viewModel.hasColdChainInCart.collectAsState()
    val applySpinCouponInCart by viewModel.applySpinCouponInCart.collectAsState()
    val coinsToRedeemInCart by viewModel.coinsToRedeemInCart.collectAsState()
    val applyWalletInCart by viewModel.applyWalletInCart.collectAsState()

    val parchiRequests by viewModel.parchiRequests.collectAsState()
    val rashanSubscription by viewModel.rashanSubscription.collectAsState()

    val orders by viewModel.orders.collectAsState()
    val latestCompletedOrder by viewModel.latestCompletedOrder.collectAsState()
    val isSubmittingOrder by viewModel.isSubmittingOrder.collectAsState()
    val selectedProductDetail by viewModel.selectedProductForDetail.collectAsState()
    val showCheckoutSheet by viewModel.showCheckoutSheet.collectAsState()
    val showParchiDialog by viewModel.showParchiDialog.collectAsState()
    val showWishlistSheet by viewModel.showWishlistSheet.collectAsState()
    val showLocationPicker by viewModel.showLocationPicker.collectAsState()
    val showLegalAndSafety by viewModel.showLegalAndSafety.collectAsState()
    val showAiAssistant by viewModel.showAiAssistant.collectAsState()
    val activeGatewaySession by viewModel.activeGatewaySession.collectAsState()
    val isDeletingAccount by viewModel.isDeletingAccount.collectAsState()

    var activeWebViewPage by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showBrandSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1100L)
        showBrandSplash = false
    }

    val openSmartUrl: (String, String) -> Unit = { title, url ->
        if (WebShieldValidator.isTrustedHttpsUrl(url)) {
            activeWebViewPage = title to url
        } else {
            viewModel.openUrl(context, url)
        }
    }

    val openWhatsApp: () -> Unit = {
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=${StoreProfile.WHATSAPP_NUMBER}&text=Hello%20Nav%20Grocery%20%26%20NavKart!")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    BackHandler(enabled = activeGatewaySession != null || activeWebViewPage != null || drawerState.isOpen || showLegalAndSafety || activeTab != 0) {
        when {
            activeGatewaySession != null -> viewModel.dismissGatewaySession()
            activeWebViewPage != null -> activeWebViewPage = null
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            showLegalAndSafety -> viewModel.setShowLegalAndSafety(false)
            activeTab != 0 -> viewModel.setActiveTab(0)
        }
    }

    if (showLegalAndSafety) {
        LegalAndSafetyScreen(
            isPunjabi = isPunjabi,
            appLanguage = appLanguage,
            offersConfig = storeOffersConfig,
            onBackClick = { viewModel.setShowLegalAndSafety(false) },
            onRequestDataDeletion = { phone -> viewModel.requestDataDeletion(phone) }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SuperAppDrawerSheet(
                isPunjabi = isPunjabi,
                appLanguage = appLanguage,
                userName = userAccount?.displayName ?: currentUser?.displayName,
                dynamicCategories = dynamicCategories,
                onSelectDepartmentCategory = { cat ->
                    viewModel.setCategory(cat)
                    viewModel.setActiveTab(0)
                    coroutineScope.launch { drawerState.close() }
                },
                onBestsellersClick = {
                    viewModel.setCategory("All Products")
                    viewModel.setSort("Biggest Discount")
                    viewModel.setActiveTab(0)
                    coroutineScope.launch { drawerState.close() }
                },
                onDealsClick = {
                    viewModel.setActiveTab(2)
                    coroutineScope.launch { drawerState.close() }
                },
                onAccountOrdersClick = {
                    viewModel.setActiveTab(3)
                    coroutineScope.launch { drawerState.close() }
                },
                onLegalClick = {
                    viewModel.setShowLegalAndSafety(true)
                    coroutineScope.launch { drawerState.close() }
                },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onSelectLanguage = { lang -> viewModel.setAppLanguage(lang) }
            )
        }
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("order_status_snackbar_host")
                ) { data ->
                    OrderStatusSnackbarBanner(
                        snackbarData = data,
                        isPunjabi = isPunjabi,
                        appLanguage = currentAppLanguage
                    )
                }
            },
            topBar = {
                val mergedOrderCount = (cloudOrders.map { it.orderId } + orders.map { it.orderId }).distinct().size
                GroceryTopBar(
                    isPunjabi = isPunjabi,
                    appLanguage = appLanguage,
                    isTestModeActive = isTestModeActive,
                    offersConfig = storeOffersConfig,
                    totalProductsCount = rawProducts.size,
                    selectedLocationLabel = selectedLocationLabel,
                    wishlistCount = wishlistIds.size,
                    signedInName = userAccount?.displayName ?: currentUser?.displayName,
                    activeOrdersCount = mergedOrderCount,
                    marqueeText = marqueeText,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { q ->
                        viewModel.setSearchQuery(q)
                        if (q.isNotBlank() && activeTab != 0) viewModel.setActiveTab(0)
                    },
                    onOpenDrawerClick = { coroutineScope.launch { drawerState.open() } },
                    onLocationClick = { viewModel.setShowLocationPicker(true) },
                    onWishlistClick = { viewModel.setShowWishlistSheet(true) },
                    onParchiVoiceClick = { viewModel.setShowParchiDialog(true) },
                    onAccountOrdersClick = { viewModel.setActiveTab(3) },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onSelectLanguage = { lang -> viewModel.setAppLanguage(lang) },
                    onCallClick = { viewModel.callStore(context) },
                    onWhatsAppClick = openWhatsApp,
                    onAiAssistantClick = { viewModel.setShowAiAssistant(true) },
                    onLegalClick = { viewModel.setShowLegalAndSafety(true) }
                )
            },
            bottomBar = {
                // Persistent 5-Tab Bottom Navigation: Home, Categories, Deals, Account, Cart
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 6.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { viewModel.setActiveTab(0) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == 0) Icons.Default.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = {
                            Text(
                                text = appLanguage.tr("Home", "ਹੋਮ", "होम"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreen,
                            selectedTextColor = ForestGreen,
                            indicatorColor = ForestGreenContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { viewModel.setActiveTab(1) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == 1) Icons.Default.Category else Icons.Outlined.Category,
                                contentDescription = "Categories"
                            )
                        },
                        label = {
                            Text(
                                text = appLanguage.tr("Categories", "ਸ਼੍ਰੇਣੀਆਂ", "श्रेणियाँ"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreen,
                            selectedTextColor = ForestGreen,
                            indicatorColor = ForestGreenContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_categories")
                    )

                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { viewModel.setActiveTab(2) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == 2) Icons.Default.LocalOffer else Icons.Outlined.LocalOffer,
                                contentDescription = "Deals"
                            )
                        },
                        label = {
                            Text(
                                text = appLanguage.tr("Deals", "ਡੀਲਜ਼", "डील्स"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreen,
                            selectedTextColor = ForestGreen,
                            indicatorColor = ForestGreenContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_deals")
                    )

                    NavigationBarItem(
                        selected = activeTab == 3,
                        onClick = { viewModel.setActiveTab(3) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == 3) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle,
                                contentDescription = "Account"
                            )
                        },
                        label = {
                            Text(
                                text = appLanguage.tr("Account", "ਖਾਤਾ", "खाता"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreen,
                            selectedTextColor = ForestGreen,
                            indicatorColor = ForestGreenContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_account")
                    )

                    NavigationBarItem(
                        selected = activeTab == 4,
                        onClick = { viewModel.setActiveTab(4) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (cartTotalCount > 0) {
                                        Badge(containerColor = WholesaleRed, contentColor = Color.White) {
                                            Text("$cartTotalCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (activeTab == 4) Icons.Default.ShoppingCart else Icons.Outlined.ShoppingCart,
                                    contentDescription = "Cart"
                                )
                            }
                        },
                        label = {
                            Text(
                                text = appLanguage.tr("Cart", "ਕਾਰਟ", "कार्ट"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreen,
                            selectedTextColor = ForestGreen,
                            indicatorColor = ForestGreenContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_cart")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    0 -> CatalogScreen(
                        products = filteredProducts,
                        heroBanners = heroBanners,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        selectedDepartmentScope = selectedDepartmentScope,
                        brands = brands,
                        selectedBrand = selectedBrand,
                        selectedSort = selectedSort,
                        offersConfig = storeOffersConfig,
                        heroPromoConfig = storeHeroPromoConfig,
                        categoryDiscounts = categoryDiscounts,
                        categoryReturnPolicies = categoryReturnPolicies,
                        selectedPriceRange = selectedPriceRange,
                        wishlistIds = wishlistIds,
                        cartItems = cart,
                        cartTotalCount = cartTotalCount,
                        cartTotalSalePrice = cartTotalSalePrice,
                        hasColdChainInCart = hasColdChainInCart,
                        isPunjabi = isPunjabi,
                        appLanguage = appLanguage,
                        onDepartmentScopeSelected = { viewModel.setDepartmentScope(it) },
                        onCategorySelected = { viewModel.setCategory(it) },
                        onBrandSelected = { viewModel.setBrand(it) },
                        onPriceRangeSelected = { viewModel.setPriceRange(it) },
                        onSortSelected = { viewModel.setSort(it) },
                        onToggleWishlist = { viewModel.toggleWishlist(it) },
                        onProductClick = { viewModel.selectProductForDetail(it) },
                        onAddToCart = { prod, isPeti -> viewModel.addToCart(prod, 1, isPetiCarton = isPeti) },
                        onAddComboHamper = { hamper -> viewModel.addComboHamperToCart(context, hamper) },
                        onUpdateQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                        onViewCartClick = { viewModel.setActiveTab(4) },
                        onOpenDealsClick = { viewModel.setActiveTab(2) },
                        onStoreInfoClick = { viewModel.setActiveTab(3) },
                        onEmailSupport = { viewModel.emailSupport(context) },
                        onWhatsAppSupport = openWhatsApp,
                        onOpenUrl = { url -> openSmartUrl("Nav Grocery Store", url) }
                    )

                    1 -> CategoriesScreen(
                        allProducts = rawProducts,
                        dynamicCategories = dynamicCategories,
                        offersConfig = storeOffersConfig,
                        categoryDiscounts = categoryDiscounts,
                        recentCategories = recentlyViewedCategories,
                        isPunjabi = isPunjabi,
                        appLanguage = appLanguage,
                        onBrowseCategory = { cat ->
                            viewModel.setCategory(cat)
                            viewModel.setActiveTab(0)
                        },
                        onSelectBrand = { brand ->
                            viewModel.setBrand(brand)
                            viewModel.setActiveTab(0)
                        },
                        onProductClick = { viewModel.selectProductForDetail(it) }
                    )

                    2 -> DealsScreen(
                        currentUser = currentUser,
                        userAccount = userAccount,
                        offersConfig = storeOffersConfig,
                        quests = shoppingQuests,
                        lastWonSlice = lastWonSlice,
                        spinStatusMessage = spinStatusMessage,
                        isPunjabi = isPunjabi,
                        appLanguage = appLanguage,
                        onSignInClick = { viewModel.signInWithGoogle(context) },
                        onSpinClick = { onDone -> viewModel.spinWheelNow(context, onDone) },
                        onClaimQuestClick = { q -> viewModel.claimQuestReward(context, q) },
                        onGoToCartClick = { viewModel.setActiveTab(4) }
                    )

                    3 -> key(currentUser?.uid) {
                        AccountScreen(
                            currentUser = currentUser,
                            userAccount = userAccount,
                            savedAddresses = savedAddresses,
                            cloudOrders = cloudOrders,
                            localOrders = orders,
                            parchiRequests = parchiRequests,
                            rashanSubscription = rashanSubscription,
                            wishlistCount = wishlistIds.size,
                            profileCompletionPercent = profileCompletionPercent,
                            authErrorMessage = authErrorMessage,
                            isPunjabi = isPunjabi,
                            appLanguage = appLanguage,
                            isDeletingAccount = isDeletingAccount,
                            onGoogleSignInClick = { viewModel.signInWithGoogle(context) },
                            onSignOutClick = { viewModel.signOutGoogle(context) },
                            onSaveAddress = { id, name, mobile, street, landmark, village, pin, makeDefault ->
                                viewModel.saveDeliveryAddress(
                                    context, id, name, mobile, street, landmark, village, pin, makeDefault
                                )
                            },
                            onDeleteAddress = { addrId -> viewModel.deleteDeliveryAddress(context, addrId) },
                            onSetDefaultAddress = { addrId -> viewModel.setDefaultDeliveryAddress(addrId) },
                            onOpenWishlistClick = { viewModel.setShowWishlistSheet(true) },
                            onOpenDealsClick = { viewModel.setActiveTab(2) },
                            onOpenParchiClick = { viewModel.setShowParchiDialog(true) },
                            onConfirmParchiBill = { reqId -> viewModel.confirmParchiReadyBill(context, reqId) },
                            onReorderRashanBox = {
                                if (rashanSubscription != null) viewModel.reorderMonthlyRashanBox(context)
                                else viewModel.setActiveTab(4)
                            },
                            onRequestWalletTopup = { amt, utr -> viewModel.requestWalletTopup(context, amt, utr) },
                            onApplyReferralCode = { code -> viewModel.applyReferralCode(context, code) },
                            onOpenUrl = { url -> openSmartUrl("Nav Grocery Store", url) },
                            onOpenPolicyUrl = { title, url -> openSmartUrl(title, url) },
                            onCancelOrder = { orderId -> viewModel.cancelOrder(context, orderId) },
                            onDeleteAccountAndData = { viewModel.deleteAccountAndData(context) },
                            onReorderItems = { prods ->
                                prods.filter { it.stock > 0 }.forEach { viewModel.addToCart(it, 1) }
                                viewModel.setActiveTab(4)
                            },
                            onReorderCloudOrder = { ord -> viewModel.reorderCloudOrder(context, ord) },
                            onReorderLocalOrder = { ord -> viewModel.reorderLocalOrder(context, ord) },
                            onBrowseCatalogClick = { viewModel.setActiveTab(0) }
                        )
                    }

                    4 -> CartScreen(
                        cart = cart,
                        masterCatalog = rawProducts,
                        offersConfig = storeOffersConfig,
                        paymentsConfig = storePaymentsConfig,
                        deliveryConfig = storeDeliveryConfig,
                        categoryDiscounts = categoryDiscounts,
                        userAccount = userAccount,
                        applySpinCoupon = applySpinCouponInCart,
                        coinsToRedeem = coinsToRedeemInCart,
                        applyWalletBalance = applyWalletInCart,
                        hasColdChainInCart = hasColdChainInCart,
                        totalMrp = cartTotalMrp,
                        totalSalePrice = cartTotalSalePrice,
                        totalSavings = cartTotalSavings,
                        isPunjabi = isPunjabi,
                        appLanguage = appLanguage,
                        onToggleSpinCoupon = { viewModel.setApplySpinCouponInCart(it) },
                        onCoinsChange = { viewModel.setCoinsToRedeemInCart(it) },
                        onToggleWalletBalance = { viewModel.setApplyWalletInCart(it) },
                        onSaveRashanBox = { day -> viewModel.saveCurrentCartAsRashanBox(context, day) },
                        onUpdateQuantity = { id, qty, size, color, isPeti ->
                            viewModel.updateCartQuantity(id, qty, size, color, isPeti)
                        },
                        onClearCart = { viewModel.clearCart() },
                        onCheckoutClick = { viewModel.setShowCheckoutSheet(true) },
                        onBrowseCatalogClick = { viewModel.setActiveTab(0) }
                    )
                }
            }

            if (selectedProductDetail != null) {
                val prod = selectedProductDetail!!
                ProductDetailSheet(
                    product = prod,
                    offersConfig = storeOffersConfig,
                    paymentsConfig = storePaymentsConfig,
                    categoryDiscountPercent = categoryDiscounts[prod.category],
                    categoryReturnPolicy = categoryReturnPolicies[prod.category].orEmpty(),
                    cartItems = cart,
                    isPunjabi = isPunjabi,
                    appLanguage = appLanguage,
                    onDismiss = { viewModel.selectProductForDetail(null) },
                    onAddToCart = { product, size, color, isPeti ->
                        viewModel.addToCart(product, 1, size, color, isPeti)
                    },
                    onUpdateQuantity = { id, qty, size, color, isPeti ->
                        viewModel.updateCartQuantity(id, qty, size, color, isPeti)
                    }
                )
            }

            if (showCheckoutSheet) {
                CheckoutSheet(
                    cart = cart,
                    masterCatalog = rawProducts,
                    offersConfig = storeOffersConfig,
                    paymentsConfig = storePaymentsConfig,
                    deliveryConfig = storeDeliveryConfig,
                    categoryDiscounts = categoryDiscounts,
                    currentUser = currentUser,
                    userAccount = userAccount,
                    savedAddresses = savedAddresses,
                    applySpinCoupon = applySpinCouponInCart,
                    coinsToRedeem = coinsToRedeemInCart,
                    applyWalletBalance = applyWalletInCart,
                    totalPayable = cartTotalSalePrice,
                    totalSavings = cartTotalSavings,
                    isPunjabi = isPunjabi,
                    appLanguage = appLanguage,
                    isTestModeActive = isTestModeActive,
                    isSubmittingOrder = isSubmittingOrder,
                    onGoogleSignInClick = { viewModel.signInWithGoogle(context) },
                    onSaveAddressClick = { id, name, mobile, street, landmark, village, pin, makeDefault ->
                        viewModel.saveDeliveryAddress(context, id, name, mobile, street, landmark, village, pin, makeDefault)
                    },
                    onDismiss = { viewModel.setShowCheckoutSheet(false) },
                    onConfirmOrder = { name, phone, option, address, notes, payment, dispatchSlot, isNri, useWallet, pin ->
                        viewModel.placeOrder(
                            context = context,
                            customerName = name,
                            customerPhone = phone,
                            deliveryOption = option,
                            address = address,
                            notes = notes,
                            paymentMethod = payment,
                            dispatchSlot = dispatchSlot,
                            isNriOrder = isNri,
                            useWalletBalance = useWallet,
                            pinCode = pin
                        )
                    },
                    onCallHelpline = { viewModel.callStore(context) }
                )
            }

            if (showParchiDialog) {
                ParchiVoiceOrderDialog(
                    defaultPhone = savedAddresses.firstOrNull()?.mobile ?: userAccount?.phone.orEmpty(),
                    isSignedIn = currentUser != null,
                    isPunjabi = isPunjabi,
                    appLanguage = appLanguage,
                    onSignInFirst = { viewModel.signInWithGoogle(context) },
                    onDismiss = { viewModel.setShowParchiDialog(false) },
                    onSubmitParchi = { mode, notes, voice, photoUrl, phone ->
                        viewModel.submitParchiOrder(context, mode, notes, voice, photoUrl, phone)
                    }
                )
            }

            if (latestCompletedOrder != null) {
                OrderReceiptDialog(
                    order = latestCompletedOrder!!,
                    isPunjabi = isPunjabi,
                    appLanguage = appLanguage,
                    onDismiss = { viewModel.dismissCompletedOrderReceipt() }
                )
            }

            if (showWishlistSheet) {
                val wishedProducts = rawProducts.filter { it.id in wishlistIds }
                AlertDialog(
                    onDismissRequest = { viewModel.setShowWishlistSheet(false) },
                    title = {
                        Text(
                            text = appLanguage.tr(
                                english = "My Wishlist (${wishedProducts.size})",
                                punjabi = "ਤੁਹਾਡੀ ਵਿਸ਼ਲਿਸਟ (${wishedProducts.size})",
                                hindi = "मेरी विशलिस्ट (${wishedProducts.size})"
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestGreen
                        )
                    },
                    text = {
                        if (wishedProducts.isEmpty()) {
                            Text(
                                appLanguage.tr(
                                    english = "Tap the heart icon on any marketplace item to save your favorites here.",
                                    punjabi = "ਆਪਣੀਆਂ ਮਨਪਸੰਦ ਚੀਜ਼ਾਂ ਸੇਵ ਕਰਨ ਲਈ ਦਿਲ ਵਾਲੇ ਆਈਕਨ 'ਤੇ ਟੈਪ ਕਰੋ।",
                                    hindi = "अपने पसंदीदा उत्पाद सेव करने के लिए दिल वाले आइकन पर टैप करें।"
                                )
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(wishedProducts, key = { it.id }) { prod ->
                                    val livePrice = prod.sellingPrice(categoryDiscounts[prod.category], storeOffersConfig)
                                    Surface(
                                        color = WarmCream,
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmCreamBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                                Text("₹$livePrice (MRP ₹${prod.mrp})", color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Row {
                                                TextButton(onClick = { viewModel.addToCart(prod, 1) }) {
                                                    Text(appLanguage.tr("+Cart", "+ਕਾਰਟ", "+कार्ट"), fontWeight = FontWeight.Bold, color = ForestGreen)
                                                }
                                                IconButton(onClick = { viewModel.toggleWishlist(prod.id) }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = WholesaleRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.setShowWishlistSheet(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                        ) {
                            Text(appLanguage.tr("Close", "ਬੰਦ ਕਰੋ", "बंद करें"))
                        }
                    }
                )
            }

            if (showLocationPicker) {
                AlertDialog(
                    onDismissRequest = { viewModel.setShowLocationPicker(false) },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = ForestGreen) },
                    title = {
                        Text(
                            text = appLanguage.tr(
                                english = "Select Sandhaur Hub Zone",
                                punjabi = "ਡਿਲਿਵਰੀ ਖੇਤਰ ਚੁਣੋ",
                                hindi = "संधौर डिलीवरी क्षेत्र चुनें"
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestGreen
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = {
                                    viewModel.setLocationLabel(StoreProfile.LOCATION_SHORT)
                                    viewModel.setShowLocationPicker(false)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = ForestGreenContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Main Bazaar, Sandhaur 148020 (15-Min Express)",
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            savedAddresses.forEach { addr ->
                                Surface(
                                    onClick = {
                                        viewModel.setLocationLabel("${addr.streetAddress.take(22)}, ${addr.pinCode}")
                                        viewModel.setDefaultDeliveryAddress(addr.id)
                                        viewModel.setShowLocationPicker(false)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, WarmCreamBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${addr.fullName} • ${addr.formattedLine()}",
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            StoreProfile.NEARBY_VILLAGES.take(5).forEach { v ->
                                Surface(
                                    onClick = {
                                        viewModel.setLocationLabel("$v • 148020")
                                        viewModel.setShowLocationPicker(false)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = WarmCream,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "$v (~2-Hr Local Delivery)",
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(9.dp)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.setShowLocationPicker(false) }) {
                            Text(
                                text = appLanguage.tr("Close", "ਬੰਦ ਕਰੋ", "बंद करें"),
                                color = ForestGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }

            if (showAiAssistant) {
                AiShoppingAssistantDialog(
                    messages = smartHelperMessages,
                    uiState = smartHelperUiState,
                    appLanguage = appLanguage,
                    offersConfig = storeOffersConfig,
                    categoryDiscounts = categoryDiscounts,
                    userCoinsBalance = userAccount?.navCoins ?: 0L,
                    onSelectLanguage = { lang -> viewModel.setAppLanguage(lang) },
                    onSendPrompt = { prompt, lang -> viewModel.sendSmartHelperPrompt(prompt, lang) },
                    onRetryPrompt = { viewModel.retryLastSmartHelperPrompt() },
                    onDismissError = { viewModel.clearSmartHelperError() },
                    onDismiss = { viewModel.setShowAiAssistant(false) },
                    onSelectProduct = { prod -> viewModel.selectProductForDetail(prod) },
                    onAddToCart = { prod -> viewModel.addToCart(prod, 1) }
                )
            }

            activeGatewaySession?.let { (gatewayTitle, gatewayUrl) ->
                SecureWebViewDialog(
                    title = gatewayTitle,
                    initialUrl = gatewayUrl,
                    onDismiss = { viewModel.dismissGatewaySession() }
                )
            }

            activeWebViewPage?.let { (pageTitle, pageUrl) ->
                SecureWebViewDialog(
                    title = pageTitle,
                    initialUrl = pageUrl,
                    onDismiss = { activeWebViewPage = null }
                )
            }
        }
    }

    AnimatedVisibility(
        visible = showBrandSplash,
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ForestGreenDark)
                .clickable { showBrandSplash = false }
                .testTag("brand_splash_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nav_brand_logo),
                    contentDescription = "Nav Grocery Store Navkart Official Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .border(3.dp, SignatureGold, CircleShape)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Nav Grocery Store Navkart",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Main Bazaar, Sandhaur 148020 • Official Marketplace",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = SignatureGold,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
