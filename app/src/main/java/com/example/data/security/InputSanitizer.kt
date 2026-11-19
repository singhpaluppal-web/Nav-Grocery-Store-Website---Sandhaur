package com.example.data.security

import java.security.MessageDigest

object InputSanitizer {
    private val HTML_TAG_REGEX = Regex("<[^>]*>")
    private val CONTROL_CHARS_REGEX = Regex("[\\p{Cntrl}&&[^\n\t]]")

    fun sanitizeText(input: String, maxLen: Int = 200): String =
        input
            .replace(HTML_TAG_REGEX, " ")
            .replace(CONTROL_CHARS_REGEX, "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(maxLen)

    fun sanitizeName(input: String): String =
        sanitizeText(input, 80)

    fun sanitizePhone10(input: String): String {
        val digits = input.filter { it.isDigit() }
        val normalized = if (digits.length == 12 && digits.startsWith("91")) {
            digits.substring(2)
        } else if (digits.length == 11 && digits.startsWith("0")) {
            digits.substring(1)
        } else {
            digits
        }
        return normalized.take(10)
    }

    fun isValidIndianMobile(phone: String): Boolean =
        phone.matches(Regex("^[6-9][0-9]{9}$"))

    fun sanitizePin6(input: String): String =
        input.filter { it.isDigit() }.take(6)

    fun isValidPin6(pin: String): Boolean =
        pin.matches(Regex("^[0-9]{6}$"))

    fun validateAddressFields(
        fullName: String,
        mobile: String,
        street: String,
        villageOrCity: String,
        pinCode: String
    ): String? {
        val cleanName = sanitizeName(fullName)
        val cleanPhone = sanitizePhone10(mobile)
        val cleanStreet = sanitizeText(street, 200)
        val cleanVillage = sanitizeText(villageOrCity, 120)
        val cleanPin = sanitizePin6(pinCode)
        return when {
            cleanName.length < 2 -> "Please enter recipient full name (min 2 characters)."
            !isValidIndianMobile(cleanPhone) -> "Please enter a valid 10-digit Indian mobile number starting with 6-9."
            cleanStreet.length < 4 -> "Please enter house / street / ward address."
            cleanVillage.length < 2 -> "Please enter village or city (e.g., Sandhaur)."
            !isValidPin6(cleanPin) -> "Please enter a valid 6-digit PIN code (e.g., 148020)."
            else -> null
        }
    }

    fun sha256Hex(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
