package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val phone: String = "",
    val referralCode: String = "",
    val referredBy: String = "",
    val fcmToken: String = "",
    val fcmTokenUpdatedAtMs: Long = 0L,
    val walletBalanceRupees: Long = 0L,
    val walletSpentRupees: Long = 0L,
    val navCoins: Long = 0L,
    val lastSpinAtMs: Long = 0L,
    val spinCount: Long = 0L,
    val activeSpinCouponCode: String = "",
    val activeSpinCouponLabel: String = "",
    val activeSpinCouponDiscountRupees: Long = 0L,
    val activeSpinFreeExpress: Boolean = false,
    val activeSpinFreeLocal: Boolean = false,
    val activeSpinUsed: Boolean = false,
    val hasOrderedGrocery: Boolean = false,
    val hasOrderedElectronics: Boolean = false,
    val coldChainStaplesCount: Long = 0L,
    val weeklyOrdersCount: Long = 0L,
    val weeklyWindowStartMs: Long = 0L,
    val totalOrdersCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun hasValidSpinCoupon(): Boolean =
        !activeSpinUsed && activeSpinCouponCode.isNotBlank() &&
            (activeSpinCouponDiscountRupees > 0L || activeSpinFreeExpress || activeSpinFreeLocal)

    fun canSpinNow(nowMs: Long = System.currentTimeMillis()): Boolean =
        lastSpinAtMs <= 0L || (nowMs - lastSpinAtMs) >= SPIN_COOLDOWN_MS

    fun millisUntilNextSpin(nowMs: Long = System.currentTimeMillis()): Long =
        if (canSpinNow(nowMs)) 0L else (SPIN_COOLDOWN_MS - (nowMs - lastSpinAtMs)).coerceAtLeast(0L)

    fun effectiveReferralCode(): String =
        referralCode.ifBlank {
            val cleanUid = uid.replace(Regex("[^a-zA-Z0-9]"), "").uppercase()
            "PIND-${cleanUid.take(6).ifBlank { "148020" }}"
        }

    companion object {
        const val SPIN_COOLDOWN_MS = 86_400_000L
    }
}

@Keep
@IgnoreExtraProperties
data class SavedAddress(
    @DocumentId val id: String = "",
    val uid: String = "",
    val fullName: String = "",
    val mobile: String = "",
    val streetAddress: String = "",
    val landmark: String = "",
    val villageOrCity: String = StoreProfile.DEFAULT_CITY_STATE,
    val pinCode: String = StoreProfile.DEFAULT_PIN_CODE,
    val defaultAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun formattedLine(): String =
        listOf(streetAddress, landmark, villageOrCity, "PIN $pinCode")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(", ")
}

@Keep
@IgnoreExtraProperties
data class FirestoreOrderItem(
    val productId: String = "",
    val name: String = "",
    val department: String = "",
    val category: String = "",
    val qty: Int = 1,
    val mrp: Int = 0,
    val wholesalePrice: Int = 0,
    val variant: String = "",
    val imageUrl: String = ""
)

