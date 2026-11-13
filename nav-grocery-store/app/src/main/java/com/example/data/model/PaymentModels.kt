package com.example.data.model

import com.squareup.moshi.JsonClass

enum class PaymentOptionType(val title: String, val punjabiTitle: String, val subtitle: String) {
    UPI("UPI (Instant 0% Fee)", "ਯੂ.ਪੀ.ਆਈ (ਤੁਰੰਤ ਭੁਗਤਾਨ)", "Google Pay, PhonePe, Paytm, BHIM, QR"),
    CARD("Credit / Debit Cards", "ਕ੍ਰੈਡਿਟ / ਡੈਬਿਟ ਕਾਰਡ", "Visa, Mastercard, RuPay, American Express"),
    NETBANKING("NetBanking", "ਨੈੱਟ ਬੈਂਕਿੰਗ", "All Major Indian Banks"),
    WALLET("Digital Wallets", "ਡਿਜੀਟਲ ਵਾਲੇਟ", "Paytm, PhonePe, Amazon Pay, Mobikwik"),
    EMI("EMI & Pay Later", "ਈ.ਐਮ.ਆਈ ਅਤੇ ਬਾਅਦ ਵਿੱਚ ਭੁਗਤਾਨ", "No-Cost EMI & NavKart Pay Later"),
    STORE_WALLET("NavKart Wallet & Cashback", "ਸਟੋਰ ਵਾਲੇਟ ਅਤੇ ਕੈਸ਼ਬੈਕ", "Instant checkout using stored balance"),
    COD("Cash on Delivery (COD)", "ਕੈਸ਼ ਆਨ ਡਿਲਿਵਰੀ", "Verified OTP or Pay via UPI at Doorstep")
}

@JsonClass(generateAdapter = true)
data class SavedCard(
    val id: String,
    val cardType: String, // "VISA", "MASTERCARD", "RUPAY", "AMEX"
    val lastFour: String,
    val cardHolder: String,
    val expiry: String
)

@JsonClass(generateAdapter = true)
data class PaymentTransaction(
    val transactionId: String,
    val orderId: String,
    val amount: Int,
    val paymentMethod: String,
    val paymentStatus: String, // "SUCCESS", "PENDING", "FAILED", "REFUNDED"
    val merchantAccount: String = CommissionConfig.CENTRAL_MERCHANT_NAME,
    val gstin: String = OwnerSecureLedger.GSTIN,
    val utrOrRef: String,
    val timestamp: Long = System.currentTimeMillis(),
    val platformCommission: Int,
    val netSellerPayable: Int
)

data class EmiPlan(
    val months: Int,
    val interestRate: Double,
    val isNoCost: Boolean,
    val bankName: String
) {
    fun calculateMonthlyAmount(totalAmount: Int): Int {
        return if (isNoCost || interestRate == 0.0) {
            (totalAmount.toDouble() / months).toInt()
        } else {
            val totalWithInterest = totalAmount * (1 + (interestRate / 100))
            (totalWithInterest / months).toInt()
        }
    }
}

object BankDirectory {
    val TOP_BANKS = listOf(
        "State Bank of India (SBI)",
        "HDFC Bank",
        "ICICI Bank",
        "Axis Bank",
        "Punjab National Bank (PNB)",
        "Kotak Mahindra Bank",
        "Bank of Baroda",
        "Canara Bank"
    )

    val OTHER_BANKS = listOf(
        "Union Bank of India",
        "Punjab & Sind Bank",
        "IndusInd Bank",
        "Yes Bank",
        "Federal Bank",
        "IDBI Bank",
        "Indian Overseas Bank",
        "Central Bank of India",
        "Bank of India",
        "UCO Bank",
        "AU Small Finance Bank"
    )

    val ALL_BANKS = TOP_BANKS + OTHER_BANKS
}
