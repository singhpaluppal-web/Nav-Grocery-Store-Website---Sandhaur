package com.example.data.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Public facing store and brand metadata for NavKart & Nav Grocery Store.
 *
 * STRICT PUBLIC PLAY-STORE COMPLIANCE:
 * Clean customer-only storefront configuration with zero internal admin or POS secrets.
 */
object StoreProfile {
    const val APP_NAME = "NavKart"
    const val APP_NAME_PUNJABI = "ਨਵਕਾਰਟ"
    const val APP_NAME_HINDI = "नवकार्ट"
    const val STORE_BRAND = "NavKart powered by Nav Grocery Store"
    const val STORE_NAME = "Nav Grocery Store"
    const val STORE_NAME_PUNJABI = "ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ"
    const val STORE_NAME_HINDI = "नव ग्रोसरी स्टोर"
    const val TAGLINE = "100% Sealed Packaged & Cold-Chain Groceries & Multi-Store Marketplace"
    const val TAGLINE_PUNJABI = "੧੦੦% ਸੀਲਬੰਦ ਪੈਕਡ ਅਤੇ ਫਰੋਜ਼ਨ ਗ੍ਰੋਸਰੀ ਅਤੇ ਮਾਰਕੀਟਪਲੇਸ"
    const val TAGLINE_HINDI = "१००% सीलबंद पैक्ड और कोल्ड-चेन ग्रोसरी एवं मल्टी-स्टोर मार्केटप्लेस"

    // Official Public Google Business Profile & Customer Support Location
    const val LOCATION = "Main Bazaar, HPCP+Q8, Sandhaur, District Malerkotla, Punjab 148020, India"
    const val LOCATION_SHORT = "Main Bazaar, Sandhaur 148020"
    const val LOCATION_PUNJABI = "ਮੁੱਖ ਬਾਜ਼ਾਰ, HPCP+Q8, ਸੰਧੌੜ, ਜ਼ਿਲ੍ਹਾ ਮਲੇਰਕੋਟਲਾ, ਪੰਜਾਬ ੧੪੮੦੨੦"
    const val LOCATION_HINDI = "मुख्य बाज़ार, HPCP+Q8, संधौड़, जिला मलेरकोटला, पंजाब १४८०२०"
    const val DEFAULT_CITY_STATE = "Sandhaur, District Malerkotla, Punjab"
    const val DEFAULT_PIN_CODE = "148020"

    // Official Public Customer Helplines, UPI & WhatsApp
    const val HELPLINE_PHONE = "+91 8847370905"
    const val RAW_PHONE = "8847370905"
    const val SECONDARY_HELPLINE_PHONE = "+91 9915752905"
    const val SECONDARY_RAW_PHONE = "9915752905"
    const val WHATSAPP_NUMBER = "918847370905"
    const val MERCHANT_UPI_VPA = "8847370905@upi"
    const val CUSTOMER_SUPPORT_EMAIL = "support@navgrocerystore.com"
    const val FOOTER_CONTACT_EMAIL = "singhpaluppal@gmail.com"

    // Official GSTIN & Legal Compliance URLs
    const val GSTIN = "03AHPPU6185N1ZF"
    const val PRIVACY_POLICY_URL = "https://www.navgrocerystore.com/privacy-policy"
    const val TERMS_URL = "https://www.navgrocerystore.com/terms"
    const val REFUND_POLICY_URL = "https://www.navgrocerystore.com/refund-policy"

    // Official Public Web & Social Channels
    const val WEB_URL = "https://www.navgrocerystore.com"
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.navgrocerystore.navkart"
    const val INSTAGRAM_HANDLE = "@nav_grocery_store"
    const val INSTAGRAM_URL = "https://www.instagram.com/nav_grocery_store"
    const val FACEBOOK_NAME = "Nav Grocery"
    const val FACEBOOK_URL = "https://www.facebook.com/NavGrocery"
    const val YOUTUBE_HANDLE = "@shopatnav"
    const val YOUTUBE_URL = "https://www.youtube.com/@shopatnav"
    const val MAPS_QUERY_URL = "https://maps.google.com/?q=HPCP+Q8+Sandhaur,+District+Malerkotla,+Punjab+148020"
    const val GOOGLE_REVIEW_URL = "https://maps.google.com/?q=Nav+Grocery+Store+HPCP+Q8+Sandhaur+Punjab+148020"

    // Timings & Delivery SLAs
    const val STORE_TIMINGS = "8:00 AM – 9:30 PM Daily"
    const val STORE_TIMINGS_PUNJABI = "ਸਵੇਰੇ ੮:੦੦ ਤੋਂ ਰਾਤ ੯:੩੦ ਵਜੇ ਤੱਕ ਰੋਜ਼ਾਨਾ"
    const val DELIVERY_PROMISE = "15-Min Sandhaur Express, 30-Min Store Pickup & Pind-to-Pind 11 AM / 6 PM Vans"
    const val PICKUP_PROMISE = "Store Pickup (~30 Mins at Main Bazaar, Sandhaur 148020)"

    val PIND_DISPATCH_SLOTS = listOf(
        "11:00 AM Morning Route Dispatch",
        "6:00 PM Evening Route Dispatch"
    )

    val MARKETPLACE_CATEGORIES = listOf(
        "All Products",
        "Packaged Groceries",
        "Cold Chain Frozen Foods",
        "Atta & Dals",
        "Desi Ghee & Oils",
        "Spices",
        "Clothing and Apparel",
        "Electronics and Mobiles",
        "Footwear and Shoes",
        "Home and Kitchen",
        "Personal Care"
    )

    val NEARBY_VILLAGES = listOf(
        "Sandhaur (ਸੰਧੌੜ)",
        "Sherwani Kot (ਸ਼ੇਰਵਾਨੀ ਕੋਟ)",
        "Kasba Bharal (ਕਸਬਾ ਭਰਾਲ)",
        "Kalyan (ਕਲਿਆਣ)",
        "Matoi (ਮਤੋਈ)",
        "Khurd (ਖੁਰਦ)",
        "Kudani (ਕੁਦਨੀ)",
        "Kaleran (ਕਲੇਰਾਂ)",
        "Mithewal (ਮਿੱਠੇਵਾਲ)",
        "Bapla (ਬਾਪਲਾ)",
        "Farwali (ਫਰਵਾਲੀ)",
        "Nathu Majra (ਨੱਥੂ ਮਾਜਰਾ)"
    )

    fun isStoreOpen(): Boolean {
        val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val calendar = Calendar.getInstance(istTimeZone)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute
        val openMinutes = 8 * 60
        val closeMinutes = 21 * 60 + 30
        return totalMinutes in openMinutes..closeMinutes
    }
}
