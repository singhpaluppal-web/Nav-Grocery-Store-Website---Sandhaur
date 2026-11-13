package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "sellers")
@JsonClass(generateAdapter = true)
data class Seller(
    @PrimaryKey
    val id: String,
    val shopName: String,
    val ownerName: String,
    val phone: String,
    val email: String,
    val address: String,
    val city: String = "Sandhaur",
    val pincode: String = "148020",
    val gstinOrPan: String,
    val categorySpecialization: String, // e.g. "Clothing & Apparel", "Electronics"
    val bankAccountOrUpi: String,
    val isApproved: Boolean = true,
    val rating: Float = 4.8f,
    val grossSales: Int = 0,
    val commissionDeducted: Int = 0,
    val pendingEscrow: Int = 0,
    val settledPayouts: Int = 0,
    val joinedTimestamp: Long = System.currentTimeMillis()
) {
    val netEarnings: Int
        get() = maxOf(0, grossSales - commissionDeducted)

    val withdrawableBalance: Int
        get() = maxOf(0, netEarnings - settledPayouts - pendingEscrow)
}
