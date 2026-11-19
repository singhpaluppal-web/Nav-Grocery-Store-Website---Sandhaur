package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.DeliveryOption
import com.example.data.model.Order
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.SavedAddress
import com.example.data.model.StoreDeliveryConfig
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StorePaymentsConfig
import com.example.data.model.StoreProfile
import com.example.data.model.UserAccount
import com.example.data.security.CheckoutPricingEngine
import com.example.data.security.InputSanitizer
import com.example.ui.components.EmiEligibilityBadge
import com.example.ui.components.EmiPlansBottomSheet
import com.example.ui.components.UpiVerificationQrMatrix
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.OnForestGreenContainer
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer
import com.example.ui.theme.WarmCream
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSheet(
    cart: List<CartItem> = emptyList(),
    masterCatalog: List<Product> = emptyList(),
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    paymentsConfig: StorePaymentsConfig = StorePaymentsConfig(),
    deliveryConfig: StoreDeliveryConfig = StoreDeliveryConfig(),
    categoryDiscounts: Map<String, Double> = emptyMap(),
    currentUser: FirebaseUser? = null,
    userAccount: UserAccount? = null,
    savedAddresses: List<SavedAddress> = emptyList(),
    applySpinCoupon: Boolean = true,
    coinsToRedeem: Int = 0,
    applyWalletBalance: Boolean = false,
    totalPayable: Int,
    totalSavings: Int,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    isTestModeActive: Boolean,
    isSubmittingOrder: Boolean = false,
    onGoogleSignInClick: () -> Unit = {},
    onSaveAddressClick: (
        addressId: String?,
        fullName: String,
        mobile: String,
        streetAddress: String,
        landmark: String,
        villageOrCity: String,
        pinCode: String,
        makeDefault: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDismiss: () -> Unit,
    onConfirmOrder: (
        name: String,
        phone: String,
        option: DeliveryOption,
        address: String,
        notes: String,
        payment: PaymentMethod,
        dispatchSlot: String,
        isNriOrder: Boolean,
        useWalletBalance: Boolean,
        pinCode: String
    ) -> Unit,
    onCallHelpline: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultAddr = remember(savedAddresses) {
        savedAddresses.firstOrNull { it.defaultAddress } ?: savedAddresses.firstOrNull()
    }

    var customerName by remember { mutableStateOf(defaultAddr?.fullName ?: currentUser?.displayName.orEmpty()) }
    var customerPhone by remember { mutableStateOf(defaultAddr?.mobile.orEmpty()) }
    var address by remember { mutableStateOf(defaultAddr?.formattedLine() ?: "Sandhaur, District Malerkotla, Punjab, PIN 148020") }
    var pinCode by remember { mutableStateOf(defaultAddr?.pinCode?.ifBlank { StoreProfile.DEFAULT_PIN_CODE } ?: StoreProfile.DEFAULT_PIN_CODE) }
    var notes by remember { mutableStateOf("") }
    var selectedDeliveryOption by remember { mutableStateOf(DeliveryOption.EXPRESS_15_MIN) }
    var selectedDispatchSlot by remember { mutableStateOf(StoreProfile.PIND_DISPATCH_SLOTS.first()) }
    var isNriOrder by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.UPI_INTENT) }
    var useWalletBalance by remember { mutableStateOf(applyWalletBalance) }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showEmiPlansSheet by remember { mutableStateOf(false) }

    LaunchedEffect(defaultAddr?.id) {
        if (defaultAddr != null) {
            if (customerName.isBlank()) customerName = defaultAddr.fullName
            if (customerPhone.isBlank()) customerPhone = defaultAddr.mobile
            address = defaultAddr.formattedLine()
            if (defaultAddr.pinCode.length == 6) pinCode = defaultAddr.pinCode
        }
    }

    val verifiedSummary = remember(
        cart,
        masterCatalog,
        selectedDeliveryOption,
        userAccount,
        applySpinCoupon,
        coinsToRedeem,
        useWalletBalance,
        selectedPaymentMethod,
        offersConfig,
        categoryDiscounts,
        deliveryConfig,
        pinCode
    ) {
        CheckoutPricingEngine.verifyAndCompute(
            cartItems = cart,
            masterCatalog = masterCatalog.ifEmpty { cart.map { it.product } },
            deliveryOption = selectedDeliveryOption,
            userAccount = userAccount,
            applySpinCoupon = applySpinCoupon,
            requestedCoinsToRedeem = coinsToRedeem,
            applyWalletBalance = useWalletBalance || selectedPaymentMethod == PaymentMethod.NAVKART_WALLET,
            offersConfig = offersConfig,
            categoryDiscounts = categoryDiscounts,
            deliveryConfig = deliveryConfig,
            pinCode = pinCode
        ).getOrNull()
    }

    val payableAmount = verifiedSummary?.netGatewayPayable ?: totalPayable
    val verifiedSavings = (verifiedSummary?.wholesaleDiscount ?: totalSavings) +
        (verifiedSummary?.petiDiscountRupees ?: 0) +
        (verifiedSummary?.spinCouponDiscountRupees ?: 0) +
        (verifiedSummary?.coinDiscountRupees ?: 0)
    val isSignedInOrSandbox = currentUser != null || isTestModeActive

    LaunchedEffect(payableAmount, paymentsConfig) {
        if (selectedPaymentMethod == PaymentMethod.EMI_INSTALLMENTS && !paymentsConfig.isEligibleForEmi(payableAmount)) {
            selectedPaymentMethod = PaymentMethod.UPI_INTENT
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmCream,
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
                            text = appLanguage.tr(
                                english = "Verified Express Checkout",
                                punjabi = "ਆਰਡਰ ਅਤੇ ਭੁਗਤਾਨ ਪੁਸ਼ਟੀ",
                                hindi = "सत्यापित एक्सप्रेस चेकआउट"
                            ),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = ForestGreen)
                        )
                        Text(
                            text = if (currentUser != null) {
                                appLanguage.tr(
                                    english = "Signed in: ${currentUser.email}",
                                    punjabi = "ਸਾਈਨ ਇਨ: ${currentUser.email}",
                                    hindi = "साइन इन: ${currentUser.email}"
                                )
                            } else {
                                appLanguage.tr(
                                    english = "Gmail Sign-In required to confirm & track orders",
                                    punjabi = "ਆਰਡਰ ਪੁਸ਼ਟੀ ਅਤੇ ਟਰੈਕ ਕਰਨ ਲਈ Gmail ਸਾਈਨ-ਇਨ ਲੋੜੀਂਦਾ ਹੈ",
                                    hindi = "ऑर्डर की पुष्टि और ट्रैक करने के लिए Gmail साइन-इन आवश्यक है"
                                )
                            },
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF5A6D63))
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = appLanguage.tr("Close", "ਬੰਦ ਕਰੋ", "बंद करें")
                        )
                    }
                }

                // Required Google Sign-In Gate Banner (when guest attempts checkout)
                if (!isSignedInOrSandbox) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = SignatureGoldContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, SignatureGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_nav_brand_logo),
                                    contentDescription = "Nav Grocery Store Navkart",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = appLanguage.tr(
                                        english = "Sign in with Google (Gmail) to Continue",
                                        punjabi = "ਆਰਡਰ ਪੱਕਾ ਕਰਨ ਲਈ Google (Gmail) ਨਾਲ ਸਾਈਨ ਇਨ ਕਰੋ",
                                        hindi = "ऑर्डर कन्फर्म करने के लिए Google (Gmail) से साइन इन करें"
                                    ),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ForestGreenDark
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = appLanguage.tr(
                                    english = "One-tap Gmail authentication secures your saved Sandhaur delivery addresses, Nav Coins, and live order tracking.",
                                    punjabi = "ਇੱਕ-ਟੈਪ Gmail ਸਾਈਨ-ਇਨ ਨਾਲ ਤੁਹਾਡੇ ਸੰਧੌੜ ਪਤੇ, ਨਵ ਸਿੱਕੇ ਅਤੇ ਲਾਈਵ ਆਰਡਰ ਟਰੈਕਿੰਗ ਸੁਰੱਖਿਅਤ ਰਹਿੰਦੇ ਹਨ।",
                                    hindi = "वन-टैप Gmail साइन-इन आपके संधौर पते, नव कॉइन्स और लाइव ऑर्डर ट्रैकिंग को सुरक्षित रखता है।"
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF3A2700))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onGoogleSignInClick,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("checkout_google_signin_btn")
                            ) {
                                Text(
                                    text = appLanguage.tr(
                                        english = "Continue with Google (Gmail)",
                                        punjabi = "Google (Gmail) ਨਾਲ ਜਾਰੀ ਰੱਖੋ",
                                        hindi = "Google (Gmail) के साथ जारी रखें"
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (isTestModeActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = SignatureGoldContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚡ TABLET TEST MODE (SANDBOX): Real charges disabled for verification.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF523600),
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 24.dp)
                        ) {
                            CheckoutSummaryAndDeliveryColumn(
                                payableAmount = payableAmount,
                                deliveryFee = verifiedSummary?.deliveryFee ?: 0,
                                totalSavings = verifiedSavings,
                                deliveryConfig = deliveryConfig,
                                pinCode = pinCode,
                                onPinCodeChange = { pinCode = InputSanitizer.sanitizePin6(it) },
                                isPunjabi = isPunjabi,
                                appLanguage = appLanguage,
                                savedAddresses = savedAddresses,
                                selectedDeliveryOption = selectedDeliveryOption,
                                onDeliveryOptionChange = { selectedDeliveryOption = it },
                                selectedDispatchSlot = selectedDispatchSlot,
                                onDispatchSlotChange = { selectedDispatchSlot = it },
                                isNriOrder = isNriOrder,
                                onNriOrderChange = {
                                    isNriOrder = it
                                    if (it && selectedPaymentMethod == PaymentMethod.CASH_ON_DELIVERY) {
                                        selectedPaymentMethod = PaymentMethod.UPI_INTENT
                                    }
                                },
                                customerName = customerName,
                                onCustomerNameChange = { customerName = it },
                                customerPhone = customerPhone,
                                onCustomerPhoneChange = { customerPhone = it },
                                address = address,
                                onAddressChange = { address = it },
                                notes = notes,
                                onNotesChange = { notes = it },
                                onSelectSavedAddress = { picked ->
                                    customerName = picked.fullName
                                    customerPhone = picked.mobile
                                    address = picked.formattedLine()
                                    if (picked.pinCode.length == 6) pinCode = picked.pinCode
                                },
                                onOpenAddAddress = { showAddAddressDialog = true }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 24.dp)
                        ) {
                            CheckoutPaymentAndConfirmColumn(
                                totalPayable = payableAmount,
                                walletBalance = (userAccount?.walletBalanceRupees ?: 0L).toInt(),
                                useWalletBalance = useWalletBalance,
                                onUseWalletChange = { useWalletBalance = it },
                                paymentsConfig = paymentsConfig,
                                onShowEmiPlans = { showEmiPlansSheet = true },
                                isNriOrder = isNriOrder,
                                isPunjabi = isPunjabi,
                                appLanguage = appLanguage,
                                isTestModeActive = isTestModeActive,
                                isSubmittingOrder = isSubmittingOrder,
                                canSubmit = isSignedInOrSandbox,
                                selectedPaymentMethod = selectedPaymentMethod,
                                onPaymentMethodChange = { selectedPaymentMethod = it },
                                onConfirmOrder = {
                                    val slot = if (selectedDeliveryOption == DeliveryOption.PIND_SCHEDULED) selectedDispatchSlot else ""
                                    onConfirmOrder(
                                        customerName,
                                        customerPhone,
                                        selectedDeliveryOption,
                                        address,
                                        notes,
                                        selectedPaymentMethod,
                                        slot,
                                        isNriOrder,
                                        useWalletBalance || selectedPaymentMethod == PaymentMethod.NAVKART_WALLET,
                                        pinCode
                                    )
                                },
                                onCallHelpline = onCallHelpline
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    ) {
                        CheckoutSummaryAndDeliveryColumn(
                            payableAmount = payableAmount,
                            deliveryFee = verifiedSummary?.deliveryFee ?: 0,
                            totalSavings = verifiedSavings,
                            deliveryConfig = deliveryConfig,
                            pinCode = pinCode,
                            onPinCodeChange = { pinCode = InputSanitizer.sanitizePin6(it) },
                            isPunjabi = isPunjabi,
                            appLanguage = appLanguage,
                            savedAddresses = savedAddresses,
                            selectedDeliveryOption = selectedDeliveryOption,
                            onDeliveryOptionChange = { selectedDeliveryOption = it },
                            selectedDispatchSlot = selectedDispatchSlot,
                            onDispatchSlotChange = { selectedDispatchSlot = it },
                            isNriOrder = isNriOrder,
                            onNriOrderChange = {
                                isNriOrder = it
                                if (it && selectedPaymentMethod == PaymentMethod.CASH_ON_DELIVERY) {
                                    selectedPaymentMethod = PaymentMethod.UPI_INTENT
                                }
                            },
                            customerName = customerName,
                            onCustomerNameChange = { customerName = it },
                            customerPhone = customerPhone,
                            onCustomerPhoneChange = { customerPhone = it },
                            address = address,
                            onAddressChange = { address = it },
                            notes = notes,
                            onNotesChange = { notes = it },
                            onSelectSavedAddress = { picked ->
                                customerName = picked.fullName
                                customerPhone = picked.mobile
                                address = picked.formattedLine()
                                if (picked.pinCode.length == 6) pinCode = picked.pinCode
                            },
                            onOpenAddAddress = { showAddAddressDialog = true }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CheckoutPaymentAndConfirmColumn(
                            totalPayable = payableAmount,
                            walletBalance = (userAccount?.walletBalanceRupees ?: 0L).toInt(),
                            useWalletBalance = useWalletBalance,
                            onUseWalletChange = { useWalletBalance = it },
                            paymentsConfig = paymentsConfig,
                            onShowEmiPlans = { showEmiPlansSheet = true },
                            isNriOrder = isNriOrder,
                            isPunjabi = isPunjabi,
                            appLanguage = appLanguage,
                            isTestModeActive = isTestModeActive,
                            isSubmittingOrder = isSubmittingOrder,
                            canSubmit = isSignedInOrSandbox,
                            selectedPaymentMethod = selectedPaymentMethod,
                            onPaymentMethodChange = { selectedPaymentMethod = it },
                            onConfirmOrder = {
                                val slot = if (selectedDeliveryOption == DeliveryOption.PIND_SCHEDULED) selectedDispatchSlot else ""
                                onConfirmOrder(
                                    customerName,
                                    customerPhone,
                                    selectedDeliveryOption,
                                    address,
                                    notes,
                                    selectedPaymentMethod,
                                    slot,
                                    isNriOrder,
                                    useWalletBalance || selectedPaymentMethod == PaymentMethod.NAVKART_WALLET,
                                    pinCode
                                )
                            },
                            onCallHelpline = onCallHelpline
                        )
                    }
                }
            }
        }
    }

    if (showAddAddressDialog) {
        AddressEditorDialog(
            initialAddress = null,
            defaultName = customerName.ifBlank { currentUser?.displayName.orEmpty() },
            isPunjabi = isPunjabi,
            appLanguage = appLanguage,
            onDismiss = { showAddAddressDialog = false },
            onSave = { name, mobile, street, landmark, village, pin, makeDefault ->
                onSaveAddressClick(null, name, mobile, street, landmark, village, pin, makeDefault)
                customerName = name
                customerPhone = mobile
                pinCode = pin.ifBlank { StoreProfile.DEFAULT_PIN_CODE }
                address = listOf(street, landmark, village, "PIN $pin").filter { it.isNotBlank() }.joinToString(", ")
                showAddAddressDialog = false
            }
        )
    }

    if (showEmiPlansSheet) {
        EmiPlansBottomSheet(
            amountInr = payableAmount,
            paymentsConfig = paymentsConfig,
            appLanguage = appLanguage,
            onDismiss = { showEmiPlansSheet = false }
        )
    }
}

