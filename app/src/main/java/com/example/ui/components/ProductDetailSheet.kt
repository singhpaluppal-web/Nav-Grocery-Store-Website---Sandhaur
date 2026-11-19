package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StorePaymentsConfig
import com.example.data.model.StoreProfile
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.WholesaleRed
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailSheet(
    product: Product?,
    cartItems: List<CartItem>,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    paymentsConfig: StorePaymentsConfig = StorePaymentsConfig(),
    categoryDiscountPercent: Double? = null,
    categoryReturnPolicy: String = "",
    onDismiss: () -> Unit,
    onAddToCart: (Product, String, String, Boolean) -> Unit,
    onUpdateQuantity: (String, Int, String, String, Boolean) -> Unit
) {
    if (product == null) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val galleryUrls = remember(product.id, product.additionalImages, product.image) {
        product.galleryImageUrls.ifEmpty { listOf(product.fullImageUrl) }
    }
    val galleryPagerState = rememberPagerState(pageCount = { galleryUrls.size.coerceAtLeast(1) })
    val effDiscount = product.effectiveDiscountPercent(categoryDiscountPercent, offersConfig)
    val discountPctText = StoreOffersConfig.formatPercent(effDiscount)
    val unitSalePrice = product.sellingPrice(effDiscount)
    val petiTotalPrice = product.petiTotalPriceFor(categoryDiscountPercent, offersConfig)

    val sizes = remember(product.id) { product.sizeList }
    val colors = remember(product.id) { product.colorList }
    var selectedSize by remember(product.id) { mutableStateOf(sizes.firstOrNull().orEmpty()) }
    var selectedColor by remember(product.id) { mutableStateOf(colors.firstOrNull().orEmpty()) }
    var isPetiMode by remember(product.id) { mutableStateOf(false) }
    var showEmiPlansSheet by remember(product.id) { mutableStateOf(false) }
    var skuCopied by remember(product.id) { mutableStateOf(false) }
    var isHoverZoomed by remember(product.id) { mutableStateOf(false) }
    var isTapZoomed by remember(product.id) { mutableStateOf(false) }
    var zoomOrigin by remember(product.id) { mutableStateOf(TransformOrigin.Center) }
    var imageContainerSize by remember(product.id) { mutableStateOf(IntSize.Zero) }
    val isZoomed = isHoverZoomed || isTapZoomed
    val animatedScale by animateFloatAsState(
        targetValue = if (isZoomed) 2.2f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "detail_image_zoom"
    )

    LaunchedEffect(skuCopied) {
        if (skuCopied) {
            delay(2000L)
            skuCopied = false
        }
    }

    val matchingCartItem = cartItems.firstOrNull {
        it.product.id == product.id &&
            it.isPetiCarton == (isPetiMode && product.isBulkEligible) &&
            it.selectedSize.equals(selectedSize, ignoreCase = true) &&
            it.selectedColor.equals(selectedColor, ignoreCase = true)
    }
    val variantQty = matchingCartItem?.quantity ?: 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appLanguage.tr(
                        english = "Product Details & Variants",
                        punjabi = "ਉਤਪਾਦ ਵੇਰਵਾ (Product Details)",
                        hindi = "उत्पाद विवरण (Product Details)"
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Big Product Image on Clean Studio Background with Hover-to-Zoom & Tap-to-Zoom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color.White)
                    .clipToBounds()
                    .onSizeChanged { imageContainerSize = it }
                    .pointerInput(product.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val pos = event.changes.firstOrNull()?.position
                                val w = imageContainerSize.width.toFloat().coerceAtLeast(1f)
                                val h = imageContainerSize.height.toFloat().coerceAtLeast(1f)
                                when (event.type) {
                                    PointerEventType.Enter, PointerEventType.Move -> {
                                        if (pos != null) {
                                            zoomOrigin = TransformOrigin(
                                                pivotFractionX = (pos.x / w).coerceIn(0.05f, 0.95f),
                                                pivotFractionY = (pos.y / h).coerceIn(0.05f, 0.95f)
                                            )
                                        }
                                        if (event.type == PointerEventType.Enter || isHoverZoomed) {
                                            isHoverZoomed = true
                                        }
                                    }
                                    PointerEventType.Exit -> {
                                        isHoverZoomed = false
                                        if (!isTapZoomed) zoomOrigin = TransformOrigin.Center
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(product.id) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val w = imageContainerSize.width.toFloat().coerceAtLeast(1f)
                                val h = imageContainerSize.height.toFloat().coerceAtLeast(1f)
                                zoomOrigin = TransformOrigin(
                                    pivotFractionX = (tapOffset.x / w).coerceIn(0.05f, 0.95f),
                                    pivotFractionY = (tapOffset.y / h).coerceIn(0.05f, 0.95f)
                                )
                                isTapZoomed = !isTapZoomed
                                if (!isTapZoomed && !isHoverZoomed) {
                                    zoomOrigin = TransformOrigin.Center
                                }
                            }
                        )
                    }
                    .testTag("detail_image_zoom_container"),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = galleryPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val imageUrl = galleryUrls[pageIndex]
                    val imageModel: Any = if (pageIndex == 0) {
                        com.example.data.util.ProductImageAutoFetcher.resolveDisplayImageModel(context, product)
                    } else {
                        imageUrl
                    }
                    val cacheKey = "${product.id}_gallery_${pageIndex}_$imageUrl"
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageModel)
                            .memoryCacheKey(cacheKey)
                            .diskCacheKey(cacheKey)
                            .crossfade(false)
                            .placeholder(R.drawable.ic_nav_brand_logo)
                            .error(R.drawable.ic_nav_brand_logo)
                            .build(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(235.dp)
                            .padding(12.dp)
                            .graphicsLayer {
                                if (galleryPagerState.currentPage == pageIndex) {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    transformOrigin = zoomOrigin
                                }
                            }
                    )
                }

                if (effDiscount > 0.0) {
                    Surface(
                        color = WholesaleRed,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "FLAT $discountPctText% OFF ON MRP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (product.isColdChain) {
                    Surface(
                        color = Color(0xFF0288D1),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cold-Chain Frozen",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Zoom Indicator & Toggle Pill
                Surface(
                    onClick = {
                        isTapZoomed = !isTapZoomed
                        if (!isTapZoomed) {
                            isHoverZoomed = false
                            zoomOrigin = TransformOrigin.Center
                        }
                    },
                    color = if (isZoomed) EmeraldPrimary.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .testTag("detail_zoom_toggle")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = if (isZoomed) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                            contentDescription = if (isZoomed) "Reset image zoom" else "Zoom product image",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isZoomed) {
                                appLanguage.tr("2.2x Zoom • Reset", "2.2x ਜ਼ੂਮ • ਰੀਸੈਟ", "2.2x ज़ूम • रीसेट")
                            } else {
                                appLanguage.tr("Hover / Tap Zoom", "ਲੇਬਲ ਜ਼ੂਮ ਕਰੋ", "लेबल ज़ूम करें")
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            if (galleryUrls.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    galleryUrls.forEachIndexed { idx, thumbUrl ->
                        val selectedThumb = galleryPagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(
                                    width = if (selectedThumb) 2.dp else 1.dp,
                                    color = if (selectedThumb) EmeraldPrimary else Color(0xFFDDE3DF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    coroutineScope.launch { galleryPagerState.animateScrollToPage(idx) }
                                }
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(
                                        if (idx == 0) com.example.data.util.ProductImageAutoFetcher.resolveDisplayImageModel(context, product)
                                        else thumbUrl
                                    )
                                    .placeholder(R.drawable.ic_nav_brand_logo)
                                    .error(R.drawable.ic_nav_brand_logo)
                                    .build(),
                                contentDescription = "${product.name} photo ${idx + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Specs, Variants & Title
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.brand.ifBlank { "NAV SELECTION" }.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldSecondary,
                            letterSpacing = 1.sp
                        )
                    )

                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = OnEmeraldContainer,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Verified Seller / Partner Shop Badge
                Surface(
                    color = GoldContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GoldSecondary.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Verified Seller",
                            tint = OnGoldContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = appLanguage.tr(
                                english = "Sold by ${product.sellerName.ifBlank { "Nav Grocery Store" }}",
                                punjabi = "ਵਿਕਰੇਤਾ: ${product.sellerName.ifBlank { "Nav Grocery Store" }}",
                                hindi = "विक्रेता: ${product.sellerName.ifBlank { "Nav Grocery Store" }}"
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = OnGoldContainer
                            )
                        )
                        if (product.isVerifiedSeller) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Partner",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Authentic Metadata Row (SKU ID with Copy button, Category Spec / Net Wt, GST)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("NavKart SKU ID", product.id))
                            skuCopied = true
                            val toastMsg = appLanguage.tr(
                                english = "Copied SKU ${product.id} for WhatsApp",
                                punjabi = "SKU ${product.id} WhatsApp ਆਰਡਰ ਲਈ ਕਾਪੀ ਹੋ ਗਿਆ",
                                hindi = "SKU ${product.id} WhatsApp ऑर्डर के लिए कॉपी हो गया"
                            )
                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                        },
                        color = if (skuCopied) EmeraldPrimary else EmeraldContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (skuCopied) EmeraldPrimary else EmeraldPrimary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .minimumInteractiveComponentSize()
                            .testTag("copy_sku_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SKU: ${product.id}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (skuCopied) Color.White else OnEmeraldContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (skuCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = if (skuCopied) "SKU ID copied" else "Copy SKU ID",
                                tint = if (skuCopied) Color.White else EmeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (skuCopied) {
                                    appLanguage.tr("Copied!", "ਕਾਪੀ!", "कॉपी!")
                                } else {
                                    appLanguage.tr("Copy", "ਕਾਪੀ", "कॉपी")
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (skuCopied) Color.White else EmeraldPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }

                    if (product.netWeightOrPackLabel.isNotBlank()) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            text = "Pack: ${product.netWeightOrPackLabel}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Text(
                        text = "GST: ${product.gst}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                // Interactive Size Selector Chips
                if (sizes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = appLanguage.tr(
                            english = "Select Size / Variant:",
                            punjabi = "ਸਾਈਜ਼ / ਵੇਰੀਐਂਟ ਚੁਣੋ (Select Size):",
                            hindi = "साइज़ / वेरिएंट चुनें (Select Size):"
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sizes.forEach { size ->
                            val isSelected = size.equals(selectedSize, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSize = size },
                                label = {
                                    Text(
                                        text = size,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = EmeraldPrimary
                                ),
                                modifier = Modifier.testTag("variant_size_$size")
                            )
                        }
                    }
                }

                // Interactive Color Selector Chips
                if (colors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = appLanguage.tr(
                            english = "Select Color:",
                            punjabi = "ਰੰਗ ਚੁਣੋ (Select Color):",
                            hindi = "रंग चुनें (Select Color):"
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        colors.forEach { color ->
                            val isSelected = color.equals(selectedColor, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedColor = color },
                                label = {
                                    Text(
                                        text = color,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldContainer,
                                    selectedLabelColor = OnGoldContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = GoldSecondary
                                ),
                                modifier = Modifier.testTag("variant_color_$color")
                            )
                        }
                    }
                }

                // Category-Specific Return / Replacement Policy Guarantee Badge
                val returnPolicyLabel = product.effectiveReturnWindowBadge(appLanguage, categoryReturnPolicy)
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = GoldContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldSecondary.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("replacement_guarantee_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = returnPolicyLabel,
                            tint = OnGoldContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = returnPolicyLabel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OnGoldContainer
                                )
                            )
                            Text(
                                text = product.cardTrustBadgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = OnGoldContainer.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }

                // Product Description & Specifications Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = appLanguage.tr(
                                english = "Specifications & Quality Assurance",
                                punjabi = "ਵਿਸ਼ੇਸ਼ਤਾਵਾਂ ਅਤੇ ਪੈਕੇਜਿੰਗ (Specs & Packaging)",
                                hindi = "विवरण और गुणवत्ता गारंटी (Specs)"
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.displayDescription,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        )
                        if (product.warranty.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Warranty / Promise: ${product.warranty}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (product.isBulkEligible) {
                    Text(
                        text = appLanguage.tr(
                            english = "Purchase Mode (Single vs. Vadda Dabba Peti):",
                            punjabi = "ਪੈਕ ਕਿਸਮ (Single Unit vs Vadda Dabba Peti):",
                            hindi = "पैक चुनें (Single Unit vs बड़ा पेटी पैक):"
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isPetiMode,
                            onClick = { isPetiMode = false },
                            label = { Text("Single Unit • ₹$unitSalePrice", fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = isPetiMode,
                            onClick = { isPetiMode = true },
                            label = { Text("Full Peti (${product.petiUnits}x) • ₹$petiTotalPrice (-${offersConfig.petiLabelPercent}% Extra)", fontWeight = FontWeight.Bold) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Wholesale Pricing Card
                val effectivePrice = if (isPetiMode && product.isBulkEligible) petiTotalPrice else unitSalePrice
                val effectiveMrp = if (isPetiMode && product.isBulkEligible) product.mrp * product.petiUnits else product.mrp
                val effectiveSave = (effectiveMrp - effectivePrice).coerceAtLeast(0)
                Surface(
                    color = EmeraldDark,
                    shape = RoundedCornerShape(12.dp),
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
                                text = if (isPetiMode && product.isBulkEligible)
                                    "Vadda Dabba Peti (${product.petiUnits} Units)"
                                else appLanguage.tr(
                                    english = "Wholesale Store Price",
                                    punjabi = "ਥੋਕ ਵਿਕਰੀ ਮੁੱਲ (Wholesale Price)",
                                    hindi = "थोक बिक्री मूल्य (Wholesale Price)"
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹$effectivePrice",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "MRP ₹$effectiveMrp",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color.White.copy(alpha = 0.6f),
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                )
                            }
                        }

                        Surface(
                            color = WholesaleRed,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = appLanguage.tr(
                                    english = "Save ₹$effectiveSave\n(${if (isPetiMode && product.isBulkEligible) "$discountPctText%+${offersConfig.petiLabelPercent}%" else "$discountPctText% OFF"})",
                                    punjabi = "ਬੱਚਤ ₹$effectiveSave",
                                    hindi = "बचत ₹$effectiveSave"
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (paymentsConfig.isEligibleForEmi(effectivePrice)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    EmiEligibilityBadge(
                        amountInr = effectivePrice,
                        paymentsConfig = paymentsConfig,
                        appLanguage = appLanguage,
                        onViewPlansClick = { showEmiPlansSheet = true }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Delivery & Seal Guarantee features
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = appLanguage.tr(
                                    english = "Delivery: ~2 hrs across Sandhaur & nearby villages",
                                    punjabi = "ਡਿਲਿਵਰੀ: ਸੰਧੌੜ ਅਤੇ ਆਸ-ਪਾਸ ਪਿੰਡਾਂ ਵਿੱਚ ~੨ ਘੰਟੇ",
                                    hindi = "डिलीवरी: संधौड़ और आसपास के गाँवों में ~2 घंटे"
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = GoldSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = appLanguage.tr(
                                    english = "100% Genuine Sealed Factory Packaged Guarantee",
                                    punjabi = "੧੦੦% ਅਸਲੀ ਸੀਲਬੰਦ ਪੈਕਿੰਗ ਗਾਰੰਟੀ",
                                    hindi = "100% असली सीलबंद पैकेजिंग की गारंटी"
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Add/Quantity with selected Size/Color & WhatsApp Quick Inquiry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!product.isInStock) {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color(0xFFE7E8E3),
                                disabledContentColor = Color(0xFF7B8C83)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("detail_out_of_stock_btn")
                        ) {
                            Text(
                                text = appLanguage.tr("Out of Stock", "ਸਟਾਕ ਖਤਮ", "स्टॉक खत्म"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else if (variantQty == 0) {
                        val useCarton = isPetiMode && product.isBulkEligible
                        Button(
                            onClick = { onAddToCart(product, selectedSize, selectedColor, useCarton) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("detail_add_to_cart_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (useCarton) "Add Peti (${product.petiUnits}x)" else appLanguage.tr("Add to Cart", "ਕਾਰਟ ਵਿੱਚ ਜੋੜੋ", "कार्ट में जोड़ें"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        val useCarton = isPetiMode && product.isBulkEligible
                        val unitSale = if (useCarton) petiTotalPrice else unitSalePrice
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(EmeraldContainer, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    onUpdateQuantity(product.id, variantQty - 1, selectedSize, selectedColor, useCarton)
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = OnEmeraldContainer)
                            }

                            Text(
                                text = "$variantQty ${if (useCarton) "Peti" else ""} in Cart (₹${unitSale * variantQty})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = OnEmeraldContainer
                                )
                            )

                            IconButton(
                                onClick = {
                                    onUpdateQuantity(product.id, variantQty + 1, selectedSize, selectedColor, useCarton)
                                },
                                enabled = variantQty < product.stock
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = if (variantQty < product.stock) OnEmeraldContainer else Color(0xFF9AA89F)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val variantNote = listOfNotNull(
                                selectedSize.takeIf { it.isNotBlank() }?.let { "Size: $it" },
                                selectedColor.takeIf { it.isNotBlank() }?.let { "Color: $it" }
                            ).joinToString(", ")
                            val variantPart = if (variantNote.isNotBlank()) " ($variantNote)" else ""
                            val msg = "Hello Nav Grocery Store, I have a query about ${product.name}$variantPart (SKU: ${product.id}, Store Price: ₹$unitSalePrice). Is it available?"
                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=${StoreProfile.WHATSAPP_NUMBER}&text=${URLEncoder.encode(msg, "UTF-8")}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }

    if (showEmiPlansSheet) {
        val effectivePriceForSheet = if (isPetiMode && product.isBulkEligible) petiTotalPrice else unitSalePrice
        EmiPlansBottomSheet(
            amountInr = effectivePriceForSheet,
            paymentsConfig = paymentsConfig,
            appLanguage = appLanguage,
            onDismiss = { showEmiPlansSheet = false }
        )
    }
}
