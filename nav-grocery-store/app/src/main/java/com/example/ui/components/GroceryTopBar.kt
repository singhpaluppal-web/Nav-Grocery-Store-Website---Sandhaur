package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoreProfile
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.OnSignatureGoldContainer
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer

/**
 * Public App TopBar for NavKart Buyers, Shopkeepers, and Riders.
 * STRICT PRIVACY SHIELD: No owner admin PIN or owner personal PII in public view.
 */
@Composable
fun GroceryTopBar(
    isPunjabi: Boolean,
    isTestModeActive: Boolean,
    onToggleLanguage: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onSellerRegisterClick: () -> Unit,
    onRiderHubClick: () -> Unit,
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

    Column(modifier = Modifier.fillMaxWidth()) {
        // Test Mode Top Strip (When Sandbox Active)
        if (isTestModeActive) {
            Surface(
                color = SignatureGold,
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
                        text = "Sandhaur Hub",
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
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // NavKart Branding
                    Column(modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = StoreProfile.APP_NAME,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SignatureGold)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "12% OFF",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF332000)
                                    )
                                )
                            }
                        }

                        Text(
                            text = if (isPunjabi) "${StoreProfile.APP_NAME_PUNJABI} • ${StoreProfile.LOCATION_PUNJABI}" else "NavKart • Main Bazaar, Sandhaur",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action Pills with clean 10-12dp horizontal spacing (never crowded)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        // AI Shopping Assistant pill
                        Surface(
                            onClick = onAiAssistantClick,
                            shape = RoundedCornerShape(16.dp),
                            color = SignatureGold,
                            modifier = Modifier
                                .testTag("ai_assistant_top_btn")
                                .height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = Color(0xFF332000),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI Help",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF332000),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Sell on NavKart
                        Surface(
                            onClick = onSellerRegisterClick,
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .testTag("sell_on_navkart_btn")
                                .height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = "Sell",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPunjabi) "ਵੇਚੋ" else "Sell",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Delivery Rider Hub Icon
                        IconButton(
                            onClick = onRiderHubClick,
                            modifier = Modifier
                                .testTag("rider_hub_top_btn")
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBike,
                                contentDescription = "Delivery Rider Hub",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Legal & Safety (Indian E-Commerce Compliance)
                        IconButton(
                            onClick = onLegalClick,
                            modifier = Modifier
                                .testTag("legal_safety_btn")
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Policy,
                                contentDescription = "Indian Laws & Safety",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Language Switcher
                        Surface(
                            onClick = onToggleLanguage,
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Language",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPunjabi) "ਪੰਜਾਬੀ" else "EN / PA",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        // WhatsApp Chat Button
                        IconButton(
                            onClick = onWhatsAppClick,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "WhatsApp Helpline",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Status Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOpen) Color(0xFF4CAF50) else Color(0xFFFF9800))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOpen) {
                                if (isPunjabi) "ਸਟੋਰ ਖੁੱਲ੍ਹਾ ਹੈ (8:00 AM - 9:30 PM)" else "Open 8:00 AM – 9:30 PM"
                            } else {
                                if (isPunjabi) "ਸਵੇਰੇ 8:00 ਵਜੇ ਖੁੱਲ੍ਹੇਗਾ" else "Opens at 8:00 AM"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Text(
                        text = if (isPunjabi) "ਸੰਧੌੜ ਖੇਤਰ ~2 ਘੰਟੇ ਡਿਲਿਵਰੀ" else "~2 hr Delivery Sandhaur & Villages",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SignatureGoldContainer,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Animated Premium Gold & Emerald Marquee Ticker Strip
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
                Text(
                    text = if (isPunjabi)
                        "🌾 ਨਵਕਾਰਟ ਸੰਧੌੜ: ਸਾਰੇ 308 ਸੀਲਬੰਦ ਗ੍ਰੋਸਰੀ ਅਤੇ ਫ੍ਰੋਜ਼ਨ ਆਈਟਮਾਂ 'ਤੇ ਫਲੈਟ 12% ਛੋਟ • ~2 ਘੰਟੇ ਡਿਲਿਵਰੀ • WhatsApp: +91 8847370905 🚚"
                    else
                        "🌾 NavKart Sandhaur Hub: Flat 12% OFF MRP on 308 Verified Sealed Groceries & Frozen Foods • ~2 Hr Express Delivery • WhatsApp: +91 8847370905 🚚",
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