@Composable
private fun CheckoutSummaryAndDeliveryColumn(
    payableAmount: Int,
    deliveryFee: Int,
    totalSavings: Int,
    deliveryConfig: StoreDeliveryConfig,
    pinCode: String,
    onPinCodeChange: (String) -> Unit,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    savedAddresses: List<SavedAddress>,
    selectedDeliveryOption: DeliveryOption,
    onDeliveryOptionChange: (DeliveryOption) -> Unit,
    selectedDispatchSlot: String,
    onDispatchSlotChange: (String) -> Unit,
    isNriOrder: Boolean,
    onNriOrderChange: (Boolean) -> Unit,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    customerPhone: String,
    onCustomerPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onSelectSavedAddress: (SavedAddress) -> Unit,
    onOpenAddAddress: () -> Unit
) {
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
                    text = appLanguage.tr(
                        english = "Verified Payable Amount",
                        punjabi = "ਕੁੱਲ ਦੇਣਯੋਗ ਰਕਮ",
                        hindi = "सत्यापित देय राशि"
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(color = OnForestGreenContainer)
                )
                Text(
                    text = "₹$payableAmount",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = OnForestGreenContainer
                    )
                )
                Text(
                    text = if (deliveryFee == 0) {
                        appLanguage.tr("Delivery Fee: FREE", "ਡਿਲਿਵਰੀ ਫੀਸ: ਮੁਫ਼ਤ", "डिलीवरी शुल्क: मुफ़्त")
                    } else {
                        appLanguage.tr("Express Fee: ₹$deliveryFee", "ਐਕਸਪ੍ਰੈੱਸ ਫੀਸ: ₹$deliveryFee", "एक्सप्रेस शुल्क: ₹$deliveryFee")
                    },
                    style = MaterialTheme.typography.labelSmall.copy(color = ForestGreen, fontWeight = FontWeight.Bold)
                )
            }

            Surface(
                color = SignatureGold,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = appLanguage.tr(
                        english = "Total Savings: ₹$totalSavings",
                        punjabi = "ਕੁੱਲ ਬੱਚਤ: ₹$totalSavings",
                        hindi = "कुल बचत: ₹$totalSavings"
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF332000)
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // NRI Order for Home in Punjab Toggle
    Surface(
        color = if (isNriOrder) SignatureGoldContainer else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isNriOrder) 1.5.dp else 1.dp,
            if (isNriOrder) SignatureGold else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth().testTag("nri_family_order_card")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Public, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = appLanguage.tr(
                            english = "Ordering from Abroad for Family in Punjab",
                            punjabi = "ਵਿਦੇਸ਼ ਤੋਂ ਪੰਜਾਬ ਪਰਿਵਾਰ ਲਈ ਆਰਡਰ (NRI)",
                            hindi = "विदेश से पंजाब परिवार के लिए ऑर्डर (NRI)"
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, color = ForestGreenDark, fontSize = 12.sp)
                    )
                    Text(
                        text = appLanguage.tr(
                            english = "Pay online with Gmail & get rider delivery confirmation photo in My Orders",
                            punjabi = "Gmail ਨਾਲ ਆਨਲਾਈਨ ਭੁਗਤਾਨ ਕਰੋ ਅਤੇ My Orders ਵਿੱਚ ਡਿਲਿਵਰੀ ਫੋਟੋ ਪ੍ਰਾਪਤ ਕਰੋ",
                            hindi = "Gmail से ऑनलाइन भुगतान करें और My Orders में डिलीवरी प्रूफ फोटो प्राप्त करें"
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4B5E54), fontSize = 10.sp)
                    )
                }
            }
            Switch(
                checked = isNriOrder,
                onCheckedChange = onNriOrderChange,
                colors = SwitchDefaults.colors(checkedThumbColor = SignatureGold, checkedTrackColor = ForestGreen),
                modifier = Modifier.testTag("nri_mode_switch")
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 4 Fulfillment Tiers: 15-Min Express, 2-Hr Local, Pind-to-Pind Route, 30-Min Main Bazaar Pickup
    Text(
        text = appLanguage.tr(
            english = "Select Delivery / Pind Route / Pickup Tier:",
            punjabi = "ਡਿਲਿਵਰੀ ਵਿਕਲਪ ਚੁਣੋ:",
            hindi = "डिलीवरी / पिंड रूट / पिकअप विकल्प चुनें:"
        ),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DeliveryCard(
            icon = Icons.Default.Bolt,
            title = appLanguage.tr("15-Min Express", "15m ਐਕਸਪ੍ਰੈੱਸ", "15-मिनट एक्सप्रेस"),
            subtitle = "₹${deliveryConfig.expressDeliveryFee} (or Spin Free)",
            isSelected = selectedDeliveryOption == DeliveryOption.EXPRESS_15_MIN,
            onClick = { onDeliveryOptionChange(DeliveryOption.EXPRESS_15_MIN) },
            modifier = Modifier.weight(1f)
        )
        DeliveryCard(
            icon = Icons.Default.Home,
            title = appLanguage.tr("2-Hr Local", "2-ਘੰਟੇ ਲੋਕਲ", "2-घंटे लोकल"),
            subtitle = if (deliveryConfig.isLocalPincode(pinCode)) {
                "FREE ₹${deliveryConfig.freeDeliveryMinOrder}+"
            } else {
                "Std ₹${deliveryConfig.standardDeliveryFee}"
            },
            isSelected = selectedDeliveryOption == DeliveryOption.LOCAL_2_HR || selectedDeliveryOption == DeliveryOption.HOME_DELIVERY,
            onClick = { onDeliveryOptionChange(DeliveryOption.LOCAL_2_HR) },
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DeliveryCard(
            icon = Icons.Default.LocalShipping,
            title = appLanguage.tr("Pind-to-Pind Route", "ਪਿੰਡ-ਤੋਂ-ਪਿੰਡ ਰੂਟ", "गाँव-से-गाँव रूट"),
            subtitle = "11 AM / 6 PM Van",
            isSelected = selectedDeliveryOption == DeliveryOption.PIND_SCHEDULED,
            onClick = { onDeliveryOptionChange(DeliveryOption.PIND_SCHEDULED) },
            modifier = Modifier.weight(1f)
        )
        DeliveryCard(
            icon = Icons.Default.Store,
            title = appLanguage.tr("Store Pickup (~30m)", "ਸਟੋਰ ਪਿਕਅੱਪ (~30m)", "स्टोर पिकअप (~30m)"),
            subtitle = "Main Bazaar 148020",
            isSelected = selectedDeliveryOption == DeliveryOption.STORE_PICKUP,
            onClick = { onDeliveryOptionChange(DeliveryOption.STORE_PICKUP) },
            modifier = Modifier.weight(1f)
        )
    }

    if (selectedDeliveryOption == DeliveryOption.STORE_PICKUP) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            color = ForestGreenContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = appLanguage.tr(
                        english = "Store Pickup Ready in ~30 Mins (FREE)",
                        punjabi = "ਸਟੋਰ ਪਿਕਅੱਪ ~30 ਮਿੰਟ ਵਿੱਚ ਤਿਆਰ (ਮੁਫ਼ਤ)",
                        hindi = "स्टोर पिकअप ~30 मिनट में तैयार (मुफ़्त)"
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = ForestGreenDark)
                )
                Text(
                    text = deliveryConfig.pickupAddress,
                    style = MaterialTheme.typography.labelSmall.copy(color = OnForestGreenContainer, fontSize = 11.sp)
                )
            }
        }
    }

    if (selectedDeliveryOption == DeliveryOption.PIND_SCHEDULED) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = appLanguage.tr(
                english = "Choose Daily Pind Van Dispatch Slot:",
                punjabi = "ਰੋਜ਼ਾਨਾ ਪਿੰਡ ਵੈਨ ਡਿਸਪੈਚ ਸਮਾਂ ਚੁਣੋ:",
                hindi = "दैनिक गाँव वैन डिस्पैच समय चुनें:"
            ),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StoreProfile.PIND_DISPATCH_SLOTS.forEach { slot ->
                FilterChip(
                    selected = selectedDispatchSlot == slot,
                    onClick = { onDispatchSlotChange(slot) },
                    label = { Text(slot, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
    }

    // Saved Addresses Auto-Fill Strip
    if (selectedDeliveryOption != DeliveryOption.STORE_PICKUP) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appLanguage.tr(
                    english = "Saved Delivery Addresses",
                    punjabi = "ਸੁਰੱਖਿਅਤ ਪਤੇ (Saved Addresses)",
                    hindi = "सहेजे गए डिलीवरी पते"
                ),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
            )
            TextButton(onClick = onOpenAddAddress, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = ForestGreen)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = appLanguage.tr("+ Add New", "+ ਨਵਾਂ ਜੋੜੋ", "+ नया जोड़ें"),
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                    fontSize = 12.sp
                )
            }
        }

        if (savedAddresses.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savedAddresses.forEach { saved ->
                    val isCurrent = address.contains(saved.streetAddress)
                    FilterChip(
                        selected = isCurrent,
                        onClick = { onSelectSavedAddress(saved) },
                        label = {
                            Text(
                                text = "${saved.fullName} • ${saved.pinCode}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = customerName,
        onValueChange = onCustomerNameChange,
        label = {
            Text(
                appLanguage.tr(
                    english = "Recipient Full Name*",
                    punjabi = "ਪੂਰਾ ਨਾਮ (Full Name)*",
                    hindi = "प्राप्तकर्ता का पूरा नाम*"
                )
            )
        },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("checkout_name_input")
    )

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
        value = customerPhone,
        onValueChange = { onCustomerPhoneChange(it.filter { c -> c.isDigit() }.take(10)) },
        label = {
            Text(
                appLanguage.tr(
                    english = "10-Digit Indian Mobile Number*",
                    punjabi = "10-ਅੰਕੀ ਮੋਬਾਈਲ ਨੰਬਰ*",
                    hindi = "10-अंकीय भारतीय मोबाइल नंबर*"
                )
            )
        },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input")
    )

    if (selectedDeliveryOption != DeliveryOption.STORE_PICKUP) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = pinCode,
            onValueChange = { onPinCodeChange(InputSanitizer.sanitizePin6(it)) },
            label = {
                Text(
                    appLanguage.tr(
                        english = "Delivery PIN Code (6 digits, e.g. 148020)*",
                        punjabi = "ਡਿਲਿਵਰੀ ਪਿੰਨ ਕੋਡ (6 ਅੰਕ, ਜਿਵੇਂ 148020)*",
                        hindi = "डिलीवरी पिन कोड (6 अंक, जैसे 148020)*"
                    )
                )
            },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("checkout_pincode_input")
        )
        Spacer(modifier = Modifier.height(6.dp))
        val isLocalPin = deliveryConfig.isLocalPincode(pinCode)
        Surface(
            color = if (isLocalPin) ForestGreenContainer else SignatureGoldContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    pinCode.length < 6 -> appLanguage.tr(
                        english = "Enter 6-digit PIN code to verify delivery zone & fee",
                        punjabi = "ਡਿਲਿਵਰੀ ਖੇਤਰ ਅਤੇ ਫੀਸ ਦੀ ਜਾਂਚ ਲਈ 6-ਅੰਕੀ ਪਿੰਨ ਦਰਜ ਕਰੋ",
                        hindi = "डिलीवरी क्षेत्र और शुल्क जांचने के लिए 6-अंकीय पिन कोड दर्ज करें"
                    )
                    isLocalPin -> appLanguage.tr(
                        english = "Sandhaur Local Zone ($pinCode) • ~2-Hr Express • FREE above ₹${deliveryConfig.freeDeliveryMinOrder} (else ₹${deliveryConfig.localDeliveryFee})",
                        punjabi = "ਸੰਧੌੜ ਲੋਕਲ ਜ਼ੋਨ ($pinCode) • ~2 ਘੰਟੇ ਡਿਲਿਵਰੀ • ₹${deliveryConfig.freeDeliveryMinOrder}+ 'ਤੇ ਮੁਫ਼ਤ",
                        hindi = "संधौड़ लोकल ज़ोन ($pinCode) • ~2 घंटे डिलीवरी • ₹${deliveryConfig.freeDeliveryMinOrder}+ पर मुफ़्त"
                    )
                    else -> appLanguage.tr(
                        english = "Standard Zone ($pinCode) • ₹${deliveryConfig.standardDeliveryFee} fee (FREE on ₹${deliveryConfig.freeDeliveryMinOrder}+)",
                        punjabi = "ਸਟੈਂਡਰਡ ਜ਼ੋਨ ($pinCode) • ₹${deliveryConfig.standardDeliveryFee} ਫੀਸ (₹${deliveryConfig.freeDeliveryMinOrder}+ 'ਤੇ ਮੁਫ਼ਤ)",
                        hindi = "स्टैंडर्ड ज़ोन ($pinCode) • ₹${deliveryConfig.standardDeliveryFee} शुल्क (₹${deliveryConfig.freeDeliveryMinOrder}+ पर मुफ़्त)"
                    )
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenDark,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = {
                Text(
                    appLanguage.tr(
                        english = "Delivery Street, Village & PIN*",
                        punjabi = "ਪਤਾ / ਪਿੰਡ / PIN*",
                        hindi = "पता / गाँव / PIN*"
                    )
                )
            },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("checkout_address_input")
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StoreProfile.NEARBY_VILLAGES.forEach { village ->
                FilterChip(
                    selected = address.contains(village.split(" ")[0]),
                    onClick = {
                        onAddressChange("$village, District Malerkotla, Punjab, PIN ${pinCode.ifBlank { "148020" }}")
                    },
                    label = { Text(village, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = {
            Text(
                appLanguage.tr(
                    english = "Landmark / Instructions (Optional)",
                    punjabi = "ਨੋਟ / ਲੈਂਡਮਾਰਕ (ਵਿਕਲਪਿਕ)",
                    hindi = "लैंडमार्क / निर्देश (वैकल्पिक)"
                )
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CheckoutPaymentAndConfirmColumn(
    totalPayable: Int,
    walletBalance: Int,
    useWalletBalance: Boolean,
    onUseWalletChange: (Boolean) -> Unit,
    paymentsConfig: StorePaymentsConfig,
    onShowEmiPlans: () -> Unit,
    isNriOrder: Boolean,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    isTestModeActive: Boolean,
    isSubmittingOrder: Boolean,
    canSubmit: Boolean,
    selectedPaymentMethod: PaymentMethod,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onConfirmOrder: () -> Unit,
    onCallHelpline: () -> Unit
) {
    val context = LocalContext.current

    Text(
        text = appLanguage.tr(
            english = "Secure Payment Method:",
            punjabi = "ਭੁਗਤਾਨ ਦਾ ਢੰਗ ਚੁਣੋ:",
            hindi = "सुरक्षित भुगतान का तरीका चुनें:"
        ),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (walletBalance > 0) {
        Surface(
            color = ForestGreenContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Deduct Prepaid Family Wallet (₹$walletBalance avail)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ForestGreenDark),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = useWalletBalance,
                    onCheckedChange = onUseWalletChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = SignatureGold, checkedTrackColor = ForestGreen)
                )
            }
        }
    }

    PaymentMethod.entries.forEach { method ->
        val enabled = when (method) {
            PaymentMethod.CASH_ON_DELIVERY -> !isNriOrder
            PaymentMethod.EMI_INSTALLMENTS -> paymentsConfig.isEligibleForEmi(totalPayable)
            else -> true
        }
        val extraHint = when (method) {
            PaymentMethod.NAVKART_WALLET -> "Wallet Balance: ₹$walletBalance"
            PaymentMethod.CASH_ON_DELIVERY -> if (isNriOrder) "Use online UPI/Wallet for NRI family orders" else method.description
            PaymentMethod.EMI_INSTALLMENTS -> when {
                !paymentsConfig.isEmiEnabled -> "EMI temporarily disabled by store config"
                totalPayable < paymentsConfig.sanitizedMinEmiOrderAmount ->
                    "Min order ₹${paymentsConfig.sanitizedMinEmiOrderAmount} required (Bajaj Finserv EMI Card & Bank EMI)"
                else ->
                    "From ₹${paymentsConfig.emiStartingPerMonth(totalPayable)}/mo • ${paymentsConfig.effectiveProviders.take(4).joinToString(", ")}"
            }
            else -> method.description
        }
        PaymentOptionRow(
            method = method,
            appLanguage = appLanguage,
            subtitleOverride = extraHint,
            enabled = enabled,
            isSelected = selectedPaymentMethod == method,
            onClick = { if (enabled) onPaymentMethodChange(method) }
        )
        Spacer(modifier = Modifier.height(6.dp))
    }

    if (paymentsConfig.isEligibleForEmi(totalPayable)) {
        Spacer(modifier = Modifier.height(6.dp))
        EmiEligibilityBadge(
            amountInr = totalPayable,
            paymentsConfig = paymentsConfig,
            appLanguage = appLanguage,
            onViewPlansClick = onShowEmiPlans
        )
    }

    if ((selectedPaymentMethod == PaymentMethod.UPI_INTENT || selectedPaymentMethod == PaymentMethod.UPI_QR) && !isTestModeActive) {
        val upiUriStr = remember(totalPayable) {
            "upi://pay?pa=${StoreProfile.MERCHANT_UPI_VPA}&pn=NavKart%20Sandhaur&am=$totalPayable&cu=INR&tn=NavKart%20Verified%20Order"
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan Live UPI QR or Launch App (₹$totalPayable)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
                )
                Spacer(modifier = Modifier.height(8.dp))
                UpiVerificationQrMatrix(upiUriPayload = upiUriStr, sizeDp = 136.dp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(upiUriStr)))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Open GPay/PhonePe and pay ₹$totalPayable to ${StoreProfile.MERCHANT_UPI_VPA}", Toast.LENGTH_LONG).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = ForestGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pay ₹$totalPayable via GPay / PhonePe / Paytm", color = ForestGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onConfirmOrder,
        enabled = canSubmit && !isSubmittingOrder,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8A3C)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("submit_whatsapp_order_btn")
    ) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = when {
                isSubmittingOrder -> appLanguage.tr(
                    english = "Securing & Placing Order...",
                    punjabi = "ਆਰਡਰ ਸੁਰੱਖਿਅਤ ਅਤੇ ਦਰਜ ਹੋ ਰਿਹਾ ਹੈ...",
                    hindi = "ऑर्डर सुरक्षित और दर्ज किया जा रहा है..."
                )
                isTestModeActive -> "Run Tablet Test Order (Sandbox)"
                !canSubmit -> appLanguage.tr(
                    english = "Sign In with Google Above First",
                    punjabi = "ਪਹਿਲਾਂ ਉੱਪਰ Google ਨਾਲ ਸਾਈਨ ਇਨ ਕਰੋ",
                    hindi = "पहले ऊपर Google से साइन इन करें"
                )
                else -> appLanguage.tr(
                    english = "Confirm Order • ₹$totalPayable",
                    punjabi = "ਆਰਡਰ ਪੱਕਾ ਕਰੋ • ₹$totalPayable",
                    hindi = "ऑर्डर कन्फर्म करें • ₹$totalPayable"
                )
            },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = Color.White)
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
        onClick = onCallHelpline,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(46.dp)
    ) {
        Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = ForestGreen)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = appLanguage.tr(
                english = "Call Store Helpline (+91 8847370905)",
                punjabi = "ਸਟੋਰ ਹੈਲਪਲਾਈਨ: +91 8847370905",
                hindi = "स्टोर हेल्पलाइन: +91 8847370905"
            ),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = ForestGreen)
        )
    }
}

