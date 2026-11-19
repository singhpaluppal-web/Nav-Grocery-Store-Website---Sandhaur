package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BabyChangingStation
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.MarketplaceCategory
import com.example.data.model.Product
import com.example.data.model.StoreOffersConfig
import com.example.data.util.ProductImageAutoFetcher
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmCreamBorder

private data class RailDepartment(
    val title: String,
    val punjabiTitle: String,
    val hindiTitle: String,
    val catalogFilterCategory: String,
    val icon: ImageVector,
    val keywords: List<String>
) {
    fun displayTitle(language: AppLanguage): String = when (language) {
        AppLanguage.PUNJABI -> punjabiTitle.ifBlank { title }
        AppLanguage.HINDI -> hindiTitle.ifBlank { title }
        AppLanguage.ENGLISH -> title
    }
}

private val SUPER_APP_RAIL_CATEGORIES = listOf(
    RailDepartment("Just for you", "ਤੁਹਾਡੇ ਲਈ ਖਾਸ", "आपके लिए खास", "All Products", Icons.Default.Star, emptyList()),
    RailDepartment("Pantry Staples", "ਆਟਾ ਤੇ ਦਾਲਾਂ", "आटा और दालें", "Atta & Dals", Icons.Default.Inventory2, listOf("atta", "dal", "rice", "besan", "sugar")),
    RailDepartment("Dairy and Ghee", "ਘਿਓ ਤੇ ਡੇਅਰੀ", "देसी घी और डेयरी", "Desi Ghee & Oils", Icons.Default.LocalDrink, listOf("ghee", "butter", "paneer", "milk", "oil")),
    RailDepartment("Cold Chain Frozen Foods", "ਫ੍ਰੋਜ਼ਨ ਭੋਜਨ", "फ्रोज़न फ़ूड्स", "Cold Chain Frozen Foods", Icons.Default.AcUnit, listOf("frozen", "mccain", "safal", "peas", "nuggets")),
    RailDepartment("Snacks and Beverages", "ਸਨੈਕਸ ਤੇ ਚਾਹ", "स्नैक्स और चाय", "Packaged Groceries", Icons.Default.Fastfood, listOf("biscuit", "tea", "coffee", "noodles", "juice")),
    RailDepartment("Personal Care", "ਪਰਸਨਲ ਕੇਅਰ", "पर्सनल केयर", "Personal Care", Icons.Default.Spa, listOf("soap", "shampoo", "cream", "toothpaste", "face")),
    RailDepartment("Household Cleaning", "ਘਰੇਲੂ ਸਫਾਈ", "घर की सफाई", "Household Cleaning", Icons.Default.CleaningServices, listOf("detergent", "surf", "ariel", "harpic", "cleaner")),
    RailDepartment("Electronics", "ਇਲੈਕਟ੍ਰਾਨਿਕਸ", "इलेक्ट्रॉनिक्स", "Electronics and Mobiles", Icons.Default.Devices, listOf("speaker", "headphone", "tv", "earbuds", "iron")),
    RailDepartment("Mobiles and Accessories", "ਮੋਬਾਈਲ & ਚਾਰਜਰ", "मोबाइल और चार्जर", "Electronics and Mobiles", Icons.Default.PhoneAndroid, listOf("5g", "smartphone", "watch", "power bank", "charger")),
    RailDepartment("Fashion", "ਕੱਪੜੇ ਤੇ ਜੁੱਤੇ", "कपड़े और जूते", "Clothing and Apparel", Icons.Default.Checkroom, listOf("shirt", "kurta", "jeans", "shoe", "puma", "bata")),
    RailDepartment("Home and Kitchen", "ਘਰ ਤੇ ਰਸੋਈ", "घर और रसोई", "Home and Kitchen", Icons.Default.Home, listOf("cooker", "mixer", "bedsheet", "curtain", "flask")),
    RailDepartment("Baby and Toys", "ਬੱਚੇ ਤੇ ਖਿਡੌਣੇ", "बेबी और खिलौने", "Baby and Toys", Icons.Default.BabyChangingStation, listOf("cerelac", "diaper", "baby", "johnson", "toy"))
)

