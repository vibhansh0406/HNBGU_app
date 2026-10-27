package com.campusone.app.domain.usecase

import com.campusone.app.domain.model.AccountStatus
import com.campusone.app.domain.model.CampusAppRole
import com.campusone.app.domain.model.LoginIntentPath
import com.campusone.app.domain.model.RemoteProfileDto

sealed class AuthorizationDecision {
    data class AuthorizedStudent(
        val profile: RemoteProfileDto,
        val effectiveRole: CampusAppRole
    ) : AuthorizationDecision()

    data class AuthorizedAdmin(
        val profile: RemoteProfileDto,
        val effectiveRole: CampusAppRole,
        val departmentId: String?
    ) : AuthorizationDecision()

    data class AccessDenied(
        val title: String,
        val reason: String,
        val canFallbackToStudentDashboard: Boolean = false
    ) : AuthorizationDecision()
}

object RoleAuthorizationEngine {

    /**
     * Evaluates whether an authenticated Supabase profile is permitted to enter
     * the requested login path (Student vs Admin).
     * Selecting "Admin Login" NEVER assigns or elevates a user's role.
     */
    fun evaluateLoginGate(
        intentPath: LoginIntentPath,
        profile: RemoteProfileDto?
    ): AuthorizationDecision {
        if (profile == null) {
            return AuthorizationDecision.AccessDenied(
                title = "Profile Not Initialized",
                reason = "No profile record was found in Supabase for this account. Ensure migration 20261027000001_initial_campusone_schema.sql has been executed in your Supabase SQL Editor.",
                canFallbackToStudentDashboard = false
            )
        }

        when (profile.parsedStatus) {
            AccountStatus.SUSPENDED -> {
                return AuthorizationDecision.AccessDenied(
                    title = "Account Suspended",
                    reason = "Your CampusOne account (${profile.email}) has been suspended by an administrator. Protected campus resources are locked.",
                    canFallbackToStudentDashboard = false
                )
            }
            AccountStatus.PENDING -> {
                return AuthorizationDecision.AccessDenied(
                    title = "Account Pending Approval",
                    reason = "Your account (${profile.email}) is awaiting verification. Please contact your department administrator.",
                    canFallbackToStudentDashboard = false
                )
            }
            AccountStatus.ACTIVE -> Unit
        }

        val role = profile.parsedRole

        return when (intentPath) {
            LoginIntentPath.ADMIN -> {
                if (!role.isAdminRole) {
                    AuthorizationDecision.AccessDenied(
                        title = "Admin Access Denied",
                        reason = "Your account (${profile.email}) has the 'student' role in Supabase. Selecting Admin Login does not grant administrative privileges.",
                        canFallbackToStudentDashboard = true
                    )
                } else {
                    AuthorizationDecision.AuthorizedAdmin(
                        profile = profile,
                        effectiveRole = role,
                        departmentId = profile.departmentId
                    )
                }
            }
            LoginIntentPath.STUDENT -> {
                AuthorizationDecision.AuthorizedStudent(
                    profile = profile,
                    effectiveRole = role
                )
            }
        }
    }

    fun canManageDepartmentContent(
        role: CampusAppRole,
        userDepartmentId: String?,
        targetDepartmentId: String?
    ): Boolean {
        return when (role) {
            CampusAppRole.SUPER_ADMIN -> true
            CampusAppRole.DEPARTMENT_ADMIN ->
                !userDepartmentId.isNullOrBlank() && userDepartmentId == targetDepartmentId
            else -> false
        }
    }

    fun canManageNotices(
        role: CampusAppRole,
        userDepartmentId: String?,
        targetDepartmentId: String?
    ): Boolean {
        return when (role) {
            CampusAppRole.SUPER_ADMIN -> true
            CampusAppRole.DEPARTMENT_ADMIN, CampusAppRole.NOTICE_ADMIN ->
                !userDepartmentId.isNullOrBlank() && userDepartmentId == targetDepartmentId
            else -> false
        }
    }

    fun canManageExams(
        role: CampusAppRole,
        userDepartmentId: String?,
        targetDepartmentId: String?
    ): Boolean {
        return when (role) {
            CampusAppRole.SUPER_ADMIN -> true
            CampusAppRole.DEPARTMENT_ADMIN, CampusAppRole.EXAM_ADMIN ->
                !userDepartmentId.isNullOrBlank() && userDepartmentId == targetDepartmentId
            else -> false
        }
    }
}
