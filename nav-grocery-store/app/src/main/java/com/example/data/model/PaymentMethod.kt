package com.example.data.model

enum class PaymentMethod(
    val title: String,
    val punjabiTitle: String,
    val description: String,
    val badge: String = ""
) {
    UPI_INTENT(
        title = "UPI (Google Pay / PhonePe / Paytm / BHIM)",
        punjabiTitle = "ਯੂ.ਪੀ.ਆਈ (ਗੂਗਲ ਪੇ / ਫੋਨਪੇ)",
        description = "Direct 0% fee instant bank transfer via NavKart Verified Merchant Escrow",
        badge = "Instant 0% Fee"
    ),
    UPI_QR(
        title = "Merchant Dynamic QR Code",
        punjabiTitle = "ਵਪਾਰੀ ਕਿਊ ਆਰ ਕੋਡ",
        description = "Scan & Pay using any UPI app via NavKart Verified Merchant Escrow",
        badge = "Scan & Pay"
    ),
    CREDIT_DEBIT_CARD(
        title = "Credit / Debit Card",
        punjabiTitle = "ਕ੍ਰੈਡਿਟ / ਡੈਬਿਟ ਕਾਰਡ",
        description = "Visa, MasterCard, RuPay with 256-bit sandbox encryption"
    ),
    NET_BANKING(
        title = "Net Banking",
        punjabiTitle = "ਨੈੱਟ ਬੈਂਕਿੰਗ",
        description = "SBI, HDFC, ICICI, PNB, Punjab Gramin Bank & all major banks"
    ),
    NAVKART_WALLET(
        title = "NavKart Store Wallet & Coins",
        punjabiTitle = "ਨਵਕਾਰਟ ਵਾਲਿਟ ਅਤੇ ਸਿੱਕੇ",
        description = "Use preloaded store balance & loyalty cashback",
        badge = "Preloaded"
    ),
    ONLINE_WALLETS(
        title = "Wallets (Paytm / PhonePe Wallet)",
        punjabiTitle = "ਆਨਲਾਈਨ ਵਾਲਿਟ",
        description = "Pay via digital wallet balance"
    ),
    EMI_INSTALLMENTS(
        title = "Easy EMI (3 / 6 / 12 Months)",
        punjabiTitle = "ਕਿਸ਼ਤਾਂ (EMI)",
        description = "Flexible monthly installments for retail & electronics"
    ),
    CASH_ON_DELIVERY(
        title = "Cash on Delivery (COD) / Pay on Pickup",
        punjabiTitle = "ਕੈਸ਼ ਆਨ ਡਿਲਿਵਰੀ",
        description = "Pay cash or scan QR at your doorstep in Sandhaur"
    )
}
