package `in`.campusone.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpaCalculatorTest {

    @Test
    fun `marks convert to the ten point scale`() {
        assertEquals(10.0, GpaCalculator.convertMarksToTenPointGrade(95.0), 0.001)
        assertEquals(9.0, GpaCalculator.convertMarksToTenPointGrade(85.0), 0.001)
        assertEquals(8.0, GpaCalculator.convertMarksToTenPointGrade(72.0), 0.001)
        assertEquals(4.0, GpaCalculator.convertMarksToTenPointGrade(42.0), 0.001)
        assertEquals(0.0, GpaCalculator.convertMarksToTenPointGrade(12.0), 0.001)
    }

    @Test
    fun `sgpa is credit weighted`() {
        val result = GpaCalculator.calculateSgpaAndCgpa(
            currentSemesterNumber = 3,
            currentSemesterCourses = listOf(
                CourseGradeInput(subjectCode = "BCS301", subjectName = "DBMS", credits = 4, gradePoint = 9.0),
                CourseGradeInput(subjectCode = "BCS302", subjectName = "OS", credits = 2, gradePoint = 7.0)
            )
        )
        // (4 * 9 + 2 * 7) / 6 = 50 / 6 = 8.33
        assertEquals(8.33, result.sgpa, 0.01)
        assertEquals(6, result.totalCredits)
    }

    @Test
    fun `cgpa combines past semesters with the current one`() {
        val result = GpaCalculator.calculateSgpaAndCgpa(
            currentSemesterNumber = 3,
            currentSemesterCourses = listOf(
                CourseGradeInput(subjectCode = "BCS301", subjectName = "DBMS", credits = 4, gradePoint = 8.0)
            ),
            pastSemesters = listOf(
                SemesterGpaRecord(semesterNumber = 1, totalCredits = 6, sgpa = 8.0),
                SemesterGpaRecord(semesterNumber = 2, totalCredits = 6, sgpa = 8.0)
            )
        )
        // ((4 * 8) + (6 * 8) + (6 * 8)) / 16 = 160 / 16 = 10 is impossible, so recompute:
        // current weighted = 32, past weighted = 48 + 48 = 96, total = 128 / 16 = 8.0
        assertEquals(8.0, result.cgpa, 0.01)
    }

    @Test
    fun `zero credits does not divide by zero`() {
        val result = GpaCalculator.calculateSgpaAndCgpa(
            currentSemesterNumber = 1,
            currentSemesterCourses = emptyList()
        )
        assertEquals(0.0, result.sgpa, 0.001)
        assertEquals(0.0, result.cgpa, 0.001)
    }

    @Test
    fun `required sgpa for a target cgpa is computed from credits`() {
        // 60 credits done at 7.0, 24 upcoming credits, target 8.0:
        // (8 * 84 - 7 * 60) / 24 = (672 - 420) / 24 = 10.5 -> not achievable on a 10 point scale
        val requirement = GpaCalculator.calculateRequiredSgpaForTargetCgpa(
            completedCredits = 60,
            currentCgpa = 7.0,
            upcomingSemesterCredits = 24,
            targetCgpa = 8.0
        )
        assertEquals(10.5, requirement.requiredSgpa, 0.01)
        assertFalse(requirement.isAchievable)
        assertFalse(requirement.alreadyAchieved)
    }

    @Test
    fun `target cgpa already met reports achieved without upcoming credits`() {
        val requirement = GpaCalculator.calculateRequiredSgpaForTargetCgpa(
            completedCredits = 60,
            currentCgpa = 8.4,
            upcomingSemesterCredits = 0,
            targetCgpa = 8.0
        )
        assertTrue(requirement.alreadyAchieved)
        assertTrue(requirement.isAchievable)
    }

    @Test
    fun `achievable target returns the required sgpa`() {
        val requirement = GpaCalculator.calculateRequiredSgpaForTargetCgpa(
            completedCredits = 60,
            currentCgpa = 7.0,
            upcomingSemesterCredits = 24,
            targetCgpa = 7.5
        )
        // (7.5 * 84 - 7 * 60) / 24 = (630 - 420) / 24 = 8.75
        assertEquals(8.75, requirement.requiredSgpa, 0.01)
        assertTrue(requirement.isAchievable)
    }
}
