package `in`.campusone.app.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow
import kotlin.Result

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient,
    private val config: SupabaseConfig
) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        if (config.googleWebClientId.isNotBlank()) {
            builder.requestIdToken(config.googleWebClientId)
        }
        GoogleSignIn.getClient(context, builder.build())
    }

    /** True when a Google web client ID is available for ID-token sign-in. */
    val isGoogleSignInConfigured: Boolean
        get() = config.googleWebClientId.isNotBlank()

    /** Observable Supabase session (survives process restarts). */
    val session: StateFlow<SupabaseSession?> = supabaseClient.sessionFlow

    /** Initialize Google Sign-In client (call once). */
    fun initGoogleSignIn() {
        googleSignInClient // force initialization
    }

    /** Get the sign-in intent for launching from Activity. */
    fun getSignInIntent(): android.content.Intent = googleSignInClient.signInIntent

    /** Handle the result from onActivityResult. Returns the Google ID token. */
    fun handleSignInResult(data: android.content.Intent?): Task<GoogleSignInAccount> {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return task
    }

    /** Exchange Google ID token with Supabase. Returns the Supabase session. */
    suspend fun signInWithGoogle(idToken: String): Result<SupabaseSession> {
        return try {
            val session = supabaseClient.auth.signInWithIdToken("google", idToken)
            if (session != null) {
                Result.success(session)
            } else {
                Result.failure(Exception("Supabase sign-in returned null session"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Sign out from Supabase. */
    suspend fun signOut(): Result<Unit> {
        return try {
            supabaseClient.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Get current Supabase session. */
    fun currentSession(): SupabaseSession? = supabaseClient.auth.currentSession

    /** Get current Supabase user ID. */
    fun currentUserId(): String? = supabaseClient.auth.currentUser?.id

    /** Get the signed-in account email (Supabase session, then Google). */
    fun currentUserEmail(): String? =
        supabaseClient.auth.currentUser?.email
            ?: GoogleSignIn.getLastSignedInAccount(context)?.email

    /** Listen to auth state changes. */
    fun onAuthStateChanged(callback: (SupabaseSession?) -> Unit) {
        supabaseClient.auth.authStateChanges(callback)
    }

    /** Create a Supabase user profile record after sign-in. */
    suspend fun ensureUserProfile(
        uid: String,
        email: String?,
        name: String?,
        role: String = "STUDENT"
    ): Result<Unit> {
        return try {
            // The row is created through a SECURITY DEFINER function so the
            // server decides the role; the client value is only a hint.
            val response = supabaseClient.rpc(
                "campusone_bootstrap_profile",
                mapOf("p_name" to (name ?: ""), "p_email" to (email ?: ""))
            )
            val ok = (response.data as? Map<String, Any>)?.get("ok") == true
            if (ok) Result.success(Unit)
            else Result.failure(Exception(response.error ?: "Failed to save profile"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refreshes the access token when it is close to expiry. A revoked or
     * expired session cannot be refreshed, so the caller learns about it and can
     * drop privileged access immediately.
     */
    suspend fun ensureFreshToken(): Boolean {
        val session = supabaseClient.auth.currentSession ?: return false
        val expiresAt = session.expiresAt
        val refreshToken = session.refreshToken
        if (refreshToken.isNullOrBlank()) return false
        if (expiresAt > 0L && System.currentTimeMillis() < expiresAt - 60_000L) return true
        return supabaseClient.auth.refreshSession(refreshToken)
    }
}
