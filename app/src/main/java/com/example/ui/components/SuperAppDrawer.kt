package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.MarketplaceCategory
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmCreamBorder

/**
 * Slide-out Super-App Hamburger Drawer listing all Nav Grocery Store Navkart
 * departments dynamically with right chevron arrows, Bestsellers, Deals, and EN/PA/HI switching.
 */
@Composable
fun SuperAppDrawerSheet(
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    userName: String?,
    dynamicCategories: List<MarketplaceCategory> = emptyList(),
    onSelectDepartmentCategory: (String) -> Unit,
    onBestsellersClick: () -> Unit,
    onDealsClick: () -> Unit,
    onAccountOrdersClick: () -> Unit,
    onLegalClick: () -> Unit,
    onToggleLanguage: () -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit = { onToggleLanguage() }
) {
    ModalDrawerSheet(
        drawerContainerColor = WarmCream,
        modifier = Modifier.width(310.dp)
    ) {
        // Brand Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForestGreenDark)
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nav_brand_logo),
                    contentDescription = "Nav Grocery Store Navkart Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, SignatureGold, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appLanguage.tr(
                            english = "Nav Grocery Store Navkart",
                            punjabi = "ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ ਨਵਕਾਰਟ",
                            hindi = "नव ग्रोसरी स्टोर नवकार्ट"
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp
                        )
                    )
                    Text(
                        text = appLanguage.tr(
                            english = "Main Bazaar, Sandhaur 148020",
                            punjabi = "ਮੁੱਖ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ 148020",
                            hindi = "मुख्य बाज़ार, संधौड़ 148020"
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SignatureGoldContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1-Tap EN | ਪੰਜਾਬੀ | हिंदी Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Translate, contentDescription = "Language", tint = SignatureGold, modifier = Modifier.size(15.dp))
                AppLanguage.entries.forEach { lang ->
                    val selected = appLanguage == lang
                    Surface(
                        onClick = { onSelectLanguage(lang) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) SignatureGold else Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.testTag("drawer_lang_${lang.code}")
                    ) {
                        Text(
                            text = lang.chipLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selected) ForestGreenDark else Color.White,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                onClick = onAccountOrdersClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = SignatureGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = userName?.takeIf { it.isNotBlank() }?.let { "Namaste, $it" }
                            ?: appLanguage.tr(
                                english = "Sign in with Gmail Account",
                                punjabi = "ਗੂਗਲ ਨਾਲ ਸਾਈਨ ਇਨ ਕਰੋ",
                                hindi = "Google / Gmail से साइन इन करें"
                            ),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            DrawerSectionTitle(
                appLanguage.tr(
                    english = "NAV GROCERY (100% SEALED • NO PRODUCE)",
                    punjabi = "ਨਵ ਗ੍ਰੋਸਰੀ (ਸੀਲਬੰਦ + ਫ੍ਰੋਜ਼ਨ)",
                    hindi = "नव ग्रोसरी (१००% सीलबंद राशन)"
                )
            )
            DrawerNavItem(Icons.Default.Inventory2, appLanguage.tr("Packaged Groceries", "ਪੈਕਡ ਗ੍ਰੋਸਰੀ", "पैक्ड ग्रोसरी और राशन"), onClick = { onSelectDepartmentCategory("Packaged Groceries") })
            DrawerNavItem(Icons.Default.Inventory2, appLanguage.tr("Atta & Dals", "ਆਟਾ ਅਤੇ ਦਾਲਾਂ", "आटा और दालें"), onClick = { onSelectDepartmentCategory("Atta & Dals") })
            DrawerNavItem(Icons.Default.Verified, appLanguage.tr("Desi Ghee & Oils", "ਦੇਸੀ ਘਿਓ ਤੇ ਤੇਲ", "देसी घी और तेल"), onClick = { onSelectDepartmentCategory("Desi Ghee & Oils") })
            DrawerNavItem(Icons.Default.Inventory2, appLanguage.tr("Spices", "ਮਸਾਲੇ", "मसाले और नमक"), onClick = { onSelectDepartmentCategory("Spices") })
            DrawerNavItem(Icons.Default.AcUnit, appLanguage.tr("Cold Chain Frozen Foods", "ਫ੍ਰੋਜ਼ਨ ਭੋਜਨ", "कोल्ड-चेन फ्रोज़न आइटम"), onClick = { onSelectDepartmentCategory("Cold Chain Frozen Foods") })

            HorizontalDivider(color = WarmCreamBorder, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

            DrawerSectionTitle(
                appLanguage.tr(
                    english = "NAVKART SUPERMALL DEPARTMENTS",
                    punjabi = "ਨਵਕਾਰਟ ਸੁਪਰਮਾਲ",
                    hindi = "नवकार्ट सुपरमॉल विभाग"
                )
            )
            DrawerNavItem(Icons.Default.Devices, appLanguage.tr("Electronics and Mobiles", "ਇਲੈਕਟ੍ਰਾਨਿਕਸ ਅਤੇ ਮੋਬਾਈਲ", "इलेक्ट्रॉनिक्स और मोबाइल"), onClick = { onSelectDepartmentCategory("Electronics and Mobiles") })
            DrawerNavItem(Icons.Default.Checkroom, appLanguage.tr("Clothing and Apparel", "ਕੱਪੜੇ ਅਤੇ ਫੈਸ਼ਨ", "कपड़े और फैशन"), onClick = { onSelectDepartmentCategory("Clothing and Apparel") })
            DrawerNavItem(Icons.Default.SportsSoccer, appLanguage.tr("Footwear and Shoes", "ਜੁੱਤੀਆਂ ਅਤੇ ਚੱਪਲਾਂ", "जूते और चप्पल"), onClick = { onSelectDepartmentCategory("Footwear and Shoes") })
            DrawerNavItem(Icons.Default.Home, appLanguage.tr("Home and Kitchen", "ਘਰ ਅਤੇ ਰਸੋਈ", "होम और किचन"), onClick = { onSelectDepartmentCategory("Home and Kitchen") })
            DrawerNavItem(Icons.Default.Spa, appLanguage.tr("Personal Care", "ਪਰਸਨਲ ਕੇਅਰ ਅਤੇ ਸਾਬਣ", "कॉस्मेटिक्स, साबुन और पर्सनल केयर"), onClick = { onSelectDepartmentCategory("Personal Care") })

            val defaultDrawerSet = setOf(
                "Packaged Groceries", "Atta & Dals", "Desi Ghee & Oils", "Spices",
                "Cold Chain Frozen Foods", "Electronics and Mobiles", "Clothing and Apparel",
                "Footwear and Shoes", "Home and Kitchen", "Personal Care", "All Products"
            )
            val extraRemoteCats = dynamicCategories.filter { cat ->
                cat.isActive && defaultDrawerSet.none { it.equals(cat.name, ignoreCase = true) }
            }
            if (extraRemoteCats.isNotEmpty()) {
                HorizontalDivider(color = WarmCreamBorder, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                DrawerSectionTitle(
                    appLanguage.tr(
                        english = "LIVE MARKETPLACE CATEGORIES",
                        punjabi = "ਲਾਈਵ ਮਾਰਕੀਟਪਲੇਸ ਸ਼੍ਰੇਣੀਆਂ",
                        hindi = "लाइव मार्केटप्लेस श्रेणियाँ"
                    )
                )
                extraRemoteCats.forEach { cat ->
                    DrawerNavItem(Icons.Default.Storefront, cat.displayName(appLanguage), onClick = {
                        onSelectDepartmentCategory(cat.name)
                    })
                }
            }

            HorizontalDivider(color = WarmCreamBorder, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

            DrawerSectionTitle(
                appLanguage.tr(
                    english = "HIGHLIGHTS & HUBS",
                    punjabi = "ਖਾਸ ਸੈਕਸ਼ਨ ਅਤੇ ਸੇਵਾਵਾਂ",
                    hindi = "ऑफ़र्स, ऑर्डर और जानकारी"
                )
            )
            DrawerNavItem(Icons.Default.Star, appLanguage.tr("Bestsellers & Top Deals", "ਸਭ ਤੋਂ ਵੱਧ ਬੱਚਤ ਵਾਲੇ ਉਤਪਾਦ", "बेस्टसेलर्स और टॉप डील्स"), onClick = onBestsellersClick)
            DrawerNavItem(Icons.Default.LocalOffer, appLanguage.tr("Spin the Wheel & Quests", "ਸਪਿਨ ਅਤੇ ਇਨਾਮ", "स्पिन व्हील और नव कॉइन्स"), onClick = onDealsClick)
            DrawerNavItem(Icons.Default.AccountCircle, appLanguage.tr("Account & My Orders", "ਖਾਤਾ ਅਤੇ ਮੇਰੇ ਆਰਡਰ", "मेरा खाता और ऑर्डर"), onClick = onAccountOrdersClick)
            DrawerNavItem(Icons.Default.Policy, appLanguage.tr("Store Laws, Safety & Info", "ਸਟੋਰ ਨਿਯਮ ਅਤੇ ਜਾਣਕਾਰੀ", "स्टोर नियम और सहायता"), onClick = onLegalClick)
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            color = ForestGreen,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp
        ),
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
    )
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF1A2A22),
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF7B8C83),
            modifier = Modifier.size(18.dp)
        )
    }
}
