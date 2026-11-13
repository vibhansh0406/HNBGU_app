package `in`.campusone.app.data.remote

import kotlinx.coroutines.flow.StateFlow

interface SupabaseClient {

    fun from(table: String): SupabaseQueryBuilder

    val auth: SupabaseAuth

    /** Observable session so the UI reacts to sign-in/sign-out. */
    val sessionFlow: StateFlow<SupabaseSession?>
}

interface SupabaseQueryBuilder {
    fun select(columns: String): SupabaseQueryBuilder
    fun eq(column: String, value: Any): SupabaseQueryBuilder
    fun order(column: String, ascending: Boolean): SupabaseQueryBuilder
    fun single(): SupabaseQueryBuilder
    suspend fun upsert(data: Map<String, Any>): SupabaseResponse
    suspend fun upsert(data: List<Map<String, Any>>): SupabaseResponse
    suspend fun execute(): SupabaseResponse
}

data class SupabaseResponse(
    val data: Any?,
    val error: String? = null,
    val statusCode: Int = 0
) {
    val isSuccess: Boolean
        get() = error == null && statusCode in 200..299
}

interface SupabaseAuth {
    suspend fun signInWithIdToken(provider: String, token: String): SupabaseSession?
    suspend fun signOut()
    val currentSession: SupabaseSession?
    val currentUser: SupabaseUser?
    fun authStateChanges(callback: (SupabaseSession?) -> Unit): Unit
}

data class SupabaseSession(
    val accessToken: String?,
    val refreshToken: String? = null,
    val expiresAt: Long = 0L,
    val user: SupabaseUser? = null
)

data class SupabaseUser(
    val id: String?,
    val email: String? = null
)