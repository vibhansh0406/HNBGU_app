package `in`.campusone.app.data.remote

import `in`.campusone.app.BuildConfig
import java.util.Properties

data class SupabaseConfig(
    val url: String = "",
    val anonKey: String = "",
    val googleClientId: String = "",
    val googleWebClientId: String = "",
    val googleWebClientSecret: String = "",
    val oauthCallbackUrl: String = ""
)

object SupabaseConfigLoader {

    @JvmStatic
    fun load(context: android.content.Context): SupabaseConfig {
        val props = loadLocalProperties()

        fun value(buildValue: String, key: String): String =
            buildValue.takeIf { it.isNotBlank() }
                ?: props.getProperty(key)?.trim().orEmpty()

        return SupabaseConfig(
            url = value(BuildConfig.SUPABASE_URL, "supabase.url"),
            anonKey = value(BuildConfig.SUPABASE_ANON_KEY, "supabase.anon.key"),
            googleClientId = value(BuildConfig.GOOGLE_OAUTH_CLIENT_ID, "google.oauth.client_id"),
            googleWebClientId = value(BuildConfig.GOOGLE_OAUTH_WEB_CLIENT_ID, "google.oauth.web_client_id"),
            googleWebClientSecret = value(BuildConfig.GOOGLE_OAUTH_WEB_CLIENT_SECRET, "google.oauth.web_client_secret"),
            oauthCallbackUrl = value(BuildConfig.GOOGLE_OAUTH_CALLBACK_URL, "google.oauth.callback_url")
        )
    }

    private fun loadLocalProperties(): Properties {
        val props = Properties()
        val candidates = listOf(
            java.io.File("local.properties"),
            java.io.File("CampusOne", "local.properties"),
            java.io.File(System.getProperty("user.dir") ?: ".", "local.properties"),
            java.io.File(java.io.File(System.getProperty("user.dir") ?: ".", "CampusOne"), "local.properties")
        )
        for (file in candidates) {
            if (file.exists()) {
                try {
                    file.inputStream().use { props.load(it) }
                } catch (_: Exception) {
                    // Ignore malformed properties file
                }
                return props
            }
        }
        return props
    }

    fun isConfigured(context: android.content.Context): Boolean {
        val config = load(context)
        return config.url.isNotBlank() && config.anonKey.isNotBlank()
    }
}
