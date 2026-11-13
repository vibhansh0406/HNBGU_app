package `in`.campusone.app.data.remote

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

@Singleton
class AuthManager @Inject constructor(
    private val context: Context,
    private val supabaseClient: SupabaseClient,
    private val config: SupabaseConfig
) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(config.googleWebClientId)
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

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
            supabaseClient.from("profiles").upsert(mapOf(
                "id" to uid,
                "email" to email,
                "full_name" to name,
                "role" to role,
                "updated_at" to java.time.Instant.now().toString()
            )).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
