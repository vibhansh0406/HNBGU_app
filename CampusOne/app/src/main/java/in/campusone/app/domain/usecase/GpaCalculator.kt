package `in`.campusone.app.domain.usecase

import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.roundToInt

@Serializable
enum class GradingScaleType(val label: String, val maxScalePoint: Double) {
    TEN_POINT_INDIAN("10-Point UGC / NEP (India)", 10.0),
    FOUR_POINT_WES("4.0-Point Scale", 4.0),
    DIRECT_PERCENTAGE("Percentage Marks (100%)", 100.0)
}

@Serializable
data class CourseGradeInput(
    val id: String = "",
    val subjectCode: String,
    val subjectName: String,
    val credits: Int,
    val gradePoint: Double, // 0..10 on 10-pt, 0..4 on 4-pt, or 0..100 on Percentage scale
    val marksObtained: Double? = null
)

@Serializable
data class SemesterGpaRecord(
    val semesterNumber: Int,
    val totalCredits: Int,
    val sgpa: Double,
    val courses: List<CourseGradeInput> = emptyList(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class TargetCgpaRequirement(
    val targetCgpa: Double,
    val upcomingCredits: Int,
    val requiredSgpa: Double,
    val isAchievable: Boolean,
    val alreadyAchieved: Boolean,
    val maxScalePoint: Double
)

data class GpaCalculationResult(
    val scaleType: GradingScaleType,
    val sgpa: Double,
    val cgpa: Double,
    val equivalentPercentage: Double,
    val totalCredits: Int,
    val semesterTrend: List<SemesterGpaRecord>,
    val formattedSgpa: String,
    val formattedCgpa: String,
    val formattedPercentage: String
)

/**
 * Comprehensive Phase 2 GPA / CGPA / Percentage Engine:
 * - Supports 10-Point Indian CBCS/NEP, 4.0-Point scale, and Direct Percentage.
 * - Computes weighted SGPA, multi-semester CGPA, and semester trend series.
 * - Includes "What do I need?" calculator for target CGPA in the coming semester.
 */
object GpaCalculator {

    val standardGradeScale: Map<String, Int> = linkedMapOf(
        "O (10)" to 10,
        "A+ (9)" to 9,
        "A (8)" to 8,
        "B+ (7)" to 7,
        "B (6)" to 6,
        "C (5)" to 5,
        "P (4)" to 4,
        "F (0)" to 0
    )

    fun convertMarksToTenPointGrade(marksOutOf100: Double): Double = when {
        marksOutOf100 >= 90.0 -> 10.0
        marksOutOf100 >= 80.0 -> 9.0
        marksOutOf100 >= 70.0 -> 8.0
        marksOutOf100 >= 60.0 -> 7.0
        marksOutOf100 >= 50.0 -> 6.0
        marksOutOf100 >= 45.0 -> 5.0
        marksOutOf100 >= 40.0 -> 4.0
        else -> 0.0
    }

    fun calculateSgpaAndCgpa(
        currentSemesterNumber: Int = 3,
        currentSemesterCourses: List<CourseGradeInput>,
        pastSemesters: List<SemesterGpaRecord> = emptyList(),
        scaleType: GradingScaleType = GradingScaleType.TEN_POINT_INDIAN,
        percentageMultiplier: Double = 9.5
    ): GpaCalculationResult {
        val maxPt = scaleType.maxScalePoint
        val currentCredits = currentSemesterCourses.sumOf { it.credits.coerceAtLeast(0) }
        val currentWeightedPoints = currentSemesterCourses.sumOf { course ->
            val effectivePoint = course.gradePoint.coerceIn(0.0, maxPt)
            course.credits.coerceAtLeast(0) * effectivePoint
        }

        val rawSgpa = if (currentCredits > 0) {
            currentWeightedPoints / currentCredits.toDouble()
        } else {
            0.0
        }

        val otherSemesters = pastSemesters.filter { it.semesterNumber != currentSemesterNumber }
        val pastCredits = otherSemesters.sumOf { it.totalCredits.coerceAtLeast(0) }
        val pastWeightedPoints = otherSemesters.sumOf { it.totalCredits.coerceAtLeast(0) * it.sgpa }

        val combinedCredits = currentCredits + pastCredits
        val rawCgpa = if (combinedCredits > 0) {
            (currentWeightedPoints + pastWeightedPoints) / combinedCredits.toDouble()
        } else {
            rawSgpa
        }

        val percentage = when (scaleType) {
            GradingScaleType.TEN_POINT_INDIAN -> (rawCgpa * percentageMultiplier).coerceIn(0.0, 100.0)
            GradingScaleType.FOUR_POINT_WES -> ((rawCgpa / 4.0) * 100.0).coerceIn(0.0, 100.0)
            GradingScaleType.DIRECT_PERCENTAGE -> rawCgpa.coerceIn(0.0, 100.0)
        }

        val currentSemRecord = SemesterGpaRecord(
            semesterNumber = currentSemesterNumber,
            totalCredits = currentCredits,
            sgpa = rawSgpa,
            courses = currentSemesterCourses
        )
        val trend = (otherSemesters + currentSemRecord).sortedBy { it.semesterNumber }

        return GpaCalculationResult(
            scaleType = scaleType,
            sgpa = rawSgpa,
            cgpa = rawCgpa,
            equivalentPercentage = percentage,
            totalCredits = combinedCredits,
            semesterTrend = trend,
            formattedSgpa = String.format(Locale.US, "%.2f", rawSgpa),
            formattedCgpa = String.format(Locale.US, "%.2f", rawCgpa),
            formattedPercentage = String.format(Locale.US, "%.1f%%", percentage)
        )
    }

    /**
     * "What do I need?" Mode:
     * Given completed credits `C_done`, current CGPA `G_curr`, and upcoming semester credits `C_next`,
     * computes required SGPA `S_req` to achieve `G_target`:
     *   S_req = (G_target * (C_done + C_next) - G_curr * C_done) / C_next
     */
    fun calculateRequiredSgpaForTargetCgpa(
        completedCredits: Int,
        currentCgpa: Double,
        upcomingSemesterCredits: Int,
        targetCgpa: Double,
        scaleType: GradingScaleType = GradingScaleType.TEN_POINT_INDIAN
    ): TargetCgpaRequirement {
        val maxScale = scaleType.maxScalePoint
        if (upcomingSemesterCredits <= 0) {
            return TargetCgpaRequirement(
                targetCgpa = targetCgpa,
                upcomingCredits = 0,
                requiredSgpa = 0.0,
                isAchievable = currentCgpa >= targetCgpa,
                alreadyAchieved = currentCgpa >= targetCgpa,
                maxScalePoint = maxScale
            )
        }

        val totalFutureCredits = completedCredits.coerceAtLeast(0) + upcomingSemesterCredits
        val totalPointsNeeded = targetCgpa * totalFutureCredits
        val currentPointsEarned = currentCgpa * completedCredits.coerceAtLeast(0)
        val rawRequired = (totalPointsNeeded - currentPointsEarned) / upcomingSemesterCredits.toDouble()

        val clampedRequired = rawRequired.coerceAtLeast(0.0)
        val achievable = rawRequired <= maxScale + 1e-6
        val alreadyDone = rawRequired <= 0.0

        return TargetCgpaRequirement(
            targetCgpa = targetCgpa,
            upcomingCredits = upcomingSemesterCredits,
            requiredSgpa = (clampedRequired * 100.0).roundToInt() / 100.0,
            isAchievable = achievable,
            alreadyAchieved = alreadyDone,
            maxScalePoint = maxScale
        )
    }
}
