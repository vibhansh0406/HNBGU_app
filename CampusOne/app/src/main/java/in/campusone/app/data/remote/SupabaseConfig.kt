package `in`.campusone.app.data.remote

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

        return SupabaseConfig(
            url = props.getProperty("supabase.url")?.trim() ?: "",
            anonKey = props.getProperty("supabase.anon.key")?.trim() ?: "",
            googleClientId = props.getProperty("google.oauth.client_id")?.trim() ?: "",
            googleWebClientId = props.getProperty("google.oauth.web_client_id")?.trim() ?: "",
            googleWebClientSecret = props.getProperty("google.oauth.web_client_secret")?.trim() ?: "",
            oauthCallbackUrl = props.getProperty("google.oauth.callback_url")?.trim() ?: ""
        )
    }

    private fun loadLocalProperties(): java.util.Properties {
        val props = java.util.Properties()
        val candidates = listOf(
            java.io.File("local.properties"),
            java.io.File("CampusOne/local.properties"),
            java.io.File(System.getProperty("user.dir"), "local.properties"),
            java.io.File(System.getProperty("user.dir"), "CampusOne", "local.properties")
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
        val props = loadLocalProperties()
        return props.containsKey("supabase.url") && props.containsKey("supabase.anon.key")
    }
}
