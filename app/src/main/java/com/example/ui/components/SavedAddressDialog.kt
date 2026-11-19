package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedAddress
import com.example.data.model.StoreProfile
import com.example.data.security.InputSanitizer
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.WholesaleRed

/**
 * Validated Delivery Address Add/Edit Dialog shared across Customer Account & Checkout Drawer.
 */
@Composable
fun SavedAddressDialog(
    initialAddress: SavedAddress?,
    defaultNameHint: String = "",
    onDismiss: () -> Unit,
    onSave: (
        addressId: String?,
        fullName: String,
        mobile: String,
        streetAddress: String,
        landmark: String,
        villageOrCity: String,
        pinCode: String,
        makeDefault: Boolean
    ) -> Unit
) {
    var fullName by remember(initialAddress) {
        mutableStateOf(initialAddress?.fullName?.ifBlank { defaultNameHint } ?: defaultNameHint)
    }
    var mobile by remember(initialAddress) { mutableStateOf(initialAddress?.mobile.orEmpty()) }
    var street by remember(initialAddress) { mutableStateOf(initialAddress?.streetAddress.orEmpty()) }
    var landmark by remember(initialAddress) { mutableStateOf(initialAddress?.landmark.orEmpty()) }
    var villageOrCity by remember(initialAddress) {
        mutableStateOf(initialAddress?.villageOrCity ?: StoreProfile.DEFAULT_CITY_STATE)
    }
    var pinCode by remember(initialAddress) {
        mutableStateOf(initialAddress?.pinCode ?: StoreProfile.DEFAULT_PIN_CODE)
    }
    var makeDefault by remember(initialAddress) {
        mutableStateOf(initialAddress?.defaultAddress ?: true)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAddress == null) "Add Delivery Address" else "Edit Delivery Address",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ForestGreen)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; validationError = null },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("addr_full_name")
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it.filter { ch -> ch.isDigit() }.take(10); validationError = null },
                    label = { Text("10-Digit Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("addr_mobile")
                )

                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it; validationError = null },
                    label = { Text("House / Street / Ward Address *") },
                    modifier = Modifier.fillMaxWidth().testTag("addr_street")
                )

                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text("Landmark (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("addr_landmark")
                )

                OutlinedTextField(
                    value = villageOrCity,
                    onValueChange = { villageOrCity = it; validationError = null },
                    label = { Text("Village / City & District *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("addr_village")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StoreProfile.NEARBY_VILLAGES.take(5).forEach { v ->
                        val short = v.substringBefore(" (")
                        FilterChip(
                            selected = villageOrCity.contains(short, ignoreCase = true),
                            onClick = {
                                villageOrCity = "$short, District Malerkotla, Punjab"
                                pinCode = StoreProfile.DEFAULT_PIN_CODE
                            },
                            label = { Text(short, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = pinCode,
                    onValueChange = { pinCode = it.filter { ch -> ch.isDigit() }.take(6); validationError = null },
                    label = { Text("6-Digit PIN Code *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("addr_pincode")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set as Default Address", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    Switch(
                        checked = makeDefault,
                        onCheckedChange = { makeDefault = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ForestGreen)
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelSmall.copy(color = WholesaleRed, fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val err = InputSanitizer.validateAddressFields(fullName, mobile, street, villageOrCity, pinCode)
                    if (err != null) {
                        validationError = err
                    } else {
                        onSave(initialAddress?.id, fullName, mobile, street, landmark, villageOrCity, pinCode, makeDefault)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_address_submit_btn")
            ) {
                Text("Save Address", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
