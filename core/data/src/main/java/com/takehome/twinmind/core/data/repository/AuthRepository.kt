package com.takehome.twinmind.core.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
) {
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    val currentUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    val isSignedIn: Boolean get() = firebaseAuth.currentUser != null

    val uid: String? get() = firebaseAuth.currentUser?.uid

    val user: FirebaseUser? get() = firebaseAuth.currentUser

    val displayName: String? get() = firebaseAuth.currentUser?.displayName

    val email: String? get() = firebaseAuth.currentUser?.email

    val photoUrl: String? get() = firebaseAuth.currentUser?.photoUrl?.toString()

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> =
        runCatching {
            val credentialManager = CredentialManager.create(activityContext)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val googleIdTokenCredential =
                GoogleIdTokenCredential.createFrom(result.credential.data)

            val firebaseCredential =
                GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            authResult.user ?: error("Firebase auth returned null user")
        }.onFailure { Timber.e(it, "Google sign-in failed") }

    fun signOut() {
        firebaseAuth.signOut()
    }

    companion object {
        private const val WEB_CLIENT_ID =
            "945429471595-7nkjnbv8sj5a8f5f4e2t7ltesiovdjop.apps.googleusercontent.com"
    }
}
