package com.campusone.app.domain.usecase

import com.campusone.app.domain.model.CourseGradeEntry
import com.campusone.app.domain.model.GradingScale
import com.campusone.app.domain.model.SemesterGradeSummary
import kotlin.math.round

object GpaCalculator {

    fun normalizeValueToScale(raw: Double, scale: GradingScale): Double {
        return raw.coerceIn(0.0, scale.maxPoints)
    }

    fun calculateSgpa(entries: List<CourseGradeEntry>, scale: GradingScale): Double {
        val valid = entries.filter { it.credits > 0 }
        val totalCredits = valid.sumOf { it.credits }
        if (totalCredits <= 0) return 0.0
        val weightedSum = valid.sumOf { entry ->
            entry.credits.toDouble() * normalizeValueToScale(entry.gradePointOrMarks, scale)
        }
        return round((weightedSum / totalCredits.toDouble()) * 100.0) / 100.0
    }

    fun summarizeBySemester(
        allEntries: List<CourseGradeEntry>,
        scale: GradingScale
    ): List<SemesterGradeSummary> {
        return allEntries
            .groupBy { it.semester }
            .toSortedMap()
            .map { (sem, list) ->
                SemesterGradeSummary(
                    semester = sem,
                    totalCredits = list.filter { it.credits > 0 }.sumOf { it.credits },
                    sgpa = calculateSgpa(list, scale),
                    courses = list
                )
            }
    }

    fun calculateCgpa(summaries: List<SemesterGradeSummary>): Double {
        val valid = summaries.filter { it.totalCredits > 0 }
        val totalCredits = valid.sumOf { it.totalCredits }
        if (totalCredits <= 0) return 0.0
        val weighted = valid.sumOf { it.sgpa * it.totalCredits.toDouble() }
        return round((weighted / totalCredits.toDouble()) * 100.0) / 100.0
    }

    fun requiredSgpaForTargetCgpa(
        currentCgpa: Double,
        completedCredits: Int,
        nextSemesterCredits: Int,
        targetCgpa: Double
    ): Double {
        if (nextSemesterCredits <= 0) return 0.0
        val totalFutureCredits = completedCredits + nextSemesterCredits
        val neededQualityPoints = (targetCgpa * totalFutureCredits.toDouble()) -
            (currentCgpa * completedCredits.toDouble())
        val rawRequired = neededQualityPoints / nextSemesterCredits.toDouble()
        return round(rawRequired * 100.0) / 100.0
    }
}

