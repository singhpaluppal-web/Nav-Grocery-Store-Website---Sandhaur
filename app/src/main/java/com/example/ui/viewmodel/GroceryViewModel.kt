package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.SmartHelperAiService
import com.example.data.auth.GoogleAuthManager
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.DeliveryOption
import com.example.data.model.FirestoreOrderRecord
import com.example.data.model.HeroBannerSlide
import com.example.data.model.MarketplaceCategory
import com.example.data.model.Order
import com.example.data.model.PaymentMethod
import com.example.data.model.ParchiRequest
import com.example.data.model.PriceFilterRange
import com.example.data.model.Product
import com.example.data.model.RashanSubscription
import com.example.data.model.SavedAddress
import com.example.data.model.ShoppingQuest
import com.example.data.model.SmartComboHamper
import com.example.data.model.SpinWheelSlice
import com.example.data.model.StoreDeliveryConfig
import com.example.data.model.StoreHeroPromoConfig
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StorePaymentsConfig
import com.example.data.model.StoreProfile
import com.example.data.model.UserAccount
import com.example.data.notifications.NavKartFirebaseMessagingService
import com.example.data.repository.CustomerAccountRepository
import com.example.data.repository.GroceryRepository
import com.example.ui.components.WebShieldValidator
import com.example.data.security.CheckoutPricingEngine
import com.example.data.security.InputSanitizer
import com.example.data.security.OrderStatusChange
import com.example.data.security.OrderStatusTracker
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface SmartHelperUiState {
    data object Idle : SmartHelperUiState
    data object Loading : SmartHelperUiState
    data class Streaming(val partialText: String) : SmartHelperUiState
    data object Success : SmartHelperUiState
    data class Error(val message: String) : SmartHelperUiState
}

