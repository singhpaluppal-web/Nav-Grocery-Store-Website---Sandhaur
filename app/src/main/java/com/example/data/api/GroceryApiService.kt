package com.example.data.api

import com.example.data.model.CoinRewardConfig
import com.example.data.model.StoreDeliveryConfig
import com.example.data.model.StoreHeroPromoConfig
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StorePaymentsConfig
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class RtdbSystemLocksDto(
    val marqueeTicker: String = "",
    val emergencyStorefrontFreeze: Boolean = false,
    val upiEnabled: Boolean = true,
    val codEnabled: Boolean = true
)

/**
 * Strictly read-only public API endpoints for catalog, dynamic A-to-Z categories,
 * remote store offers/coin/payment/delivery/heroPromo rules, hero banners, and system locks.
 */
interface GroceryApiService {
    @GET("products.json")
    suspend fun getProductsRaw(): ResponseBody

    @GET("categories.json")
    suspend fun getCategoriesRaw(): ResponseBody

    @GET("storeConfig/offers.json")
    suspend fun getStoreOffersConfig(): StoreOffersConfig?

    @GET("storeConfig/coins.json")
    suspend fun getStoreCoinConfig(): CoinRewardConfig?

    @GET("storeConfig/payments.json")
    suspend fun getStorePaymentsConfig(): StorePaymentsConfig?

    @GET("storeConfig/delivery.json")
    suspend fun getStoreDeliveryConfig(): StoreDeliveryConfig?

    @GET("storeConfig/heroPromo.json")
    suspend fun getStoreHeroPromoConfig(): StoreHeroPromoConfig?

    @GET("hero_banners.json")
    suspend fun getHeroBannersRaw(): ResponseBody

    @GET("system_locks.json")
    suspend fun getSystemLocks(): RtdbSystemLocksDto?
}
