package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PayoutRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PayoutDao {
    @Query("SELECT * FROM payouts ORDER BY timestamp DESC")
    fun getAllPayouts(): Flow<List<PayoutRecord>>

    @Query("SELECT * FROM payouts WHERE sellerId = :sellerId ORDER BY timestamp DESC")
    fun getPayoutsBySeller(sellerId: String): Flow<List<PayoutRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayout(payout: PayoutRecord)
}
