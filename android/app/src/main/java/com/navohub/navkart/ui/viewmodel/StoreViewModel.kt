package com.navohub.navkart.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.navohub.navkart.data.local.CatalogCache
import com.navohub.navkart.data.model.CartEntry
import com.navohub.navkart.data.model.NavProduct
import com.navohub.navkart.data.model.OrderItemPayload
import com.navohub.navkart.data.model.OrderPayload
import com.navohub.navkart.data.model.PaymentChannel
import com.navohub.navkart.data.model.ServiceVertical
import com.navohub.navkart.data.remote.NavFirebaseApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StoreUiState(
    val loading: Boolean = true,
    val offlineMode: Boolean = false,
    val activeVertical: ServiceVertical = ServiceVertical.STORE,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val wishlistIds: Set<String> = emptySet(),
    val allProducts: List<NavProduct> = emptyList(),
    val filteredProducts: List<NavProduct> = emptyList(),
    val cart: List<CartEntry> = emptyList(),
    val totalPayable: Int = 0,
    val verifiedPhone: String = "",
    val customerName: String = "",
    val statusMessage: String? = null,
    val lastOrderId: String? = null
)

class StoreViewModel(application: Application) : AndroidViewModel(application) {
    private val api = NavFirebaseApi.create()
    private val cache = CatalogCache(application)

    private val _uiState = MutableStateFlow(
        StoreUiState(
            verifiedPhone = cache.getSavedPhone(),
            customerName = cache.getSavedName()
        )
    )
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        refreshCatalog()
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            val cached = cache.loadProducts()
            if (cached.isNotEmpty()) {
                updateCatalogState(cached, loading = false, offline = false, status = null)
            }
            try {
                val feed = api.fetchAndroidFeed()
                val items = feed.items.ifEmpty {
                    api.fetchAllProductsMap().values.toList()
                }
                if (items.isNotEmpty()) {
                    cache.saveProducts(items)
                    updateCatalogState(items, loading = false, offline = false, status = null)
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        offlineMode = true,
                        statusMessage = if (it.allProducts.isNotEmpty()) "Offline cached catalog" else null
                    )
                }
            }
        }
    }

    private fun updateCatalogState(
        products: List<NavProduct>,
        loading: Boolean,
        offline: Boolean,
        status: String?
    ) {
        _uiState.update { st ->
            val next = st.copy(
                loading = loading,
                offlineMode = offline,
                allProducts = products,
                statusMessage = status
            )
            next.copy(filteredProducts = applyFilters(next))
        }
    }

    private fun applyFilters(st: StoreUiState): List<NavProduct> {
        return st.allProducts.filter { prod ->
            if (!prod.isSafeRetailBoundary()) return@filter false

            val verticalOk = when (st.activeVertical) {
                ServiceVertical.STORE -> prod.isGroceryCore && prod.channels.navGroceryWeb
                ServiceVertical.SUPERMALL -> !prod.isGroceryCore && prod.channels.navKartWeb
                ServiceVertical.EXPRESS -> prod.stock > 0 && prod.isGroceryCore
                ServiceVertical.ESSENTIALS -> prod.isGroceryCore
            }
            if (!verticalOk) return@filter false

            if (st.selectedCategory != "All" && prod.category != st.selectedCategory) {
                return@filter false
            }

            if (st.searchQuery.isNotBlank()) {
                val hay = "${prod.id} ${prod.brand} ${prod.name} ${prod.category}".lowercase()
                return@filter st.searchQuery.trim().lowercase().split(" ").all { hay.contains(it) }
            }
            true
        }
    }

    fun selectVertical(vertical: ServiceVertical) {
        _uiState.update { st ->
            val next = st.copy(activeVertical = vertical, selectedCategory = "All")
            next.copy(filteredProducts = applyFilters(next))
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { st ->
            val next = st.copy(searchQuery = query)
            next.copy(filteredProducts = applyFilters(next))
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { st ->
            val next = st.copy(selectedCategory = category)
            next.copy(filteredProducts = applyFilters(next))
        }
    }

    fun toggleWishlist(skuId: String) {
        _uiState.update { st ->
            val next = if (skuId in st.wishlistIds) st.wishlistIds - skuId else st.wishlistIds + skuId
            st.copy(wishlistIds = next)
        }
    }

    fun addToCart(product: NavProduct) {
        _uiState.update { st ->
            val existing = st.cart.find { it.product.id == product.id }
            val updated = if (existing != null) {
                st.cart.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                st.cart + CartEntry(product, 1)
            }
            val saleSum = updated.sumOf { it.product.effectiveSale * it.quantity }
            val fee = if (saleSum >= 499 || saleSum == 0) 0 else 30
            st.copy(cart = updated, totalPayable = saleSum + fee)
        }
    }

    fun saveWhatsAppLogin(name: String, phone: String, address: String) {
        cache.saveSession(phone, name, address)
        _uiState.update { it.copy(customerName = name, verifiedPhone = phone) }
    }

    fun buildUpiPaymentUri(amountInr: Int): Uri {
        return Uri.parse(
            "upi://pay?pa=8847370905@paytm&pn=NavGroceryStore&am=${amountInr}.00&cu=INR&tn=NavKartOrder"
        )
    }

    fun submitOrder(
        paymentMethod: PaymentChannel,
        utrRef: String?,
        fulfillment: String = "delivery"
    ) {
        viewModelScope.launch {
            val st = _uiState.value
            if (st.cart.isEmpty()) return@launch
            val mrpTotal = st.cart.sumOf { it.product.mrp * it.quantity }
            val saleTotal = st.cart.sumOf { it.product.effectiveSale * it.quantity }
            val deliveryFee = if (fulfillment == "pickup" || saleTotal >= 499) 0 else 30
            val totalPayable = saleTotal + deliveryFee
            val gstIncluded = (saleTotal * 5) / 105
            val dateCode = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
            val orderId = "NGS-$dateCode-${(1000..9999).random()}"

            val record = OrderPayload(
                orderId = orderId,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                customerName = st.customerName.ifBlank { "Sandhaur Customer" },
                customerPhone = st.verifiedPhone.ifBlank { "8847370905" },
                address = cache.getSavedAddress(),
                fulfillment = fulfillment,
                slot = if (fulfillment == "pickup") "30-Min Store Pickup" else "15-Min Express Delivery",
                paymentMethod = paymentMethod.name.lowercase(),
                paymentStatus = if (paymentMethod == PaymentChannel.COD) "Pay on Delivery" else "Paid / UTR Submitted",
                utrRef = utrRef,
                subtotalMrp = mrpTotal,
                subtotalSale = saleTotal,
                gstTotal = gstIncluded,
                savings = mrpTotal - saleTotal,
                deliveryFee = deliveryFee,
                totalPayable = totalPayable,
                items = st.cart.map {
                    OrderItemPayload(
                        id = it.product.id,
                        name = it.product.name,
                        brand = it.product.brand,
                        qty = it.quantity,
                        mrp = it.product.mrp,
                        sale = it.product.effectiveSale,
                        gst = it.product.gst
                    )
                }
            )
            try {
                api.submitOrder(orderId, record)
                _uiState.update {
                    it.copy(
                        cart = emptyList(),
                        totalPayable = 0,
                        lastOrderId = orderId,
                        statusMessage = "Order $orderId synced to Nav Grocery & Nav Kart"
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(statusMessage = "Order sync delayed — check internet connectivity")
                }
            }
        }
    }
}
