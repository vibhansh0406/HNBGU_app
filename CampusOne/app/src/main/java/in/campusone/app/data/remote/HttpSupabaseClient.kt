package `in`.campusone.app.data.remote

import android.content.Context
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpSupabaseClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: SupabaseConfig
) : SupabaseClient {

    private val baseUrl = config.url.trimEnd { it == '/' } + "/rest/v1"
    private val authBaseUrl = config.url.trimEnd { it == '/' } + "/auth/v1"
    private val headers = mapOf(
        "apikey" to config.anonKey,
        "Authorization" to "Bearer ${config.anonKey}",
        "Content-Type" to "application/json",
        "Prefer" to "return=representation"
    )

    override fun from(table: String): SupabaseQueryBuilder =
        HttpSupabaseQueryBuilder("$baseUrl/$table", headers)

    override val auth: SupabaseAuth = HttpSupabaseAuth(config, authBaseUrl)

    private fun request(
        method: String,
        url: String,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): SupabaseResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        headers.forEach { (k, v) -> connection.addRequestProperty(k, v) }
        extraHeaders.forEach { (k, v) -> connection.addRequestProperty(k, v) }
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        body?.let {
            connection.outputStream.use { os -> os.write(it.toByteArray(StandardCharsets.UTF_8)) }
        }

        val responseCode = connection.responseCode
        val responseBody = connection.inputStream.use { it.readBytes() }
        connection.disconnect()

        val json = String(responseBody, StandardCharsets.UTF_8)
        return when {
            responseCode in 200..299 -> SupabaseResponse(parseJson(json))
            else -> SupabaseResponse(null)
        }
    }

    private fun parseJson(json: String): Any? {
        return try {
            org.json.JSONArray(json)
        } catch (_: Exception) {
            try {
                org.json.JSONObject(json)
            } catch (_: Exception) {
                json
            }
        }
    }
}

private class HttpSupabaseQueryBuilder(
    private val url: String,
    private val headers: Map<String, String>
) : SupabaseQueryBuilder {

    private var selectColumns: String = "*"
    private var filters: MutableList<String> = mutableListOf()
    private var orderColumn: String? = null
    private var orderAscending: Boolean = true
    private var isSingle: Boolean = false
    private var upsertData: Any? = null

    override fun select(columns: String): SupabaseQueryBuilder {
        selectColumns = columns
        return this
    }

    override fun eq(column: String, value: Any): SupabaseQueryBuilder {
        filters.add("$column=eq.${value}")
        return this
    }

    override fun order(column: String, ascending: Boolean): SupabaseQueryBuilder {
        orderColumn = column
        orderAscending = ascending
        return this
    }

    override fun single(): SupabaseQueryBuilder {
        isSingle = true
        return this
    }

    override fun upsert(data: Map<String, Any>): SupabaseQueryBuilder {
        upsertData = listOf(data)
        return this
    }

    override fun upsert(data: List<Map<String, Any>>): SupabaseQueryBuilder {
        upsertData = data
        return this
    }

    override fun execute(): SupabaseResponse {
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        if (upsertData != null) {
            connection.requestMethod = "POST"
            headers.forEach { (k, v) -> connection.addRequestProperty(k, v) }
            connection.addRequestProperty("Prefer", "return=representation")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            val body = org.json.JSONArray(upsertData).toString()
            connection.outputStream.use { os -> os.write(body.toByteArray(java.nio.charset.StandardCharsets.UTF_8)) }
        } else {
            connection.requestMethod = "GET"
            headers.forEach { (k, v) -> connection.addRequestProperty(k, v) }
            val queryParams = mutableListOf<String>()
            queryParams.add("select=$selectColumns")
            filters.forEach { queryParams.add(it) }
            if (isSingle) queryParams.add("limit=1")
            if (orderColumn != null) {
                queryParams.add("order=${orderColumn}.${if (orderAscending) "asc" else "desc"}")
            }
            val fullUrl = if (queryParams.isNotEmpty()) "$url?${queryParams.joinToString("&")}" else url
            // Re-open with full URL
            return get(fullUrl)
        }

        val responseCode = connection.responseCode
        val body = connection.inputStream.use { it.readBytes() }
        connection.disconnect()
        val json = String(body, java.nio.charset.StandardCharsets.UTF_8)
        return when {
            responseCode in 200..299 -> SupabaseResponse(parseJson(json))
            else -> SupabaseResponse(null)
        }
    }

    private fun get(fullUrl: String): SupabaseResponse {
        val connection = java.net.URL(fullUrl).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        headers.forEach { (k, v) -> connection.addRequestProperty(k, v) }
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        val responseCode = connection.responseCode
        val body = if (responseCode in 200..299) connection.inputStream.use { it.readBytes() } else byteArray(0)
        connection.disconnect()
        val json = String(body, java.nio.charset.StandardCharsets.UTF_8)
        return when {
            responseCode in 200..299 -> SupabaseResponse(parseJson(json))
            else -> SupabaseResponse(null)
        }
    }

    private fun parseJson(json: String): Any? {
        return if (json.isBlank()) null else try {
            org.json.JSONArray(json)
        } catch (_: Exception) {
            try {
                org.json.JSONObject(json)
            } catch (_: Exception) {
                json
            }
        }
    }
}

private class HttpSupabaseAuth(
    private val config: SupabaseConfig,
    private val authBaseUrl: String
) : SupabaseAuth {

    private var currentSessionValue: SupabaseSession? = null

    override fun signInWithIdToken(provider: String, token: String): SupabaseSession? {
        val url = "$authBaseUrl/token?grant_type=id_token&provider=$provider"
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.addRequestProperty("apikey", config.anonKey)
        connection.addRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        val body = """{"access_token":"$token"}"""
        connection.outputStream.use { os -> os.write(body.toByteArray(java.nio.charset.StandardCharsets.UTF_8)) }
        val responseCode = connection.responseCode
        val response = if (responseCode in 200..299) {
            connection.inputStream.use { it.readBytes() }
        } else {
            connection.errorStream?.use { it.readBytes() } ?: byteArray(0)
        }
        connection.disconnect()
        val json = String(response, java.nio.charset.StandardCharsets.UTF_8)
        if (responseCode in 200..299) {
            try {
                val obj = org.json.JSONObject(json)
                val accessToken = obj.optString("access_token", null)
                val userObj = obj.optJSONObject("user")
                val userId = userObj?.optString("id", null)
                currentSessionValue = SupabaseSession(accessToken, SupabaseUser(userId))
                return currentSessionValue
            } catch (_: Exception) {
                return null
            }
        }
        return null
    }

    override fun signOut(): Unit {
        currentSessionValue = null
    }

    override val currentSession: SupabaseSession?
        get() = currentSessionValue

    override val currentUser: SupabaseUser?
        get() = currentSessionValue?.user

    override fun authStateChanges(callback: (SupabaseSession?) -> Unit): Unit {
        // Stateless HTTP client - no persistent auth state
        callback(currentSessionValue)
    }
}

private fun parseJson(json: String): Any? {
    return if (json.isBlank()) null else try {
        org.json.JSONArray(json)
    } catch (_: Exception) {
        try {
            org.json.JSONObject(json)
        } catch (_: Exception) {
            json
        }
    }
}
