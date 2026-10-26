package `in`.campusone.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun `zero classes conducted returns NO_DATA health and zero counts`() {
        val insight = AttendanceCalculator.calculate(
            attended = 0,
            totalConducted = 0,
            cancelled = 3,
            targetPercentage = 75
        )
        assertEquals(AttendanceHealth.NO_DATA, insight.health)
        assertEquals(0, insight.classesCanMissSafely)
        assertEquals(0, insight.classesMustAttendToRecover)
        assertEquals(3, insight.cancelled)
    }

    @Test
    fun `exact 75 percent (15 of 20) is ON_EDGE with 0 safe misses`() {
        val insight = AttendanceCalculator.calculate(
            attended = 15,
            totalConducted = 20,
            targetPercentage = 75
        )
        assertEquals(75.0, insight.currentPercentage, 0.001)
        assertEquals(0, insight.classesCanMissSafely)
        assertEquals(0, insight.classesMustAttendToRecover)
        assertEquals(AttendanceHealth.ON_EDGE, insight.health)
    }

    @Test
    fun `above target (18 of 20 = 90 percent) allows 4 safe misses to stay at 75 percent`() {
        // 18 / (20 + 4) = 18 / 24 = 75.0%
        val insight = AttendanceCalculator.calculate(
            attended = 18,
            totalConducted = 20,
            targetPercentage = 75
        )
        assertEquals(90.0, insight.currentPercentage, 0.001)
        assertEquals(4, insight.classesCanMissSafely)
        assertEquals(0, insight.classesMustAttendToRecover)
        assertEquals(AttendanceHealth.SAFE, insight.health)
    }

    @Test
    fun `below target (12 of 20 = 60 percent) requires 12 consecutive classes to reach 75 percent`() {
        // (12 + 12) / (20 + 12) = 24 / 32 = 75.0%
        val insight = AttendanceCalculator.calculate(
            attended = 12,
            totalConducted = 20,
            targetPercentage = 75
        )
        assertEquals(60.0, insight.currentPercentage, 0.001)
        assertEquals(0, insight.classesCanMissSafely)
        assertEquals(12, insight.classesMustAttendToRecover)
        assertEquals(AttendanceHealth.CRITICAL, insight.health)
    }

    @Test
    fun `slightly below target (14 of 20 = 70 percent) returns WARNING health and 4 classes to recover`() {
        // (14 + 4) / (20 + 4) = 18 / 24 = 75.0%
        val insight = AttendanceCalculator.calculate(
            attended = 14,
            totalConducted = 20,
            targetPercentage = 75
        )
        assertEquals(70.0, insight.currentPercentage, 0.001)
        assertEquals(4, insight.classesMustAttendToRecover)
        assertEquals(AttendanceHealth.WARNING, insight.health)
    }

    @Test
    fun `custom target 80 percent calculates recovery accurately`() {
        // 7 / 10 = 70%. For 80%: (7 + y) / (10 + y) >= 0.8 => 7 + y >= 8 + 0.8y => 0.2y >= 1 => y = 5
        val insight = AttendanceCalculator.calculate(
            attended = 7,
            totalConducted = 10,
            targetPercentage = 80
        )
        assertEquals(5, insight.classesMustAttendToRecover)
    }
}
