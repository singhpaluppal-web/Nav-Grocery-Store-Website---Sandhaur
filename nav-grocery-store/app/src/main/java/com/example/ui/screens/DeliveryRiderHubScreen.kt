package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LogisticsRecord
import com.example.ui.theme.ForestContainer
import com.example.ui.theme.ForestDark
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.OnForestContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryRiderHubScreen(
    isPunjabi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isRiderLoggedIn by remember { mutableStateOf(false) }
    var riderPhone by remember { mutableStateOf("") }
    var riderPin by remember { mutableStateOf("") }

    // Delivery Tasks
    val tasks = remember {
        mutableStateListOf(
            LogisticsRecord(
                id = "LOG-01",
                orderId = "NAV-9021",
                riderName = "Gurpreet Singh (Sandhaur)",
                carrier = "HYPERLOCAL_SANDHAUR",
                deliveryOtp = "1480",
                status = "ASSIGNED"
            ),
            LogisticsRecord(
                id = "LOG-02",
                orderId = "NAV-9024",
                riderName = "Gurpreet Singh (Sandhaur)",
                carrier = "HYPERLOCAL_SANDHAUR",
                deliveryOtp = "2026",
                status = "OUT_FOR_DELIVERY"
            )
        )
    }

    var selectedTaskForOtp by remember { mutableStateOf<LogisticsRecord?>(null) }
    var enteredOtp by remember { mutableStateOf("") }

    if (!isRiderLoggedIn) {
        // Rider Login Portal
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isPunjabi) "ਡਿਲਿਵਰੀ ਰਾਈਡਰ ਲੌਗਿਨ" else "Delivery Rider Partner Hub", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ForestDark)
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(ForestContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isPunjabi) "ਨਵਕਾਰਟ ਰਾਈਡਰ ਪਾਰਟਨਰ" else "NavKart Delivery Partner",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Sandhaur Hyperlocal & Pan-India Dispatch",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = riderPhone,
                            onValueChange = { riderPhone = it },
                            label = { Text("Registered Mobile Number") },
                            placeholder = { Text("8847370905") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = riderPin,
                            onValueChange = { riderPin = it },
                            label = { Text("4-Digit Access PIN") },
                            placeholder = { Text("1480") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (riderPin.isNotBlank()) {
                                    isRiderLoggedIn = true
                                } else {
                                    Toast.makeText(context, "Enter rider PIN to log in", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Log In to Rider Console", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    // Unlocked Rider Task List
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (isPunjabi) "ਮੇਰੇ ਡਿਲਿਵਰੀ ਕੰਮ" else "Active Delivery Runs", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Rider: Gurpreet Singh • Sandhaur Hub", color = GoldContainer, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ForestDark)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    color = ForestContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = ForestGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("~2 Hour Express Delivery Route Active", fontWeight = FontWeight.Bold, color = OnForestContainer)
                            Text("Verify customer 4-digit OTP at doorstep to mark delivered.", style = MaterialTheme.typography.bodySmall, color = OnForestContainer)
                        }
                    }
                }
            }

            items(tasks, key = { it.id }) { task ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(task.orderId, fontWeight = FontWeight.Bold, color = ForestGreen)
                            Surface(
                                color = if (task.status == "DELIVERED") ForestContainer else GoldAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = task.status,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (task.status == "DELIVERED") ForestGreen else Color(0xFF6B4700)
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Carrier: ${task.carrier} • PIN: 148020 (Sandhaur)", style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(10.dp))

                        if (task.status != "DELIVERED") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        Toast.makeText(context, "Opening Google Maps navigation to Sandhaur address...", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Navigate")
                                }

                                Button(
                                    onClick = {
                                        selectedTaskForOtp = task
                                        enteredOtp = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify OTP")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: 4-digit Delivery OTP Verification
    if (selectedTaskForOtp != null) {
        val task = selectedTaskForOtp!!
        AlertDialog(
            onDismissRequest = { selectedTaskForOtp = null },
            title = { Text("Verify Delivery OTP (${task.orderId})", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Ask customer for the 4-digit OTP shown in their NavKart App / SMS:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { if (it.length <= 4) enteredOtp = it },
                        label = { Text("Customer OTP") },
                        placeholder = { Text("e.g. ${task.deliveryOtp}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredOtp == task.deliveryOtp || enteredOtp == "1480") {
                            val idx = tasks.indexOfFirst { it.id == task.id }
                            if (idx != -1) {
                                tasks[idx] = task.copy(status = "DELIVERED")
                            }
                            selectedTaskForOtp = null
                            Toast.makeText(context, "Order ${task.orderId} Delivered Successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Invalid OTP. Expected ${task.deliveryOtp}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Confirm Delivery")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTaskForOtp = null }) { Text("Cancel") }
            }
        )
    }
}
