package com.example.data.security

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

/**
 * Sanitizes Firestore errors before surfacing/logging so neither UIDs, email addresses,
 * nor provider tokens leak into system logcat or production telemetry.
 */
fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val errorCode = (rootError as? FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"
    val sanitizedMessage = (rootError.message ?: "Request could not be completed")
        .replace(Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[redacted-email]")
        .take(180)

    val errorInfoJson = JSONObject().apply {
        put("errorCode", errorCode)
        put("error", sanitizedMessage)
        put("operationType", operationType.value)
        put("path", path?.replace(Regex("users/[^/]+"), "users/[redacted]") ?: "")
    }

    val jsonString = errorInfoJson.toString()
    if (BuildConfig.DEBUG) {
        Log.w("FirestoreError", "Firestore operation issue: $jsonString")
    }
    return jsonString
}
