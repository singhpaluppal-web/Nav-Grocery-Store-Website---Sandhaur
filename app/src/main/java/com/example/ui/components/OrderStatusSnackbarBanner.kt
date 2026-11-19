package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.security.OrderStatusChange
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.SignatureGold

class OrderStatusSnackbarVisuals(
    val orderId: String,
    val previousStatus: String,
    val newStatus: String,
    override val message: String,
    override val actionLabel: String?,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Long
) : SnackbarVisuals

fun formatOrderStatusNotificationText(
    change: OrderStatusChange,
    appLanguage: AppLanguage
): String = when (appLanguage) {
    AppLanguage.PUNJABI -> "ਆਰਡਰ ${change.orderId}: ${change.previousStatus} → ${change.newStatus}"
    AppLanguage.HINDI -> "ऑर्डर ${change.orderId}: ${change.previousStatus} → ${change.newStatus}"
    AppLanguage.ENGLISH -> "Order ${change.orderId}: ${change.previousStatus} → ${change.newStatus}"
}

fun formatOrderStatusNotificationText(
    change: OrderStatusChange,
    isPunjabi: Boolean
): String = formatOrderStatusNotificationText(
    change = change,
    appLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH
)

@Composable
fun OrderStatusSnackbarBanner(
    snackbarData: SnackbarData,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    modifier: Modifier = Modifier
) {
    val visuals = snackbarData.visuals
    val statusVisuals = visuals as? OrderStatusSnackbarVisuals
    val statusLower = (statusVisuals?.newStatus ?: visuals.message).lowercase()
    val statusIcon = when {
        statusLower.contains("out for delivery") || statusLower.contains("dispatch") -> Icons.Default.LocalShipping
        statusLower.contains("pack") -> Icons.Default.Inventory2
        statusLower.contains("deliver") -> Icons.Default.CheckCircle
        else -> Icons.Default.NotificationsActive
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ForestGreenDark,
        border = BorderStroke(1.5.dp, SignatureGold),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("order_status_snackbar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = SignatureGold.copy(alpha = 0.2f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusVisuals?.newStatus ?: "Order Status Update",
                        tint = SignatureGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (statusVisuals != null) {
                        appLanguage.tr(
                            english = "Live Order Update • ${statusVisuals.orderId}",
                            punjabi = "ਲਾਈਵ ਆਰਡਰ ਅਪਡੇਟ • ${statusVisuals.orderId}",
                            hindi = "लाइव ऑर्डर अपडेट • ${statusVisuals.orderId}"
                        )
                    } else {
                        appLanguage.tr("Order Update", "ਆਰਡਰ ਅਪਡੇਟ", "ऑर्डर अपडेट")
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = SignatureGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (statusVisuals != null) {
                        "${statusVisuals.previousStatus} → ${statusVisuals.newStatus}"
                    } else {
                        visuals.message
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val actionText = visuals.actionLabel
            if (!actionText.isNullOrBlank()) {
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = SignatureGold,
                        contentColor = ForestGreenDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("order_status_snackbar_action")
                ) {
                    Text(
                        text = actionText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(
                onClick = { snackbarData.dismiss() },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("order_status_snackbar_dismiss")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss notification",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
