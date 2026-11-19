package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.FirestoreOrderRecord
import com.example.data.model.Order
import com.example.data.model.ParchiRequest
import com.example.data.model.Product
import com.example.data.model.RashanSubscription
import com.example.data.model.SavedAddress
import com.example.data.model.StoreProfile
import com.example.data.model.UserAccount
import com.example.data.security.InputSanitizer
import com.example.ui.components.DigitalGstInvoiceDialog
import com.example.ui.components.WalletTopupDialog
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmCreamBorder
import com.example.ui.theme.WholesaleRed
import com.google.firebase.auth.FirebaseUser

@Composable
fun AccountScreen(
    currentUser: FirebaseUser?,
    userAccount: UserAccount?,
    savedAddresses: List<SavedAddress>,
    cloudOrders: List<FirestoreOrderRecord>,
    localOrders: List<Order>,
    parchiRequests: List<ParchiRequest> = emptyList(),
    rashanSubscription: RashanSubscription? = null,
    wishlistCount: Int,
    profileCompletionPercent: Int,
    authErrorMessage: String?,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    isDeletingAccount: Boolean = false,
    onGoogleSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSaveAddress: (
        id: String?,
        name: String,
        mobile: String,
        street: String,
        landmark: String,
        village: String,
        pin: String,
        makeDefault: Boolean
    ) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onSetDefaultAddress: (String) -> Unit,
    onOpenWishlistClick: () -> Unit,
    onOpenDealsClick: () -> Unit,
    onOpenParchiClick: () -> Unit = {},
    onConfirmParchiBill: (String) -> Unit = {},
    onReorderRashanBox: () -> Unit = {},
    onRequestWalletTopup: (Int, String) -> Unit = { _, _ -> },
    onApplyReferralCode: (String) -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onOpenPolicyUrl: (String, String) -> Unit = { _, url -> onOpenUrl(url) },
    onCancelOrder: (String) -> Unit = {},
    onDeleteAccountAndData: () -> Unit = {},
    onReorderItems: (List<Product>) -> Unit,
    onReorderCloudOrder: (FirestoreOrderRecord) -> Unit = {},
    onReorderLocalOrder: (Order) -> Unit = {},
    onBrowseCatalogClick: () -> Unit
) {
    val context = LocalContext.current
    var editingAddress by remember { mutableStateOf<SavedAddress?>(null) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var showWalletDialog by remember { mutableStateOf(false) }
    var referralInput by remember { mutableStateOf("") }
    var invoiceCloudOrder by remember { mutableStateOf<FirestoreOrderRecord?>(null) }
    var invoiceLocalOrder by remember { mutableStateOf<Order?>(null) }
    var cancelConfirmOrderId by remember { mutableStateOf<String?>(null) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .testTag("customer_account_screen")
    ) {
        // Welcome Greeting Card + Profile Completion + Gmail OAuth Sign-In
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenDark)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nav_brand_logo),
                            contentDescription = "Nav Grocery Store Navkart Official Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(SignatureGold)
                                .padding(2.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.displayName?.let {
                                    appLanguage.tr("Namaste, $it", "ਸਤ ਸ੍ਰੀ ਅਕਾਲ, $it", "नमस्ते, $it")
                                } ?: appLanguage.tr(
                                    english = "Welcome to Nav Grocery & NavKart",
                                    punjabi = "ਨਵ ਗ੍ਰੋਸਰੀ & ਨਵਕਾਰਟ ਵਿੱਚ ਜੀ ਆਇਆਂ ਨੂੰ",
                                    hindi = "नव ग्रोसरी स्टोर और नवकार्ट में आपका स्वागत है"
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Text(
                                text = currentUser?.email ?: "Main Bazaar, Sandhaur 148020 • Guest Browsing",
                                style = MaterialTheme.typography.bodySmall.copy(color = SignatureGoldContainer)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Profile Completion Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = appLanguage.tr("Profile Completion", "ਪ੍ਰੋਫਾਈਲ ਪੂਰਨਤਾ", "प्रोफ़ाइल पूर्णता"),
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$profileCompletionPercent%",
                            style = MaterialTheme.typography.labelSmall.copy(color = SignatureGold, fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { (profileCompletionPercent / 100f).coerceIn(0f, 1f) },
                        color = SignatureGold,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    if (!authErrorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = authErrorMessage,
                            style = MaterialTheme.typography.labelSmall.copy(color = SignatureGoldContainer)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentUser == null) {
                        Button(
                            onClick = onGoogleSignInClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SignatureGold,
                                contentColor = ForestGreenDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("account_google_signin_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = appLanguage.tr(
                                    english = "1-Click Login & Signup with Gmail",
                                    punjabi = "Gmail ਨਾਲ ਸਾਈਨ ਇਨ / ਸਾਈਨ ਅੱਪ ਕਰੋ",
                                    hindi = "Gmail से 1-क्लिक लॉगिन / साइन अप करें"
                                ),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = SignatureGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${userAccount?.navCoins ?: 0} Nav Coins",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onSignOutClick,
                                border = BorderStroke(1.dp, SignatureGold),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SignatureGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("account_signout_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Quick Action Row: Wishlist + Spin & Quests
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onOpenWishlistClick,
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, WarmCreamBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = WholesaleRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Wishlist", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreenDark)
                            Text("$wishlistCount saved", fontSize = 11.sp, color = Color(0xFF5A6D63))
                        }
                    }
                }

                Surface(
                    onClick = onOpenDealsClick,
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, WarmCreamBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Spin & Quests", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreenDark)
                            Text("Earn Nav Coins", fontSize = 11.sp, color = Color(0xFF5A6D63))
                        }
                    }
                }
            }
        }

        // Prepaid Family Wallet Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Prepaid Family Wallet", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = ForestGreenDark)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${userAccount?.walletBalanceRupees ?: 0}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = ForestGreen
                        )
                        Text(
                            text = "Instant 1-tap balance deduction at express checkout",
                            fontSize = 11.sp,
                            color = Color(0xFF5A6D63)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (currentUser == null) onGoogleSignInClick() else showWalletDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Load via UPI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Mahina Rashan Box + Pind Ambassador Referral Card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, WarmCreamBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Autorenew, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mahina Rashan Box", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = ForestGreenDark)
                            }
                            Text(
                                text = if (rashanSubscription != null)
                                    "Day ${rashanSubscription.monthlyDayOfMonth} • ₹${rashanSubscription.estimatedTotal} (${rashanSubscription.itemsSummary.take(45)}...)"
                                else "Save any Cart as your recurring monthly box (+30 Nav Coins)",
                                fontSize = 11.sp,
                                color = Color(0xFF4B5E54)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onReorderRashanBox,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(if (rashanSubscription != null) "1-Tap Reorder" else "Build in Cart", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Pind Ambassador Referral Card
                val myCode = userAccount?.effectiveReferralCode() ?: "PIND-148020"
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SignatureGoldContainer),
                    border = BorderStroke(1.dp, SignatureGold)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pind Ambassador Referral", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = ForestGreenDark)
                                Text("Share code ($myCode): Both neighbors earn +50 Nav Coins on first order!", fontSize = 11.sp, color = Color(0xFF3A2700))
                            }
                            Surface(
                                onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("NavKart Referral", myCode))
                                    Toast.makeText(context, "Copied $myCode! Share with Sandhaur neighbors.", Toast.LENGTH_SHORT).show()
                                },
                                color = ForestGreen,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = SignatureGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(myCode, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                }
                            }
                        }

                        if (userAccount != null && userAccount.referredBy.isBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = referralInput,
                                    onValueChange = { referralInput = it.uppercase() },
                                    label = { Text("Enter neighbor code (PIND-...)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { onApplyReferralCode(referralInput) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Parchi / 15s Voice Order Tracker
        if (parchiRequests.isNotEmpty() || currentUser != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, WarmCreamBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Parchi & 15s Voice Orders", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = ForestGreenDark)
                            }
                            TextButton(onClick = onOpenParchiClick) {
                                Text("+ New Parchi", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 12.sp)
                            }
                        }
                        if (parchiRequests.isEmpty()) {
                            Text("Send a handwritten Parchi photo or Punjabi voice note directly to Main Bazaar staff.", fontSize = 11.sp, color = Color(0xFF5A6D63))
                        } else {
                            parchiRequests.take(3).forEach { req ->
                                Surface(color = WarmCream, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${req.requestId} • ${req.status}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = ForestGreenDark)
                                            if (req.matchedSummary.isNotBlank()) {
                                                Text(req.matchedSummary, fontSize = 11.sp, color = ForestGreen)
                                            }
                                        }
                                        if (req.status == "BILL_READY" && req.readyBillTotal > 0) {
                                            Button(
                                                onClick = { onConfirmParchiBill(req.requestId) },
                                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Confirm ₹${req.readyBillTotal}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Verified Google Business Profile (HPCP+Q8) Quick Strip
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Google Business Profile • HPCP+Q8 Sandhaur", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = SignatureGold)
                    Text(StoreProfile.LOCATION, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onOpenUrl(StoreProfile.MAPS_QUERY_URL) },
                            border = BorderStroke(1.dp, SignatureGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = SignatureGold, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Directions", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onOpenUrl(StoreProfile.GOOGLE_REVIEW_URL) },
                            colors = ButtonDefaults.buttonColors(containerColor = SignatureGold, contentColor = ForestGreenDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rate Us (+25 Coins)", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Saved Delivery Addresses Manager (Sandhaur 148020)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appLanguage.tr(
                        english = "Saved Delivery Addresses",
                        punjabi = "ਸੇਵ ਕੀਤੇ ਡਿਲਿਵਰੀ ਪਤੇ",
                        hindi = "सहेजे गए डिलीवरी पते"
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = ForestGreenDark
                    )
                )
                Button(
                    onClick = {
                        if (currentUser == null) {
                            onGoogleSignInClick()
                        } else {
                            editingAddress = null
                            showAddressDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("add_address_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Address", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (savedAddresses.isEmpty()) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, WarmCreamBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (currentUser == null) "Sign in with Gmail to save delivery addresses in Sandhaur 148020."
                            else "No saved address yet. Add your Sandhaur 148020 / Malerkotla delivery address for 1-click checkout.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF5A6D63))
                        )
                    }
                }
            }
        } else {
            items(savedAddresses, key = { it.id }) { addr ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(if (addr.defaultAddress) 2.dp else 1.dp, if (addr.defaultAddress) ForestGreen else WarmCreamBorder),
                    modifier = Modifier.testTag("saved_addr_${addr.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(addr.fullName, fontWeight = FontWeight.ExtraBold, color = ForestGreenDark, fontSize = 14.sp)
                                if (addr.defaultAddress) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(color = ForestGreenContainer, shape = RoundedCornerShape(6.dp)) {
                                        Text("DEFAULT", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = ForestGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingAddress = addr
                                        showAddressDialog = true
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Address", tint = ForestGreen, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteAddress(addr.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Address", tint = WholesaleRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Text("+91 ${addr.mobile}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForestGreen)
                        Text(addr.formattedLine(), fontSize = 12.sp, color = Color(0xFF4B5E54))
                        if (!addr.defaultAddress) {
                            TextButton(
                                onClick = { onSetDefaultAddress(addr.id) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Set as Default Address", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                            }
                        }
                    }
                }
            }
        }

        // My Orders Live 4-Stage Status Tracker
        item {
            Text(
                text = appLanguage.tr(
                    english = "My Orders (Live Status Tracker)",
                    punjabi = "ਮੇਰੇ ਆਰਡਰ (ਲਾਈਵ ਟਰੈਕਰ)",
                    hindi = "मेरे ऑर्डर (लाइव स्टेटस ट्रैकर)"
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = ForestGreenDark
                )
            )
        }

        if (cloudOrders.isEmpty() && localOrders.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, WarmCreamBorder)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nav_brand_logo),
                            contentDescription = "Nav Grocery Store Navkart",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No orders placed yet", fontWeight = FontWeight.Bold, color = ForestGreenDark)
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onBrowseCatalogClick,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Browse Marketplace", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(cloudOrders, key = { it.orderId }) { ord ->
                CloudOrderCard(
                    ord = ord,
                    appLanguage = appLanguage,
                    onReorder = { onReorderCloudOrder(ord) },
                    onViewInvoice = { invoiceCloudOrder = ord },
                    onCancelOrderClick = { cancelConfirmOrderId = ord.orderId }
                )
            }
            val cloudIds = cloudOrders.map { it.orderId }.toSet()
            val extraLocal = localOrders.filter { it.orderId !in cloudIds }
            items(extraLocal, key = { it.orderId }) { ord ->
                LocalOrderCard(
                    order = ord,
                    appLanguage = appLanguage,
                    onReorder = { onReorderLocalOrder(ord) },
                    onViewInvoice = { invoiceLocalOrder = ord },
                    onCancelOrderClick = { cancelConfirmOrderId = ord.orderId }
                )
            }
        }

        // About Store & Official Brand Identity Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, WarmCreamBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nav_brand_logo),
                            contentDescription = "Nav Grocery Store Navkart Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "About Nav Grocery Store Navkart",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = ForestGreenDark
                            )
                            Text(
                                text = StoreProfile.LOCATION,
                                fontSize = 11.sp,
                                color = Color(0xFF4B5E54)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Helplines: ${StoreProfile.HELPLINE_PHONE} • ${StoreProfile.SECONDARY_HELPLINE_PHONE} • GSTIN: ${StoreProfile.GSTIN}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ForestGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onOpenUrl(StoreProfile.WEB_URL) },
                        border = BorderStroke(1.dp, ForestGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Visit www.navgrocerystore.com", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                    }
                }
            }
        }

        // Play Store Compliance Legal Links Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, WarmCreamBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = appLanguage.tr("Legal & Store Policies", "ਕਾਨੂੰਨੀ ਨੀਤੀਆਂ", "कानूनी नीतियां व शर्तें"),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = ForestGreenDark
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { onOpenPolicyUrl("Privacy Policy", StoreProfile.PRIVACY_POLICY_URL) },
                            border = BorderStroke(1.dp, WarmCreamBorder),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(34.dp).testTag("account_privacy_policy_btn")
                        ) {
                            Text("Privacy Policy", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                        }
                        OutlinedButton(
                            onClick = { onOpenPolicyUrl("Terms & Conditions", StoreProfile.TERMS_URL) },
                            border = BorderStroke(1.dp, WarmCreamBorder),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(34.dp).testTag("account_terms_btn")
                        ) {
                            Text("Terms & Conditions", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                        }
                    }
                    OutlinedButton(
                        onClick = { onOpenPolicyUrl("Return & Refund Policy", StoreProfile.REFUND_POLICY_URL) },
                        border = BorderStroke(1.dp, WarmCreamBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp).testTag("account_refund_policy_btn")
                    ) {
                        Text("Return & Refund Policy (www.navgrocerystore.com)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                    }
                }
            }
        }

        // Google Play Mandatory Self-Service Account Deletion Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                border = BorderStroke(1.dp, WholesaleRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = WholesaleRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = appLanguage.tr(
                                english = "Delete Account & Personal Data",
                                punjabi = "ਖਾਤਾ ਅਤੇ ਨਿੱਜੀ ਡੇਟਾ ਮਿਟਾਓ",
                                hindi = "खाता और व्यक्तिगत डेटा हटाएं"
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = WholesaleRed
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = appLanguage.tr(
                            english = "Permanently deletes your profile, saved addresses, and cart from Firebase, anonymizes historical tax/order records per law, and signs you out.",
                            punjabi = "ਤੁਹਾਡੀ ਪ੍ਰੋਫਾਈਲ, ਸੇਵ ਕੀਤੇ ਪਤੇ ਅਤੇ ਕਾਰਟ Firebase ਤੋਂ ਹਮੇਸ਼ਾ ਲਈ ਮਿਟਾ ਦਿੰਦਾ ਹੈ ਅਤੇ ਤੁਹਾਨੂੰ ਸਾਈਨ ਆਊਟ ਕਰਦਾ ਹੈ।",
                            hindi = "यह आपके प्रोफ़ाइल, सहेजे गए पते और कार्ट को Firebase से स्थायी रूप से हटा देता है और आपको साइन आउट करता है।"
                        ),
                        fontSize = 11.sp,
                        color = Color(0xFF5D2A2A)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (currentUser == null) onGoogleSignInClick() else showDeleteAccountDialog = true
                        },
                        enabled = !isDeletingAccount,
                        colors = ButtonDefaults.buttonColors(containerColor = WholesaleRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("delete_my_account_btn")
                    ) {
                        if (isDeletingAccount) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentUser == null) {
                                    "Sign In to Manage / Delete Account"
                                } else {
                                    appLanguage.tr(
                                        english = "Delete My Account & Data",
                                        punjabi = "ਮੇਰਾ ਖਾਤਾ ਅਤੇ ਡੇਟਾ ਮਿਟਾਓ",
                                        hindi = "मेरा खाता और डेटा हटाएं"
                                    )
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddressDialog) {
        AddressEditorDialog(
            initialAddress = editingAddress,
            defaultName = currentUser?.displayName.orEmpty(),
            isPunjabi = isPunjabi,
            appLanguage = appLanguage,
            onDismiss = { showAddressDialog = false },
            onSave = { name, mobile, street, landmark, village, pin, makeDefault ->
                onSaveAddress(editingAddress?.id, name, mobile, street, landmark, village, pin, makeDefault)
                showAddressDialog = false
            }
        )
    }

    if (showWalletDialog) {
        WalletTopupDialog(
            currentBalance = (userAccount?.walletBalanceRupees ?: 0L).toInt(),
            isPunjabi = isPunjabi,
            appLanguage = appLanguage,
            onDismiss = { showWalletDialog = false },
            onSubmitTopup = onRequestWalletTopup
        )
    }

    invoiceCloudOrder?.let { cloudOrd ->
        DigitalGstInvoiceDialog(
            cloudOrder = cloudOrd,
            appLanguage = appLanguage,
            onDismiss = { invoiceCloudOrder = null }
        )
    }

    invoiceLocalOrder?.let { localOrd ->
        DigitalGstInvoiceDialog(
            localOrder = localOrd,
            appLanguage = appLanguage,
            onDismiss = { invoiceLocalOrder = null }
        )
    }

    cancelConfirmOrderId?.let { oid ->
        AlertDialog(
            onDismissRequest = { cancelConfirmOrderId = null },
            icon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = WholesaleRed) },
            title = {
                Text(
                    text = appLanguage.tr("Cancel Order $oid?", "ਆਰਡਰ $oid ਰੱਦ ਕਰੋ?", "ऑर्डर $oid रद्द करें?"),
                    fontWeight = FontWeight.ExtraBold,
                    color = WholesaleRed
                )
            },
            text = {
                Text(
                    text = appLanguage.tr(
                        english = "Orders can only be cancelled before Packing / Dispatch. Are you sure you want to cancel $oid?",
                        punjabi = "ਪੈਕਿੰਗ ਸ਼ੁਰੂ ਹੋਣ ਤੋਂ ਪਹਿਲਾਂ ਹੀ ਆਰਡਰ ਰੱਦ ਕੀਤਾ ਜਾ ਸਕਦਾ ਹੈ। ਕੀ ਤੁਸੀਂ $oid ਰੱਦ ਕਰਨਾ ਚਾਹੁੰਦੇ ਹੋ?",
                        hindi = "पैकिंग शुरू होने से पहले ही ऑर्डर रद्द किया जा सकता है। क्या आप $oid रद्द करना चाहते हैं?"
                    ),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelOrder(oid)
                        cancelConfirmOrderId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WholesaleRed)
                ) {
                    Text("Confirm Cancel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelConfirmOrderId = null }) {
                    Text("Keep Order")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = WholesaleRed, modifier = Modifier.size(34.dp)) },
            title = {
                Text(
                    text = appLanguage.tr(
                        english = "Permanently Delete Account?",
                        punjabi = "ਖਾਤਾ ਹਮੇਸ਼ਾ ਲਈ ਮਿਟਾਉਣਾ ਹੈ?",
                        hindi = "खाता स्थायी रूप से हटाएं?"
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = WholesaleRed
                )
            },
            text = {
                Text(
                    text = appLanguage.tr(
                        english = "This action permanently erases your NavKart profile, Gmail link metadata, saved addresses, quests, and cart. Completed tax/order ledgers remain anonymized as required by Indian tax law. You will be signed out immediately.",
                        punjabi = "ਇਹ ਕਾਰਵਾਈ ਤੁਹਾਡੀ ਪ੍ਰੋਫਾਈਲ, ਪਤੇ ਅਤੇ ਕਾਰਟ ਹਮੇਸ਼ਾ ਲਈ ਹਟਾ ਦੇਵੇਗੀ। ਟੈਕਸ ਨਿਯਮਾਂ ਅਨੁਸਾਰ ਪਿਛਲੇ ਆਰਡਰ ਗੁਪਤ (anonymized) ਰਹਿਣਗੇ।",
                        hindi = "यह कार्रवाई आपकी प्रोफ़ाइल, सहेजे गए पते और कार्ट को स्थायी रूप से हटा देगी। भारतीय कर कानून के अनुसार पूर्ण ऑर्डर अनाम रहेंगे।"
                    ),
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccountAndData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WholesaleRed),
                    modifier = Modifier.testTag("confirm_delete_account_btn")
                ) {
                    Text("Yes, Delete Permanently", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Keep Account")
                }
            }
        )
    }
}

