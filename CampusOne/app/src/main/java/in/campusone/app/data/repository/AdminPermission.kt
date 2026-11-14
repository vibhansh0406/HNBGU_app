package `in`.campusone.app.data.repository

/**
 * Capability codes mirrored from `public.admin_permissions` in supabase/schema.sql.
 * The database is the only authority: these values exist so the UI can decide
 * what to render, never to decide what is allowed.
 */
enum class AdminPermission(val code: String) {
    VIEW_USERS("VIEW_USERS"),
    MANAGE_USERS("MANAGE_USERS"),
    MANAGE_ROLES("MANAGE_ROLES"),
    MANAGE_NOTICES("MANAGE_NOTICES"),
    MANAGE_RESOURCES("MANAGE_RESOURCES"),
    MANAGE_ASSIGNMENTS("MANAGE_ASSIGNMENTS"),
    MODERATE_REPORTS("MODERATE_REPORTS"),
    MANAGE_EVENTS("MANAGE_EVENTS"),
    VIEW_AUDIT_LOGS("VIEW_AUDIT_LOGS"),
    MANAGE_SYSTEM_SETTINGS("MANAGE_SYSTEM_SETTINGS");

    companion object {
        fun from(code: String): AdminPermission? = entries.firstOrNull { it.code == code }
    }
}

/** Server-verified identity of the signed-in account. */
data class AdminIdentity(
    val role: String,
    val isOwner: Boolean,
    val isSuspended: Boolean,
    val permissions: Set<AdminPermission>
) {
    fun can(permission: AdminPermission): Boolean = isOwner || permission in permissions

    val isSuperAdmin: Boolean get() = isOwner && role == "SUPER_ADMIN"

    companion object {
        val Anonymous = AdminIdentity(
            role = "ANON",
            isOwner = false,
            isSuspended = false,
            permissions = emptySet()
        )
    }
}