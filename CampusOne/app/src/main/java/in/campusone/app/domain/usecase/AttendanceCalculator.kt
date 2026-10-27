package `in`.campusone.app.domain.usecase

import `in`.campusone.app.domain.model.AttendanceAdvice
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Pure Kotlin Attendance Calculator (zero Android framework dependencies).
 * Computes current percentage, safe classes to miss ("bunks"), and classes required to recover.
 */
object AttendanceCalculator {

    fun evaluate(
        attended: Int,
        totalConducted: Int,
        targetPercentage: Int = 75
    ): AttendanceAdvice {
        val safeAttended = max(0, attended)
        val safeTotal = max(safeAttended, totalConducted)
        val clampedTarget = targetPercentage.coerceIn(1, 99)
        val targetRatio = clampedTarget / 100.0

        if (safeTotal == 0) {
            return AttendanceAdvice(
                attended = 0,
                totalConducted = 0,
                currentPercentage = 100.0,
                targetPercentage = clampedTarget,
                safeBunksAvailable = 0,
                classesMustAttendToRecover = 0,
                isAboveTarget = true
            )
        }

        val currentPct = (safeAttended.toDouble() * 100.0) / safeTotal.toDouble()
        val isAbove = currentPct + 1e-9 >= clampedTarget.toDouble()

        val safeBunks = if (isAbove) {
            // (attended) / (total + x) >= targetRatio  =>  x <= (attended / targetRatio) - total
            val raw = (safeAttended.toDouble() / targetRatio) - safeTotal.toDouble()
            max(0, floor(raw + 1e-9).toInt())
        } else {
            0
        }

        val mustAttend = if (!isAbove) {
            // (attended + y) / (total + y) >= targetRatio => y >= (targetRatio * total - attended) / (1 - targetRatio)
            val numerator = (targetRatio * safeTotal.toDouble()) - safeAttended.toDouble()
            val denominator = 1.0 - targetRatio
            max(1, ceil((numerator / denominator) - 1e-9).toInt())
        } else {
            0
        }

        return AttendanceAdvice(
            attended = safeAttended,
            totalConducted = safeTotal,
            currentPercentage = kotlin.math.round(currentPct * 10.0) / 10.0,
            targetPercentage = clampedTarget,
            safeBunksAvailable = safeBunks,
            classesMustAttendToRecover = mustAttend,
            isAboveTarget = isAbove
        )
    }
}
