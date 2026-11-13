package `in`.campusone.app.data.remote

interface SupabaseClient {

    fun from(table: String): SupabaseQueryBuilder

    val auth: SupabaseAuth
}

interface SupabaseQueryBuilder {
    fun select(columns: String): SupabaseQueryBuilder
    fun eq(column: String, value: Any): SupabaseQueryBuilder
    fun order(column: String, ascending: Boolean): SupabaseQueryBuilder
    fun single(): SupabaseQueryBuilder
    fun upsert(data: Map<String, Any>): SupabaseResponse
    fun upsert(data: List<Map<String, Any>>): SupabaseResponse
    fun execute(): SupabaseResponse
}

data class SupabaseResponse(val data: Any?)

interface SupabaseAuth {
    fun signInWithIdToken(provider: String, token: String): SupabaseSession?
    fun signOut(): Unit
    val currentSession: SupabaseSession?
    val currentUser: SupabaseUser?
    fun authStateChanges(callback: (SupabaseSession?) -> Unit): Unit
}

data class SupabaseSession(val accessToken: String?, val user: SupabaseUser?)
data class SupabaseUser(val id: String?)
