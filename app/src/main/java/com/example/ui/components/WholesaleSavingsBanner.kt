package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.OutlinedButton
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.HeroBannerSlide
import com.example.data.model.StoreHeroPromoConfig
import com.example.data.model.StoreOffersConfig
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldSecondary
import kotlinx.coroutines.delay

@Composable
fun WholesaleSavingsBanner(
    slides: List<HeroBannerSlide> = HeroBannerSlide.DEFAULT_SLIDES,
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    heroPromoConfig: StoreHeroPromoConfig = StoreHeroPromoConfig(),
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    onSlideCtaClick: (String) -> Unit = {},
    onOpenPromoVideo: (String) -> Unit = {},
    onStoreInfoClick: () -> Unit
) {
    val activeSlides = slides.ifEmpty { HeroBannerSlide.DEFAULT_SLIDES }
    val pagerState = rememberPagerState(pageCount = { activeSlides.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    // Smooth auto-scroll every 4.5s unless user is actively dragging
    LaunchedEffect(activeSlides.size, isDragged) {
        if (activeSlides.size > 1 && !isDragged) {
            while (true) {
                delay(4500L)
                val nextPage = (pagerState.currentPage + 1) % activeSlides.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (offersConfig.isOfferActive && offersConfig.offerBannerText.isNotBlank()) {
            Surface(
                color = GoldContainer,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("live_remote_offer_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = "Active Offer",
                        tint = EmeraldDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = offersConfig.offerBannerText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = EmeraldDark,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_banner_carousel")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val slide = activeSlides[page]
                    HeroSlideItem(
                        slide = slide,
                        fallbackDrawableRes = resolveSlideDrawable(page, slide),
                        appLanguage = appLanguage,
                        onShopNowClick = { onSlideCtaClick(slide.ctaCategory) }
                    )
                }

                // Smooth Page Indicator Dots (Bottom End)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(activeSlides.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(if (isSelected) 20.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) GoldSecondary
                                    else Color.White.copy(alpha = 0.55f)
                                )
                        )
                    }
                }
            }
        }

        if (heroPromoConfig.isActive &&
            (heroPromoConfig.title.isNotBlank() || heroPromoConfig.bannerImageUrl.isNotBlank() || heroPromoConfig.videoUrl.isNotBlank())
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3D28)),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_hero_promo_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = GoldSecondary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = heroPromoConfig.badgeText.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF1F1600),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        if (heroPromoConfig.videoUrl.startsWith("https://", ignoreCase = true)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Promo Video",
                                        tint = GoldSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (heroPromoConfig.mediaType.equals("VIDEO", ignoreCase = true)) "PROMO VIDEO" else "VIDEO TOUR",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (heroPromoConfig.title.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = heroPromoConfig.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                    if (heroPromoConfig.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = heroPromoConfig.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GoldContainer
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onSlideCtaClick(heroPromoConfig.ctaCategory.ifBlank { "All Products" }) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldSecondary,
                                contentColor = Color(0xFF1F1600)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text(
                                text = heroPromoConfig.ctaText.ifBlank { "Shop Promo" },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (heroPromoConfig.videoUrl.startsWith("https://", ignoreCase = true)) {
                            OutlinedButton(
                                onClick = { onOpenPromoVideo(heroPromoConfig.videoUrl) },
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldSecondary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Watch Promo Video",
                                    tint = GoldSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = appLanguage.tr("Watch", "ਦੇਖੋ", "देखें"),
                                    color = GoldSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Three Trust Badges Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrustPill(
                icon = Icons.Default.Lock,
                title = appLanguage.tr("100% Sealed", "੧੦੦% ਸੀਲਬੰਦ", "100% सीलबंद"),
                subtitle = appLanguage.tr("Purity Sealed", "ਕੋਈ ਖੁੱਲ੍ਹਾ ਸਾਮਾਨ ਨਹੀਂ", "पूरी तरह सीलबंद"),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStoreInfoClick() }
            )
            TrustPill(
                icon = Icons.Default.AcUnit,
                title = appLanguage.tr("Cold Chain", "ਕੋਲਡ ਚੇਨ", "कोल्ड चेन"),
                subtitle = appLanguage.tr("Sub-zero Frozen", "ਫ੍ਰੋਜ਼ਨ ਪਨੀਰ ਤੇ ਮਟਰ", "फ्रोज़न पनीर व मटर"),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSlideCtaClick("Cold Chain Frozen Foods") }
            )
            TrustPill(
                icon = Icons.Default.Storefront,
                title = appLanguage.tr("Sandhaur Hub", "ਸੰਧੌੜ ਬਾਜ਼ਾਰ", "संधौड़ बाज़ार"),
                subtitle = appLanguage.tr("Main Bazaar Store", "ਮੇਨ ਬਾਜ਼ਾਰ ਸਟੋਰ", "मेन बाज़ार स्टोर"),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStoreInfoClick() }
            )
        }
    }
}

