package com.example.data.model

/**
 * Supported customer UI, voice recognition (STT), TTS, and Gemini Smart Helper languages.
 */
enum class AppLanguage(
    val code: String,
    val speechLocaleTag: String,
    val shortLabel: String,
    val chipLabel: String
) {
    ENGLISH(
        code = "en",
        speechLocaleTag = "en-IN",
        shortLabel = "EN",
        chipLabel = "English"
    ),
    PUNJABI(
        code = "pa",
        speechLocaleTag = "pa-IN",
        shortLabel = "ਪੰਜਾਬੀ",
        chipLabel = "ਪੰਜਾਬੀ"
    ),
    HINDI(
        code = "hi",
        speechLocaleTag = "hi-IN",
        shortLabel = "हिंदी",
        chipLabel = "हिंदी"
    );

    fun tr(english: String, punjabi: String, hindi: String): String = when (this) {
        ENGLISH -> english
        PUNJABI -> punjabi
        HINDI -> hindi
    }

    fun next(): AppLanguage = when (this) {
        ENGLISH -> PUNJABI
        PUNJABI -> HINDI
        HINDI -> ENGLISH
    }

    companion object {
        fun fromCode(raw: String?): AppLanguage = when (raw?.trim()?.lowercase()) {
            "hi", "hi-in", "hindi" -> HINDI
            "pa", "pa-in", "punjabi" -> PUNJABI
            else -> ENGLISH
        }

        /**
         * Auto-detects language from Unicode script or explicit Romanized Hindi/Punjabi cues.
         * Falls back to [fallback] when text is numeric/SKU/ambiguous.
         */
        fun detectFromText(text: String, fallback: AppLanguage): AppLanguage {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return fallback
            if (trimmed.any { it in '\u0900'..'\u097F' }) return HINDI
            if (trimmed.any { it in '\u0A00'..'\u0A7F' }) return PUNJABI

            val lower = trimmed.lowercase()
            val hindiWords = listOf(
                "hindi", "namaste", "bhaiya", "kya", "kitne", "chahiye",
                "dikhao", "batao", "khareedna", "sasta", "raashan", "aata",
                "ka rate", "mere order"
            )
            if (hindiWords.any { lower.contains(it) }) return HINDI

            val punjabiWords = listOf(
                "punjabi", "sat sri akal", "veere", "daso", "kinne",
                "chahida", "vikhao", "khareedo", "parchi"
            )
            if (punjabiWords.any { lower.contains(it) }) return PUNJABI

            val hasLetters = trimmed.any { it.isLetter() }
            return if (hasLetters && trimmed.all { it.code < 128 }) {
                if (fallback == ENGLISH) ENGLISH else fallback
            } else {
                fallback
            }
        }

        /**
         * Determines the best Android Text-to-Speech locale based strictly on the output text script,
         * falling back cleanly to [fallback].
         */
        fun detectTtsLanguage(text: String, fallback: AppLanguage): AppLanguage {
            if (text.any { it in '\u0900'..'\u097F' }) return HINDI
            if (text.any { it in '\u0A00'..'\u0A7F' }) return PUNJABI
            return if (text.any { it in 'a'..'z' || it in 'A'..'Z' }) ENGLISH else fallback
        }
    }
}
