package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Storefront
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.Product
import com.example.data.model.StoreOffersConfig
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OnForestGreenContainer
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.WholesaleRed

@Composable
fun ProductCard(
    product: Product,
    cartQuantity: Int,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    categoryDiscountPercent: Double? = null,
    categoryReturnPolicy: String = "",
    isWishlisted: Boolean = false,
    onToggleWishlist: (String) -> Unit = {},
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product, Boolean) -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var usePetiMode by remember(product.id) { mutableStateOf(false) }
    val effDiscount = product.effectiveDiscountPercent(categoryDiscountPercent, offersConfig)
    val unitSalePrice = product.sellingPrice(effDiscount)
    val petiTotalPrice = product.petiTotalPriceFor(categoryDiscountPercent, offersConfig)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .testTag("product_card_${product.id}")
            .clickable { onProductClick(product) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Top Image Container with badges + Wishlist heart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.08f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFEEEAE0), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.runtime.key(product.id, product.fullImageUrl) {
                    val context = LocalContext.current
                    val cacheKey = "${product.id}_${product.fullImageUrl}"
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(com.example.data.util.ProductImageAutoFetcher.resolveDisplayImageModel(context, product))
                            .memoryCacheKey(cacheKey)
                            .diskCacheKey(cacheKey)
                            .crossfade(false)
                            .placeholder(R.drawable.ic_nav_brand_logo)
                            .error(R.drawable.ic_nav_brand_logo)
                            .build(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                }

                // Top Left: Dynamic Wholesale Discount Badge
                if (effDiscount > 0.0) {
                    Surface(
                        color = WholesaleRed,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "${StoreOffersConfig.formatPercent(effDiscount)}% OFF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Top Right: Cold Chain icon + Wishlist Heart button
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (product.isColdChain) {
                        Surface(
                            color = Color(0xFF0288D1),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = "Cold Chain Frozen",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(3.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = { onToggleWishlist(product.id) },
                        color = Color.White.copy(alpha = 0.92f),
                        shape = CircleShape,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("wishlist_btn_${product.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isWishlisted) "Remove from wishlist" else "Save to wishlist",
                                tint = if (isWishlisted) WholesaleRed else Color(0xFF4B5E54),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.brand.ifBlank { "NAV QUALITY" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GoldSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = if (product.isInStock) ForestGreenContainer else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (product.isInStock) {
                            appLanguage.tr(
                                english = "Stock: ${product.stock}",
                                punjabi = "ਸਟਾਕ: ${product.stock}",
                                hindi = "स्टॉक: ${product.stock}"
                            )
                        } else {
                            appLanguage.tr(
                                english = "OUT OF STOCK",
                                punjabi = "ਸਟਾਕ ਖਤਮ",
                                hindi = "स्टॉक खत्म"
                            )
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (product.isInStock) OnForestGreenContainer else WholesaleRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "GST ${product.gst}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 38.dp)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = product.cardCategorySubtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = EmeraldContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = product.effectiveReturnWindowBadge(appLanguage, categoryReturnPolicy),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = OnEmeraldContainer,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }

            if (product.sellerName.isNotBlank() && !product.sellerName.equals("Nav Grocery Store", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    color = GoldContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Seller Shop",
                            tint = OnGoldContainer,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = appLanguage.tr(
                                english = "Sold by ${product.sellerName}",
                                punjabi = "ਵਿਕਰੇਤਾ: ${product.sellerName}",
                                hindi = "विक्रेता: ${product.sellerName}"
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = OnGoldContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val activePrice = if (usePetiMode && product.isBulkEligible) petiTotalPrice else unitSalePrice
                    val activeMrp = if (usePetiMode && product.isBulkEligible) product.mrp * product.petiUnits else product.mrp
                    val activeSave = (activeMrp - activePrice).coerceAtLeast(0)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹$activePrice",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "₹$activeMrp",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Text(
                        text = if (usePetiMode && product.isBulkEligible)
                            "Peti (${product.petiUnits}x) • ${appLanguage.tr("Save", "ਬੱਚਤ", "बचत")} ₹$activeSave"
                        else appLanguage.tr("Save ₹$activeSave", "ਬੱਚਤ ₹$activeSave", "बचत ₹$activeSave"),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WholesaleRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            if (product.isBulkEligible) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { usePetiMode = false },
                        shape = RoundedCornerShape(6.dp),
                        color = if (!usePetiMode) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Single",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (!usePetiMode) Color.White else OnEmeraldContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            ),
                            modifier = Modifier.padding(vertical = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Surface(
                        onClick = { usePetiMode = true },
                        shape = RoundedCornerShape(6.dp),
                        color = if (usePetiMode) com.example.ui.theme.SignatureGold else EmeraldContainer.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(
                            text = "Peti (${product.petiUnits}x) -${offersConfig.petiLabelPercent}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (usePetiMode) Color(0xFF2B1C00) else OnEmeraldContainer,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.5.sp
                            ),
                            modifier = Modifier.padding(vertical = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!product.isInStock) {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color(0xFFE7E8E3),
                        disabledContentColor = Color(0xFF7B8C83)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("out_of_stock_btn_${product.id}")
                ) {
                    Text(
                        text = appLanguage.tr("Out of Stock", "ਸਟਾਕ ਖਤਮ", "स्टॉक खत्म"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else if (cartQuantity == 0 || (usePetiMode && product.isBulkEligible)) {
                Button(
                    onClick = { onAddToCart(product, usePetiMode && product.isBulkEligible) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("add_to_cart_btn_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (usePetiMode && product.isBulkEligible) "Add Peti (${product.petiUnits}x)"
                        else appLanguage.tr("Add", "ਜੋੜੋ (Add)", "जोड़ें (Add)"),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(EmeraldContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { onUpdateQuantity(product.id, cartQuantity - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = OnEmeraldContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "$cartQuantity",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnEmeraldContainer
                        )
                    )

                    IconButton(
                        onClick = { onUpdateQuantity(product.id, cartQuantity + 1) },
                        enabled = cartQuantity < product.stock,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = if (cartQuantity < product.stock) OnEmeraldContainer else Color(0xFF9AA89F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
