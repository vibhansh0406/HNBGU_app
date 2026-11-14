package `in`.campusone.app.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted Supabase session (access/refresh token + user id).
 * Stored in a private SharedPreferences file so the session survives process death.
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    private val _session = MutableStateFlow(read())
    val session: StateFlow<SupabaseSession?> = _session.asStateFlow()

    fun save(session: SupabaseSession?) {
        val editor = prefs.edit()
        if (session == null) {
            editor.remove("access_token")
            editor.remove("refresh_token")
            editor.remove("expires_at")
            editor.remove("user_id")
            editor.remove("user_email")
        } else {
            editor.putString("access_token", session.accessToken)
            editor.putString("refresh_token", session.refreshToken)
            editor.putLong("expires_at", session.expiresAt)
            editor.putString("user_id", session.user?.id)
            editor.putString("user_email", session.user?.email)
        }
        editor.apply()
        _session.value = session
    }

    fun clear() = save(null)

    private fun read(): SupabaseSession? {
        val token = prefs.getString("access_token", null) ?: return null
        return SupabaseSession(
            accessToken = token,
            refreshToken = prefs.getString("refresh_token", null),
            expiresAt = prefs.getLong("expires_at", 0L),
            user = SupabaseUser(
                id = prefs.getString("user_id", null),
                email = prefs.getString("user_email", null)
            )
        )
    }
}

@Singleton
class HttpSupabaseClient @Inject constructor(
    private val config: SupabaseConfig,
    private val sessionStore: SessionStore
) : SupabaseClient {

    private val baseUrl = config.url.trimEnd('/') + "/rest/v1"
    private val authBaseUrl = config.url.trimEnd('/') + "/auth/v1"

    override val auth: SupabaseAuth = HttpSupabaseAuth(config, authBaseUrl, sessionStore)
    override val sessionFlow: StateFlow<SupabaseSession?> = sessionStore.session

    override fun from(table: String): SupabaseQueryBuilder =
        HttpSupabaseQueryBuilder(baseUrl, table, config, sessionStore)

    internal fun authHeaders(): Map<String, String> {
        val token = sessionStore.session.value?.accessToken?.takeIf { it.isNotBlank() }
            ?: config.anonKey
        return mapOf(
            "apikey" to config.anonKey,
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        )
    }
}

private class HttpSupabaseQueryBuilder(
    private val baseUrl: String,
    private val table: String,
    private val config: SupabaseConfig,
    private val sessionStore: SessionStore
) : SupabaseQueryBuilder {

    private var selectColumns: String = "*"
    private val filters: MutableList<String> = mutableListOf()
    private val orders: MutableList<String> = mutableListOf()
    private var isSingle: Boolean = false

    override fun select(columns: String): SupabaseQueryBuilder {
        selectColumns = columns
        return this
    }

    override fun eq(column: String, value: Any): SupabaseQueryBuilder {
        filters.add("${encode(column)}=eq.${encode(value.toString())}")
        return this
    }

    override fun order(column: String, ascending: Boolean): SupabaseQueryBuilder {
        orders.add("${column}.${if (ascending) "asc" else "desc"}")
        return this
    }

    override fun single(): SupabaseQueryBuilder {
        isSingle = true
        return this
    }

    override suspend fun upsert(data: Map<String, Any>): SupabaseResponse = doUpsert(listOf(data))

    override suspend fun upsert(data: List<Map<String, Any>>): SupabaseResponse = doUpsert(data)

    override suspend fun execute(): SupabaseResponse = doGet()

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun buildQuery(): String {
        val params = mutableListOf("select=${encode(selectColumns)}")
        params.addAll(filters)
        params.addAll(orders)
        if (isSingle) params.add("limit=1")
        return params.joinToString("&")
    }

    private suspend fun doGet(): SupabaseResponse = withContext(Dispatchers.IO) {
        val fullUrl = "$baseUrl/$table?${buildQuery()}"
        val conn = URL(fullUrl).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", config.anonKey)
            conn.setRequestProperty(
                "Authorization",
                "Bearer " + (sessionStore.session.value?.accessToken?.takeIf { it.isNotBlank() } ?: config.anonKey)
            )
            if (isSingle) conn.setRequestProperty("Accept", "application/vnd.pgrst.object+json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            readResponse(conn)
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun doUpsert(data: List<Map<String, Any>>): SupabaseResponse =
        withContext(Dispatchers.IO) {
            if (data.isEmpty()) return@withContext SupabaseResponse(null)
            val fullUrl = "$baseUrl/$table?${buildQuery()}&on_conflict=id"
            val conn = URL(fullUrl).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", config.anonKey)
                conn.setRequestProperty(
                    "Authorization",
                    "Bearer " + (sessionStore.session.value?.accessToken?.takeIf { it.isNotBlank() } ?: config.anonKey)
                )
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "return=representation,resolution=merge-duplicates")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val body = JSONArray(data).toString()
                conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                readResponse(conn)
            } finally {
                conn.disconnect()
            }
        }

    private fun readResponse(conn: HttpURLConnection): SupabaseResponse {
        val code = try {
            conn.responseCode
        } catch (e: Exception) {
            return SupabaseResponse(null, e.message ?: e.javaClass.simpleName, -1)
        }
        val body = try {
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            stream?.use { it.readBytes() } ?: ByteArray(0)
        } catch (e: Exception) {
            return SupabaseResponse(null, e.message ?: e.javaClass.simpleName, code)
        }
        val json = String(body, StandardCharsets.UTF_8)
        return if (code in 200..299) {
            SupabaseResponse(parseJson(json), null, code)
        } else {
            val message = runCatching {
                JSONObject(json).optString("message").takeIf { it.isNotBlank() }
                    ?: JSONObject(json).optString("error").takeIf { it.isNotBlank() }
            }.getOrNull()
            SupabaseResponse(null, message ?: "HTTP $code ${json.take(300)}", code)
        }
    }

    private fun parseJson(json: String): Any? {
        if (json.isBlank()) return null
        return try {
            JSONArray(json).toListAny()
        } catch (_: Exception) {
            try {
                JSONObject(json).toMapAny()
            } catch (_: Exception) {
                json
            }
        }
    }

    private fun JSONObject.toMapAny(): Map<String, Any> {
        val map = mutableMapOf<String, Any?>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = convertJsonValue(opt(key))
        }
        @Suppress("UNCHECKED_CAST")
        return map as Map<String, Any>
    }

    private fun JSONArray.toListAny(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        for (i in 0 until length()) {
            val value = opt(i)
            if (value is JSONObject) list.add(value.toMapAny())
        }
        return list
    }

    private fun convertJsonValue(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> value.toMapAny()
        is JSONArray -> value.toListAny()
        else -> value
    }
}

