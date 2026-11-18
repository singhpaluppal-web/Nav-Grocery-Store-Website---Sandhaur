package com.navohub.navkart.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.navohub.navkart.data.model.NavProduct

class CatalogCache(context: Context) {
    private val prefs = context.getSharedPreferences("nav_kart_offline_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveProducts(products: List<NavProduct>) {
        prefs.edit()
            .putString(KEY_PRODUCTS_JSON, gson.toJson(products))
            .putLong(KEY_LAST_SYNC_MS, System.currentTimeMillis())
            .apply()
    }

    fun loadProducts(): List<NavProduct> {
        val raw = prefs.getString(KEY_PRODUCTS_JSON, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<NavProduct>>() {}.type
            gson.fromJson<List<NavProduct>>(raw, type).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSession(phone: String, name: String, address: String) {
        prefs.edit()
            .putString(KEY_PHONE, phone)
            .putString(KEY_NAME, name)
            .putString(KEY_ADDRESS, address)
            .apply()
    }

    fun getSavedPhone(): String = prefs.getString(KEY_PHONE, "") ?: ""
    fun getSavedName(): String = prefs.getString(KEY_NAME, "") ?: ""
    fun getSavedAddress(): String = prefs.getString(KEY_ADDRESS, "Main Bazaar, Sandhaur 148020") ?: ""

    private companion object {
        const val KEY_PRODUCTS_JSON = "cached_products_v1"
        const val KEY_LAST_SYNC_MS = "last_sync_ms"
        const val KEY_PHONE = "customer_phone"
        const val KEY_NAME = "customer_name"
        const val KEY_ADDRESS = "customer_address"
    }
}
