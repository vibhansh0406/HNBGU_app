package `in`.campusone.app.config

/**
 * Global compile-time feature gate.
 * Keep CLOUD_FEATURES_ENABLED = false for 100% on-device operation without Firebase or login.
 * Cloud-backed screens (Live Section Notice Board, Shared Notes Moderation, Clubs & Events RSVP,
 * and Lost & Found) remain cleanly architected behind repository interfaces and display a
 * "Coming Soon" banner when this flag is false.
 */
object FeatureFlags {
    const val CLOUD_FEATURES_ENABLED: Boolean = false
    const val MAX_IMPORT_FILE_BYTES: Long = 10L * 1024L * 1024L // 10 MB
}
