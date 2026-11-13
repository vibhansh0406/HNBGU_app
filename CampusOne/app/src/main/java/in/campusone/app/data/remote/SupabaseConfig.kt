package `in`.campusone.app.data.remote

import android.content.Context
import java.io.File
import java.util.Properties

data class SupabaseConfig(
    val url: String,
    val anonKey: String,
    val googleClientId: String,
    val googleWebClientId: String,
    val googleWebClientSecret: String,
    val oauthCallbackUrl: String
)

object SupabaseConfigLoader {

    private const val LOCAL_PROPERTIES_KEYS = listOf(
        "supabase.url",
        "supabase.anon.key",
        "google.oauth.client_id",
        "google.oauth.web_client_id",
        "google.oauth.web_client_secret",
        "google.oauth.callback_url"
    )

    @JvmStatic
    fun load(context: Context): SupabaseConfig {
        val props = loadLocalProperties()

        return SupabaseConfig(
            url = requireNonBlank(props, "supabase.url"),
            anonKey = requireNonBlank(props, "supabase.anon.key"),
            googleClientId = requireNonBlank(props, "google.oauth.client_id"),
            googleWebClientId = requireNonBlank(props, "google.oauth.web_client_id"),
            googleWebClientSecret = requireNonBlank(props, "google.oauth.web_client_secret"),
            oauthCallbackUrl = requireNonBlank(props, "google.oauth.callback_url")
        )
    }

    private fun loadLocalProperties(): Properties {
        val props = Properties()
        // local.properties lives in the module root (CampusOne/local.properties)
        val candidates = listOf(
            File("local.properties"),
            File("CampusOne/local.properties"),
            File(System.getProperty("user.dir"), "local.properties"),
            File(System.getProperty("user.dir"), "CampusOne", "local.properties")
        )
        for (file in candidates) {
            if (file.exists()) {
                file.inputStream().use { props.load(it) }
                return props
            }
        }
        // Fallback: empty props (will trigger clear error messages below)
        return props
    }

    private fun requireNonBlank(props: Properties, key: String): String {
        val value = props.getProperty(key)?.trim()
        if (value.isNullOrBlank() || value.startsWith("your_")) {
            throw IllegalStateException(
                "Missing config value for '$key' in local.properties. " +
                    "Add it to CampusOne/local.properties (gitignored)."
            )
        }
        return value
    }

    fun isConfigured(context: Context): Boolean {
        return try {
            load(context)
            true
        } catch (_: Exception) {
            false
        }
    }
}
