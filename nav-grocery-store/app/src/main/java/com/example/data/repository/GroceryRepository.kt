package com.example.data.repository

import com.example.data.api.GroceryApiService
import com.example.data.local.GroceryLocalStore
import com.example.data.local.ProductDao
import com.example.data.local.SellerDao
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.SellerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class GroceryRepository(
    private val apiService: GroceryApiService,
    private val productDao: ProductDao,
    private val sellerDao: SellerDao,
    private val localStore: GroceryLocalStore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    // Reactive Room Flow exposed as StateFlow
    val products: StateFlow<List<Product>> = productDao.getAllProducts()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val sellers: StateFlow<List<SellerRegistration>> = sellerDao.getAllSellers()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isPunjabi = MutableStateFlow(false)
    val isPunjabi: StateFlow<Boolean> = _isPunjabi.asStateFlow()

    private val _isTestModeActive = MutableStateFlow(false)
    val isTestModeActive: StateFlow<Boolean> = _isTestModeActive.asStateFlow()

    init {
        scope.launch {
            // 1. If Room database has fewer than 308 products, seed it immediately with the 308 locked products + multi-vendor items
            val count = productDao.getProductCount()
            if (count < 308) {
                val initialSeed = localStore.loadSeedProducts()
                val sanitizedSeed = sanitizeAndEnforceWholesale(initialSeed)
                if (sanitizedSeed.isNotEmpty()) {
                    productDao.insertProducts(sanitizedSeed)
                }
            }
        }

        // 2. Load saved cart, past orders, and language preference
        _cart.value = localStore.loadCart()
        _orders.value = localStore.loadOrders()
        _isPunjabi.value = localStore.isPunjabiLanguage()
    }

    suspend fun refreshProductsFromCloud() {
        withContext(Dispatchers.IO) {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                val cloudProducts = apiService.getProducts()
                val validList = sanitizeAndEnforceWholesale(cloudProducts)
                if (validList.isNotEmpty()) {
                    productDao.insertProducts(validList)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Offline mode enabled"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    suspend fun insertProduct(product: Product) {
        withContext(Dispatchers.IO) {
            val sanitized = sanitizeProduct(product)
            productDao.insertProduct(sanitized)
        }
    }

    suspend fun updateProduct(product: Product) {
        withContext(Dispatchers.IO) {
            val sanitized = sanitizeProduct(product)
            productDao.updateProduct(sanitized)
        }
    }

    suspend fun deleteProductById(productId: String) {
        withContext(Dispatchers.IO) {
            productDao.deleteProductById(productId)
        }
    }

    suspend fun registerSeller(seller: SellerRegistration) {
        withContext(Dispatchers.IO) {
            sellerDao.insertSeller(seller)
        }
    }

    suspend fun approveSeller(seller: SellerRegistration) {
        withContext(Dispatchers.IO) {
            sellerDao.updateSeller(seller.copy(isApproved = true, status = "Verified Partner"))
        }
    }

    fun setTestMode(active: Boolean) {
        _isTestModeActive.value = active
    }

    fun purgeUserData(phone: String) {
        clearCart()
        localStore.saveOrders(emptyList())
        _orders.value = emptyList()
    }

    /**
     * STRICT BUSINESS RULES ENFORCEMENT:
     * 1. 100% Sealed Packaged & Cold-Chain items ONLY.
     *    Fresh fruits and fresh vegetables are strictly prohibited.
     * 2. WHOLESALE PRICING RULE: Flat 12% OFF on MRP:
     *    sale = Math.round(mrp * 0.88)
     */
    private fun sanitizeAndEnforceWholesale(rawList: List<Product>): List<Product> {
        val prohibitedKeywords = listOf(
            "fresh fruit", "fresh vegetable", "raw fruit", "raw vegetable",
            "fresh tomato", "fresh potato", "fresh onion", "fresh apple", "fresh banana",
            "kacha fal", "taaza sabzi"
        )

        return rawList.filter { product ->
            val nameLower = product.name.lowercase()
            val catLower = product.category.lowercase()

            val isProhibited = prohibitedKeywords.any { kw ->
                nameLower.contains(kw) || catLower.contains(kw)
            }
            !isProhibited && product.id.isNotBlank() && product.mrp > 0
        }.map { sanitizeProduct(it) }
    }

    private fun sanitizeProduct(product: Product): Product {
        val computedWholesale = (product.mrp * 0.88).roundToInt()
        val resolvedSale = if (product.sale > 0) product.sale else computedWholesale
        return product.copy(
            sale = resolvedSale,
            price = resolvedSale,
            description = if (product.description.isNotBlank()) product.description else product.displayDescription
        )
    }

    fun addToCart(product: Product, quantityToAdd: Int = 1) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + quantityToAdd)
        } else {
            current.add(CartItem(product = product, quantity = quantityToAdd))
        }
        _cart.value = current
        localStore.saveCart(current)
    }

    fun updateCartQuantity(productId: String, newQuantity: Int) {
        val current = _cart.value.toMutableList()
        if (newQuantity <= 0) {
            current.removeAll { it.product.id == productId }
        } else {
            val index = current.indexOfFirst { it.product.id == productId }
            if (index >= 0) {
                current[index] = current[index].copy(quantity = newQuantity)
            }
        }
        _cart.value = current
        localStore.saveCart(current)
    }

    fun clearCart() {
        _cart.value = emptyList()
        localStore.saveCart(emptyList())
    }

    fun recordOrder(order: Order) {
        localStore.saveOrder(order)
        _orders.value = localStore.loadOrders()
        clearCart()
    }

    fun toggleLanguage() {
        val newLang = !_isPunjabi.value
        _isPunjabi.value = newLang
        localStore.setPunjabiLanguage(newLang)
    }
}
