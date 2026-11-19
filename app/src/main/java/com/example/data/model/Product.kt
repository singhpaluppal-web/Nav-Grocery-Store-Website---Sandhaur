package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.util.ProductImageAutoFetcher
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlin.math.roundToInt

@Entity(tableName = "products")
@JsonClass(generateAdapter = true)
data class Product(
    @PrimaryKey
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "department") val department: String = "Packaged Groceries",
    @Json(name = "brand") val brand: String = "",
    @Json(name = "category") val category: String = "",
    @Json(name = "barcode") val barcode: String = "",
    @Json(name = "mrp") val mrp: Int = 0,
    @Json(name = "cost") val cost: Int = 0,
    @Json(name = "price") val price: Int = 0,
    @Json(name = "sale") val sale: Int = 0,
    @Json(name = "discountPercent") val discountPercent: Double? = null,
    @Json(name = "stock") val stock: Int = 25,
    @Json(name = "gst") val gst: String = "5%",
    @Json(name = "image") val image: String = "",
    @Json(name = "additionalImages") val additionalImages: String = "",
    @Json(name = "sellerId") val sellerId: String = "SELLER_NAV_STORE",
    @Json(name = "sellerName") val sellerName: String = "Nav Grocery Store",
    @Json(name = "rating") val rating: Float = 4.6f,
    @Json(name = "ratingCount") val ratingCount: Int = 120,
    @Json(name = "availableSizes") val availableSizes: String = "", // e.g. "S,M,L,XL,XXL" or "7,8,9,10,11"
    @Json(name = "availableColors") val availableColors: String = "", // e.g. "Forest Green,Maroon,Mustard Gold"
    @Json(name = "warranty") val warranty: String = "",
    @Json(name = "returnPolicy") val returnPolicy: String = "",
    @Json(name = "specs") val specs: String = "",
    @Json(name = "isVerifiedSeller") val isVerifiedSeller: Boolean = true,
    @Json(name = "approvalStatus") val approvalStatus: String = "APPROVED" // "APPROVED", "PENDING", "REJECTED"
) {
    /**
     * Legacy fallback wholesale price helper (uses offline default rate when no live store config is supplied).
     * UI and checkout always compute live selling price via [effectiveDiscountPercent] and [sellingPrice].
     */
    val wholesalePrice: Int
        get() = if (sale > 0 && discountPercent == null) {
            sale
        } else {
            sellingPrice(effectiveDiscountPercent(null, StoreOffersConfig()))
        }

    val savingsAmount: Int
        get() = maxOf(0, mrp - wholesalePrice)

    val savingsPercent: Int
        get() = if (mrp > 0) ((savingsAmount.toDouble() / mrp) * 100).roundToInt() else StoreOffersConfig.DEFAULT_DISCOUNT_PERCENT.roundToInt()

    fun effectiveDiscountPercent(
        categoryDiscountPercent: Double? = null,
        storeOffersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Double {
        return (discountPercent ?: categoryDiscountPercent ?: storeOffersConfig.discountPercent)
            .coerceIn(0.0, 90.0)
    }

    fun sellingPrice(effectiveDiscount: Double): Int {
        if (mrp <= 0) return 0
        val pct = effectiveDiscount.coerceIn(0.0, 90.0)
        return (mrp * (1.0 - pct / 100.0)).roundToInt().coerceAtLeast(0)
    }

    fun sellingPrice(
        categoryDiscountPercent: Double? = null,
        storeOffersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Int = sellingPrice(effectiveDiscountPercent(categoryDiscountPercent, storeOffersConfig))

    fun savingsAmountFor(
        categoryDiscountPercent: Double? = null,
        storeOffersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Int = maxOf(0, mrp - sellingPrice(categoryDiscountPercent, storeOffersConfig))

    val isBulkEligible: Boolean
        get() = !isSupermallItem ||
            category.contains("Grocery", ignoreCase = true) ||
            category.contains("Frozen", ignoreCase = true) ||
            category.contains("Atta", ignoreCase = true) ||
            category.contains("Ghee", ignoreCase = true) ||
            category.contains("Oil", ignoreCase = true) ||
            category.contains("Snack", ignoreCase = true) ||
            category.contains("Beverage", ignoreCase = true) ||
            name.contains("Biscuit", ignoreCase = true) ||
            name.contains("Oil", ignoreCase = true) ||
            name.contains("Pepsi", ignoreCase = true) ||
            name.contains("Coke", ignoreCase = true) ||
            name.contains("Juice", ignoreCase = true)

    val petiUnits: Int
        get() = when {
            name.contains("5kg", ignoreCase = true) || name.contains("10kg", ignoreCase = true) ||
                name.contains("5L", ignoreCase = true) || name.contains("5 L", ignoreCase = true) -> 4
            name.contains("1L", ignoreCase = true) || name.contains("1kg", ignoreCase = true) ||
                name.contains("Ghee", ignoreCase = true) || name.contains("Oil", ignoreCase = true) -> 10
            else -> 12
        }

    /** Full Carton (Peti) wholesale slab: additional volume rebate on top of live discounted price */
    fun petiTotalPriceFor(
        categoryDiscountPercent: Double? = null,
        storeOffersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Int {
        val unitSale = sellingPrice(categoryDiscountPercent, storeOffersConfig)
        return (unitSale * petiUnits * storeOffersConfig.petiSlabFactor).roundToInt()
    }

    fun petiExtraSavingsFor(
        categoryDiscountPercent: Double? = null,
        storeOffersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Int {
        val unitSale = sellingPrice(categoryDiscountPercent, storeOffersConfig)
        val total = petiTotalPriceFor(categoryDiscountPercent, storeOffersConfig)
        return ((unitSale * petiUnits) - total).coerceAtLeast(0)
    }

    val petiTotalPrice: Int
        get() = petiTotalPriceFor()

    val petiExtraSavings: Int
        get() = petiExtraSavingsFor()

    val fullImageUrl: String
        get() = ProductImageAutoFetcher.resolveStableImageUrl(this)

    val galleryImageUrls: List<String>
        get() {
            val primary = fullImageUrl
            val extras = additionalImages
                .split(",", "|", "\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { url ->
                    when {
                        url.startsWith("https://", ignoreCase = true) || url.startsWith("file://", ignoreCase = true) -> url
                        url.startsWith("http://", ignoreCase = true) -> "https://${url.substringAfter("://")}"
                        else -> "https://www.navgrocerystore.com/${url.removePrefix("/")}"
                    }
                }
            return (listOf(primary) + extras)
                .filter { it.isNotBlank() }
                .distinct()
        }

    val displayDescription: String
        get() {
            val cleaned = description.trim()
            if (cleaned.isNotBlank() && !cleaned.startsWith("100% genuine sealed", ignoreCase = true)) {
                return cleaned
            }
            return when {
                department.equals("Clothing and Apparel", ignoreCase = true) ||
                    category.contains("Clothing", ignoreCase = true) ->
                    "Tailored comfort fit • Breathable fabric • ${availableSizes.ifBlank { "S, M, L, XL, XXL" }}"
                department.equals("Footwear and Shoes", ignoreCase = true) ||
                    category.contains("Footwear", ignoreCase = true) ->
                    "Cushioned EVA insole • High-grip sole • ${availableSizes.ifBlank { "UK 6–11" }}"
                department.equals("Electronics and Mobiles", ignoreCase = true) ||
                    category.contains("Electronics", ignoreCase = true) ->
                    specs.ifBlank { "Original brand-sealed retail unit • Fast charging / HD performance" }
                department.equals("Home and Kitchen", ignoreCase = true) ||
                    category.contains("Home", ignoreCase = true) ->
                    specs.ifBlank { "Heavy-duty daily utility • Food-grade / long-life finish" }
                category.contains("Personal Care", ignoreCase = true) ->
                    specs.ifBlank { "Genuine factory-sealed personal care pack" }
                else -> when (category) {
                    "Atta & Dals" -> "${netWeightOrPackLabel.ifBlank { "Wholesale Pack" }} • Unpolished & stone-ground purity"
                    "Desi Ghee & Oils" -> "${netWeightOrPackLabel.ifBlank { "Sealed Jar/Pouch" }} • Authentic cooking oil & pure desi ghee aroma"
                    "Frozen Foods", "Cold Chain Frozen Foods" -> "${netWeightOrPackLabel.ifBlank { "Frozen Pack" }} • Sub-zero cold-chain preserved"
                    "Spices" -> "${netWeightOrPackLabel.ifBlank { "Sealed Masala" }} • Freshly milled authentic flavour"
                    else -> "${netWeightOrPackLabel.ifBlank { "Sealed Pack" }} • Factory sealed wholesale pack"
                }
            }
        }

    val netWeightOrPackLabel: String
        get() {
            val match = Regex("(\\d+(?:\\.\\d+)?\\s*(?:kg|g|gm|ml|ltr|l|pcs|pack of \\d+))", RegexOption.IGNORE_CASE).find(name)
            return match?.value?.trim().orEmpty()
        }

    val isApparelOrFootwear: Boolean
        get() = department.contains("Clothing", ignoreCase = true) ||
                category.contains("Clothing", ignoreCase = true) ||
                department.contains("Footwear", ignoreCase = true) ||
                category.contains("Footwear", ignoreCase = true)

    val isElectronics: Boolean
        get() = department.contains("Electronics", ignoreCase = true) ||
                category.contains("Electronics", ignoreCase = true)

    val isSupermallItem: Boolean
        get() = isApparelOrFootwear || isElectronics ||
                department.contains("Clothing", ignoreCase = true) ||
                department.contains("Footwear", ignoreCase = true) ||
                department.contains("Home", ignoreCase = true) ||
                category.contains("Home", ignoreCase = true) ||
                category.contains("Personal Care", ignoreCase = true) ||
                category.contains("Appliance", ignoreCase = true) ||
                category.contains("Baby", ignoreCase = true) ||
                category.contains("Toy", ignoreCase = true)

    val cardCategorySubtitle: String
        get() = when {
            category.contains("Clothing", ignoreCase = true) || department.contains("Clothing", ignoreCase = true) -> {
                val sizes = availableSizes.ifBlank { "S, M, L, XL, XXL" }
                val fabric = specs.substringBefore("|").trim().ifBlank { "Premium Cotton Blend" }
                "Sizes: $sizes • $fabric"
            }
            category.contains("Footwear", ignoreCase = true) || department.contains("Footwear", ignoreCase = true) -> {
                val sizes = availableSizes.ifBlank { "UK 6–11" }
                val sole = specs.substringBefore("|").trim().ifBlank { "Cushioned Grip Sole" }
                "$sizes • $sole"
            }
            isElectronics -> {
                specs.ifBlank { warranty.ifBlank { "Certified Original • 1 Yr Brand Warranty" } }
            }
            department.contains("Home", ignoreCase = true) || category.contains("Home", ignoreCase = true) -> {
                specs.ifBlank { warranty.ifBlank { "Durable Daily Utility • Official Pack" } }
            }
            else -> {
                val wt = netWeightOrPackLabel.ifBlank { "Sealed Pack" }
                if (isColdChain) "Net Wt: $wt • Cold-Chain Frozen (-18°C)"
                else "Net Wt: $wt • FSSAI 100% Sealed"
            }
        }

    val cardTrustBadgeText: String
        get() = when {
            isElectronics -> warranty.ifBlank { "1 Year Brand Warranty" }
            isApparelOrFootwear -> "7-Day Size Exchange Guarantee"
            department.contains("Home", ignoreCase = true) || category.contains("Home", ignoreCase = true) ->
                warranty.ifBlank { "7-Day Easy Replacement" }
            isColdChain -> "Cold-Chain Frozen • FSSAI Sealed"
            else -> "FSSAI 100% Sealed • Batch Verified"
        }

    val isInStock: Boolean
        get() = stock > 0

    fun effectiveReturnWindowBadge(
        language: AppLanguage = AppLanguage.ENGLISH,
        categoryReturnPolicy: String = ""
    ): String {
        val remotePolicy = returnPolicy.trim().ifBlank { categoryReturnPolicy.trim() }
        if (remotePolicy.isNotBlank() && language == AppLanguage.ENGLISH) {
            return remotePolicy
        }
        return when {
            isElectronics -> language.tr(
                english = remotePolicy.ifBlank { "7-Day Replacement" },
                punjabi = "7-ਦਿਨ ਰਿਪਲੇਸਮੈਂਟ",
                hindi = "7-दिन रिप्लेसमेंट"
            )
            isApparelOrFootwear -> language.tr(
                english = remotePolicy.ifBlank { "7-Day Size Exchange / Return" },
                punjabi = "7-ਦਿਨ ਸਾਈਜ਼ ਐਕਸਚੇਂਜ / ਰਿਟਰਨ",
                hindi = "7-दिन साइज़ एक्सचेंज / रिटर्न"
            )
            department.contains("Home", ignoreCase = true) || category.contains("Home", ignoreCase = true) ||
                category.contains("Household", ignoreCase = true) -> language.tr(
                english = remotePolicy.ifBlank { "7-Day Easy Replacement" },
                punjabi = "7-ਦਿਨ ਆਸਾਨ ਰਿਪਲੇਸਮੈਂਟ",
                hindi = "7-दिन आसान रिप्लेसमेंट"
            )
            isColdChain -> language.tr(
                english = remotePolicy.ifBlank { "Non-returnable cold-chain item (damage/melt covered)" },
                punjabi = "ਨਾ-ਵਾਪਸੀਯੋਗ ਕੋਲਡ-ਚੇਨ ਆਈਟਮ (ਨੁਕਸਾਨ ਕਵਰ)",
                hindi = "नॉन-रिटर्नेबल कोल्ड-चेन आइटम (क्षति पर सहायता)"
            )
            else -> language.tr(
                english = remotePolicy.ifBlank { "Non-returnable sealed item (damage/expiry covered)" },
                punjabi = "ਨਾ-ਵਾਪਸੀਯੋਗ ਸੀਲਬੰਦ ਆਈਟਮ (ਨੁਕਸਾਨ/ਐਕਸਪਾਇਰੀ ਕਵਰ)",
                hindi = "नॉन-रिटर्नेबल सीलबंद आइटम (क्षति/एक्सपायरी कवर)"
            )
        }
    }

    val isColdChain: Boolean
        get() = category.contains("Frozen", ignoreCase = true) ||
                department.contains("Frozen", ignoreCase = true) ||
                name.contains("Frozen", ignoreCase = true) ||
                name.contains("Paneer", ignoreCase = true)

    val sizeList: List<String>
        get() = if (availableSizes.isNotBlank()) {
            availableSizes.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else if (category.contains("Clothing", ignoreCase = true) || department.contains("Clothing", ignoreCase = true)) {
            listOf("S", "M", "L", "XL", "XXL")
        } else if (category.contains("Footwear", ignoreCase = true) || department.contains("Footwear", ignoreCase = true)) {
            listOf("UK 6", "UK 7", "UK 8", "UK 9", "UK 10", "UK 11")
        } else {
            emptyList()
        }

    val colorList: List<String>
        get() = if (availableColors.isNotBlank()) {
            availableColors.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else if (category.contains("Clothing", ignoreCase = true) || department.contains("Clothing", ignoreCase = true)) {
            listOf("Royal Emerald", "Ivory Cream", "Classic Navy")
        } else if (category.contains("Footwear", ignoreCase = true) || department.contains("Footwear", ignoreCase = true)) {
            listOf("Classic Black", "Pure White", "Slate Grey")
        } else {
            emptyList()
        }
}