private fun isOrderCancellableBeforePacking(status: String): Boolean {
    val norm = status.trim().lowercase()
    return norm == "order placed" || norm == "placed" || norm == "pending" || norm == "confirmed"
}

@Composable
private fun CloudOrderCard(
    ord: FirestoreOrderRecord,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    onReorder: () -> Unit = {},
    onViewInvoice: () -> Unit = {},
    onCancelOrderClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isCancelled = ord.status.contains("cancel", ignoreCase = true)
    val isCancellable = isOrderCancellableBeforePacking(ord.status)
    val stages = listOf("Order Placed", "Packing", "Out for Delivery", "Delivered")
    val activeIdx = when {
        isCancelled -> -1
        ord.status.equals("Delivered", ignoreCase = true) -> 3
        ord.status.contains("Out", ignoreCase = true) -> 2
        ord.status.contains("Pack", ignoreCase = true) -> 1
        else -> 0
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isCancelled) WholesaleRed.copy(alpha = 0.35f) else WarmCreamBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ord.orderId, fontWeight = FontWeight.ExtraBold, color = ForestGreenDark, fontSize = 14.sp)
                Surface(
                    color = if (isCancelled) Color(0xFFFFEBEE) else SignatureGoldContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = ord.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCancelled) WholesaleRed else ForestGreenDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("${ord.items.sumOf { it.qty }} items • Paid ₹${ord.finalTotal}", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 13.sp)
            if (ord.isNriOrder || ord.dispatchSlot.isNotBlank()) {
                Text(
                    text = listOfNotNull(
                        if (ord.isNriOrder) "NRI Punjab Family Order" else null,
                        ord.dispatchSlot.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenDark
                )
            }
            Text(ord.addressSnapshot, fontSize = 11.sp, color = Color(0xFF5A6D63))

            if (!isCancelled) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    stages.forEachIndexed { idx, label ->
                        val reached = idx <= activeIdx
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = label,
                                tint = if (reached) ForestGreen else Color(0xFFD0DAD4),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal,
                                color = if (reached) ForestGreenDark else Color(0xFF7B8C83)
                            )
                        }
                    }
                }
            }

            if (ord.deliveryProofUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = ForestGreenContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Delivery Confirmation Proof Photo (Sandhaur Rider)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestGreenDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(ord.deliveryProofUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Delivery Proof Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewInvoice,
                    border = BorderStroke(1.dp, ForestGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("view_invoice_${ord.orderId}")
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GST Invoice", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                }

                if (!isCancelled && !ord.status.equals("Delivered", ignoreCase = true)) {
                    OutlinedButton(
                        onClick = onCancelOrderClick,
                        enabled = isCancellable,
                        border = BorderStroke(1.dp, if (isCancellable) WholesaleRed else WarmCreamBorder),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("cancel_order_${ord.orderId}")
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isCancellable) WholesaleRed else Color(0xFF9AA89F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCancellable) "Cancel Order" else "Packing Locked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCancellable) WholesaleRed else Color(0xFF9AA89F)
                        )
                    }
                }
            }

            if (ord.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onReorder,
                    border = BorderStroke(1.dp, ForestGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("reorder_cloud_order_${ord.orderId}")
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = appLanguage.tr(
                            english = "Reorder Items",
                            punjabi = "ਦੁਬਾਰਾ ਆਰਡਰ (Reorder In-Stock)",
                            hindi = "फिर से ऑर्डर करें (Reorder)"
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ForestGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalOrderCard(
    order: Order,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    onReorder: () -> Unit,
    onViewInvoice: () -> Unit = {},
    onCancelOrderClick: () -> Unit = {}
) {
    val isCancelled = order.status.contains("cancel", ignoreCase = true)
    val isCancellable = isOrderCancellableBeforePacking(order.status) || order.isTestModeOrder && !isCancelled

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isCancelled) WholesaleRed.copy(alpha = 0.35f) else WarmCreamBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.orderId, fontWeight = FontWeight.ExtraBold, color = ForestGreenDark)
                Text("₹${order.finalTotal}", fontWeight = FontWeight.ExtraBold, color = ForestGreen)
            }
            Text("${order.totalItemsCount} items • ${order.status}", fontSize = 12.sp, color = if (isCancelled) WholesaleRed else Color(0xFF5A6D63))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewInvoice,
                    border = BorderStroke(1.dp, ForestGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp).testTag("view_local_invoice_${order.orderId}")
                ) {
                    Text("Digital Bill / GST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                }
                if (!isCancelled && !order.status.equals("Delivered", ignoreCase = true)) {
                    OutlinedButton(
                        onClick = onCancelOrderClick,
                        enabled = isCancellable,
                        border = BorderStroke(1.dp, if (isCancellable) WholesaleRed else WarmCreamBorder),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(34.dp).testTag("cancel_local_order_${order.orderId}")
                    ) {
                        Text(
                            text = if (isCancellable) "Cancel Order" else "Locked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCancellable) WholesaleRed else Color(0xFF9AA89F)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onReorder,
                border = BorderStroke(1.dp, ForestGreen),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth().height(34.dp)
            ) {
                Text(appLanguage.tr("Reorder Items", "ਦੁਬਾਰਾ ਆਰਡਰ", "फिर से ऑर्डर करें"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
            }
        }
    }
}

/**
 * Shared Delivery Address Editor Dialog used from both AccountScreen and CheckoutDialog.
 */
@Composable
fun AddressEditorDialog(
    initialAddress: SavedAddress?,
    defaultName: String,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        mobile: String,
        street: String,
        landmark: String,
        village: String,
        pin: String,
        makeDefault: Boolean
    ) -> Unit
) {
    var fullName by remember { mutableStateOf(initialAddress?.fullName ?: defaultName) }
    var mobile by remember { mutableStateOf(initialAddress?.mobile.orEmpty()) }
    var street by remember { mutableStateOf(initialAddress?.streetAddress.orEmpty()) }
    var landmark by remember { mutableStateOf(initialAddress?.landmark.orEmpty()) }
    var village by remember { mutableStateOf(initialAddress?.villageOrCity ?: StoreProfile.DEFAULT_CITY_STATE) }
    var pinCode by remember { mutableStateOf(initialAddress?.pinCode ?: StoreProfile.DEFAULT_PIN_CODE) }
    var makeDefault by remember { mutableStateOf(initialAddress?.defaultAddress ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAddress != null) "Edit Delivery Address" else "Add Sandhaur Delivery Address",
                fontWeight = FontWeight.ExtraBold,
                color = ForestGreenDark
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(appLanguage.tr("Full Name", "ਪੂਰਾ ਨਾਮ", "पूरा नाम")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = InputSanitizer.sanitizePhone10(it) },
                    label = { Text("10-Digit Indian Mobile") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("House / Street Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text("Landmark (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    label = { Text("Village / City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pinCode,
                    onValueChange = { pinCode = InputSanitizer.sanitizePin6(it) },
                    label = { Text("6-Digit PIN Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = makeDefault,
                        onCheckedChange = { makeDefault = it },
                        colors = CheckboxDefaults.colors(checkedColor = ForestGreen)
                    )
                    Text("Set as Default Delivery Address", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                if (!validationError.isNullOrBlank()) {
                    Text(validationError!!, color = WholesaleRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = InputSanitizer.sanitizeName(fullName)
                    val cleanMobile = InputSanitizer.sanitizePhone10(mobile)
                    val cleanStreet = InputSanitizer.sanitizeText(street, 200)
                    val cleanPin = InputSanitizer.sanitizePin6(pinCode)
                    when {
                        cleanName.length < 2 -> validationError = "Enter full name"
                        !InputSanitizer.isValidIndianMobile(cleanMobile) -> validationError = "Enter valid 10-digit Indian mobile"
                        cleanStreet.length < 4 -> validationError = "Enter complete house/street address"
                        !InputSanitizer.isValidPin6(cleanPin) -> validationError = "Enter 6-digit PIN code (e.g. 148020)"
                        else -> onSave(
                            cleanName,
                            cleanMobile,
                            cleanStreet,
                            InputSanitizer.sanitizeText(landmark, 120),
                            InputSanitizer.sanitizeText(village, 120).ifBlank { StoreProfile.DEFAULT_CITY_STATE },
                            cleanPin,
                            makeDefault
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Save Address", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
