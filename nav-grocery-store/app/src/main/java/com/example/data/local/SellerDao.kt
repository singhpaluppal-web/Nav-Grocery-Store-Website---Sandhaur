package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SellerRegistration
import kotlinx.coroutines.flow.Flow

@Dao
interface SellerDao {
    @Query("SELECT * FROM sellers ORDER BY timestamp DESC")
    fun getAllSellers(): Flow<List<SellerRegistration>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeller(seller: SellerRegistration)

    @Update
    suspend fun updateSeller(seller: SellerRegistration)

    @Query("DELETE FROM sellers WHERE id = :id")
    suspend fun deleteSellerById(id: String)
}
