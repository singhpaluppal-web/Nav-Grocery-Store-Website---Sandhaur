package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StoreProfile
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer
import com.example.ui.theme.WholesaleRed

/**
 * Sticky Mobile Super-App Header with Left Hamburger Drawer Trigger,
 * Nav Grocery Store Navkart Brand Logo, Clickable Location Pill (Main Bazaar, Sandhaur 148020),
 * Instant Marketplace Search Bar with Trilingual Voice Search (`en-IN` / `pa-IN` / `hi-IN`),
 * 1-tap `English | ਪੰਜਾਬੀ | हिंदी` chips, and Wishlist Heart Badge.
 */
@Composable
fun GroceryTopBar(
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    isTestModeActive: Boolean,
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    totalProductsCount: Int = 0,
    selectedLocationLabel: String = StoreProfile.LOCATION_SHORT,
    wishlistCount: Int = 0,
    signedInName: String? = null,
    activeOrdersCount: Int = 0,
    marqueeText: String = "",
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onOpenDrawerClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onWishlistClick: () -> Unit = {},
    onParchiVoiceClick: () -> Unit = {},
    onAccountOrdersClick: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit = { onToggleLanguage() },
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onLegalClick: () -> Unit
) {
    val isOpen = StoreProfile.isStoreOpen()
    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    val marqueeOffset by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marqueeOffset"
    )

    val voiceSearchController = rememberVoiceInputController(
        currentLanguage = appLanguage,
        promptTitle = appLanguage.tr(
            english = "Speak product name (English / ਪੰਜਾਬੀ / हिंदी)",
            punjabi = "ਚੀਜ਼ ਦਾ ਨਾਮ ਬੋਲੋ (ਪੰਜਾਬੀ / हिंदी / English)",
            hindi = "उत्पाद का नाम बोलें (हिंदी / ਪੰਜਾਬੀ / English)"
        )
    ) { transcript, detectedLang ->
        onSelectLanguage(detectedLang)
        onSearchQueryChange(transcript)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isTestModeActive) {
            Surface(
                color = SignatureGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TABLET TEST MODE ACTIVE (SANDBOX)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Text(
                        text = "Sandhaur 148020",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                }
            }
        }

        Surface(
            color = ForestGreenDark,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!isTestModeActive) Modifier.statusBarsPadding() else Modifier)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Row 1: Hamburger Drawer + Brand Logo + Delivery Location + Wishlist Heart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onOpenDrawerClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("open_drawer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Store Menu Drawer",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Image(
                            painter = painterResource(id = R.drawable.ic_nav_brand_logo),
                            contentDescription = "Nav Grocery Store Navkart Official Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, SignatureGold, CircleShape)
                                .testTag("topbar_brand_logo")
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.padding(end = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = appLanguage.tr(
                                        english = "Nav Grocery Store Navkart",
                                        punjabi = "ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ ਨਵਕਾਰਟ",
                                        hindi = "नव ग्रोसरी स्टोर नवकार्ट"
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontSize = 14.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (offersConfig.sanitizedDiscountPercent > 0.0) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SignatureGold)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "${offersConfig.discountLabelPercent}% OFF",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF2B1C00),
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                            }

                            // Clickable Sandhaur 148020 Location Pill
                            Surface(
                                onClick = onLocationClick,
                                color = Color.Transparent,
                                modifier = Modifier.testTag("location_selector_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Delivery Location",
                                        tint = SignatureGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = selectedLocationLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = SignatureGoldContainer,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = SignatureGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Right Actions: Wishlist Heart Badge + WhatsApp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onWishlistClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("wishlist_header_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (wishlistCount > 0) {
                                        Badge(containerColor = WholesaleRed, contentColor = Color.White) {
                                            Text("$wishlistCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (wishlistCount > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Saved Wishlist Items",
                                    tint = if (wishlistCount > 0) SignatureGold else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onWhatsAppClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("whatsapp_header_btn")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "WhatsApp Support",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: Full-width Instant Marketplace Search Bar with Trilingual Voice Search Mic
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("top_instant_search_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Marketplace Products",
                            tint = ForestGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                val countPrefix = if (totalProductsCount > 0) "$totalProductsCount+ " else ""
                                Text(
                                    text = if (voiceSearchController.isListening) {
                                        voiceSearchController.listeningLanguage.tr(
                                            english = "Listening (en-IN)... Speak item name",
                                            punjabi = "ਸੁਣ ਰਿਹਾ ਹਾਂ (pa-IN)... ਚੀਜ਼ ਬੋਲੋ",
                                            hindi = "सुन रहा हूँ (hi-IN)... उत्पाद का नाम बोलें"
                                        )
                                    } else {
                                        appLanguage.tr(
                                            english = "Search ${countPrefix}items (Groceries, Electronics, Cosmetics, Fashion)...",
                                            punjabi = "${countPrefix}ਉਤਪਾਦ ਖੋਜੋ (ਗ੍ਰੋਸਰੀ, ਇਲੈਕਟ੍ਰਾਨਿਕਸ, ਫੈਸ਼ਨ)...",
                                            hindi = "${countPrefix}उत्पाद खोजें (राशन, इलेक्ट्रॉनिक्स, कॉस्मेटिक्स, कपड़े)..."
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (voiceSearchController.isListening) WholesaleRed else Color(0xFF6B7B72),
                                        fontSize = 12.sp,
                                        fontWeight = if (voiceSearchController.isListening) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF0E1B13),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                cursorBrush = SolidColor(ForestGreenDark),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search",
                                    tint = Color(0xFF5A6D63),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                if (voiceSearchController.isListening) {
                                    voiceSearchController.stopListening()
                                } else {
                                    voiceSearchController.startListening(appLanguage)
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (voiceSearchController.isListening) WholesaleRed.copy(alpha = 0.14f) else ForestGreenDark.copy(alpha = 0.08f))
                                .testTag("topbar_voice_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Search (${appLanguage.speechLocaleTag})",
                                tint = if (voiceSearchController.isListening) WholesaleRed else ForestGreen,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 3: Quick Utility Strip (1-tap EN/PA/HI Chips, Parchi/Bol Ke Khareedo, Smart Helper, Account, Call, Info)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1-Tap Trilingual Switcher Bar (English | ਪੰਜਾਬੀ | हिंदी)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SignatureGold.copy(alpha = 0.7f)),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Translate,
                                contentDescription = "Language Selector",
                                tint = SignatureGold,
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(13.dp)
                            )
                            AppLanguage.entries.forEach { lang ->
                                val active = appLanguage == lang
                                Surface(
                                    onClick = { onSelectLanguage(lang) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) SignatureGold else Color.Transparent,
                                    modifier = Modifier.testTag("lang_chip_${lang.code}")
                                ) {
                                    Text(
                                        text = lang.shortLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (active) Color(0xFF2B1C00) else Color.White,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        onClick = onParchiVoiceClick,
                        shape = RoundedCornerShape(14.dp),
                        color = SignatureGold,
                        modifier = Modifier.height(30.dp).testTag("parchi_voice_header_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Bol Ke Khareedo or Send Parchi",
                                tint = Color(0xFF2B1C00),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = appLanguage.tr(
                                    english = "Send Parchi / Voice",
                                    punjabi = "ਪਰਚੀ / ਬੋਲ ਕੇ ਖਰੀਦੋ",
                                    hindi = "बोल के खरीदें / पर्ची"
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2B1C00),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Surface(
                        onClick = onAiAssistantClick,
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1A5E3F),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SignatureGold.copy(alpha = 0.6f)),
                        modifier = Modifier.height(30.dp).testTag("ai_assistant_top_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 9.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Smart Helper", tint = SignatureGold, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = appLanguage.tr("Smart Helper", "ਸਮਾਰਟ ਹੈਲਪਰ", "स्मार्ट हेल्पर"),
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            )
                        }
                    }

                    Surface(
                        onClick = onAccountOrdersClick,
                        shape = RoundedCornerShape(14.dp),
                        color = if (!signedInName.isNullOrBlank()) Color(0xFF1A5E3F) else SignatureGold,
                        modifier = Modifier.height(30.dp).testTag("topbar_account_orders_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Account & My Orders",
                                tint = if (!signedInName.isNullOrBlank()) SignatureGold else Color(0xFF2B1C00),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val firstWord = signedInName?.trim()?.substringBefore(" ")?.take(10)
                            val pillLabel = when {
                                !firstWord.isNullOrBlank() && activeOrdersCount > 0 ->
                                    "$firstWord • ${appLanguage.tr("Orders", "ਆਰਡਰ", "ऑर्डर")} ($activeOrdersCount)"
                                !firstWord.isNullOrBlank() ->
                                    "$firstWord • ${appLanguage.tr("My Orders", "ਮੇਰੇ ਆਰਡਰ", "मेरे ऑर्डर")}"
                                else -> appLanguage.tr("Sign In / Orders", "ਖਾਤਾ / ਆਰਡਰ", "खाता / ऑर्डर")
                            }
                            Text(
                                text = pillLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (!signedInName.isNullOrBlank()) Color.White else Color(0xFF2B1C00),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Surface(
                        onClick = onCallClick,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 9.dp)) {
                            Icon(Icons.Default.Call, contentDescription = "Call Helpline", tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("8847370905", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                        }
                    }

                    Surface(
                        onClick = onLegalClick,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.height(30.dp).testTag("legal_safety_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 9.dp)) {
                            Icon(Icons.Default.Policy, contentDescription = "Safety", tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = appLanguage.tr("Store Info", "ਸਟੋਰ ਜਾਣਕਾਰੀ", "स्टोर जानकारी"),
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOpen) Color(0xFF4CAF50) else Color(0xFFFF9800))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOpen) {
                                appLanguage.tr("Open 8AM–9:30PM", "ਖੁੱਲ੍ਹਾ 8AM–9:30PM", "खुला 8AM–9:30PM")
                            } else {
                                appLanguage.tr("Opens 8AM", "8AM ਖੁੱਲ੍ਹੇਗਾ", "8AM खुलेगा")
                            },
                            style = MaterialTheme.typography.labelSmall.copy(color = SignatureGoldContainer, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Gold Marquee Ticker Strip
        Surface(
            color = SignatureGold,
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clipToBounds(),
                contentAlignment = Alignment.CenterStart
            ) {
                val resolvedTicker = when {
                    offersConfig.isOfferActive && offersConfig.offerBannerText.isNotBlank() -> offersConfig.offerBannerText
                    marqueeText.isNotBlank() -> marqueeText
                    else -> appLanguage.tr(
                        english = "🌾 Nav Grocery Store Navkart Sandhaur 148020: ${offersConfig.discountLabelPercent}% OFF MRP Across Verified Marketplace • 15-Min Express & Pind Dispatch! 🚚",
                        punjabi = "🌾 ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ ਨਵਕਾਰਟ ਸੰਧੌੜ: ਸੀਲਬੰਦ ਗ੍ਰੋਸਰੀ, ਫ੍ਰੋਜ਼ਨ ਅਤੇ ਏ-ਟੂ-ਜ਼ੈੱਡ ਮਾਲ 'ਤੇ ${offersConfig.discountLabelPercent}% ਛੋਟ • 15m ਐਕਸਪ੍ਰੈੱਸ 🚚",
                        hindi = "🌾 नव ग्रोसरी स्टोर नवकार्ट संधौड़: सीलबंद राशन, फ्रोज़न और A-to-Z उत्पादों पर ${offersConfig.discountLabelPercent}% छूट • 15m एक्सप्रेस डिलीवरी 🚚"
                    )
                }
                Text(
                    text = resolvedTicker,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF1F1600),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.graphicsLayer {
                        translationX = marqueeOffset * 420f
                    }
                )
            }
        }
    }
}
