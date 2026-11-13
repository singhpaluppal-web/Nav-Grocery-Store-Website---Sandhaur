package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
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
    @Json(name = "stock") val stock: Int = 25,
    @Json(name = "gst") val gst: String = "5%",
    @Json(name = "image") val image: String = "",
    @Json(name = "sellerId") val sellerId: String = "SELLER_NAV_STORE",
    @Json(name = "sellerName") val sellerName: String = "Nav Grocery Store",
    @Json(name = "rating") val rating: Float = 4.6f,
    @Json(name = "ratingCount") val ratingCount: Int = 120,
    @Json(name = "availableSizes") val availableSizes: String = "", // e.g. "S,M,L,XL,XXL" or "7,8,9,10,11"
    @Json(name = "availableColors") val availableColors: String = "", // e.g. "Forest Green,Maroon,Mustard Gold"
    @Json(name = "warranty") val warranty: String = "",
    @Json(name = "specs") val specs: String = "",
    @Json(name = "isVerifiedSeller") val isVerifiedSeller: Boolean = true,
    @Json(name = "approvalStatus") val approvalStatus: String = "APPROVED" // "APPROVED", "PENDING", "REJECTED"
) {
    /**
     * Navo Hub Wholesale Pricing Rule:
     * Flat 12% OFF on MRP across groceries, or seller configured sale price
     */
    val wholesalePrice: Int
        get() = if (sale > 0) sale else (mrp * 0.88).roundToInt()

    val savingsAmount: Int
        get() = maxOf(0, mrp - wholesalePrice)

    val savingsPercent: Int
        get() = if (mrp > 0) ((savingsAmount.toDouble() / mrp) * 100).roundToInt() else 12

    val fullImageUrl: String
        get() {
            return when {
                image.isBlank() -> ""
                image.startsWith("http://") || image.startsWith("https://") -> image
                else -> "https://www.navgrocerystore.com/${image.removePrefix("/")}"
            }
        }

    val displayDescription: String
        get() {
            if (description.isNotBlank()) return description
            return when (department) {
                "Clothing & Apparel" -> "Authentic premium fabric tailored with traditional Punjabi craftsmanship, rich dye quality, and comfort fit."
                "Electronics & Mobiles" -> "Original certified device with manufacturer warranty, fast charging support, and official invoice."
                "Footwear & Shoes" -> "Durable high-grip sole with cushioned insole for all-day comfort and athletic styling."
                "Home & Lifestyle" -> "Premium grade stainless steel / durable kitchenware designed for long-lasting daily utility."
                else -> when (category) {
                    "Atta & Dals" -> "100% genuine sealed stone-ground flour & high-protein unpolished pulses."
                    "Desi Ghee & Oils" -> "Authentic traditional cooking oil & pure desi ghee sealed for authentic aroma."
                    "Frozen Foods" -> "Sub-zero cold-chain preserved package. Retains freshness and nutrition without preservatives."
                    "Spices" -> "Freshly milled, sealed pure spices and masalas for authentic Punjabi flavour."
                    else -> "Premium sealed packaged grocery item at flat 12% wholesale discount."
                }
            }
        }

    val isColdChain: Boolean
        get() = category.equals("Frozen Foods", ignoreCase = true) ||
                name.contains("Frozen", ignoreCase = true) ||
                name.contains("Paneer", ignoreCase = true)

    val sizeList: List<String>
        get() = if (availableSizes.isNotBlank()) availableSizes.split(",").map { it.trim() } else emptyList()

    val colorList: List<String>
        get() = if (availableColors.isNotBlank()) availableColors.split(",").map { it.trim() } else emptyList()
}
