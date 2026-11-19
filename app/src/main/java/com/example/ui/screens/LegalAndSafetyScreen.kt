package com.example.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.model.AppLanguage
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StoreProfile
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.OnForestGreenContainer
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.WholesaleRed

@Composable
fun LegalAndSafetyScreen(
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    onBackClick: () -> Unit,
    onRequestDataDeletion: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletionPhone by remember { mutableStateOf("") }

    val tabTitles = when (appLanguage) {
        AppLanguage.PUNJABI -> listOf("ਈ-ਕਾਮਰਸ ਨਿਯਮ", "ਡੇਟਾ ਸੁਰੱਖਿਆ DPDP", "FSSAI ਅਤੇ ਪੈਕਿੰਗ", "RBI ਪੇਮੈਂਟ ਅਤੇ ਰਿਫੰਡ", "ਖਾਤਾ ਹਟਾਓ")
        AppLanguage.HINDI -> listOf("ई-कॉमर्स नियम", "डेटा सुरक्षा DPDP", "FSSAI व पैकिंग", "RBI व रिफंड", "डेटा हटाएं")
        AppLanguage.ENGLISH -> listOf("E-Commerce Rules", "Data Protection DPDP", "FSSAI & Metrology", "RBI & Refunds", "Delete Data")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            color = ForestGreen,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = appLanguage.tr(
                            english = "Indian Govt Laws & Legal Suite",
                            punjabi = "ਭਾਰਤ ਸਰਕਾਰ ਕਾਨੂੰਨ ਅਤੇ ਨੀਤੀਆਂ",
                            hindi = "भारत सरकार के कानून और नीतियां"
                        ),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Consumer Protection • DPDP 2023 • FSSAI • RBI Guidelines",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SignatureGold,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = ForestGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ForestGreen
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            ),
                            maxLines = 1
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> ConsumerProtectionRulesSection(isPunjabi, offersConfig)
                1 -> DpdpAct2023Section(isPunjabi)
                2 -> FssaiAndMetrologySection(isPunjabi)
                3 -> RbiAndRefundPolicySection(isPunjabi)
                4 -> AccountDeletionSection(
                    isPunjabi = isPunjabi,
                    appLanguage = appLanguage,
                    onOpenDialog = { showDeleteDialog = true }
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = WholesaleRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = appLanguage.tr(
                        english = "Request Data & Account Deletion",
                        punjabi = "ਡੇਟਾ ਅਤੇ ਖਾਤਾ ਮਿਟਾਉਣ ਦੀ ਬੇਨਤੀ",
                        hindi = "डेटा और खाता हटाने का अनुरोध"
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = appLanguage.tr(
                            english = "Under the Digital Personal Data Protection Act 2023 and Google Play Store policies, you have the fundamental right to erasure. Enter your registered phone number to purge all cached data and identifiers:",
                            punjabi = "ਡੀਪੀਡੀਪੀ ਐਕਟ 2023 ਅਤੇ ਗੂਗਲ ਪਲੇ ਸਟੋਰ ਨੀਤੀ ਅਨੁਸਾਰ, ਤੁਹਾਡਾ ਸਾਰਾ ਸਥਾਨਕ ਆਰਡਰ ਇਤਿਹਾਸ, ਕਾਰਟ ਅਤੇ ਫ਼ੋਨ ਵੇਰਵੇ ਤੁਰੰਤ ਮਿਟਾ ਦਿੱਤੇ ਜਾਣਗੇ। ਆਪਣਾ ਫ਼ੋਨ ਨੰਬਰ ਦਰਜ ਕਰੋ:",
                            hindi = "DPDP अधिनियम 2023 और Google Play नीतियों के अनुसार, आपके पास डेटा मिटाने का पूरा अधिकार है। सारा स्थानीय डेटा हटाने के लिए अपना पंजीकृत फ़ोन नंबर दर्ज करें:"
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = deletionPhone,
                        onValueChange = { deletionPhone = it },
                        label = { Text(appLanguage.tr("Phone Number (+91)", "ਫ਼ੋਨ ਨੰਬਰ (+91)", "फ़ोन नंबर (+91)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("deletion_phone_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deletionPhone.isNotBlank()) {
                            onRequestDataDeletion(deletionPhone)
                            showDeleteDialog = false
                            Toast.makeText(
                                context,
                                appLanguage.tr(
                                    english = "Data deletion request submitted successfully",
                                    punjabi = "ਡੇਟਾ ਮਿਟਾਉਣ ਦੀ ਬੇਨਤੀ ਸਫਲਤਾਪੂਰਵਕ ਭੇਜੀ ਗਈ",
                                    hindi = "डेटा हटाने का अनुरोध सफलतापूर्वक सबमिट हो गया"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                appLanguage.tr(
                                    english = "Please enter your phone number",
                                    punjabi = "ਕਿਰਪਾ ਕਰਕੇ ਆਪਣਾ ਫ਼ੋਨ ਨੰਬਰ ਦਰਜ ਕਰੋ",
                                    hindi = "कृपया अपना फ़ोन नंबर दर्ज करें"
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WholesaleRed)
                ) {
                    Text(appLanguage.tr("Delete Everything", "ਸਭ ਕੁਝ ਮਿਟਾਓ", "सब कुछ हटाएं"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(appLanguage.tr("Cancel", "ਰੱਦ ਕਰੋ", "रद्द करें"))
                }
            }
        )
    }
}

@Composable
private fun ConsumerProtectionRulesSection(
    isPunjabi: Boolean,
    offersConfig: StoreOffersConfig = StoreOffersConfig()
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Consumer Protection (E-Commerce) Rules, 2020 & Act 2019",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
            )
            Text(
                text = "Administered by Ministry of Consumer Affairs, Food and Public Distribution, Govt. of India",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            LegalParagraph(
                title = "1. Transparent MRP & Unit Level Pricing",
                content = "Every listed item transparently displays the Maximum Retail Price (MRP), actual discounted wholesale selling price (${offersConfig.discountLabelPercent}% store discount or category/item offer), net quantity per unit, and applicable tax breakdown with zero hidden service charges."
            )

            LegalParagraph(
                title = "2. Zero Dark Patterns Pledged",
                content = "NavKart strictly enforces the Guidelines for Prevention and Regulation of Dark Patterns, 2023. We do not use forced action, false urgency countdowns on standard staples, basket sneaking, or confirm-shaming."
            )

            LegalParagraph(
                title = "3. Verified Seller Badges & Marketplace Accountability",
                content = "Every seller on NavKart undergoes shop KYC verification, category authorization, and quality audits before products go live. The seller name and location are prominently disclosed on every product detail sheet."
            )

            LegalParagraph(
                title = "4. Statutory Grievance Redressal Desk",
                content = "Under Rule 5(3)(a) of E-Commerce Rules 2020, customers can register any grievance with our Grievance Officer via WhatsApp Helpline: +91 8847370905 or Email: support@navgrocerystore.com. Timelines: Acknowledgment within 48 hours; full resolution within 30 days."
            )
        }
    }
}

@Composable
private fun DpdpAct2023Section(isPunjabi: Boolean) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Digital Personal Data Protection Act, 2023 & IT Act 2000",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
            )
            Text(
                text = "Governed by Data Protection Board of India & Ministry of Electronics and IT (MeitY)",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            LegalParagraph(
                title = "1. Strict Data Minimization Principle",
                content = "We collect only what is strictly necessary for order delivery: Customer Name, Phone Number, and Delivery Village/Address. We never collect Aadhaar, biometric information, contacts, or background location."
            )

            LegalParagraph(
                title = "2. Explicit Affirmative Consent",
                content = "Consent is gathered unconditionally before placing an order. Customers retain the right to withdraw consent at any time without penalty."
            )

            LegalParagraph(
                title = "3. End-to-End Encryption in Transit",
                content = "All payload data between the Android app, Firebase Realtime Database, and WhatsApp API is transmitted over HTTPS/TLS with 256-bit encryption."
            )

            LegalParagraph(
                title = "4. Right to Erasure & Data Deletion (One-Tap)",
                content = "In accordance with Section 12 of the DPDP Act 2023, you can tap the 'Delete Data' tab to instantly wipe all local order history and submit a cloud removal request."
            )
        }
    }
}

@Composable
private fun FssaiAndMetrologySection(isPunjabi: Boolean) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "FSSAI Safety Standards & Legal Metrology Rules, 2011",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
            )
            Text(
                text = "Food Safety and Standards Authority of India (FSSAI) & Dept of Legal Metrology Compliance",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            LegalParagraph(
                title = "1. 100% Sealed Packaged Goods & Cold-Chain Only Policy",
                content = "NavKart sells 100% factory sealed packaged groceries and cold-chain frozen items ONLY (peas, sweet corn, paneer, french fries). In accordance with strict hygiene and cold-chain assurance, fresh fruits and fresh vegetables are strictly excluded from our catalog."
            )

            LegalParagraph(
                title = "2. Packaged Commodities Declarations",
                content = "In compliance with Legal Metrology (Packaged Commodities) Rules, 2011, every item listing details Net Quantity (Weight/Volume), Best Before / Expiry date, Cold-Chain sub-zero storage temperature specifications, and Country of Origin (India)."
            )

            LegalParagraph(
                title = "3. Sealed Consumable Hygiene Policy",
                content = "For FSSAI food hygiene safety, sealed food packets once opened by the customer cannot be returned if unsealed, except in cases of manufacturer defect, contamination, or broken transit seals reported upon delivery."
            )
        }
    }
}

