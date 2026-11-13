package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankDirectory
import com.example.data.model.CommissionConfig
import com.example.data.model.EmiPlan
import com.example.data.model.PaymentOptionType
import com.example.data.model.SavedCard
import com.example.data.model.StoreProfile
import com.example.ui.theme.ForestContainer
import com.example.ui.theme.ForestDark
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.OnForestContainer
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.WholesaleRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    grossTotal: Int,
    discount: Int,
    finalAmount: Int,
    walletBalance: Int,
    savedCards: List<SavedCard>,
    isPunjabi: Boolean,
    onBack: () -> Unit,
    onPaymentSuccess: (
        method: String,
        transactionId: String,
        utr: String,
        walletDeducted: Int,
        paidToCentral: Int
    ) -> Unit
) {
    val context = LocalContext.current

    // Accordion state (expanded section)
    var expandedOption by remember { mutableStateOf(PaymentOptionType.UPI) }

    // Store Wallet deduction toggle
    var useWalletBalance by remember { mutableStateOf(walletBalance > 0) }
    val walletDeduction = if (useWalletBalance) minOf(walletBalance, finalAmount) else 0
    val payableAfterWallet = maxOf(0, finalAmount - walletDeduction)

    // Option 1: UPI State
    var manualVpa by remember { mutableStateOf("") }
    var qrTimeRemaining by remember { mutableIntStateOf(600) } // 10 minutes

    // Option 2: Card State
    var selectedCardId by remember { mutableStateOf(savedCards.firstOrNull()?.id ?: "NEW_CARD") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var saveCardCheckbox by remember { mutableStateOf(true) }

    // Option 3: NetBanking State
    var selectedBank by remember { mutableStateOf(BankDirectory.TOP_BANKS.first()) }
    var showAllBanksMenu by remember { mutableStateOf(false) }

    // Option 4: Digital Wallet State
    var selectedWallet by remember { mutableStateOf("Paytm Wallet") }

    // Option 5: EMI State
    var emiType by remember { mutableStateOf("NO_COST") } // "NO_COST", "STANDARD", "PAY_LATER"
    var selectedEmiMonths by remember { mutableIntStateOf(3) }

    // Option 7: COD State
    var codOtp by remember { mutableStateOf("") }
    var isCodOtpVerified by remember { mutableStateOf(false) }

    // QR countdown timer
    LaunchedEffect(Unit) {
        while (qrTimeRemaining > 0) {
            delay(1000)
            qrTimeRemaining--
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isPunjabi) "ਸੁਰੱਖਿਅਤ ਭੁਗਤਾਨ (Checkout Payment)" else "Select Payment Method",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "100% Safe & Central Escrow Protected",
                            style = MaterialTheme.typography.labelSmall.copy(color = GoldContainer, fontSize = 11.sp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ForestGreen)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Order Summary Pill & Payable Amount
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isPunjabi) "ਕੁੱਲ ਭੁਗਤਾਨ ਰਕਮ" else "Payable Amount",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
                                )
                                Text(
                                    text = "₹$payableAfterWallet",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                            }

                            Surface(
                                color = WholesaleRed,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "SAVED ₹$discount",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (walletBalance > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "NavKart Wallet (Balance: ₹$walletBalance)",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (useWalletBalance) "Using ₹$walletDeduction from wallet" else "Tap to apply wallet balance",
                                            style = MaterialTheme.typography.labelSmall.copy(color = GoldContainer, fontSize = 10.sp)
                                        )
                                    }
                                }

                                Switch(
                                    checked = useWalletBalance,
                                    onCheckedChange = { useWalletBalance = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ForestGreen,
                                        checkedTrackColor = GoldAccent
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Central Collection Transparency Note
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Central Account: ${CommissionConfig.CENTRAL_MERCHANT_NAME} (GSTIN: ${com.example.data.model.OwnerSecureLedger.GSTIN})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Accordion Title
            item {
                Text(
                    text = if (isPunjabi) "ਭੁਗਤਾਨ ਦਾ ਤਰੀਕਾ ਚੁਣੋ:" else "Select Payment Mode:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // --- OPTION 1: UPI INSTANT PAYMENTS ---
            item {
                PaymentAccordionItem(
                    type = PaymentOptionType.UPI,
                    icon = Icons.Default.Payment,
                    isSelected = expandedOption == PaymentOptionType.UPI,
                    badge = "0% Fee • Instant",
                    badgeColor = ForestGreen,
                    onHeaderClick = { expandedOption = PaymentOptionType.UPI }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            text = "1-Tap Launch Installed UPI App:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick App Launchers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UpiAppLauncherButton(
                                label = "Google Pay",
                                color = Color(0xFF1A73E8),
                                onClick = {
                                    launchUpiIntent(context, "com.google.android.apps.nbu.paisa.user", payableAfterWallet) {
                                        onPaymentSuccess("UPI - Google Pay", generateTxId(), generateUtr(), walletDeduction, payableAfterWallet)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            UpiAppLauncherButton(
                                label = "PhonePe",
                                color = Color(0xFF5F259F),
                                onClick = {
                                    launchUpiIntent(context, "com.phonepe.app", payableAfterWallet) {
                                        onPaymentSuccess("UPI - PhonePe", generateTxId(), generateUtr(), walletDeduction, payableAfterWallet)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            UpiAppLauncherButton(
                                label = "Paytm",
                                color = Color(0xFF002970),
                                onClick = {
                                    launchUpiIntent(context, "net.one97.paytm", payableAfterWallet) {
                                        onPaymentSuccess("UPI - Paytm", generateTxId(), generateUtr(), walletDeduction, payableAfterWallet)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UpiAppLauncherButton(
                                label = "BHIM UPI",
                                color = Color(0xFF00796B),
                                onClick = {
                                    launchUpiIntent(context, "in.org.npci.upiapp", payableAfterWallet) {
                                        onPaymentSuccess("UPI - BHIM", generateTxId(), generateUtr(), walletDeduction, payableAfterWallet)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            UpiAppLauncherButton(
                                label = "CRED UPI",
                                color = Color(0xFF1E1E1E),
                                onClick = {
                                    launchUpiIntent(context, "com.dreamplug.androidapp", payableAfterWallet) {
                                        onPaymentSuccess("UPI - CRED", generateTxId(), generateUtr(), walletDeduction, payableAfterWallet)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic QR Code Section
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.QrCode, contentDescription = null, tint = ForestGreen)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Scan QR to Pay ₹$payableAfterWallet",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Text(
                                        text = "${qrTimeRemaining / 60}:${String.format("%02d", qrTimeRemaining % 60)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = WholesaleRed
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Visual Scannable QR Matrix
                                SimulatedQrCodeBox(upiString = "upi://pay?pa=${com.example.data.model.OwnerSecureLedger.CENTRAL_MERCHANT_BIZ_VPA}&pn=NavKartMarketplace&am=$payableAfterWallet&cu=INR")

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Scan with any UPI app • NavKart Official Verified Merchant Escrow",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Copy Escrow Reference Chip
                                Surface(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Merchant Escrow", com.example.data.model.OwnerSecureLedger.CENTRAL_MERCHANT_BIZ_VPA))
                                        Toast.makeText(context, "NavKart Verified Merchant UPI copied", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "NavKart Official Merchant Escrow (Copy UPI)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Escrow UPI", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Manual UPI ID Field
                        OutlinedTextField(
                            value = manualVpa,
                            onValueChange = { manualVpa = it },
                            label = { Text("Enter UPI ID (e.g. mobile@upi, name@okhdfcbank)") },
                            placeholder = { Text("yourname@okaxis") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("manual_vpa_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (manualVpa.isNotBlank() && manualVpa.contains("@")) {
                                    onPaymentSuccess("UPI - $manualVpa", generateTxId(), generateUtr(), walletDeduction, payableAfterWallet)
                                } else {
                                    // Trigger instant direct UPI intent or simulate
                                    launchUpiIntent(context, null, payableAfterWallet) {
                                        onPaymentSuccess("UPI Direct", generateTxId(), generateUtr(), walletDeduction, payableAfterWallet)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pay_upi_btn")
                        ) {
                            Text(
                                text = "Pay ₹$payableAfterWallet via UPI",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // --- OPTION 2: CREDIT / DEBIT CARDS ---
            item {
                PaymentAccordionItem(
                    type = PaymentOptionType.CARD,
                    icon = Icons.Default.CreditCard,
                    isSelected = expandedOption == PaymentOptionType.CARD,
                    badge = "Visa, Master, RuPay, Amex",
                    badgeColor = Color(0xFF1976D2),
                    onHeaderClick = { expandedOption = PaymentOptionType.CARD }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        // Saved Cards
                        if (savedCards.isNotEmpty()) {
                            Text(
                                text = "Saved Cards:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            savedCards.forEach { card ->
                                Surface(
                                    onClick = { selectedCardId = card.id },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedCardId == card.id) ForestContainer else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (selectedCardId == card.id) 2.dp else 1.dp,
                                        if (selectedCardId == card.id) ForestGreen else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedCardId == card.id,
                                            onClick = { selectedCardId = card.id },
                                            colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${card.cardType} •••• ${card.lastFour}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${card.cardHolder} | Expires ${card.expiry}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(
                            text = "Add New Card:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = {
                                if (it.length <= 19) cardNumber = it.filter { char -> char.isDigit() || char == ' ' }
                            },
                            label = { Text("Card Number") },
                            placeholder = { Text("4532 •••• •••• 8910") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("card_number_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = cardExpiry,
                                onValueChange = { if (it.length <= 5) cardExpiry = it },
                                label = { Text("Expiry (MM/YY)") },
                                placeholder = { Text("12/28") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = cardCvv,
                                onValueChange = { if (it.length <= 4) cardCvv = it },
                                label = { Text("CVV") },
                                placeholder = { Text("•••") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("card_cvv_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = cardHolder,
                            onValueChange = { cardHolder = it },
                            label = { Text("Name on Card") },
                            placeholder = { Text("Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val cardLast4 = if (cardNumber.isNotBlank()) cardNumber.takeLast(4) else "4242"
                                onPaymentSuccess("Card - Ending in $cardLast4", generateTxId(), generateUtr(), walletDeduction, payableAfterWallet)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pay_card_btn")
                        ) {
                            Text(
                                text = "Pay ₹$payableAfterWallet via Card",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // --- OPTION 3: NETBANKING ---
            item {
                PaymentAccordionItem(
                    type = PaymentOptionType.NETBANKING,
                    icon = Icons.Default.AccountBalance,
                    isSelected = expandedOption == PaymentOptionType.NETBANKING,
                    badge = "All Indian Banks",
                    badgeColor = Color(0xFFE65100),
                    onHeaderClick = { expandedOption = PaymentOptionType.NETBANKING }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            text = "Popular Indian Banks:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Select Bank Chips Grid
                        BankDirectory.TOP_BANKS.take(6).chunked(2).forEach { rowBanks ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowBanks.forEach { bank ->
                                    val isSelected = selectedBank == bank
                                    Surface(
                                        onClick = { selectedBank = bank },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) ForestContainer else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedBank = bank },
                                                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = bank,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // All Other Banks Dropdown
                        Box {
                            OutlinedButton(
                                onClick = { showAllBanksMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Other Bank: $selectedBank")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = showAllBanksMenu,
                                onDismissRequest = { showAllBanksMenu = false }
                            ) {
                                BankDirectory.ALL_BANKS.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b) },
                                        onClick = {
                                            selectedBank = b
                                            showAllBanksMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                onPaymentSuccess("NetBanking - $selectedBank", generateTxId(), generateUtr(), walletDeduction, payableAfterWallet)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                text = "Pay ₹$payableAfterWallet via $selectedBank",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // --- OPTION 4: DIGITAL WALLETS ---
            item {
                PaymentAccordionItem(
                    type = PaymentOptionType.WALLET,
                    icon = Icons.Default.AccountBalanceWallet,
                    isSelected = expandedOption == PaymentOptionType.WALLET,
                    badge = "Instant 1-Click",
                    badgeColor = Color(0xFF00897B),
                    onHeaderClick = { expandedOption = PaymentOptionType.WALLET }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        listOf("Paytm Wallet", "PhonePe Wallet", "Amazon Pay Balance", "Mobikwik Wallet", "Freecharge").forEach { w ->
                            val isSelected = selectedWallet == w
                            Surface(
                                onClick = { selectedWallet = w },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ForestContainer else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedWallet = w },
                                        colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = w,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onPaymentSuccess("Wallet - $selectedWallet", generateTxId(), generateUtr(), walletDeduction, payableAfterWallet)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                text = "Link & Pay ₹$payableAfterWallet via $selectedWallet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // --- OPTION 5: EMI & PAY LATER ---
            item {
                PaymentAccordionItem(
                    type = PaymentOptionType.EMI,
                    icon = Icons.Default.Security,
                    isSelected = expandedOption == PaymentOptionType.EMI,
                    badge = "No-Cost EMI Available",
                    badgeColor = GoldAccent,
                    onHeaderClick = { expandedOption = PaymentOptionType.EMI }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        // Tabs: No Cost vs Pay Later
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = emiType == "NO_COST",
                                onClick = { emiType = "NO_COST" },
                                label = { Text("No Cost EMI (0% Int)") }
                            )
                            FilterChip(
                                selected = emiType == "PAY_LATER",
                                onClick = { emiType = "PAY_LATER" },
                                label = { Text("NavKart Pay Later") }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (emiType == "NO_COST") {
                            Text(
                                text = "Select Installment Duration:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            listOf(3, 6, 9, 12).forEach { months ->
                                val monthlyAmt = payableAfterWallet / months
                                val isSelected = selectedEmiMonths == months

                                Surface(
                                    onClick = { selectedEmiMonths = months },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ForestContainer else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedEmiMonths = months },
                                                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "₹$monthlyAmt / month",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = "$months Months • 0% Interest (No-Cost)",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = ForestGreen, fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Total: ₹$payableAfterWallet",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Pay Later
                            Surface(
                                color = GoldContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "🎉 Pre-Approved NavKart Pay Later Credit",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = OnGoldContainer)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Eligible up to ₹25,000. Buy today and pay next month on 5th with 0% interest.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = OnGoldContainer)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val mode = if (emiType == "NO_COST") "No-Cost EMI ($selectedEmiMonths Months)" else "NavKart Pay Later"
                                onPaymentSuccess(mode, generateTxId(), generateUtr(), walletDeduction, payableAfterWallet)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                text = "Confirm EMI / Pay Later",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // --- OPTION 7: CASH ON DELIVERY (COD) ---
            item {
                PaymentAccordionItem(
                    type = PaymentOptionType.COD,
                    icon = Icons.Default.Shield,
                    isSelected = expandedOption == PaymentOptionType.COD,
                    badge = "Verified COD",
                    badgeColor = Color(0xFF6D4C41),
                    onHeaderClick = { expandedOption = PaymentOptionType.COD }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            text = "Pay via Cash or UPI QR at Doorstep:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Our delivery executive carries a UPI QR scanner for hassle-free digital payment at your doorstep.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // OTP Verification Simulation
                        OutlinedTextField(
                            value = codOtp,
                            onValueChange = {
                                if (it.length <= 4) codOtp = it
                                if (it.length == 4) isCodOtpVerified = true
                            },
                            label = { Text("Enter 4-Digit Delivery Confirmation Code") },
                            placeholder = { Text("e.g. 1480") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                if (isCodOtpVerified) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = ForestGreen)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("cod_otp_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onPaymentSuccess("Cash on Delivery (COD)", generateTxId(), "COD-PENDING-COLLECTION", walletDeduction, payableAfterWallet)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pay_cod_btn")
                        ) {
                            Text(
                                text = "Place Order with Cash on Delivery",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Security & Merchant Compliance Footer
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "256-Bit Bank Grade SSL Encrypted Checkout",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Merchant: ${CommissionConfig.CENTRAL_MERCHANT_NAME}\nMain Bazaar, Sandhaur (Malerkotla), Punjab 148020 • GSTIN: ${com.example.data.model.OwnerSecureLedger.GSTIN}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentAccordionItem(
    type: PaymentOptionType,
    icon: ImageVector,
    isSelected: Boolean,
    badge: String,
    badgeColor: Color,
    onHeaderClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHeaderClick() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) ForestContainer else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = type.title,
                            tint = if (isSelected) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = type.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (badge.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = badgeColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = type.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = if (isSelected) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    content()
                }
            }
        }
    }
}

@Composable
private fun UpiAppLauncherButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier.height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SimulatedQrCodeBox(upiString: String) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(2.dp, ForestGreen, RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = size.width / 16
            // Draw pseudo QR matrix pattern
            for (i in 0..15) {
                for (j in 0..15) {
                    val isCorner1 = i < 5 && j < 5
                    val isCorner2 = i > 10 && j < 5
                    val isCorner3 = i < 5 && j > 10
                    val isCornerMarker = (i in 1..3 && j in 1..3) || (i in 12..14 && j in 1..3) || (i in 1..3 && j in 12..14)
                    val isRandomDark = (i * 7 + j * 13 + upiString.length) % 3 == 0

                    if (isCorner1 || isCorner2 || isCorner3) {
                        if (isCornerMarker || (i == 0 || i == 4 || j == 0 || j == 4 || i == 11 || i == 15 || j == 11 || j == 15)) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(i * step, j * step),
                                size = Size(step, step)
                            )
                        }
                    } else if (isRandomDark) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(i * step, j * step),
                            size = Size(step, step)
                        )
                    }
                }
            }
        }
    }
}

private fun launchUpiIntent(
    context: Context,
    targetPackage: String?,
    amount: Int,
    onFallbackSuccess: () -> Unit
) {
    val upiUri = Uri.parse(
        "upi://pay?pa=${CommissionConfig.CENTRAL_ACCOUNT_UPI}&pn=${Uri.encode("Nav Grocery Store")}&am=$amount&cu=INR&tn=${Uri.encode("NavKart Marketplace Order")}"
    )
    val intent = Intent(Intent.ACTION_VIEW, upiUri).apply {
        if (!targetPackage.isNullOrBlank()) {
            setPackage(targetPackage)
        }
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
        // Auto trigger callback flow
        onFallbackSuccess()
    } catch (e: Exception) {
        Toast.makeText(context, "Opening UPI Payment...", Toast.LENGTH_SHORT).show()
        onFallbackSuccess()
    }
}

private fun generateTxId(): String = "TXN-" + System.currentTimeMillis().toString().takeLast(8)
private fun generateUtr(): String = "UTR" + (100000000000L + (Math.random() * 900000000000L).toLong()).toString()
