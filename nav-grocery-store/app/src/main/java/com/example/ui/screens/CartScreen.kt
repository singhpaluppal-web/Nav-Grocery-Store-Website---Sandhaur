package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.StoreProfile
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.WholesaleRed

@Composable
fun CartScreen(
    cart: List<CartItem>,
    totalMrp: Int,
    totalSalePrice: Int,
    totalSavings: Int,
    isPunjabi: Boolean,
    onUpdateQuantity: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onCheckoutClick: () -> Unit,
    onBrowseCatalogClick: () -> Unit
) {
    if (cart.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveShoppingCart,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isPunjabi) "ਤੁਹਾਡਾ ਕਾਰਟ ਖਾਲੀ ਹੈ" else "Your Cart is Empty",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isPunjabi) "ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ ਤੋਂ ੧੦੦% ਸੀਲਬੰਦ ਸਾਮਾਨ ੧੨% ਥੋਕ ਛੋਟ 'ਤੇ ਖਰੀਦੋ"
                           else "Explore 308 sealed packaged groceries & cold-chain items at flat 12% wholesale discount",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onBrowseCatalogClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("cart_browse_catalog_btn")
                ) {
                    Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPunjabi) "ਸਾਮਾਨ ਦੇਖੋ (Browse Catalog)" else "Browse Catalog",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Cart Header & Clear Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isPunjabi) "ਤੁਹਾਡਾ ਕਾਰਟ (${cart.size} ਚੀਜ਼ਾਂ)" else "My Cart (${cart.size} items)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isPunjabi) "ਫਲੈਟ ੧੨% ਥੋਕ ਬੱਚਤ ਲਾਗੂ ਹੈ" else "Flat 12% Wholesale discount applied",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            OutlinedButton(
                onClick = onClearCart,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear",
                    modifier = Modifier.size(14.dp),
                    tint = WholesaleRed
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPunjabi) "ਖਾਲੀ ਕਰੋ" else "Clear",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = WholesaleRed,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // List of Cart Items
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cart, key = { it.product.id }) { item ->
                CartItemRow(
                    item = item,
                    isPunjabi = isPunjabi,
                    onUpdateQuantity = onUpdateQuantity
                )
            }

            // Delivery Assurance Box
            item {
                Surface(
                    color = EmeraldContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isPunjabi) "ਸੰਧੌੜ ਅਤੇ ਆਸ-ਪਾਸ ਪਿੰਡਾਂ ਵਿੱਚ ~੨ ਘੰਟੇ ਡਿਲਿਵਰੀ ਜਾਂ ਮੇਨ ਬਾਜ਼ਾਰ ਤੋਂ ੩੦ ਮਿੰਟ ਪਿਕਅੱਪ"
                                   else "~2 Hr Home Delivery across Sandhaur & nearby villages or 30-min Store Pickup at Main Bazaar",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = OnEmeraldContainer,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Summary Breakdown Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isPunjabi) "ਬਿੱਲ ਦਾ ਵੇਰਵਾ (Bill Summary)" else "Bill Summary",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        BillLineRow(
                            label = if (isPunjabi) "ਕੁੱਲ ਐਮ.ਆਰ.ਪੀ (Total MRP)" else "Total MRP",
                            value = "₹$totalMrp"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        BillLineRow(
                            label = if (isPunjabi) "ਥੋਕ ਛੋਟ (Flat 12% OFF)" else "Wholesale Discount (Flat 12% OFF)",
                            value = "-₹$totalSavings",
                            valueColor = WholesaleRed
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        BillLineRow(
                            label = if (isPunjabi) "ਡਿਲਿਵਰੀ ਚਾਰਜ (ਸੰਧੌੜ ਖੇਤਰ)" else "Delivery Fee (Sandhaur area)",
                            value = if (isPunjabi) "ਮੁਫ਼ਤ (FREE)" else "FREE",
                            valueColor = EmeraldPrimary
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outline
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPunjabi) "ਕੁੱਲ ਭੁਗਤਾਨ (To Pay)" else "Grand Total",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = "₹$totalSalePrice",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GoldContainer, RoundedCornerShape(6.dp))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPunjabi) "ਤੁਸੀਂ ਇਸ ਆਰਡਰ 'ਤੇ ਕੁੱਲ ₹$totalSavings ਬਚਾ ਰਹੇ ਹੋ!"
                                       else "🎉 You are saving ₹$totalSavings at Nav Grocery Store!",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5D4037)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Bottom CTA Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isPunjabi) "ਦੇਣਯੋਗ ਰਕਮ" else "Payable Amount",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "₹$totalSalePrice",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                    )
                }

                Button(
                    onClick = onCheckoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("proceed_to_checkout_btn")
                ) {
                    Text(
                        text = if (isPunjabi) "ਆਰਡਰ ਕਰੋ (Checkout)" else "Proceed to Order",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    isPunjabi: Boolean,
    onUpdateQuantity: (String, Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().testTag("cart_item_${item.product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.product.fullImageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.nav_store_logo)
                    .error(R.drawable.nav_store_logo)
                    .build(),
                contentDescription = item.product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.brand.ifBlank { "NAV" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GoldSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )

                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${item.product.wholesalePrice}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "₹${item.product.mrp}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Stepper
            Row(
                modifier = Modifier
                    .background(EmeraldContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onUpdateQuantity(item.product.id, item.quantity - 1) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = OnEmeraldContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnEmeraldContainer
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = { onUpdateQuantity(item.product.id, item.quantity + 1) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = OnEmeraldContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BillLineRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
