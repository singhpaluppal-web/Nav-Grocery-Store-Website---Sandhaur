package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.ui.theme.ForestContainer
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OnForestContainer

data class ChatMessage(
    val sender: String,
    val message: String,
    val suggestedProducts: List<Product> = emptyList(),
    val trackedOrder: Order? = null,
    val showWhatsAppEscalation: Boolean = false
)

enum class ChatLanguage(val label: String) {
    PUNJABI("ਪੰਜਾਬੀ (Gurmukhi)"),
    HINDI("हिंदी (Devanagari)"),
    ENGLISH("English")
}

/**
 * Public Simple AI Agent:
 * - Rule-based, ultra-fast, zero-token cost
 * - Strictly NO Gemini AI models or API keys exposed on public app
 * - Trilingual (Punjabi, Hindi, English) with auto-detection & manual switch
 * - Live product search with photo, price, and Add to Cart button
 * - Live Order & AWB tracking by Order ID or phone number
 * - 1-tap WhatsApp escalation to +91 8847370905
 */
@Composable
fun AiShoppingAssistantDialog(
    allProducts: List<Product>,
    orders: List<Order> = emptyList(),
    isPunjabi: Boolean,
    onDismiss: () -> Unit,
    onSelectProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit
) {
    val context = LocalContext.current
    var selectedLang by remember {
        mutableStateOf(if (isPunjabi) ChatLanguage.PUNJABI else ChatLanguage.ENGLISH)
    }
    var inputQuery by remember { mutableStateOf("") }

    val welcomeMessage = when (selectedLang) {
        ChatLanguage.PUNJABI -> "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ ਜੀ! ਮੈਂ ਨਵਕਾਰਟ ਸਟੋਰ ਅਸਿਸਟੈਂਟ ਹਾਂ। ਤੁਸੀਂ ੧੦੦% ਸੀਲਬੰਦ ਰਾਸ਼ਨ (੧੨% ਥੋਕ ਛੋਟ), ਕੱਪੜੇ, ਮੋਬਾਈਲ ਜਾਂ ਜੁੱਤੀਆਂ ਵਿੱਚ ਕੀ ਲੱਭ ਰਹੇ ਹੋ? ਆਰਡਰ ਨੰਬਰ ਜਾਂ ਫ਼ੋਨ ਨੰਬਰ ਲਿਖ ਕੇ ਲਾਈਵ ਆਰਡਰ ਟਰੈਕ ਵੀ ਕਰ ਸਕਦੇ ਹੋ।"
        ChatLanguage.HINDI -> "नमस्ते! मैं नवਕਾਰਟ स्टोर असिस्टेंट हूँ। १००% सीलबंद राशन (१२% थोक छूट), कपड़े, मोबाइल या जूते खोजें, या आर्डर नंबर डालकर लाइव ट्रैकिंग करें।"
        ChatLanguage.ENGLISH -> "Hello! I am your NavKart Store Shopping & Support Agent. Search 100% sealed groceries at flat 12% wholesale discount, track live orders by Order ID / Mobile number, or get 24x7 policy help!"
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(sender = "AI", message = welcomeMessage)
        )
    }

    val quickPrompts = when (selectedLang) {
        ChatLanguage.PUNJABI -> listOf(
            "ਪੰਜਾਬੀ ਸੂਟ ਅਤੇ ਕੁੜਤੀਆਂ",
            "੫ਜੀ ਮੋਬਾਈਲ ਅਤੇ ਹੈੱਡਫੋਨ",
            "ਦੇਸੀ ਘਿਓ ਅਤੇ ਚੱਕੀ ਆਟਾ",
            "ਸਪੋਰਟਸ ਸ਼ੂਜ਼ (UK 8)",
            "ਆਰਡਰ ਟਰੈਕ ਕਰੋ",
            "ਵਾਪਸੀ ਅਤੇ ਰਿਫੰਡ ਨੀਤੀ"
        )
        ChatLanguage.HINDI -> listOf(
            "पंजाबी सूट और कपड़े",
            "5G स्मार्टफोन",
            "देसी घी और आटा",
            "स्पोर्ट्स जूते",
            "आर्डर ट्रैक करें",
            "रिटर्न और रिफंड नियम"
        )
        ChatLanguage.ENGLISH -> listOf(
            "Best Punjabi Suits under ₹2000",
            "5G Smartphones & Earbuds",
            "Pure Desi Ghee & Chakki Atta",
            "Sports Running Shoes",
            "Track Order by ID",
            "Return & Refund Policy"
        )
    }

    fun handleUserPrompt(prompt: String) {
        messages.add(ChatMessage(sender = "USER", message = prompt))

        val pLower = prompt.lowercase()

        // 0. Auto Language Detection
        if (prompt.any { it in '\u0A00'..'\u0A7F' }) {
            selectedLang = ChatLanguage.PUNJABI
        } else if (prompt.any { it in '\u0900'..'\u097F' }) {
            selectedLang = ChatLanguage.HINDI
        }

        // 1. Order Tracking by Order ID or Phone
        val matchedOrder = orders.firstOrNull { ord ->
            ord.orderId.equals(prompt.trim(), ignoreCase = true) ||
            (prompt.length >= 4 && ord.orderId.contains(prompt.trim(), ignoreCase = true)) ||
            (ord.customerPhone.isNotBlank() && ord.customerPhone.contains(prompt.filter { it.isDigit() }))
        }

        if (matchedOrder != null) {
            val trackReply = when (selectedLang) {
                ChatLanguage.PUNJABI -> "ਤੁਹਾਡਾ ਆਰਡਰ ਮਿਲ ਗਿਆ ਹੈ! ਆਰਡਰ ID: ${matchedOrder.orderId} • ਸਥਿਤੀ: ${matchedOrder.status} • ਡਿਲਿਵਰੀ: ${matchedOrder.deliveryType} (ਸੰਧੌੜ ਹੱਬ)"
                ChatLanguage.HINDI -> "आपका आर्डर मिल गया है! आर्डर ID: ${matchedOrder.orderId} • स्टेटस: ${matchedOrder.status} • कुल: ₹${matchedOrder.finalTotal}"
                ChatLanguage.ENGLISH -> "Order Located! Order ID: ${matchedOrder.orderId} • Live Status: ${matchedOrder.status} • Payable: ₹${matchedOrder.finalTotal}"
            }
            messages.add(ChatMessage(sender = "AI", message = trackReply, trackedOrder = matchedOrder, showWhatsAppEscalation = true))
            return
        }

        // 2. Policy & Return/Refund queries
        if (pLower.contains("return") || pLower.contains("refund") || pLower.contains("ਵਾਪਸੀ") || pLower.contains("ਰਿਫੰਡ") || pLower.contains("वापसी")) {
            val policyAnswer = when (selectedLang) {
                ChatLanguage.PUNJABI -> "ਨਵਕਾਰਟ ਰਿਟਰਨ ਨੀਤੀ: ਕੱਪੜੇ ਅਤੇ ਜੁੱਤੀਆਂ ਲਈ ੭ ਦਿਨ ਦੀ ਅਸਾਨ ਵਾਪਸੀ ਹੈ। ਇਲੈਕਟ੍ਰਾਨਿਕਸ ਲਈ ੭ ਦਿਨ ਦੀ ਮੈਨੂਫੈਕਚਰਿੰਗ ਡਿਫੈਕਟ ਬਦਲੀ ਹੈ। ਸੀਲਬੰਦ ਰਾਸ਼ਨ ਅਤੇ ਫਰੋਜ਼ਨ ਆਈਟਮਾਂ 'ਤੇ ਖਰਾਬੀ ਜਾਂ ਡੈਮੇਜ ਹੋਣ 'ਤੇ ਤੁਰੰਤ ਡੋਰਸਟੈਪ ਬਦਲੀ ਜਾਂ ਰਿਫੰਡ ਮਿਲਦਾ ਹੈ।"
                ChatLanguage.HINDI -> "नवਕਾਰਟ रिटर्न पालिसी: कपड़ों और जूतों पर ७ दिन की आसान वापसी है। इलेक्ट्रॉनिक्स पर ७ दिन का डिफेक्ट रिप्लेसमेंट है। सीलबंद राशन खराब होने पर तत्काल डोरस्टेप रिप्लेसमेंट मिलता है।"
                ChatLanguage.ENGLISH -> "NavKart Policy: 7-day easy return/exchange on Clothing & Footwear with original tags. 7-day replacement on Electronics for manufacturing defects. Instant doorstep replacement or refund for damaged/expired sealed groceries."
            }
            messages.add(ChatMessage(sender = "AI", message = policyAnswer, showWhatsAppEscalation = true))
            return
        }

        // 3. General Order Tracking prompt without ID
        if (pLower.contains("track") || pLower.contains("ਟਰੈਕ") || pLower.contains("ट्रैक")) {
            val trackPrompt = when (selectedLang) {
                ChatLanguage.PUNJABI -> "ਕਿਰਪਾ ਕਰਕੇ ਆਪਣਾ ੪ ਜਾਂ ੬ ਅੰਕਾਂ ਦਾ ਆਰਡਰ ਨੰਬਰ (ਜਿਵੇਂ NAV-101) ਜਾਂ ਆਪਣਾ ੧੦ ਅੰਕਾਂ ਦਾ ਮੋਬਾਈਲ ਨੰਬਰ ਇੱਥੇ ਲਿਖੋ ਤਾਂ ਜੋ ਮੈਂ ਲਾਈਵ ਸਥਿਤੀ ਦਿਖਾ ਸਕਾਂ।"
                ChatLanguage.HINDI -> "कृपया अपना आर्डर नंबर (जैसे NAV-101) या मोबाइल नंबर दर्ज करें ताकि मैं लाइव ट्रैकिंग दिखा सकूँ।"
                ChatLanguage.ENGLISH -> "Please enter your Order ID (e.g. NAV-101) or 10-digit mobile number here to view real-time delivery stage & courier AWB!"
            }
            messages.add(ChatMessage(sender = "AI", message = trackPrompt, showWhatsAppEscalation = true))
            return
        }

        // 4. Product Catalog Search
        val matched = allProducts.filter { prod ->
            val name = prod.name.lowercase()
            val dept = prod.department.lowercase()
            val cat = prod.category.lowercase()
            name.contains(pLower) || dept.contains(pLower) || cat.contains(pLower) ||
            (pLower.contains("suit") && dept.contains("clothing")) ||
            (pLower.contains("phone") || pLower.contains("mobile") && dept.contains("electronics")) ||
            (pLower.contains("shoe") && dept.contains("footwear")) ||
            (pLower.contains("ghee") || pLower.contains("atta") && dept.contains("groceries")) ||
            (pLower.contains("frozen") && (cat.contains("frozen") || name.contains("frozen")))
        }.take(3)

        val reply = when (selectedLang) {
            ChatLanguage.PUNJABI -> {
                if (matched.isNotEmpty()) {
                    "ਮੈਂ ਤੁਹਾਡੇ ਲਈ ਨਵਕਾਰਟ ਤੋਂ ${matched.size} ਚੀਜ਼ਾਂ ਲੱਭੀਆਂ ਹਨ! ਫਲੈਟ ੧੨% ਥੋਕ ਛੋਟ ਲਾਗੂ ਹੈ:"
                } else {
                    "ਤੁਸੀਂ ਸਾਡੇ ਕੈਟਾਲਾਗ ਵਿੱਚੋਂ ਕੱਪੜੇ, ਇਲੈਕਟ੍ਰਾਨਿਕਸ, ਜੁੱਤੀਆਂ ਜਾਂ ਸੀਲਬੰਦ ਰਾਸ਼ਨ ਖੋਜ ਸਕਦੇ ਹੋ। ਨਵਕਾਰਟ 'ਤੇ ਤਾਜ਼ੇ ਫਲ/ਸਬਜ਼ੀਆਂ ਨਹੀਂ ਵਿਕਦੀਆਂ।"
                }
            }
            ChatLanguage.HINDI -> {
                if (matched.isNotEmpty()) {
                    "मैंने आपके लिए ${matched.size} प्रोडक्ट्स खोजे हैं! फ्लैट १२% थोक छूट लागू है:"
                } else {
                    "आप हमारे कैटलॉग में कपड़े, इलेक्ट्रॉनिक्स, जूते या सीलबंद राशन खोज सकते हैं।"
                }
            }
            ChatLanguage.ENGLISH -> {
                if (matched.isNotEmpty()) {
                    "Found ${matched.size} verified items with flat 12% wholesale discount applied:"
                } else {
                    "I searched our marketplace across Clothing, Electronics, Footwear, and Sealed Groceries. Note: NavKart strictly never sells fresh fruits or vegetables."
                }
            }
        }

        messages.add(ChatMessage(sender = "AI", message = reply, suggestedProducts = matched))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(ForestGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NavKart Store Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Lightweight • Trilingual • Instant", style = MaterialTheme.typography.labelSmall.copy(color = ForestGreen, fontSize = 10.sp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Trilingual Switcher
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChatLanguage.values().forEach { lang ->
                        FilterChip(
                            selected = selectedLang == lang,
                            onClick = { selectedLang = lang },
                            label = { Text(lang.label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                // Quick Suggestion Chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPrompts.forEach { q ->
                        AssistChip(
                            onClick = { handleUserPrompt(q) },
                            label = { Text(q, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Messages List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        val isAi = msg.sender == "AI"
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isAi) Alignment.Start else Alignment.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAi) ForestContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth(0.92f)
                            ) {
                                Text(
                                    text = msg.message,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isAi) OnForestContainer else MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 16.sp
                                    ),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            // Render Tracked Order Card
                            if (msg.trackedOrder != null) {
                                val o = msg.trackedOrder
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen),
                                    modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(o.orderId, fontWeight = FontWeight.Bold, color = ForestGreen)
                                            Text("₹${o.finalTotal}", fontWeight = FontWeight.ExtraBold)
                                        }
                                        Text("Stage: ${o.status}", style = MaterialTheme.typography.bodySmall)
                                        Text("Recipient: ${o.customerName} (${o.customerPhone})", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                        Text("Destination: ${o.address}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    }
                                }
                            }

                            // In-chat product cards with Photo, Price & Add to Cart button
                            if (msg.suggestedProducts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                msg.suggestedProducts.forEach { p ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (p.fullImageUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = p.fullImageUrl,
                                                    contentDescription = p.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(p.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                                Text("₹${p.wholesalePrice} (MRP: ₹${p.mrp})", style = MaterialTheme.typography.labelSmall.copy(color = ForestGreen, fontWeight = FontWeight.Bold))
                                            }

                                            Button(
                                                onClick = {
                                                    onAddToCart(p)
                                                    onDismiss()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // WhatsApp Escalation Option
                            if (msg.showWhatsAppEscalation) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    onClick = {
                                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=${StoreProfile.WHATSAPP_NUMBER}&text=Hello%20NavKart%20Help%20Desk")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    },
                                    color = Color(0xFF25D366),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(0.92f).height(34.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Chat on WhatsApp (+91 8847370905)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Bar
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        placeholder = {
                            Text(
                                when (selectedLang) {
                                    ChatLanguage.PUNJABI -> "ਆਰਡਰ ID ਜਾਂ ਚੀਜ਼ ਦਾ ਨਾਮ ਲਿਖੋ..."
                                    ChatLanguage.HINDI -> "आर्डर ID या प्रोडक्ट का नाम लिखें..."
                                    ChatLanguage.ENGLISH -> "Enter Order ID, phone, or item..."
                                },
                                fontSize = 11.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (inputQuery.isNotBlank()) {
                                val q = inputQuery
                                inputQuery = ""
                                handleUserPrompt(q)
                            }
                        },
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(ForestGreen)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
