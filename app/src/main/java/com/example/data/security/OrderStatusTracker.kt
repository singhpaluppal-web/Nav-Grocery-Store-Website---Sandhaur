package com.example.data.security

import com.example.data.model.FirestoreOrderRecord

data class OrderStatusChange(
    val id: String,
    val orderId: String,
    val previousStatus: String,
    val newStatus: String,
    val deliveryProofUrl: String = "",
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Tracks real-time order status updates per signed-in UID and emits [OrderStatusChange] events
 * strictly when an observed order transitions from a previously known status to a new status
 * (e.g., "Packing" -> "Out for Delivery"), preventing false notifications on initial sign-in.
 */
class OrderStatusTracker {
    private var activeUid: String? = null
    private var baselineInitialized: Boolean = false
    private val knownStatuses = LinkedHashMap<String, String>()

    fun reset(uid: String? = null) {
        activeUid = uid
        baselineInitialized = false
        knownStatuses.clear()
    }

    fun onOrdersSnapshot(
        uid: String?,
        orders: List<FirestoreOrderRecord>
    ): List<OrderStatusChange> {
        if (uid.isNullOrBlank()) {
            reset(null)
            return emptyList()
        }
        if (activeUid != uid) {
            reset(uid)
        }

        if (!baselineInitialized) {
            for (order in orders) {
                val oid = order.orderId.ifBlank { order.id }.trim()
                val status = order.status.trim()
                if (oid.isNotBlank() && status.isNotBlank()) {
                    knownStatuses[oid] = status
                }
            }
            baselineInitialized = true
            return emptyList()
        }

        val changes = mutableListOf<OrderStatusChange>()
        for (order in orders) {
            val oid = order.orderId.ifBlank { order.id }.trim()
            val nextStatus = order.status.trim()
            if (oid.isBlank() || nextStatus.isBlank()) continue

            val previousStatus = knownStatuses[oid]
            if (previousStatus != null && !previousStatus.equals(nextStatus, ignoreCase = true)) {
                changes.add(
                    OrderStatusChange(
                        id = "${oid}_${nextStatus}_${System.nanoTime()}",
                        orderId = oid,
                        previousStatus = previousStatus,
                        newStatus = nextStatus,
                        deliveryProofUrl = order.deliveryProofUrl
                    )
                )
            }
            knownStatuses[oid] = nextStatus
        }
        return changes
    }

    companion object {
        fun statusStageRank(status: String): Int {
            val normalized = status.trim().lowercase()
            return when {
                normalized.contains("deliver") && !normalized.contains("out") -> 4
                normalized.contains("out for delivery") || normalized.contains("dispatched") -> 3
                normalized.contains("pack") -> 2
                normalized.contains("order placed") || normalized.contains("pending") || normalized.contains("confirmed") -> 1
                normalized.contains("cancel") || normalized.contains("refund") -> 5
                else -> 0
            }
        }

        /**
         * Resolves live status between Firestore and RTDB so real-time updates from either source
         * advance immediately without stale defaults overwriting newer stages.
         */
        fun resolveMergedStatus(firestoreStatus: String, rtdbStatus: String): String {
            val fs = firestoreStatus.trim()
            val rtdb = rtdbStatus.trim()
            if (rtdb.isBlank()) return fs
            if (fs.isBlank()) return rtdb
            if (fs.equals(rtdb, ignoreCase = true)) return fs

            val fsRank = statusStageRank(fs)
            val rtdbRank = statusStageRank(rtdb)
            return when {
                rtdbRank > fsRank -> rtdb
                fsRank > rtdbRank -> fs
                else -> rtdb
            }
        }
    }
}
