package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Credential Manager + Firebase Auth handler supporting Google Gmail OAuth.
 * Strictly prohibits anonymous or password sign-in.
 */
class GoogleAuthManager(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    /**
     * Explicit interactive 1-click Google Sign-In bottom sheet using GetSignInWithGoogleOption.
     */
    suspend fun signInInteractive(context: Context): Result<FirebaseUser> {
        val webClientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrNull().orEmpty()
        if (webClientId.isBlank()) {
            return Result.failure(IllegalStateException("Google Sign-In Web Client ID is not configured."))
        }

        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            handleCredentialResponse(response.credential)
        } catch (cancelled: GetCredentialCancellationException) {
            if (com.example.BuildConfig.DEBUG) {
                Log.i(TAG, "User cancelled Google Sign-In dialog")
            }
            Result.failure(cancelled)
        } catch (noCred: NoCredentialException) {
            if (com.example.BuildConfig.DEBUG) {
                Log.w(TAG, "No Google account available on device for Credential Manager")
            }
            Result.failure(IllegalStateException("Please add or sign into a Google account on your device.", noCred))
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                Log.e(TAG, "Interactive Google Sign-In failed", e)
            }
            Result.failure(e)
        }
    }

    /**
     * Separate passive/silent check for previously authorized Google account.
     */
    suspend fun attemptSilentSignIn(context: Context): Result<FirebaseUser?> {
        if (firebaseAuth.currentUser != null) {
            return Result.success(firebaseAuth.currentUser)
        }
        val webClientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrNull().orEmpty()
        if (webClientId.isBlank()) {
            return Result.success(null)
        }
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            handleCredentialResponse(response.credential).map { it }
        } catch (_: NoCredentialException) {
            Result.success(null)
        } catch (_: GetCredentialCancellationException) {
            Result.success(null)
        } catch (_: Exception) {
            Result.success(null)
        }
    }

    private suspend fun handleCredentialResponse(credential: androidx.credentials.Credential): Result<FirebaseUser> {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCred).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Firebase authentication returned null user."))
            return Result.success(user)
        }
        return Result.failure(IllegalStateException("Unsupported credential type returned."))
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            runCatching { firebaseAuth.signOut() }
            runCatching {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            }
        }
    }

    companion object {
        private const val TAG = "GoogleAuthManager"
    }
}
