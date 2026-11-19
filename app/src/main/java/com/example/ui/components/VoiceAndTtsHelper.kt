package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.data.ai.SmartHelperAiService
import com.example.data.model.AppLanguage
import java.util.Locale
import java.util.UUID

private const val VOICE_TAG = "NavKartVoiceTts"

@Stable
class VoiceInputController internal constructor(
    private val launchWithPermissionCheck: (AppLanguage?) -> Unit,
    private val stopInternal: () -> Unit
) {
    var isListening: Boolean by mutableStateOf(false)
        internal set

    var listeningLanguage: AppLanguage by mutableStateOf(AppLanguage.ENGLISH)
        internal set

    fun startListening(overrideLanguage: AppLanguage? = null) {
        launchWithPermissionCheck(overrideLanguage)
    }

    fun stopListening() {
        stopInternal()
    }
}

/**
 * Reusable Trilingual Voice Recognition helper supporting Hindi (`hi-IN`),
 * Punjabi (`pa-IN`), and English (`en-IN`).
 * Uses native Android [SpeechRecognizer] (when available + RECORD_AUDIO granted)
 * with graceful fallback to [RecognizerIntent.ACTION_RECOGNIZE_SPEECH].
 */
@Composable
fun rememberVoiceInputController(
    currentLanguage: AppLanguage,
    promptTitle: String? = null,
    onResult: (transcript: String, detectedLanguage: AppLanguage) -> Unit
): VoiceInputController {
    val context = LocalContext.current
    val latestLanguage by rememberUpdatedState(currentLanguage)
    val latestCallback by rememberUpdatedState(onResult)

    var recognizerRef by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var pendingLanguage by remember { mutableStateOf(currentLanguage) }

    val controller = remember {
        VoiceInputController(
            launchWithPermissionCheck = { _ -> },
            stopInternal = {
                runCatching { recognizerRef?.stopListening() }
            }
        )
    }

    val intentSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        controller.isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spoken.isNotBlank()) {
                val resolved = AppLanguage.detectFromText(spoken, pendingLanguage)
                latestCallback(spoken, resolved)
            }
        }
    }

    fun launchRecognizerIntentFallback(targetLang: AppLanguage) {
        val prompt = promptTitle ?: targetLang.tr(
            english = "Speak product name or question (English / हिंदी / ਪੰਜਾਬੀ)",
            punjabi = "ਉਤਪਾਦ ਦਾ ਨਾਮ ਜਾਂ ਸਵਾਲ ਬੋਲੋ (ਪੰਜਾਬੀ / हिंदी / English)",
            hindi = "प्रोडक्ट का नाम या सवाल बोलें (हिंदी / ਪੰਜਾਬੀ / English)"
        )
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLang.speechLocaleTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLang.speechLocaleTag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        runCatching {
            controller.isListening = true
            controller.listeningLanguage = targetLang
            intentSpeechLauncher.launch(intent)
        }.onFailure {
            controller.isListening = false
            Toast.makeText(
                context,
                targetLang.tr(
                    english = "Voice recognition unavailable on this device; please type instead.",
                    punjabi = "ਇਸ ਡਿਵਾਈਸ 'ਤੇ ਵੌਇਸ ਸਰਵਿਸ ਉਪਲਬਧ ਨਹੀਂ ਹੈ; ਕਿਰਪਾ ਕਰਕੇ ਟਾਈਪ ਕਰੋ।",
                    hindi = "इस डिवाइस पर वॉइस सेवा उपलब्ध नहीं है; कृपया टाइप करें।"
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun startDirectSpeechRecognizer(targetLang: AppLanguage) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            launchRecognizerIntentFallback(targetLang)
            return
        }
        try {
            recognizerRef?.destroy()
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            recognizerRef = sr
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    controller.isListening = true
                    controller.listeningLanguage = targetLang
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    controller.isListening = false
                    if (error == SpeechRecognizer.ERROR_CLIENT ||
                        error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                    ) {
                        launchRecognizerIntentFallback(targetLang)
                    } else if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Log.w(VOICE_TAG, "SpeechRecognizer error code: $error")
                    }
                }

                override fun onResults(results: Bundle?) {
                    controller.isListening = false
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (spoken.isNotBlank()) {
                        val resolved = AppLanguage.detectFromText(spoken, targetLang)
                        latestCallback(spoken, resolved)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLang.speechLocaleTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLang.speechLocaleTag)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            controller.isListening = true
            controller.listeningLanguage = targetLang
            sr.startListening(recognizerIntent)
        } catch (e: Exception) {
            Log.w(VOICE_TAG, "Direct SpeechRecognizer start failed, using intent fallback", e)
            controller.isListening = false
            launchRecognizerIntentFallback(targetLang)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startDirectSpeechRecognizer(pendingLanguage)
        } else {
            launchRecognizerIntentFallback(pendingLanguage)
        }
    }

    val wiredController = remember(controller) {
        VoiceInputController(
            launchWithPermissionCheck = { overrideLang ->
                val target = overrideLang ?: latestLanguage
                pendingLanguage = target
                val hasAudioPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasAudioPermission) {
                    startDirectSpeechRecognizer(target)
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            stopInternal = {
                controller.isListening = false
                runCatching { recognizerRef?.stopListening() }
            }
        )
    }

    // Keep state properties mirrored
    wiredController.isListening = controller.isListening
    wiredController.listeningLanguage = controller.listeningLanguage

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recognizerRef?.destroy() }
            recognizerRef = null
        }
    }

    return wiredController
}

