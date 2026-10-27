package com.campusone.app.domain

import com.campusone.app.domain.model.CourseGradeEntry
import com.campusone.app.domain.model.GradingScale
import com.campusone.app.domain.usecase.GpaCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class GpaCalculatorTest {

    @Test
    fun calculateSgpaAndCgpa_10PointScale() {
        val entries = listOf(
            CourseGradeEntry("1", 1, "Math", 4, 9.0),
            CourseGradeEntry("2", 1, "DSA", 4, 8.0),
            CourseGradeEntry("3", 2, "OS", 4, 9.5)
        )
        val summaries = GpaCalculator.summarizeBySemester(entries, GradingScale.SCALE_10)
        assertEquals(2, summaries.size)
        assertEquals(8.50, summaries[0].sgpa, 0.01)
        assertEquals(9.50, summaries[1].sgpa, 0.01)

        val cgpa = GpaCalculator.calculateCgpa(summaries)
        assertEquals(8.83, cgpa, 0.01)
    }

    @Test
    fun requiredSgpaForTargetCgpa_computesTargetAccurately() {
        val req = GpaCalculator.requiredSgpaForTargetCgpa(
            currentCgpa = 8.0,
            completedCredits = 60,
            nextSemesterCredits = 20,
            targetCgpa = 8.25
        )
        assertEquals(9.00, req, 0.01)
    }
}
