package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.FirestoreOrderRecord
import com.example.data.model.Order
import com.example.data.model.StorePaymentsConfig
import com.example.data.model.StoreProfile
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.OnForestGreenContainer
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer
import com.example.ui.theme.WarmCream
import com.example.ui.theme.WarmCreamBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Informational EMI eligibility badge displayed on high-value product detail sheets and cart totals.
 * Never collects raw card numbers, Bajaj card numbers, expiry, CVV, or PINs.
 */
@Composable
fun EmiEligibilityBadge(
    amountInr: Int,
    paymentsConfig: StorePaymentsConfig,
    appLanguage: AppLanguage,
    onViewPlansClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!paymentsConfig.isEligibleForEmi(amountInr)) return
    val startingMonthly = paymentsConfig.emiStartingPerMonth(amountInr)

    Surface(
        color = ForestGreenContainer.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("emi_eligibility_badge")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditScore,
                    contentDescription = "EMI Available",
                    tint = ForestGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = appLanguage.tr(
                            english = "EMI Available from ₹$startingMonthly/month (Bajaj Finserv EMI Card & Bank Cards)",
                            punjabi = "₹$startingMonthly/ਮਹੀਨਾ ਤੋਂ EMI ਉਪਲਬਧ (Bajaj Finserv EMI Card ਅਤੇ ਬੈਂਕ ਕਾਰਡ)",
                            hindi = "₹$startingMonthly/माह से EMI उपलब्ध (Bajaj Finserv EMI Card और बैंक कार्ड)"
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestGreenDark,
                            lineHeight = 16.sp
                        )
                    )
                    Text(
                        text = appLanguage.tr(
                            english = "Eligible on orders ≥ ₹${paymentsConfig.sanitizedMinEmiOrderAmount} via gateway checkout",
                            punjabi = "₹${paymentsConfig.sanitizedMinEmiOrderAmount}+ ਦੇ ਆਰਡਰ 'ਤੇ ਪੇਮੈਂਟ ਗੇਟਵੇ ਰਾਹੀਂ EMI",
                            hindi = "₹${paymentsConfig.sanitizedMinEmiOrderAmount}+ ऑर्डर पर RBI-लाइसेंसी पेमेंट गेटवे से आसान किश्तें"
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = OnForestGreenContainer,
                            fontSize = 10.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            TextButton(
                onClick = onViewPlansClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("view_emi_plans_btn")
            ) {
                Text(
                    text = appLanguage.tr("View EMI Plans", "EMI ਪਲਾਨ ਦੇਖੋ", "EMI प्लान देखें"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ForestGreen
                )
            }
        }
    }
}

