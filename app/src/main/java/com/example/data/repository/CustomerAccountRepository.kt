package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.example.data.api.CheckoutBackendService
import com.example.data.api.RetrofitClient
import com.example.data.api.SecureCheckoutItemPayload
import com.example.data.api.SecureOrderSubmissionRequest
import com.example.data.model.CartItem
import com.example.data.model.DeliveryOption
import com.example.data.model.FirestoreOrderItem
import com.example.data.model.FirestoreOrderRecord
import com.example.data.model.ParchiRequest
import com.example.data.model.PaymentMethod
import com.example.data.model.RashanSubscription
import com.example.data.model.SavedAddress
import com.example.data.model.ShoppingQuest
import com.example.data.model.SpinWheelSlice
import com.example.data.model.StoreOffersConfig
import com.example.data.model.StoreProfile
import com.example.data.model.UserAccount
import com.example.data.security.OperationType
import com.example.data.security.OrderStatusTracker
import com.example.data.security.VerifiedCheckoutSummary
import com.example.data.security.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

data class PlacedCloudOrderResult(
    val orderId: String,
    val paymentSessionUrl: String? = null
)

/**
 * Unified Firestore + Firebase RTDB repository for authenticated Google customers.
 * Supports:
 *   - User profile, Pind Ambassador referral code & Prepaid Family Wallet balance
 *   - Saved delivery addresses & Pind-to-Pind scheduled slots
 *   - Server-verified checkout payload submission (identifiers + quantity + idempotencyKey only)
 *   - Live 4-stage Order Tracker + Order Cancellation + DPDP Account Deletion
 *   - Monthly Mahina Rashan Box subscriptions & Parchi/Voice quick bills
 */
