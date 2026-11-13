package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Product
import com.example.data.model.SellerRegistration

@Database(entities = [Product::class, SellerRegistration::class], version = 2, exportSchema = false)
abstract class NavGroceryDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun sellerDao(): SellerDao

    companion object {
        @Volatile
        private var INSTANCE: NavGroceryDatabase? = null

        fun getDatabase(context: Context): NavGroceryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NavGroceryDatabase::class.java,
                    "navkart_store.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
