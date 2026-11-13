package com.navohub.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.navohub.app.ui.theme.*

data class HubItem(
    val id: String,
    val name: String,
    val brand: String,
    val category: String,
    val mrp: Double,
    val salePrice: Double,
    val imageUrl: String,
    val isFrozen: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavoHubScreen() {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Packaged Grocery", "Frozen Produce", "Electronics", "Apparel")

    // Mock representation of the 308+ verified SKUs
    val sampleProducts = remember {
        listOf(
            HubItem("1", "Safal Select IQF Frozen Green Peas 1kg", "Safal", "Frozen Produce", 120.0, 106.0, "https://images.navgrocerystore.com/peas.png", true),
            HubItem("2", "Aashirvaad Shudh Chakki Atta 10kg", "Aashirvaad", "Packaged Grocery", 480.0, 422.0, "https://images.navgrocerystore.com/atta.png", false),
            HubItem("3", "Verka Pure Punjabi Desi Ghee 1L Tin", "Verka", "Packaged Grocery", 650.0, 572.0, "https://images.navgrocerystore.com/ghee.png", false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Navo Hub (ਨਵ ਹੱਬ)", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Sandhaur, Punjab | Direct Wholesale Delivery", fontSize = 10.sp, color = SunshineGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldGreen,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(SurfaceLight)
        ) {
            // Category Selector
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                containerColor = EmeraldDark,
                contentColor = SunshineGold,
                edgePadding = 8.dp
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = { Text(cat, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }

            // Products Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sampleProducts) { item ->
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(item.imageUrl),
                                    contentDescription = item.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                Surface(
                                    color = Color(0xFFDC3545),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        "-12%",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                item.brand.uppercase(),
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                item.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("₹${item.salePrice.toInt()}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = EmeraldGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("₹${item.mrp.toInt()}", textDecoration = TextDecoration.LineThrough, fontSize = 11.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { /* Direct Cart Action */ },
                                colors = ButtonDefaults.buttonColors(containerColor = SunshineGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+ Add to Cart", color = CharcoalDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}