@Composable
fun CategoriesScreen(
    allProducts: List<Product>,
    dynamicCategories: List<MarketplaceCategory> = emptyList(),
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    categoryDiscounts: Map<String, Double> = emptyMap(),
    recentCategories: List<String>,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    onBrowseCategory: (String) -> Unit,
    onSelectBrand: (String) -> Unit,
    onProductClick: (Product) -> Unit
) {
    val railCategories = remember(dynamicCategories) {
        val baseTitles = SUPER_APP_RAIL_CATEGORIES.map { it.catalogFilterCategory.lowercase() }.toSet()
        val extraRemote = dynamicCategories
            .filter { it.isActive && it.name.isNotBlank() && it.name.lowercase() !in baseTitles }
            .sortedBy { it.sortOrder }
            .map { cat ->
                RailDepartment(
                    title = cat.name,
                    punjabiTitle = cat.punjabiName.ifBlank { cat.name },
                    hindiTitle = cat.hindiName.ifBlank { cat.name },
                    catalogFilterCategory = cat.name,
                    icon = Icons.Default.Inventory2,
                    keywords = listOf(cat.name, cat.effectiveDepartment())
                )
            }
        SUPER_APP_RAIL_CATEGORIES + extraRemote
    }
    var selectedRail by remember(railCategories) {
        mutableStateOf(railCategories.firstOrNull() ?: SUPER_APP_RAIL_CATEGORIES.first())
    }
    val context = LocalContext.current

    val matchingProducts = remember(allProducts, selectedRail) {
        if (selectedRail.title == "Just for you") {
            allProducts.take(18)
        } else {
            val hits = allProducts.filter { p ->
                p.category.equals(selectedRail.catalogFilterCategory, ignoreCase = true) ||
                    p.department.equals(selectedRail.catalogFilterCategory, ignoreCase = true) ||
                    selectedRail.keywords.any { kw ->
                        p.name.contains(kw, ignoreCase = true) ||
                            p.category.contains(kw, ignoreCase = true) ||
                            p.department.contains(kw, ignoreCase = true)
                    }
            }
            hits.ifEmpty { allProducts.take(12) }.take(18)
        }
    }

    val topBrandsInPane = remember(matchingProducts) {
        matchingProducts.map { it.brand.trim() }
            .filter { it.isNotBlank() && it.length > 1 }
            .distinct()
            .take(8)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .testTag("split_pane_categories_screen")
    ) {
        // Scrollable Left Vertical Category Rail (Core + Remote A-to-Z Marketplace Departments)
        LazyColumn(
            modifier = Modifier
                .width(104.dp)
                .fillMaxHeight()
                .background(Color.White)
                .testTag("categories_left_rail")
        ) {
            items(railCategories, key = { it.title }) { dept ->
                val isSelected = dept.title == selectedRail.title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) WarmCream else Color.White)
                        .clickable { selectedRail = dept }
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(34.dp)
                                .background(ForestGreen, RoundedCornerShape(4.dp))
                        )
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ForestGreen else ForestGreenContainer.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = dept.icon,
                                contentDescription = dept.title,
                                tint = if (isSelected) SignatureGold else ForestGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dept.displayTitle(appLanguage),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) ForestGreenDark else Color(0xFF3E4F46),
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Right 3-Column Visual Subcategory Thumbnail Grid + Top Picks + Recently Viewed
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("categories_right_grid")
        ) {
            // Department Header & Direct Browse Button
            item(span = { GridItemSpan(3) }) {
                Surface(
                    color = ForestGreenDark,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedRail.displayTitle(appLanguage),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            val railDiscPct = categoryDiscounts[selectedRail.catalogFilterCategory] ?: offersConfig.sanitizedDiscountPercent
                            val railDiscLabel = StoreOffersConfig.formatPercent(railDiscPct)
                            val railReturnPolicy = dynamicCategories.firstOrNull {
                                it.name.equals(selectedRail.catalogFilterCategory, ignoreCase = true) ||
                                    it.department.equals(selectedRail.catalogFilterCategory, ignoreCase = true)
                            }?.returnPolicy?.takeIf { it.isNotBlank() } ?: when {
                                selectedRail.catalogFilterCategory.contains("Electronic", ignoreCase = true) ->
                                    appLanguage.tr("7-Day Replacement", "7-ਦਿਨ ਰਿਪਲੇਸਮੈਂਟ", "7-दिन रिप्लेसमेंट")
                                selectedRail.catalogFilterCategory.contains("Clothing", ignoreCase = true) ||
                                    selectedRail.catalogFilterCategory.contains("Footwear", ignoreCase = true) ->
                                    appLanguage.tr("7-Day Size Exchange / Return", "7-ਦਿਨ ਸਾਈਜ਼ ਐਕਸਚੇਂਜ", "7-दिन साइज़ एक्सचेंज / रिटर्न")
                                selectedRail.catalogFilterCategory.contains("Home", ignoreCase = true) ||
                                    selectedRail.catalogFilterCategory.contains("Household", ignoreCase = true) ->
                                    appLanguage.tr("7-Day Easy Replacement", "7-ਦਿਨ ਆਸਾਨ ਰਿਪਲੇਸਮੈਂਟ", "7-दिन आसान रिप्लेसमेंट")
                                selectedRail.catalogFilterCategory.contains("Frozen", ignoreCase = true) ->
                                    appLanguage.tr("Non-returnable cold-chain (damage covered)", "ਕੋਲਡ-ਚੇਨ ਨੁਕਸਾਨ ਕਵਰ", "नॉन-रिटर्नेबल कोल्ड-चेन")
                                else ->
                                    appLanguage.tr("Non-returnable sealed groceries (damage/expiry covered)", "ਸੀਲਬੰਦ ਨੁਕਸਾਨ/ਐਕਸਪਾਇਰੀ ਕਵਰ", "सीलबंद ग्रोसरी (क्षति/एक्सपायरी कवर)")
                            }
                            Text(
                                text = appLanguage.tr(
                                    english = "Flat $railDiscLabel% OFF • $railReturnPolicy",
                                    punjabi = "ਫਲੈਟ $railDiscLabel% ਛੋਟ • $railReturnPolicy",
                                    hindi = "फ्लैट $railDiscLabel% छूट • $railReturnPolicy"
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SignatureGold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Button(
                            onClick = { onBrowseCategory(selectedRail.catalogFilterCategory) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SignatureGold,
                                contentColor = ForestGreenDark
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = appLanguage.tr("Explore", "ਦੇਖੋ", "सभी देखें"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Recently Viewed Categories
            if (recentCategories.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = appLanguage.tr(
                                    english = "Recently Viewed Categories",
                                    punjabi = "ਹਾਲ ਹੀ ਵਿੱਚ ਦੇਖੀਆਂ ਸ਼੍ਰੇਣੀਆਂ",
                                    hindi = "हाल ही में देखी गई श्रेणियाँ"
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ForestGreenDark
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recentCategories.forEach { recentCat ->
                                Surface(
                                    onClick = { onBrowseCategory(recentCat) },
                                    color = Color.White,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, WarmCreamBorder)
                                ) {
                                    Text(
                                        text = recentCat,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ForestGreen,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Picks Section Header + Brand Quick Pills
            item(span = { GridItemSpan(3) }) {
                Column {
                    Text(
                        text = appLanguage.tr(
                            english = "Top Picks in ${selectedRail.title}",
                            punjabi = "ਟੌਪ ਪਿਕਸ (Top Picks)",
                            hindi = "${selectedRail.displayTitle(appLanguage)} में प्रमुख उत्पाद"
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestGreenDark
                        )
                    )
                    if (topBrandsInPane.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            topBrandsInPane.forEach { brand ->
                                Surface(
                                    onClick = { onSelectBrand(brand) },
                                    color = ForestGreenContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = brand,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ForestGreenDark,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3-Column Visual Product Thumbnail Grid
            items(matchingProducts, key = { it.id }) { prod ->
                Surface(
                    onClick = { onProductClick(prod) },
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WarmCreamBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(WarmCream),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(ProductImageAutoFetcher.resolveDisplayImageModel(context, prod))
                                    .memoryCacheKey("${prod.id}_${prod.fullImageUrl}")
                                    .placeholder(R.drawable.ic_nav_brand_logo)
                                    .error(R.drawable.ic_nav_brand_logo)
                                    .build(),
                                contentDescription = prod.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                            if (!prod.isInStock) {
                                Surface(
                                    color = Color(0xCC8B1A1A),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(2.dp)
                                ) {
                                    Text(
                                        text = appLanguage.tr("Out of Stock", "ਖਤਮ", "स्टॉक खत्म"),
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = prod.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreenDark
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        val livePrice = prod.sellingPrice(categoryDiscounts[prod.category], offersConfig)
                        Text(
                            text = "₹$livePrice",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ForestGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
