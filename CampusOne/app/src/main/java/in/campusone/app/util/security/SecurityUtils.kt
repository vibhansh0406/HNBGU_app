package `in`.campusone.app.util.security

import android.text.TextUtils
import androidx.compose.material3.MaterialTheme
import `in`.campusone.app.domain.model.UserRole
import kotlin.Result
import kotlin.text.Regex

object SecurityUtils {

    /** Max length for free-text inputs to prevent abuse. */
    const val MAX_INPUT_LENGTH = 500
    const val MAX_SHORT_LENGTH = 100

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    /** Validates a non-blank, length-bounded display name. */
    fun validateName(value: String): Result<String> {
        val trimmed = value.trim()
        return when {
            trimmed.isBlank() -> Result.failure(IllegalArgumentException("Name is required"))
            trimmed.length < 2 -> Result.failure(IllegalArgumentException("Name too short"))
            trimmed.length > MAX_SHORT_LENGTH -> Result.failure(
                IllegalArgumentException("Name too long (max $MAX_SHORT_LENGTH chars)")
            )
            !trimmed.matches(Regex("^[\\p{L} .'-]{2,}$")) -> Result.failure(
                IllegalArgumentException("Name contains invalid characters")
            )
            else -> Result.success(trimmed)
        }
    }

    /** Validates an email address. */
    fun validateEmail(value: String): Result<String> {
        val trimmed = value.trim().lowercase()
        return when {
            trimmed.isBlank() -> Result.failure(IllegalArgumentException("Email is required"))
            !EMAIL_REGEX.matches(trimmed) -> Result.failure(
                IllegalArgumentException("Enter a valid email address")
            )
            trimmed.length > MAX_SHORT_LENGTH -> Result.failure(
                IllegalArgumentException("Email too long")
            )
            else -> Result.success(trimmed)
        }
    }

    /** Validates a moderator scope/department name. */
    fun validateScope(value: String): Result<String> {
        val trimmed = value.trim()
        return when {
            trimmed.isBlank() -> Result.failure(IllegalArgumentException("Department scope is required"))
            trimmed.length < 2 -> Result.failure(IllegalArgumentException("Scope too short"))
            trimmed.length > MAX_SHORT_LENGTH -> Result.failure(
                IllegalArgumentException("Scope too long (max $MAX_SHORT_LENGTH chars)")
            )
            !trimmed.matches(Regex("^[\\p{L}0-9 .&'-]{2,}$")) -> Result.failure(
                IllegalArgumentException("Scope contains invalid characters")
            )
            else -> Result.success(trimmed)
        }
    }

    /** Validates a notice/announcement title. */
    fun validateTitle(value: String): Result<String> {
        val trimmed = value.trim()
        return when {
            trimmed.isBlank() -> Result.failure(IllegalArgumentException("Title is required"))
            trimmed.length > MAX_INPUT_LENGTH -> Result.failure(
                IllegalArgumentException("Title too long (max $MAX_INPUT_LENGTH chars)")
            )
            else -> Result.success(trimmed)
        }
    }

    /** Validates a notice/announcement body. */
    fun validateBody(value: String): Result<String> {
        val trimmed = value.trim()
        return when {
            trimmed.isBlank() -> Result.failure(IllegalArgumentException("Details are required"))
            trimmed.length > MAX_INPUT_LENGTH * 4 -> Result.failure(
                IllegalArgumentException("Details too long")
            )
            else -> Result.success(trimmed)
        }
    }

    /** Sanitizes user input by removing control characters and excessive whitespace. */
    fun sanitize(value: String): String {
        return value
            .replace(Regex("[\\p{Cc}]"), "") // remove control chars
            .replace(Regex("\\s+"), " ")     // collapse whitespace
            .trim()
    }

    /** Role-based access check: only ADMIN can manage moderators/users. */
    fun requireAdmin(role: UserRole): Result<Unit> {
        return if (role == UserRole.ADMIN) {
            Result.success(Unit)
        } else {
            Result.failure(
                SecurityException("Access denied: only App Admin can manage moderators and users.")
            )
        }
    }

    /** Role-based access check: ADMIN or active MODERATOR can manage scoped data. */
    fun requireModeratorOrAdmin(role: UserRole, isActiveModerator: Boolean): Result<Unit> {
        return when {
            role == UserRole.ADMIN -> Result.success(Unit)
            isActiveModerator -> Result.success(Unit)
            else -> Result.failure(
                SecurityException("Access denied: insufficient permissions for this department.")
            )
        }
    }

    /** Checks that a moderator's scope matches the target department before allowing writes. */
    fun requireScopeMatch(
        moderatorScope: String,
        targetScope: String,
        role: UserRole
    ): Result<Unit> {
        if (role == UserRole.ADMIN) return Result.success(Unit)
        if (moderatorScope.isNotBlank() && moderatorScope.equals(targetScope, ignoreCase = true)) {
            return Result.success(Unit)
        }
        return Result.failure(
            SecurityException("Access denied: you can only manage ${moderatorScope.ifBlank { "your department" }} data.")
        )
    }

    /** Empty result helper. */
    fun <T> emptyResult(): Result<T> = Result.failure(IllegalStateException("Operation not permitted"))
}
