package com.example.data.api

import com.example.data.model.Product
import retrofit2.http.GET

interface GroceryApiService {
    @GET("products.json")
    suspend fun getProducts(): List<Product>
}
