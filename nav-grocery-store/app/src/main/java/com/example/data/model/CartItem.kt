package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val selectedSize: String = "",
    val selectedColor: String = ""
) {
    val totalMrp: Int get() = product.mrp * quantity
    val totalSalePrice: Int get() = product.wholesalePrice * quantity
    val totalSavings: Int get() = totalMrp - totalSalePrice

    val variantSummary: String
        get() {
            val parts = mutableListOf<String>()
            if (selectedSize.isNotBlank()) parts.add("Size: $selectedSize")
            if (selectedColor.isNotBlank()) parts.add("Color: $selectedColor")
            return parts.joinToString(" • ")
        }
}
