package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.data.api.RetrofitClient
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.squareup.moshi.Types
import java.io.BufferedReader
import java.io.InputStreamReader

class GroceryLocalStore(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nav_grocery_prefs", Context.MODE_PRIVATE)

    private val moshi = RetrofitClient.moshi
    private val cartListType = Types.newParameterizedType(List::class.java, CartItem::class.java)
    private val cartAdapter = moshi.adapter<List<CartItem>>(cartListType)

    private val orderListType = Types.newParameterizedType(List::class.java, Order::class.java)
    private val orderAdapter = moshi.adapter<List<Order>>(orderListType)

    private val productListType = Types.newParameterizedType(List::class.java, Product::class.java)
    private val productAdapter = moshi.adapter<List<Product>>(productListType)

    fun loadSeedProducts(): List<Product> {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.products_seed)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            productAdapter.fromJson(jsonString) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveCart(cart: List<CartItem>) {
        val json = cartAdapter.toJson(cart)
        prefs.edit().putString("saved_cart_json", json).apply()
    }

    fun loadCart(): List<CartItem> {
        val json = prefs.getString("saved_cart_json", null) ?: return emptyList()
        return try {
            cartAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveOrders(orders: List<Order>) {
        val json = orderAdapter.toJson(orders)
        prefs.edit().putString("saved_orders_json", json).apply()
    }

    fun saveOrder(order: Order) {
        val existing = loadOrders().toMutableList()
        existing.add(0, order) // latest first
        val json = orderAdapter.toJson(existing)
        prefs.edit().putString("saved_orders_json", json).apply()
    }

    fun loadOrders(): List<Order> {
        val json = prefs.getString("saved_orders_json", null) ?: return emptyList()
        return try {
            orderAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isPunjabiLanguage(): Boolean {
        return prefs.getBoolean("pref_is_punjabi", false)
    }

    fun setPunjabiLanguage(isPunjabi: Boolean) {
        prefs.edit().putBoolean("pref_is_punjabi", isPunjabi).apply()
    }
}
