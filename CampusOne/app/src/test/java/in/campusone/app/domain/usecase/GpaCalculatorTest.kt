package `in`.campusone.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpaCalculatorTest {

    @Test
    fun `10-point Indian scale computes weighted SGPA, multi-semester CGPA, and 9_5x percentage`() {
        val currentCourses = listOf(
            CourseGradeInput("1", "CS301", "DSA", credits = 4, gradePoint = 9.0),     // 36
            CourseGradeInput("2", "CS302", "OS", credits = 4, gradePoint = 8.0),      // 32
            CourseGradeInput("3", "SEC301", "Android", credits = 2, gradePoint = 10.0) // 20
        )
        // Current Sem: 88 / 10 = 8.80 SGPA
        val pastSems = listOf(
            SemesterGpaRecord(semesterNumber = 1, totalCredits = 10, sgpa = 8.0) // 80
        )
        // Combined: (88 + 80) / 20 = 8.40 CGPA -> 8.40 * 9.5 = 79.8%
        val result = GpaCalculator.calculateSgpaAndCgpa(
            currentSemesterNumber = 2,
            currentSemesterCourses = currentCourses,
            pastSemesters = pastSems,
            scaleType = GradingScaleType.TEN_POINT_INDIAN,
            percentageMultiplier = 9.5
        )

        assertEquals(8.80, result.sgpa, 0.001)
        assertEquals(8.40, result.cgpa, 0.001)
        assertEquals(79.8, result.equivalentPercentage, 0.01)
        assertEquals(2, result.semesterTrend.size)
    }

    @Test
    fun `4-point scale computes SGPA and percentage accurately`() {
        val courses = listOf(
            CourseGradeInput("1", "CS101", "Intro CS", credits = 4, gradePoint = 4.0),
            CourseGradeInput("2", "MA101", "Calculus", credits = 4, gradePoint = 3.0)
        )
        val result = GpaCalculator.calculateSgpaAndCgpa(
            currentSemesterNumber = 1,
            currentSemesterCourses = courses,
            scaleType = GradingScaleType.FOUR_POINT_WES
        )
        assertEquals(3.50, result.sgpa, 0.001)
        assertEquals(87.5, result.equivalentPercentage, 0.01)
    }

    @Test
    fun `what do I need mode calculates exact required SGPA and detects impossible targets`() {
        // Completed 40 credits with 8.0 CGPA (320 points). Next sem is 20 credits (total 60).
        // To reach 8.5 CGPA overall: need 8.5 * 60 = 510 total points => 510 - 320 = 190 points in 20 credits => 9.50 SGPA.
        val achievable = GpaCalculator.calculateRequiredSgpaForTargetCgpa(
            completedCredits = 40,
            currentCgpa = 8.0,
            upcomingSemesterCredits = 20,
            targetCgpa = 8.5,
            scaleType = GradingScaleType.TEN_POINT_INDIAN
        )
        assertTrue(achievable.isAchievable)
        assertFalse(achievable.alreadyAchieved)
        assertEquals(9.50, achievable.requiredSgpa, 0.01)

        // Target 9.2 CGPA overall: need 9.2 * 60 = 552 points => 232 / 20 = 11.60 SGPA (> 10.0 max)
        val impossible = GpaCalculator.calculateRequiredSgpaForTargetCgpa(
            completedCredits = 40,
            currentCgpa = 8.0,
            upcomingSemesterCredits = 20,
            targetCgpa = 9.2,
            scaleType = GradingScaleType.TEN_POINT_INDIAN
        )
        assertFalse(impossible.isAchievable)
        assertEquals(11.60, impossible.requiredSgpa, 0.01)
    }
}