@Composable
private fun RbiAndRefundPolicySection(isPunjabi: Boolean) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "RBI Payment Aggregator Guidelines & Refund Policies",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
            )
            Text(
                text = "Reserve Bank of India (RBI) Tokenization & Settlement Framework",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            LegalParagraph(
                title = "1. Tokenized Payments & Zero Raw Card Storage",
                content = "In accordance with RBI circular on card tokenization and PCI-DSS rules, NavKart never stores CVV, full 16-digit card numbers, or NetBanking MPINs on user devices. Digital payments route securely through RBI-authorized payment aggregators into the central merchant escrow."
            )

            LegalParagraph(
                title = "2. Free Order Cancellation Before Dispatch",
                content = "Customers can cancel any order free of charge before the package is handed over to the Sandhaur express delivery rider or courier hub."
            )

            LegalParagraph(
                title = "3. Category-Wise Return & Replacement Windows",
                content = "• Clothing, Footwear & Home: 7-day easy return or size exchange for unused items with tags intact.\n• Electronics & Mobiles: 7-day doorstep replacement for manufacturing defects.\n• Packaged Groceries & Frozen: Immediate doorstep replacement or full refund if damaged, expired, or seal is broken."
            )

            LegalParagraph(
                title = "4. Refund Settlement Timelines (RBI Mandate)",
                content = "• NavKart Store Wallet: Instant credit (within 2 hours).\n• Original UPI Source: 24 to 48 hours.\n• Credit/Debit Card or NetBanking: 5 to 7 business days as per banking clearing cycle."
            )
        }
    }
}

