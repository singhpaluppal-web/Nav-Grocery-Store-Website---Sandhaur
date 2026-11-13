package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommissionConfig
import com.example.data.model.MarketplaceDepartment
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.Seller
import com.example.ui.theme.ForestContainer
import com.example.ui.theme.ForestDark
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.OnForestContainer
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.WholesaleRed

@Composable
fun SellerDashboardScreen(
    currentSeller: Seller?,
    allSellers: List<Seller>,
    sellerProducts: List<Product>,
    orders: List<Order>,
    isPunjabi: Boolean,
    onRegisterSeller: (Seller) -> Unit,
    onSelectSeller: (Seller) -> Unit,
    onAddNewProduct: (Product) -> Unit,
    onUpdateOrderStatus: (String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Orders Queue, 1: Products, 2: Wallet & Escrow
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    val activeSeller = currentSeller ?: allSellers.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Seller Profile & Switcher Header
        Card(
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeSeller?.shopName ?: "Seller Hub",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Merchant",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "${activeSeller?.ownerName} • ${activeSeller?.categorySpecialization}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                        )
                    }

                    OutlinedButton(
                        onClick = { showRegisterDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
                    ) {
                        Text("+ New Seller", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wallet Quick Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SellerStatTile(
                        label = "Gross Sales",
                        value = "₹${activeSeller?.grossSales ?: 0}",
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    SellerStatTile(
                        label = "Escrow Pending",
                        value = "₹${activeSeller?.pendingEscrow ?: 0}",
                        color = GoldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    SellerStatTile(
                        label = "Settled Payouts",
                        value = "₹${activeSeller?.settledPayouts ?: 0}",
                        color = Color(0xFF81C784),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = ForestGreen
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Orders Queue (${orders.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Catalog (${sellerProducts.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Wallet & Escrow", fontWeight = FontWeight.Bold) }
            )
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Orders Fulfillment Queue
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (orders.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No incoming orders yet. Share your shop link!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(orders, key = { it.orderId }) { order ->
                            SellerOrderCard(
                                order = order,
                                onUpdateStatus = onUpdateOrderStatus
                            )
                        }
                    }
                }
            }
            1 -> {
                // Catalog Manager
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Products in Your Shop",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Button(
                            onClick = { showAddProductDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Product")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sellerProducts, key = { it.id }) { product ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                        Text("${product.department} • ${product.category}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                        Text("Wholesale / Sale: ₹${product.wholesalePrice} (MRP: ₹${product.mrp})", style = MaterialTheme.typography.labelSmall.copy(color = ForestGreen, fontWeight = FontWeight.Bold))
                                    }

                                    Surface(
                                        color = if (product.approvalStatus == "APPROVED") ForestContainer else GoldContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = product.approvalStatus,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Wallet & Escrow Settlement Breakdown
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Merchant Settlement Account", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Bank / UPI ID: ${activeSeller?.bankAccountOrUpi ?: "Not provided"}", style = MaterialTheme.typography.bodyMedium)
                            Text("GSTIN / PAN: ${activeSeller?.gstinOrPan ?: "N/A"}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Gross Sales:")
                                Text("₹${activeSeller?.grossSales ?: 0}", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Platform Commission Deducted:")
                                Text("-₹${activeSeller?.commissionDeducted ?: 0}", color = WholesaleRed, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Net Escrow Balance (In Clearing):")
                                Text("₹${activeSeller?.pendingEscrow ?: 0}", color = GoldAccent, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Settled Payouts (Direct to Bank):")
                                Text("₹${activeSeller?.settledPayouts ?: 0}", color = ForestGreen, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                color = ForestContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Payouts are automatically settled from the Central Owner Account (${CommissionConfig.CENTRAL_MERCHANT_NAME}) every Tuesday and Friday upon order delivery.",
                                    style = MaterialTheme.typography.labelSmall.copy(color = OnForestContainer),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Open Public Seller Onboarding Dialog
    if (showRegisterDialog) {
        SellerRegistrationDialog(
            onDismiss = { showRegisterDialog = false },
            onRegister = { newSeller ->
                onRegisterSeller(newSeller)
                showRegisterDialog = false
                Toast.makeText(context, "Seller Registered Successfully! Welcome to Navo Hub.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal: Add New Product Dialog
    if (showAddProductDialog && activeSeller != null) {
        AddNewProductDialog(
            seller = activeSeller,
            onDismiss = { showAddProductDialog = false },
            onAddProduct = { prod ->
                onAddNewProduct(prod)
                showAddProductDialog = false
                Toast.makeText(context, "Product Listed Successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun SellerStatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f)))
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = color))
        }
    }
}

@Composable
private fun SellerOrderCard(
    order: Order,
    onUpdateStatus: (String, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(order.orderId, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ForestGreen))
                    Text("Customer: ${order.customerName} (${order.customerPhone})", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }

                Surface(
                    color = ForestContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = order.orderStatus,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = OnForestContainer),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Address: ${order.address}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(4.dp))

            Text("Items (${order.items.size}): " + order.items.joinToString(", ") { "${it.product.name} (x${it.quantity})" }, style = MaterialTheme.typography.bodySmall, maxLines = 2)

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total: ₹${order.finalTotal} (${order.paymentMethod})", fontWeight = FontWeight.Bold)
                Text("Net Seller Share: ₹${order.netSellerAmount}", fontWeight = FontWeight.Bold, color = ForestGreen)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fulfillment Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (order.orderStatus == "PLACED" || order.orderStatus == "CONFIRMED") {
                    Button(
                        onClick = { onUpdateStatus(order.orderId, "PACKED") },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Mark Packed", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (order.orderStatus == "PACKED") {
                    Button(
                        onClick = { onUpdateStatus(order.orderId, "SHIPPED") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Hand over to Delivery", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (order.orderStatus == "SHIPPED" || order.orderStatus == "OUT_FOR_DELIVERY") {
                    Button(
                        onClick = { onUpdateStatus(order.orderId, "DELIVERED") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Mark Delivered", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerRegistrationDialog(
    onDismiss: () -> Unit,
    onRegister: (Seller) -> Unit
) {
    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MarketplaceDepartment.CLOTHING) }
    var bankUpi by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Seller Onboarding", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("Shop / Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number (OTP)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Shop Address in Sandhaur / Punjab") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = gstin, onValueChange = { gstin = it }, label = { Text("GSTIN or PAN Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = bankUpi, onValueChange = { bankUpi = it }, label = { Text("Bank Account / Settlement UPI ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (shopName.isNotBlank() && phone.isNotBlank()) {
                        val sellerId = "SELLER_" + System.currentTimeMillis().toString().takeLast(6)
                        onRegister(
                            Seller(
                                id = sellerId,
                                shopName = shopName,
                                ownerName = ownerName.ifBlank { "Store Owner" },
                                phone = phone,
                                email = email.ifBlank { "seller@navohub.com" },
                                address = address.ifBlank { "Main Bazaar, Sandhaur" },
                                gstinOrPan = gstin.ifBlank { "PENDING_VERIFICATION" },
                                categorySpecialization = category,
                                bankAccountOrUpi = bankUpi.ifBlank { "$phone@upi" },
                                isApproved = true
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Complete Registration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddNewProductDialog(
    seller: Seller,
    onDismiss: () -> Unit,
    onAddProduct: (Product) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var department by remember { mutableStateOf(seller.categorySpecialization) }
    var category by remember { mutableStateOf("New Arrival") }
    var mrp by remember { mutableStateOf("") }
    var salePrice by remember { mutableStateOf("") }
    var sizes by remember { mutableStateOf("") }
    var colors by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Product to Your Shop", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = mrp, onValueChange = { mrp = it }, label = { Text("MRP (₹)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = salePrice, onValueChange = { salePrice = it }, label = { Text("Your Sale Price (₹)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = sizes, onValueChange = { sizes = it }, label = { Text("Available Sizes (e.g. S,M,L or 7,8,9)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = colors, onValueChange = { colors = it }, label = { Text("Available Colors (e.g. Blue, Green)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Product Description & Specs") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mrpInt = mrp.toIntOrNull() ?: 999
                    val saleInt = salePrice.toIntOrNull() ?: (mrpInt * 0.90).toInt()
                    val newId = "PRD-" + System.currentTimeMillis().toString().takeLast(6)

                    onAddProduct(
                        Product(
                            id = newId,
                            name = name.ifBlank { "New Product" },
                            description = description,
                            department = department,
                            category = category,
                            mrp = mrpInt,
                            sale = saleInt,
                            price = saleInt,
                            stock = 25,
                            sellerId = seller.id,
                            sellerName = seller.shopName,
                            availableSizes = sizes,
                            availableColors = colors,
                            approvalStatus = "APPROVED"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Publish to Marketplace")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
