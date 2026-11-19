package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.AppLanguage
import com.example.data.model.StoreProfile
import com.example.data.security.InputSanitizer
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.SignatureGold
import com.example.ui.theme.SignatureGoldContainer
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Live ticking Cold-Chain Protected Express Dispatch (-18°C) countdown badge
 * shown whenever Frozen items are in the Cart.
 */
@Composable
fun ColdChainCountdownBadge(
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    modifier: Modifier = Modifier
) {
    var secondsRemaining by remember { mutableIntStateOf(15 * 60) }
    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining = (secondsRemaining - 1).coerceAtLeast(60)
        }
    }
    val mins = secondsRemaining / 60
    val secs = secondsRemaining % 60
    val clockLabel = "%02d:%02d".format(mins, secs)

    Surface(
        color = Color(0xFF01579B),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF81D4FA)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cold_chain_countdown_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = "Cold Chain -18°C",
                    tint = Color(0xFF81D4FA),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = appLanguage.tr(
                            english = "Cold-Chain Protected Express (-18°C)",
                            punjabi = "ਕੋਲਡ-ਚੇਨ ਸੁਰੱਖਿਅਤ ਐਕਸਪ੍ਰੈੱਸ (-18°C)",
                            hindi = "कोल्ड-चेन सुरक्षित एक्सप्रेस (-18°C)"
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        text = appLanguage.tr(
                            english = "Thermal insulated box reserved for your frozen items",
                            punjabi = "ਫ਼ਰੋਜ਼ਨ ਪੈਕਾਂ ਲਈ ਤਰਜੀਹੀ ਡਿਸਪੈਚ",
                            hindi = "आपके फ्रोज़न उत्पादों के लिए थर्मल इंसुलेटेड बॉक्स"
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFE1F5FE),
                            fontSize = 10.sp
                        )
                    )
                }
            }
            Surface(
                color = SignatureGold,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = clockLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = ForestGreenDark,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Send Parchi Photo (zero-permission Android Photo Picker) or 15-Second Punjabi Voice Order Dialog.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParchiVoiceOrderDialog(
    defaultPhone: String,
    isSignedIn: Boolean,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    onSignInFirst: () -> Unit,
    onDismiss: () -> Unit,
    onSubmitParchi: (
        mode: String,
        notesText: String,
        voiceTranscript: String,
        photoDataUrl: String,
        phoneInput: String
    ) -> Unit
) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf(defaultPhone) }
    var notesText by remember { mutableStateOf("") }
    var voiceTranscript by remember { mutableStateOf("") }
    var photoDataUrl by remember { mutableStateOf("") }
    var photoSelectedLabel by remember { mutableStateOf("") }
    var voiceLanguage by remember(appLanguage) { mutableStateOf(appLanguage) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encoded = encodeUriAsCompactDataUrl(context, uri)
            if (encoded != null) {
                photoDataUrl = encoded
                photoSelectedLabel = voiceLanguage.tr(
                    english = "Parchi Photo Attached (Ready)",
                    punjabi = "ਪਰਚੀ ਫੋਟੋ ਜੁੜ ਗਈ (ਤਿਆਰ)",
                    hindi = "पर्ची फोटो जुड़ गई है (तैयार)"
                )
                Toast.makeText(context, photoSelectedLabel, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not read selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val voiceController = rememberVoiceInputController(
        currentLanguage = voiceLanguage,
        promptTitle = voiceLanguage.tr(
            english = "Bol Ke Khareedo: Speak your NavKart list (English / ਪੰਜਾਬੀ / हिंदी)",
            punjabi = "ਬੋਲ ਕੇ ਖਰੀਦੋ: ਆਪਣੀ 15s ਲਿਸਟ ਬੋਲੋ (ਪੰਜਾਬੀ / हिंदी / English)",
            hindi = "बोल के खरीदें: अपनी 15 सेकंड की लिस्ट बोलें (हिंदी / ਪੰਜਾਬੀ / English)"
        )
    ) { spoken, detectedLang ->
        voiceLanguage = detectedLang
        voiceTranscript = if (voiceTranscript.isBlank()) spoken else "$voiceTranscript\n$spoken"
        Toast.makeText(
            context,
            detectedLang.tr(
                english = "Voice note captured!",
                punjabi = "ਵੌਇਸ ਨੋਟ ਸੇਵ ਹੋ ਗਿਆ!",
                hindi = "वॉइस नोट सेव हो गया!"
            ),
            Toast.LENGTH_SHORT
        ).show()
    }

    AlertDialog(
        onDismissRequest = {
            voiceController.stopListening()
            onDismiss()
        },
        icon = {
            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(32.dp))
        },
        title = {
            Text(
                text = voiceLanguage.tr(
                    english = "Send Parchi Photo / Bol Ke Khareedo",
                    punjabi = "ਪਰਚੀ ਫੋਟੋ / 15s ਬੋਲ ਕੇ ਖਰੀਦੋ ਆਰਡਰ",
                    hindi = "पर्ची फोटो / बोल के खरीदें (15s वॉइस ऑर्डर)"
                ),
                fontWeight = FontWeight.ExtraBold,
                color = ForestGreenDark,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = voiceLanguage.tr(
                        english = "Upload a handwritten Parchi photo or dictate in Hindi, Punjabi, or English. Sandhaur staff matches SKUs & pushes your ready bill.",
                        punjabi = "ਹੱਥ-ਲਿਖਤ ਪਰਚੀ ਦੀ ਫੋਟੋ ਭੇਜੋ ਜਾਂ ਪੰਜਾਬੀ/हिंदी/English ਵਿੱਚ ਬੋਲ ਕੇ ਲਿਸਟ ਲਿਖਵਾਓ। ਸੰਧੌੜ ਸਟਾਫ਼ ਤੁਹਾਡਾ ਬਿੱਲ ਬਣਾ ਕੇ ਭੇਜੇਗਾ।",
                        hindi = "हाथ से लिखी पर्ची की फोटो भेजें या हिंदी / ਪੰਜਾਬੀ / English में बोलकर अपनी लिस्ट लिखवाएं। संधौड़ स्टाफ आपका बिल तैयार करके भेजेगा।"
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4B5E54))
                )

                // Voice Recognition Language Selector (hi-IN | pa-IN | en-IN)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = voiceLanguage == lang,
                            onClick = { voiceLanguage = lang },
                            label = {
                                Text(
                                    "${lang.chipLabel} (${lang.speechLocaleTag})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("parchi_voice_lang_${lang.code}")
                        )
                    }
                }

                if (!isSignedIn) {
                    Surface(
                        color = SignatureGoldContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SignatureGold)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = voiceLanguage.tr(
                                    english = "Sign in with Gmail to push Parchi live & confirm your ready bill in My Orders.",
                                    punjabi = "ਪਰਚੀ ਭੇਜਣ ਅਤੇ ਮੇਰੇ ਆਰਡਰਾਂ ਵਿੱਚ ਬਿੱਲ ਦੇਖਣ ਲਈ Gmail ਨਾਲ ਸਾਈਨ ਇਨ ਕਰੋ।",
                                    hindi = "पर्ची लाइव भेजने और 'मेरे ऑर्डर' में बिल कन्फर्म करने के लिए Gmail से साइन इन करें।"
                                ),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onSignInFirst,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    voiceLanguage.tr("Sign In with Gmail", "Gmail ਨਾਲ ਸਾਈਨ ਇਨ ਕਰੋ", "Gmail से साइन इन करें"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        border = BorderStroke(1.dp, ForestGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("parchi_pick_photo_btn")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (photoDataUrl.isNotBlank()) {
                                voiceLanguage.tr("Change Photo", "ਫੋਟੋ ਬਦਲੋ", "फोटो बदलें")
                            } else {
                                voiceLanguage.tr("Parchi Photo", "ਪਰਚੀ ਫੋਟੋ", "पर्ची फोटो")
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                    }

                    Button(
                        onClick = {
                            if (voiceController.isListening) {
                                voiceController.stopListening()
                            } else {
                                voiceController.startListening(voiceLanguage)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (voiceController.isListening) Color(0xFFB71C1C) else ForestGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("parchi_voice_record_btn")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = SignatureGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (voiceController.isListening) {
                                voiceLanguage.tr("Listening...", "ਸੁਣ ਰਿਹਾ ਹਾਂ...", "सुन रहा हूँ...")
                            } else {
                                voiceLanguage.tr(
                                    english = "15s ${voiceLanguage.shortLabel} Mic",
                                    punjabi = "15s ${voiceLanguage.shortLabel} ਮਾਈਕ",
                                    hindi = "बोल के खरीदें (${voiceLanguage.shortLabel})"
                                )
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (photoSelectedLabel.isNotBlank()) {
                    Surface(
                        color = ForestGreenContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(photoSelectedLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreenDark)
                        }
                    }
                }

                if (voiceTranscript.isNotBlank()) {
                    OutlinedTextField(
                        value = voiceTranscript,
                        onValueChange = { voiceTranscript = it },
                        label = { Text(voiceLanguage.tr("15s Voice Transcript", "ਵੌਇਸ ਟ੍ਰਾਂਸਕ੍ਰਿਪਟ", "वॉइस ट्रांसक्रिप्ट")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = {
                        Text(
                            voiceLanguage.tr(
                                english = "Grocery / Parchi Items List",
                                punjabi = "ਲਿਸਟ ਲਿਖੋ (ਜਿਵੇਂ 10kg ਆਟਾ, 1L ਘਿਓ...)",
                                hindi = "राशन / सामान लिस्ट लिखें (जैसे 10kg आटा, 1L घी...)"
                            )
                        )
                    },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parchi_notes_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = InputSanitizer.sanitizePhone10(it) },
                    label = {
                        Text(
                            voiceLanguage.tr(
                                english = "10-Digit Indian Mobile",
                                punjabi = "10-ਅੰਕਾਂ ਦਾ ਮੋਬਾਈਲ ਨੰਬਰ",
                                hindi = "10 अंकों का मोबाइल नंबर"
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mode = when {
                        photoDataUrl.isNotBlank() -> "PARCHI_PHOTO"
                        voiceTranscript.isNotBlank() -> "VOICE_${voiceLanguage.name}"
                        else -> "VOICE_OR_TEXT"
                    }
                    voiceController.stopListening()
                    onSubmitParchi(mode, notesText, voiceTranscript, photoDataUrl, phone)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier.testTag("parchi_submit_btn")
            ) {
                Text(
                    voiceLanguage.tr("Send Parchi Order", "ਪਰਚੀ ਆਰਡਰ ਭੇਜੋ", "पर्ची ऑर्डर भेजें"),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                voiceController.stopListening()
                onDismiss()
            }) {
                Text(voiceLanguage.tr("Cancel", "ਰੱਦ ਕਰੋ", "रद्द करें"))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletTopupDialog(
    currentBalance: Int,
    isPunjabi: Boolean,
    appLanguage: AppLanguage = if (isPunjabi) AppLanguage.PUNJABI else AppLanguage.ENGLISH,
    onDismiss: () -> Unit,
    onSubmitTopup: (amount: Int, utrRef: String) -> Unit
) {
    val context = LocalContext.current
    var selectedAmount by remember { mutableIntStateOf(1000) }
    var utrRef by remember { mutableStateOf("") }
    val upiUri = remember(selectedAmount) {
        "upi://pay?pa=${StoreProfile.MERCHANT_UPI_VPA}&pn=NavKart%20Family%20Wallet&am=$selectedAmount&cu=INR&tn=NavKart%20Wallet%20Topup"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(30.dp))
        },
        title = {
            Text(
                text = appLanguage.tr(
                    english = "Load Prepaid Family Wallet",
                    punjabi = "ਨਵਕਾਰਟ ਫੈਮਿਲੀ ਵਾਲਿਟ ਲੋਡ ਕਰੋ",
                    hindi = "प्रीपेड फ़ैमिली वॉलेट लोड करें"
                ),
                fontWeight = FontWeight.ExtraBold,
                color = ForestGreenDark
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Current Balance: ₹$currentBalance • Earn +2% Bonus Nav Coins on wallet load!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(500, 1000, 2000, 5000).forEach { amt ->
                        FilterChip(
                            selected = selectedAmount == amt,
                            onClick = { selectedAmount = amt },
                            label = { Text("₹$amt", fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.25f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        UpiVerificationQrMatrix(upiUriPayload = upiUri, sizeDp = 124.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Pay ₹$selectedAmount via UPI & enter UTR below", fontSize = 10.sp, color = Color(0xFF4B5E54))
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(upiUri)))
                                }.onFailure {
                                    Toast.makeText(context, "Pay to ${StoreProfile.MERCHANT_UPI_VPA}", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Text("Open GPay / PhonePe App", fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = utrRef,
                    onValueChange = { utrRef = it },
                    label = { Text("UPI Transaction / UTR Reference") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitTopup(selectedAmount, utrRef)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Submit Top-Up", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun encodeUriAsCompactDataUrl(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val original = BitmapFactory.decodeStream(stream) ?: return null
            val maxDim = 640
            val scale = min(1f, maxDim.toFloat() / maxOf(original.width, original.height).toFloat())
            val w = (original.width * scale).toInt().coerceAtLeast(1)
            val h = (original.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(original, w, h, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 72, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        }
    }.getOrNull()
}
