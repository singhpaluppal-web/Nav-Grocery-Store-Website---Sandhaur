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
import com.example.data.model.AppLanguage
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnEmeraldContainer

@Composable
fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
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
            val label = getCategoryDisplayLabel(cat, appLanguage)

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

fun getCategoryDisplayLabel(category: String, isPunjabi: Boolean): String =
    getCategoryDisplayLabel(category, if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH)

fun getCategoryDisplayLabel(category: String, appLanguage: AppLanguage): String {
    return when (category) {
        "All", "All Products" -> appLanguage.tr("All Products", "ਸਾਰੇ ਉਤਪਾਦ", "सभी उत्पाद")
        "Packaged Groceries", "Grocery & Staples" -> appLanguage.tr("Packaged Groceries", "ਸੀਲਬੰਦ ਗ੍ਰੋਸਰੀ", "सीलबंद किराना")
        "Cold Chain Frozen Foods", "Frozen Foods" -> appLanguage.tr("Frozen Foods ❄️", "ਕੋਲਡ ਚੇਨ ਫ੍ਰੋਜ਼ਨ ❄️", "फ्रोज़न फ़ूड्स ❄️")
        "Atta & Dals" -> appLanguage.tr("Atta & Dals", "ਆਟਾ ਅਤੇ ਦਾਲਾਂ", "आटा और दालें")
        "Desi Ghee & Oils" -> appLanguage.tr("Desi Ghee & Oils", "ਦੇਸੀ ਘਿਓ ਅਤੇ ਤੇਲ", "देसी घी और तेल")
        "Spices" -> appLanguage.tr("Spices", "ਮਸਾਲੇ (Spices)", "मसाले (Spices)")
        "Clothing and Apparel", "Clothing & Apparel" -> appLanguage.tr("Clothing & Apparel", "ਕੱਪੜੇ ਅਤੇ ਪਹਿਰਾਵਾ", "कपड़े और परिधान")
        "Electronics and Mobiles", "Electronics & Gadgets" -> appLanguage.tr("Electronics & Mobiles", "ਇਲੈਕਟ੍ਰਾਨਿਕਸ ਅਤੇ ਮੋਬਾਈਲ", "इलेक्ट्रॉनिक्स और मोबाइल")
        "Footwear and Shoes", "Footwear" -> appLanguage.tr("Footwear & Shoes", "ਜੁੱਤੀਆਂ ਅਤੇ ਸੈਂਡਲ", "जूते और चप्पल")
        "Home and Kitchen", "Home Goods" -> appLanguage.tr("Home & Kitchen", "ਘਰੇਲੂ ਅਤੇ ਰਸੋਈ", "घर और रसोई")
        "Personal Care" -> appLanguage.tr("Personal Care", "ਨਿੱਜੀ ਦੇਖਭਾਲ", "पर्सनल केयर")
        else -> category
    }
}
