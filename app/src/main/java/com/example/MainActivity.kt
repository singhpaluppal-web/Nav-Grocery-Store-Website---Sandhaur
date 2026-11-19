package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.api.RetrofitClient
import com.example.data.auth.GoogleAuthManager
import com.example.data.local.GroceryLocalStore
import com.example.data.local.NavGroceryDatabase
import com.example.data.repository.CustomerAccountRepository
import com.example.data.repository.GroceryRepository
import com.example.data.security.configureAppCheck
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GroceryViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val authManager by lazy { GoogleAuthManager() }

    private val viewModel: GroceryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val localStore = GroceryLocalStore(applicationContext)
                val database = NavGroceryDatabase.getDatabase(applicationContext)
                val repository = GroceryRepository(
                    apiService = RetrofitClient.apiService,
                    productDao = database.productDao(),
                    localStore = localStore
                )
                val customerRepo = CustomerAccountRepository(applicationContext)
                return GroceryViewModel(
                    repository = repository,
                    customerRepository = customerRepo,
                    googleAuthManager = authManager
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must configure Firebase App Check before any AI Logic or Firebase calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        lifecycleScope.launch {
            authManager.attemptSilentSignIn(this@MainActivity)
        }
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}
