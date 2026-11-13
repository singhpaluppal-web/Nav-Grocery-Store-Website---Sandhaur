package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.api.RetrofitClient
import com.example.data.local.GroceryLocalStore
import com.example.data.local.NavGroceryDatabase
import com.example.data.repository.GroceryRepository
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GroceryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GroceryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val localStore = GroceryLocalStore(applicationContext)
                val database = NavGroceryDatabase.getDatabase(applicationContext)
                val repository = GroceryRepository(
                    apiService = RetrofitClient.apiService,
                    productDao = database.productDao(),
                    sellerDao = database.sellerDao(),
                    localStore = localStore
                )
                return GroceryViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}