@Composable
private fun HeroSlideItem(
    slide: HeroBannerSlide,
    @DrawableRes fallbackDrawableRes: Int,
    appLanguage: AppLanguage,
    onShopNowClick: () -> Unit
) {
    val context = LocalContext.current
    val rawUrl = slide.imageUrl.trim()
    val remoteUrl = if (rawUrl.startsWith("http://", ignoreCase = true)) {
        "https://${rawUrl.substringAfter("://")}"
    } else {
        rawUrl
    }
    val useRemoteUrl = remoteUrl.startsWith("https://", ignoreCase = true) ||
        remoteUrl.startsWith("data:", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onShopNowClick() }
    ) {
        if (useRemoteUrl) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(remoteUrl)
                    .crossfade(true)
                    .placeholder(fallbackDrawableRes)
                    .error(fallbackDrawableRes)
                    .build(),
                contentDescription = slide.headline,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = fallbackDrawableRes),
                contentDescription = slide.headline,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Rich Emerald + Charcoal gradient overlay for high-contrast typography
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF041A0F).copy(alpha = 0.88f),
                            Color(0xFF072617).copy(alpha = 0.62f),
                            Color.Black.copy(alpha = 0.25f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Luxury Gold Badge
            Surface(
                color = GoldSecondary,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = slide.badge.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F1600),
                        fontSize = 10.sp,
                        letterSpacing = 0.7.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth(0.9f)) {
                Text(
                    text = slide.headline,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 17.sp,
                        lineHeight = 22.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = slide.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = GoldContainer,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Shop Now CTA Button
            Button(
                onClick = onShopNowClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldSecondary,
                    contentColor = Color(0xFF1F1600)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("hero_cta_${slide.id}")
            ) {
                Text(
                    text = if (slide.ctaText == "Shop Now") {
                        appLanguage.tr("Shop Now", "ਹੁਣੇ ਖਰੀਦੋ • Shop Now", "अभी खरीदें • Shop Now")
                    } else {
                        slide.ctaText
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F1600),
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF1F1600),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@DrawableRes
private fun resolveSlideDrawable(index: Int, slide: HeroBannerSlide): Int {
    val key = "${slide.id}_${slide.imageUrl}".lowercase()
    return when {
        key.contains("hero_2") || key.contains("slide_2") || key.contains("fashion") ->
            R.drawable.img_hero_fashion_footwear
        key.contains("hero_3") || key.contains("slide_3") || key.contains("electron") ->
            R.drawable.img_hero_electronics_tech
        key.contains("hero_4") || key.contains("slide_4") || key.contains("grocery") ->
            R.drawable.img_hero_grocery_wholesale
        key.contains("hero_1") || key.contains("slide_1") || key.contains("marketplace") ->
            R.drawable.img_hero_marketplace
        else -> when (index % 4) {
            1 -> R.drawable.img_hero_fashion_footwear
            2 -> R.drawable.img_hero_electronics_tech
            3 -> R.drawable.img_hero_grocery_wholesale
            else -> R.drawable.img_hero_marketplace
        }
    }
}

@Composable
private fun TrustPill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
