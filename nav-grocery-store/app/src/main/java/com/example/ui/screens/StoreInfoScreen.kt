package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.StoreProfile
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.WholesaleRed

@Composable
fun StoreInfoScreen(
    isPunjabi: Boolean,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onInstagramClick: () -> Unit,
    onMapClick: () -> Unit
) {
    val context = LocalContext.current
    val isOpen = StoreProfile.isStoreOpen()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        // Store Header Card with Logo
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nav_store_logo),
                    contentDescription = "NavKart Brand Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(2.dp, GoldSecondary, CircleShape)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = StoreProfile.STORE_BRAND,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )

                Text(
                    text = StoreProfile.STORE_NAME_PUNJABI,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GoldContainer
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = StoreProfile.TAGLINE,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Open/Closed badge
                Surface(
                    color = if (isOpen) Color(0xFF2E7D32) else Color(0xFFE65100),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOpen) {
                                if (isPunjabi) "ਸਟੋਰ ਖੁੱਲ੍ਹਾ ਹੈ (ਰਾਤ ੯:੩੦ ਵਜੇ ਤੱਕ)" else "Store Open (8:00 AM – 9:30 PM)"
                            } else {
                                if (isPunjabi) "ਸਟੋਰ ਬੰਦ ਹੈ (ਸਵੇਰੇ ੮:੦੦ ਖੁੱਲ੍ਹੇਗਾ)" else "Opens Tomorrow at 8:00 AM"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions Grid (Call, WhatsApp, Maps, Instagram)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                icon = Icons.Default.Call,
                title = "Customer Care",
                subtitle = StoreProfile.HELPLINE_PHONE,
                color = EmeraldPrimary,
                onClick = onCallClick,
                modifier = Modifier.weight(1f)
            )

            ActionTile(
                icon = Icons.Default.Chat,
                title = "WhatsApp Desk",
                subtitle = "1-Tap Chat",
                color = Color(0xFF25D366),
                onClick = onWhatsAppClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                icon = Icons.Default.Map,
                title = "Store Location",
                subtitle = "Main Bazaar, Sandhaur",
                color = Color(0xFF1976D2),
                onClick = onMapClick,
                modifier = Modifier.weight(1f)
            )

            ActionTile(
                icon = Icons.Default.PhotoCamera,
                title = "Instagram",
                subtitle = StoreProfile.INSTAGRAM_HANDLE,
                color = Color(0xFFE1306C),
                onClick = onInstagramClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Connected Social Channels (Facebook & YouTube)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                icon = Icons.Default.ThumbUp,
                title = "Facebook",
                subtitle = StoreProfile.FACEBOOK_NAME,
                color = Color(0xFF1877F2),
                onClick = {
                    val uri = Uri.parse(StoreProfile.FACEBOOK_URL)
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                },
                modifier = Modifier.weight(1f)
            )

            ActionTile(
                icon = Icons.Default.PlayCircle,
                title = "YouTube",
                subtitle = StoreProfile.YOUTUBE_HANDLE,
                color = Color(0xFFFF0000),
                onClick = {
                    val uri = Uri.parse(StoreProfile.YOUTUBE_URL)
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Strict Business Policies Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isPunjabi) "ਸਟੋਰ ਦੇ ਸਖ਼ਤ ਨਿਯਮ ਅਤੇ ਗਾਰੰਟੀ" else "Official Store Guarantees & Strict Policy",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                PolicyRow(
                    icon = Icons.Default.Percent,
                    iconTint = WholesaleRed,
                    title = "Wholesale Pricing Rule",
                    description = "Flat 12% OFF on MRP across all 308 locked products (sale = Math.round(mrp * 0.88)). Real wholesale savings for all customers."
                )

                Spacer(modifier = Modifier.height(12.dp))

                PolicyRow(
                    icon = Icons.Default.Lock,
                    iconTint = EmeraldPrimary,
                    title = "100% Sealed Packaged & Cold-Chain Only",
                    description = "NavKart exclusively sells factory sealed packaged groceries (atta, dals, desi ghee, cooking oils, spices) and sub-zero cold-chain frozen items (peas, sweet corn, paneer, french fries). Fresh fruits and fresh vegetables are strictly prohibited and never sold."
                )

                Spacer(modifier = Modifier.height(12.dp))

                PolicyRow(
                    icon = Icons.Default.Schedule,
                    iconTint = GoldSecondary,
                    title = "Store Timings & Express Delivery",
                    description = "Open 8:00 AM to 9:30 PM Daily. ~2 Hour Express Home Delivery across Sandhaur & nearby villages, or 30-minute Store Pickup at Main Bazaar."
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Official Brand & Public Contact Details (PII Shield Applied)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isPunjabi) "ਸੰਪਰਕ ਅਤੇ ਸਹਾਇਤਾ ਵੇਰਵੇ" else "Official Brand Support & Hub",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(label = "Official Brand Name", value = StoreProfile.STORE_BRAND)
                InfoRow(label = "Physical Storefront", value = StoreProfile.LOCATION)
                InfoRow(label = "Customer Support & Helpline", value = "${StoreProfile.HELPLINE_PHONE} (Mon-Sun, 8am-9:30pm)")
                InfoRow(label = "WhatsApp Orders & Desk", value = "+91 8847370905 (Instant Response)")
                InfoRow(label = "Official Public Website", value = "www.navgrocerystore.com")
                InfoRow(label = "Social Channels", value = "Instagram: @nav_grocery_store | FB: Nav Grocery | YT: @shopatnav")

                Spacer(modifier = Modifier.height(10.dp))

                // Visit Website button
                OutlinedButton(
                    onClick = {
                        val uri = Uri.parse(StoreProfile.WEB_URL)
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Visit www.navgrocerystore.com",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PolicyRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
