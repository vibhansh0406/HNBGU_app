package com.campusone.app.domain

import com.campusone.app.domain.model.CampusAppRole
import com.campusone.app.domain.model.LoginIntentPath
import com.campusone.app.domain.model.RemoteProfileDto
import com.campusone.app.domain.usecase.AuthorizationDecision
import com.campusone.app.domain.usecase.RoleAuthorizationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleAuthorizationEngineTest {

    @Test
    fun studentSelectingAdminLogin_isDeniedAdminAccess() {
        val studentProfile = RemoteProfileDto(
            id = "u_student_1",
            fullName = "Aarav Rawat",
            email = "aarav@hnbgu.ac.in",
            role = "student",
            departmentId = "dept_cse",
            accountStatus = "active"
        )
        val decision = RoleAuthorizationEngine.evaluateLoginGate(
            intentPath = LoginIntentPath.ADMIN,
            profile = studentProfile
        )
        assertTrue(decision is AuthorizationDecision.AccessDenied)
        val denied = decision as AuthorizationDecision.AccessDenied
        assertTrue(denied.canFallbackToStudentDashboard)
        assertTrue(denied.reason.contains("student"))
    }

    @Test
    fun suspendedAdmin_isDeniedAccessOnBothPaths() {
        val suspendedAdmin = RemoteProfileDto(
            id = "u_admin_1",
            fullName = "Suspended Admin",
            email = "admin@hnbgu.ac.in",
            role = "department_admin",
            departmentId = "dept_cse",
            accountStatus = "suspended"
        )
        val adminDecision = RoleAuthorizationEngine.evaluateLoginGate(
            intentPath = LoginIntentPath.ADMIN,
            profile = suspendedAdmin
        )
        assertTrue(adminDecision is AuthorizationDecision.AccessDenied)
        assertFalse((adminDecision as AuthorizationDecision.AccessDenied).canFallbackToStudentDashboard)
    }

    @Test
    fun activeDepartmentAdmin_canManageOnlyAssignedDepartment() {
        val deptAdmin = RemoteProfileDto(
            id = "u_admin_2",
            fullName = "CSE HOD",
            email = "hod.cse@hnbgu.ac.in",
            role = "department_admin",
            departmentId = "dept_cse",
            accountStatus = "active"
        )
        val decision = RoleAuthorizationEngine.evaluateLoginGate(
            intentPath = LoginIntentPath.ADMIN,
            profile = deptAdmin
        )
        assertTrue(decision is AuthorizationDecision.AuthorizedAdmin)
        val authAdmin = decision as AuthorizationDecision.AuthorizedAdmin
        assertEquals(CampusAppRole.DEPARTMENT_ADMIN, authAdmin.effectiveRole)

        assertTrue(
            RoleAuthorizationEngine.canManageDepartmentContent(
                role = CampusAppRole.DEPARTMENT_ADMIN,
                userDepartmentId = "dept_cse",
                targetDepartmentId = "dept_cse"
            )
        )
        assertFalse(
            RoleAuthorizationEngine.canManageDepartmentContent(
                role = CampusAppRole.DEPARTMENT_ADMIN,
                userDepartmentId = "dept_cse",
                targetDepartmentId = "dept_ece"
            )
        )
    }
}
