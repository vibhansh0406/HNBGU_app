package `in`.campusone.app.domain.usecase

import kotlin.math.ceil
import kotlin.math.floor

enum class AttendanceHealth {
    SAFE,      // Above target with >= 1 class buffer
    ON_EDGE,   // At or above target, but missing 1 class drops below target
    WARNING,   // Within 5% below target
    CRITICAL,  // More than 5% below target
    NO_DATA    // 0 classes conducted so far
}

data class AttendanceInsight(
    val attended: Int,
    val totalConducted: Int,
    val cancelled: Int,
    val targetPercentage: Int,
    val currentPercentage: Double,
    val classesCanMissSafely: Int,
    val classesMustAttendToRecover: Int,
    val health: AttendanceHealth
)

/**
 * Pure, deterministic calculator for Indian college attendance rules (default 75%).
 *
 * Key rules:
 * - Cancelled classes do NOT count toward `totalConducted` or `attended`.
 * - If current % >= target %:
 *     We solve for maximum `x` (future missed classes) such that:
 *     attended / (totalConducted + x) >= target / 100
 *     => x <= (100 * attended - target * totalConducted) / target
 * - If current % < target %:
 *     We solve for minimum `y` (future consecutive attended classes) such that:
 *     (attended + y) / (totalConducted + y) >= target / 100
 *     => y >= (target * totalConducted - 100 * attended) / (100 - target)
 */
object AttendanceCalculator {

    fun calculate(
        attended: Int,
        totalConducted: Int,
        cancelled: Int = 0,
        targetPercentage: Int = 75
    ): AttendanceInsight {
        require(attended >= 0) { "Attended count cannot be negative" }
        require(totalConducted >= attended) { "Total conducted ($totalConducted) cannot be less than attended ($attended)" }
        require(targetPercentage in 1..100) { "Target percentage must be between 1 and 100" }

        if (totalConducted == 0) {
            return AttendanceInsight(
                attended = 0,
                totalConducted = 0,
                cancelled = cancelled,
                targetPercentage = targetPercentage,
                currentPercentage = 0.0,
                classesCanMissSafely = 0,
                classesMustAttendToRecover = 0,
                health = AttendanceHealth.NO_DATA
            )
        }

        val currentPct = (attended.toDouble() * 100.0) / totalConducted.toDouble()
        val targetDouble = targetPercentage.toDouble()

        return if (currentPct >= targetDouble) {
            val rawSkippable = (100.0 * attended - targetDouble * totalConducted) / targetDouble
            val canMiss = floor(rawSkippable + 1e-9).toInt().coerceAtLeast(0)
            val health = if (canMiss >= 1) AttendanceHealth.SAFE else AttendanceHealth.ON_EDGE
            AttendanceInsight(
                attended = attended,
                totalConducted = totalConducted,
                cancelled = cancelled,
                targetPercentage = targetPercentage,
                currentPercentage = currentPct,
                classesCanMissSafely = canMiss,
                classesMustAttendToRecover = 0,
                health = health
            )
        } else {
            val mustAttend = if (targetPercentage == 100) {
                Int.MAX_VALUE // Cannot recover to 100% once a class is missed
            } else {
                val rawNeeded = (targetDouble * totalConducted - 100.0 * attended) / (100.0 - targetDouble)
                ceil(rawNeeded - 1e-9).toInt().coerceAtLeast(1)
            }
            val deficit = targetDouble - currentPct
            val health = if (deficit <= 5.0) AttendanceHealth.WARNING else AttendanceHealth.CRITICAL
            AttendanceInsight(
                attended = attended,
                totalConducted = totalConducted,
                cancelled = cancelled,
                targetPercentage = targetPercentage,
                currentPercentage = currentPct,
                classesCanMissSafely = 0,
                classesMustAttendToRecover = mustAttend,
                health = health
            )
        }
    }
}
