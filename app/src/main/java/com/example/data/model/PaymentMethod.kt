package com.example.data.model

enum class PaymentMethod(
    val title: String,
    val punjabiTitle: String,
    val hindiTitle: String,
    val description: String,
    val badge: String = "",
    val isGatewayHosted: Boolean = false
) {
    UPI_INTENT(
        title = "UPI (Google Pay / PhonePe / Paytm / BHIM)",
        punjabiTitle = "ਯੂ.ਪੀ.ਆਈ (ਗੂਗਲ ਪੇ / ਫੋਨਪੇ)",
        hindiTitle = "यूपीआई (गूगल पे / फोनपे / पेटीएम)",
        description = "Direct 0% fee instant bank transfer via NavKart Verified Merchant Escrow",
        badge = "Instant 0% Fee"
    ),
    UPI_QR(
        title = "Merchant Dynamic QR Code",
        punjabiTitle = "ਵਪਾਰੀ ਕਿਊ ਆਰ ਕੋਡ",
        hindiTitle = "मर्चेंट डायनेमिक QR कोड",
        description = "Scan & Pay using any UPI app via NavKart Verified Merchant Escrow",
        badge = "Scan & Pay"
    ),
    CREDIT_DEBIT_CARD(
        title = "Credit / Debit Card (Gateway Checkout)",
        punjabiTitle = "ਕ੍ਰੈਡਿਟ / ਡੈਬਿਟ ਕਾਰਡ",
        hindiTitle = "क्रेडिट / डेबिट कार्ड (सुरक्षित गेटवे)",
        description = "Visa, MasterCard, RuPay processed securely via PCI-DSS certified payment gateway",
        isGatewayHosted = true
    ),
    NET_BANKING(
        title = "Net Banking",
        punjabiTitle = "ਨੈੱਟ ਬੈਂਕਿੰਗ",
        hindiTitle = "नेट बैंकिंग",
        description = "SBI, HDFC, ICICI, PNB, Punjab Gramin Bank & all major banks",
        isGatewayHosted = true
    ),
    NAVKART_WALLET(
        title = "Prepaid NavKart Family Wallet",
        punjabiTitle = "ਪ੍ਰੀਪੇਡ ਨਵਕਾਰਟ ਫੈਮਿਲੀ ਵਾਲਿਟ",
        hindiTitle = "प्रीपेड नवकार्ट फैमिली वॉलेट",
        description = "1-tap balance checkout loaded via UPI with instant Bonus Nav Coins",
        badge = "1-Tap Pay"
    ),
    ONLINE_WALLETS(
        title = "Wallets (Paytm / PhonePe Wallet)",
        punjabiTitle = "ਆਨਲਾਈਨ ਵਾਲਿਟ",
        hindiTitle = "ऑनलाइन वॉलेट",
        description = "Pay via digital wallet balance on hosted checkout",
        isGatewayHosted = true
    ),
    EMI_INSTALLMENTS(
        title = "Pay in Easy Installments (Bajaj Finserv EMI Card / Bank EMI)",
        punjabiTitle = "ਆਸਾਨ ਕਿਸ਼ਤਾਂ (ਬਜਾਜ ਫਿਨਸਰਵ EMI ਕਾਰਡ / ਬੈਂਕ EMI)",
        hindiTitle = "आसान किश्तें (बजाज फिनसर्व EMI कार्ड / बैंक EMI)",
        description = "3 / 6 / 9 / 12-month installments via Bajaj Finserv EMI Card, HDFC, SBI, ICICI, Axis & Kotak on RBI-licensed gateway",
        badge = "Bajaj / Bank EMI",
        isGatewayHosted = true
    ),
    CASH_ON_DELIVERY(
        title = "Cash on Delivery (COD) / Pay on Pickup",
        punjabiTitle = "ਕੈਸ਼ ਆਨ ਡਿਲਿਵਰੀ",
        hindiTitle = "कैश ऑन डिलीवरी / पिकअप पर भुगतान",
        description = "Pay cash or scan QR at your doorstep or Main Bazaar store counter"
    ),
    WHATSAPP_ORDER(
        title = "WhatsApp Instant Order & Pay Later",
        punjabiTitle = "ਵਟਸਐਪ ਤੁਰੰਤ ਆਰਡਰ",
        hindiTitle = "व्हाट्सएप इंस्टेंट ऑर्डर",
        description = "Confirm & dispatch itemized order summary directly on +91 8847370905",
        badge = "Fast Dispatch"
    );

    fun titleFor(language: AppLanguage): String = when (language) {
        AppLanguage.PUNJABI -> punjabiTitle
        AppLanguage.HINDI -> hindiTitle
        AppLanguage.ENGLISH -> title
    }
}
