package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.SellerRegistration
import com.example.data.model.StoreProfile
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.OnForestGreenContainer
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer
import com.example.ui.theme.WholesaleRed

@Composable
fun AdminPanelScreen(
    isTestModeActive: Boolean,
    orders: List<Order>,
    products: List<Product>,
    sellers: List<SellerRegistration>,
    isPunjabi: Boolean,
    onToggleTestMode: (Boolean) -> Unit,
    onUpdateProductPrice: (String, Int) -> Unit,
    onUpdateProductDescription: (String, String) -> Unit,
    onApproveSeller: (SellerRegistration) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var selectedAdminTab by remember { mutableIntStateOf(0) } // 0: Dashboard & Test Mode, 1: Orders, 2: Inventory, 3: Sellers

    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var editPriceText by remember { mutableStateOf("") }
    var editDescText by remember { mutableStateOf("") }

    if (!isUnlocked) {
        // PIN Prompt Screen (148020)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(ForestGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Super Admin Security",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(
                        text = "Enter 6-digit authorized store PIN to access tablet test mode, orders & shopkeeper controls",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 6) {
                                enteredPin = it
                                pinError = false
                            }
                        },
                        label = { Text("Super Admin PIN") },
                        placeholder = { Text("148020") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinError,
                        supportingText = if (pinError) {
                            { Text("Incorrect PIN. Please enter authorized 148020", color = WholesaleRed) }
                        } else null,
                        modifier = Modifier.fillMaxWidth().testTag("admin_pin_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (enteredPin == com.example.data.model.OwnerSecureLedger.SUPER_ADMIN_PIN) {
                                isUnlocked = true
                            } else {
                                pinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_unlock_btn")
                    ) {
                        Text("Unlock Admin Controls")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onBackClick) {
                        Text("Return to Store")
                    }
                }
            }
        }
        return
    }

    // Unlocked Super Admin Panel
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            color = ForestGreen,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "NavKart Super Admin",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "PIN 148020 • Sandhaur Main Hub",
                            style = MaterialTheme.typography.labelSmall.copy(color = SignatureGold)
                        )
                    }
                }

                // Lock button
                IconButton(onClick = { isUnlocked = false }) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White)
                }
            }
        }

        // Test Mode Banner
        Surface(
            color = if (isTestModeActive) SignatureGoldContainer else ForestGreenContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isTestModeActive) Icons.Default.BugReport else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isTestModeActive) Color(0xFF8A5B00) else ForestGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isTestModeActive) "TABLET TEST MODE ACTIVE" else "LIVE PRODUCTION MODE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isTestModeActive) Color(0xFF523600) else OnForestGreenContainer
                            )
                        )
                        Text(
                            text = if (isTestModeActive)
                                "Sandbox UPI, mock payments & test order WhatsApp alerts enabled"
                            else
                                "Real orders routed to Amrit Pal Singh Uppal (+91 8847370905)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = if (isTestModeActive) Color(0xFF6B4700) else OnForestGreenContainer.copy(alpha = 0.8f)
                            )
                        )
                    }
                }

                Switch(
                    checked = isTestModeActive,
                    onCheckedChange = { onToggleTestMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SignatureGold,
                        checkedTrackColor = Color(0xFF6B4700)
                    ),
                    modifier = Modifier.testTag("admin_test_mode_switch")
                )
            }
        }

        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedAdminTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = ForestGreen
        ) {
            Tab(
                selected = selectedAdminTab == 0,
                onClick = { selectedAdminTab = 0 },
                text = { Text("Overview", style = MaterialTheme.typography.labelSmall) }
            )
            Tab(
                selected = selectedAdminTab == 1,
                onClick = { selectedAdminTab = 1 },
                text = { Text("Orders (${orders.size})", style = MaterialTheme.typography.labelSmall) }
            )
            Tab(
                selected = selectedAdminTab == 2,
                onClick = { selectedAdminTab = 2 },
                text = { Text("Inventory (308)", style = MaterialTheme.typography.labelSmall) }
            )
            Tab(
                selected = selectedAdminTab == 3,
                onClick = { selectedAdminTab = 3 },
                text = { Text("Sellers (${sellers.size})", style = MaterialTheme.typography.labelSmall) }
            )
        }

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            when (selectedAdminTab) {
                0 -> AdminOverviewSection(
                    orders = orders,
                    products = products,
                    sellers = sellers,
                    isTestModeActive = isTestModeActive
                )
                1 -> AdminOrdersSection(orders = orders)
                2 -> AdminInventorySection(
                    products = products,
                    onEditProduct = { prod ->
                        editingProduct = prod
                        editPriceText = prod.mrp.toString()
                        editDescText = prod.displayDescription
                    }
                )
                3 -> AdminSellersSection(
                    sellers = sellers,
                    onApprove = onApproveSeller
                )
            }
        }
    }

    // Edit Product Dialog
    if (editingProduct != null) {
        val prod = editingProduct!!
        AlertDialog(
            onDismissRequest = { editingProduct = null },
            title = { Text("Edit Item: ${prod.name}", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Text("Wholesale 12% rule applies automatically to new MRP.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editPriceText,
                        onValueChange = { editPriceText = it },
                        label = { Text("MRP Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editDescText,
                        onValueChange = { editDescText = it },
                        label = { Text("Description & Packaging Specs") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newMrp = editPriceText.toIntOrNull() ?: prod.mrp
                        onUpdateProductPrice(prod.id, newMrp)
                        if (editDescText.isNotBlank()) {
                            onUpdateProductDescription(prod.id, editDescText)
                        }
                        editingProduct = null
                        Toast.makeText(context, "Item updated in Room database", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProduct = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminOverviewSection(
    orders: List<Order>,
    products: List<Product>,
    sellers: List<SellerRegistration>,
    isTestModeActive: Boolean
) {
    Column {
        Text("Admin System Metrics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Total Orders", "${orders.size}", ForestGreen, Modifier.weight(1f))
            StatCard("Catalog SKUs", "${products.size}", SignatureGold, Modifier.weight(1f))
            StatCard("Registered Sellers", "${sellers.size}", Color(0xFF1976D2), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Central Merchant Account", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                Text("Owner: ${com.example.data.model.OwnerSecureLedger.OWNER_LEGAL_NAME}")
                Text("Primary UPI VPA: ${com.example.data.model.OwnerSecureLedger.CENTRAL_MERCHANT_VPA}")
                Text("Secondary VPA: ${com.example.data.model.OwnerSecureLedger.CENTRAL_MERCHANT_SECONDARY_VPA}")
                Text("GSTIN: ${com.example.data.model.OwnerSecureLedger.GSTIN}")
                Text("Helpline / WhatsApp: ${StoreProfile.HELPLINE_PHONE}")
                Text("Cloud DB: nav-grocery-store-default-rtdb.firebaseio.com")
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.SemiBold))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = color))
        }
    }
}

@Composable
private fun AdminOrdersSection(orders: List<Order>) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No orders recorded yet. Place a test order from Catalog!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(orders) { order ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(order.orderId, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ForestGreen))
                        if (order.isTestModeOrder) {
                            Surface(color = SignatureGoldContainer, shape = RoundedCornerShape(4.dp)) {
                                Text("SANDBOX TEST", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                    Text("Customer: ${order.customerName} (${order.customerPhone})", style = MaterialTheme.typography.bodySmall)
                    Text("Payment: ${order.paymentMethod} • ₹${order.finalTotal} (Saved ₹${order.totalDiscount})", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    Text("Delivery: ${order.address}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }
    }
}

@Composable
private fun AdminInventorySection(
    products: List<Product>,
    onEditProduct: (Product) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(products.take(50)) { prod ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prod.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                        Text("MRP: ₹${prod.mrp} | Wholesale: ₹${prod.wholesalePrice} (12% OFF) | Stock: ${prod.stock}", style = MaterialTheme.typography.labelSmall.copy(color = ForestGreen))
                        Text(prod.displayDescription, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 1)
                    }
                    IconButton(onClick = { onEditProduct(prod) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ForestGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSellersSection(
    sellers: List<SellerRegistration>,
    onApprove: (SellerRegistration) -> Unit
) {
    if (sellers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No shopkeeper registrations yet. Sellers can register via 'Sell on NavKart'.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sellers) { seller ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(seller.shopName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Surface(
                            color = if (seller.isApproved) ForestGreenContainer else SignatureGoldContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (seller.isApproved) "VERIFIED" else "PENDING",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Text("Owner: ${seller.ownerName} • Phone: ${seller.phone}", style = MaterialTheme.typography.bodySmall)
                    Text("Category: ${seller.businessCategory} • Location: ${seller.villageOrAddress}", style = MaterialTheme.typography.bodySmall)

                    if (!seller.isApproved) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onApprove(seller) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Approve Seller", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
