package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SellerRegistration
import com.example.data.model.StoreProfile
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.OnForestGreenContainer
import com.example.ui.theme.SignatureGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerRegistrationSheet(
    isPunjabi: Boolean,
    onDismiss: () -> Unit,
    onSubmitSeller: (SellerRegistration) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Clothing & Apparel") }
    var address by remember { mutableStateOf("Main Bazaar, Sandhaur") }
    var gstin by remember { mutableStateOf("") }

    val categories = listOf(
        "Clothing & Apparel",
        "Electronics & Gadgets",
        "Footwear",
        "Home Goods",
        "Packaged Staples & Dry Kiryana",
        "Personal Care"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isPunjabi) "ਦੁਕਾਨਦਾਰ ਰਜਿਸਟ੍ਰੇਸ਼ਨ (Sell on NavKart)" else "Sell on NavKart Marketplace",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
                    )
                    Text(
                        text = "Partner with Nav Grocery Store Hub, Sandhaur (Malerkotla)",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = ForestGreenContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPunjabi)
                        "ਸੰਧੌੜ ਅਤੇ ਨੇੜਲੇ ਪਿੰਡਾਂ ਦੇ ਸਾਰੇ ਦੁਕਾਨਦਾਰ (ਕੱਪੜੇ, ਇਲੈਕਟ੍ਰਾਨਿਕਸ, ਜੁੱਤੇ, ਘਰੇਲੂ ਸਮਾਨ) ਨਵਕਾਰਟ 'ਤੇ ਆਪਣੇ ਉਤਪਾਦ ਵੇਚ ਸਕਦੇ ਹਨ।"
                    else
                        "Local merchants in Sandhaur & nearby villages can list apparel, electronics, footwear & goods with 0% onboarding fee.",
                    style = MaterialTheme.typography.bodySmall.copy(color = OnForestGreenContainer),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it },
                label = { Text(if (isPunjabi) "ਦੁਕਾਨ / ਫਰਮ ਦਾ ਨਾਮ" else "Shop / Business Name") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("seller_shop_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text(if (isPunjabi) "ਮਾਲਕ ਦਾ ਨਾਮ" else "Owner Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("seller_owner_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(if (isPunjabi) "ਮੋਬਾਈਲ ਨੰਬਰ (WhatsApp)" else "Mobile / WhatsApp Number (+91)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("seller_phone_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isPunjabi) "ਕਾਰੋਬਾਰ ਸ਼੍ਰੇਣੀ (Category):" else "Select Business Category:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(if (isPunjabi) "ਦੁਕਾਨ ਦਾ ਪਤਾ / ਪਿੰਡ" else "Shop Location / Village / Market") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("seller_address_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = gstin,
                onValueChange = { gstin = it },
                label = { Text("GSTIN or Aadhaar ID (Optional)") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (shopName.isNotBlank() && ownerName.isNotBlank() && phone.isNotBlank()) {
                        val seller = SellerRegistration(
                            id = "SEL-" + System.currentTimeMillis().toString().takeLast(6),
                            shopName = shopName,
                            ownerName = ownerName,
                            phone = phone,
                            businessCategory = selectedCategory,
                            villageOrAddress = address,
                            gstinOrAadhaar = gstin,
                            isApproved = false,
                            status = "Pending Verification"
                        )
                        onSubmitSeller(seller)
                        onDismiss()
                        Toast.makeText(context, "Registration submitted! Admin will contact within 2 hours.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Please fill in Shop Name, Owner Name & Phone", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_seller_registration_btn")
            ) {
                Text(
                    text = if (isPunjabi) "ਦੁਕਾਨ ਦਰਜ ਕਰੋ (Submit Registration)" else "Submit Shopkeeper Registration",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
