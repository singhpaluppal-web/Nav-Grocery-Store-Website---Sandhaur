package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.DeliveryOption
import com.example.data.model.Product
import com.example.data.model.StoreDeliveryConfig
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StorePaymentsConfig
import com.example.data.model.StoreProfile
import com.example.data.model.UserAccount
import com.example.data.security.CheckoutPricingEngine
import com.example.ui.components.ColdChainCountdownBadge
import com.example.ui.components.EmiEligibilityBadge
import com.example.ui.components.EmiPlansBottomSheet
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.OnForestGreenContainer
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmCreamBorder
import com.example.ui.theme.WholesaleRed
import kotlin.math.roundToInt

@Composable
fun CartScreen(
    cart: List<CartItem>,
    masterCatalog: List<Product> = emptyList(),
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    paymentsConfig: StorePaymentsConfig = StorePaymentsConfig(),
    deliveryConfig: StoreDeliveryConfig = StoreDeliveryConfig(),
    categoryDiscounts: Map<String, Double> = emptyMap(),
    userAccount: UserAccount? = null,
    applySpinCoupon: Boolean = true,
    coinsToRedeem: Int = 0,
    applyWalletBalance: Boolean = false,
    hasColdChainInCart: Boolean = false,
    totalMrp: Int,
    totalSalePrice: Int,
    totalSavings: Int,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    onToggleSpinCoupon: (Boolean) -> Unit = {},
    onCoinsChange: (Int) -> Unit = {},
    onToggleWalletBalance: (Boolean) -> Unit = {},
    onSaveRashanBox: (Int) -> Unit = {},
    onUpdateQuantity: (String, Int, String, String, Boolean) -> Unit,
    onClearCart: () -> Unit,
    onCheckoutClick: () -> Unit,
    onBrowseCatalogClick: () -> Unit
) {
    if (cart.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmCream)
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
                        .background(ForestGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveShoppingCart,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = appLanguage.tr("Your Cart is Empty", "ਤੁਹਾਡਾ ਕਾਰਟ ਖਾਲੀ ਹੈ", "आपका कार्ट खाली है"),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = appLanguage.tr(
                        english = "Explore sealed packaged groceries, cold-chain & A-to-Z marketplace items at flat ${offersConfig.discountLabelPercent}% off",
                        punjabi = "ਨਵ ਗ੍ਰੋਸਰੀ & ਨਵਕਾਰਟ ਤੋਂ ਸੀਲਬੰਦ ਸਾਮਾਨ ${offersConfig.discountLabelPercent}% ਥੋਕ ਛੋਟ 'ਤੇ ਖਰੀਦੋ",
                        hindi = "नव ग्रोसरी और नवकार्ट से सीलबंद उत्पाद फ्लैट ${offersConfig.discountLabelPercent}% थोक छूट पर खरीदें"
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onBrowseCatalogClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("cart_browse_catalog_btn")
                ) {
                    Text(
                        text = appLanguage.tr("Browse Marketplace", "ਸਾਮਾਨ ਦੇਖੋ (Browse Catalog)", "सामान देखें (Browse)"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SignatureGold)
                }
            }
        }
        return
    }

    val verifiedSummary = remember(
        cart,
        masterCatalog,
        userAccount,
        applySpinCoupon,
        coinsToRedeem,
        applyWalletBalance,
        offersConfig,
        categoryDiscounts,
        deliveryConfig
    ) {
        CheckoutPricingEngine.verifyAndCompute(
            cartItems = cart,
            masterCatalog = masterCatalog.ifEmpty { cart.map { it.product } },
            deliveryOption = DeliveryOption.LOCAL_2_HR,
            userAccount = userAccount,
            applySpinCoupon = applySpinCoupon,
            requestedCoinsToRedeem = coinsToRedeem,
            applyWalletBalance = applyWalletBalance,
            offersConfig = offersConfig,
            categoryDiscounts = categoryDiscounts,
            deliveryConfig = deliveryConfig,
            pinCode = StoreProfile.DEFAULT_PIN_CODE
        ).getOrNull()
    }

    val displayMrp = verifiedSummary?.totalMrp ?: totalMrp
    val displayWholesaleDiscount = verifiedSummary?.wholesaleDiscount ?: totalSavings
    val displayPetiDiscount = verifiedSummary?.petiDiscountRupees ?: 0
    val displaySpinDiscount = verifiedSummary?.spinCouponDiscountRupees ?: 0
    val displayCoinsRedeemed = verifiedSummary?.coinsRedeemed ?: 0
    val displayCoinDiscountRupees = verifiedSummary?.coinDiscountRupees ?: 0
    val displayWalletDeducted = verifiedSummary?.walletDeductedRupees ?: 0
    val displayDeliveryFee = verifiedSummary?.deliveryFee ?: 0
    val displayPayable = verifiedSummary?.netGatewayPayable ?: totalSalePrice
    val maxRedeemableCoins = remember(userAccount, displayMrp, displayWholesaleDiscount, displayPetiDiscount, displaySpinDiscount, offersConfig) {
        val avail = (userAccount?.navCoins ?: 0L).toInt().coerceAtLeast(0)
        val afterCoupon = (displayMrp - displayWholesaleDiscount - displayPetiDiscount - displaySpinDiscount).coerceAtLeast(0)
        offersConfig.maxRedeemableCoins(afterCoupon, avail)
    }
    var selectedRashanDay by remember { mutableIntStateOf(5) }
    var showEmiPlansSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = appLanguage.tr(
                        english = "My Cart (${cart.size} items)",
                        punjabi = "ਤੁਹਾਡਾ ਕਾਰਟ (${cart.size} ਚੀਜ਼ਾਂ)",
                        hindi = "मेरा कार्ट (${cart.size} आइटम)"
                    ),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = ForestGreen)
                )
                Text(
                    text = appLanguage.tr(
                        english = "Locked Flat ${offersConfig.discountLabelPercent}% Wholesale Savings Verified",
                        punjabi = "ਫਲੈਟ ${offersConfig.discountLabelPercent}% ਥੋਕ ਬੱਚਤ ਲਾਗੂ ਹੈ",
                        hindi = "फ्लैट ${offersConfig.discountLabelPercent}% थोक बचत लागू है"
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ForestGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            OutlinedButton(
                onClick = onClearCart,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear",
                    modifier = Modifier.size(14.dp),
                    tint = WholesaleRed
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = appLanguage.tr("Clear", "ਖਾਲੀ ਕਰੋ", "खाली करें"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = WholesaleRed,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasColdChainInCart) {
                item {
                    ColdChainCountdownBadge(isPunjabi = isPunjabi, appLanguage = appLanguage)
                }
            }

            items(cart, key = { "${it.product.id}_${it.selectedSize}_${it.selectedColor}_${it.isPetiCarton}" }) { item ->
                CartItemRow(
                    item = item,
                    offersConfig = offersConfig,
                    categoryDiscountPercent = categoryDiscounts[item.product.category],
                    onUpdateQuantity = onUpdateQuantity
                )
            }

            // Mahina Rashan Box Monthly Auto-Reorder Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = appLanguage.tr(
                                english = "Save as Mahina Rashan Box (+30 Bonus Coins)",
                                punjabi = "ਮਹੀਨਾ ਰਾਸ਼ਨ ਬਾਕਸ ਸੇਵ ਕਰੋ (+30 ਕੌਇਨਜ਼)",
                                hindi = "महीना राशन बॉक्स सेव करें (+30 कॉइन्स)"
                            ),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = ForestGreenDark)
                        )
                        Text(
                            text = "Lock monthly auto-delivery from Main Bazaar Sandhaur on your preferred date:",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4B5E54))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf(1, 5, 10, 15).forEach { day ->
                                FilterChip(
                                    selected = selectedRashanDay == day,
                                    onClick = { selectedRashanDay = day },
                                    label = { Text("Day $day", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { onSaveRashanBox(selectedRashanDay) },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("save_rashan_box_btn")
                            ) {
                                Text("Save Box", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Spin Reward Coupon, Family Wallet & Nav Coins Redemption Card (for Signed-in Gmail users)
            val coinsActiveInCart = offersConfig.coinConfig.isCoinsEnabled && maxRedeemableCoins > 0
            if (userAccount != null && (userAccount.hasValidSpinCoupon() || coinsActiveInCart || userAccount.walletBalanceRupees > 0L)) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SignatureGoldContainer),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SignatureGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = appLanguage.tr(
                                    english = "Loyalty Rewards & Coins Redemption",
                                    punjabi = "ਸਪਿਨ ਇਨਾਮ ਅਤੇ ਨਵ ਕੌਇਨਜ਼ ਵਰਤੋ",
                                    hindi = "स्पिन इनाम और नव कॉइन्स उपयोग करें"
                                ),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ForestGreenDark
                                )
                            )

                            if (userAccount.hasValidSpinCoupon()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Casino, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Apply Spin Reward (${userAccount.activeSpinCouponCode})",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ForestGreenDark)
                                            )
                                            Text(
                                                text = userAccount.activeSpinCouponLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF3A2700))
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = applySpinCoupon,
                                        onCheckedChange = onToggleSpinCoupon,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SignatureGold,
                                            checkedTrackColor = ForestGreen
                                        )
                                    )
                                }
                            }

                            if (coinsActiveInCart) {
                                val effectiveCoins = coinsToRedeem.coerceIn(0, maxRedeemableCoins)
                                val coinRupees = offersConfig.coinDiscountRupees(effectiveCoins)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Redeem Nav Coins: $effectiveCoins / $maxRedeemableCoins max",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ForestGreenDark)
                                        )
                                    }
                                    Text(
                                        text = "-₹$coinRupees",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = ForestGreen)
                                    )
                                }
                                Slider(
                                    value = coinsToRedeem.coerceIn(0, maxRedeemableCoins).toFloat(),
                                    onValueChange = { onCoinsChange(it.roundToInt()) },
                                    valueRange = 0f..maxRedeemableCoins.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = ForestGreen,
                                        activeTrackColor = ForestGreen
                                    )
                                )
                            }

                            if ((userAccount.walletBalanceRupees) > 0L) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Use Prepaid Family Wallet (₹${userAccount.walletBalanceRupees} avail)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ForestGreenDark)
                                        )
                                        Text(
                                            text = "Instant 1-tap deduction on checkout",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF3A2700))
                                        )
                                    }
                                    Switch(
                                        checked = applyWalletBalance,
                                        onCheckedChange = onToggleWalletBalance,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SignatureGold,
                                            checkedTrackColor = ForestGreen
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (paymentsConfig.isEligibleForEmi(displayPayable)) {
                item {
                    EmiEligibilityBadge(
                        amountInr = displayPayable,
                        paymentsConfig = paymentsConfig,
                        appLanguage = appLanguage,
                        onViewPlansClick = { showEmiPlansSheet = true }
                    )
                }
            }

            // Delivery Assurance Box
            item {
                Surface(
                    color = ForestGreenContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = appLanguage.tr(
                                english = "FREE local delivery on orders ₹${deliveryConfig.freeDeliveryMinOrder}+ (PIN 148020) or FREE 30-Min Main Bazaar Store Pickup",
                                punjabi = "₹${deliveryConfig.freeDeliveryMinOrder}+ 'ਤੇ ਮੁਫ਼ਤ ਲੋਕਲ ਡਿਲਿਵਰੀ (148020) ਜਾਂ 30-ਮਿੰਟ ਮੇਨ ਬਾਜ਼ਾਰ ਪਿਕਅੱਪ",
                                hindi = "₹${deliveryConfig.freeDeliveryMinOrder}+ ऑर्डर पर मुफ़्त लोकल डिलीवरी (148020) या 30-मिनट स्टोर पिकअप"
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = OnForestGreenContainer,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Bill Breakdown Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarmCreamBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = appLanguage.tr(
                                english = "Verified Bill Summary",
                                punjabi = "ਪ੍ਰਮਾਣਿਤ ਬਿੱਲ ਵੇਰਵਾ",
                                hindi = "सत्यापित बिल विवरण"
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        BillLineRow(
                            label = appLanguage.tr("Total MRP", "ਕੁੱਲ ਐਮ.ਆਰ.ਪੀ (Total MRP)", "कुल एमआरपी (Total MRP)"),
                            value = "₹$displayMrp"
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        BillLineRow(
                            label = appLanguage.tr(
                                english = "Store Wholesale Discount (${offersConfig.discountLabelPercent}% OFF)",
                                punjabi = "ਥੋਕ ਛੋਟ (${offersConfig.discountLabelPercent}% OFF)",
                                hindi = "थोक छूट (${offersConfig.discountLabelPercent}% OFF)"
                            ),
                            value = "-₹$displayWholesaleDiscount",
                            valueColor = WholesaleRed
                        )

                        if (displayPetiDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            BillLineRow(
                                label = "Vadda Dabba (Peti) Extra ${offersConfig.petiLabelPercent}% Rebate",
                                value = "-₹$displayPetiDiscount",
                                valueColor = ForestGreen
                            )
                        }

                        if (displaySpinDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            BillLineRow(
                                label = "Spin Reward (${verifiedSummary?.spinCouponCode ?: ""})",
                                value = "-₹$displaySpinDiscount",
                                valueColor = ForestGreen
                            )
                        }

                        if (displayCoinsRedeemed > 0 && displayCoinDiscountRupees > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            BillLineRow(
                                label = "Nav Coins Redeemed ($displayCoinsRedeemed coins)",
                                value = "-₹$displayCoinDiscountRupees",
                                valueColor = ForestGreen
                            )
                        }

                        if (displayWalletDeducted > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            BillLineRow(
                                label = "Prepaid Family Wallet Applied",
                                value = "-₹$displayWalletDeducted",
                                valueColor = ForestGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        BillLineRow(
                            label = appLanguage.tr(
                                english = "2-Hr Local / Pind Route (${StoreProfile.DEFAULT_PIN_CODE})",
                                punjabi = "ਲੋਕਲ 2-ਘੰਟੇ / ਪਿੰਡ ਰੂਟ (${StoreProfile.DEFAULT_PIN_CODE})",
                                hindi = "लोकल 2-घंटे / गाँव रूट (${StoreProfile.DEFAULT_PIN_CODE})"
                            ),
                            value = if (displayDeliveryFee == 0) "FREE" else "₹$displayDeliveryFee",
                            valueColor = if (displayDeliveryFee == 0) ForestGreen else ForestGreenDark
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = WarmCreamBorder
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = appLanguage.tr("Estimated Payable", "ਕੁੱਲ ਭੁਗਤਾਨ", "कुल देय राशि"),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = "₹$displayPayable",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ForestGreen
                                )
                            )
                        }
                    }
                }
            }
        }

        // Bottom CTA Bar
        Surface(
            color = Color.White,
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
                        text = appLanguage.tr("Verified Payable", "ਦੇਣਯੋਗ ਰਕਮ", "देय राशि"),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "₹$displayPayable",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestGreen
                        )
                    )
                }

                Button(
                    onClick = onCheckoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("proceed_to_checkout_btn")
                ) {
                    Text(
                        text = appLanguage.tr("Proceed to Checkout", "ਚੈੱਕਆਊਟ ਕਰੋ", "चेकआउट करें"),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SignatureGold)
                }
            }
        }
    }

    if (showEmiPlansSheet) {
        EmiPlansBottomSheet(
            amountInr = displayPayable,
            paymentsConfig = paymentsConfig,
            appLanguage = appLanguage,
            onDismiss = { showEmiPlansSheet = false }
        )
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    categoryDiscountPercent: Double? = null,
    onUpdateQuantity: (String, Int, String, String, Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmCreamBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_${item.product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.runtime.key(item.product.id, item.product.fullImageUrl) {
                val context = LocalContext.current
                val cacheKey = "${item.product.id}_${item.product.fullImageUrl}"
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(com.example.data.util.ProductImageAutoFetcher.resolveDisplayImageModel(context, item.product))
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .crossfade(false)
                        .placeholder(R.drawable.ic_nav_brand_logo)
                        .error(R.drawable.ic_nav_brand_logo)
                        .build(),
                    contentDescription = item.product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarmCream)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.brand.ifBlank { "NAV" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SignatureGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )

                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.variantSummary.isNotBlank()) {
                    Text(
                        text = item.variantSummary,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val unitLineSale = if (item.isPetiCarton && item.product.isBulkEligible) {
                        item.product.petiTotalPriceFor(categoryDiscountPercent, offersConfig)
                    } else {
                        item.product.sellingPrice(categoryDiscountPercent, offersConfig)
                    }
                    val unitLineMrp = if (item.isPetiCarton && item.product.isBulkEligible) item.product.mrp * item.petiUnits else item.product.mrp
                    Text(
                        text = "₹$unitLineSale",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestGreen
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "₹$unitLineMrp",
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .background(ForestGreenContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onUpdateQuantity(item.product.id, item.quantity - 1, item.selectedSize, item.selectedColor, item.isPetiCarton)
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = OnForestGreenContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnForestGreenContainer
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = {
                        onUpdateQuantity(item.product.id, item.quantity + 1, item.selectedSize, item.selectedColor, item.isPetiCarton)
                    },
                    enabled = item.quantity < item.product.stock,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = if (item.quantity < item.product.stock) OnForestGreenContainer else Color(0xFF9AA89F),
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
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
