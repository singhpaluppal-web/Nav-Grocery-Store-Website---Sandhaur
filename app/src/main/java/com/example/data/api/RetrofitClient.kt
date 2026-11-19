package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://nav-grocery-store-default-rtdb.firebaseio.com/"
    private const val STORE_BACKEND_BASE_URL = "https://www.navgrocerystore.com/"

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val sharedOkHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .addInterceptor { chain ->
                val request = chain.request()
                check(request.url.isHttps) {
                    "Cleartext HTTP connections are blocked by security policy."
                }
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    val apiService: GroceryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(sharedOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GroceryApiService::class.java)
    }

    val checkoutBackendService: CheckoutBackendService by lazy {
        Retrofit.Builder()
            .baseUrl(STORE_BACKEND_BASE_URL)
            .client(sharedOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CheckoutBackendService::class.java)
    }
}