@Keep
@IgnoreExtraProperties
data class FirestoreOrderRecord(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val uid: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val pinCode: String = "",
    val anonymized: Boolean = false,
    val deliveryTier: String = DeliveryOption.LOCAL_2_HR.name,
    val dispatchSlot: String = "",
    val isNriOrder: Boolean = false,
    val nriBuyerEmail: String = "",
    val deliveryProofUrl: String = "",
    val addressSnapshot: String = "",
    val paymentMethod: String = PaymentMethod.UPI_INTENT.name,
    val status: String = "Order Placed",
    val totalMrp: Long = 0L,
    val wholesaleDiscount: Long = 0L,
    val petiDiscountRupees: Long = 0L,
    val spinCouponDiscountRupees: Long = 0L,
    val spinCouponCode: String = "",
    val coinsRedeemed: Long = 0L,
    val walletDeductedRupees: Long = 0L,
    val deliveryFee: Long = 0L,
    val finalTotal: Long = 0L,
    val coinsEarned: Long = 0L,
    val integrityDigest: String = "",
    val items: List<FirestoreOrderItem> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
data class ParchiRequest(
    val requestId: String = "",
    val uid: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val mode: String = "VOICE_OR_TEXT", // PARCHI_PHOTO | PUNJABI_VOICE
    val notesText: String = "",
    val photoDataUrl: String = "",
    val voiceTranscript: String = "",
    val status: String = "SUBMITTED", // SUBMITTED | BILL_READY | CONFIRMED
    val matchedSummary: String = "",
    val readyBillTotal: Int = 0,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Keep
data class RashanSubscription(
    val uid: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val monthlyDayOfMonth: Int = 5,
    val addressSnapshot: String = "",
    val itemsSummary: String = "",
    val productIdsCsv: String = "",
    val estimatedTotal: Int = 0,
    val active: Boolean = true,
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class SmartComboHamper(
    val id: String,
    val title: String,
    val punjabiTitle: String,
    val hindiTitle: String = "",
    val subtitle: String,
    val badge: String,
    val skuIds: List<String>,
    val bonusCoins: Int
) {
    fun displayTitle(language: AppLanguage): String = when (language) {
        AppLanguage.PUNJABI -> punjabiTitle.ifBlank { title }
        AppLanguage.HINDI -> hindiTitle.ifBlank { title }
        AppLanguage.ENGLISH -> title
    }

    companion object {
        val FEATURED_HAMPERS = listOf(
            SmartComboHamper(
                id = "COMBO_VIAH_SHAGUN",
                title = "Punjab Shagun & Family Gift Hamper",
                punjabiTitle = "ਪੰਜਾਬ ਸ਼ਗਨ & ਫੈਮਿਲੀ ਗਿਫਟ ਹੈਂਪਰ",
                hindiTitle = "पंजाब शगुन और फैमिली गिफ्ट हैम्पर",
                subtitle = "Amul Ghee + Britannia Biscuits + Kettle/Cookware Trio • Priority Ribbon Pack",
                badge = "Shagun Special",
                skuIds = listOf("NAV001", "NAV018", "NAV309", "NAV681"),
                bonusCoins = 40
            ),
            SmartComboHamper(
                id = "COMBO_CHAI_NASHTA_KITCHEN",
                title = "Chai-Nashta + Smart Kitchen Appliance Combo",
                punjabiTitle = "ਚਾਹ-ਨਾਸ਼ਤਾ + ਸਮਾਰਟ ਰਸੋਈ ਕੌਂਬੋ",
                hindiTitle = "चाय-नाश्ता + स्मार्ट किचन कॉम्बो",
                subtitle = "Tata Tea + Biscuits + Electric Kettle / Toaster Utility Bundle",
                badge = "Home + Grocery",
                skuIds = listOf("NAV012", "NAV025", "NAV316", "NAV685"),
                bonusCoins = 30
            ),
            SmartComboHamper(
                id = "COMBO_MAHINA_RASHAN_STARTER",
                title = "Sandhaur Mahina Rashan Monthly Starter Box",
                punjabiTitle = "ਸੰਧੌੜ ਮਹੀਨਾ ਰਾਸ਼ਨ ਸਟਾਰਟਰ ਬਾਕਸ",
                hindiTitle = "संधौड़ महीना राशन स्टार्टर बॉक्स",
                subtitle = "Aashirvaad Chakki Atta + Basmati Rice + Fortune Oil + Toor Dal +Tata Salt",
                badge = "Monthly Box",
                skuIds = listOf("NAV309", "NAV313", "NAV320", "NAV316", "NAV328"),
                bonusCoins = 50
            ),
            SmartComboHamper(
                id = "COMBO_NRI_PARENTS_CARE",
                title = "NRI Parents Wellness & Sealed Pantry Care Pack",
                punjabiTitle = "NRI ਮਾਪਿਆਂ ਦੀ ਸੇਵਾ & ਪੈਂਟਰੀ ਕੇਅਰ ਪੈਕ",
                hindiTitle = "NRI माता-पिता की सेवा और पैंट्री केयर पैक",
                subtitle = "Pure Desi Ghee, Honey, Boost & Sealed Health Packs delivered in Sandhaur 148020",
                badge = "NRI Favorite",
                skuIds = listOf("NAV004", "NAV324", "NAV325", "NAV340"),
                bonusCoins = 50
            )
        )
    }
}

enum class SpinWheelSlice(
    val id: String,
    val title: String,
    val punjabiTitle: String,
    val hindiTitle: String,
    val couponCode: String,
    val discountRupees: Int,
    val bonusCoins: Int,
    val freeExpress: Boolean = false,
    val freeLocal: Boolean = false
) {
    EXTRA_20_OFF(
        id = "SLICE_20_OFF",
        title = "Extra ₹20 Off Next Order",
        punjabiTitle = "ਅਗਲੇ ਆਰਡਰ 'ਤੇ ₹20 ਵਾਧੂ ਛੋਟ",
        hindiTitle = "अगले ऑर्डर पर ₹20 अतिरिक्त छूट",
        couponCode = "SPIN20",
        discountRupees = 20,
        bonusCoins = 0
    ),
    FREE_15MIN_EXPRESS(
        id = "SLICE_FREE_EXPRESS",
        title = "Free 15-Minute Express Delivery",
        punjabiTitle = "ਮੁਫ਼ਤ 15-ਮਿੰਟ ਐਕਸਪ੍ਰੈੱਸ ਡਿਲਿਵਰੀ",
        hindiTitle = "मुफ़्त 15-मिनट एक्सप्रेस डिलीवरी",
        couponCode = "SPINEXP",
        discountRupees = 0,
        bonusCoins = 0,
        freeExpress = true
    ),
    COINS_50_BONUS(
        id = "SLICE_50_COINS",
        title = "50 Nav Coins Bonus",
        punjabiTitle = "50 ਨਵ ਕੌਇਨਜ਼ ਬੋਨਸ",
        hindiTitle = "50 नव कॉइन्स बोनस",
        couponCode = "",
        discountRupees = 0,
        bonusCoins = 50
    ),
    COINS_100_JACKPOT(
        id = "SLICE_100_JACKPOT",
        title = "100 Nav Coins Jackpot",
        punjabiTitle = "100 ਨਵ ਕੌਇਨਜ਼ ਜੈਕਪਾਟ",
        hindiTitle = "100 नव कॉइन्स जैकपॉट",
        couponCode = "",
        discountRupees = 0,
        bonusCoins = 100
    ),
    FREE_2HR_LOCAL(
        id = "SLICE_FREE_LOCAL",
        title = "Free Delivery on 2-Hour Local Order",
        punjabiTitle = "2-ਘੰਟੇ ਲੋਕਲ ਆਰਡਰ 'ਤੇ ਮੁਫ਼ਤ ਡਿਲਿਵਰੀ",
        hindiTitle = "2-घंटे लोकल ऑर्डर पर मुफ़्त डिलीवरी",
        couponCode = "SPINLOCAL",
        discountRupees = 10,
        bonusCoins = 0,
        freeLocal = true
    ),
    TRY_AGAIN_TOMORROW(
        id = "SLICE_RETRY",
        title = "Try Again Tomorrow",
        punjabiTitle = "ਕੱਲ੍ਹ ਦੁਬਾਰਾ ਕੋਸ਼ਿਸ਼ ਕਰੋ",
        hindiTitle = "कल फिर से कोशिश करें",
        couponCode = "",
        discountRupees = 0,
        bonusCoins = 5 // Consolation loyalty point
    );

    fun displayTitle(language: AppLanguage): String = when (language) {
        AppLanguage.PUNJABI -> punjabiTitle
        AppLanguage.HINDI -> hindiTitle
        AppLanguage.ENGLISH -> title
    }
}

data class ShoppingQuest(
    val id: String,
    val title: String,
    val punjabiTitle: String,
    val hindiTitle: String = "",
    val description: String,
    val progress: Int,
    val target: Int,
    val rewardCoins: Int,
    val completed: Boolean,
    val claimed: Boolean
) {
    fun displayTitle(language: AppLanguage): String = when (language) {
        AppLanguage.PUNJABI -> punjabiTitle.ifBlank { title }
        AppLanguage.HINDI -> hindiTitle.ifBlank { title }
        AppLanguage.ENGLISH -> title
    }
}