class CustomerAccountRepository(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val checkoutBackendService: CheckoutBackendService = RetrofitClient.checkoutBackendService
) {
    private val databaseId: String = context.applicationContext.getString(R.string.firestore_database_id)
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId)
    private val rtdb: FirebaseDatabase? = runCatching {
        FirebaseDatabase.getInstance(RTDB_URL)
    }.getOrNull()

    fun observeUserAccount(uid: String?): Flow<UserAccount?> {
        if (uid.isNullOrBlank() || auth.currentUser?.uid != uid) return flowOf(null)
        val firestoreFlow = firestore.collection("users").document(uid)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.GET, "users/$uid")
                emit(null)
            }

        val rtdbFinancialFlow = observeRtdbFinancialState(uid)

        return combine(firestoreFlow, rtdbFinancialFlow) { fsAccount, rtdbFin ->
            fsAccount?.let { acc ->
                val effectiveWallet = if (rtdbFin.walletBalanceRupees > 0L) {
                    (rtdbFin.walletBalanceRupees - acc.walletSpentRupees).coerceAtLeast(0L)
                } else {
                    acc.walletBalanceRupees
                }
                acc.copy(
                    walletBalanceRupees = effectiveWallet
                )
            }
        }
    }

    private data class RtdbFinancialState(
        val walletBalanceRupees: Long = 0L
    )

    private fun observeRtdbFinancialState(uid: String): Flow<RtdbFinancialState> = callbackFlow {
        val db = rtdb
        if (db == null) {
            trySend(RtdbFinancialState())
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.reference.child("users/$uid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wallet = (snapshot.child("walletBalanceRupees").value as? Number)?.toLong() ?: 0L
                trySend(RtdbFinancialState(wallet))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(RtdbFinancialState())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeSavedAddresses(uid: String?): Flow<List<SavedAddress>> {
        if (uid.isNullOrBlank() || auth.currentUser?.uid != uid) return flowOf(emptyList())
        return firestore.collection("users").document(uid)
            .collection("addresses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(SavedAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.defaultAddress }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "users/$uid/addresses")
                emit(emptyList())
            }
    }

    fun observeCloudOrders(uid: String?): Flow<List<FirestoreOrderRecord>> {
        if (uid.isNullOrBlank() || auth.currentUser?.uid != uid) return flowOf(emptyList())
        val fsFlow = firestore.collection("users").document(uid)
            .collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreOrderRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "users/$uid/orders")
                emit(emptyList())
            }

        val rtdbOrderUpdatesFlow = observeRtdbCustomerOrders(uid)

        return combine(fsFlow, rtdbOrderUpdatesFlow) { fsList, rtdbStatusMap ->
            fsList.map { ord ->
                val live = rtdbStatusMap[ord.orderId]
                if (live != null) {
                    ord.copy(
                        status = OrderStatusTracker.resolveMergedStatus(ord.status, live.first),
                        deliveryProofUrl = live.second.ifBlank { ord.deliveryProofUrl }
                    )
                } else ord
            }
        }
    }

    private fun observeRtdbCustomerOrders(uid: String): Flow<Map<String, Pair<String, String>>> = callbackFlow {
        val db = rtdb
        if (db == null) {
            trySend(emptyMap())
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.reference.child("customer_orders/$uid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Pair<String, String>>()
                for (c in snapshot.children) {
                    val oid = c.child("orderId").getValue(String::class.java) ?: c.key ?: continue
                    val st = c.child("status").getValue(String::class.java).orEmpty()
                    val proof = c.child("deliveryProofUrl").getValue(String::class.java).orEmpty()
                    map[oid] = st to proof
                }
                trySend(map)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyMap())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeParchiRequests(uid: String?): Flow<List<ParchiRequest>> {
        if (uid.isNullOrBlank() || auth.currentUser?.uid != uid) return flowOf(emptyList())
        return callbackFlow {
            val db = rtdb
            if (db == null) {
                trySend(emptyList())
                awaitClose {}
                return@callbackFlow
            }
            val ref = db.reference.child("customer_parchi/$uid")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children.mapNotNull { c ->
                        ParchiRequest(
                            requestId = c.child("requestId").getValue(String::class.java) ?: c.key.orEmpty(),
                            uid = uid,
                            customerName = c.child("customerName").getValue(String::class.java).orEmpty(),
                            customerPhone = c.child("customerPhone").getValue(String::class.java).orEmpty(),
                            mode = c.child("mode").getValue(String::class.java) ?: "PUNJABI_VOICE",
                            notesText = c.child("notesText").getValue(String::class.java).orEmpty(),
                            photoDataUrl = c.child("photoDataUrl").getValue(String::class.java).orEmpty(),
                            voiceTranscript = c.child("voiceTranscript").getValue(String::class.java).orEmpty(),
                            status = c.child("status").getValue(String::class.java) ?: "SUBMITTED",
                            matchedSummary = c.child("matchedSummary").getValue(String::class.java).orEmpty(),
                            readyBillTotal = (c.child("readyBillTotal").value as? Number)?.toInt() ?: 0,
                            createdAtMs = (c.child("createdAtMs").value as? Number)?.toLong() ?: 0L
                        )
                    }.sortedByDescending { it.createdAtMs }
                    trySend(list)
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }
    }

    fun observeRashanSubscription(uid: String?): Flow<RashanSubscription?> {
        if (uid.isNullOrBlank() || auth.currentUser?.uid != uid) return flowOf(null)
        return firestore.collection("users").document(uid)
            .collection("rashan_box").document("monthly")
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(RashanSubscription::class.java)
            }
            .catch { emit(null) }
    }

    fun observeQuests(uid: String?): Flow<List<ShoppingQuest>> {
        if (uid.isNullOrBlank() || auth.currentUser?.uid != uid) {
            return flowOf(buildDefaultQuests(null, emptyList()))
        }
        return firestore.collection("users").document(uid)
            .collection("quests")
            .snapshots()
            .map { snap ->
                val rawMap = snap.documents.associate { doc ->
                    val qid = doc.getString("questId") ?: doc.id
                    val prog = (doc.get("progress") as? Number)?.toInt() ?: 0
                    val claimed = doc.getBoolean("claimed") ?: false
                    qid to Pair(prog, claimed)
                }
                val defaults = buildDefaultQuests(null, emptyList())
                defaults.map { base ->
                    val entry = rawMap[base.id]
                    if (entry != null) {
                        val prog = entry.first.coerceAtMost(base.target)
                        base.copy(
                            progress = prog,
                            completed = prog >= base.target,
                            claimed = entry.second
                        )
                    } else base
                }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "users/$uid/quests")
                emit(buildDefaultQuests(null, emptyList()))
            }
    }

    suspend fun ensureUserAccount(user: FirebaseUser): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = user.uid
        val docRef = firestore.collection("users").document(uid)
        val generatedRefCode = "PIND-${uid.replace(Regex("[^a-zA-Z0-9]"), "").uppercase().take(6).ifBlank { "148020" }}"
        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = hashMapOf<String, Any>(
                    "uid" to uid,
                    "email" to (user.email ?: "").take(120),
                    "displayName" to (user.displayName ?: "NavKart Shopper").take(80),
                    "referralCode" to generatedRefCode,
                    "referredBy" to "",
                    "walletBalanceRupees" to 0L,
                    "walletSpentRupees" to 0L,
                    "navCoins" to 25L,
                    "lastSpinAtMs" to 0L,
                    "spinCount" to 0L,
                    "activeSpinCouponCode" to "",
                    "activeSpinCouponLabel" to "",
                    "activeSpinCouponDiscountRupees" to 0L,
                    "activeSpinFreeExpress" to false,
                    "activeSpinFreeLocal" to false,
                    "activeSpinUsed" to false,
                    "hasOrderedGrocery" to false,
                    "hasOrderedElectronics" to false,
                    "coldChainStaplesCount" to 0L,
                    "weeklyOrdersCount" to 0L,
                    "weeklyWindowStartMs" to System.currentTimeMillis(),
                    "totalOrdersCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                user.photoUrl?.toString()?.takeIf { it.isNotBlank() }?.let { payload["photoUrl"] = it.take(500) }
                user.phoneNumber?.takeIf { it.isNotBlank() }?.let { payload["phone"] = it.take(20) }
                docRef.set(payload).await()
                syncRtdbProfile(uid, payload.filterKeys { it != "createdAt" && it != "updatedAt" })
                syncRtdbNode("referrals/$generatedRefCode", mapOf(
                    "code" to generatedRefCode,
                    "ownerUid" to uid,
                    "ownerName" to (user.displayName ?: "Sandhaur Member"),
                    "rewardCoins" to 50
                ))
            } else {
                val updates = hashMapOf<String, Any>(
                    "displayName" to (user.displayName ?: existing.getString("displayName") ?: "NavKart Shopper").take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (existing.getString("referralCode").isNullOrBlank()) {
                    updates["referralCode"] = generatedRefCode
                }
                user.photoUrl?.toString()?.takeIf { it.isNotBlank() }?.let { updates["photoUrl"] = it.take(500) }
                docRef.set(updates, SetOptions.merge()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun saveAddress(
        uid: String,
        addressId: String?,
        fullName: String,
        mobile: String,
        streetAddress: String,
        landmark: String,
        villageOrCity: String,
        pinCode: String,
        makeDefault: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        if (auth.currentUser?.uid != uid) {
            return@withContext Result.failure(IllegalStateException("Authentication required to save delivery address."))
        }
        val collectionRef = firestore.collection("users").document(uid).collection("addresses")
        val targetId = addressId?.takeIf { it.isNotBlank() } ?: "addr_${UUID.randomUUID().toString().take(10)}"
        val docRef = collectionRef.document(targetId)
        try {
            if (makeDefault) {
                val existingSnap = collectionRef.get().await()
                existingSnap.documents.forEach { d ->
                    if (d.id != targetId && d.getBoolean("defaultAddress") == true) {
                        d.reference.update(mapOf("defaultAddress" to false, "updatedAt" to FieldValue.serverTimestamp())).await()
                    }
                }
            }
            val existingDoc = docRef.get().await()
            if (existingDoc.exists()) {
                val updateMap = hashMapOf<String, Any>(
                    "fullName" to fullName.take(80),
                    "mobile" to mobile,
                    "streetAddress" to streetAddress.take(200),
                    "villageOrCity" to villageOrCity.take(120),
                    "pinCode" to pinCode,
                    "defaultAddress" to makeDefault,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (landmark.isNotBlank()) updateMap["landmark"] = landmark.take(120)
                docRef.update(updateMap).await()
            } else {
                val createMap = hashMapOf<String, Any>(
                    "uid" to uid,
                    "fullName" to fullName.take(80),
                    "mobile" to mobile,
                    "streetAddress" to streetAddress.take(200),
                    "villageOrCity" to villageOrCity.take(120),
                    "pinCode" to pinCode,
                    "defaultAddress" to makeDefault,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (landmark.isNotBlank()) createMap["landmark"] = landmark.take(120)
                docRef.set(createMap).await()
            }
            upsertQuestProgress(uid, QUEST_PROFILE_ADDRESS, progress = 100, target = 100, rewardCoins = 40)
            syncRtdbNode("users/$uid/addresses/$targetId", mapOf(
                "fullName" to fullName,
                "mobile" to mobile,
                "streetAddress" to streetAddress,
                "landmark" to landmark,
                "villageOrCity" to villageOrCity,
                "pinCode" to pinCode,
                "defaultAddress" to makeDefault
            ))
            Result.success(targetId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/addresses/$targetId")
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(uid: String, addressId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (auth.currentUser?.uid != uid) return@withContext Result.failure(IllegalStateException("Sign in required"))
        try {
            firestore.collection("users").document(uid).collection("addresses").document(addressId).delete().await()
            rtdb?.reference?.child("users/$uid/addresses/$addressId")?.removeValue()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/addresses/$addressId")
            Result.failure(e)
        }
    }

    suspend fun setDefaultAddress(uid: String, addressId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (auth.currentUser?.uid != uid) return@withContext Result.failure(IllegalStateException("Sign in required"))
        val col = firestore.collection("users").document(uid).collection("addresses")
        try {
            val docs = col.get().await().documents
            docs.forEach { d ->
                val shouldBeDefault = d.id == addressId
                if (d.getBoolean("defaultAddress") != shouldBeDefault) {
                    d.reference.update(mapOf("defaultAddress" to shouldBeDefault, "updatedAt" to FieldValue.serverTimestamp())).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/addresses/$addressId")
            Result.failure(e)
        }
    }

    suspend fun performSpinWheel(
        uid: String,
        offersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Result<SpinWheelSlice> = withContext(Dispatchers.IO) {
        if (auth.currentUser?.uid != uid) {
            return@withContext Result.failure(IllegalStateException("Sign in with Google to spin & win rewards."))
        }
        val docRef = firestore.collection("users").document(uid)
        val wonSlice = SpinWheelSlice.entries[Random.nextInt(SpinWheelSlice.entries.size)]
        try {
            val coinsAllowed = offersConfig.coinConfig.isCoinsEnabled
            firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                val nowMs = System.currentTimeMillis()
                val lastSpin = (snap.get("lastSpinAtMs") as? Number)?.toLong() ?: 0L
                val existingSpins = (snap.get("spinCount") as? Number)?.toLong() ?: 0L
                val existingCoins = (snap.get("navCoins") as? Number)?.toLong() ?: 0L

                if (lastSpin > 0L && (nowMs - lastSpin) < UserAccount.SPIN_COOLDOWN_MS) {
                    throw IllegalStateException("Spin Wheel unlocks once every 24 hours.")
                }

                val addedCoins = if (coinsAllowed) wonSlice.bonusCoins.toLong() else 0L
                val newCoins = (existingCoins + addedCoins).coerceAtMost(50_000L)
                val hasCoupon = wonSlice.couponCode.isNotBlank()
                val updates = hashMapOf<String, Any>(
                    "navCoins" to newCoins,
                    "lastSpinAtMs" to nowMs,
                    "spinCount" to existingSpins + 1L,
                    "activeSpinCouponCode" to if (hasCoupon) wonSlice.couponCode else (snap.getString("activeSpinCouponCode") ?: ""),
                    "activeSpinCouponLabel" to if (hasCoupon) wonSlice.title else (snap.getString("activeSpinCouponLabel") ?: ""),
                    "activeSpinCouponDiscountRupees" to if (hasCoupon) wonSlice.discountRupees.toLong() else ((snap.get("activeSpinCouponDiscountRupees") as? Number)?.toLong() ?: 0L),
                    "activeSpinFreeExpress" to if (hasCoupon) wonSlice.freeExpress else (snap.getBoolean("activeSpinFreeExpress") ?: false),
                    "activeSpinFreeLocal" to if (hasCoupon) wonSlice.freeLocal else (snap.getBoolean("activeSpinFreeLocal") ?: false),
                    "activeSpinUsed" to if (hasCoupon) false else (snap.getBoolean("activeSpinUsed") ?: false),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.update(docRef, updates)
                wonSlice
            }.await()

            syncRtdbNode("users/$uid/spinState", mapOf("lastSpinAtMs" to System.currentTimeMillis(), "lastPrize" to wonSlice.title))
            Result.success(wonSlice)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun submitParchiRequest(
        uid: String,
        customerName: String,
        customerPhone: String,
        mode: String,
        notesText: String,
        voiceTranscript: String,
        photoDataUrl: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (auth.currentUser?.uid != uid) {
            return@withContext Result.failure(IllegalStateException("Please sign in with Google first."))
        }
        val reqId = "PARCHI-${System.currentTimeMillis().toString().takeLast(6)}"
        val payload = mapOf(
            "requestId" to reqId,
            "uid" to uid,
            "customerName" to customerName.take(80),
            "customerPhone" to customerPhone,
            "mode" to mode,
            "notesText" to notesText.take(600),
            "voiceTranscript" to voiceTranscript.take(600),
            "photoDataUrl" to photoDataUrl.take(400_000),
            "status" to "SUBMITTED",
            "matchedSummary" to "",
            "readyBillTotal" to 0,
            "createdAtMs" to System.currentTimeMillis()
        )
        return@withContext try {
            firestore.collection("users").document(uid)
                .collection("parchi_requests").document(reqId)
                .set(payload).await()
            syncRtdbNode("parchi_requests/$reqId", payload)
            syncRtdbNode("customer_parchi/$uid/$reqId", payload)
            Result.success(reqId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmParchiReadyBill(uid: String, requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            syncRtdbNode("parchi_requests/$requestId/status", "CONFIRMED")
            syncRtdbNode("customer_parchi/$uid/$requestId/status", "CONFIRMED")
            firestore.collection("users").document(uid)
                .collection("parchi_requests").document(requestId)
                .set(mapOf("status" to "CONFIRMED"), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveRashanSubscription(
        uid: String,
        subscription: RashanSubscription
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (auth.currentUser?.uid != uid) {
            return@withContext Result.failure(IllegalStateException("Please sign in with Google first."))
        }
        return@withContext try {
            val payload = hashMapOf<String, Any>(
                "uid" to uid,
                "customerName" to subscription.customerName.take(80),
                "customerPhone" to subscription.customerPhone,
                "monthlyDayOfMonth" to subscription.monthlyDayOfMonth.coerceIn(1, 28),
                "addressSnapshot" to subscription.addressSnapshot.take(300),
                "itemsSummary" to subscription.itemsSummary.take(600),
                "productIdsCsv" to subscription.productIdsCsv.take(600),
                "estimatedTotal" to subscription.estimatedTotal,
                "active" to subscription.active,
                "updatedAtMs" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid)
                .collection("rashan_box").document("monthly")
                .set(payload).await()
            syncRtdbNode("rashan_subscriptions/$uid", payload)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestWalletTopup(
        uid: String,
        customerName: String,
        amountRupees: Int,
        utrReference: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (auth.currentUser?.uid != uid) {
            return@withContext Result.failure(IllegalStateException("Please sign in with Google first."))
        }
        val clamped = amountRupees.coerceIn(100, 25000)
        val reqId = "WAL-${System.currentTimeMillis().toString().takeLast(6)}"
        val payload = mapOf(
            "requestId" to reqId,
            "uid" to uid,
            "customerName" to customerName.take(80),
            "amountRupees" to clamped,
            "bonusCoins" to (clamped * 0.05).toInt().coerceAtLeast(10),
            "utrReference" to utrReference.take(60),
            "status" to "PENDING_VERIFICATION",
            "createdAtMs" to System.currentTimeMillis()
        )
        return@withContext try {
            rtdb?.reference?.child("wallet_requests/$reqId")?.setValue(payload)?.await()
            Result.success(reqId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyReferralCode(uid: String, rawCode: String): Result<String> = withContext(Dispatchers.IO) {
        if (auth.currentUser?.uid != uid) return@withContext Result.failure(IllegalStateException("Sign in first."))
        val code = rawCode.trim().uppercase().take(24)
        if (!code.startsWith("PIND-") || code.length < 7) {
            return@withContext Result.failure(IllegalArgumentException("Enter valid Pind Ambassador code (e.g. PIND-XXXXXX)"))
        }
        return@withContext try {
            val userDoc = firestore.collection("users").document(uid)
            val snap = userDoc.get().await()
            if (!snap.getString("referredBy").isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("Referral code already linked to your account."))
            }
            if (snap.getString("referralCode").equals(code, ignoreCase = true)) {
                return@withContext Result.failure(IllegalArgumentException("You cannot apply your own referral code."))
            }
            userDoc.update(mapOf("referredBy" to code, "updatedAt" to FieldValue.serverTimestamp())).await()
            syncRtdbNode("referrals/${code}/referredUid", uid)
            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submits an authenticated customer order.
     * Step 1: Sends minimal identifiers (`productId`, `variantId`, `quantity`, `redeemCoinsRequested`, `idempotencyKey`)
     * to the backend `/api/orders/verify-and-create` endpoint so prices, discounts, and coin math are never trusted from the client.
     * Step 2: Persists the verified order record under `users/$uid/orders/$orderId` & RTDB `customer_orders/$uid/$orderId`.
     */
    suspend fun placeVerifiedCloudOrder(
        uid: String,
        orderId: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        deliveryOption: DeliveryOption,
        dispatchSlot: String,
        isNriOrder: Boolean,
        addressSnapshot: String,
        paymentMethod: PaymentMethod,
        cartItems: List<CartItem>,
        summary: VerifiedCheckoutSummary,
        idempotencyKey: String = orderId,
        requestedCoinsToRedeem: Int = summary.coinsRedeemed,
        applySpinCoupon: Boolean = false,
        applyWalletBalance: Boolean = false,
        offersConfig: StoreOffersConfig = StoreOffersConfig(),
        categoryDiscounts: Map<String, Double> = emptyMap(),
        pinCode: String = ""
    ): Result<PlacedCloudOrderResult> = withContext(Dispatchers.IO) {
        val currentFirebaseUser = auth.currentUser
        if (currentFirebaseUser?.uid != uid) {
            return@withContext Result.failure(IllegalStateException("Google Sign-In required before placing order."))
        }

        val cleanPin = pinCode.filter { it.isDigit() }.take(6).let { if (it.length == 6) it else "" }

        // Attempt authoritative server verification with strictly minimal item IDs & quantities (no client prices/secrets)
        val serverVerification = runCatching {
            val token = currentFirebaseUser.getIdToken(false).await()?.token.orEmpty()
            if (token.isNotBlank()) {
                val payload = SecureOrderSubmissionRequest(
                    idempotencyKey = idempotencyKey,
                    items = cartItems.take(50).map { ci ->
                        SecureCheckoutItemPayload(
                            productId = ci.product.id,
                            variantId = buildVariantKey(ci),
                            quantity = ci.quantity
                        )
                    },
                    redeemCoinsRequested = if (offersConfig.coinConfig.isCoinsEnabled) requestedCoinsToRedeem else 0,
                    applySpinCoupon = applySpinCoupon,
                    applyWalletBalance = applyWalletBalance,
                    deliveryTier = deliveryOption.name,
                    paymentMethod = paymentMethod.name,
                    addressSnapshot = addressSnapshot.take(300),
                    pinCode = cleanPin,
                    dispatchSlot = dispatchSlot.take(60),
                    isNriOrder = isNriOrder
                )
                val response = checkoutBackendService.verifyAndCreateOrder(
                    bearerIdToken = "Bearer $token",
                    idempotencyKey = idempotencyKey,
                    payload = payload
                )
                if (response.isSuccessful) response.body() else null
            } else {
                null
            }
        }.getOrNull()

        val coinsEnabled = offersConfig.coinConfig.isCoinsEnabled
        val verifiedOrderId = serverVerification?.orderId?.takeIf { it.isNotBlank() } ?: orderId
        val verifiedTotalMrp = serverVerification?.verifiedTotalMrp ?: summary.totalMrp
        val verifiedWholesaleDiscount = serverVerification?.verifiedWholesaleDiscount ?: summary.wholesaleDiscount
        val verifiedPetiDiscount = serverVerification?.verifiedPetiDiscount ?: summary.petiDiscountRupees
        val verifiedCoinsRedeemed = if (coinsEnabled) {
            serverVerification?.verifiedCoinsRedeemed ?: summary.coinsRedeemed
        } else 0
        val verifiedDeliveryFee = serverVerification?.verifiedDeliveryFee ?: summary.deliveryFee
        val verifiedFinalTotal = serverVerification?.verifiedFinalTotal ?: summary.finalPayableTotal
        val verifiedCoinsEarned = if (coinsEnabled) {
            serverVerification?.verifiedCoinsEarned ?: summary.coinsToEarn
        } else 0

        val userDoc = firestore.collection("users").document(uid)
        val orderDoc = userDoc.collection("orders").document(verifiedOrderId)

        val itemMaps = cartItems.take(50).map { ci ->
            val catPct = categoryDiscounts[ci.product.category]
            val unitLivePrice = ci.product.sellingPrice(catPct, offersConfig)
            val unitsPerPack = ci.effectiveUnitsPerItem
            val effectiveUnitPrice = if (ci.isPetiCarton && unitsPerPack > 1) {
                ci.product.petiTotalPriceFor(catPct, offersConfig) / unitsPerPack
            } else {
                unitLivePrice
            }
            hashMapOf<String, Any>(
                "productId" to ci.product.id,
                "name" to ci.product.name.take(120),
                "department" to ci.product.department.take(60),
                "category" to ci.product.category.take(60),
                "qty" to ci.quantity * unitsPerPack,
                "mrp" to ci.product.mrp,
                "wholesalePrice" to effectiveUnitPrice,
                "variant" to ci.variantSummary.take(60),
                "imageUrl" to ci.product.fullImageUrl.take(300)
            )
        }

        val orderPayload = hashMapOf<String, Any>(
            "orderId" to verifiedOrderId,
            "uid" to uid,
            "customerName" to customerName.take(80),
            "customerPhone" to customerPhone,
            "customerEmail" to customerEmail.take(120),
            "pinCode" to cleanPin,
            "anonymized" to false,
            "deliveryTier" to deliveryOption.name,
            "dispatchSlot" to dispatchSlot.take(60),
            "isNriOrder" to isNriOrder,
            "nriBuyerEmail" to if (isNriOrder) customerEmail.take(120) else "",
            "deliveryProofUrl" to "",
            "addressSnapshot" to addressSnapshot.take(300),
            "paymentMethod" to paymentMethod.name,
            "status" to "Order Placed",
            "totalMrp" to verifiedTotalMrp.toLong(),
            "wholesaleDiscount" to verifiedWholesaleDiscount.toLong(),
            "petiDiscountRupees" to verifiedPetiDiscount.toLong(),
            "spinCouponDiscountRupees" to summary.spinCouponDiscountRupees.toLong(),
            "spinCouponCode" to summary.spinCouponCode.take(40),
            "coinsRedeemed" to verifiedCoinsRedeemed.toLong(),
            "walletDeductedRupees" to summary.walletDeductedRupees.toLong(),
            "deliveryFee" to verifiedDeliveryFee.toLong(),
            "finalTotal" to verifiedFinalTotal.toLong(),
            "coinsEarned" to verifiedCoinsEarned.toLong(),
            "integrityDigest" to summary.integrityDigest.take(128),
            "items" to itemMaps,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val orderedGrocery = cartItems.any { !it.product.isSupermallItem }
        val orderedElectronics = cartItems.any { it.product.isSupermallItem }
        val staplesQty = cartItems.filter { !it.product.isSupermallItem }.sumOf { it.quantity * it.effectiveUnitsPerItem }

        try {
            orderDoc.set(orderPayload).await()

            var updatedGrocery = orderedGrocery
            var updatedElectronics = orderedElectronics
            var updatedStaples = staplesQty.toLong()
            var updatedWeekly = 1L
            var updatedTotalOrders = 1L
            var firstOrderReferralBonus = 0L

            firestore.runTransaction { tx ->
                val snap = tx.get(userDoc)
                val now = System.currentTimeMillis()
                val curCoins = (snap.get("navCoins") as? Number)?.toLong() ?: 0L
                val curTotal = (snap.get("totalOrdersCount") as? Number)?.toLong() ?: 0L
                val curStaples = (snap.get("coldChainStaplesCount") as? Number)?.toLong() ?: 0L
                val curWeekly = (snap.get("weeklyOrdersCount") as? Number)?.toLong() ?: 0L
                val windowStart = (snap.get("weeklyWindowStartMs") as? Number)?.toLong() ?: 0L
                val curWallet = (snap.get("walletBalanceRupees") as? Number)?.toLong() ?: 0L
                val referredBy = snap.getString("referredBy").orEmpty()
                val inSameWeek = windowStart > 0L && (now - windowStart) <= 7L * UserAccount.SPIN_COOLDOWN_MS

                if (coinsEnabled && curTotal == 0L && referredBy.isNotBlank()) {
                    firstOrderReferralBonus = 50L
                }

                updatedGrocery = (snap.getBoolean("hasOrderedGrocery") ?: false) || orderedGrocery
                updatedElectronics = (snap.getBoolean("hasOrderedElectronics") ?: false) || orderedElectronics
                updatedStaples = (curStaples + staplesQty).coerceAtMost(1000L)
                updatedWeekly = (if (inSameWeek) curWeekly + 1L else 1L).coerceAtMost(1000L)
                updatedTotalOrders = (curTotal + 1L).coerceAtMost(10_000L)

                val netCoinDelta = if (coinsEnabled) {
                    (verifiedCoinsEarned.toLong() + firstOrderReferralBonus - verifiedCoinsRedeemed.toLong())
                } else 0L
                val nextCoins = (curCoins + netCoinDelta).coerceIn(0L, 50_000L)
                val curWalletSpent = (snap.get("walletSpentRupees") as? Number)?.toLong() ?: 0L
                val nextWallet = (curWallet - summary.walletDeductedRupees).coerceAtLeast(0L)
                val nextWalletSpent = (curWalletSpent + summary.walletDeductedRupees).coerceAtMost(100_000L)

                val userUpdates = hashMapOf<String, Any>(
                    "navCoins" to nextCoins,
                    "walletBalanceRupees" to nextWallet,
                    "walletSpentRupees" to nextWalletSpent,
                    "hasOrderedGrocery" to updatedGrocery,
                    "hasOrderedElectronics" to updatedElectronics,
                    "coldChainStaplesCount" to updatedStaples,
                    "weeklyOrdersCount" to updatedWeekly,
                    "weeklyWindowStartMs" to if (inSameWeek) windowStart else now,
                    "totalOrdersCount" to updatedTotalOrders,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (summary.spinCouponCode.isNotBlank() || summary.freeExpressApplied) {
                    userUpdates["activeSpinUsed"] = true
                }
                if (customerPhone.isNotBlank() && (snap.getString("phone").isNullOrBlank())) {
                    userUpdates["phone"] = customerPhone
                }
                tx.update(userDoc, userUpdates)
            }.await()

            upsertQuestProgress(uid, QUEST_FIRST_ORDER, progress = 1, target = 1, rewardCoins = 50)
            val crossProg = (if (updatedGrocery) 1 else 0) + (if (updatedElectronics) 1 else 0)
            upsertQuestProgress(uid, QUEST_CROSS_CATEGORY, progress = crossProg.coerceAtMost(2), target = 2, rewardCoins = 75)
            upsertQuestProgress(uid, QUEST_STAPLES_FROZEN, progress = updatedStaples.toInt().coerceAtMost(3), target = 3, rewardCoins = 60)
            upsertQuestProgress(uid, QUEST_WEEKLY_STREAK, progress = updatedWeekly.toInt().coerceAtMost(3), target = 3, rewardCoins = 100)

            val rtdbOrderSummary = mapOf(
                "orderId" to verifiedOrderId,
                "uid" to uid,
                "customerName" to customerName,
                "customerPhone" to customerPhone,
                "customerEmail" to customerEmail,
                "deliveryTier" to deliveryOption.name,
                "dispatchSlot" to dispatchSlot,
                "isNriOrder" to isNriOrder,
                "addressSnapshot" to addressSnapshot,
                "paymentMethod" to paymentMethod.name,
                "status" to "Order Placed",
                "deliveryProofUrl" to "",
                "pinCode" to cleanPin,
                "anonymized" to false,
                "finalTotal" to verifiedFinalTotal,
                "totalMrp" to verifiedTotalMrp,
                "petiDiscountRupees" to verifiedPetiDiscount,
                "walletDeductedRupees" to summary.walletDeductedRupees,
                "idempotencyKey" to idempotencyKey,
                "timestamp" to System.currentTimeMillis(),
                "itemCount" to cartItems.sumOf { it.quantity * it.effectiveUnitsPerItem }
            )
            syncRtdbNode("orders/$verifiedOrderId", rtdbOrderSummary)
            syncRtdbNode("customer_orders/$uid/$verifiedOrderId", rtdbOrderSummary)

            Result.success(
                PlacedCloudOrderResult(
                    orderId = verifiedOrderId,
                    paymentSessionUrl = serverVerification?.paymentSessionUrl
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/orders/$verifiedOrderId")
            Result.failure(e)
        }
    }

    suspend fun cancelCustomerOrder(uid: String, orderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (auth.currentUser?.uid != uid) {
            return@withContext Result.failure(IllegalStateException("Google Sign-In required to cancel order."))
        }
        val docRef = firestore.collection("users").document(uid).collection("orders").document(orderId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return@withContext Result.failure(IllegalStateException("Order not found in cloud history."))
            }
            val currentStatus = snap.getString("status").orEmpty().trim()
            val cancellable = currentStatus.equals("Order Placed", ignoreCase = true) ||
                currentStatus.equals("Placed", ignoreCase = true) ||
                currentStatus.equals("Pending", ignoreCase = true) ||
                currentStatus.equals("Confirmed", ignoreCase = true)
            if (!cancellable) {
                return@withContext Result.failure(
                    IllegalStateException("Order cannot be cancelled once it is $currentStatus.")
                )
            }
            docRef.update(
                mapOf(
                    "status" to "Cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            runCatching {
                rtdb?.reference?.child("orders/$orderId")
                    ?.updateChildren(mapOf("status" to "Cancelled"))
                rtdb?.reference?.child("customer_orders/$uid/$orderId")
                    ?.updateChildren(mapOf("status" to "Cancelled"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/orders/$orderId")
            Result.failure(e)
        }
    }

    /**
     * Play Store & DPDP 2023 Account Deletion:
     * Permanently deletes saved addresses, cart, quests, monthly rashan box, parchi drafts, and customer profile;
     * anonymizes retained order/tax records (preserves GSTIN ledger amounts while redacting PII).
     */
    suspend fun deleteCustomerAccountAndData(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user?.uid != uid) {
            return@withContext Result.failure(IllegalStateException("Please sign in to delete your account."))
        }
        val userDoc = firestore.collection("users").document(uid)
        try {
            // 1. Anonymize completed/tax order records in Firestore & RTDB
            val ordersSnap = userDoc.collection("orders").get().await()
            for (ordDoc in ordersSnap.documents) {
                val anonUpdate = mapOf(
                    "customerName" to "Deleted Customer",
                    "customerPhone" to "",
                    "customerEmail" to "",
                    "nriBuyerEmail" to "",
                    "addressSnapshot" to "Redacted (DPDP 2023)",
                    "anonymized" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ordDoc.reference.update(anonUpdate).await()
                val oid = ordDoc.getString("orderId") ?: ordDoc.id
                val rtdbAnon = mapOf(
                    "customerName" to "Deleted Customer",
                    "customerPhone" to "",
                    "customerEmail" to "",
                    "addressSnapshot" to "Redacted (DPDP 2023)",
                    "anonymized" to true
                )
                runCatching {
                    rtdb?.reference?.child("orders/$oid")?.updateChildren(rtdbAnon)
                    rtdb?.reference?.child("customer_orders/$uid/$oid")?.updateChildren(rtdbAnon)
                }
            }

            // 2. Permanently delete personal subcollections (addresses, quests, rashan_box, parchi_requests)
            val subcollections = listOf("addresses", "quests", "rashan_box", "parchi_requests")
            for (sub in subcollections) {
                val docs = runCatching { userDoc.collection(sub).get().await().documents }.getOrDefault(emptyList())
                docs.forEach { d -> runCatching { d.reference.delete().await() } }
            }

            // 3. Delete Firestore user profile document
            userDoc.delete().await()

            // 4. Delete personal RTDB nodes (profile, carts, subscriptions, milestones)
            runCatching { rtdb?.reference?.child("users/$uid")?.removeValue()?.await() }
            runCatching { rtdb?.reference?.child("carts/$uid")?.removeValue()?.await() }
            runCatching { rtdb?.reference?.child("rashan_subscriptions/$uid")?.removeValue()?.await() }
            runCatching { rtdb?.reference?.child("quest_milestones/$uid")?.removeValue()?.await() }

            // 5. Delete Firebase Auth account if recently authenticated (never blocks local sign-out)
            runCatching { user.delete().await() }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }

    private fun buildVariantKey(ci: CartItem): String {
        val parts = buildList {
            if (ci.selectedSize.isNotBlank()) add(ci.selectedSize.trim())
            if (ci.selectedColor.isNotBlank()) add(ci.selectedColor.trim())
            if (ci.isPetiCarton) add("PETI_${ci.petiUnits}")
        }
        return if (parts.isEmpty()) "STANDARD" else parts.joinToString("|")
    }

    suspend fun claimQuestReward(
        uid: String,
        quest: ShoppingQuest,
        offersConfig: StoreOffersConfig = StoreOffersConfig()
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (auth.currentUser?.uid != uid) {
            return@withContext Result.failure(IllegalStateException("Sign in to claim quest rewards."))
        }
        if (!offersConfig.coinConfig.isCoinsEnabled) {
            return@withContext Result.failure(IllegalStateException("Coin rewards are currently paused by store offers."))
        }
        if (!quest.completed || quest.claimed) {
            return@withContext Result.failure(IllegalStateException("Quest is not ready to claim."))
        }
        val userDoc = firestore.collection("users").document(uid)
        val questDoc = userDoc.collection("quests").document(quest.id)
        try {
            firestore.runTransaction { tx ->
                val userSnap = tx.get(userDoc)
                val qSnap = tx.get(questDoc)
                if (qSnap.exists() && qSnap.getBoolean("claimed") == true) {
                    throw IllegalStateException("Quest reward already claimed.")
                }
                val curCoins = (userSnap.get("navCoins") as? Number)?.toLong() ?: 0L
                val nextCoins = (curCoins + quest.rewardCoins.toLong()).coerceAtMost(50_000L)
                tx.update(userDoc, mapOf("navCoins" to nextCoins, "updatedAt" to FieldValue.serverTimestamp()))
                if (qSnap.exists()) {
                    tx.update(questDoc, mapOf("claimed" to true, "progress" to quest.target, "updatedAt" to FieldValue.serverTimestamp()))
                } else {
                    tx.set(questDoc, mapOf(
                        "uid" to uid,
                        "questId" to quest.id,
                        "progress" to quest.target,
                        "target" to quest.target,
                        "rewardCoins" to quest.rewardCoins,
                        "claimed" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                }
            }.await()
            syncRtdbNode("quest_milestones/$uid/${quest.id}", mapOf("claimed" to true, "rewardCoins" to quest.rewardCoins, "timestamp" to System.currentTimeMillis()))
            Result.success(quest.rewardCoins)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/quests/${quest.id}")
            Result.failure(e)
        }
    }

    private suspend fun upsertQuestProgress(
        uid: String,
        questId: String,
        progress: Int,
        target: Int,
        rewardCoins: Int
    ) {
        val docRef = firestore.collection("users").document(uid).collection("quests").document(questId)
        try {
            val snap = docRef.get().await()
            val clampedProg = progress.coerceIn(0, target)
            if (snap.exists()) {
                val curProg = (snap.get("progress") as? Number)?.toInt() ?: 0
                if (clampedProg > curProg) {
                    docRef.update(mapOf("progress" to clampedProg, "updatedAt" to FieldValue.serverTimestamp())).await()
                }
            } else {
                docRef.set(mapOf(
                    "uid" to uid,
                    "questId" to questId,
                    "progress" to clampedProg,
                    "target" to target,
                    "rewardCoins" to rewardCoins,
                    "claimed" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )).await()
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Quest progress sync skipped for $questId")
            }
        }
    }

    private fun syncRtdbProfile(uid: String, profile: Map<String, Any>) {
        runCatching { rtdb?.reference?.child("users/$uid/profile")?.updateChildren(profile) }
    }

    private fun syncRtdbNode(path: String, value: Any) {
        runCatching { rtdb?.reference?.child(path)?.setValue(value) }
    }

    companion object {
        private const val TAG = "CustomerAccountRepo"
        private const val RTDB_URL = "https://nav-grocery-store-default-rtdb.firebaseio.com/"

        const val QUEST_PROFILE_ADDRESS = "QUEST_PROFILE_ADDRESS"
        const val QUEST_FIRST_ORDER = "QUEST_FIRST_ORDER"
        const val QUEST_CROSS_CATEGORY = "QUEST_CROSS_CATEGORY"
        const val QUEST_STAPLES_FROZEN = "QUEST_STAPLES_FROZEN"
        const val QUEST_WEEKLY_STREAK = "QUEST_WEEKLY_STREAK"

        fun buildDefaultQuests(
            account: UserAccount?,
            addresses: List<SavedAddress>
        ): List<ShoppingQuest> {
            val hasAddr = addresses.isNotEmpty()
            val totalOrders = (account?.totalOrdersCount ?: 0L).toInt()
            val crossCount = (if (account?.hasOrderedGrocery == true) 1 else 0) +
                (if (account?.hasOrderedElectronics == true) 1 else 0)
            val staplesCount = (account?.coldChainStaplesCount ?: 0L).toInt()
            val weeklyCount = (account?.weeklyOrdersCount ?: 0L).toInt()

            return listOf(
                ShoppingQuest(
                    id = QUEST_PROFILE_ADDRESS,
                    title = "Complete Profile & Save Sandhaur 148020 Address",
                    punjabiTitle = "ਪ੍ਰੋਫਾਈਲ ਪੂਰਾ ਕਰੋ ਅਤੇ ਸੰਧੌੜ 148020 ਪਤਾ ਸੇਵ ਕਰੋ",
                    description = "Sign in with Google and save at least 1 delivery address.",
                    progress = if (hasAddr) 100 else if (account != null) 50 else 0,
                    target = 100,
                    rewardCoins = 40,
                    completed = hasAddr,
                    claimed = false
                ),
                ShoppingQuest(
                    id = QUEST_FIRST_ORDER,
                    title = "Place Your First Nav Grocery / NavKart Order",
                    punjabiTitle = "ਆਪਣਾ ਪਹਿਲਾ ਨਵ ਗ੍ਰੋਸਰੀ / ਨਵਕਾਰਟ ਆਰਡਰ ਦਿਓ",
                    description = "Order any verified sealed grocery or partner marketplace product.",
                    progress = totalOrders.coerceAtMost(1),
                    target = 1,
                    rewardCoins = 50,
                    completed = totalOrders >= 1,
                    claimed = false
                ),
                ShoppingQuest(
                    id = QUEST_CROSS_CATEGORY,
                    title = "Shop Across Both Grocery & Electronics",
                    punjabiTitle = "ਗ੍ਰੋਸਰੀ ਅਤੇ ਇਲੈਕਟ੍ਰਾਨਿਕਸ ਦੋਵਾਂ ਤੋਂ ਖਰੀਦਦਾਰੀ ਕਰੋ",
                    description = "Purchase items from both Nav Grocery Store & NavKart partner categories.",
                    progress = crossCount.coerceAtMost(2),
                    target = 2,
                    rewardCoins = 75,
                    completed = crossCount >= 2,
                    claimed = false
                ),
                ShoppingQuest(
                    id = QUEST_STAPLES_FROZEN,
                    title = "Order 3 Cold-Chain Frozen or Pantry Staples",
                    punjabiTitle = "3 ਕੋਲਡ-ਚੇਨ ਫ੍ਰੋਜ਼ਨ ਜਾਂ ਪੈਂਟਰੀ ਸਟੈਪਲ ਆਰਡਰ ਕਰੋ",
                    description = "Stock up sealed atta, ghee, spices or Verka/McCain frozen packs.",
                    progress = staplesCount.coerceAtMost(3),
                    target = 3,
                    rewardCoins = 60,
                    completed = staplesCount >= 3,
                    claimed = false
                ),
                ShoppingQuest(
                    id = QUEST_WEEKLY_STREAK,
                    title = "Complete 3 Orders in a Week",
                    punjabiTitle = "ਇੱਕ ਹਫ਼ਤੇ ਵਿੱਚ 3 ਆਰਡਰ ਪੂਰੇ ਕਰੋ",
                    description = "Weekly Sandhaur Hub loyalty streak.",
                    progress = weeklyCount.coerceAtMost(3),
                    target = 3,
                    rewardCoins = 100,
                    completed = weeklyCount >= 3,
                    claimed = false
                )
            )
        }
    }
}