/**
 * Read-only indicative 3, 6, 9, and 12-month EMI plans sheet for Bajaj Finserv & Bank Cards.
 * Strictly zero card inputs inside the app; actual EMI onboarding occurs only on RBI-licensed gateway.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EmiPlansBottomSheet(
    amountInr: Int,
    paymentsConfig: StorePaymentsConfig,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val plans = paymentsConfig.buildEmiPlans(amountInr)
    val providers = paymentsConfig.effectiveProviders

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmCream,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
                .testTag("emi_plans_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appLanguage.tr(
                            english = "Indicative EMI Installment Plans",
                            punjabi = "ਸੰਭਾਵੀ EMI ਕਿਸ਼ਤ ਪਲਾਨ (Bajaj & Banks)",
                            hindi = "अनुमानित EMI किश्त प्लान (Bajaj व बैंक)"
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestGreenDark
                        )
                    )
                    Text(
                        text = appLanguage.tr(
                            english = "For Order / Product Value: ₹$amountInr",
                            punjabi = "ਆਰਡਰ / ਉਤਪਾਦ ਮੁੱਲ: ₹$amountInr",
                            hindi = "उत्पाद / ऑर्डर राशि: ₹$amountInr"
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close EMI Plans")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = appLanguage.tr(
                    english = "Supported EMI Partners:",
                    punjabi = "ਸਹਾਇਕ EMI ਭਾਈਵਾਲ:",
                    hindi = "समर्थित EMI पार्टनर्स:"
                ),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenDark
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                providers.forEach { provider ->
                    val isBajaj = provider.contains("Bajaj", ignoreCase = true)
                    Surface(
                        color = if (isBajaj) SignatureGoldContainer else Color.White,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isBajaj) SignatureGold else WarmCreamBorder
                        )
                    ) {
                        Text(
                            text = provider,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isBajaj) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = ForestGreenDark,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            plans.forEach { plan ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WarmCreamBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${plan.months} Months • ₹${plan.monthlyAmount}/mo",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ForestGreenDark
                                    )
                                )
                                if (plan.isNoCost) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(color = ForestGreenContainer, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = "No-Cost EMI",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ForestGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (plan.isNoCost) {
                                    "0% p.a. indicative interest • Total ₹${plan.totalPayable}"
                                } else {
                                    "~${plan.annualInterestPercent.roundToInt()}% p.a. indicative • Total ₹${plan.totalPayable}"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF5A6D63))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = SignatureGoldContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SignatureGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = ForestGreenDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = appLanguage.tr(
                            english = "PCI-DSS & Play Store Safe: Nav Grocery Store Navkart is not a lender or NBFC. Exact tenure eligibility & card entry happen exclusively on our RBI-licensed payment gateway at checkout.",
                            punjabi = "ਸੁਰੱਖਿਅਤ ਗੇਟਵੇ: ਅਸੀਂ ਕਰਜ਼ਾ ਦੇਣ ਵਾਲੀ ਸੰਸਥਾ ਨਹੀਂ ਹਾਂ। ਅਸਲ EMI ਵਿਕਲਪ ਅਤੇ ਕਾਰਡ ਵੇਰਵੇ ਸਿਰਫ਼ ਸੁਰੱਖਿਅਤ ਪੇਮੈਂਟ ਗੇਟਵੇ 'ਤੇ ਦਰਜ ਕੀਤੇ ਜਾਂਦੇ ਹਨ।",
                            hindi = "PCI-DSS सुरक्षित: नव ग्रोसरी स्टोर कोई ऋणदाता नहीं है। EMI की पात्रता व कार्ड विवरण केवल चेकआउट पर RBI-लाइसेंसी पेमेंट गेटवे पर ही संसाधित होते हैं।"
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ForestGreenDark,
                            lineHeight = 15.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(appLanguage.tr("Close Plans", "ਬੰਦ ਕਰੋ", "बंद करें"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class InvoiceLineDisplay(
    val name: String,
    val qty: Int,
    val mrpTotal: Int,
    val sellingTotal: Int
)

/**
 * Digital Tax / GST Bill Modal for both Firestore cloud orders and local device orders.
 */
@Composable
fun DigitalGstInvoiceDialog(
    cloudOrder: FirestoreOrderRecord,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val dateLabel = cloudOrder.createdAt?.toDate()?.let {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(it)
    } ?: SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date())

    val lines = cloudOrder.items.map { item ->
        InvoiceLineDisplay(
            name = listOf(item.name, item.variant).filter { it.isNotBlank() }.joinToString(" • "),
            qty = item.qty,
            mrpTotal = item.mrp * item.qty,
            sellingTotal = item.wholesalePrice * item.qty
        )
    }

    DigitalGstInvoiceCoreDialog(
        orderId = cloudOrder.orderId,
        dateLabel = dateLabel,
        customerName = if (cloudOrder.anonymized) "Anonymized Customer (DPDP)" else cloudOrder.customerName,
        customerPhone = if (cloudOrder.anonymized) "REDACTED" else "+91 ${cloudOrder.customerPhone}",
        address = cloudOrder.addressSnapshot,
        pinCode = cloudOrder.pinCode.ifBlank { StoreProfile.DEFAULT_PIN_CODE },
        deliveryTier = cloudOrder.deliveryTier,
        paymentMethod = cloudOrder.paymentMethod,
        status = cloudOrder.status,
        lines = lines,
        totalMrp = cloudOrder.totalMrp.toInt(),
        totalDiscount = (cloudOrder.wholesaleDiscount + cloudOrder.petiDiscountRupees + cloudOrder.spinCouponDiscountRupees + cloudOrder.coinDiscountRupeesCompat()).toInt(),
        petiDiscount = cloudOrder.petiDiscountRupees.toInt(),
        spinCoupon = cloudOrder.spinCouponDiscountRupees.toInt(),
        coinsRedeemed = cloudOrder.coinsRedeemed.toInt(),
        walletDeducted = cloudOrder.walletDeductedRupees.toInt(),
        deliveryFee = cloudOrder.deliveryFee.toInt(),
        finalTotal = cloudOrder.finalTotal.toInt(),
        integrityDigest = cloudOrder.integrityDigest,
        appLanguage = appLanguage,
        onDismiss = onDismiss
    )
}

