package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnEmeraldContainer

@Composable
fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    isPunjabi: Boolean,
    onCategorySelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = cat == selectedCategory
            val label = getCategoryDisplayLabel(cat, isPunjabi)

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(cat) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = EmeraldPrimary
                ),
                modifier = Modifier.testTag("filter_chip_$cat")
            )
        }
    }
}

fun getCategoryDisplayLabel(category: String, isPunjabi: Boolean): String {
    return when (category) {
        "All", "All Products" -> if (isPunjabi) "ਸਾਰੇ ਉਤਪਾਦ (308 SKUs)" else "All (308 SKUs)"
        "Packaged Groceries", "Grocery & Staples" -> if (isPunjabi) "ਸੀਲਬੰਦ ਗ੍ਰੋਸਰੀ" else "Packaged Groceries"
        "Cold Chain Frozen Foods", "Frozen Foods" -> if (isPunjabi) "ਕੋਲਡ ਚੇਨ ਫ੍ਰੋਜ਼ਨ ❄️" else "Frozen Foods ❄️"
        "Atta & Dals" -> if (isPunjabi) "ਆਟਾ ਅਤੇ ਦਾਲਾਂ" else "Atta & Dals"
        "Desi Ghee & Oils" -> if (isPunjabi) "ਦੇਸੀ ਘਿਓ ਅਤੇ ਤੇਲ" else "Desi Ghee & Oils"
        "Spices" -> if (isPunjabi) "ਮਸਾਲੇ (Spices)" else "Spices"
        "Clothing and Apparel", "Clothing & Apparel" -> if (isPunjabi) "ਕੱਪੜੇ ਅਤੇ ਪਹਿਰਾਵਾ" else "Clothing & Apparel"
        "Electronics and Mobiles", "Electronics & Gadgets" -> if (isPunjabi) "ਇਲੈਕਟ੍ਰਾਨਿਕਸ ਅਤੇ ਮੋਬਾਈਲ" else "Electronics & Mobiles"
        "Footwear and Shoes", "Footwear" -> if (isPunjabi) "ਜੁੱਤੀਆਂ ਅਤੇ ਸੈਂਡਲ" else "Footwear & Shoes"
        "Home and Kitchen", "Home Goods" -> if (isPunjabi) "ਘਰੇਲੂ ਅਤੇ ਰਸੋਈ" else "Home & Kitchen"
        "Personal Care" -> if (isPunjabi) "ਨਿੱਜੀ ਦੇਖਭਾਲ" else "Personal Care"
        else -> category
    }
}
