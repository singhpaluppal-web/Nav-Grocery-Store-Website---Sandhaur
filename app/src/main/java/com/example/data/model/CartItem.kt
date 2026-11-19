package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val selectedSize: String = "",
    val selectedColor: String = "",
    val isPetiCarton: Boolean = false,
    val petiUnits: Int = 1
) {
    val effectiveUnitsPerItem: Int
        get() = if (isPetiCarton && petiUnits > 1) petiUnits else 1

    val totalMrp: Int
        get() = product.mrp * effectiveUnitsPerItem * quantity

    fun unitSalePriceFor(
        categoryDiscountPercent: Double? = null,
        storeOffersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Int = product.sellingPrice(categoryDiscountPercent, storeOffersConfig)

    fun totalSalePriceFor(
        categoryDiscountPercent: Double? = null,
        storeOffersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Int = if (isPetiCarton && petiUnits > 1) {
        product.petiTotalPriceFor(categoryDiscountPercent, storeOffersConfig) * quantity
    } else {
        unitSalePriceFor(categoryDiscountPercent, storeOffersConfig) * quantity
    }

    fun baseWholesaleTotalFor(
        categoryDiscountPercent: Double? = null,
        storeOffersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Int = unitSalePriceFor(categoryDiscountPercent, storeOffersConfig) * effectiveUnitsPerItem * quantity

    fun petiExtraSavingsFor(
        categoryDiscountPercent: Double? = null,
        storeOffersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Int = (baseWholesaleTotalFor(categoryDiscountPercent, storeOffersConfig) -
        totalSalePriceFor(categoryDiscountPercent, storeOffersConfig)).coerceAtLeast(0)

    fun totalSavingsFor(
        categoryDiscountPercent: Double? = null,
        storeOffersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Int = (totalMrp - totalSalePriceFor(categoryDiscountPercent, storeOffersConfig)).coerceAtLeast(0)

    val totalSalePrice: Int
        get() = totalSalePriceFor()

    val baseWholesaleTotal: Int
        get() = baseWholesaleTotalFor()

    val petiExtraSavings: Int
        get() = petiExtraSavingsFor()

    val totalSavings: Int
        get() = totalSavingsFor()

    val variantSummary: String
        get() {
            val parts = mutableListOf<String>()
            if (isPetiCarton && petiUnits > 1) parts.add("Peti Carton (${petiUnits}x)")
            if (selectedSize.isNotBlank()) parts.add("Size: $selectedSize")
            if (selectedColor.isNotBlank()) parts.add("Color: $selectedColor")
            return parts.joinToString(" • ")
        }
}