private fun FirestoreOrderRecord.coinDiscountRupeesCompat(): Long = coinsRedeemed

@Composable
fun DigitalGstInvoiceDialog(
    localOrder: Order,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val dateLabel = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(localOrder.timestamp))
    val lines = localOrder.items.map { item ->
        val unitSale = if (item.isPetiCarton && item.product.isBulkEligible) {
            item.product.petiTotalPrice
        } else {
            item.product.wholesalePrice
        }
        val unitMrp = if (item.isPetiCarton && item.product.isBulkEligible) {
            item.product.mrp * item.petiUnits
        } else {
            item.product.mrp
        }
        InvoiceLineDisplay(
            name = listOf(item.product.name, item.variantSummary).filter { it.isNotBlank() }.joinToString(" • "),
            qty = item.quantity,
            mrpTotal = unitMrp * item.quantity,
            sellingTotal = unitSale * item.quantity
        )
    }

    DigitalGstInvoiceCoreDialog(
        orderId = localOrder.orderId,
        dateLabel = dateLabel,
        customerName = localOrder.customerName,
        customerPhone = "+91 ${localOrder.customerPhone}",
        address = localOrder.address,
        pinCode = Regex("\\b(\\d{6})\\b").find(localOrder.address)?.groupValues?.getOrNull(1) ?: StoreProfile.DEFAULT_PIN_CODE,
        deliveryTier = localOrder.deliveryType,
        paymentMethod = localOrder.paymentMethod,
        status = localOrder.status,
        lines = lines,
        totalMrp = localOrder.totalMrp,
        totalDiscount = localOrder.totalDiscount,
        petiDiscount = localOrder.petiDiscountRupees,
        spinCoupon = localOrder.spinCouponRupees,
        coinsRedeemed = localOrder.coinsRedeemed,
        walletDeducted = localOrder.walletDeductedRupees,
        deliveryFee = localOrder.deliveryFee,
        finalTotal = localOrder.finalTotal,
        integrityDigest = localOrder.integrityDigest,
        appLanguage = appLanguage,
        onDismiss = onDismiss
    )
}

