package com.example.data.util

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.api.RetrofitClient
import com.example.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale

data class ImageCacheSummary(
    val totalChecked: Int,
    val missingFromFirebaseResolved: Int,
    val fetchedAndCachedCount: Int,
    val alreadyCachedCount: Int,
    val failedSkus: List<String>
)

/**
 * Utility for resolving, fetching, and locally caching packaging photos for NavKart SKUs
 * (especially NAV309..NAV808) using a verified product-name-to-URL mapping whenever Firebase
 * omits or serves a blank/broken image reference.
 */
object ProductImageAutoFetcher {

    private const val CACHE_DIR_NAME = "navkart_sku_images"
    private val NAV309_TO_808_RANGE = 309..808

    private val allCatalogEntries: List<Triple<String, String, String>> by lazy {
        ProductPackagingImageMapGroceries.entries + ProductPackagingImageMapLifestyle.entries
    }

    /**
     * Exact product name -> stable image source URL mapping for all 500 SKUs (NAV309..NAV808).
     */
    val productNameToImageUrl: Map<String, String> by lazy {
        allCatalogEntries.associate { (_, name, url) -> name to url }
    }

    /**
     * Normalized lowercase product name -> stable image source URL mapping for resilient matching.
     */
    val normalizedNameToImageUrl: Map<String, String> by lazy {
        allCatalogEntries.associate { (_, name, url) -> normalizeProductName(name) to url }
    }

    /**
     * Immutable SKU ID (NAV309..NAV808) -> stable image source URL mapping.
     */
    val skuToImageUrl: Map<String, String> by lazy {
        allCatalogEntries.associate { (sku, _, url) -> sku to url }
    }

    private val tokenizedCatalogEntries: List<Pair<Set<String>, String>> by lazy {
        allCatalogEntries.map { (_, name, url) ->
            tokenizeName(name) to url
        }
    }

