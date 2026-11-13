package com.example.data.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Public facing store and brand metadata for NavKart.
 *
 * STRICT PUBLIC PRIVACY SHIELD:
 * Owner personal name, personal email, GSTIN, bank accounts, and IFSC
 * are strictly NEVER exposed in this public profile.
 */
object StoreProfile {
    const val APP_NAME = "NavKart"
    const val APP_NAME_PUNJABI = "ਨਵਕਾਰਟ"
    const val STORE_BRAND = "NavKart powered by Nav Grocery Store"
    const val STORE_NAME = "Nav Grocery Store"
    const val STORE_NAME_PUNJABI = "ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ"
    const val TAGLINE = "100% Sealed Packaged & Cold-Chain Groceries & Multi-Store Marketplace"
    const val TAGLINE_PUNJABI = "੧੦੦% ਸੀਲਬੰਦ ਪੈਕਡ ਅਤੇ ਫਰੋਜ਼ਨ ਗ੍ਰੋਸਰੀ ਅਤੇ ਮਾਰਕੀਟਪਲੇਸ"

    // Official Public Customer Support Location
    const val LOCATION = "Main Bazaar, Sandhaur, District Malerkotla, Punjab 148020, India"
    const val LOCATION_SHORT = "Main Bazaar, Sandhaur (Malerkotla)"
    const val LOCATION_PUNJABI = "ਮੁੱਖ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ, ਜ਼ਿਲ੍ਹਾ ਮਲੇਰਕੋਟਲਾ, ਪੰਜਾਬ ੧੪੮੦੨੦"

    // Official Public Customer Helpline & WhatsApp
    const val HELPLINE_PHONE = "+91 8847370905"
    const val RAW_PHONE = "8847370905"
    const val WHATSAPP_NUMBER = "918847370905"
    const val CUSTOMER_SUPPORT_EMAIL = "support@navgrocerystore.com"

    // Official Public Web & Social Channels
    const val WEB_URL = "https://www.navgrocerystore.com"
    const val INSTAGRAM_HANDLE = "@nav_grocery_store"
    const val INSTAGRAM_URL = "https://www.instagram.com/nav_grocery_store"
    const val FACEBOOK_NAME = "Nav Grocery"
    const val FACEBOOK_URL = "https://www.facebook.com/NavGrocery"
    const val YOUTUBE_HANDLE = "@shopatnav"
    const val YOUTUBE_URL = "https://www.youtube.com/@shopatnav"
    const val MAPS_QUERY_URL = "geo:30.5600,75.8700?q=Nav+Grocery+Store,+Main+Bazaar,+Sandhaur,+Punjab+148020"

    // Timings & Delivery SLAs
    const val STORE_TIMINGS = "8:00 AM – 9:30 PM Daily"
    const val STORE_TIMINGS_PUNJABI = "ਸਵੇਰੇ ੮:੦੦ ਤੋਂ ਰਾਤ ੯:੩੦ ਵਜੇ ਤੱਕ ਰੋਜ਼ਾਨਾ"
    const val DELIVERY_PROMISE = "~2 hr delivery across Sandhaur & nearby villages"
    const val PICKUP_PROMISE = "30-min store pickup at Main Bazaar"

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
        "Kudani (ਕੁਦਨੀ)",
        "Kaleran (ਕਲੇਰਾਂ)",
        "Mithewal (ਮਿੱਠੇਵਾਲ)",
        "Bapla (ਬਾਪਲਾ)",
        "Farwali (ਫਰਵਾਲੀ)",
        "Nathu Majra (ਨੱਥੂ ਮਾਜਰਾ)",
        "Shergarh Cheema (ਸ਼ੇਰਗੜ੍ਹ ਚੀਮਾ)",
        "Dhurkot (ਧੁਰਕੋਟ)"
    )

    fun isStoreOpen(): Boolean {
        val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val calendar = Calendar.getInstance(istTimeZone)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute
        val openMinutes = 8 * 60 // 08:00 AM
        val closeMinutes = 21 * 60 + 30 // 09:30 PM
        return totalMinutes in openMinutes..closeMinutes
    }
}