@Composable
private fun DigitalGstInvoiceCoreDialog(
    orderId: String,
    dateLabel: String,
    customerName: String,
    customerPhone: String,
    address: String,
    pinCode: String,
    deliveryTier: String,
    paymentMethod: String,
    status: String,
    lines: List<InvoiceLineDisplay>,
    totalMrp: Int,
    totalDiscount: Int,
    petiDiscount: Int,
    spinCoupon: Int,
    coinsRedeemed: Int,
    walletDeducted: Int,
    deliveryFee: Int,
    finalTotal: Int,
    integrityDigest: String,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    // Standard 5% composite retail GST split (2.5% CGST + 2.5% SGST) inclusive on goods total
    val goodsValue = (finalTotal - deliveryFee).coerceAtLeast(0)
    val taxableValue = (goodsValue / 1.05).roundToInt()
    val totalTax = (goodsValue - taxableValue).coerceAtLeast(0)
    val cgst = totalTax / 2
    val sgst = totalTax - cgst

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = "GST Tax Invoice",
                tint = ForestGreen,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = appLanguage.tr(
                        english = "TAX INVOICE / DIGITAL BILL",
                        punjabi = "ਟੈਕਸ ਇਨਵੌਇਸ / ਡਿਜੀਟਲ ਬਿੱਲ",
                        hindi = "टैक्स इनवॉइस / डिजिटल बिल"
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = ForestGreenDark
                    )
                )
                Text(
                    text = "Nav Grocery Store Navkart",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                )
                Text(
                    text = "Main Bazaar, Sandhaur, Punjab 148020",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF5A6D63))
                )
                Surface(
                    color = SignatureGoldContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "GSTIN: ${StoreProfile.GSTIN}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestGreenDark,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("digital_gst_invoice_body"),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                HorizontalDivider(color = WarmCreamBorder)
                InvoiceMetaRow("Invoice / Order ID", orderId)
                InvoiceMetaRow("Date & Time", dateLabel)
                InvoiceMetaRow("Status", status)
                InvoiceMetaRow("Customer", "$customerName ($customerPhone)")
                InvoiceMetaRow("Delivery / PIN", "$deliveryTier • PIN $pinCode")
                Text(
                    text = address,
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF5A6D63)),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                InvoiceMetaRow("Payment Mode", paymentMethod)

                HorizontalDivider(color = WarmCreamBorder, modifier = Modifier.padding(vertical = 4.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Item", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, modifier = Modifier.weight(2.2f))
                    Text("Qty", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(0.5f))
                    Text("Net ₹", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                }
                HorizontalDivider(color = WarmCreamBorder.copy(alpha = 0.7f))

                lines.forEach { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = line.name,
                            fontSize = 10.5.sp,
                            color = ForestGreenDark,
                            modifier = Modifier.weight(2.2f)
                        )
                        Text(
                            text = "${line.qty}",
                            fontSize = 10.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.5f)
                        )
                        Text(
                            text = "₹${line.sellingTotal}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }

                HorizontalDivider(color = WarmCreamBorder, modifier = Modifier.padding(vertical = 4.dp))

                InvoiceMetaRow("Total MRP Value", "₹$totalMrp")
                InvoiceMetaRow("Store & Loyalty Savings", "-₹$totalDiscount", valueColor = Color(0xFFC62828))
                if (petiDiscount > 0) InvoiceMetaRow("Peti Slab Rebate", "-₹$petiDiscount", valueColor = ForestGreen)
                if (spinCoupon > 0) InvoiceMetaRow("Spin Reward Discount", "-₹$spinCoupon", valueColor = ForestGreen)
                if (coinsRedeemed > 0) InvoiceMetaRow("Nav Coins Redeemed", "-₹$coinsRedeemed", valueColor = ForestGreen)
                if (walletDeducted > 0) InvoiceMetaRow("Prepaid Wallet Used", "-₹$walletDeducted", valueColor = ForestGreen)
                InvoiceMetaRow("Delivery Charge", if (deliveryFee == 0) "FREE" else "₹$deliveryFee")

                Surface(
                    color = WarmCream,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("GST Tax Summary (Inclusive in Price)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ForestGreenDark)
                        InvoiceMetaRow("Taxable Value", "₹$taxableValue")
                        InvoiceMetaRow("CGST (2.5%)", "₹$cgst")
                        InvoiceMetaRow("SGST (2.5%)", "₹$sgst")
                    }
                }

                Surface(
                    color = ForestGreenContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GRAND TOTAL PAID",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = ForestGreenDark
                        )
                        Text(
                            text = "₹$finalTotal",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = ForestGreen
                        )
                    }
                }

                if (integrityDigest.isNotBlank()) {
                    Text(
                        text = "Verification Hash: ${integrityDigest.take(20)}...",
                        fontSize = 9.5.sp,
                        color = Color(0xFF5A6D63)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text(appLanguage.tr("Close Bill", "ਬਿੱਲ ਬੰਦ ਕਰੋ", "बिल बंद करें"), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun InvoiceMetaRow(
    label: String,
    value: String,
    valueColor: Color = ForestGreenDark
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF5A6D63),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.3f)
        )
    }
}
