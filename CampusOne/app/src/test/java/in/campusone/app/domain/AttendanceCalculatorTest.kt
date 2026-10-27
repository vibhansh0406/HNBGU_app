package `in`.campusone.app.domain

import `in`.campusone.app.domain.usecase.AttendanceCalculator
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
        // 18 / 20 = 90%. At 75% target, 18 / 0.75 = 24 -> can miss 4 more classes (18/24 = 75.0%)
        val advice = AttendanceCalculator.evaluate(attended = 18, totalConducted = 20, targetPercentage = 75)
        assertEquals(90.0, advice.currentPercentage, 0.01)
        assertEquals(4, advice.safeBunksAvailable)
        assertEquals(0, advice.classesMustAttendToRecover)
        assertTrue(advice.isAboveTarget)
    }

    @Test
    fun below75Percent_computesExactClassesToRecover() {
        // 12 / 20 = 60%. Need (12 + y) / (20 + y) >= 0.75 => y = 12 (24/32 = 75%)
        val advice = AttendanceCalculator.evaluate(attended = 12, totalConducted = 20, targetPercentage = 75)
        assertEquals(60.0, advice.currentPercentage, 0.01)
        assertEquals(0, advice.safeBunksAvailable)
        assertEquals(12, advice.classesMustAttendToRecover)
        assertFalse(advice.isAboveTarget)
    }
}
