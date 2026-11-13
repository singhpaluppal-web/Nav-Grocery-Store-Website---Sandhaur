package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "payouts")
@JsonClass(generateAdapter = true)
data class PayoutRecord(
    @PrimaryKey
    val payoutId: String,
    val sellerId: String,
    val sellerName: String,
    val amount: Int,
    val commissionDeducted: Int,
    val utrReference: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SETTLED"
)
