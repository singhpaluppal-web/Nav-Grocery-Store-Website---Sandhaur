package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "sellers")
@JsonClass(generateAdapter = true)
data class SellerRegistration(
    @PrimaryKey val id: String,
    val shopName: String,
    val ownerName: String,
    val phone: String,
    val businessCategory: String,
    val villageOrAddress: String,
    val gstinOrAadhaar: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isApproved: Boolean = false,
    val status: String = "Pending Verification"
)
