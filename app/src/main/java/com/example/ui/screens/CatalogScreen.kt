package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.HeroBannerSlide
import com.example.data.model.PriceFilterRange
import com.example.data.model.Product
import com.example.data.model.SmartComboHamper
import com.example.data.model.StoreHeroPromoConfig
import com.example.data.model.StoreOffersConfig
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.ColdChainCountdownBadge
import com.example.ui.components.MobileFooterSection
import com.example.ui.components.ProductCard
import com.example.ui.components.WholesaleSavingsBanner
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmCreamBorder

@Composable
fun CatalogScreen(
    products: List<Product>,
    heroBanners: List<HeroBannerSlide> = HeroBannerSlide.DEFAULT_SLIDES,
    categories: List<String>,
    selectedCategory: String,
    selectedDepartmentScope: String = "ALL",
    brands: List<String>,
    selectedBrand: String,
    selectedSort: String,
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    heroPromoConfig: StoreHeroPromoConfig = StoreHeroPromoConfig(),
    categoryDiscounts: Map<String, Double> = emptyMap(),
    categoryReturnPolicies: Map<String, String> = emptyMap(),
    selectedPriceRange: PriceFilterRange = PriceFilterRange.ALL,
    wishlistIds: List<String> = emptyList(),
    cartItems: List<CartItem>,
    cartTotalCount: Int,
    cartTotalSalePrice: Int,
    hasColdChainInCart: Boolean = false,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    onDepartmentScopeSelected: (String) -> Unit = {},
    onCategorySelected: (String) -> Unit,
    onBrandSelected: (String) -> Unit,
    onPriceRangeSelected: (PriceFilterRange) -> Unit = {},
    onSortSelected: (String) -> Unit,
    onToggleWishlist: (String) -> Unit = {},
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product, Boolean) -> Unit,
    onAddComboHamper: (SmartComboHamper) -> Unit = {},
    onUpdateQuantity: (String, Int) -> Unit,
    onViewCartClick: () -> Unit,
    onOpenDealsClick: () -> Unit = {},
    onStoreInfoClick: () -> Unit,
    onEmailSupport: () -> Unit = {},
    onWhatsAppSupport: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(bottom = if (cartTotalCount > 0) 90.dp else 16.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(WarmCream)
        ) {
            // 1. Unified Hub Switcher Strip (All Products | Nav Grocery | NavKart Supermall)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmCream)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val scopes = listOf(
                        "ALL" to appLanguage.tr("All Products", "ਸਾਰੇ ਉਤਪਾਦ", "सभी उत्पाद"),
                        "GROCERY" to appLanguage.tr("Nav Grocery", "ਨਵ ਗ੍ਰੋਸਰੀ", "नव ग्रोसरी"),
                        "SUPERMALL" to appLanguage.tr("NavKart", "ਨਵਕਾਰਟ", "नवकार्ट")
                    )
                    scopes.forEach { (scopeKey, label) ->
                        val active = selectedDepartmentScope == scopeKey
                        Surface(
                            onClick = { onDepartmentScopeSelected(scopeKey) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (active) ForestGreen else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (active) ForestGreen else WarmCreamBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scope_pill_${scopeKey.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (active) Color.White else ForestGreen,
                                        fontSize = 11.5.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 2. Luxury 4-Slide Multi-Category Hero Carousel
            item(span = { GridItemSpan(maxLineSpan) }) {
                WholesaleSavingsBanner(
                    slides = heroBanners,
                    offersConfig = offersConfig,
                    heroPromoConfig = heroPromoConfig,
                    isPunjabi = isPunjabi,
                    appLanguage = appLanguage,
                    onSlideCtaClick = { targetCategory -> onCategorySelected(targetCategory) },
                    onOpenPromoVideo = { url -> onOpenUrl(url) },
                    onStoreInfoClick = onStoreInfoClick
                )
            }

            // 2b. Curated Smart Combos & Festival Shagun Gift Hampers Strip
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = appLanguage.tr(
                                english = "Smart Combos & Shagun Gift Hampers",
                                punjabi = "ਸਮਾਰਟ ਕੌਂਬੋ & ਸ਼ਗਨ ਗਿਫਟ ਹੈਂਪਰ",
                                hindi = "स्मार्ट कॉम्बो और शगुन गिफ्ट हैंपर"
                            ),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = ForestGreenDark
                            )
                        )
                        Text(
                            text = "1-Tap Bundle",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ForestGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartComboHamper.FEATURED_HAMPERS.forEach { hamper ->
                            Surface(
                                onClick = { onAddComboHamper(hamper) },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, SignatureGold),
                                modifier = Modifier.width(236.dp).testTag("combo_card_${hamper.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(color = SignatureGoldContainer, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = hamper.badge,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = ForestGreenDark,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.5.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "+${hamper.bonusCoins} Coins",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = ForestGreen,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = hamper.displayTitle(appLanguage),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ForestGreenDark,
                                            fontSize = 13.sp
                                        ),
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = hamper.subtitle,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF5A6D63),
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = appLanguage.tr(
                                            english = "+ Add Bundle to Cart",
                                            punjabi = "+ ਕਾਰਟ ਵਿੱਚ ਜੋੜੋ (1-Tap)",
                                            hindi = "+ कार्ट में जोड़ें (1-Tap)"
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ForestGreen,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Category Filter Chips Row
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryFilterRow(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    isPunjabi = isPunjabi,
                    appLanguage = appLanguage,
                    onCategorySelected = onCategorySelected
                )
            }

            // 4. Brand Chips + Sort Menu
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        brands.forEach { brand ->
                            val isSelected = brand.equals(selectedBrand, ignoreCase = true)
                            AssistChip(
                                onClick = { onBrandSelected(brand) },
                                label = {
                                    Text(
                                        text = if (brand == "All") appLanguage.tr("All Brands", "ਸਾਰੇ ਬ੍ਰਾਂਡ", "सभी ब्रांड") else brand,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) SignatureGold else Color.White,
                                    labelColor = if (isSelected) ForestGreenDark else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Surface(
                            onClick = { showSortMenu = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarmCreamBorder),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = appLanguage.tr("Sort", "ਕ੍ਰਮ", "क्रमबद्ध करें"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(appLanguage.tr("Default (Featured SKUs)", "ਡਿਫਾਲਟ (ਸਟੋਰ ਕ੍ਰਮ)", "डिफ़ॉल्ट (स्टोर क्रम)")) },
                                onClick = { onSortSelected("Default"); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(appLanguage.tr("Name: A to Z", "ਨਾਮ ਕ੍ਰਮ (A to Z)", "नाम: A से Z")) },
                                onClick = { onSortSelected("Name: A to Z"); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(appLanguage.tr("Brand: A to Z", "ਬ੍ਰਾਂਡ ਨਾਮ (A to Z)", "ब्रांड: A से Z")) },
                                onClick = { onSortSelected("Brand: A to Z"); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(appLanguage.tr("Price: Low to High", "ਕੀਮਤ: ਘੱਟ ਤੋਂ ਵੱਧ", "कीमत: कम से ज़्यादा")) },
                                onClick = { onSortSelected("Price: Low to High"); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(appLanguage.tr("Price: High to Low", "ਕੀਮਤ: ਵੱਧ ਤੋਂ ਘੱਟ", "कीमत: ज़्यादा से कम")) },
                                onClick = { onSortSelected("Price: High to Low"); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(appLanguage.tr("Biggest Discount", "ਵੱਧ ਤੋਂ ਵੱਧ ਬੱਚਤ", "सबसे बड़ी बचत")) },
                                onClick = { onSortSelected("Biggest Discount"); showSortMenu = false }
                            )
                        }
                    }
                }
            }

            // 4b. Noon-Style Price Range Filter Chips
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PriceFilterRange.entries.forEach { range ->
                        val active = selectedPriceRange == range
                        AssistChip(
                            onClick = { onPriceRangeSelected(range) },
                            label = {
                                Text(
                                    text = range.labelFor(appLanguage),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (active) ForestGreen else Color.White,
                                labelColor = if (active) SignatureGold else ForestGreenDark
                            ),
                            modifier = Modifier.testTag("price_range_chip_${range.name.lowercase()}")
                        )
                    }
                }
            }

            // 5. Results Count & Zero-Produce Assurance Strip
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appLanguage.tr(
                            english = "Showing ${products.size} Verified Products",
                            punjabi = "${products.size} ਸੀਲਬੰਦ ਉਤਪਾਦ",
                            hindi = "${products.size} प्रमाणित उत्पाद"
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = appLanguage.tr(
                            english = "100% Sealed & Frozen • Zero Produce",
                            punjabi = "100% ਸੀਲਬੰਦ ਤੇ ਫ੍ਰੋਜ਼ਨ • ਕੋਈ ਖੁੱਲ੍ਹਾ ਸਾਮਾਨ ਨਹੀਂ",
                            hindi = "100% सीलबंद व फ्रोज़न • कोई खुला सामान नहीं"
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // 6. Product Grid
            if (products.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = appLanguage.tr("No Products Found", "ਕੋਈ ਉਤਪਾਦ ਨਹੀਂ ਮਿਲਿਆ", "कोई उत्पाद नहीं मिला"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = appLanguage.tr(
                                    english = "Try clearing search or switching category/brand",
                                    punjabi = "ਕਿਰਪਾ ਕਰਕੇ ਹੋਰ ਸ਼ਬਦ ਨਾਲ ਖੋਜ ਕਰੋ",
                                    hindi = "कृपया दूसरा शब्द खोजें या श्रेणी बदलें"
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = products,
                    key = { it.id },
                    contentType = { "product_card" }
                ) { product ->
                    val inCart = cartItems.filter { it.product.id == product.id }.sumOf { it.quantity }
                    Box(modifier = Modifier.padding(6.dp)) {
                        androidx.compose.runtime.key(product.id, product.fullImageUrl) {
                            ProductCard(
                                product = product,
                                cartQuantity = inCart,
                                isPunjabi = isPunjabi,
                                appLanguage = appLanguage,
                                offersConfig = offersConfig,
                                categoryDiscountPercent = categoryDiscounts[product.category],
                                categoryReturnPolicy = categoryReturnPolicies[product.category].orEmpty(),
                                isWishlisted = product.id in wishlistIds,
                                onToggleWishlist = onToggleWishlist,
                                onProductClick = onProductClick,
                                onAddToCart = onAddToCart,
                                onUpdateQuantity = onUpdateQuantity
                            )
                        }
                    }
                }
            }

            // 7. Structured Expandable Mobile Footer at bottom of Catalog Scroll
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(16.dp))
                MobileFooterSection(
                    isPunjabi = isPunjabi,
                    appLanguage = appLanguage,
                    offersConfig = offersConfig,
                    onCategorySelected = onCategorySelected,
                    onBrandSelected = onBrandSelected,
                    onOpenDeals = onOpenDealsClick,
                    onEmailSupport = onEmailSupport,
                    onWhatsAppSupport = onWhatsAppSupport,
                    onOpenUrl = onOpenUrl
                )
            }
        }

        // Floating Spin & Win Reward Badge (Top-Bottom Corner Above Cart Pill)
        Surface(
            onClick = onOpenDealsClick,
            shape = RoundedCornerShape(24.dp),
            color = SignatureGold,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (cartTotalCount > 0) 82.dp else 16.dp)
                .testTag("floating_spin_reward_badge")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = "Spin and Win Rewards",
                    tint = ForestGreenDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = appLanguage.tr("Spin & Quests", "ਸਪਿਨ & ਜਿੱਤੋ", "स्पिन और जीतें"),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ForestGreenDark,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }

        if (hasColdChainInCart && cartTotalCount > 0) {
            ColdChainCountdownBadge(
                isPunjabi = isPunjabi,
                appLanguage = appLanguage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 78.dp)
            )
        }

        // Floating Cart Summary Pill
        AnimatedVisibility(
            visible = cartTotalCount > 0,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Surface(
                onClick = onViewCartClick,
                shape = RoundedCornerShape(16.dp),
                color = ForestGreen,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("floating_cart_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = appLanguage.tr(
                                    english = "$cartTotalCount items • ₹$cartTotalSalePrice",
                                    punjabi = "$cartTotalCount ਚੀਜ਼ਾਂ • ₹$cartTotalSalePrice",
                                    hindi = "$cartTotalCount आइटम • ₹$cartTotalSalePrice"
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = appLanguage.tr(
                                    english = "Flat ${offersConfig.discountLabelPercent}% Wholesale Savings",
                                    punjabi = "${offersConfig.discountLabelPercent}% ਥੋਕ ਛੋਟ ਲਾਗੂ",
                                    hindi = "फ्लैट ${offersConfig.discountLabelPercent}% थोक बचत लागू"
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SignatureGoldContainer,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = appLanguage.tr("View Cart", "ਕਾਰਟ ਦੇਖੋ", "कार्ट देखें"),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = SignatureGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