    fun normalizeProductName(name: String): String {
        return name.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    private fun tokenizeName(name: String): Set<String> {
        return normalizeProductName(name)
            .split(" ")
            .filter { it.length >= 2 }
            .toSet()
    }

    fun isNav309To808Sku(skuId: String): Boolean {
        val cleaned = skuId.trim().uppercase(Locale.ROOT)
        if (!cleaned.startsWith("NAV")) return false
        val num = cleaned.removePrefix("NAV").toIntOrNull() ?: return false
        return num in NAV309_TO_808_RANGE
    }

    private val legacyBrokenPatterns = listOf(
        "wikimedia.org",
        "wikipedia.org",
        "egg-slicer",
        "selfie-stick-monopod",
        "tv-studio-camera-pedestal",
        "black-whisk",
        "calvin-klein-heel-shoes",
        "nike-baseball-cleats",
        "fragrances/",
        "realme-x"
    )

    fun isMissingOrBrokenImage(imageUrl: String?, skuId: String = ""): Boolean {
        val cleaned = imageUrl?.trim().orEmpty()
        if (cleaned.isBlank()) return true
        val lower = cleaned.lowercase(Locale.ROOT)
        if (lower == "null" || lower == "undefined" || lower.contains("placeholder")) return true
        if (legacyBrokenPatterns.any { lower.contains(it) }) return true

        val isHttp = lower.startsWith("http://") || lower.startsWith("https://")
        if (isHttp) return false

        // NAV001..NAV308 legitimately use bundled/verified batch paths ("images/batch_X/Y.png"),
        // whereas NAV309..NAV808 must resolve to real external URLs rather than unbacked relative paths.
        if (isNav309To808Sku(skuId)) return true
        return !lower.startsWith("images/")
    }

    /**
     * Looks up a stable packaging photo URL using exact product name, normalized product name,
     * token overlap matching, or SKU ID fallback.
     */
    fun resolveImageUrlByNameOrSku(productName: String, skuId: String = ""): String? {
        val trimmedName = productName.trim()
        if (trimmedName.isNotEmpty()) {
            productNameToImageUrl[trimmedName]?.let { return it }

            val normalized = normalizeProductName(trimmedName)
            normalizedNameToImageUrl[normalized]?.let { return it }

            val queryTokens = tokenizeName(trimmedName)
            if (queryTokens.size >= 2) {
                var bestScore = 0
                var bestUrl: String? = null
                for ((entryTokens, url) in tokenizedCatalogEntries) {
                    val overlap = queryTokens.count { it in entryTokens }
                    if (overlap >= 2 && overlap > bestScore) {
                        bestScore = overlap
                        bestUrl = url
                    }
                }
                if (bestUrl != null) return bestUrl
            }
        }

        val normalizedSku = skuId.trim().uppercase(Locale.ROOT)
        if (normalizedSku.isNotEmpty()) {
            skuToImageUrl[normalizedSku]?.let { return it }
        }
        return null
    }

    /**
     * Resolves the canonical URL for a Product, automatically filling missing/broken Firebase image
     * fields for NAV309..NAV808 (and matching product names) from the stable URL mapping.
     */
    fun resolveStableImageUrl(product: Product): String {
        val cleaned = product.image.trim()
        if (!isMissingOrBrokenImage(cleaned, product.id)) {
            return when {
                cleaned.startsWith("https://") -> cleaned
                cleaned.startsWith("http://") -> "https://${cleaned.removePrefix("http://")}"
                else -> "https://www.navgrocerystore.com/${cleaned.removePrefix("/")}"
            }
        }

        val resolved = resolveImageUrlByNameOrSku(product.name, product.id)
        if (resolved != null) {
            return if (resolved.startsWith("http://")) "https://${resolved.removePrefix("http://")}" else resolved
        }

        return "https://www.navgrocerystore.com/images/verified/${product.id.trim()}.jpeg"
    }

    /**
     * Returns a copy of [product] guaranteed to have its `image` field populated if it was missing
     * from Firebase and matches a known product name or NAV309..NAV808 SKU.
     */
    fun ensureProductHasImage(product: Product): Product {
        if (!isMissingOrBrokenImage(product.image, product.id)) return product
        val resolved = resolveImageUrlByNameOrSku(product.name, product.id) ?: return product
        return product.copy(image = resolved)
    }

    /**
     * Ensures every product in [products]—including all SKUs NAV309..NAV808—has an associated
     * stable image URL when missing from Firebase.
     */
    fun ensureCatalogImages(products: List<Product>): List<Product> {
        return products.map { ensureProductHasImage(it) }
    }

    fun getCacheDirectory(context: Context): File {
        return File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    fun getCachedPhotoFile(context: Context, skuId: String): File? {
        val safeSku = sanitizeFileKey(skuId)
        if (safeSku.isBlank()) return null
        val file = File(getCacheDirectory(context), "$safeSku.img")
        return if (file.exists() && file.length() > 0L) file else null
    }

    /**
     * Returns the locally cached packaging photo file if already fetched, otherwise the resolved
     * remote URL so Coil seamlessly uses the offline cache first.
     */
    fun resolveDisplayImageModel(context: Context, product: Product): Any {
        return getCachedPhotoFile(context, product.id) ?: resolveStableImageUrl(product)
    }

    /**
     * Automatically fetches and caches product packaging photos onto local app cache storage and
     * Coil's image cache whenever they are missing from Firebase (or optionally for all uncached
     * NAV309..NAV808 SKUs).
     */
    suspend fun fetchAndCacheMissingPackagingPhotos(
        context: Context,
        products: List<Product>,
        missingFromFirebaseSkus: Set<String> = emptySet(),
        prefetchAllNav309To808: Boolean = false,
        httpClient: OkHttpClient = RetrofitClient.sharedOkHttpClient,
        imageLoader: ImageLoader? = Coil.imageLoader(context),
        concurrency: Int = 6
    ): ImageCacheSummary = withContext(Dispatchers.IO) {
        val cacheDir = getCacheDirectory(context)
        val semaphore = Semaphore(concurrency.coerceIn(1, 12))

        val candidates = products.filter { product ->
            val missingFromCloud = product.id in missingFromFirebaseSkus ||
                isMissingOrBrokenImage(product.image, product.id)
            missingFromCloud || (prefetchAllNav309To808 && isNav309To808Sku(product.id))
        }

        if (candidates.isEmpty()) {
            return@withContext ImageCacheSummary(
                totalChecked = products.size,
                missingFromFirebaseResolved = 0,
                fetchedAndCachedCount = 0,
                alreadyCachedCount = 0,
                failedSkus = emptyList()
            )
        }

        var missingResolvedCount = 0
        val resolvedCandidates = candidates.mapNotNull { product ->
            val url = resolveStableImageUrl(product)
            if (url.startsWith("http://") || url.startsWith("https://")) {
                if (product.id in missingFromFirebaseSkus || isMissingOrBrokenImage(product.image, product.id)) {
                    missingResolvedCount++
                }
                product to url
            } else {
                null
            }
        }

        val outcomes = coroutineScope {
            resolvedCandidates.map { (product, imageUrl) ->
                async {
                    semaphore.withPermit {
                        fetchAndCacheSingle(
                            context = context,
                            cacheDir = cacheDir,
                            skuId = product.id,
                            imageUrl = imageUrl,
                            httpClient = httpClient,
                            imageLoader = imageLoader
                        )
                    }
                }
            }.awaitAll()
        }

        val fetchedCount = outcomes.count { it == SingleFetchOutcome.FETCHED }
        val alreadyCount = outcomes.count { it == SingleFetchOutcome.ALREADY_CACHED }
        val failedSkus = resolvedCandidates.zip(outcomes)
            .filter { (_, outcome) -> outcome == SingleFetchOutcome.FAILED }
            .map { (pair, _) -> pair.first.id }

        ImageCacheSummary(
            totalChecked = candidates.size,
            missingFromFirebaseResolved = missingResolvedCount,
            fetchedAndCachedCount = fetchedCount,
            alreadyCachedCount = alreadyCount,
            failedSkus = failedSkus
        )
    }

    private enum class SingleFetchOutcome {
        ALREADY_CACHED,
        FETCHED,
        FAILED
    }

    private fun fetchAndCacheSingle(
        context: Context,
        cacheDir: File,
        skuId: String,
        imageUrl: String,
        httpClient: OkHttpClient,
        imageLoader: ImageLoader?
    ): SingleFetchOutcome {
        val safeSku = sanitizeFileKey(skuId)
        val targetFile = File(cacheDir, "$safeSku.img")
        val urlStampFile = File(cacheDir, "$safeSku.url")

        if (targetFile.exists() && targetFile.length() > 0L &&
            urlStampFile.exists() && urlStampFile.readText() == imageUrl
        ) {
            primeCoilCache(context, imageLoader, skuId, imageUrl, targetFile)
            return SingleFetchOutcome.ALREADY_CACHED
        }

        return try {
            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "NavKart-Android/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val contentType = response.header("Content-Type")?.lowercase(Locale.ROOT).orEmpty()
                val bodyBytes = response.body?.bytes()
                if (!response.isSuccessful || bodyBytes == null || bodyBytes.isEmpty()) {
                    return SingleFetchOutcome.FAILED
                }
                if (contentType.isNotEmpty() &&
                    !contentType.startsWith("image/") &&
                    !contentType.contains("octet-stream")
                ) {
                    return SingleFetchOutcome.FAILED
                }

                val tmpFile = File(cacheDir, "$safeSku.tmp")
                tmpFile.writeBytes(bodyBytes)
                if (targetFile.exists()) targetFile.delete()
                if (!tmpFile.renameTo(targetFile)) {
                    targetFile.writeBytes(bodyBytes)
                    tmpFile.delete()
                }
                urlStampFile.writeText(imageUrl)
                primeCoilCache(context, imageLoader, skuId, imageUrl, targetFile)
                SingleFetchOutcome.FETCHED
            }
        } catch (_: Exception) {
            SingleFetchOutcome.FAILED
        }
    }

    private fun primeCoilCache(
        context: Context,
        imageLoader: ImageLoader?,
        skuId: String,
        imageUrl: String,
        cachedFile: File
    ) {
        if (imageLoader == null) return
        try {
            val cacheKey = "${skuId}_${imageUrl}"
            val request = ImageRequest.Builder(context)
                .data(cachedFile)
                .memoryCacheKey(cacheKey)
                .diskCacheKey(cacheKey)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
            imageLoader.enqueue(request)
        } catch (_: Exception) {
            // Best-effort Coil warm-up; disk file is already cached.
        }
    }

    private fun sanitizeFileKey(skuId: String): String {
        return skuId.trim().replace(Regex("[^A-Za-z0-9_-]"), "_")
    }
}