@Stable
class TrilingualTtsController(
    private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isReady: Boolean = false
    private var pendingUtterance: Triple<String, String, AppLanguage>? = null

    var isSpeaking: Boolean by mutableStateOf(false)
        private set

    var activeMessageId: String? by mutableStateOf(null)
        private set

    fun speak(
        messageId: String,
        rawText: String,
        preferredLanguage: AppLanguage
    ) {
        val detectedLang = AppLanguage.detectTtsLanguage(rawText, preferredLanguage)
        val cleanSpeech = SmartHelperAiService.sanitizeForTts(rawText, detectedLang)
        if (cleanSpeech.isBlank()) return

        if (tts == null) {
            pendingUtterance = Triple(messageId, cleanSpeech, detectedLang)
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isReady = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking = true
                        }

                        override fun onDone(utteranceId: String?) {
                            isSpeaking = false
                            activeMessageId = null
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                            activeMessageId = null
                        }
                    })
                    pendingUtterance?.let { (id, text, lang) ->
                        pendingUtterance = null
                        dispatchSpeak(id, text, lang)
                    }
                } else {
                    Log.w(VOICE_TAG, "TextToSpeech init failed with status=$status")
                    isSpeaking = false
                    activeMessageId = null
                }
            }
        } else if (isReady) {
            dispatchSpeak(messageId, cleanSpeech, detectedLang)
        } else {
            pendingUtterance = Triple(messageId, cleanSpeech, detectedLang)
        }
    }

    private fun dispatchSpeak(
        messageId: String,
        cleanSpeech: String,
        language: AppLanguage
    ) {
        val engine = tts ?: return
        val localeCandidates = when (language) {
            AppLanguage.HINDI -> listOf(
                Locale.forLanguageTag("hi-IN"),
                Locale.forLanguageTag("hi"),
                Locale.forLanguageTag("en-IN")
            )
            AppLanguage.PUNJABI -> listOf(
                Locale.forLanguageTag("pa-IN"),
                Locale.forLanguageTag("pa"),
                Locale.forLanguageTag("hi-IN"),
                Locale.forLanguageTag("en-IN")
            )
            AppLanguage.ENGLISH -> listOf(
                Locale.forLanguageTag("en-IN"),
                Locale.US
            )
        }

        for (candidate in localeCandidates) {
            val res = engine.setLanguage(candidate)
            if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                break
            }
        }

        val utteranceId = "${messageId}_${UUID.randomUUID()}"
        activeMessageId = messageId
        isSpeaking = true
        engine.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        pendingUtterance = null
        runCatching { tts?.stop() }
        isSpeaking = false
        activeMessageId = null
    }

    fun shutdown() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
        isReady = false
    }
}

@Composable
fun rememberTrilingualTtsController(): TrilingualTtsController {
    val context = LocalContext.current
    val controller = remember(context) { TrilingualTtsController(context) }
    DisposableEffect(controller) {
        onDispose { controller.shutdown() }
    }
    return controller
}
