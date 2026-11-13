package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.DeliveryOption
import com.example.data.model.Order
import com.example.data.model.OwnerSecureLedger
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.SellerRegistration
import com.example.data.model.StoreProfile
import com.example.data.repository.GroceryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class GroceryViewModel(
    private val repository: GroceryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All Products")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedBrand = MutableStateFlow("All")
    val selectedBrand: StateFlow<String> = _selectedBrand.asStateFlow()

    private val _selectedSort = MutableStateFlow("Default")
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()

    private val _selectedProductForDetail = MutableStateFlow<Product?>(null)
    val selectedProductForDetail: StateFlow<Product?> = _selectedProductForDetail.asStateFlow()

    private val _showCheckoutSheet = MutableStateFlow(false)
    val showCheckoutSheet: StateFlow<Boolean> = _showCheckoutSheet.asStateFlow()

    private val _showSellerRegistrationSheet = MutableStateFlow(false)
    val showSellerRegistrationSheet: StateFlow<Boolean> = _showSellerRegistrationSheet.asStateFlow()

    private val _showRiderHub = MutableStateFlow(false)
    val showRiderHub: StateFlow<Boolean> = _showRiderHub.asStateFlow()

    private val _showAdminController = MutableStateFlow(false)
    val showAdminController: StateFlow<Boolean> = _showAdminController.asStateFlow()

    private val _showLegalAndSafety = MutableStateFlow(false)
    val showLegalAndSafety: StateFlow<Boolean> = _showLegalAndSafety.asStateFlow()

    private val _showAiAssistant = MutableStateFlow(false)
    val showAiAssistant: StateFlow<Boolean> = _showAiAssistant.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: Catalog, 1: Cart, 2: Orders, 3: Store Info
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    // Room reactive source of truth
    val rawProducts: StateFlow<List<Product>> = repository.products
    val sellers: StateFlow<List<SellerRegistration>> = repository.sellers
    val cart: StateFlow<List<CartItem>> = repository.cart
    val orders: StateFlow<List<Order>> = repository.orders
    val isRefreshing: StateFlow<Boolean> = repository.isRefreshing
    val isPunjabi: StateFlow<Boolean> = repository.isPunjabi
    val isTestModeActive: StateFlow<Boolean> = repository.isTestModeActive

    init {
        viewModelScope.launch {
            repository.refreshProductsFromCloud()
        }
    }

    val cartTotalCount: StateFlow<Int> = repository.cart.combine(repository.cart) { cartItems, _ ->
        cartItems.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartTotalMrp: StateFlow<Int> = repository.cart.combine(repository.cart) { cartItems, _ ->
        cartItems.sumOf { it.totalMrp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartTotalSalePrice: StateFlow<Int> = repository.cart.combine(repository.cart) { cartItems, _ ->
        cartItems.sumOf { it.totalSalePrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartTotalSavings: StateFlow<Int> = repository.cart.combine(repository.cart) { cartItems, _ ->
        cartItems.sumOf { it.totalSavings }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val availableCategories: StateFlow<List<String>> = repository.products.combine(isPunjabi) { list, _ ->
        val productCats = list.map { it.category }.filter { it.isNotBlank() }.distinct()
        val allMarketplace = (StoreProfile.MARKETPLACE_CATEGORIES + productCats).distinct()
        allMarketplace
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreProfile.MARKETPLACE_CATEGORIES)

    val availableBrands: StateFlow<List<String>> = repository.products.combine(_selectedCategory) { list, cat ->
        val filtered = if (cat == "All" || cat == "All Products") list else list.filter {
            if (cat.equals("Packaged Groceries", ignoreCase = true)) {
                it.department.equals("Packaged Groceries", ignoreCase = true) || it.category.equals("Packaged Groceries", ignoreCase = true) || it.category.equals("Grocery & Staples", ignoreCase = true)
            } else if (cat.contains("Frozen", ignoreCase = true)) {
                it.category.contains("Frozen", ignoreCase = true) || it.department.contains("Frozen", ignoreCase = true)
            } else {
                it.category.equals(cat, ignoreCase = true)
            }
        }
        val invalidBrandRegex = Regex(".*\\d+.*|(?i)^(baking|powder|salt|oil|flour|grams|pack|g)$")
        val extractedBrands = filtered.map { it.brand.trim() }
            .filter { it.isNotBlank() && !it.matches(invalidBrandRegex) && it.length > 1 }
            .distinct()

        val priorityBrands = listOf(
            "Amul", "Britannia", "Aashirvaad", "Tata", "Fortune", "Nestle",
            "Patanjali", "Safal", "McCain", "MDH", "Catch", "Verka", "Everest",
            "Knorr", "Rin", "Dettol", "Prestige", "boAt", "Bata"
        )

        val topBrands = priorityBrands.filter { pb -> extractedBrands.any { it.equals(pb, ignoreCase = true) } }
        val otherBrands = extractedBrands.filter { eb -> priorityBrands.none { it.equals(eb, ignoreCase = true) } }.sorted()

        listOf("All") + (topBrands + otherBrands).distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All", "Amul", "Britannia", "Aashirvaad", "Tata", "Fortune", "Nestle", "Patanjali", "Safal", "McCain", "MDH", "Catch", "Verka"))

    val filteredProducts: StateFlow<List<Product>> = combine(
        repository.products,
        _searchQuery,
        _selectedCategory,
        _selectedBrand,
        _selectedSort
    ) { products, query, cat, brand, sort ->
        var result = products

        if (cat != "All" && cat != "All Products") {
            result = result.filter {
                if (cat.equals("Packaged Groceries", ignoreCase = true)) {
                    it.department.equals("Packaged Groceries", ignoreCase = true) || it.category.equals("Packaged Groceries", ignoreCase = true) || it.category.equals("Grocery & Staples", ignoreCase = true)
                } else if (cat.contains("Frozen", ignoreCase = true)) {
                    it.category.contains("Frozen", ignoreCase = true) || it.department.contains("Frozen", ignoreCase = true)
                } else {
                    it.category.equals(cat, ignoreCase = true)
                }
            }
        }

        if (brand != "All") {
            result = result.filter { it.brand.equals(brand, ignoreCase = true) }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.name.lowercase().contains(q) ||
                it.brand.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }

        when (sort) {
            "Price: Low to High" -> result.sortedBy { it.wholesalePrice }
            "Price: High to Low" -> result.sortedByDescending { it.wholesalePrice }
            "Biggest Discount" -> result.sortedByDescending { it.savingsAmount }
            "Name: A to Z" -> result.sortedBy { it.name }
            else -> result
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Product Updates ---

    fun updateProductPrice(productId: String, newMrp: Int) {
        viewModelScope.launch {
            val current = rawProducts.value.firstOrNull { it.id == productId } ?: return@launch
            val newSale = (newMrp * 0.88).roundToInt()
            val updated = current.copy(
                mrp = newMrp,
                sale = newSale,
                price = newSale
            )
            repository.updateProduct(updated)
            if (_selectedProductForDetail.value?.id == productId) {
                _selectedProductForDetail.value = updated
            }
        }
    }

    fun updateProductDescription(productId: String, newDescription: String) {
        viewModelScope.launch {
            val current = rawProducts.value.firstOrNull { it.id == productId } ?: return@launch
            val updated = current.copy(description = newDescription)
            repository.updateProduct(updated)
            if (_selectedProductForDetail.value?.id == productId) {
                _selectedProductForDetail.value = updated
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProductById(productId)
            if (_selectedProductForDetail.value?.id == productId) {
                _selectedProductForDetail.value = null
            }
        }
    }

    // --- Seller / Shopkeeper Registration ---

    fun registerSeller(seller: SellerRegistration) {
        viewModelScope.launch {
            repository.registerSeller(seller)
        }
    }

    fun approveSeller(seller: SellerRegistration) {
        viewModelScope.launch {
            repository.approveSeller(seller)
        }
    }

    // --- Admin & Test Mode Controls ---

    fun toggleTestMode(active: Boolean) {
        repository.setTestMode(active)
    }

    fun requestDataDeletion(phone: String) {
        repository.purgeUserData(phone)
    }

    fun setShowSellerRegistration(show: Boolean) {
        _showSellerRegistrationSheet.value = show
    }

    fun setShowRiderHub(show: Boolean) {
        _showRiderHub.value = show
    }

    fun setShowAdminController(show: Boolean) {
        _showAdminController.value = show
    }

    fun setShowLegalAndSafety(show: Boolean) {
        _showLegalAndSafety.value = show
    }

    fun setShowAiAssistant(show: Boolean) {
        _showAiAssistant.value = show
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        _selectedBrand.value = "All"
    }

    fun setBrand(brand: String) {
        _selectedBrand.value = brand
    }

    fun setSort(sort: String) {
        _selectedSort.value = sort
    }

    fun selectProductForDetail(product: Product?) {
        _selectedProductForDetail.value = product
    }

    fun setActiveTab(index: Int) {
        _activeTab.value = index
    }

    fun setShowCheckoutSheet(show: Boolean) {
        _showCheckoutSheet.value = show
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        repository.addToCart(product, quantity)
    }

    fun updateCartQuantity(productId: String, quantity: Int) {
        repository.updateCartQuantity(productId, quantity)
    }

    fun clearCart() {
        repository.clearCart()
    }

    fun toggleLanguage() {
        repository.toggleLanguage()
    }

    fun refreshProducts() {
        viewModelScope.launch {
            repository.refreshProductsFromCloud()
        }
    }

    /**
     * Dispatches order via WhatsApp with complete itemized bill to +91 8847370905
     * and records order in local Room database.
     * PUBLIC PRIVACY SHIELD: Renders customer-facing brand support instead of owner personal PII.
     */
    fun placeOrder(
        context: Context,
        customerName: String,
        customerPhone: String,
        deliveryOption: DeliveryOption,
        address: String,
        notes: String,
        paymentMethod: PaymentMethod = PaymentMethod.UPI_INTENT
    ) {
        val currentCart = cart.value
        if (currentCart.isEmpty()) {
            Toast.makeText(context, "Your cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val totalMrp = cartTotalMrp.value
        val totalSavings = cartTotalSavings.value
        val finalTotal = cartTotalSalePrice.value
        val isTestMode = isTestModeActive.value
        val orderPrefix = if (isTestMode) "TEST-" else "NAV-"
        val orderId = orderPrefix + SimpleDateFormat("ddMM-HHmmss", Locale.ENGLISH).format(Date())

        val isPb = isPunjabi.value
        val greeting = if (isTestMode) {
            "⚡ *[TABLET TEST MODE - SANDBOX ORDER]*\nਸਤਿ ਸ੍ਰੀ ਅਕਾਲ ਜੀ! ਇਹ ਟੈਬਲੇਟ ਟੈਸਟਿੰਗ ਆਰਡਰ ਹੈ:"
        } else if (isPb) {
            "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ ਜੀ! ਮੈਂ ਨਵਕਾਰਟ ਤੋਂ ਆਰਡਰ ਦੇਣਾ ਚਾਹੁੰਦਾ/ਚਾਹੁੰਦੀ ਹਾਂ:"
        } else {
            "Hello NavKart! I would like to place an order:"
        }

        val orderSummaryBuilder = StringBuilder()
        orderSummaryBuilder.append("$greeting\n\n")
        orderSummaryBuilder.append("🧾 *Order ID:* $orderId\n")
        orderSummaryBuilder.append("👤 *Customer:* ${customerName.ifBlank { "Valued Customer" }}\n")
        orderSummaryBuilder.append("📞 *Phone:* ${customerPhone.ifBlank { "Not provided" }}\n")
        orderSummaryBuilder.append("💳 *Payment Mode:* ${paymentMethod.title}\n")
        orderSummaryBuilder.append("🚚 *Delivery:* ${deliveryOption.title} (${deliveryOption.badge})\n")
        if (deliveryOption == DeliveryOption.HOME_DELIVERY) {
            orderSummaryBuilder.append("📍 *Address/Village:* $address\n")
        }
        if (notes.isNotBlank()) {
            orderSummaryBuilder.append("📝 *Notes:* $notes\n")
        }
        orderSummaryBuilder.append("\n🛒 *ORDER ITEMS (Sealed Packaged & Cold-Chain):*\n")

        currentCart.forEachIndexed { index, item ->
            val p = item.product
            orderSummaryBuilder.append(
                "${index + 1}. *${p.name}* (${p.brand})\n" +
                "   Qty: ${item.quantity} x ₹${p.wholesalePrice} (MRP: ₹${p.mrp})\n" +
                "   Subtotal: ₹${item.totalSalePrice} (Saved ₹${item.totalSavings})\n"
            )
        }

        orderSummaryBuilder.append("\n━━━━━━━━━━━━━━━━━━━━━━\n")
        orderSummaryBuilder.append("Total MRP: ₹$totalMrp\n")
        orderSummaryBuilder.append("Wholesale Discount (12% OFF): -₹$totalSavings\n")
        orderSummaryBuilder.append("💰 *FINAL PAYABLE AMOUNT: ₹$finalTotal*\n")
        orderSummaryBuilder.append("━━━━━━━━━━━━━━━━━━━━━━\n")
        orderSummaryBuilder.append("🏪 NavKart Marketplace • Main Bazaar, Sandhaur\n")
        orderSummaryBuilder.append("Customer Helpline: ${StoreProfile.HELPLINE_PHONE}\n")
        orderSummaryBuilder.append("Public Website: www.navgrocerystore.com")

        val messageText = orderSummaryBuilder.toString()

        // 1. Save in local order history
        val newOrder = Order(
            orderId = orderId,
            timestamp = System.currentTimeMillis(),
            customerName = customerName,
            customerPhone = customerPhone,
            deliveryType = deliveryOption.name,
            address = address,
            items = currentCart,
            totalMrp = totalMrp,
            totalDiscount = totalSavings,
            finalTotal = finalTotal,
            status = if (isTestMode) "Sandbox Test Order" else "Dispatched to Helpline",
            paymentMethod = paymentMethod.title,
            isPaid = isTestMode || paymentMethod == PaymentMethod.UPI_INTENT,
            isTestModeOrder = isTestMode,
            merchantVpa = OwnerSecureLedger.CENTRAL_MERCHANT_VPA
        )
        repository.recordOrder(newOrder)
        _showCheckoutSheet.value = false

        if (isTestMode) {
            Toast.makeText(context, "Test order $orderId simulated successfully in Sandbox!", Toast.LENGTH_LONG).show()
        }

        // 2. Launch WhatsApp Intent
        try {
            val encodedMessage = URLEncoder.encode(messageText, "UTF-8")
            val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=${StoreProfile.WHATSAPP_NUMBER}&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackUri = Uri.parse("https://wa.me/${StoreProfile.WHATSAPP_NUMBER}?text=${URLEncoder.encode(messageText, "UTF-8")}")
            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    fun callStore(context: Context) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${StoreProfile.HELPLINE_PHONE}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openInstagram(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(StoreProfile.INSTAGRAM_URL)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openLocation(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(StoreProfile.MAPS_QUERY_URL)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
