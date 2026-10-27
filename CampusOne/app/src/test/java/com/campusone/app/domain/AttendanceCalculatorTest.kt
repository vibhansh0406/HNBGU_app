package com.campusone.app.domain

import com.campusone.app.domain.usecase.AttendanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun zeroConductedClasses_returns100PercentAndZeroBunks() {
        val advice = AttendanceCalculator.evaluate(attended = 0, totalConducted = 0, targetPercentage = 75)
        assertEquals(100.0, advice.currentPercentage, 0.01)
        assertEquals(0, advice.safeBunksAvailable)
        assertEquals(0, advice.classesMustAttendToRecover)
        assertTrue(advice.isAboveTarget)
    }

    @Test
    fun above75Percent_computesExactSafeBunks() {
        val advice = AttendanceCalculator.evaluate(attended = 18, totalConducted = 20, targetPercentage = 75)
        assertEquals(90.0, advice.currentPercentage, 0.01)
        assertEquals(4, advice.safeBunksAvailable)
        assertEquals(0, advice.classesMustAttendToRecover)
        assertTrue(advice.isAboveTarget)
    }

    @Test
    fun below75Percent_computesExactClassesToRecover() {
        val advice = AttendanceCalculator.evaluate(attended = 12, totalConducted = 20, targetPercentage = 75)
        assertEquals(60.0, advice.currentPercentage, 0.01)
        assertEquals(0, advice.safeBunksAvailable)
        assertEquals(12, advice.classesMustAttendToRecover)
        assertFalse(advice.isAboveTarget)
    }
}
