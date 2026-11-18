package com.navohub.navkart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.navohub.navkart.ui.screens.NavKartAppScreen
import com.navohub.navkart.ui.viewmodel.StoreViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val storeViewModel: StoreViewModel = viewModel()
            NavKartAppScreen(viewModel = storeViewModel)
        }
    }
}
