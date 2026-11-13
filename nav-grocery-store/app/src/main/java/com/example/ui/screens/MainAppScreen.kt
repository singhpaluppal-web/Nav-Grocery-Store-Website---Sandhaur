package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Seller
import com.example.ui.components.GroceryTopBar
import com.example.ui.components.ProductDetailSheet
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.WholesaleRed
import com.example.ui.viewmodel.GroceryViewModel

@Composable
fun MainAppScreen(
    viewModel: GroceryViewModel
) {
    val context = LocalContext.current

    val activeTab by viewModel.activeTab.collectAsState()
    val isPunjabi by viewModel.isPunjabi.collectAsState()
    val isTestModeActive by viewModel.isTestModeActive.collectAsState()

    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val rawProducts by viewModel.rawProducts.collectAsState()
    val sellers by viewModel.sellers.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val brands by viewModel.availableBrands.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val cart by viewModel.cart.collectAsState()
    val cartTotalCount by viewModel.cartTotalCount.collectAsState()
    val cartTotalMrp by viewModel.cartTotalMrp.collectAsState()
    val cartTotalSalePrice by viewModel.cartTotalSalePrice.collectAsState()
    val cartTotalSavings by viewModel.cartTotalSavings.collectAsState()

    val orders by viewModel.orders.collectAsState()
    val selectedProductDetail by viewModel.selectedProductForDetail.collectAsState()
    val showCheckoutSheet by viewModel.showCheckoutSheet.collectAsState()
    val showSellerRegistrationSheet by viewModel.showSellerRegistrationSheet.collectAsState()
    val showRiderHub by viewModel.showRiderHub.collectAsState()
    val showAdminController by viewModel.showAdminController.collectAsState()
    val showLegalAndSafety by viewModel.showLegalAndSafety.collectAsState()
    val showAiAssistant by viewModel.showAiAssistant.collectAsState()

    // Smooth Android Back handling across secondary views
    BackHandler(enabled = showAdminController || showRiderHub || showLegalAndSafety || activeTab != 0) {
        when {
            showAdminController -> viewModel.setShowAdminController(false)
            showRiderHub -> viewModel.setShowRiderHub(false)
            showLegalAndSafety -> viewModel.setShowLegalAndSafety(false)
            activeTab != 0 -> viewModel.setActiveTab(0)
        }
    }

    // 1. Dedicated Admin Controller App Modal (Protected by Master PIN 148020)
    if (showAdminController) {
        val mappedSellers = sellers.map { reg ->
            Seller(
                id = reg.id,
                shopName = reg.shopName,
                ownerName = reg.ownerName,
                phone = reg.phone,
                email = "seller@navgrocerystore.com",
                address = reg.villageOrAddress,
                gstinOrPan = reg.gstinOrAadhaar,
                categorySpecialization = reg.businessCategory,
                bankAccountOrUpi = "UPI Linked",
                isApproved = reg.isApproved
            )
        }

        AdminControlCenterScreen(
            orders = orders,
            sellers = mappedSellers,
            payouts = emptyList(),
            products = rawProducts,
            isPunjabi = isPunjabi,
            onBack = { viewModel.setShowAdminController(false) },
            onSettlePayout = { _, _, _ -> },
            onRefundOrder = { _ -> },
            onToggleSellerApproval = { id, approved ->
                val sellerReg = sellers.firstOrNull { it.id == id }
                if (sellerReg != null) {
                    viewModel.approveSeller(sellerReg.copy(isApproved = approved))
                }
            }
        )
        return
    }

    // 2. Delivery Rider Partner Hub
    if (showRiderHub) {
        DeliveryRiderHubScreen(
            isPunjabi = isPunjabi,
            onBack = { viewModel.setShowRiderHub(false) }
        )
        return
    }

    // 3. Indian Government Laws & Legal Policies Suite
    if (showLegalAndSafety) {
        LegalAndSafetyScreen(
            isPunjabi = isPunjabi,
            onBackClick = { viewModel.setShowLegalAndSafety(false) },
            onRequestDataDeletion = { phone -> viewModel.requestDataDeletion(phone) }
        )
        return
    }

    // 4. Main NavKart Public Storefront
    Scaffold(
        topBar = {
            GroceryTopBar(
                isPunjabi = isPunjabi,
                isTestModeActive = isTestModeActive,
                onToggleLanguage = { viewModel.toggleLanguage() },
                onCallClick = { viewModel.callStore(context) },
                onWhatsAppClick = {
                    val fallbackUri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=918847370905&text=Hello%20NavKart!")
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, fallbackUri).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                },
                onAiAssistantClick = { viewModel.setShowAiAssistant(true) },
                onSellerRegisterClick = { viewModel.setShowSellerRegistration(true) },
                onRiderHubClick = { viewModel.setShowRiderHub(true) },
                onLegalClick = { viewModel.setShowLegalAndSafety(true) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                // 1. Shop / Catalog
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 0) Icons.Default.Storefront else Icons.Outlined.Storefront,
                            contentDescription = "Shop"
                        )
                    },
                    label = {
                        Text(
                            text = if (isPunjabi) "ਸਟੋਰ" else "Catalog",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = ForestGreenContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_catalog")
                )

                // 2. Cart Tab with Badge
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { viewModel.setActiveTab(1) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cartTotalCount > 0) {
                                    Badge(
                                        containerColor = WholesaleRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("$cartTotalCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (activeTab == 1) Icons.Default.ShoppingCart else Icons.Outlined.ShoppingCart,
                                contentDescription = "Cart"
                            )
                        }
                    },
                    label = {
                        Text(
                            text = if (isPunjabi) "ਕਾਰਟ" else "Cart",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = ForestGreenContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_cart")
                )

                // 3. My Orders
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { viewModel.setActiveTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 2) Icons.Default.History else Icons.Outlined.History,
                            contentDescription = "Orders"
                        )
                    },
                    label = {
                        Text(
                            text = if (isPunjabi) "ਆਰਡਰ" else "Orders",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = ForestGreenContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_orders")
                )

                // 4. Store Info & Contact
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { viewModel.setActiveTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 3) Icons.Default.Info else Icons.Outlined.Info,
                            contentDescription = "Store Info"
                        )
                    },
                    label = {
                        Text(
                            text = if (isPunjabi) "ਸੰਪਰਕ" else "Store Info",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = ForestGreenContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_store_info")
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
                    categories = categories,
                    selectedCategory = selectedCategory,
                    brands = brands,
                    selectedBrand = selectedBrand,
                    selectedSort = selectedSort,
                    searchQuery = searchQuery,
                    cartItems = cart,
                    cartTotalCount = cartTotalCount,
                    cartTotalSalePrice = cartTotalSalePrice,
                    isPunjabi = isPunjabi,
                    onCategorySelected = { viewModel.setCategory(it) },
                    onBrandSelected = { viewModel.setBrand(it) },
                    onSortSelected = { viewModel.setSort(it) },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onProductClick = { viewModel.selectProductForDetail(it) },
                    onAddToCart = { viewModel.addToCart(it, 1) },
                    onUpdateQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                    onViewCartClick = { viewModel.setActiveTab(1) },
                    onStoreInfoClick = { viewModel.setActiveTab(3) }
                )

                1 -> CartScreen(
                    cart = cart,
                    totalMrp = cartTotalMrp,
                    totalSalePrice = cartTotalSalePrice,
                    totalSavings = cartTotalSavings,
                    isPunjabi = isPunjabi,
                    onUpdateQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                    onClearCart = { viewModel.clearCart() },
                    onCheckoutClick = { viewModel.setShowCheckoutSheet(true) },
                    onBrowseCatalogClick = { viewModel.setActiveTab(0) }
                )

                2 -> OrdersHistoryScreen(
                    orders = orders,
                    isPunjabi = isPunjabi,
                    onReorder = { prods ->
                        prods.forEach { viewModel.addToCart(it, 1) }
                        viewModel.setActiveTab(1)
                    },
                    onBrowseCatalog = { viewModel.setActiveTab(0) }
                )

                3 -> StoreInfoScreen(
                    isPunjabi = isPunjabi,
                    onCallClick = { viewModel.callStore(context) },
                    onWhatsAppClick = {
                        val fallbackUri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=918847370905&text=Hello%20NavKart!")
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, fallbackUri).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    },
                    onInstagramClick = { viewModel.openInstagram(context) },
                    onMapClick = { viewModel.openLocation(context) }
                )
            }
        }

        // Product Detail Bottom Sheet
        if (selectedProductDetail != null) {
            val prod = selectedProductDetail!!
            val qtyInCart = cart.firstOrNull { it.product.id == prod.id }?.quantity ?: 0

            ProductDetailSheet(
                product = prod,
                cartQuantity = qtyInCart,
                isPunjabi = isPunjabi,
                onDismiss = { viewModel.selectProductForDetail(null) },
                onAddToCart = { viewModel.addToCart(it, 1) },
                onUpdateQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) }
            )
        }

        // Checkout Bottom Sheet
        if (showCheckoutSheet) {
            CheckoutSheet(
                totalPayable = cartTotalSalePrice,
                totalSavings = cartTotalSavings,
                isPunjabi = isPunjabi,
                isTestModeActive = isTestModeActive,
                onDismiss = { viewModel.setShowCheckoutSheet(false) },
                onConfirmOrder = { name, phone, option, address, notes, payment ->
                    viewModel.placeOrder(
                        context = context,
                        customerName = name,
                        customerPhone = phone,
                        deliveryOption = option,
                        address = address,
                        notes = notes,
                        paymentMethod = payment
                    )
                },
                onCallHelpline = { viewModel.callStore(context) }
            )
        }

        // Seller Registration Modal Sheet
        if (showSellerRegistrationSheet) {
            SellerRegistrationSheet(
                isPunjabi = isPunjabi,
                onDismiss = { viewModel.setShowSellerRegistration(false) },
                onSubmitSeller = { seller -> viewModel.registerSeller(seller) }
            )
        }

        // Trilingual AI Shopping & Tracking Assistant Dialog
        if (showAiAssistant) {
            AiShoppingAssistantDialog(
                allProducts = rawProducts,
                orders = orders,
                isPunjabi = isPunjabi,
                onDismiss = { viewModel.setShowAiAssistant(false) },
                onSelectProduct = { prod ->
                    viewModel.selectProductForDetail(prod)
                },
                onAddToCart = { prod ->
                    viewModel.addToCart(prod, 1)
                }
            )
        }
    }
}
