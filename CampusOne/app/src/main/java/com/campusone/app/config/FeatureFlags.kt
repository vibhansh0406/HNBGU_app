package com.campusone.app.config

import com.campusone.app.BuildConfig

data class SupabaseRuntimeConfig(
    val supabaseUrl: String,
    val supabasePublishableKey: String,
    val googleWebClientId: String,
    val fcmEnabled: Boolean
) {
    val isSupabaseConfigured: Boolean
        get() = supabaseUrl.startsWith("https://") &&
            !supabaseUrl.contains("YOUR_PROJECT_REF") &&
            supabasePublishableKey.length > 20 &&
            !supabasePublishableKey.contains("YOUR_SUPABASE")

    val isGoogleAuthConfigured: Boolean
        get() = googleWebClientId.endsWith(".googleusercontent.com") &&
            !googleWebClientId.contains("YOUR_GOOGLE")

    fun missingConfigDiagnosis(): String {
        val missing = mutableListOf<String>()
        if (!isSupabaseConfigured) {
            missing.add("SUPABASE_URL & SUPABASE_PUBLISHABLE_KEY")
        }
        if (!isGoogleAuthConfigured) {
            missing.add("GOOGLE_WEB_CLIENT_ID")
        }
        return if (missing.isEmpty()) {
            "Supabase and Google OAuth client configuration verified."
        } else {
            "Missing configuration: ${missing.joinToString(", ")}. Add them in CampusOne/local.properties (see local.properties.example) or configure them in Settings -> Cloud & Supabase Setup."
        }
    }
}

object FeatureFlags {
    const val CLOUD_FEATURES_ENABLED: Boolean = true
    const val MAX_IMPORT_FILE_BYTES: Long = 10L * 1024L * 1024L

    fun resolveConfig(
        overrideUrl: String = "",
        overrideKey: String = "",
        overrideGoogleClientId: String = ""
    ): SupabaseRuntimeConfig {
        val url = overrideUrl.trim().ifEmpty { BuildConfig.SUPABASE_URL.trim() }.trimEnd('/')
        val key = overrideKey.trim().ifEmpty { BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim() }
        val googleId = overrideGoogleClientId.trim().ifEmpty { BuildConfig.GOOGLE_WEB_CLIENT_ID.trim() }
        val fcm = BuildConfig.FCM_ENABLED.equals("true", ignoreCase = true)
        return SupabaseRuntimeConfig(
            supabaseUrl = url,
            supabasePublishableKey = key,
            googleWebClientId = googleId,
            fcmEnabled = fcm
        )
    }
}