@Composable
private fun AccountDeletionSection(
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    onOpenDialog: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = appLanguage.tr(
                    english = "Right to Erasure & Account Deletion",
                    punjabi = "ਖਾਤਾ ਅਤੇ ਡੇਟਾ ਹਟਾਉਣਾ (DPDP 2023)",
                    hindi = "डेटा और खाता हटाने का अधिकार (DPDP 2023)"
                ),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = WholesaleRed)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appLanguage.tr(
                    english = "In compliance with Section 12 of India's Digital Personal Data Protection Act 2023 and Google Play User Data policies, users can permanently delete their account, cached cart, and order history.",
                    punjabi = "ਗੂਗਲ ਪਲੇ ਸਟੋਰ ਅਤੇ ਭਾਰਤ ਦੇ ਡੀਪੀਡੀਪੀ ਐਕਟ 2023 ਦੇ ਅਨੁਸਾਰ, ਉਪਭੋਗਤਾ ਕਿਸੇ ਵੀ ਸਮੇਂ ਆਪਣਾ ਡੇਟਾ, ਆਰਡਰ ਇਤਿਹਾਸ ਅਤੇ ਪਤਾ ਸਥਾਈ ਤੌਰ 'ਤੇ ਮਿਟਾਉਣ ਲਈ ਬੇਨਤੀ ਕਰ ਸਕਦੇ ਹਨ।",
                    hindi = "भारत के DPDP अधिनियम 2023 की धारा 12 और Google Play उपयोगकर्ता डेटा नीतियों के अनुसार, उपयोगकर्ता अपना खाता, कार्ट और ऑर्डर इतिहास स्थायी रूप से हटा सकते हैं।"
                ),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenDialog,
                colors = ButtonDefaults.buttonColors(containerColor = WholesaleRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("request_data_deletion_btn")
            ) {
                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = appLanguage.tr(
                        english = "Request Data & Account Deletion",
                        punjabi = "ਸਾਰਾ ਡੇਟਾ ਅਤੇ ਖਾਤਾ ਮਿਟਾਓ",
                        hindi = "सारा डेटा और खाता हटाएं"
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun LegalParagraph(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
        )
    }
}
