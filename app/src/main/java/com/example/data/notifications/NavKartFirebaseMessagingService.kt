package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles Firebase Cloud Messaging token registration and live order status alerts
 * (Order Confirmed, Packing, Out for Delivery, Delivered).
 * Respects Android 13+ POST_NOTIFICATIONS runtime consent before posting notifications.
 */
class NavKartFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        cacheTokenLocally(applicationContext, token)
        syncTokenForCurrentUser(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Nav Grocery Store Navkart"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["statusMessage"]
            ?: "Your order status has been updated."
        val orderId = message.data["orderId"].orEmpty()

        showOrderUpdateNotification(
            context = applicationContext,
            title = title,
            body = body,
            notificationId = if (orderId.isNotBlank()) orderId.hashCode() else System.currentTimeMillis().toInt()
        )
    }

    companion object {
        private const val TAG = "NavKartFCM"
        const val CHANNEL_ORDER_UPDATES = "navkart_orders"
        private const val PREF_NAME = "navkart_fcm_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"

        fun ensureOrderChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                val existing = manager.getNotificationChannel(CHANNEL_ORDER_UPDATES)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ORDER_UPDATES,
                        "Order Status & Delivery Updates",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Live notifications when your Nav Grocery Store order is confirmed, out for delivery, or delivered."
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun showOrderUpdateNotification(
            context: Context,
            title: String,
            body: String,
            notificationId: Int
        ) {
            if (!hasNotificationPermission(context)) return
            ensureOrderChannel(context)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ORDER_UPDATES)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            } catch (sec: SecurityException) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted", sec)
            }
        }

        fun cacheTokenLocally(context: Context, token: String) {
            if (token.isBlank()) return
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FCM_TOKEN, token)
                .apply()
        }

        fun fetchAndSyncCurrentToken(context: Context) {
            ensureOrderChannel(context)
            runCatching {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        cacheTokenLocally(context, token)
                        syncTokenForCurrentUser(context, token)
                    }
                }
            }
        }

        fun syncTokenForCurrentUser(context: Context, tokenOverride: String? = null) {
            val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
                ?.takeIf { it.isNotBlank() } ?: return
            val token = tokenOverride?.takeIf { it.isNotBlank() }
                ?: context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_FCM_TOKEN, null)
                    ?.takeIf { it.isNotBlank() }
                ?: return

            val trimmedToken = token.take(512)
            runCatching {
                val fs = FirebaseFirestore.getInstance(
                    context.applicationContext.getString(R.string.firestore_database_id)
                )
                fs.collection("users").document(uid)
                    .update(
                        mapOf(
                            "fcmToken" to trimmedToken,
                            "fcmTokenUpdatedAtMs" to System.currentTimeMillis(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .addOnFailureListener { err ->
                        Log.w(TAG, "FCM token Firestore sync deferred: ${err.message}")
                    }
            }
            runCatching {
                FirebaseDatabase.getInstance("https://nav-grocery-store-default-rtdb.firebaseio.com/")
                    .reference
                    .child("users")
                    .child(uid)
                    .updateChildren(
                        mapOf(
                            "fcmToken" to trimmedToken,
                            "fcmTokenUpdatedAtMs" to ServerValue.TIMESTAMP
                        )
                    )
            }
        }
    }
}
