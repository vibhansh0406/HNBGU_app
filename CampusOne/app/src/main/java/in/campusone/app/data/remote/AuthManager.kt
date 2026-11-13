package `in`.campusone.app.data.remote

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.kelvinino18.supabase.SupabaseClient
import io.github.kelvinino18.supabase.auth.AuthChangeEvent
import io.github.kelvinino18.supabase.auth.AuthFlowError
import io.github.kelvinino18.supabase.auth.Session
import io.github.kelvinino18.supabase.auth.SupabaseAuth
import io.github.kelvinino18.supabase.auth.UserAttributes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
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

    val supabaseAuth: SupabaseAuth = supabaseClient.auth

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
    suspend fun signInWithGoogle(idToken: String): Result<Session> {
        return try {
            // Supabase GoTrue supports Google OAuth via provider token
            val session = supabaseClient.auth.signInWithIdToken(
                provider = "google",
                token = idToken
            )
            Result.success(session)
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
    fun currentSession(): Session? = supabaseClient.auth.currentSession

    /** Get current Supabase user. */
    fun currentUserId(): String? = supabaseClient.auth.currentUser?.id

    /** Listen to auth state changes. */
    fun onAuthStateChanged(callback: (Session?) -> Unit) {
        supabaseClient.auth.authStateChanges { event, session ->
            callback(session)
        }
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
