package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppLanguage
import com.example.data.model.Product
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StoreProfile
import com.example.data.util.ProductImageAutoFetcher
import com.example.ui.components.rememberTrilingualTtsController
import com.example.ui.components.rememberVoiceInputController
import com.example.ui.theme.ForestContainer
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.OnForestContainer
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.WholesaleRed
import com.example.ui.viewmodel.SmartHelperChatMessage
import com.example.ui.viewmodel.SmartHelperUiState

/**
 * Trilingual Gemini Smart Helper powered by Firebase AI Logic (`gemini-3.8-flash`):
 * - Supports English (`en-IN`), Punjabi (`pa-IN`), and Hindi (`hi-IN`) with auto-detection
 *   and 1-tap language chips (`English | ਪੰਜਾਬੀ | हिंदी`).
 * - Integrated Voice Input (`SpeechRecognizer` + `ACTION_RECOGNIZE_SPEECH`) & TTS audio playback.
 * - Strictly customer-scoped: A-to-Z product discovery, live Firebase offers/coins, and customer's own orders.
 * - Zero API keys or internal admin/POS/Navo Hub references.
 */
@Composable
fun AiShoppingAssistantDialog(
    messages: List<SmartHelperChatMessage>,
    uiState: SmartHelperUiState,
    appLanguage: AppLanguage,
    offersConfig: StoreOffersConfig = StoreOffersConfig(),
    categoryDiscounts: Map<String, Double> = emptyMap(),
    userCoinsBalance: Long = 0L,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSendPrompt: (String, AppLanguage?) -> Unit,
    onRetryPrompt: () -> Unit,
    onDismissError: () -> Unit,
    onDismiss: () -> Unit,
    onSelectProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit
) {
    val context = LocalContext.current
    var inputQuery by remember { mutableStateOf("") }
    var autoSpeakReplies by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val ttsController = rememberTrilingualTtsController()

    val voiceController = rememberVoiceInputController(
        currentLanguage = appLanguage,
        promptTitle = appLanguage.tr(
            english = "Speak in English, ਪੰਜਾਬੀ or हिंदी",
            punjabi = "ਪੰਜਾਬੀ, हिंदी ਜਾਂ English ਵਿੱਚ ਬੋਲੋ",
            hindi = "हिंदी, ਪੰਜਾਬੀ या English में बोलें"
        )
    ) { spokenText, detectedLang ->
        onSelectLanguage(detectedLang)
        onSendPrompt(spokenText, detectedLang)
    }

    val discLabel = offersConfig.discountLabelPercent
    val welcomeMessage = remember(appLanguage, discLabel, userCoinsBalance) {
        appLanguage.tr(
            english = "Hello! I am your NavKart Smart Helper (Gemini AI). Ask me about sealed groceries at $discLabel% OFF, Electronics, Cosmetics, Soaps, Clothing, live Nav Coins ($userCoinsBalance coins), or your own orders!",
            punjabi = "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ ਜੀ! ਮੈਂ ਨਵਕਾਰਟ ਸਮਾਰਟ ਹੈਲਪਰ ਹਾਂ। ਤੁਸੀਂ $discLabel% ਥੋਕ ਛੋਟ ਵਾਲੀ ਸੀਲਬੰਦ ਗ੍ਰੋਸਰੀ, ਇਲੈਕਟ੍ਰਾਨਿਕਸ, ਕਾਸਮੈਟਿਕਸ, ਕੱਪੜੇ, ਆਪਣੇ ਨਵ ਕੌਇਨਜ਼ ($userCoinsBalance) ਜਾਂ ਆਪਣੇ ਆਰਡਰਾਂ ਬਾਰੇ ਪੁੱਛ ਸਕਦੇ ਹੋ!",
            hindi = "नमस्ते! मैं नवकार्ट स्मार्ट हेल्पर हूँ। मुझसे $discLabel% छूट वाले सीलबंद राशन, इलेक्ट्रॉनिक्स, कॉस्मेटिक्स, साबुन, कपड़े, आपके नव कॉइन्स ($userCoinsBalance कॉइन्स) या अपने ऑर्डर के बारे में हिंदी में पूछें!"
        )
    }

    val quickPrompts = remember(appLanguage) {
        when (appLanguage) {
            AppLanguage.HINDI -> listOf(
                "देसी घी और आटा दिखाओ",
                "कॉस्मेटिक्स और साबुन",
                "5G मोबाइल और इयरबड्स",
                "पंजाबी सूट और कपड़े",
                "मेरे नव कॉइन्स और छूट बताओ",
                "मेरा ऑर्डर स्टेटस क्या है?"
            )
            AppLanguage.PUNJABI -> listOf(
                "ਦੇਸੀ ਘਿਓ ਅਤੇ ਚੱਕੀ ਆਟਾ",
                "ਸਾਬਣ ਅਤੇ ਪਰਸਨਲ ਕੇਅਰ",
                "5G ਮੋਬਾਈਲ ਅਤੇ ਹੈੱਡਫੋਨ",
                "ਪੰਜਾਬੀ ਸੂਟ ਅਤੇ ਜੁੱਤੀਆਂ",
                "ਮੇਰੇ ਨਵ ਕੌਇਨਜ਼ ਅਤੇ ਛੋਟ ਦੱਸੋ",
                "ਮੇਰਾ ਆਰਡਰ ਟਰੈਕ ਕਰੋ"
            )
            AppLanguage.ENGLISH -> listOf(
                "Show Pure Desi Ghee & Atta",
                "Soaps & Personal Care",
                "5G Smartphones & Earbuds",
                "Punjabi Suits & Shoes",
                "Check live offers & my Nav Coins",
                "Track my orders"
            )
        }
    }

    // Auto-scroll and optional TTS auto-read on new AI reply
    LaunchedEffect(messages.size, uiState) {
        val totalItems = messages.size + 1 + if (uiState is SmartHelperUiState.Streaming) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem((totalItems - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(messages.lastOrNull()?.id) {
        val last = messages.lastOrNull()
        if (autoSpeakReplies && last != null && last.sender == "AI") {
            ttsController.speak(last.id, last.message, last.language)
        }
    }

    AlertDialog(
        onDismissRequest = {
            ttsController.stop()
            voiceController.stopListening()
            onDismiss()
        },
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ForestGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = appLanguage.tr(
                                    english = "NavKart Smart Helper",
                                    punjabi = "ਨਵਕਾਰਟ ਸਮਾਰਟ ਹੈਲਪਰ",
                                    hindi = "नवकार्ट स्मार्ट हेल्पर"
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            )
                            Text(
                                text = appLanguage.tr(
                                    english = "Gemini AI • EN / ਪੰਜਾਬੀ / हिंदी",
                                    punjabi = "Gemini AI • ਪੰਜਾਬੀ / हिंदी / EN",
                                    hindi = "Gemini AI • हिंदी / ਪੰਜਾਬੀ / EN"
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ForestGreen,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            autoSpeakReplies = !autoSpeakReplies
                            if (!autoSpeakReplies) ttsController.stop()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("smart_helper_auto_tts_toggle")
                    ) {
                        Icon(
                            imageVector = if (autoSpeakReplies) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (autoSpeakReplies) "Mute auto voice replies" else "Auto speak replies",
                            tint = if (autoSpeakReplies) ForestGreen else Color(0xFF6B7B72)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 1-Tap Trilingual Selector: English | ਪੰਜਾਬੀ | हिंदी
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val selected = appLanguage == lang
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectLanguage(lang) },
                            label = {
                                Text(
                                    text = lang.chipLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreen,
                                selectedLabelColor = SignatureGold
                            ),
                            modifier = Modifier.testTag("smart_helper_lang_${lang.code}")
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(390.dp)) {
                // Quick Suggestion Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPrompts.forEach { q ->
                        AssistChip(
                            onClick = { onSendPrompt(q, appLanguage) },
                            label = { Text(q, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dynamic Welcome Bubble
                    item(key = "welcome_bubble") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ForestContainer,
                            modifier = Modifier.fillMaxWidth(0.92f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = welcomeMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = OnForestContainer,
                                        lineHeight = 16.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (ttsController.activeMessageId == "welcome_bubble" && ttsController.isSpeaking) {
                                            ttsController.stop()
                                        } else {
                                            ttsController.speak("welcome_bubble", welcomeMessage, appLanguage)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (ttsController.activeMessageId == "welcome_bubble" && ttsController.isSpeaking) {
                                            Icons.Default.Stop
                                        } else {
                                            Icons.AutoMirrored.Filled.VolumeUp
                                        },
                                        contentDescription = "Listen",
                                        tint = ForestGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    items(messages, key = { it.id }) { msg ->
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
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = msg.message,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isAi) OnForestContainer else MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 17.sp
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isAi) {
                                        val playingThis = ttsController.activeMessageId == msg.id && ttsController.isSpeaking
                                        IconButton(
                                            onClick = {
                                                if (playingThis) ttsController.stop()
                                                else ttsController.speak(msg.id, msg.message, msg.language)
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("tts_play_${msg.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (playingThis) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = if (playingThis) "Stop audio" else "Play audio",
                                                tint = ForestGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Render Matched Customer-Scoped Cloud Order Card
                            msg.trackedCloudOrder?.let { o ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, ForestGreen),
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(o.orderId, fontWeight = FontWeight.Bold, color = ForestGreen)
                                            Text("₹${o.finalTotal}", fontWeight = FontWeight.ExtraBold)
                                        }
                                        Text("${appLanguage.tr("Stage", "ਸਥਿਤੀ", "स्टेटस")}: ${o.status}", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = "${o.customerName} (${o.customerPhone})",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                        Text(
                                            text = o.addressSnapshot,
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }

                            // Fallback Local Order Card
                            msg.trackedLocalOrder?.let { o ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, ForestGreen),
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(o.orderId, fontWeight = FontWeight.Bold, color = ForestGreen)
                                            Text("₹${o.finalTotal}", fontWeight = FontWeight.ExtraBold)
                                        }
                                        Text("${appLanguage.tr("Stage", "ਸਥਿਤੀ", "स्टेटस")}: ${o.status}", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = o.address,
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }

                            // Clickable Suggested Products returned by Gemini [SKU:...]
                            if (msg.suggestedProducts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                msg.suggestedProducts.forEach { p ->
                                    Card(
                                        onClick = { onSelectProduct(p) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .fillMaxWidth(0.92f)
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (p.fullImageUrl.isNotBlank()) {
                                                androidx.compose.runtime.key(p.id, p.fullImageUrl) {
                                                    val cacheKey = "${p.id}_${p.fullImageUrl}"
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(ProductImageAutoFetcher.resolveDisplayImageModel(context, p))
                                                            .memoryCacheKey(cacheKey)
                                                            .diskCacheKey(cacheKey)
                                                            .crossfade(false)
                                                            .build(),
                                                        contentDescription = p.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                val liveUnit = p.sellingPrice(categoryDiscounts[p.category], offersConfig)
                                                Text(
                                                    p.name,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1
                                                )
                                                Text(
                                                    "₹$liveUnit (MRP: ₹${p.mrp})",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = ForestGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }

                                            Button(
                                                onClick = { onAddToCart(p) },
                                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    appLanguage.tr("Add", "ਜੋੜੋ", "जोड़ें"),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (msg.showWhatsAppEscalation) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    onClick = {
                                        val uri = Uri.parse(
                                            "https://api.whatsapp.com/send?phone=${StoreProfile.WHATSAPP_NUMBER}&text=Hello%20NavKart%20Help%20Desk"
                                        )
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    },
                                    color = Color(0xFF25D366),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .height(34.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = appLanguage.tr(
                                                english = "Chat on WhatsApp (+91 8847370905)",
                                                punjabi = "ਵਟਸਐਪ 'ਤੇ ਗੱਲ ਕਰੋ (+91 8847370905)",
                                                hindi = "व्हाट्सएप पर सहायता लें (+91 8847370905)"
                                            ),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Streaming / Loading indicator
                    if (uiState is SmartHelperUiState.Loading || uiState is SmartHelperUiState.Streaming) {
                        item(key = "streaming_indicator") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ForestContainer,
                                modifier = Modifier.fillMaxWidth(0.92f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (uiState is SmartHelperUiState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = ForestGreen
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = appLanguage.tr(
                                                english = "Thinking...",
                                                punjabi = "ਸੋਚ ਰਿਹਾ ਹਾਂ...",
                                                hindi = "जवाब तैयार किया जा रहा है..."
                                            ),
                                            style = MaterialTheme.typography.bodySmall.copy(color = OnForestContainer)
                                        )
                                    } else if (uiState is SmartHelperUiState.Streaming) {
                                        Text(
                                            text = uiState.partialText.ifBlank { "..." },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = OnForestContainer,
                                                lineHeight = 16.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Transparent Error UI with Retry CTA
                    if (uiState is SmartHelperUiState.Error) {
                        item(key = "error_banner") {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = WholesaleRed.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, WholesaleRed.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("smart_helper_error_banner")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = WholesaleRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = uiState.message,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = WholesaleRed,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = onRetryPrompt) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(appLanguage.tr("Retry", "ਦੁਬਾਰਾ ਕੋਸ਼ਿਸ਼", "पुनः प्रयास करें"), fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(onClick = onDismissError) {
                                            Text(appLanguage.tr("Dismiss", "ਹਟਾਓ", "बंद करें"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (voiceController.isListening) {
                    Surface(
                        color = SignatureGold.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = voiceController.listeningLanguage.tr(
                                english = "Listening in English (en-IN)... Speak now",
                                punjabi = "ਪੰਜਾਬੀ (pa-IN) ਵਿੱਚ ਸੁਣਿਆ ਜਾ ਰਿਹਾ ਹੈ... ਬੋਲੋ ਜੀ",
                                hindi = "हिंदी (hi-IN) में सुना जा रहा है... अब बोलें"
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ForestGreenDark,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Input Bar with Voice Mic (hi-IN / pa-IN / en-IN) + Send
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        placeholder = {
                            Text(
                                text = appLanguage.tr(
                                    english = "Ask in English, ਪੰਜਾਬੀ, or हिंदी...",
                                    punjabi = "ਪੰਜਾਬੀ, हिंदी ਜਾਂ English ਵਿੱਚ ਪੁੱਛੋ...",
                                    hindi = "हिंदी में पूछें (राशन, छूट, ऑर्डर)..."
                                ),
                                fontSize = 11.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("smart_helper_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (voiceController.isListening) {
                                voiceController.stopListening()
                            } else {
                                voiceController.startListening(appLanguage)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (voiceController.isListening) WholesaleRed else SignatureGold)
                            .testTag("smart_helper_mic_btn")
                    ) {
                        Icon(
                            imageVector = if (voiceController.isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Voice Input (${appLanguage.speechLocaleTag})",
                            tint = if (voiceController.isListening) Color.White else ForestGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        enabled = inputQuery.isNotBlank() && uiState !is SmartHelperUiState.Loading,
                        onClick = {
                            if (inputQuery.isNotBlank()) {
                                val q = inputQuery.trim()
                                inputQuery = ""
                                onSendPrompt(q, null)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ForestGreen)
                            .testTag("smart_helper_send_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                ttsController.stop()
                voiceController.stopListening()
                onDismiss()
            }) {
                Text(appLanguage.tr("Close", "ਬੰਦ ਕਰੋ", "बंद करें"))
            }
        }
    )
}