@Composable
fun OrderReceiptDialog(
    order: Order,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified Receipt",
                tint = ForestGreen,
                modifier = Modifier.size(44.dp)
            )
        },
        title = {
            Text(
                text = appLanguage.tr(
                    english = "Verified Order Confirmed!",
                    punjabi = "ਆਰਡਰ ਪ੍ਰਮਾਣਿਤ ਅਤੇ ਦਰਜ ਹੋ ਗਿਆ!",
                    hindi = "सत्यापित ऑर्डर कन्फर्म हो गया!"
                ),
                fontWeight = FontWeight.ExtraBold,
                color = ForestGreen
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Order ID: ${order.orderId} • GSTIN: ${StoreProfile.GSTIN}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Payment: ${order.paymentMethod}", fontSize = 12.sp)
                Text("Delivery Tier: ${order.deliveryType}${if (order.dispatchSlot.isNotBlank()) " • ${order.dispatchSlot}" else ""}")
                if (order.isNriOrder) {
                    Text("NRI Family Delivery Order for Punjab", fontWeight = FontWeight.Bold, color = ForestGreen)
                }
                Text("Recipient: ${order.customerName} (+91 ${order.customerPhone})")
                Text("Address: ${order.address}", fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Total MRP: ₹${order.totalMrp}")
                Text("Wholesale & Bulk Rebates: -₹${order.totalDiscount}", color = Color(0xFFC62828))
                if (order.petiDiscountRupees > 0) {
                    Text("Includes Peti 3% Extra Rebate: -₹${order.petiDiscountRupees}", color = ForestGreen)
                }
                if (order.spinCouponRupees > 0) {
                    Text("Spin Coupon (${order.spinCouponCode}): -₹${order.spinCouponRupees}", color = ForestGreen)
                }
                if (order.coinsRedeemed > 0) {
                    Text("Nav Coins Redeemed: -₹${order.coinsRedeemed}", color = ForestGreen)
                }
                if (order.walletDeductedRupees > 0) {
                    Text("Family Wallet Deducted: -₹${order.walletDeductedRupees}", color = ForestGreen)
                }
                Text("Delivery Fee: ${if (order.deliveryFee == 0) "FREE" else "₹${order.deliveryFee}"}")
                Text(
                    text = appLanguage.tr(
                        english = "FINAL ORDER TOTAL: ₹${order.finalTotal}",
                        punjabi = "ਅੰਤਿਮ ਆਰਡਰ ਕੁੱਲ: ₹${order.finalTotal}",
                        hindi = "अंतिम ऑर्डर कुल: ₹${order.finalTotal}"
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = ForestGreen
                )
                if (order.integrityDigest.isNotBlank()) {
                    Text(
                        text = "Integrity Digest: ${order.integrityDigest.take(18)}...",
                        fontSize = 10.sp,
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
                Text(appLanguage.tr("Done", "ਠੀਕ ਹੈ", "हो गया"))
            }
        }
    )
}

@Composable
private fun PaymentOptionRow(
    method: PaymentMethod,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    subtitleOverride: String = method.description,
    enabled: Boolean = true,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = when {
            !enabled -> Color(0xFFF1F1ED)
            isSelected -> ForestGreenContainer.copy(alpha = 0.55f)
            else -> Color.White
        },
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
                enabled = enabled,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = method.titleFor(appLanguage),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface else Color(0xFF7B8C83)
                        )
                    )
                    if (method.badge.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = SignatureGold, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = method.badge,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ForestGreenDark,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }
                Text(
                    text = subtitleOverride,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFC62828),
                        fontSize = 10.sp
                    ),
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
        color = if (isSelected) ForestGreenContainer else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) OnForestGreenContainer else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isSelected) OnForestGreenContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.5.sp
                ),
                maxLines = 1
            )
        }
    }
}
