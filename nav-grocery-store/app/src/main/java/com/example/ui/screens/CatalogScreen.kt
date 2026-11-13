package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.ProductCard
import com.example.ui.components.WholesaleSavingsBanner
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.WholesaleRed

@Composable
fun CatalogScreen(
    products: List<Product>,
    categories: List<String>,
    selectedCategory: String,
    brands: List<String>,
    selectedBrand: String,
    selectedSort: String,
    searchQuery: String,
    cartItems: List<CartItem>,
    cartTotalCount: Int,
    cartTotalSalePrice: Int,
    isPunjabi: Boolean,
    onCategorySelected: (String) -> Unit,
    onBrandSelected: (String) -> Unit,
    onSortSelected: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    onViewCartClick: () -> Unit,
    onStoreInfoClick: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(bottom = if (cartTotalCount > 0) 90.dp else 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Hero Promotional Wholesale Banner
            item(span = { GridItemSpan(maxLineSpan) }) {
                WholesaleSavingsBanner(
                    isPunjabi = isPunjabi,
                    onStoreInfoClick = onStoreInfoClick
                )
            }

            // 2. Search Bar
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = if (isPunjabi) "ਆਟਾ, ਦਾਲਾਂ, ਦੇਸੀ ਘਿਓ, ਫ੍ਰੋਜ਼ਨ ਮਟਰ ਖੋਜੋ..."
                                       else "Search 308 items, brands, barcode...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = EmeraldPrimary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("catalog_search_input")
                    )
                }
            }

            // 3. Category Filter Chips Row
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryFilterRow(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    isPunjabi = isPunjabi,
                    onCategorySelected = onCategorySelected
                )
            }

            // 4. Sub-filters: Brand chips & Sort button
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brand Filter Horizontal Scroll
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        brands.take(10).forEach { brand ->
                            val isSelected = brand == selectedBrand
                            AssistChip(
                                onClick = { onBrandSelected(brand) },
                                label = {
                                    Text(
                                        text = if (brand == "All") (if (isPunjabi) "ਸਾਰੇ ਬ੍ਰਾਂਡ" else "All Brands") else brand,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) GoldSecondary else MaterialTheme.colorScheme.surface,
                                    labelColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Sort Button & Dropdown
                    Box {
                        Surface(
                            onClick = { showSortMenu = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPunjabi) "ਕ੍ਰਮ" else "Sort",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isPunjabi) "ਡਿਫਾਲਟ (Default)" else "Default") },
                                onClick = { onSortSelected("Default"); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isPunjabi) "ਕੀਮਤ: ਘੱਟ ਤੋਂ ਵੱਧ" else "Price: Low to High") },
                                onClick = { onSortSelected("PriceLowHigh"); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isPunjabi) "ਕੀਮਤ: ਵੱਧ ਤੋਂ ਘੱਟ" else "Price: High to Low") },
                                onClick = { onSortSelected("PriceHighLow"); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isPunjabi) "ਵੱਧ ਤੋਂ ਵੱਧ ਬੱਚਤ (Highest Discount)" else "Highest Savings") },
                                onClick = { onSortSelected("DiscountHigh"); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isPunjabi) "ਬ੍ਰਾਂਡ ਨਾਮ (A-Z)" else "Brand Name (A-Z)") },
                                onClick = { onSortSelected("Brand"); showSortMenu = false }
                            )
                        }
                    }
                }
            }

            // Results count banner
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPunjabi) "${products.size} ਸੀਲਬੰਦ ਉਤਪਾਦ ਉਪਲਬਧ ਹਨ" else "Showing ${products.size} Products",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Text(
                        text = "100% Sealed & Cold-Chain",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // 5. Product Grid Items
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
                                text = if (isPunjabi) "ਕੋਈ ਉਤਪਾਦ ਨਹੀਂ ਮਿਲਿਆ" else "No Products Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isPunjabi) "ਕਿਰਪਾ ਕਰਕੇ ਹੋਰ ਸ਼ਬਦ ਨਾਲ ਖੋਜ ਕਰੋ" else "Try searching with a different brand or item name",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            } else {
                items(products, key = { it.id }) { product ->
                    val inCart = cartItems.firstOrNull { it.product.id == product.id }?.quantity ?: 0
                    Box(modifier = Modifier.padding(6.dp)) {
                        ProductCard(
                            product = product,
                            cartQuantity = inCart,
                            isPunjabi = isPunjabi,
                            onProductClick = onProductClick,
                            onAddToCart = onAddToCart,
                            onUpdateQuantity = onUpdateQuantity
                        )
                    }
                }
            }
        }

        // Floating Cart Summary Pill (when cart has items)
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
                color = EmeraldPrimary,
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
                                text = if (isPunjabi) "$cartTotalCount ਚੀਜ਼ਾਂ • ₹$cartTotalSalePrice" else "$cartTotalCount items • ₹$cartTotalSalePrice",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isPunjabi) "੧੨% ਥੋਕ ਛੋਟ ਲਾਗੂ" else "Flat 12% Wholesale Savings",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GoldContainer,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPunjabi) "ਕਾਰਟ ਦੇਖੋ" else "View Cart",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
