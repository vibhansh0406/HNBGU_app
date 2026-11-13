package `in`.campusone.app.util

/**
 * Parses a timestamp that may arrive as epoch millis (Number) or an ISO-8601
 * string (e.g. `2026-01-01T10:00:00Z`) coming back from PostgREST.
 */
object TimestampUtils {

    fun toEpochMillis(value: Any?): Long? = when (value) {
        null -> null
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
            ?: runCatching { java.time.Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
        else -> null
    }
}