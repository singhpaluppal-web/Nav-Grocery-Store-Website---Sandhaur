package com.navohub.navkart.data.remote

import com.navohub.navkart.BuildConfig
import com.navohub.navkart.data.model.AndroidFeedResponse
import com.navohub.navkart.data.model.NavProduct
import com.navohub.navkart.data.model.OrderPayload
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface NavFirebaseApi {
    @GET("android_feed.json")
    suspend fun fetchAndroidFeed(): AndroidFeedResponse

    @GET("products.json")
    suspend fun fetchAllProductsMap(): Map<String, NavProduct>

    @PUT("orders/{orderId}.json")
    suspend fun submitOrder(
        @Path("orderId") orderId: String,
        @Body payload: OrderPayload
    ): OrderPayload

    companion object {
        fun create(): NavFirebaseApi {
            return Retrofit.Builder()
                .baseUrl(BuildConfig.FIREBASE_RTDB_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NavFirebaseApi::class.java)
        }
    }
}