private class HttpSupabaseAuth(
    private val config: SupabaseConfig,
    private val authBaseUrl: String,
    private val sessionStore: SessionStore
) : SupabaseAuth {

    override suspend fun signInWithIdToken(provider: String, token: String): SupabaseSession? =
        withContext(Dispatchers.IO) {
            val url = "$authBaseUrl/token?grant_type=id_token"
            val safeToken = token.replace("\\", "\\\\").replace("\"", "\\\"")
            val body = """{"provider":"$provider","id_token":"$safeToken"}"""
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", config.anonKey)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                readAuthSession(conn).also { sessionStore.save(it) }
            } finally {
                conn.disconnect()
            }
        }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            val token = sessionStore.session.value?.accessToken?.takeIf { it.isNotBlank() }
            if (token != null) {
                val conn = URL("$authBaseUrl/logout").openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("apikey", config.anonKey)
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.inputStream.use { it.readBytes() }
                } catch (_: Exception) {
                    // best-effort server logout; local session is cleared regardless
                } finally {
                    conn.disconnect()
                }
            }
        }
        sessionStore.clear()
    }

    override val currentSession: SupabaseSession?
        get() = sessionStore.session.value

    override val currentUser: SupabaseUser?
        get() = currentSession?.user

    override fun authStateChanges(callback: (SupabaseSession?) -> Unit) {
        callback(currentSession)
    }

    private fun readAuthSession(conn: HttpURLConnection): SupabaseSession {
        val code = try {
            conn.responseCode
        } catch (e: Exception) {
            throw IllegalStateException("Auth network error: ${e.message}")
        }
        val body = try {
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            stream?.use { it.readBytes() } ?: ByteArray(0)
        } catch (_: Exception) {
            ByteArray(0)
        }
        if (code !in 200..299) {
            val raw = String(body, StandardCharsets.UTF_8)
            val detail = try {
                val obj = JSONObject(raw)
                listOf("error_description", "msg", "message", "error")
                    .firstNotNullOfOrNull { key -> obj.optString(key).takeIf { it.isNotBlank() } }
            } catch (_: Exception) {
                null
            }
            throw IllegalStateException("Auth failed ($code): ${detail ?: raw.take(120)}")
        }
        return try {
            val obj = JSONObject(String(body, StandardCharsets.UTF_8))
            val accessToken = obj.optString("access_token").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Auth response missing access_token")
            val refreshToken = obj.optString("refresh_token").takeIf { it.isNotBlank() }
            val expiresIn = obj.optLong("expires_in", 3600L)
            val userObj = obj.optJSONObject("user")
            SupabaseSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAt = System.currentTimeMillis() + expiresIn * 1000,
                user = SupabaseUser(
                    id = userObj?.optString("id")?.takeIf { it.isNotBlank() },
                    email = userObj?.optString("email")?.takeIf { it.isNotBlank() }
                )
            )
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("Auth response parse failed: ${e.message}")
        }
    }
}