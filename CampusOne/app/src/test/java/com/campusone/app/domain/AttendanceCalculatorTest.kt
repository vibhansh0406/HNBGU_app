package com.campusone.app.domain

import com.campusone.app.domain.model.SubjectSummary
import com.campusone.app.domain.model.TimetableSlot
import com.campusone.app.domain.usecase.AttendanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AttendanceCalculatorTest {

    @Test
    fun zeroConductedClasses_returns100PercentAndZeroBunks() {
        val advice = AttendanceCalculator.evaluate(attended = 0, totalConducted = 0, targetPercentage = 75)
        assertEquals(100.0, advice.currentPercentage, 0.01)
        assertEquals(0, advice.safeBunksAvailable)
        assertEquals(0, advice.classesMustAttendToRecover)
        assertTrue(advice.isAboveTarget)
        assertFalse(advice.isWarningZone)
    }

    @Test
    fun above75Percent_computesExactSafeBunks() {
        val advice = AttendanceCalculator.evaluate(attended = 18, totalConducted = 20, targetPercentage = 75)
        assertEquals(90.0, advice.currentPercentage, 0.01)
        assertEquals(4, advice.safeBunksAvailable)
        assertEquals(0, advice.classesMustAttendToRecover)
        assertTrue(advice.isAboveTarget)
        assertFalse(advice.isWarningZone)
    }

    @Test
    fun below75Percent_computesExactClassesToRecover() {
        val advice = AttendanceCalculator.evaluate(attended = 12, totalConducted = 20, targetPercentage = 75)
        assertEquals(60.0, advice.currentPercentage, 0.01)
        assertEquals(0, advice.safeBunksAvailable)
        assertEquals(12, advice.classesMustAttendToRecover)
        assertFalse(advice.isAboveTarget)
        assertTrue(advice.isWarningZone)
    }

    @Test
    fun whatIfSlider_handlesZeroClassesAndSkipsWithoutDivisionByZero() {
        val zeroResult = AttendanceCalculator.calculateWhatIfSkipPercentage(0, 0, 0)
        assertEquals(100.0, zeroResult, 0.01)

        val skipFromZero = AttendanceCalculator.calculateWhatIfSkipPercentage(0, 0, 3)
        assertEquals(0.0, skipFromZero, 0.01)

        val skipTwo = AttendanceCalculator.calculateWhatIfSkipPercentage(18, 20, 2)
        assertEquals(81.8, skipTwo, 0.1)
    }

    @Test
    fun projectSubjectWithTimetable_projectsRemainingClassesAccurately() {
        val advice = AttendanceCalculator.evaluate(15, 20, 75)
        val subject = SubjectSummary(
            id = "sub_dsa",
            code = "CSE-401",
            name = "Algorithms",
            professor = "Prof A",
            semester = 4,
            attendedCount = 15,
            totalClassesCount = 20,
            cancelledCount = 0,
            targetPercentage = 75,
            colorHex = "#0F4C81",
            advice = advice
        )
        val slots = listOf(
            TimetableSlot("s1", "sub_dsa", "Algorithms", "CSE-401", 1, "09:00", "10:00", "R1", "Prof A", 4, "A")
        )
        val start = LocalDate.of(2026, 10, 26)
        val end = start.plusDays(14)
        val proj = AttendanceCalculator.projectSubjectWithTimetable(
            subject = subject,
            timetableSlots = slots,
            fromDate = start,
            endDate = end,
            whatIfSkipCount = 1
        )
        assertEquals(2, proj.remainingScheduledClasses)
        assertEquals(71.4, proj.whatIfPercentage, 0.1)
        assertFalse(proj.isWhatIfAboveTarget)
    }
}
