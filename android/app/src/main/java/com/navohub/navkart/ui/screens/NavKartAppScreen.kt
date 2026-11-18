package com.navohub.navkart.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.navohub.navkart.data.model.NavProduct
import com.navohub.navkart.data.model.PaymentChannel
import com.navohub.navkart.data.model.ServiceVertical
import com.navohub.navkart.ui.viewmodel.StoreViewModel

private val ForestGreen = Color(0xFF0F3D28)
private val GoldAccent = Color(0xFFE6A817)
private val WarmCream = Color(0xFFF8F7F2)
private val DarkInk = Color(0xFF14221B)

@Composable
fun NavKartAppScreen(vm: StoreViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = WarmCream,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ForestGreen)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Nav Grocery & Nav Kart",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Main Bazaar, Sandhaur · 148020 · Flat 12% Off",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            if (state.totalPayable > 0) {
                                val upiIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    vm.buildUpiPaymentUri(state.totalPayable)
                                )
                                context.startActivity(upiIntent)
                                vm.submitOrder(PaymentChannel.UPI, "ANDROID-UPI-INTENT")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = DarkInk
                        )
                    ) {
                        Text(
                            text = "Basket (${state.cart.sumOf { it.quantity }}) ₹${state.totalPayable}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = vm::updateSearch,
                    placeholder = { Text("Search 308+ groceries, Supermall & SKUs...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ServiceVertical.entries) { vertical ->
                        FilterChip(
                            selected = state.activeVertical == vertical,
                            onClick = { vm.selectVertical(vertical) },
                            label = { Text(vertical.label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            state.statusMessage?.let { msg ->
                Text(
                    text = msg,
                    color = ForestGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 165.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.filteredProducts, key = { it.id }) { product ->
                    ProductGridTile(product = product, onAdd = { vm.addToCart(product) })
                }
            }
        }
    }
}

@Composable
private fun ProductGridTile(product: NavProduct, onAdd: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = product.resolvedImageUrl(),
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${product.brand} · ${product.id}",
                fontSize = 10.sp,
                color = Color(0xFF526058),
                maxLines = 1
            )
            Text(
                text = product.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹${product.effectiveSale}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                    if (product.mrp > product.effectiveSale) {
                        Text(
                            text = "₹${product.mrp}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
                Button(
                    onClick = onAdd,
                    enabled = product.stock > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (product.stock > 0) "Add" else "Out", fontSize = 12.sp)
                }
            }
        }
    }
}