data class SmartHelperChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER" | "AI"
    val message: String,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val suggestedProducts: List<Product> = emptyList(),
    val trackedCloudOrder: FirestoreOrderRecord? = null,
    val trackedLocalOrder: Order? = null,
    val showWhatsAppEscalation: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class GroceryViewModel(
    private val repository: GroceryRepository,
    private val customerRepository: CustomerAccountRepository,
    private val googleAuthManager: GoogleAuthManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All Products")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedDepartmentScope = MutableStateFlow("ALL") // ALL | GROCERY | SUPERMALL
    val selectedDepartmentScope: StateFlow<String> = _selectedDepartmentScope.asStateFlow()

    private val _selectedBrand = MutableStateFlow("All")
    val selectedBrand: StateFlow<String> = _selectedBrand.asStateFlow()

    private val _selectedSort = MutableStateFlow("Default")
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()

    private val _selectedPriceRange = MutableStateFlow(PriceFilterRange.ALL)
    val selectedPriceRange: StateFlow<PriceFilterRange> = _selectedPriceRange.asStateFlow()

    private val _selectedProductForDetail = MutableStateFlow<Product?>(null)
    val selectedProductForDetail: StateFlow<Product?> = _selectedProductForDetail.asStateFlow()

    private val _showCheckoutSheet = MutableStateFlow(false)
    val showCheckoutSheet: StateFlow<Boolean> = _showCheckoutSheet.asStateFlow()

    private val _showParchiDialog = MutableStateFlow(false)
    val showParchiDialog: StateFlow<Boolean> = _showParchiDialog.asStateFlow()

    private val _showWishlistSheet = MutableStateFlow(false)
    val showWishlistSheet: StateFlow<Boolean> = _showWishlistSheet.asStateFlow()

    private val _showLocationPicker = MutableStateFlow(false)
    val showLocationPicker: StateFlow<Boolean> = _showLocationPicker.asStateFlow()

    private val _showLegalAndSafety = MutableStateFlow(false)
    val showLegalAndSafety: StateFlow<Boolean> = _showLegalAndSafety.asStateFlow()

    private val _showAiAssistant = MutableStateFlow(false)
    val showAiAssistant: StateFlow<Boolean> = _showAiAssistant.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: Home, 1: Categories, 2: Deals, 3: Account, 4: Cart
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _wishlistIds = MutableStateFlow<List<String>>(emptyList())
    val wishlistIds: StateFlow<List<String>> = _wishlistIds.asStateFlow()

    private val _recentlyViewedCategories = MutableStateFlow<List<String>>(
        listOf("Packaged Groceries", "Cold Chain Frozen Foods", "Electronics and Mobiles", "Clothing and Apparel")
    )
    val recentlyViewedCategories: StateFlow<List<String>> = _recentlyViewedCategories.asStateFlow()

    private val _selectedLocationLabel = MutableStateFlow(StoreProfile.LOCATION_SHORT)
    val selectedLocationLabel: StateFlow<String> = _selectedLocationLabel.asStateFlow()

    private val _applySpinCouponInCart = MutableStateFlow(true)
    val applySpinCouponInCart: StateFlow<Boolean> = _applySpinCouponInCart.asStateFlow()

    private val _coinsToRedeemInCart = MutableStateFlow(0)
    val coinsToRedeemInCart: StateFlow<Int> = _coinsToRedeemInCart.asStateFlow()

    private val _applyWalletInCart = MutableStateFlow(false)
    val applyWalletInCart: StateFlow<Boolean> = _applyWalletInCart.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _lastWonSlice = MutableStateFlow<SpinWheelSlice?>(null)
    val lastWonSlice: StateFlow<SpinWheelSlice?> = _lastWonSlice.asStateFlow()

    private val _spinStatusMessage = MutableStateFlow<String?>(null)
    val spinStatusMessage: StateFlow<String?> = _spinStatusMessage.asStateFlow()

    private val _latestCompletedOrder = MutableStateFlow<Order?>(null)
    val latestCompletedOrder: StateFlow<Order?> = _latestCompletedOrder.asStateFlow()

    private val _isSubmittingOrder = MutableStateFlow(false)
    val isSubmittingOrder: StateFlow<Boolean> = _isSubmittingOrder.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    private val _activeGatewaySession = MutableStateFlow<Pair<String, String>?>(null)
    val activeGatewaySession: StateFlow<Pair<String, String>?> = _activeGatewaySession.asStateFlow()

    private val orderStatusTracker = OrderStatusTracker()
    private val _orderStatusEvents = MutableSharedFlow<OrderStatusChange>(extraBufferCapacity = 16)
    val orderStatusEvents: SharedFlow<OrderStatusChange> = _orderStatusEvents.asSharedFlow()

    private var lastOrderSubmittedAtMs: Long = 0L

    // Live Catalog, Dynamic Categories, Remote Store Pricing/Offer/EMI/Delivery Configs & Banners
    val rawProducts: StateFlow<List<Product>> = repository.products
    val dynamicCategories: StateFlow<List<MarketplaceCategory>> = repository.dynamicCategories
    val storeOffersConfig: StateFlow<StoreOffersConfig> = repository.storeOffersConfig
    val storePaymentsConfig: StateFlow<StorePaymentsConfig> = repository.storePaymentsConfig
    val storeDeliveryConfig: StateFlow<StoreDeliveryConfig> = repository.storeDeliveryConfig
    val storeHeroPromoConfig: StateFlow<StoreHeroPromoConfig> = repository.storeHeroPromoConfig
    val heroBanners: StateFlow<List<HeroBannerSlide>> = repository.heroBanners
    val marqueeText: StateFlow<String> = repository.marqueeText
    val cart: StateFlow<List<CartItem>> = repository.cart
    val orders: StateFlow<List<Order>> = repository.orders
    val isRefreshing: StateFlow<Boolean> = repository.isRefreshing
    val appLanguage: StateFlow<AppLanguage> = repository.appLanguage
    val isPunjabi: StateFlow<Boolean> = repository.isPunjabi
    val isTestModeActive: StateFlow<Boolean> = repository.isTestModeActive

    // Firebase AI Logic Smart Helper (Lazy initialized)
    private val smartHelperAiService by lazy { SmartHelperAiService() }
    private var smartHelperJob: Job? = null
    private var lastSmartHelperPrompt: String? = null

    private val _smartHelperMessages = MutableStateFlow<List<SmartHelperChatMessage>>(emptyList())
    val smartHelperMessages: StateFlow<List<SmartHelperChatMessage>> = _smartHelperMessages.asStateFlow()

    private val _smartHelperUiState = MutableStateFlow<SmartHelperUiState>(SmartHelperUiState.Idle)
    val smartHelperUiState: StateFlow<SmartHelperUiState> = _smartHelperUiState.asStateFlow()

    val categoryDiscounts: StateFlow<Map<String, Double>> = dynamicCategories.map { list ->
        list.mapNotNull { cat ->
            cat.discountPercent?.let { cat.name to it }
        }.toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val categoryReturnPolicies: StateFlow<Map<String, String>> = dynamicCategories.map { list ->
        list.mapNotNull { cat ->
            cat.returnPolicy.trim().takeIf { it.isNotBlank() }?.let { cat.name to it }
        }.toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Firebase Auth + Firestore + RTDB reactive customer streams
    val currentUser: StateFlow<FirebaseUser?> = googleAuthManager.authStateFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, googleAuthManager.currentUser)

    val userAccount: StateFlow<UserAccount?> = currentUser
        .flatMapLatest { user -> customerRepository.observeUserAccount(user?.uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedAddresses: StateFlow<List<SavedAddress>> = currentUser
        .flatMapLatest { user -> customerRepository.observeSavedAddresses(user?.uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudOrders: StateFlow<List<FirestoreOrderRecord>> = currentUser
        .flatMapLatest { user -> customerRepository.observeCloudOrders(user?.uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parchiRequests: StateFlow<List<ParchiRequest>> = currentUser
        .flatMapLatest { user -> customerRepository.observeParchiRequests(user?.uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rashanSubscription: StateFlow<RashanSubscription?> = currentUser
        .flatMapLatest { user -> customerRepository.observeRashanSubscription(user?.uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val firestoreQuestRecords: StateFlow<List<ShoppingQuest>> = currentUser
        .flatMapLatest { user -> customerRepository.observeQuests(user?.uid) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CustomerAccountRepository.buildDefaultQuests(null, emptyList())
        )

    val shoppingQuests: StateFlow<List<ShoppingQuest>> = combine(
        firestoreQuestRecords,
        userAccount,
        savedAddresses
    ) { remoteQuests, account, addresses ->
        val computed = CustomerAccountRepository.buildDefaultQuests(account, addresses)
        val remoteById = remoteQuests.associateBy { it.id }
        computed.map { base ->
            val r = remoteById[base.id]
            val mergedProg = maxOf(base.progress, r?.progress ?: 0).coerceAtMost(base.target)
            base.copy(
                progress = mergedProg,
                completed = mergedProg >= base.target,
                claimed = r?.claimed ?: base.claimed
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CustomerAccountRepository.buildDefaultQuests(null, emptyList())
    )

    val profileCompletionPercent: StateFlow<Int> = combine(
        currentUser,
        userAccount,
        savedAddresses
    ) { user, account, addresses ->
        if (user == null) 0
        else {
            var score = 35
            if (!user.displayName.isNullOrBlank()) score += 15
            if (addresses.isNotEmpty()) score += 25
            if (!account?.phone.isNullOrBlank() || addresses.any { it.mobile.length == 10 }) score += 15
            if ((account?.totalOrdersCount ?: 0L) > 0L) score += 10
            score.coerceIn(0, 100)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            repository.refreshProductsFromCloud()
        }
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    customerRepository.ensureUserAccount(user)
                }
            }
        }
        viewModelScope.launch {
            currentUser
                .map { it?.uid }
                .distinctUntilChanged()
                .flatMapLatest { uid ->
                    orderStatusTracker.reset(uid)
                    if (uid.isNullOrBlank()) {
                        flowOf(null to emptyList())
                    } else {
                        customerRepository.observeCloudOrders(uid).map { uid to it }
                    }
                }
                .collect { (uid, orders) ->
                    val changes = orderStatusTracker.onOrdersSnapshot(uid, orders)
                    changes.forEach { _orderStatusEvents.tryEmit(it) }
                }
        }
    }

    val cartTotalCount: StateFlow<Int> = repository.cart.map { list ->
        list.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartTotalMrp: StateFlow<Int> = repository.cart.map { list ->
        list.sumOf { it.totalMrp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartTotalSalePrice: StateFlow<Int> = combine(
        repository.cart,
        storeOffersConfig,
        categoryDiscounts
    ) { list, offers, catDiscs ->
        list.sumOf { it.totalSalePriceFor(catDiscs[it.product.category], offers) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartTotalSavings: StateFlow<Int> = combine(
        repository.cart,
        storeOffersConfig,
        categoryDiscounts
    ) { list, offers, catDiscs ->
        list.sumOf { it.totalSavingsFor(catDiscs[it.product.category], offers) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val hasColdChainInCart: StateFlow<Boolean> = repository.cart.map { list ->
        list.any { it.product.isColdChain }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Dynamic A-to-Z categories merged live from RTDB `/categories`, `/products`, and fallback defaults.
     * Supports any category pushed from the web backend without app updates.
     */
    val availableCategories: StateFlow<List<String>> = combine(
        dynamicCategories,
        repository.products
    ) { remoteCats, productList ->
        val remoteNames = remoteCats.map { it.name.trim() }.filter { it.isNotBlank() }
        val productCats = productList.map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        (listOf("All Products") + remoteNames + StoreProfile.MARKETPLACE_CATEGORIES + productCats)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreProfile.MARKETPLACE_CATEGORIES)

    val availableBrands: StateFlow<List<String>> = combine(
        repository.products,
        _selectedCategory,
        _selectedDepartmentScope
    ) { list, cat, scope ->
        val scopedList = filterByScopeAndCategory(list, scope, cat)
        val invalidBrandRegex = Regex("(?i)^(baking|powder|salt|oil|flour|grams|pack|g|kg|ml|l)$")
        val extractedBrands = scopedList.map { it.brand.trim() }
            .filter { it.isNotBlank() && !it.matches(invalidBrandRegex) && it.length > 1 }
            .distinctBy { it.lowercase() }

        val priorityBrands = listOf(
            "Amul", "Ariel", "Bajaj", "Boost", "Bournvita", "Britannia", "Aashirvaad",
            "Tata", "Fortune", "Nestle", "Patanjali", "Safal", "McCain", "MDH",
            "Catch", "Verka", "Everest", "Knorr", "Rin", "Dettol", "Prestige",
            "boAt", "Sony", "JBL", "Puma", "Bata", "Samsung", "Apple"
        )

        val topBrands = priorityBrands.filter { pb -> extractedBrands.any { it.equals(pb, ignoreCase = true) } }
        val otherBrands = extractedBrands
            .filter { eb -> priorityBrands.none { it.equals(eb, ignoreCase = true) } }
            .sortedBy { it.lowercase() }

        (listOf("All") + topBrands + otherBrands).distinctBy { it.lowercase() }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        listOf("All", "Amul", "Aashirvaad", "Tata", "Verka", "Fortune", "Nestle", "McCain", "boAt", "Puma", "Sony")
    )

    val filteredProducts: StateFlow<List<Product>> = combine(
        repository.products,
        _searchQuery,
        _selectedCategory,
        _selectedBrand,
        _selectedSort,
        _selectedDepartmentScope,
        _selectedPriceRange,
        storeOffersConfig,
        categoryDiscounts
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val products = values[0] as List<Product>
        val query = values[1] as String
        val cat = values[2] as String
        val brand = values[3] as String
        val sort = values[4] as String
        val scope = values[5] as String
        val priceRange = values[6] as PriceFilterRange
        val offers = values[7] as StoreOffersConfig
        @Suppress("UNCHECKED_CAST")
        val catDiscs = values[8] as Map<String, Double>

        var result = filterByScopeAndCategory(products, scope, cat)

        if (brand != "All") {
            result = result.filter { it.brand.equals(brand, ignoreCase = true) }
        }

        if (priceRange != PriceFilterRange.ALL) {
            result = result.filter { product ->
                val price = product.sellingPrice(catDiscs[product.category], offers)
                priceRange.matches(price)
            }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.id.lowercase().contains(q) ||
                    it.name.lowercase().contains(q) ||
                    it.brand.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.department.lowercase().contains(q) ||
                    it.sellerName.lowercase().contains(q) ||
                    it.barcode.lowercase().contains(q) ||
                    it.description.lowercase().contains(q)
            }
        }

        when (sort) {
            "Price: Low to High", "PriceLowHigh" -> result.sortedBy {
                it.sellingPrice(catDiscs[it.category], offers)
            }
            "Price: High to Low", "PriceHighLow" -> result.sortedByDescending {
                it.sellingPrice(catDiscs[it.category], offers)
            }
            "Biggest Discount", "DiscountHigh" -> result.sortedByDescending {
                it.savingsAmountFor(catDiscs[it.category], offers)
            }
            "Name: A to Z" -> result.sortedBy { it.name.lowercase() }
            "Brand: A to Z", "Brand" -> result.sortedWith(
                compareBy<Product> { it.brand.lowercase() }.thenBy { it.name.lowercase() }
            )
            else -> result.sortedBy { it.id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun filterByScopeAndCategory(products: List<Product>, scope: String, cat: String): List<Product> {
        var list = when (scope) {
            "GROCERY" -> products.filter { !it.isSupermallItem }
            "SUPERMALL" -> products.filter { it.isSupermallItem }
            else -> products
        }
        if (cat != "All" && cat != "All Products") {
            list = list.filter {
                if (cat.equals("Packaged Groceries", ignoreCase = true) || cat.equals("Grocery & Staples", ignoreCase = true)) {
                    it.department.equals("Packaged Groceries", ignoreCase = true) ||
                        it.category.equals("Packaged Groceries", ignoreCase = true) ||
                        it.category.equals("Grocery & Staples", ignoreCase = true)
                } else if (cat.contains("Frozen", ignoreCase = true)) {
                    it.category.contains("Frozen", ignoreCase = true) || it.department.contains("Frozen", ignoreCase = true)
                } else if (cat.equals("Dairy and Ghee", ignoreCase = true) || cat.equals("Desi Ghee & Oils", ignoreCase = true)) {
                    it.category.contains("Ghee", ignoreCase = true) ||
                        it.category.contains("Dairy", ignoreCase = true) ||
                        it.name.contains("Ghee", ignoreCase = true) ||
                        it.name.contains("Butter", ignoreCase = true) ||
                        it.name.contains("Paneer", ignoreCase = true)
                } else if (cat.equals("Pantry Staples", ignoreCase = true) || cat.equals("Atta & Dals", ignoreCase = true)) {
                    it.category.contains("Atta", ignoreCase = true) ||
                        it.category.contains("Dal", ignoreCase = true) ||
                        it.category.contains("Spice", ignoreCase = true) ||
                        it.category.equals("Packaged Groceries", ignoreCase = true)
                } else if (cat.equals("Snacks and Beverages", ignoreCase = true)) {
                    it.name.contains("Biscuit", ignoreCase = true) ||
                        it.name.contains("Tea", ignoreCase = true) ||
                        it.name.contains("Coffee", ignoreCase = true) ||
                        it.name.contains("Juice", ignoreCase = true) ||
                        it.name.contains("Noodle", ignoreCase = true) ||
                        it.category.contains("Snack", ignoreCase = true) ||
                        it.category.contains("Beverage", ignoreCase = true)
                } else if (cat.equals("Household Cleaning", ignoreCase = true) || cat.equals("Soaps & Household", ignoreCase = true)) {
                    it.name.contains("Detergent", ignoreCase = true) ||
                        it.name.contains("Surf", ignoreCase = true) ||
                        it.name.contains("Ariel", ignoreCase = true) ||
                        it.name.contains("Soap", ignoreCase = true) ||
                        it.name.contains("Harpic", ignoreCase = true) ||
                        it.name.contains("Lizol", ignoreCase = true) ||
                        it.category.contains("Household", ignoreCase = true)
                } else if (cat.equals("Cosmetics & Personal Care", ignoreCase = true)) {
                    it.category.contains("Personal Care", ignoreCase = true) ||
                        it.category.contains("Cosmetic", ignoreCase = true) ||
                        it.department.contains("Personal Care", ignoreCase = true)
                } else if (cat.equals("Mobiles and Accessories", ignoreCase = true)) {
                    it.category.contains("Electronic", ignoreCase = true) ||
                        it.name.contains("Phone", ignoreCase = true) ||
                        it.name.contains("5G", ignoreCase = true) ||
                        it.name.contains("Charger", ignoreCase = true) ||
                        it.name.contains("Buds", ignoreCase = true) ||
                        it.name.contains("Watch", ignoreCase = true)
                } else if (cat.equals("Fashion", ignoreCase = true)) {
                    it.category.contains("Clothing", ignoreCase = true) ||
                        it.category.contains("Apparel", ignoreCase = true) ||
                        it.category.contains("Footwear", ignoreCase = true) ||
                        it.category.contains("Shoe", ignoreCase = true)
                } else {
                    it.category.equals(cat, ignoreCase = true) ||
                        it.department.equals(cat, ignoreCase = true)
                }
            }
        }
        return list
    }

    // --- Authentication ---

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authErrorMessage.value = null
            val res = googleAuthManager.signInInteractive(context)
            res.onSuccess { user ->
                customerRepository.ensureUserAccount(user)
                NavKartFirebaseMessagingService.fetchAndSyncCurrentToken(context)
                Toast.makeText(context, "Signed in as ${user.email ?: user.displayName}", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Google Sign-In was cancelled or failed."
                _authErrorMessage.value = msg
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun syncFcmNotifications(context: Context) {
        NavKartFirebaseMessagingService.fetchAndSyncCurrentToken(context)
    }

    fun signOutGoogle(context: Context) {
        viewModelScope.launch {
            googleAuthManager.signOut(context)
            _authErrorMessage.value = null
            _spinStatusMessage.value = null
            _lastWonSlice.value = null
            _coinsToRedeemInCart.value = 0
            _applyWalletInCart.value = false
            Toast.makeText(context, "Signed out safely", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelOrder(context: Context, orderId: String) {
        val uid = currentUser.value?.uid
        val lang = appLanguage.value
        viewModelScope.launch {
            if (uid != null) {
                customerRepository.cancelCustomerOrder(uid, orderId)
                    .onSuccess {
                        repository.markOrderCancelledLocally(orderId)
                        Toast.makeText(
                            context,
                            lang.tr(
                                english = "Order $orderId cancelled successfully.",
                                punjabi = "ਆਰਡਰ $orderId ਸਫਲਤਾਪੂਰਵਕ ਰੱਦ ਕਰ ਦਿੱਤਾ ਗਿਆ ਹੈ।",
                                hindi = "ऑर्डर $orderId सफलतापूर्वक रद्द कर दिया गया है।"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .onFailure { err ->
                        Toast.makeText(
                            context,
                            err.localizedMessage ?: "Unable to cancel order $orderId",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } else {
                repository.markOrderCancelledLocally(orderId)
                Toast.makeText(
                    context,
                    lang.tr(
                        english = "Order $orderId cancelled.",
                        punjabi = "ਆਰਡਰ $orderId ਰੱਦ ਹੋ ਗਿਆ।",
                        hindi = "ऑर्डर $orderId रद्द कर दिया गया।"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun deleteAccountAndData(context: Context, onComplete: () -> Unit = {}) {
        val uid = currentUser.value?.uid
        if (uid == null) {
            Toast.makeText(context, "Sign in with Google first", Toast.LENGTH_SHORT).show()
            return
        }
        val lang = appLanguage.value
        _isDeletingAccount.value = true
        viewModelScope.launch {
            try {
                val res = customerRepository.deleteCustomerAccountAndData(uid)
                res.onSuccess {
                    repository.purgeUserData("")
                    googleAuthManager.signOut(context)
                    _authErrorMessage.value = null
                    _spinStatusMessage.value = null
                    _lastWonSlice.value = null
                    _coinsToRedeemInCart.value = 0
                    _applyWalletInCart.value = false
                    Toast.makeText(
                        context,
                        lang.tr(
                            english = "Account profile, addresses & cart deleted permanently. Signed out.",
                            punjabi = "ਤੁਹਾਡਾ ਖਾਤਾ, ਪਤੇ ਅਤੇ ਕਾਰਟ ਡਾਟਾ ਹਮੇਸ਼ਾ ਲਈ ਹਟਾ ਦਿੱਤਾ ਗਿਆ ਹੈ।",
                            hindi = "आपका खाता, पते और कार्ट डेटा स्थायी रूप से हटा दिया गया है।"
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        err.localizedMessage ?: "Account deletion failed. Please try signing in again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                _isDeletingAccount.value = false
            }
        }
    }

    // --- Saved Delivery Addresses ---

    fun saveDeliveryAddress(
        context: Context,
        id: String?,
        fullName: String,
        mobile: String,
        streetAddress: String,
        landmark: String,
        villageOrCity: String,
        pinCode: String,
        makeDefault: Boolean
    ) {
        val uid = currentUser.value?.uid
        if (uid == null) {
            Toast.makeText(context, "Please sign in with Google to save delivery addresses", Toast.LENGTH_SHORT).show()
            return
        }
        val cleanName = InputSanitizer.sanitizeName(fullName)
        val cleanMobile = InputSanitizer.sanitizePhone10(mobile)
        val cleanStreet = InputSanitizer.sanitizeText(streetAddress, 200)
        val cleanLandmark = InputSanitizer.sanitizeText(landmark, 120)
        val cleanVillage = InputSanitizer.sanitizeText(villageOrCity, 120).ifBlank { StoreProfile.DEFAULT_CITY_STATE }
        val cleanPin = InputSanitizer.sanitizePin6(pinCode).ifBlank { StoreProfile.DEFAULT_PIN_CODE }

        if (cleanName.length < 2 || !InputSanitizer.isValidIndianMobile(cleanMobile) || cleanStreet.length < 4 || !InputSanitizer.isValidPin6(cleanPin)) {
            Toast.makeText(context, "Please enter valid Name, 10-digit Indian mobile, Street & 6-digit PIN", Toast.LENGTH_LONG).show()
            return
        }

        viewModelScope.launch {
            customerRepository.saveAddress(
                uid = uid,
                addressId = id,
                fullName = cleanName,
                mobile = cleanMobile,
                streetAddress = cleanStreet,
                landmark = cleanLandmark,
                villageOrCity = cleanVillage,
                pinCode = cleanPin,
                makeDefault = makeDefault
            ).onSuccess {
                if (makeDefault) {
                    _selectedLocationLabel.value = "${cleanStreet.take(20)}, $cleanPin"
                }
                Toast.makeText(context, "Delivery address saved!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, it.localizedMessage ?: "Failed to save address", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteDeliveryAddress(context: Context, addressId: String) {
        val uid = currentUser.value?.uid ?: return
        viewModelScope.launch {
            customerRepository.deleteAddress(uid, addressId).onSuccess {
                Toast.makeText(context, "Address deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setDefaultDeliveryAddress(addressId: String) {
        val uid = currentUser.value?.uid ?: return
        viewModelScope.launch {
            customerRepository.setDefaultAddress(uid, addressId)
            val chosen = savedAddresses.value.firstOrNull { it.id == addressId }
            if (chosen != null) {
                _selectedLocationLabel.value = "${chosen.streetAddress.take(20)}, ${chosen.pinCode}"
            }
        }
    }

    // --- Parchi Photo / 15s Voice Order, Mahina Rashan Box, Wallet & Referrals ---

    fun submitParchiOrder(
        context: Context,
        mode: String,
        notesText: String,
        voiceTranscript: String,
        photoDataUrl: String,
        phoneInput: String
    ) {
        val user = currentUser.value
        if (user == null) {
            Toast.makeText(context, "Please sign in with Google to send a Parchi / Voice Order", Toast.LENGTH_LONG).show()
            return
        }
        val cleanPhone = InputSanitizer.sanitizePhone10(
            phoneInput.ifBlank { userAccount.value?.phone ?: savedAddresses.value.firstOrNull()?.mobile ?: StoreProfile.RAW_PHONE }
        )
        val cleanNotes = InputSanitizer.sanitizeText(notesText, 600)
        val cleanVoice = InputSanitizer.sanitizeText(voiceTranscript, 600)
        if (cleanNotes.isBlank() && cleanVoice.isBlank() && photoDataUrl.isBlank()) {
            Toast.makeText(context, "Attach a Parchi photo, voice note, or typed grocery list", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            customerRepository.submitParchiRequest(
                uid = user.uid,
                customerName = InputSanitizer.sanitizeName(user.displayName ?: "Customer"),
                customerPhone = cleanPhone,
                mode = mode,
                notesText = cleanNotes,
                voiceTranscript = cleanVoice,
                photoDataUrl = photoDataUrl
            ).onSuccess { reqId ->
                Toast.makeText(context, "Parchi $reqId sent to Main Bazaar Store Desk!", Toast.LENGTH_LONG).show()
                _showParchiDialog.value = false
            }.onFailure { err ->
                Toast.makeText(context, err.localizedMessage ?: "Failed to upload Parchi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun confirmParchiReadyBill(context: Context, reqId: String) {
        val uid = currentUser.value?.uid ?: return
        viewModelScope.launch {
            customerRepository.confirmParchiReadyBill(uid, reqId).onSuccess {
                Toast.makeText(context, "Parchi bill confirmed for express dispatch!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveCurrentCartAsRashanBox(context: Context, monthlyDayOfMonth: Int) {
        val user = currentUser.value
        if (user == null) {
            Toast.makeText(context, "Sign in with Google to save Monthly Mahina Rashan Box", Toast.LENGTH_SHORT).show()
            return
        }
        val items = cart.value
        if (items.isEmpty()) {
            Toast.makeText(context, "Add your monthly staples to Cart first", Toast.LENGTH_SHORT).show()
            return
        }
        val defAddr = savedAddresses.value.firstOrNull { it.defaultAddress } ?: savedAddresses.value.firstOrNull()
        val sub = RashanSubscription(
            uid = user.uid,
            customerName = defAddr?.fullName ?: (user.displayName ?: "Sandhaur Family"),
            customerPhone = defAddr?.mobile ?: userAccount.value?.phone.orEmpty(),
            monthlyDayOfMonth = monthlyDayOfMonth.coerceIn(1, 28),
            addressSnapshot = defAddr?.formattedLine() ?: StoreProfile.LOCATION,
            itemsSummary = items.joinToString(", ") { "${it.product.name} x${it.quantity}" }.take(600),
            productIdsCsv = items.joinToString(",") { "${it.product.id}:${it.quantity}:${if (it.isPetiCarton) 1 else 0}" }.take(600),
            estimatedTotal = cartTotalSalePrice.value,
            active = true
        )
        viewModelScope.launch {
            customerRepository.saveRashanSubscription(user.uid, sub).onSuccess {
                Toast.makeText(context, "Saved! Priority dispatch on Day $monthlyDayOfMonth", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun reorderMonthlyRashanBox(context: Context) {
        val sub = rashanSubscription.value
        if (sub == null || sub.productIdsCsv.isBlank()) {
            Toast.makeText(context, "Save your Cart as a Mahina Rashan Box first!", Toast.LENGTH_SHORT).show()
            return
        }
        val byId = rawProducts.value.associateBy { it.id }
        var added = 0
        sub.productIdsCsv.split(",").forEach { token ->
            val parts = token.split(":")
            val sku = parts.getOrNull(0)?.trim().orEmpty()
            val qty = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val isPeti = parts.getOrNull(2) == "1"
            val prod = byId[sku]
            if (prod != null) {
                repository.addToCart(prod, qty, isPetiCarton = isPeti)
                added++
            }
        }
        if (added > 0) {
            _activeTab.value = 4
            Toast.makeText(context, "Loaded $added Mahina Rashan Box items into Cart!", Toast.LENGTH_SHORT).show()
        }
    }

    fun addComboHamperToCart(context: Context, hamper: SmartComboHamper) {
        val all = rawProducts.value
        val byId = all.associateBy { it.id }
        var addedCount = 0
        hamper.skuIds.forEach { sku ->
            val match = byId[sku] ?: all.firstOrNull { !it.isSupermallItem }
            if (match != null) {
                repository.addToCart(match, 1)
                addedCount++
            }
        }
        Toast.makeText(context, "${hamper.title} ($addedCount SKUs) added to Cart!", Toast.LENGTH_SHORT).show()
    }

    fun reorderCloudOrder(context: Context, order: FirestoreOrderRecord) {
        val byId = rawProducts.value.associateBy { it.id }
        var addedCount = 0
        var skippedOutOfStock = 0
        order.items.forEach { item ->
            val catalogProduct = byId[item.productId]
            if (catalogProduct != null && catalogProduct.stock > 0) {
                val isPeti = item.variant.contains("Peti Carton", ignoreCase = true) && catalogProduct.isBulkEligible
                val qty = item.qty.coerceIn(1, 50)
                repository.addToCart(catalogProduct, qty, isPetiCarton = isPeti)
                addedCount++
            } else {
                skippedOutOfStock++
            }
        }
        val lang = appLanguage.value
        val msg = when {
            addedCount == 0 -> lang.tr(
                english = "Items from ${order.orderId} are currently out of stock.",
                punjabi = "ਇਸ ਆਰਡਰ ਦੀਆਂ ਸਾਰੀਆਂ ਚੀਜ਼ਾਂ ਸਟਾਕ ਤੋਂ ਬਾਹਰ ਹਨ।",
                hindi = "इस ऑर्डर के सभी उत्पाद फिलहाल स्टॉक में उपलब्ध नहीं हैं।"
            )
            skippedOutOfStock > 0 -> lang.tr(
                english = "Added $addedCount in-stock item(s) to cart ($skippedOutOfStock out of stock skipped).",
                punjabi = "$addedCount ਚੀਜ਼ਾਂ ਕਾਰਟ ਵਿੱਚ ਸ਼ਾਮਲ ($skippedOutOfStock ਸਟਾਕ ਤੋਂ ਬਾਹਰ)!",
                hindi = "$addedCount उपलब्ध आइटम कार्ट में जोड़े गए ($skippedOutOfStock स्टॉक से बाहर थे)।"
            )
            else -> lang.tr(
                english = "Reordered $addedCount item(s) from ${order.orderId} into Cart!",
                punjabi = "${order.orderId} ਦੀਆਂ $addedCount ਚੀਜ਼ਾਂ ਕਾਰਟ ਵਿੱਚ ਸ਼ਾਮਲ!",
                hindi = "${order.orderId} के $addedCount आइटम कार्ट में जोड़े गए!"
            )
        }
        if (addedCount > 0) _activeTab.value = 4
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    fun reorderLocalOrder(context: Context, order: Order) {
        val byId = rawProducts.value.associateBy { it.id }
        var addedCount = 0
        var skippedOutOfStock = 0
        order.items.forEach { cartItem ->
            val liveProduct = byId[cartItem.product.id] ?: cartItem.product
            if (liveProduct.stock > 0) {
                repository.addToCart(
                    liveProduct,
                    cartItem.quantity.coerceIn(1, 50),
                    selectedSize = cartItem.selectedSize,
                    selectedColor = cartItem.selectedColor,
                    isPetiCarton = cartItem.isPetiCarton && liveProduct.isBulkEligible
                )
                addedCount++
            } else {
                skippedOutOfStock++
            }
        }
        val lang = appLanguage.value
        val msg = when {
            addedCount == 0 -> lang.tr(
                english = "Items from ${order.orderId} are currently out of stock.",
                punjabi = "ਇਸ ਆਰਡਰ ਦੀਆਂ ਸਾਰੀਆਂ ਚੀਜ਼ਾਂ ਸਟਾਕ ਤੋਂ ਬਾਹਰ ਹਨ।",
                hindi = "इस ऑर्डर के सभी उत्पाद फिलहाल स्टॉक में उपलब्ध नहीं हैं।"
            )
            skippedOutOfStock > 0 -> lang.tr(
                english = "Added $addedCount in-stock item(s) to cart ($skippedOutOfStock out of stock skipped).",
                punjabi = "$addedCount ਚੀਜ਼ਾਂ ਕਾਰਟ ਵਿੱਚ ਸ਼ਾਮਲ ($skippedOutOfStock ਸਟਾਕ ਤੋਂ ਬਾਹਰ)!",
                hindi = "$addedCount उपलब्ध आइटम कार्ट में जोड़े गए ($skippedOutOfStock स्टॉक से बाहर थे)।"
            )
            else -> lang.tr(
                english = "Reordered $addedCount item(s) from ${order.orderId} into Cart!",
                punjabi = "${order.orderId} ਦੀਆਂ $addedCount ਚੀਜ਼ਾਂ ਕਾਰਟ ਵਿੱਚ ਸ਼ਾਮਲ!",
                hindi = "${order.orderId} के $addedCount आइटम कार्ट में जोड़े गए!"
            )
        }
        if (addedCount > 0) _activeTab.value = 4
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    fun requestWalletTopup(context: Context, amountRupees: Int, utrRef: String) {
        val user = currentUser.value ?: run {
            Toast.makeText(context, "Please sign in with Google first", Toast.LENGTH_SHORT).show()
            return
        }
        val cleanUtr = InputSanitizer.sanitizeText(utrRef, 60)
        viewModelScope.launch {
            customerRepository.requestWalletTopup(user.uid, user.displayName ?: "Customer", amountRupees, cleanUtr)
                .onSuccess { reqId ->
                    Toast.makeText(context, "Wallet load request $reqId submitted for verification!", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, err.localizedMessage ?: "Wallet request failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun applyReferralCode(context: Context, rawCode: String) {
        val uid = currentUser.value?.uid ?: run {
            Toast.makeText(context, "Sign in with Google first", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            customerRepository.applyReferralCode(uid, rawCode)
                .onSuccess { code ->
                    Toast.makeText(context, "Linked $code! Complete your first order to unlock reward coins.", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, err.localizedMessage ?: "Could not apply code", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // --- Spin the Wheel & Shopping Quests ---

    fun spinWheelNow(context: Context, onSliceSettled: (SpinWheelSlice) -> Unit) {
        val uid = currentUser.value?.uid
        if (uid == null) {
            Toast.makeText(context, "Please sign in with Google to spin & win rewards!", Toast.LENGTH_SHORT).show()
            return
        }
        val acct = userAccount.value
        if (acct != null && !acct.canSpinNow()) {
            _spinStatusMessage.value = "Daily spin already used. Come back in 24 hours!"
            Toast.makeText(context, _spinStatusMessage.value, Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _spinStatusMessage.value = null
            customerRepository.performSpinWheel(uid, storeOffersConfig.value).onSuccess { slice ->
                _lastWonSlice.value = slice
                _spinStatusMessage.value = "Won: ${slice.title}!"
                onSliceSettled(slice)
            }.onFailure { err ->
                _spinStatusMessage.value = err.localizedMessage ?: "Spin locked for 24h"
                Toast.makeText(context, _spinStatusMessage.value, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun claimQuestReward(context: Context, quest: ShoppingQuest) {
        val uid = currentUser.value?.uid
        if (uid == null) {
            Toast.makeText(context, "Sign in with Google to claim Nav Coins", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            customerRepository.claimQuestReward(uid, quest, storeOffersConfig.value).onSuccess { coins ->
                Toast.makeText(context, "+$coins Nav Coins added to your rewards!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, err.localizedMessage ?: "Quest not ready yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Cart & Checkout Redemptions ---

    fun setApplySpinCouponInCart(apply: Boolean) { _applySpinCouponInCart.value = apply }
    fun setCoinsToRedeemInCart(coins: Int) {
        _coinsToRedeemInCart.value = if (storeOffersConfig.value.coinConfig.isCoinsEnabled) {
            coins.coerceAtLeast(0)
        } else 0
    }
    fun setApplyWalletInCart(apply: Boolean) { _applyWalletInCart.value = apply }

    fun toggleWishlist(productId: String) {
        val current = _wishlistIds.value.toMutableList()
        if (productId in current) current.remove(productId) else current.add(0, productId)
        _wishlistIds.value = current
    }

    fun setLocationLabel(label: String) {
        _selectedLocationLabel.value = label.ifBlank { StoreProfile.LOCATION_SHORT }
    }

    fun setShowWishlistSheet(show: Boolean) { _showWishlistSheet.value = show }
    fun setShowLocationPicker(show: Boolean) { _showLocationPicker.value = show }
    fun setShowParchiDialog(show: Boolean) { _showParchiDialog.value = show }

    fun setDepartmentScope(scope: String) {
        _selectedDepartmentScope.value = scope
        _selectedCategory.value = "All Products"
        _selectedBrand.value = "All"
    }

    fun toggleTestMode(active: Boolean) { repository.setTestMode(active) }
    fun requestDataDeletion(phone: String) { repository.purgeUserData(phone) }
    fun setShowLegalAndSafety(show: Boolean) { _showLegalAndSafety.value = show }
    fun setShowAiAssistant(show: Boolean) { _showAiAssistant.value = show }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        _selectedBrand.value = "All"
        if (category != "All" && category != "All Products") {
            val recent = _recentlyViewedCategories.value.toMutableList()
            recent.removeAll { it.equals(category, ignoreCase = true) }
            recent.add(0, category)
            _recentlyViewedCategories.value = recent.take(6)
        }
    }

    fun setBrand(brand: String) { _selectedBrand.value = brand }
    fun setSort(sort: String) { _selectedSort.value = sort }
    fun setPriceRange(range: PriceFilterRange) { _selectedPriceRange.value = range }
    fun selectProductForDetail(product: Product?) { _selectedProductForDetail.value = product }
    fun setActiveTab(index: Int) { _activeTab.value = index }
    fun setShowCheckoutSheet(show: Boolean) { _showCheckoutSheet.value = show }

    fun openGatewaySession(title: String, url: String) {
        if (WebShieldValidator.isTrustedHttpsUrl(url)) {
            _activeGatewaySession.value = title to url
        }
    }

    fun dismissGatewaySession() {
        _activeGatewaySession.value = null
    }

    fun addToCart(
        product: Product,
        quantity: Int = 1,
        selectedSize: String = "",
        selectedColor: String = "",
        isPetiCarton: Boolean = false
    ) {
        if (product.stock <= 0) return
        repository.addToCart(product, quantity, selectedSize, selectedColor, isPetiCarton)
    }

    fun updateCartQuantity(
        productId: String,
        quantity: Int,
        selectedSize: String = "",
        selectedColor: String = "",
        isPetiCarton: Boolean? = null
    ) {
        repository.updateCartQuantity(productId, quantity, selectedSize, selectedColor, isPetiCarton)
    }

    fun clearCart() { repository.clearCart() }
    fun setAppLanguage(language: AppLanguage) { repository.setAppLanguage(language) }
    fun toggleLanguage() { repository.toggleLanguage() }
    fun refreshProducts() { viewModelScope.launch { repository.refreshProductsFromCloud() } }
    fun dismissCompletedOrderReceipt() { _latestCompletedOrder.value = null }

    fun sendSmartHelperPrompt(rawPrompt: String, explicitLanguage: AppLanguage? = null) {
        val trimmed = InputSanitizer.sanitizeText(rawPrompt, 450)
        if (trimmed.isBlank()) return

        val resolvedLang = explicitLanguage ?: AppLanguage.detectFromText(trimmed, appLanguage.value)
        if (trimmed.any { it in '\u0900'..'\u097F' } || trimmed.any { it in '\u0A00'..'\u0A7F' }) {
            repository.setAppLanguage(resolvedLang)
        }

        lastSmartHelperPrompt = trimmed
        val userMessage = SmartHelperChatMessage(
            sender = "USER",
            message = trimmed,
            language = resolvedLang
        )
        val priorHistory = _smartHelperMessages.value.map { it.sender to it.message }
        _smartHelperMessages.value = _smartHelperMessages.value + userMessage
        _smartHelperUiState.value = SmartHelperUiState.Loading

        smartHelperJob?.cancel()
        smartHelperJob = viewModelScope.launch {
            try {
                smartHelperAiService.streamReply(
                    userPrompt = trimmed,
                    preferredLanguage = resolvedLang,
                    historyPairs = priorHistory,
                    allProducts = rawProducts.value,
                    offersConfig = storeOffersConfig.value,
                    categoryDiscounts = categoryDiscounts.value,
                    userAccount = userAccount.value,
                    localOrders = orders.value,
                    cloudOrders = cloudOrders.value
                ).collect { chunk ->
                    when (chunk) {
                        is SmartHelperAiService.StreamChunk.Partial -> {
                            _smartHelperUiState.value = SmartHelperUiState.Streaming(chunk.accumulatedText)
                        }
                        is SmartHelperAiService.StreamChunk.Completed -> {
                            val byId = rawProducts.value.associateBy { it.id.uppercase() }
                            val skuMatches = chunk.recommendedSkuIds
                                .mapNotNull { byId[it.uppercase()] }
                                .take(4)

                            val cleanDigits = trimmed.filter { it.isDigit() }
                            val matchedCloudOrder = cloudOrders.value.firstOrNull { ord ->
                                trimmed.contains(ord.orderId, ignoreCase = true) ||
                                    (cleanDigits.length >= 10 && ord.customerPhone.endsWith(cleanDigits.takeLast(10)))
                            }
                            val matchedLocalOrder = if (matchedCloudOrder == null) {
                                orders.value.firstOrNull { ord ->
                                    trimmed.contains(ord.orderId, ignoreCase = true) ||
                                        (cleanDigits.length >= 10 && ord.customerPhone.endsWith(cleanDigits.takeLast(10)))
                                }
                            } else null

                            val isOrderOrPolicy = trimmed.contains("order", ignoreCase = true) ||
                                trimmed.contains("track", ignoreCase = true) ||
                                trimmed.contains("return", ignoreCase = true) ||
                                trimmed.contains("ऑर्डर") ||
                                trimmed.contains("आर्डर") ||
                                trimmed.contains("ਟਰੈਕ") ||
                                matchedCloudOrder != null ||
                                matchedLocalOrder != null

                            val aiMsg = SmartHelperChatMessage(
                                sender = "AI",
                                message = chunk.cleanText,
                                language = chunk.detectedLanguage,
                                suggestedProducts = skuMatches,
                                trackedCloudOrder = matchedCloudOrder,
                                trackedLocalOrder = matchedLocalOrder,
                                showWhatsAppEscalation = isOrderOrPolicy
                            )
                            _smartHelperMessages.value = _smartHelperMessages.value + aiMsg
                            _smartHelperUiState.value = SmartHelperUiState.Success
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("GroceryViewModel", "Smart Helper timed out", e)
                _smartHelperUiState.value = SmartHelperUiState.Error(
                    resolvedLang.tr(
                        english = "Smart Helper timed out after 60s. Please check your connection and tap Retry.",
                        punjabi = "ਸਮਾਰਟ ਹੈਲਪਰ 60s ਬਾਅਦ ਟਾਈਮ-ਆਊਟ ਹੋ ਗਿਆ। ਕਿਰਪਾ ਕਰਕੇ ਦੁਬਾਰਾ ਕੋਸ਼ਿਸ਼ ਕਰੋ।",
                        hindi = "स्मार्ट हेल्पर 60 सेकंड में जवाब नहीं दे सका। कृपया इंटरनेट जांचें और पुनः प्रयास करें।"
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("GroceryViewModel", "Smart Helper request failed", e)
                _smartHelperUiState.value = SmartHelperUiState.Error(
                    e.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: resolvedLang.tr(
                            english = "Unable to reach Smart Helper right now. Tap Retry to try again.",
                            punjabi = "ਇਸ ਸਮੇਂ ਸਮਾਰਟ ਹੈਲਪਰ ਨਾਲ ਸੰਪਰਕ ਨਹੀਂ ਹੋ ਸਕਿਆ। ਦੁਬਾਰਾ ਕੋਸ਼ਿਸ਼ ਕਰੋ।",
                            hindi = "अभी स्मार्ट हेल्पर से संपर्क नहीं हो पाया। कृपया पुनः प्रयास करें।"
                        )
                )
            }
        }
    }

    fun clearSmartHelperError() {
        if (_smartHelperUiState.value is SmartHelperUiState.Error) {
            _smartHelperUiState.value = SmartHelperUiState.Idle
        }
    }

    fun retryLastSmartHelperPrompt() {
        val last = lastSmartHelperPrompt ?: return
        sendSmartHelperPrompt(last, appLanguage.value)
    }

    fun placeOrder(
        context: Context,
        customerName: String,
        customerPhone: String,
        deliveryOption: DeliveryOption,
        address: String,
        notes: String,
        paymentMethod: PaymentMethod = PaymentMethod.UPI_INTENT,
        dispatchSlot: String = "",
        isNriOrder: Boolean = false,
        useWalletBalance: Boolean = _applyWalletInCart.value || paymentMethod == PaymentMethod.NAVKART_WALLET,
        pinCode: String = ""
    ) {
        if (_isSubmittingOrder.value) {
            Toast.makeText(context, "Order is already being processed…", Toast.LENGTH_SHORT).show()
            return
        }

        val currentCart = cart.value
        if (currentCart.isEmpty()) {
            Toast.makeText(context, "Your cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val nowMs = System.currentTimeMillis()
        if (!CheckoutPricingEngine.canSubmitNow(lastOrderSubmittedAtMs, nowMs)) {
            Toast.makeText(context, "Please wait a moment before placing another order.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = currentUser.value
        val isTestMode = isTestModeActive.value
        if (user == null && !isTestMode) {
            Toast.makeText(context, "Please Sign In with Google at Checkout to place your order.", Toast.LENGTH_LONG).show()
            return
        }

        val acct = userAccount.value

        val cleanName = InputSanitizer.sanitizeName(customerName.ifBlank { user?.displayName ?: "Valued Customer" })
        val cleanPhone = InputSanitizer.sanitizePhone10(customerPhone)
        if (!InputSanitizer.isValidIndianMobile(cleanPhone)) {
            Toast.makeText(context, "Please enter a valid 10-digit Indian mobile number.", Toast.LENGTH_LONG).show()
            return
        }

        val cleanAddress = InputSanitizer.sanitizeText(
            address.ifBlank {
                if (deliveryOption == DeliveryOption.STORE_PICKUP) StoreProfile.LOCATION
                else "Main Bazaar, Sandhaur, District Malerkotla, Punjab 148020"
            },
            300
        )

        val resolvedPin = pinCode.filter { it.isDigit() }.take(6).takeIf { it.length == 6 }
            ?: Regex("\\b(\\d{6})\\b").find(cleanAddress)?.groupValues?.getOrNull(1)
            ?: savedAddresses.value.firstOrNull { it.defaultAddress }?.pinCode
            ?: StoreProfile.DEFAULT_PIN_CODE

        val liveOffers = storeOffersConfig.value
        val liveCatDiscs = categoryDiscounts.value
        val liveDeliveryConfig = storeDeliveryConfig.value
        val summaryResult = CheckoutPricingEngine.verifyAndCompute(
            cartItems = currentCart,
            masterCatalog = rawProducts.value,
            deliveryOption = deliveryOption,
            userAccount = acct,
            applySpinCoupon = _applySpinCouponInCart.value,
            requestedCoinsToRedeem = _coinsToRedeemInCart.value,
            applyWalletBalance = useWalletBalance,
            offersConfig = liveOffers,
            categoryDiscounts = liveCatDiscs,
            deliveryConfig = liveDeliveryConfig,
            pinCode = resolvedPin
        )

        val summary = summaryResult.getOrElse { err ->
            Toast.makeText(context, err.localizedMessage ?: "Order validation error", Toast.LENGTH_LONG).show()
            return
        }

        _isSubmittingOrder.value = true
        lastOrderSubmittedAtMs = nowMs
        val orderPrefix = if (isTestMode) "TEST-" else "NAV-"
        val orderId = orderPrefix + SimpleDateFormat("ddMM-HHmmss", Locale.ENGLISH).format(Date())
        val idempotencyKey = "${orderId}-${UUID.randomUUID().toString().take(8)}"
        val cleanSlot = InputSanitizer.sanitizeText(dispatchSlot, 60)

        val newOrder = Order(
            orderId = orderId,
            timestamp = nowMs,
            customerName = cleanName,
            customerPhone = cleanPhone,
            deliveryType = deliveryOption.name,
            address = cleanAddress,
            items = currentCart,
            totalMrp = summary.totalMrp,
            totalDiscount = summary.wholesaleDiscount + summary.petiDiscountRupees + summary.spinCouponDiscountRupees + summary.coinDiscountRupees,
            deliveryFee = summary.deliveryFee,
            spinCouponRupees = summary.spinCouponDiscountRupees,
            spinCouponCode = summary.spinCouponCode,
            coinsRedeemed = summary.coinsRedeemed,
            petiDiscountRupees = summary.petiDiscountRupees,
            walletDeductedRupees = summary.walletDeductedRupees,
            dispatchSlot = cleanSlot,
            isNriOrder = isNriOrder,
            finalTotal = summary.finalPayableTotal,
            status = if (isTestMode) "Sandbox Test Order" else "Order Placed",
            paymentMethod = paymentMethod.title,
            isPaid = isTestMode || paymentMethod == PaymentMethod.UPI_INTENT || summary.netGatewayPayable == 0,
            isTestModeOrder = isTestMode,
            merchantVpa = StoreProfile.MERCHANT_UPI_VPA,
            integrityDigest = summary.integrityDigest
        )

        if (user != null) {
            viewModelScope.launch {
                try {
                    customerRepository.placeVerifiedCloudOrder(
                        uid = user.uid,
                        orderId = orderId,
                        customerName = cleanName,
                        customerPhone = cleanPhone,
                        customerEmail = user.email ?: "",
                        deliveryOption = deliveryOption,
                        dispatchSlot = cleanSlot,
                        isNriOrder = isNriOrder,
                        addressSnapshot = cleanAddress,
                        paymentMethod = paymentMethod,
                        cartItems = currentCart,
                        summary = summary,
                        idempotencyKey = idempotencyKey,
                        requestedCoinsToRedeem = _coinsToRedeemInCart.value,
                        applySpinCoupon = _applySpinCouponInCart.value,
                        applyWalletBalance = useWalletBalance,
                        offersConfig = liveOffers,
                        categoryDiscounts = liveCatDiscs,
                        pinCode = resolvedPin
                    ).onSuccess { result ->
                        if (!isTestMode && paymentMethod.isGatewayHosted && summary.netGatewayPayable > 0) {
                            val fallbackCheckoutUrl = "${StoreProfile.WEB_URL}/checkout?orderId=${result.orderId}&amount=${summary.netGatewayPayable}&method=${paymentMethod.name}"
                            val sessionUrl = result.paymentSessionUrl
                                ?.takeIf { WebShieldValidator.isTrustedHttpsUrl(it) }
                                ?: fallbackCheckoutUrl
                            openGatewaySession(
                                title = paymentMethod.titleFor(appLanguage.value),
                                url = sessionUrl
                            )
                        }
                    }.onFailure { err ->
                        Toast.makeText(context, "Cloud sync note: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    _isSubmittingOrder.value = false
                }
            }
        } else {
            _isSubmittingOrder.value = false
        }

        repository.recordOrder(newOrder)
        _coinsToRedeemInCart.value = 0
        _applyWalletInCart.value = false
        _showCheckoutSheet.value = false
        _latestCompletedOrder.value = newOrder

        if (!isTestMode && (paymentMethod == PaymentMethod.CASH_ON_DELIVERY || paymentMethod == PaymentMethod.WHATSAPP_ORDER)) {
            val cleanNotes = InputSanitizer.sanitizeText(notes, 160)
            val msg = buildString {
                append("Hello Nav Grocery & NavKart! Order ID: $orderId\n")
                if (isNriOrder) append("[NRI FAMILY ORDER FOR PUNJAB]\n")
                append("Customer: $cleanName ($cleanPhone)\n")
                append("Tier: ${deliveryOption.title}${if (cleanSlot.isNotBlank()) " • $cleanSlot" else ""}\n")
                append("Address: $cleanAddress\n")
                if (cleanNotes.isNotBlank()) append("Notes: $cleanNotes\n")
                currentCart.forEachIndexed { idx, item ->
                    val label = if (item.isPetiCarton) "Peti(${item.petiUnits}x) x${item.quantity}" else "x${item.quantity}"
                    val lineTotal = item.totalSalePriceFor(liveCatDiscs[item.product.category], liveOffers)
                    append("${idx + 1}. [${item.product.id}] ${item.product.name} $label = ₹$lineTotal\n")
                }
                append("FINAL PAYABLE: ₹${summary.finalPayableTotal} (Saved ₹${newOrder.totalDiscount})")
            }
            runCatching {
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=${StoreProfile.WHATSAPP_NUMBER}&text=${URLEncoder.encode(msg, "UTF-8")}")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
        }
    }

    fun callStore(context: Context, secondary: Boolean = false) {
        val number = if (secondary) StoreProfile.SECONDARY_HELPLINE_PHONE else StoreProfile.HELPLINE_PHONE
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun emailSupport(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${StoreProfile.FOOTER_CONTACT_EMAIL}")
            putExtra(Intent.EXTRA_SUBJECT, "Nav Grocery & NavKart Customer Support (Sandhaur 148020)")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun openInstagram(context: Context) = openUrl(context, StoreProfile.INSTAGRAM_URL)
    fun openLocation(context: Context) = openUrl(context, StoreProfile.MAPS_QUERY_URL)
}
