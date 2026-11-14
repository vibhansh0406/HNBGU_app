package `in`.campusone.app.util.security

import `in`.campusone.app.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun `validateName accepts a normal name and trims it`() {
        val result = SecurityUtils.validateName("  Aditi Rawat  ")
        assertTrue(result.isSuccess)
        assertEquals("Aditi Rawat", result.getOrNull())
    }

    @Test
    fun `validateName rejects blank and single character names`() {
        assertTrue(SecurityUtils.validateName("   ").isFailure)
        assertTrue(SecurityUtils.validateName("A").isFailure)
    }

    @Test
    fun `validateName rejects control characters and markup`() {
        assertTrue(SecurityUtils.validateName("<script>alert(1)</script>").isFailure)
        assertTrue(SecurityUtils.validateName("Aditi\nRawat").isFailure)
    }

    @Test
    fun `validateName rejects names longer than the short input limit`() {
        val tooLong = "a".repeat(SecurityUtils.MAX_SHORT_LENGTH + 1)
        assertTrue(SecurityUtils.validateName(tooLong).isFailure)
    }

    @Test
    fun `validateEmail normalises case and accepts valid addresses`() {
        val result = SecurityUtils.validateEmail("  Student@HNBGU.ac.IN ")
        assertTrue(result.isSuccess)
        assertEquals("student@hnbgu.ac.in", result.getOrNull())
    }

    @Test
    fun `validateEmail rejects malformed addresses`() {
        assertTrue(SecurityUtils.validateEmail("student@").isFailure)
        assertTrue(SecurityUtils.validateEmail("student hnbgu").isFailure)
        assertTrue(SecurityUtils.validateEmail("").isFailure)
    }

    @Test
    fun `validateScope requires a department and rejects punctuation heavy input`() {
        assertTrue(SecurityUtils.validateScope("Computer Science").isSuccess)
        assertTrue(SecurityUtils.validateScope(" ").isFailure)
        assertTrue(SecurityUtils.validateScope("CS <b>").isFailure)
    }

    @Test
    fun `validateTitle and validateBody enforce content bounds`() {
        assertTrue(SecurityUtils.validateTitle("Exam schedule").isSuccess)
        assertTrue(SecurityUtils.validateTitle(" ").isFailure)
        assertTrue(SecurityUtils.validateTitle("x".repeat(SecurityUtils.MAX_INPUT_LENGTH + 1)).isFailure)
        assertTrue(SecurityUtils.validateBody("Details").isSuccess)
        assertTrue(SecurityUtils.validateBody(" ").isFailure)
        assertTrue(
            SecurityUtils.validateBody("x".repeat(SecurityUtils.MAX_INPUT_LENGTH * 4 + 1)).isFailure
        )
    }

    @Test
    fun `sanitize removes control characters and collapses whitespace`() {
        val cleaned = SecurityUtils.sanitize("  Hello\u0000   world \n ")
        assertEquals("Hello world", cleaned)
    }

    @Test
    fun `requireAdmin only permits the admin role`() {
        assertTrue(SecurityUtils.requireAdmin(UserRole.ADMIN).isSuccess)
        assertTrue(SecurityUtils.requireAdmin(UserRole.STUDENT).isFailure)
        assertTrue(SecurityUtils.requireAdmin(UserRole.CR).isFailure)
        assertTrue(SecurityUtils.requireAdmin(UserRole.MODERATOR).isFailure)
    }

    @Test
    fun `requireModeratorOrAdmin allows approved moderators only`() {
        assertTrue(SecurityUtils.requireModeratorOrAdmin(UserRole.ADMIN, false).isSuccess)
        assertTrue(SecurityUtils.requireModeratorOrAdmin(UserRole.MODERATOR, true).isSuccess)
        assertTrue(SecurityUtils.requireModeratorOrAdmin(UserRole.MODERATOR, false).isFailure)
        assertTrue(SecurityUtils.requireModeratorOrAdmin(UserRole.STUDENT, false).isFailure)
    }

    @Test
    fun `requireScopeMatch enforces department scope unless admin`() {
        assertTrue(
            SecurityUtils.requireScopeMatch(
                moderatorScope = "Computer Science",
                targetScope = "computer science",
                role = UserRole.MODERATOR
            ).isSuccess
        )
        assertTrue(
            SecurityUtils.requireScopeMatch(
                moderatorScope = "Computer Science",
                targetScope = "Physics",
                role = UserRole.MODERATOR
            ).isFailure
        )
        assertTrue(
            SecurityUtils.requireScopeMatch(
                moderatorScope = "",
                targetScope = "Physics",
                role = UserRole.ADMIN
            ).isSuccess
        )
    }
}
