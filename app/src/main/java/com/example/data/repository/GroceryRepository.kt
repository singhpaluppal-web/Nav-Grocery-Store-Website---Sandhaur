package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.GroceryApiService
import com.example.data.local.GroceryLocalStore
import com.example.data.local.ProductDao
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.CoinRewardConfig
import com.example.data.model.HeroBannerSlide
import com.example.data.model.MarketplaceCategory
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.StoreDeliveryConfig
import com.example.data.model.StoreHeroPromoConfig
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StorePaymentsConfig
import com.example.data.util.ImageCacheSummary
import com.example.data.util.ProductImageAutoFetcher
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Strictly read-only public customer repository for dynamic catalog (/products),
 * dynamic A-to-Z categories (/categories), remote website-controlled offer & coin rules
 * (/storeConfig/offers & /storeConfig/coins), and local shopping cart state.
 * Contains zero product-mutation, seller-onboarding, or admin methods.
 */
class GroceryRepository(
    private val apiService: GroceryApiService,
    private val productDao: ProductDao,
    private val localStore: GroceryLocalStore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val rtdb: FirebaseDatabase? = runCatching {
        FirebaseDatabase.getInstance(RTDB_URL)
    }.getOrNull()

    val products: StateFlow<List<Product>> = productDao.getAllProducts()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _dynamicCategories = MutableStateFlow<List<MarketplaceCategory>>(emptyList())
    val dynamicCategories: StateFlow<List<MarketplaceCategory>> = _dynamicCategories.asStateFlow()

    private val _storeOffersConfig = MutableStateFlow(StoreOffersConfig())
    val storeOffersConfig: StateFlow<StoreOffersConfig> = _storeOffersConfig.asStateFlow()

    private val _storePaymentsConfig = MutableStateFlow(StorePaymentsConfig())
    val storePaymentsConfig: StateFlow<StorePaymentsConfig> = _storePaymentsConfig.asStateFlow()

    private val _storeDeliveryConfig = MutableStateFlow(StoreDeliveryConfig())
    val storeDeliveryConfig: StateFlow<StoreDeliveryConfig> = _storeDeliveryConfig.asStateFlow()

    private val _storeHeroPromoConfig = MutableStateFlow(StoreHeroPromoConfig())
    val storeHeroPromoConfig: StateFlow<StoreHeroPromoConfig> = _storeHeroPromoConfig.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _imageCacheSummary = MutableStateFlow<ImageCacheSummary?>(null)
    val imageCacheSummary: StateFlow<ImageCacheSummary?> = _imageCacheSummary.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _isPunjabi = MutableStateFlow(false)
    val isPunjabi: StateFlow<Boolean> = _isPunjabi.asStateFlow()

    private val _heroBanners = MutableStateFlow<List<HeroBannerSlide>>(HeroBannerSlide.DEFAULT_SLIDES)
    val heroBanners: StateFlow<List<HeroBannerSlide>> = _heroBanners.asStateFlow()

    private val _marqueeText = MutableStateFlow(DEFAULT_MARQUEE_TEXT)
    val marqueeText: StateFlow<String> = _marqueeText.asStateFlow()

    private val _isTestModeActive = MutableStateFlow(false)
    val isTestModeActive: StateFlow<Boolean> = _isTestModeActive.asStateFlow()

    init {
        scope.launch {
            val count = productDao.getProductCount()
            val sample098 = productDao.getProductByIdDirect("NAV098")
            val sample683 = productDao.getProductByIdDirect("NAV683")
            val sample731 = productDao.getProductByIdDirect("NAV731")
            val hasBrokenLegacyPhoto = (sample683 != null && ProductImageAutoFetcher.isMissingOrBrokenImage(sample683.image, sample683.id)) ||
                (sample731 != null && ProductImageAutoFetcher.isMissingOrBrokenImage(sample731.image, sample731.id))
            val isStaleSeed = (sample098 != null && sample098.image.contains("images/verified/")) || hasBrokenLegacyPhoto

            if (count == 0 || isStaleSeed) {
                val initialSeed = localStore.loadSeedProducts()
                val sanitizedSeed = sanitizeAndEnforceCatalogPolicy(initialSeed, _storeOffersConfig.value)
                if (sanitizedSeed.isNotEmpty()) {
                    productDao.clearAll()
                    productDao.insertProducts(sanitizedSeed)
                }
            }
        }

        _cart.value = localStore.loadCart()
        _orders.value = localStore.loadOrders()
        val savedLang = localStore.getAppLanguage()
        _appLanguage.value = savedLang
        _isPunjabi.value = (savedLang == AppLanguage.PUNJABI)
        startRealtimeMarketplaceSync()
    }

    /**
     * Real-time read-only listeners on Firebase RTDB for:
     * - `/products` (supports dynamic A-to-Z marketplace items beyond 308/808 without caps)
     * - `/categories` (dynamic retail categories & category-level discount overrides)
     * - `/storeConfig` (`offers` + `coins` live rates, banners & offer toggles)
     * - `/hero_banners` & `/system_locks/marqueeTicker`
     */
    private fun startRealtimeMarketplaceSync() {
        val db = rtdb ?: return
        runCatching {
            db.reference.child("products").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Product>()
                    for (child in snapshot.children) {
                        parseProductSnapshot(child)?.let { list.add(it) }
                    }
                    if (list.isNotEmpty()) {
                        scope.launch {
                            val sanitized = sanitizeAndEnforceCatalogPolicy(list, _storeOffersConfig.value)
                            if (sanitized.isNotEmpty()) {
                                productDao.clearAll()
                                productDao.insertProducts(sanitized)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "RTDB products listener cancelled: ${error.code}")
                    }
                }
            })

            db.reference.child("categories").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val cats = mutableListOf<MarketplaceCategory>()
                    for (child in snapshot.children) {
                        parseCategorySnapshot(child)?.let {
                            if (it.isActive) cats.add(it)
                        }
                    }
                    if (cats.isNotEmpty()) {
                        _dynamicCategories.value = cats.sortedWith(
                            compareBy<MarketplaceCategory> { it.sortOrder }.thenBy { it.name.lowercase() }
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "RTDB categories listener cancelled: ${error.code}")
                    }
                }
            })

            db.reference.child("storeConfig").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val offersNode = snapshot.child("offers")
                    val coinsNode = snapshot.child("coins")
                    val nestedCoinNode = offersNode.child("coinConfig")

                    val discountPct = (offersNode.child("discountPercent").value as? Number)?.toDouble()
                        ?: (snapshot.child("discountPercent").value as? Number)?.toDouble()
                        ?: StoreOffersConfig.DEFAULT_DISCOUNT_PERCENT
                    val petiExtraPct = (offersNode.child("petiExtraDiscountPercent").value as? Number)?.toDouble()
                        ?: StoreOffersConfig.DEFAULT_PETI_EXTRA_DISCOUNT_PERCENT
                    val isOfferActive = offersNode.child("isOfferActive").getValue(Boolean::class.java) ?: false
                    val offerBannerText = offersNode.child("offerBannerText").getValue(String::class.java).orEmpty().trim()

                    val coinSnap = if (coinsNode.exists()) coinsNode else nestedCoinNode
                    val coinConfig = CoinRewardConfig(
                        isCoinsEnabled = coinSnap.child("isCoinsEnabled").getValue(Boolean::class.java) ?: true,
                        earnRatePercent = (coinSnap.child("earnRatePercent").value as? Number)?.toDouble() ?: 4.0,
                        coinValueInr = (coinSnap.child("coinValueInr").value as? Number)?.toDouble() ?: 1.0,
                        maxRedeemPercent = (coinSnap.child("maxRedeemPercent").value as? Number)?.toDouble() ?: 25.0
                    )

                    val liveConfig = StoreOffersConfig(
                        discountPercent = discountPct,
                        offerBannerText = offerBannerText,
                        isOfferActive = isOfferActive,
                        petiExtraDiscountPercent = petiExtraPct,
                        coinConfig = coinConfig
                    )
                    _storeOffersConfig.value = liveConfig
                    if (isOfferActive && offerBannerText.isNotBlank()) {
                        _marqueeText.value = offerBannerText
                    }

                    val paymentsNode = snapshot.child("payments")
                    if (paymentsNode.exists()) {
                        val emiProviders = paymentsNode.child("supportedEmiProviders").children
                            .mapNotNull { it.getValue(String::class.java)?.trim() }
                            .filter { it.isNotBlank() }
                        _storePaymentsConfig.value = StorePaymentsConfig(
                            isEmiEnabled = paymentsNode.child("isEmiEnabled").getValue(Boolean::class.java) ?: true,
                            minEmiOrderAmount = (paymentsNode.child("minEmiOrderAmount").value as? Number)?.toInt()
                                ?: StorePaymentsConfig.DEFAULT_MIN_EMI_ORDER_AMOUNT,
                            supportedEmiProviders = emiProviders.ifEmpty { StorePaymentsConfig.DEFAULT_EMI_PROVIDERS }
                        )
                    }

                    val deliveryNode = snapshot.child("delivery")
                    if (deliveryNode.exists()) {
                        val localPins = deliveryNode.child("localPincodes").children
                            .mapNotNull { it.getValue(String::class.java)?.filter { ch -> ch.isDigit() } }
                            .filter { it.length == 6 }
                        _storeDeliveryConfig.value = StoreDeliveryConfig(
                            localPincodes = localPins.ifEmpty { StoreDeliveryConfig.DEFAULT_LOCAL_PINCODES },
                            freeDeliveryMinOrder = (deliveryNode.child("freeDeliveryMinOrder").value as? Number)?.toInt()
                                ?: StoreDeliveryConfig.DEFAULT_FREE_DELIVERY_MIN_ORDER,
                            localDeliveryFee = (deliveryNode.child("localDeliveryFee").value as? Number)?.toInt()
                                ?: StoreDeliveryConfig.DEFAULT_LOCAL_DELIVERY_FEE,
                            standardDeliveryFee = (deliveryNode.child("standardDeliveryFee").value as? Number)?.toInt()
                                ?: StoreDeliveryConfig.DEFAULT_STANDARD_DELIVERY_FEE,
                            expressDeliveryFee = (deliveryNode.child("expressDeliveryFee").value as? Number)?.toInt()
                                ?: StoreDeliveryConfig.DEFAULT_EXPRESS_DELIVERY_FEE,
                            storePickupEnabled = deliveryNode.child("storePickupEnabled").getValue(Boolean::class.java) ?: true,
                            pickupAddress = deliveryNode.child("pickupAddress").getValue(String::class.java)
                                ?.takeIf { it.isNotBlank() } ?: com.example.data.model.StoreProfile.LOCATION
                        )
                    }

                    val heroPromoNode = snapshot.child("heroPromo")
                    if (heroPromoNode.exists()) {
                        _storeHeroPromoConfig.value = StoreHeroPromoConfig(
                            isActive = heroPromoNode.child("isActive").getValue(Boolean::class.java) ?: false,
                            badgeText = heroPromoNode.child("badgeText").getValue(String::class.java)
                                ?.takeIf { it.isNotBlank() } ?: "FEATURED PROMO",
                            title = heroPromoNode.child("title").getValue(String::class.java).orEmpty().trim(),
                            subtitle = heroPromoNode.child("subtitle").getValue(String::class.java).orEmpty().trim(),
                            mediaType = heroPromoNode.child("mediaType").getValue(String::class.java)
                                ?.takeIf { it.isNotBlank() } ?: "BANNER",
                            bannerImageUrl = heroPromoNode.child("bannerImageUrl").getValue(String::class.java).orEmpty().trim(),
                            videoUrl = heroPromoNode.child("videoUrl").getValue(String::class.java).orEmpty().trim(),
                            ctaText = heroPromoNode.child("ctaText").getValue(String::class.java)
                                ?.takeIf { it.isNotBlank() } ?: "Shop Promo",
                            ctaCategory = heroPromoNode.child("ctaCategory").getValue(String::class.java)
                                ?.takeIf { it.isNotBlank() } ?: "All Products"
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "RTDB storeConfig listener cancelled: ${error.code}")
                    }
                }
            })

            db.reference.child("hero_banners").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val slides = snapshot.children.mapNotNull { c ->
                        val head = c.child("headline").getValue(String::class.java) ?: return@mapNotNull null
                        HeroBannerSlide(
                            id = c.child("id").getValue(String::class.java) ?: "HERO",
                            badge = c.child("badge").getValue(String::class.java).orEmpty(),
                            headline = head,
                            subtitle = c.child("subtitle").getValue(String::class.java).orEmpty(),
                            ctaText = c.child("ctaText").getValue(String::class.java) ?: "Shop Now",
                            ctaCategory = c.child("ctaCategory").getValue(String::class.java) ?: "All Products",
                            imageUrl = c.child("imageUrl").getValue(String::class.java).orEmpty()
                        )
                    }
                    if (slides.isNotEmpty()) _heroBanners.value = slides
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            db.reference.child("system_locks/marqueeTicker").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ticker = snapshot.getValue(String::class.java)?.trim()
                    if (!ticker.isNullOrBlank() && !_storeOffersConfig.value.isOfferActive) {
                        _marqueeText.value = ticker
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    suspend fun refreshProductsFromCloud() {
        withContext(Dispatchers.IO) {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                // 1. Live offers + coin loyalty rules from website admin panel
                val fetchedOffers = runCatching { apiService.getStoreOffersConfig() }.getOrNull()
                val fetchedCoins = runCatching { apiService.getStoreCoinConfig() }.getOrNull()
                if (fetchedOffers != null || fetchedCoins != null) {
                    val base = fetchedOffers ?: _storeOffersConfig.value
                    val merged = base.copy(coinConfig = fetchedCoins ?: base.coinConfig)
                    _storeOffersConfig.value = merged
                    if (merged.isOfferActive && merged.offerBannerText.isNotBlank()) {
                        _marqueeText.value = merged.offerBannerText
                    }
                }
                runCatching { apiService.getStorePaymentsConfig() }.getOrNull()?.let { payments ->
                    _storePaymentsConfig.value = payments
                }
                runCatching { apiService.getStoreDeliveryConfig() }.getOrNull()?.let { delivery ->
                    _storeDeliveryConfig.value = delivery
                }
                runCatching { apiService.getStoreHeroPromoConfig() }.getOrNull()?.let { promo ->
                    _storeHeroPromoConfig.value = promo
                }

                // 2. Dynamic A-to-Z categories from /categories
                runCatching { apiService.getCategoriesRaw().string() }.getOrNull()?.let { rawCats ->
                    val parsedCats = parseCategoriesJson(rawCats).filter { it.isActive }
                    if (parsedCats.isNotEmpty()) {
                        _dynamicCategories.value = parsedCats.sortedWith(
                            compareBy<MarketplaceCategory> { it.sortOrder }.thenBy { it.name.lowercase() }
                        )
                    }
                }

                // 3. Full dynamic product catalog from /products (no artificial count limit)
                val rawProductsJson = apiService.getProductsRaw().string()
                val cloudProducts = parseProductsJson(rawProductsJson)
                val missingFromFirebaseSkus = cloudProducts
                    .filter { ProductImageAutoFetcher.isMissingOrBrokenImage(it.image, it.id) }
                    .map { it.id }
                    .toSet()

                val validList = sanitizeAndEnforceCatalogPolicy(cloudProducts, _storeOffersConfig.value)
                if (validList.isNotEmpty()) {
                    productDao.clearAll()
                    productDao.insertProducts(validList)

                    if (missingFromFirebaseSkus.isNotEmpty()) {
                        scope.launch {
                            _imageCacheSummary.value = ProductImageAutoFetcher.fetchAndCacheMissingPackagingPhotos(
                                context = localStore.appContext,
                                products = validList,
                                missingFromFirebaseSkus = missingFromFirebaseSkus
                            )
                        }
                    }
                }

                // 4. Hero banners & marquee ticker
                runCatching { apiService.getHeroBannersRaw().string() }.getOrNull()?.let { rawBanners ->
                    val slides = parseHeroBannersJson(rawBanners).filter { it.headline.isNotBlank() }
                    if (slides.isNotEmpty()) _heroBanners.value = slides
                }
                runCatching { apiService.getSystemLocks() }.getOrNull()?.let { locks ->
                    if (locks.marqueeTicker.isNotBlank() && !_storeOffersConfig.value.isOfferActive) {
                        _marqueeText.value = locks.marqueeTicker.trim()
                    }
                }
            } catch (_: Exception) {
                _errorMessage.value = "Offline mode enabled"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    suspend fun autoFetchAndCacheMissingPhotos(prefetchAllNav309To808: Boolean = false): ImageCacheSummary {
        val currentProducts = products.value.ifEmpty {
            sanitizeAndEnforceCatalogPolicy(localStore.loadSeedProducts(), _storeOffersConfig.value)
        }
        val resolvedProducts = ProductImageAutoFetcher.ensureCatalogImages(currentProducts)
        val summary = ProductImageAutoFetcher.fetchAndCacheMissingPackagingPhotos(
            context = localStore.appContext,
            products = resolvedProducts,
            prefetchAllNav309To808 = prefetchAllNav309To808
        )
        _imageCacheSummary.value = summary
        return summary
    }

    fun setTestMode(active: Boolean) {
        _isTestModeActive.value = active
    }

    fun purgeUserData(phone: String) {
        clearCart()
        localStore.saveOrders(emptyList())
        _orders.value = emptyList()
    }

    private fun sanitizeAndEnforceCatalogPolicy(
        rawList: List<Product>,
        offersConfig: StoreOffersConfig
    ): List<Product> {
        val prohibitedKeywords = listOf(
            "fresh fruit", "fresh vegetable", "raw fruit", "raw vegetable",
            "fresh tomato", "fresh potato", "fresh onion", "fresh apple", "fresh banana",
            "kacha fal", "taaza sabzi"
        )
        val packagedExceptions = listOf(
            "ketchup", "sauce", "puree", "soup", "chips", "pickle",
            "masala", "paste", "powder", "juice", "frozen", "tea",
            "soap", "shampoo", "biscuit", "cookies", "bottle", "jar",
            "jam", "paneer", "cream", "milk", "butter", "cheese",
            "noodles", "oats", "honey", "coffee", "sharbat", "chocolate",
            "curtain", "bedsheet", "pillow", "detergent", "cleaner"
        )

        val catDiscounts = _dynamicCategories.value
            .filter { it.discountPercent != null }
            .associate { it.name.lowercase() to (it.discountPercent ?: offersConfig.discountPercent) }

        return rawList.filter { product ->
            val nameLower = product.name.lowercase()
            val catLower = product.category.lowercase()
            val isApproved = product.approvalStatus.isBlank() || product.approvalStatus.equals("APPROVED", ignoreCase = true)

            val isPackagedGoods = packagedExceptions.any { ex -> nameLower.contains(ex) }
            val isProhibited = !isPackagedGoods && prohibitedKeywords.any { kw ->
                nameLower.contains(kw) || catLower.contains(kw)
            }
            isApproved && !isProhibited && product.id.isNotBlank() && product.mrp > 0
        }.map { sanitizeProduct(it, offersConfig, catDiscounts) }
    }

    private fun sanitizeProduct(
        product: Product,
        offersConfig: StoreOffersConfig,
        catDiscounts: Map<String, Double>
    ): Product {
        val withImage = ProductImageAutoFetcher.ensureProductHasImage(product)
        val catPct = catDiscounts[withImage.category.lowercase()]
        val liveSale = withImage.sellingPrice(catPct, offersConfig)
        val resolvedSale = if (liveSale > 0) liveSale else withImage.sale
        return withImage.copy(
            sale = resolvedSale,
            price = resolvedSale,
            description = if (withImage.description.isNotBlank()) withImage.description else withImage.displayDescription
        )
    }

    private fun parseProductSnapshot(child: DataSnapshot): Product? {
        val id = (child.child("id").getValue(String::class.java) ?: child.key)?.trim() ?: return null
        val name = child.child("name").getValue(String::class.java)?.trim() ?: return null
        val mrpVal = (child.child("mrp").value as? Number)?.toInt() ?: 0
        if (id.isBlank() || mrpVal <= 0) return null

        val itemDiscountPct = (child.child("discountPercent").value as? Number)?.toDouble()
        val liveDefaultSale = (mrpVal * (1.0 - (itemDiscountPct ?: _storeOffersConfig.value.discountPercent) / 100.0)).roundToInt()
        val saleVal = (child.child("sale").value as? Number)?.toInt()
            ?: (child.child("price").value as? Number)?.toInt()
            ?: liveDefaultSale
        val stockVal = (child.child("stock").value as? Number)?.toInt() ?: 25

        val extraImagesList = mutableListOf<String>()
        val extraStr = child.child("additionalImages").getValue(String::class.java).orEmpty()
        if (extraStr.isNotBlank()) extraImagesList.add(extraStr)
        if (child.hasChild("images")) {
            child.child("images").children.forEach { imgSnap ->
                imgSnap.getValue(String::class.java)?.trim()?.takeIf { it.isNotBlank() }?.let { extraImagesList.add(it) }
            }
        }

        return Product(
            id = id,
            name = name,
            description = child.child("description").getValue(String::class.java).orEmpty(),
            department = child.child("department").getValue(String::class.java) ?: "Packaged Groceries",
            brand = child.child("brand").getValue(String::class.java).orEmpty(),
            category = child.child("category").getValue(String::class.java).orEmpty(),
            barcode = child.child("barcode").getValue(String::class.java).orEmpty(),
            mrp = mrpVal,
            cost = (child.child("cost").value as? Number)?.toInt() ?: 0,
            price = saleVal,
            sale = saleVal,
            discountPercent = itemDiscountPct,
            stock = stockVal,
            gst = child.child("gst").getValue(String::class.java) ?: "5%",
            image = child.child("image").getValue(String::class.java).orEmpty(),
            additionalImages = extraImagesList.joinToString(","),
            sellerId = child.child("sellerId").getValue(String::class.java) ?: "SELLER_NAV_STORE",
            sellerName = child.child("sellerName").getValue(String::class.java) ?: "Nav Grocery Store",
            rating = (child.child("rating").value as? Number)?.toFloat() ?: 4.6f,
            ratingCount = (child.child("ratingCount").value as? Number)?.toInt() ?: 120,
            availableSizes = child.child("availableSizes").getValue(String::class.java).orEmpty(),
            availableColors = child.child("availableColors").getValue(String::class.java).orEmpty(),
            warranty = child.child("warranty").getValue(String::class.java).orEmpty(),
            returnPolicy = child.child("returnPolicy").getValue(String::class.java).orEmpty(),
            specs = child.child("specs").getValue(String::class.java).orEmpty(),
            isVerifiedSeller = child.child("isVerifiedSeller").getValue(Boolean::class.java) ?: true,
            approvalStatus = child.child("approvalStatus").getValue(String::class.java) ?: "APPROVED"
        )
    }

    private fun parseCategorySnapshot(child: DataSnapshot): MarketplaceCategory? {
        val name = child.child("name").getValue(String::class.java)?.trim()
            ?: child.getValue(String::class.java)?.trim()
            ?: return null
        if (name.isBlank()) return null
        val id = child.child("id").getValue(String::class.java)?.trim() ?: child.key ?: name.lowercase().replace(" ", "_")
        return MarketplaceCategory(
            id = id,
            name = name,
            punjabiName = child.child("punjabiName").getValue(String::class.java).orEmpty(),
            hindiName = child.child("hindiName").getValue(String::class.java).orEmpty(),
            department = child.child("department").getValue(String::class.java).orEmpty(),
            imageUrl = child.child("imageUrl").getValue(String::class.java).orEmpty(),
            discountPercent = (child.child("discountPercent").value as? Number)?.toDouble(),
            returnPolicy = child.child("returnPolicy").getValue(String::class.java).orEmpty(),
            sortOrder = (child.child("sortOrder").value as? Number)?.toInt() ?: 100,
            isActive = child.child("isActive").getValue(Boolean::class.java) ?: true
        )
    }

    private fun parseProductsJson(raw: String): List<Product> {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed == "null") return emptyList()
        val items = mutableListOf<JSONObject>()
        if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { items.add(it) }
            }
        } else if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val child = obj.optJSONObject(key) ?: continue
                if (!child.has("id") || child.optString("id").isBlank()) {
                    child.put("id", key)
                }
                items.add(child)
            }
        }
        return items.mapNotNull { obj ->
            val id = obj.optString("id").trim()
            val name = obj.optString("name").trim()
            val mrp = obj.optInt("mrp", 0)
            if (id.isBlank() || name.isBlank() || mrp <= 0) return@mapNotNull null

            val itemDisc = if (obj.has("discountPercent") && !obj.isNull("discountPercent")) {
                obj.optDouble("discountPercent").takeIf { !it.isNaN() }
            } else null

            val extraImagesList = mutableListOf<String>()
            val extraStr = obj.optString("additionalImages").trim()
            if (extraStr.isNotBlank() && extraStr != "null") extraImagesList.add(extraStr)
            obj.optJSONArray("images")?.let { imgsArr ->
                for (j in 0 until imgsArr.length()) {
                    imgsArr.optString(j).trim().takeIf { it.isNotBlank() && it != "null" }?.let { extraImagesList.add(it) }
                }
            }

            val defaultSale = (mrp * (1.0 - (itemDisc ?: _storeOffersConfig.value.discountPercent) / 100.0)).roundToInt()
            val saleVal = obj.optInt("sale", 0).takeIf { it > 0 }
                ?: obj.optInt("price", 0).takeIf { it > 0 }
                ?: defaultSale

            Product(
                id = id,
                name = name,
                description = obj.optString("description").takeUnless { it == "null" }.orEmpty(),
                department = obj.optString("department").takeIf { it.isNotBlank() && it != "null" } ?: "Packaged Groceries",
                brand = obj.optString("brand").takeUnless { it == "null" }.orEmpty(),
                category = obj.optString("category").takeUnless { it == "null" }.orEmpty(),
                barcode = obj.optString("barcode").takeUnless { it == "null" }.orEmpty(),
                mrp = mrp,
                cost = obj.optInt("cost", 0),
                price = saleVal,
                sale = saleVal,
                discountPercent = itemDisc,
                stock = obj.optInt("stock", 25),
                gst = obj.optString("gst").takeIf { it.isNotBlank() && it != "null" } ?: "5%",
                image = obj.optString("image").takeUnless { it == "null" }.orEmpty(),
                additionalImages = extraImagesList.joinToString(","),
                sellerId = obj.optString("sellerId").takeIf { it.isNotBlank() && it != "null" } ?: "SELLER_NAV_STORE",
                sellerName = obj.optString("sellerName").takeIf { it.isNotBlank() && it != "null" } ?: "Nav Grocery Store",
                rating = obj.optDouble("rating", 4.6).toFloat(),
                ratingCount = obj.optInt("ratingCount", 120),
                availableSizes = obj.optString("availableSizes").takeUnless { it == "null" }.orEmpty(),
                availableColors = obj.optString("availableColors").takeUnless { it == "null" }.orEmpty(),
                warranty = obj.optString("warranty").takeUnless { it == "null" }.orEmpty(),
                returnPolicy = obj.optString("returnPolicy").takeUnless { it == "null" }.orEmpty(),
                specs = obj.optString("specs").takeUnless { it == "null" }.orEmpty(),
                isVerifiedSeller = obj.optBoolean("isVerifiedSeller", true),
                approvalStatus = obj.optString("approvalStatus").takeIf { it.isNotBlank() && it != "null" } ?: "APPROVED"
            )
        }
    }

    private fun parseCategoriesJson(raw: String): List<MarketplaceCategory> {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed == "null") return emptyList()
        val list = mutableListOf<MarketplaceCategory>()
        val objects = mutableListOf<Pair<String, JSONObject>>()
        if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { objects.add("cat_$i" to it) }
            }
        } else if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                root.optJSONObject(key)?.let { objects.add(key to it) }
            }
        }
        for ((key, obj) in objects) {
            val name = obj.optString("name").trim()
            if (name.isBlank() || name == "null") continue
            val disc = if (obj.has("discountPercent") && !obj.isNull("discountPercent")) {
                obj.optDouble("discountPercent").takeIf { !it.isNaN() }
            } else null
            list.add(
                MarketplaceCategory(
                    id = obj.optString("id").takeIf { it.isNotBlank() && it != "null" } ?: key,
                    name = name,
                    punjabiName = obj.optString("punjabiName").takeUnless { it == "null" }.orEmpty(),
                    hindiName = obj.optString("hindiName").takeUnless { it == "null" }.orEmpty(),
                    department = obj.optString("department").takeUnless { it == "null" }.orEmpty(),
                    imageUrl = obj.optString("imageUrl").takeUnless { it == "null" }.orEmpty(),
                    discountPercent = disc,
                    returnPolicy = obj.optString("returnPolicy").takeUnless { it == "null" }.orEmpty(),
                    sortOrder = obj.optInt("sortOrder", 100),
                    isActive = obj.optBoolean("isActive", true)
                )
            )
        }
        return list
    }

    private fun parseHeroBannersJson(raw: String): List<HeroBannerSlide> {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed == "null") return emptyList()
        val objects = mutableListOf<JSONObject>()
        if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { objects.add(it) }
            }
        } else if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            val keys = root.keys()
            while (keys.hasNext()) {
                root.optJSONObject(keys.next())?.let { objects.add(it) }
            }
        }
        return objects.mapNotNull { obj ->
            val headline = obj.optString("headline").trim()
            if (headline.isBlank() || headline == "null") return@mapNotNull null
            HeroBannerSlide(
                id = obj.optString("id").takeIf { it.isNotBlank() && it != "null" } ?: "HERO",
                badge = obj.optString("badge").takeUnless { it == "null" }.orEmpty(),
                headline = headline,
                subtitle = obj.optString("subtitle").takeUnless { it == "null" }.orEmpty(),
                ctaText = obj.optString("ctaText").takeIf { it.isNotBlank() && it != "null" } ?: "Shop Now",
                ctaCategory = obj.optString("ctaCategory").takeIf { it.isNotBlank() && it != "null" } ?: "All Products",
                imageUrl = obj.optString("imageUrl").takeUnless { it == "null" }.orEmpty()
            )
        }
    }

    fun addToCart(
        product: Product,
        quantityToAdd: Int = 1,
        selectedSize: String = "",
        selectedColor: String = "",
        isPetiCarton: Boolean = false
    ) {
        val liveProduct = products.value.firstOrNull { it.id == product.id } ?: product
        if (liveProduct.stock <= 0) return

        val maxStockLimit = liveProduct.stock.coerceIn(1, 50)
        val normSize = selectedSize.trim()
        val normColor = selectedColor.trim()
        val usePeti = isPetiCarton && liveProduct.isBulkEligible
        val petiUnits = if (usePeti) liveProduct.petiUnits else 1
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst {
            it.product.id == liveProduct.id &&
                it.selectedSize.equals(normSize, ignoreCase = true) &&
                it.selectedColor.equals(normColor, ignoreCase = true) &&
                it.isPetiCarton == usePeti
        }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(
                product = liveProduct,
                quantity = (existing.quantity + quantityToAdd).coerceIn(1, maxStockLimit)
            )
        } else {
            current.add(
                CartItem(
                    product = liveProduct,
                    quantity = quantityToAdd.coerceIn(1, maxStockLimit),
                    selectedSize = normSize,
                    selectedColor = normColor,
                    isPetiCarton = usePeti,
                    petiUnits = petiUnits
                )
            )
        }
        _cart.value = current
        localStore.saveCart(current)
    }

    fun updateCartQuantity(
        productId: String,
        newQuantity: Int,
        selectedSize: String = "",
        selectedColor: String = "",
        isPetiCarton: Boolean? = null
    ) {
        val liveStock = products.value.firstOrNull { it.id == productId }?.stock
        val normSize = selectedSize.trim()
        val normColor = selectedColor.trim()
        val hasExplicitVariant = normSize.isNotEmpty() || normColor.isNotEmpty() || isPetiCarton != null
        val current = _cart.value.toMutableList()
        val matchPredicate: (CartItem) -> Boolean = { item ->
            if (hasExplicitVariant) {
                item.product.id == productId &&
                    item.selectedSize.equals(normSize, ignoreCase = true) &&
                    item.selectedColor.equals(normColor, ignoreCase = true) &&
                    (isPetiCarton == null || item.isPetiCarton == isPetiCarton)
            } else {
                item.product.id == productId
            }
        }
        if (newQuantity <= 0 || (liveStock != null && liveStock <= 0)) {
            if (hasExplicitVariant) {
                current.removeAll(matchPredicate)
            } else {
                val idx = current.indexOfFirst(matchPredicate)
                if (idx >= 0) current.removeAt(idx)
            }
        } else {
            val index = current.indexOfFirst(matchPredicate)
            if (index >= 0) {
                val maxQty = (liveStock ?: current[index].product.stock).coerceIn(1, 50)
                current[index] = current[index].copy(quantity = newQuantity.coerceIn(1, maxQty))
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
        scope.launch {
            order.items.forEach { ci ->
                val currentProd = productDao.getProductByIdDirect(ci.product.id) ?: return@forEach
                val newStock = (currentProd.stock - ci.quantity).coerceAtLeast(0)
                if (newStock != currentProd.stock) {
                    productDao.updateProduct(currentProd.copy(stock = newStock))
                }
            }
        }
    }

    fun markOrderCancelledLocally(orderId: String) {
        updateOrderStatuses(mapOf(orderId to "Cancelled"))
    }

    fun updateOrderStatuses(statusUpdates: Map<String, String>) {
        if (statusUpdates.isEmpty()) return
        val current = localStore.loadOrders()
        var changed = false
        val updated = current.map { order ->
            val remoteStatus = statusUpdates[order.orderId]
            if (remoteStatus != null && remoteStatus != order.status) {
                changed = true
                order.copy(status = remoteStatus)
            } else {
                order
            }
        }
        if (changed) {
            localStore.saveOrders(updated)
            _orders.value = updated
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        _isPunjabi.value = (language == AppLanguage.PUNJABI)
        localStore.setAppLanguage(language)
    }

    fun toggleLanguage() {
        setAppLanguage(_appLanguage.value.next())
    }

    companion object {
        private const val TAG = "GroceryRepository"
        private const val RTDB_URL = "https://nav-grocery-store-default-rtdb.firebaseio.com/"
        const val DEFAULT_MARQUEE_TEXT =
            "Nav Grocery & NavKart Sandhaur 148020 • Live Website-Synced Marketplace Deals • Pind-to-Pind 11AM / 6PM Van • Spin & Win Rewards"
    }
}
