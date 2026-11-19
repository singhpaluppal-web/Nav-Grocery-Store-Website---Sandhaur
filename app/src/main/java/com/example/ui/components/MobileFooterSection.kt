package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.WarmCreamBorder

/**
 * Structured Mobile Footer Section with singhpaluppal@gmail.com, +91 8847370905,
 * expandable + category accordions, Google Play badge, social links, payment pills,
 * and interactive Warranty Policy / Terms of Use / Privacy Policy modals.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MobileFooterSection(
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    onCategorySelected: (String) -> Unit,
    onBrandSelected: (String) -> Unit,
    onOpenDeals: () -> Unit,
    onEmailSupport: () -> Unit,
    onWhatsAppSupport: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    var expandedAccordion by remember { mutableStateOf<String?>(null) }
    var activePolicyModal by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ForestGreenDark)
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .testTag("mobile_footer_section")
    ) {
        // Brand & Official Contact Card
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_nav_brand_logo),
                contentDescription = "Nav Grocery Store Navkart Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.dp, SignatureGold, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = appLanguage.tr(
                    english = "NAV GROCERY STORE NAVKART",
                    punjabi = "ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ & ਨਵਕਾਰਟ",
                    hindi = "नव ग्रोसरी स्टोर और नवकार्ट"
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = SignatureGold,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = StoreProfile.LOCATION,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onEmailSupport,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("footer_email_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Email Support", tint = SignatureGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = StoreProfile.FOOTER_CONTACT_EMAIL,
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        maxLines = 1
                    )
                }
            }

            Surface(
                onClick = onWhatsAppSupport,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("footer_phone_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Helplines", tint = SignatureGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${StoreProfile.RAW_PHONE} / ${StoreProfile.SECONDARY_RAW_PHONE}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { onOpenUrl(StoreProfile.MAPS_QUERY_URL) },
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("footer_maps_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Google Maps Directions", tint = SignatureGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Google Maps (HPCP+Q8)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
            Surface(
                onClick = { onOpenUrl(StoreProfile.GOOGLE_REVIEW_URL) },
                color = SignatureGold,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("footer_rate_google_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                    Icon(Icons.Default.Star, contentDescription = "Rate Us on Google", tint = ForestGreenDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rate Us (+25 Coins)", color = ForestGreenDark, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Expandable + Accordions
        val accordions = listOf(
            "Electronics" to listOf("Electronics and Mobiles" to { onCategorySelected("Electronics and Mobiles") }, "Smartphones & 5G" to { onCategorySelected("Electronics and Mobiles") }, "Audio & Speakers" to { onCategorySelected("Electronics and Mobiles") }),
            "Fashion" to listOf("Clothing and Apparel" to { onCategorySelected("Clothing and Apparel") }, "Footwear and Shoes" to { onCategorySelected("Footwear and Shoes") }, "Personal Care" to { onCategorySelected("Personal Care") }),
            "Home and Kitchen" to listOf("Home and Kitchen" to { onCategorySelected("Home and Kitchen") }, "Prestige Appliances" to { onBrandSelected("Prestige") }, "Bajaj Kitchen" to { onBrandSelected("Bajaj") }),
            "Grocery Categories" to listOf("Packaged Groceries" to { onCategorySelected("Packaged Groceries") }, "Atta & Dals" to { onCategorySelected("Atta & Dals") }, "Desi Ghee & Oils" to { onCategorySelected("Desi Ghee & Oils") }, "Cold Chain Frozen Foods" to { onCategorySelected("Cold Chain Frozen Foods") }),
            "Top Brands" to listOf("Aashirvaad" to { onBrandSelected("Aashirvaad") }, "Amul & Verka" to { onBrandSelected("Verka") }, "Tata & Fortune" to { onBrandSelected("Tata") }, "Puma & Sony" to { onBrandSelected("Puma") }),
            "Popular Deals" to listOf("Spin the Wheel (24h Rewards)" to onOpenDeals, "Shopping Quests" to onOpenDeals, "Flat ${offersConfig.discountLabelPercent}% Wholesale Offer" to { onCategorySelected("All Products") })
        )

        accordions.forEach { (sectionTitle, itemsList) ->
            val isOpen = expandedAccordion == sectionTitle
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedAccordion = if (isOpen) null else sectionTitle }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Icon(
                        imageVector = if (isOpen) Icons.Default.Remove else Icons.Default.Add,
                        contentDescription = if (isOpen) "Collapse $sectionTitle" else "Expand $sectionTitle",
                        tint = SignatureGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                AnimatedVisibility(visible = isOpen) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        itemsList.forEach { (itemLabel, action) ->
                            Surface(
                                onClick = action,
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = itemLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(color = SignatureGoldContainer, fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Get it on Google Play Badge & Social Links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { onOpenUrl(StoreProfile.PLAY_STORE_URL) },
                color = Color.Black,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SignatureGold),
                modifier = Modifier.testTag("google_play_badge")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SignatureGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("GET IT ON", fontSize = 8.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Text("Google Play", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { onOpenUrl(StoreProfile.INSTAGRAM_URL) },
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("IG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
                Surface(
                    onClick = { onOpenUrl(StoreProfile.FACEBOOK_URL) },
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("FB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
                Surface(
                    onClick = { onOpenUrl(StoreProfile.YOUTUBE_URL) },
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("YT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Accepted Payments Badges
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("UPI Instant", "RuPay", "Visa", "Mastercard", "Cash on Delivery").forEach { badge ->
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Play Store Compliance Legal Links (Privacy, Terms, Return & Refund, Warranty)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                "Privacy Policy" to { onOpenUrl(StoreProfile.PRIVACY_POLICY_URL) },
                "Terms & Conditions" to { onOpenUrl(StoreProfile.TERMS_URL) },
                "Return & Refund Policy" to { onOpenUrl(StoreProfile.REFUND_POLICY_URL) },
                "Warranty Policy" to { activePolicyModal = "Warranty Policy" }
            ).forEach { (policyTitle, onClickAction) ->
                Text(
                    text = policyTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SignatureGold,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable { onClickAction() }
                        .padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "© Nav Grocery Store Navkart • Main Bazaar, Sandhaur, District Malerkotla, Punjab 148020 • GSTIN: ${StoreProfile.GSTIN}",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 10.sp
            )
        )
    }

    if (activePolicyModal != null) {
        val title = activePolicyModal!!
        val body = "All Electronics, Mobiles & Appliances carry official manufacturer brand warranty (7-day replacement for defects). Apparel & Footwear support 7-day size exchange on unworn items with original tags. Sealed groceries & cold-chain frozen foods follow strict FSSAI non-returnable sealed-pack hygiene with damage/expiry replacement."
        AlertDialog(
            onDismissRequest = { activePolicyModal = null },
            title = { Text(title, fontWeight = FontWeight.ExtraBold, color = ForestGreen) },
            text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = { activePolicyModal = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text(appLanguage.tr("Close", "ਬੰਦ ਕਰੋ", "बंद करें"))
                }
            }
        )
    }
}
