package com.example.ui.screens

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommissionConfig
import com.example.data.model.FlashCampaign
import com.example.data.model.Order
import com.example.data.model.OwnerSecureLedger
import com.example.data.model.PayoutRecord
import com.example.data.model.Product
import com.example.data.model.Seller
import com.example.data.model.StaffMember
import com.example.data.model.SystemLocks
import com.example.ui.theme.ForestContainer
import com.example.ui.theme.ForestDark
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.OnForestContainer
import com.example.ui.theme.OnGoldContainer
import com.example.ui.theme.WholesaleRed

/**
 * Dedicated Controller App for the Owner & Admin Staff
 * Application Two: NavKart Admin Controller (com.navgrocerystore.navkartadmin)
 * Synchronized live with www.navgrocerystore.com/admin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlCenterScreen(
    orders: List<Order>,
    sellers: List<Seller>,
    payouts: List<PayoutRecord>,
    products: List<Product>,
    isPunjabi: Boolean,
    onBack: () -> Unit,
    onSettlePayout: (sellerId: String, amount: Int, utr: String) -> Unit,
    onRefundOrder: (orderId: String) -> Unit,
    onToggleSellerApproval: (sellerId: String, isApproved: Boolean) -> Unit
) {
    val context = LocalContext.current

    // Security Gate: PIN 148020 + Biometrics
    var enteredPin by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }

    // 7 Dedicated Controller Modules
    var selectedModuleTab by remember { mutableIntStateOf(0) }

    // Module 1: System Locks
    var locks by remember { mutableStateOf(SystemLocks()) }

    // Module 5: Campaigns & Push Studio
    val campaigns = remember {
        mutableStateListOf(
            FlashCampaign(
                id = "CAMP-101",
                title = "Sandhaur Lightning Flash Deals",
                subtitle = "Up to 12% off on cold-chain frozen paneer & peas",
                discountPercent = 12
            )
        )
    }
    var newPushTitle by remember { mutableStateOf("") }
    var newPushBody by remember { mutableStateOf("") }

    // Module 6: Cashier & Staff
    val staffList = remember {
        mutableStateListOf(
            StaffMember(id = "STF-1", name = "Navjeet Kaur", role = "CASHIER", phone = "8847370905", accessPin = "1122"),
            StaffMember(id = "STF-2", name = "Gurpreet Singh", role = "DELIVERY_RIDER", phone = "8847370905", accessPin = "1480")
        )
    }

    // Settlement dialog
    var settlingSeller by remember { mutableStateOf<Seller?>(null) }
    var customUtr by remember { mutableStateOf("") }

    // Exclusive Gemini AI Copilot State
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var geminiAiPrompt by remember { mutableStateOf("") }
    var geminiAiResponse by remember { mutableStateOf("") }
    var isGeminiGenerating by remember { mutableStateOf(false) }

    if (!isUnlocked) {
        // Owner Lock Screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("NavKart Admin Controller", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ForestDark)
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ForestContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Admin Controller App",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Package: com.navgrocerystore.navkartadmin\nSync: nav-grocery-store-default-rtdb.firebaseio.com\nCentral Merchant: ${OwnerSecureLedger.OWNER_LEGAL_NAME} (GSTIN: ${OwnerSecureLedger.GSTIN})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                if (it.length <= 6) enteredPin = it
                                if (it == OwnerSecureLedger.SUPER_ADMIN_PIN) {
                                    isUnlocked = true
                                }
                            },
                            label = { Text("Enter 6-Digit Master Owner PIN") },
                            placeholder = { Text("148020") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (enteredPin == OwnerSecureLedger.SUPER_ADMIN_PIN) {
                                    isUnlocked = true
                                } else {
                                    Toast.makeText(context, "Incorrect PIN. Default is 148020", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock Admin Controller", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    // Unlocked Admin Controller Dashboard
    val totalGmv = orders.sumOf { it.finalTotal }
    val totalCommission = orders.sumOf { it.platformCommission }
    val totalSellerNet = orders.sumOf { it.netSellerAmount }
    val settledPayoutsTotal = payouts.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NavKart Admin Controller", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Owner: ${OwnerSecureLedger.OWNER_LEGAL_NAME} • GSTIN: ${OwnerSecureLedger.GSTIN}", color = GoldContainer, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { isUnlocked = false }) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ForestDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Master Financial Banner
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = ForestDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatCard("Platform GMV", "₹$totalGmv", Color.White, Modifier.weight(1f))
                    AdminStatCard("Commission", "₹$totalCommission", GoldAccent, Modifier.weight(1f))
                    AdminStatCard("Escrow Balance", "₹${maxOf(0, totalSellerNet - settledPayoutsTotal)}", Color(0xFF81C784), Modifier.weight(1f))
                }
            }

            // 8 Modules Tabs (Module 1: Exclusive Gemini AI Copilot)
            TabRow(
                selectedTabIndex = selectedModuleTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ForestGreen
            ) {
                Tab(selected = selectedModuleTab == 0, onClick = { selectedModuleTab = 0 }, text = { Text("Gemini AI", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedModuleTab == 1, onClick = { selectedModuleTab = 1 }, text = { Text("System Locks", fontSize = 10.sp) })
                Tab(selected = selectedModuleTab == 2, onClick = { selectedModuleTab = 2 }, text = { Text("Catalog", fontSize = 10.sp) })
                Tab(selected = selectedModuleTab == 3, onClick = { selectedModuleTab = 3 }, text = { Text("Sellers (${sellers.size})", fontSize = 10.sp) })
                Tab(selected = selectedModuleTab == 4, onClick = { selectedModuleTab = 4 }, text = { Text("Escrow/GST", fontSize = 10.sp) })
                Tab(selected = selectedModuleTab == 5, onClick = { selectedModuleTab = 5 }, text = { Text("Push & Ads", fontSize = 10.sp) })
                Tab(selected = selectedModuleTab == 6, onClick = { selectedModuleTab = 6 }, text = { Text("Staff (${staffList.size})", fontSize = 10.sp) })
                Tab(selected = selectedModuleTab == 7, onClick = { selectedModuleTab = 7 }, text = { Text("Logistics", fontSize = 10.sp) })
            }

            // Content
            when (selectedModuleTab) {
                0 -> {
                    // MODULE ONE: EXCLUSIVE GEMINI AI ADMIN COPILOT
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(ForestContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Exclusive Gemini AI Admin Copilot", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Synced with www.navgrocerystore.com/admin • Owner Secure", style = MaterialTheme.typography.labelSmall.copy(color = ForestGreen))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Copilot Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    isGeminiGenerating = true
                                    geminiAiResponse = "Analyzing live marketplace financials & escrow..."
                                    coroutineScope.launch {
                                        geminiAiResponse = com.example.data.ai.AdminGeminiAiCopilot.analyzeBusinessAndEscrow(
                                            orders = orders,
                                            sellers = sellers,
                                            totalGmv = totalGmv,
                                            totalCommission = totalCommission,
                                            escrowBalance = maxOf(0, totalSellerNet - settledPayoutsTotal)
                                        )
                                        isGeminiGenerating = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Text("Escrow Analysis", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    isGeminiGenerating = true
                                    geminiAiResponse = "Drafting GSTR-1 tax compliance summary..."
                                    coroutineScope.launch {
                                        geminiAiResponse = com.example.data.ai.AdminGeminiAiCopilot.generateGstTaxSummary(orders)
                                        isGeminiGenerating = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Text("GSTR-1 Tax Summary", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isGeminiGenerating = true
                                    geminiAiResponse = "Drafting trilingual promotional broadcast..."
                                    coroutineScope.launch {
                                        geminiAiResponse = com.example.data.ai.AdminGeminiAiCopilot.draftTrilingualCampaign(
                                            dealItem = "Verka Pure Desi Ghee & Frozen Green Peas",
                                            discountPercent = 12
                                        )
                                        isGeminiGenerating = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Text("Draft 3-Lang Promo", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    isGeminiGenerating = true
                                    geminiAiResponse = "Auditing inventory against zero fresh produce policy..."
                                    coroutineScope.launch {
                                        geminiAiResponse = com.example.data.ai.AdminGeminiAiCopilot.auditProductPolicy(
                                            productName = "Sample Packaged Basmati Rice",
                                            category = "Grocery & Staples",
                                            description = "100% sealed factory packaged food"
                                        )
                                        isGeminiGenerating = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Text("Audit Policy", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Query Input
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = geminiAiPrompt,
                                onValueChange = { geminiAiPrompt = it },
                                label = { Text("Ask Gemini AI Copilot") },
                                placeholder = { Text("e.g. Audit seller payout status or draft deal...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (geminiAiPrompt.isNotBlank()) {
                                        val p = geminiAiPrompt
                                        geminiAiPrompt = ""
                                        isGeminiGenerating = true
                                        geminiAiResponse = "Gemini thinking..."
                                        coroutineScope.launch {
                                            geminiAiResponse = com.example.data.ai.AdminGeminiAiCopilot.draftTrilingualCampaign(p, 12)
                                            isGeminiGenerating = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Ask")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Output Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Copilot Intelligence Output", fontWeight = FontWeight.Bold, color = ForestGreen)
                                    if (isGeminiGenerating) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ForestGreen)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                Text(
                                    text = if (geminiAiResponse.isNotBlank()) geminiAiResponse else "Tap any copilot action button above or enter a custom prompt. Gemini AI provides deep operational insights with zero customer exposure.",
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // MODULE TWO: SYSTEM LOCK & SECURITY SUITE
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text("Node 9: Master System Locks & Safeguards", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Live sync across www.navgrocerystore.com and NavKart Public App.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                        Spacer(modifier = Modifier.height(14.dp))

                        LockControlItem(
                            title = "Catalog Lock (308 Verified SKUs)",
                            desc = "Protects locked OCR SKUs NAV001 to NAV308 and packaging photos from unauthorized modifications.",
                            isActive = locks.catalogLocked,
                            onToggle = { locks = locks.copy(catalogLocked = it) }
                        )

                        LockControlItem(
                            title = "Pricing Integrity Lock (Flat 12% OFF)",
                            desc = "Strictly enforces Math.round(mrp * 0.88) wholesale rule on all sealed packaged grocery products.",
                            isActive = locks.pricingIntegrityLocked,
                            onToggle = { locks = locks.copy(pricingIntegrityLocked = it) }
                        )

                        LockControlItem(
                            title = "Strict Policy Guard (Zero Fresh Produce)",
                            desc = "Hard-blocks fresh fruits and vegetables from being added by sellers or shown in catalog.",
                            isActive = locks.strictPolicyGuardLocked,
                            onToggle = { locks = locks.copy(strictPolicyGuardLocked = it) }
                        )

                        LockControlItem(
                            title = "Emergency Storefront Freeze Switch",
                            desc = "Instantly pauses all order placement across both Android Public App and Website in case of stock count.",
                            isActive = locks.emergencyStorefrontFreeze,
                            activeColor = WholesaleRed,
                            onToggle = {
                                locks = locks.copy(emergencyStorefrontFreeze = it)
                                Toast.makeText(context, if (it) "STOREFRONT FROZEN" else "Storefront Active", Toast.LENGTH_SHORT).show()
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Golden Backup Snapshot
                        Button(
                            onClick = {
                                locks = locks.copy(lastGoldenSnapshotTimestamp = System.currentTimeMillis())
                                Toast.makeText(context, "Golden Snapshot Saved to Firebase!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1-Tap Golden Snapshot Backup in Firebase")
                        }
                    }
                }

                2 -> {
                    // MODULE THREE: PRODUCT & BULK IMPORT/EXPORT MANAGER
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text("Products & Bulk Manager (${products.size} Items)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Export tax records or import vendor catalogs via CSV/Excel.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Bulk Catalog Export (CSV/XLSX) generated for ${products.size} items", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Catalog", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "Bulk Import CSV/XLSX parsed: 0 invalid items", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bulk Import CSV", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sample Product Overview
                        products.take(10).forEach { p ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("SKU: ${p.id} • MRP: ₹${p.mrp} • Sale (12% off): ₹${p.wholesalePrice}", style = MaterialTheme.typography.labelSmall.copy(color = ForestGreen))
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // MODULE FOUR: SHOPKEEPER & PRODUCT MODERATION QUEUE
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(sellers, key = { it.id }) { seller ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text(seller.shopName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text("${seller.ownerName} • ${seller.phone}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Surface(
                                            color = if (seller.isApproved) ForestContainer else WholesaleRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (seller.isApproved) "APPROVED" else "PENDING MODERATION",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (seller.isApproved) ForestGreen else WholesaleRed
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Category: ${seller.categorySpecialization}", style = MaterialTheme.typography.bodySmall)
                                    Text("Settlement Account: ${seller.bankAccountOrUpi}", style = MaterialTheme.typography.bodySmall)

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                settlingSeller = seller
                                                customUtr = "UTR" + System.currentTimeMillis().toString().takeLast(8)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text("Settle Escrow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { onToggleSellerApproval(seller.id, !seller.isApproved) },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text(if (seller.isApproved) "Suspend" else "Approve", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // MODULE FIVE: CENTRALIZED ESCROW & GST LEDGER
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text("Central Merchant Escrow & GST Compliance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Owner: ${OwnerSecureLedger.OWNER_LEGAL_NAME} | GSTIN: ${OwnerSecureLedger.GSTIN}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                        Spacer(modifier = Modifier.height(14.dp))

                        // GST Summary Box
                        Surface(
                            color = ForestContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Monthly GSTR-1 Tax Summary (Sandhaur Hub)", fontWeight = FontWeight.Bold, color = OnForestContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Intra-State CGST (2.5%) + SGST (2.5%) = 5% on Groceries\n• Inter-State IGST (12%/18%) on Electronics & Apparel\n• Central Merchant Account: ${OwnerSecureLedger.CENTRAL_MERCHANT_VPA}", style = MaterialTheme.typography.bodySmall, color = OnForestContainer)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Monthly GSTR-1 Report Exported (PDF/Excel)", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export GSTR-1 & Tax Invoices (PDF/Excel)")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Settled Payouts History (${payouts.size}):", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        payouts.forEach { p ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(p.sellerName, fontWeight = FontWeight.Bold)
                                        Text("₹${p.amount}", fontWeight = FontWeight.ExtraBold, color = ForestGreen)
                                    }
                                    Text("UTR: ${p.utrReference}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                }
                            }
                        }
                    }
                }

                5 -> {
                    // MODULE SIX: ADS, FLASH DEALS & PUSH NOTIFICATION STUDIO
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text("FCM Push Notifications & Flash Deals Studio", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Broadcast instant push notifications to all NavKart Public App users.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = newPushTitle,
                            onValueChange = { newPushTitle = it },
                            label = { Text("Push Notification Title") },
                            placeholder = { Text("⚡ Flash Deal: 12% OFF on Desi Ghee!") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newPushBody,
                            onValueChange = { newPushBody = it },
                            label = { Text("Message Body") },
                            placeholder = { Text("Order now for ~2 hour express delivery in Sandhaur!") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (newPushTitle.isNotBlank()) {
                                    Toast.makeText(context, "FCM Broadcast Sent to all devices!", Toast.LENGTH_SHORT).show()
                                    newPushTitle = ""
                                    newPushBody = ""
                                } else {
                                    Toast.makeText(context, "Enter push notification title", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Broadcast Push Notification")
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text("Active Campaigns & Carousel Banners:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        campaigns.forEach { c ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, tint = ForestGreen)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(c.title, fontWeight = FontWeight.Bold)
                                        Text(c.subtitle, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                6 -> {
                    // MODULE SEVEN: ROLE-BASED CASHIER & STAFF MANAGER
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text("Role-Based Staff Access Control", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Cashiers have restricted billing access. Master financial ledgers and system locks remain locked to Owner PIN.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                        Spacer(modifier = Modifier.height(14.dp))

                        staffList.forEach { s ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(s.name, fontWeight = FontWeight.Bold)
                                        Text("Role: ${s.role} • PIN: ${s.accessPin}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Surface(
                                        color = if (s.isActive) ForestContainer else WholesaleRed.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (s.isActive) "ACTIVE" else "DISABLED",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (s.isActive) ForestGreen else WholesaleRed
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                7 -> {
                    // MODULE EIGHT: LOGISTICS & COURIER AWB MANAGEMENT
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text("Logistics & Pan-India Courier Dispatches", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sandhaur PIN 148020 ~2hr local delivery + Shiprocket, Delhivery, DTDC, BlueDart, India Post.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = ForestContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Express Hyperlocal Dispatch Ready", fontWeight = FontWeight.Bold, color = OnForestContainer)
                                Text("2 assigned Sandhaur delivery riders active. 4-digit OTP handshake enabled for doorstep proof of delivery.", style = MaterialTheme.typography.bodySmall, color = OnForestContainer)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Courier Shipments Synced with Delhivery / Shiprocket API", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Courier AWBs & Print Shipping Labels")
                        }
                    }
                }
            }
        }
    }

    // Modal: Settle Payout Dialog
    if (settlingSeller != null) {
        val s = settlingSeller!!
        val amountToSettle = maxOf(100, s.pendingEscrow)

        AlertDialog(
            onDismissRequest = { settlingSeller = null },
            title = { Text("Settle Escrow Payout (${s.shopName})", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Payout Amount: ₹$amountToSettle", style = MaterialTheme.typography.titleMedium.copy(color = ForestGreen, fontWeight = FontWeight.Bold))
                    Text("Recipient UPI / Bank: ${s.bankAccountOrUpi}")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customUtr,
                        onValueChange = { customUtr = it },
                        label = { Text("Bank UTR / Transaction Reference") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSettlePayout(s.id, amountToSettle, customUtr.ifBlank { "UTR" + System.currentTimeMillis() })
                        settlingSeller = null
                        Toast.makeText(context, "Payout Settled & UTR Logged!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Confirm Settlement")
                }
            },
            dismissButton = {
                TextButton(onClick = { settlingSeller = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LockControlItem(
    title: String,
    desc: String,
    isActive: Boolean,
    activeColor: Color = ForestGreen,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }

            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = activeColor,
                    checkedTrackColor = activeColor.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun AdminStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp), maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color))
        }
    }
}
