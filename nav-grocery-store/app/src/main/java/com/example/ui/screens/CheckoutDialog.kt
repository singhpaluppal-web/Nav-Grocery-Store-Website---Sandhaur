package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryOption
import com.example.data.model.PaymentMethod
import com.example.data.model.StoreProfile
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.OnForestGreenContainer
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer
import com.example.ui.theme.WholesaleRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSheet(
    totalPayable: Int,
    totalSavings: Int,
    isPunjabi: Boolean,
    isTestModeActive: Boolean,
    onDismiss: () -> Unit,
    onConfirmOrder: (name: String, phone: String, option: DeliveryOption, address: String, notes: String, payment: PaymentMethod) -> Unit,
    onCallHelpline: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("Sandhaur, Punjab") }
    var notes by remember { mutableStateOf("") }
    var selectedDeliveryOption by remember { mutableStateOf(DeliveryOption.HOME_DELIVERY) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.UPI_INTENT) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isTablet = maxWidth >= 600.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 960.dp)
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPunjabi) "ਆਰਡਰ ਅਤੇ ਭੁਗਤਾਨ ਪੁਸ਼ਟੀ" else "Order & Payment Confirmation",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
                        )
                        Text(
                            text = "NavKart Official Verified Merchant Escrow Checkout",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Test Mode Indicator
                if (isTestModeActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = SignatureGoldContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ TABLET TEST MODE (SANDBOX): Real charges disabled for verification.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF523600),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isTablet) {
                    // Responsive Two-Column Split Screen for Tablets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Column: Unclipped Payable Summary + Delivery Method + Customer Info
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 24.dp)
                        ) {
                            CheckoutSummaryAndDeliveryColumn(
                                totalPayable = totalPayable,
                                totalSavings = totalSavings,
                                isPunjabi = isPunjabi,
                                selectedDeliveryOption = selectedDeliveryOption,
                                onDeliveryOptionChange = { selectedDeliveryOption = it },
                                customerName = customerName,
                                onCustomerNameChange = { customerName = it },
                                customerPhone = customerPhone,
                                onCustomerPhoneChange = { customerPhone = it },
                                address = address,
                                onAddressChange = { address = it },
                                notes = notes,
                                onNotesChange = { notes = it }
                            )
                        }

                        // Right Column: Scrollable Payment Options + Order Dispatch Buttons
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 24.dp)
                        ) {
                            CheckoutPaymentAndConfirmColumn(
                                totalPayable = totalPayable,
                                isPunjabi = isPunjabi,
                                isTestModeActive = isTestModeActive,
                                selectedPaymentMethod = selectedPaymentMethod,
                                onPaymentMethodChange = { selectedPaymentMethod = it },
                                onConfirmOrder = {
                                    onConfirmOrder(customerName, customerPhone, selectedDeliveryOption, address, notes, selectedPaymentMethod)
                                },
                                onCallHelpline = onCallHelpline
                            )
                        }
                    }
                } else {
                    // Single-Column Mobile Scroll
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    ) {
                        CheckoutSummaryAndDeliveryColumn(
                            totalPayable = totalPayable,
                            totalSavings = totalSavings,
                            isPunjabi = isPunjabi,
                            selectedDeliveryOption = selectedDeliveryOption,
                            onDeliveryOptionChange = { selectedDeliveryOption = it },
                            customerName = customerName,
                            onCustomerNameChange = { customerName = it },
                            customerPhone = customerPhone,
                            onCustomerPhoneChange = { customerPhone = it },
                            address = address,
                            onAddressChange = { address = it },
                            notes = notes,
                            onNotesChange = { notes = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CheckoutPaymentAndConfirmColumn(
                            totalPayable = totalPayable,
                            isPunjabi = isPunjabi,
                            isTestModeActive = isTestModeActive,
                            selectedPaymentMethod = selectedPaymentMethod,
                            onPaymentMethodChange = { selectedPaymentMethod = it },
                            onConfirmOrder = {
                                onConfirmOrder(customerName, customerPhone, selectedDeliveryOption, address, notes, selectedPaymentMethod)
                            },
                            onCallHelpline = onCallHelpline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutSummaryAndDeliveryColumn(
    totalPayable: Int,
    totalSavings: Int,
    isPunjabi: Boolean,
    selectedDeliveryOption: DeliveryOption,
    onDeliveryOptionChange: (DeliveryOption) -> Unit,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    customerPhone: String,
    onCustomerPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    // Total Summary Pill
    Surface(
        color = ForestGreenContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isPunjabi) "ਕੁੱਲ ਦੇਣਯੋਗ ਰਕਮ" else "Payable Amount",
                    style = MaterialTheme.typography.labelSmall.copy(color = OnForestGreenContainer)
                )
                Text(
                    text = "₹$totalPayable",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = OnForestGreenContainer
                    )
                )
            }

            Surface(
                color = SignatureGold,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isPunjabi) "੧੨% ਥੋਕ ਬੱਚਤ: ₹$totalSavings" else "Wholesale Savings: ₹$totalSavings",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF332000)
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Delivery Option Tabs
    Text(
        text = if (isPunjabi) "ਡਿਲਿਵਰੀ ਦੀ ਚੋਣ ਕਰੋ:" else "Delivery Method:",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DeliveryCard(
            icon = Icons.Default.Home,
            title = if (isPunjabi) "ਘਰੇ ਡਿਲਿਵਰੀ" else "Home Delivery",
            subtitle = if (isPunjabi) "~੨ ਘੰਟੇ (ਸੰਧੌੜ ਤੇ ਪਿੰਡ)" else "~2 hrs in Sandhaur",
            isSelected = selectedDeliveryOption == DeliveryOption.HOME_DELIVERY,
            onClick = { onDeliveryOptionChange(DeliveryOption.HOME_DELIVERY) },
            modifier = Modifier.weight(1f)
        )

        DeliveryCard(
            icon = Icons.Default.Store,
            title = if (isPunjabi) "ਸਟੋਰ ਪਿਕਅੱਪ" else "Store Pickup",
            subtitle = if (isPunjabi) "੩੦ ਮਿੰਟ (ਮੇਨ ਬਾਜ਼ਾਰ)" else "30 mins at Main Bazaar",
            isSelected = selectedDeliveryOption == DeliveryOption.STORE_PICKUP,
            onClick = { onDeliveryOptionChange(DeliveryOption.STORE_PICKUP) },
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Customer Name
    OutlinedTextField(
        value = customerName,
        onValueChange = onCustomerNameChange,
        label = { Text(if (isPunjabi) "ਤੁਹਾਡਾ ਨਾਮ (Customer Name)" else "Your Name") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("checkout_name_input")
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Customer Phone
    OutlinedTextField(
        value = customerPhone,
        onValueChange = onCustomerPhoneChange,
        label = { Text(if (isPunjabi) "ਫ਼ੋਨ ਨੰਬਰ (Mobile Number)" else "Phone Number") },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input")
    )

    // Address if Home Delivery
    if (selectedDeliveryOption == DeliveryOption.HOME_DELIVERY) {
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text(if (isPunjabi) "ਪਤਾ / ਪਿੰਡ (Delivery Address / Village)" else "Address / Village in Sandhaur") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("checkout_address_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isPunjabi) "ਨੇੜਲੇ ਪਿੰਡ ਚੁਣੋ:" else "Select nearby village:",
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StoreProfile.NEARBY_VILLAGES.forEach { village ->
                FilterChip(
                    selected = address.contains(village.split(" ")[0]),
                    onClick = {
                        onAddressChange("$village, Sandhaur, Malerkotla")
                    },
                    label = { Text(village, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Additional Notes
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text(if (isPunjabi) "ਕੋਈ ਖਾਸ ਹਦਾਇਤ (Landmark Note)" else "Landmark / Special Instructions (Optional)") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CheckoutPaymentAndConfirmColumn(
    totalPayable: Int,
    isPunjabi: Boolean,
    isTestModeActive: Boolean,
    selectedPaymentMethod: PaymentMethod,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onConfirmOrder: () -> Unit,
    onCallHelpline: () -> Unit
) {
    val context = LocalContext.current

    // Payment Options Section
    Text(
        text = if (isPunjabi) "ਭੁਗਤਾਨ ਦਾ ਢੰਗ (Select Payment Method):" else "Payment Options (100% Secure):",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
    )

    Text(
        text = "100% Encrypted Central Escrow Checkout",
        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    )

    Spacer(modifier = Modifier.height(8.dp))

    PaymentMethod.values().forEach { method ->
        PaymentOptionRow(
            method = method,
            isSelected = selectedPaymentMethod == method,
            onClick = { onPaymentMethodChange(method) }
        )
        Spacer(modifier = Modifier.height(6.dp))
    }

    // If UPI selected, give 1-tap Launch UPI button
    if (selectedPaymentMethod == PaymentMethod.UPI_INTENT && !isTestModeActive) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val upiUri = Uri.parse(
                    "upi://pay?pa=${com.example.data.model.OwnerSecureLedger.CENTRAL_MERCHANT_VPA}&pn=NavKart%20Marketplace&am=$totalPayable&cu=INR&tn=NavKart%20Order"
                )
                val intent = Intent(Intent.ACTION_VIEW, upiUri)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No UPI app found. Please open GPay/PhonePe or pay on WhatsApp", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = ForestGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pay ₹$totalPayable via GPay / PhonePe / Paytm", color = ForestGreen, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Primary Dispatch: Place Order
    Button(
        onClick = onConfirmOrder,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("submit_whatsapp_order_btn")
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = null,
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (isTestModeActive)
                "Run Tablet Test Order (Sandbox)"
            else
                "Place Order on WhatsApp (+91 8847370905)",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Secondary Helpline Dial
    OutlinedButton(
        onClick = onCallHelpline,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Call,
            contentDescription = null,
            tint = ForestGreen
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isPunjabi) "ਸਟੋਰ ਹੈਲਪਲਾਈਨ ਨੂੰ ਫ਼ੋਨ ਕਰੋ (+91 8847370905)"
                   else "Call Store Helpline (+91 8847370905)",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = ForestGreen
            )
        )
    }
}

@Composable
private fun PaymentOptionRow(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) ForestGreenContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = method.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    )
                    if (method.badge.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = SignatureGold,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = method.badge,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                Text(
                    text = method.description,
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DeliveryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) ForestGreenContainer else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) OnForestGreenContainer else MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isSelected) OnForestGreenContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
        }
    }
}
