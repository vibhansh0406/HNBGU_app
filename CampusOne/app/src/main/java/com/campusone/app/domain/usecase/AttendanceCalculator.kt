package com.campusone.app.domain.usecase

import com.campusone.app.domain.model.AttendanceAdvice
import com.campusone.app.domain.model.AttendanceRecordItem
import com.campusone.app.domain.model.AttendanceStatus
import com.campusone.app.domain.model.AttendanceTrendPoint
import com.campusone.app.domain.model.MonthlyHeatmapDay
import com.campusone.app.domain.model.SafeBunkProjection
import com.campusone.app.domain.model.SubjectSummary
import com.campusone.app.domain.model.TimetableSlot
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

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
                isAboveTarget = true,
                isWarningZone = false
            )
        }

        val currentPct = (safeAttended.toDouble() * 100.0) / safeTotal.toDouble()
        val isAbove = currentPct + 1e-9 >= clampedTarget.toDouble()

        val safeBunks = if (isAbove) {
            val raw = (safeAttended.toDouble() / targetRatio) - safeTotal.toDouble()
            max(0, floor(raw + 1e-9).toInt())
        } else {
            0
        }

        val mustAttend = if (!isAbove) {
            val numerator = (targetRatio * safeTotal.toDouble()) - safeAttended.toDouble()
            val denominator = 1.0 - targetRatio
            max(1, ceil((numerator / denominator) - 1e-9).toInt())
        } else {
            0
        }

        val isWarning = !isAbove || safeBunks <= 2

        return AttendanceAdvice(
            attended = safeAttended,
            totalConducted = safeTotal,
            currentPercentage = round(currentPct * 10.0) / 10.0,
            targetPercentage = clampedTarget,
            safeBunksAvailable = safeBunks,
            classesMustAttendToRecover = mustAttend,
            isAboveTarget = isAbove,
            isWarningZone = isWarning
        )
    }

    fun calculateWhatIfSkipPercentage(
        attended: Int,
        totalConducted: Int,
        skipNextN: Int
    ): Double {
        val safeAttended = max(0, attended)
        val safeTotal = max(safeAttended, totalConducted)
        val safeSkip = max(0, skipNextN)
        val denominator = safeTotal + safeSkip
        if (denominator <= 0) return 100.0
        val pct = (safeAttended.toDouble() * 100.0) / denominator.toDouble()
        return round(pct * 10.0) / 10.0
    }

    fun projectSubjectWithTimetable(
        subject: SubjectSummary,
        timetableSlots: List<TimetableSlot>,
        fromDate: LocalDate,
        endDate: LocalDate,
        whatIfSkipCount: Int
    ): SafeBunkProjection {
        val clampedTarget = subject.targetPercentage.coerceIn(1, 99)
        val targetRatio = clampedTarget / 100.0
        val adviceNow = evaluate(subject.attendedCount, subject.totalClassesCount, clampedTarget)
        val remainingScheduled = TimetableEngine.countRemainingOccurrencesUntil(
            allSlots = timetableSlots,
            subjectId = subject.id,
            subjectCode = subject.code,
            subjectName = subject.name,
            fromDate = fromDate,
            toDate = endDate
        )

        val totalBySemesterEnd = subject.totalClassesCount + remainingScheduled
        val minRequiredAttendedByEnd = ceil((totalBySemesterEnd.toDouble() * targetRatio) - 1e-9).toInt()
        val projectedMustAttend = (minRequiredAttendedByEnd - subject.attendedCount)
            .coerceIn(0, remainingScheduled)
        val projectedSafeBunks = (remainingScheduled - projectedMustAttend).coerceAtLeast(0)

        val safeSkip = whatIfSkipCount.coerceIn(0, max(20, remainingScheduled))
        val whatIfPct = calculateWhatIfSkipPercentage(
            attended = subject.attendedCount,
            totalConducted = subject.totalClassesCount,
            skipNextN = safeSkip
        )

        return SafeBunkProjection(
            subjectId = subject.id,
            subjectCode = subject.code,
            subjectName = subject.name,
            attended = subject.attendedCount,
            totalConducted = subject.totalClassesCount,
            targetPercentage = clampedTarget,
            currentPercentage = adviceNow.currentPercentage,
            safeBunksNow = adviceNow.safeBunksAvailable,
            mustAttendToRecoverNow = adviceNow.classesMustAttendToRecover,
            remainingScheduledClasses = remainingScheduled,
            projectedSafeBunksUntilEnd = projectedSafeBunks,
            projectedMustAttendUntilEnd = projectedMustAttend,
            whatIfSkipCount = safeSkip,
            whatIfPercentage = whatIfPct,
            isWhatIfAboveTarget = whatIfPct + 1e-9 >= clampedTarget.toDouble()
        )
    }

    fun shouldTriggerLowAttendanceAlert(
        attended: Int,
        totalConducted: Int,
        thresholdPercent: Int
    ): Boolean {
        if (totalConducted <= 0) return false
        val advice = evaluate(
            attended = attended,
            totalConducted = totalConducted,
            targetPercentage = thresholdPercent.coerceIn(1, 99)
        )
        return !advice.isAboveTarget || advice.safeBunksAvailable <= 2
    }

    fun buildMonthlyHeatmap(
        records: List<AttendanceRecordItem>,
        yearMonth: YearMonth,
        subjectIdFilter: String? = null
    ): List<MonthlyHeatmapDay> {
        val filtered = if (subjectIdFilter.isNullOrBlank()) {
            records
        } else {
            records.filter { it.subjectId == subjectIdFilter }
        }
        val byDay = filtered.groupBy { it.epochDay }
        val daysInMonth = yearMonth.lengthOfMonth()
        return (1..daysInMonth).map { dayOfMonth ->
            val date = yearMonth.atDay(dayOfMonth)
            val epochDay = date.toEpochDay()
            val dayRecords = byDay[epochDay].orEmpty()
            MonthlyHeatmapDay(
                epochDay = epochDay,
                dayOfMonth = dayOfMonth,
                presentCount = dayRecords.count { it.status == AttendanceStatus.PRESENT },
                absentCount = dayRecords.count { it.status == AttendanceStatus.ABSENT },
                cancelledCount = dayRecords.count { it.status == AttendanceStatus.CANCELLED }
            )
        }
    }

    fun buildAttendanceTrend(
        records: List<AttendanceRecordItem>,
        subjectIdFilter: String? = null
    ): List<AttendanceTrendPoint> {
        val filtered = (if (subjectIdFilter.isNullOrBlank()) {
            records
        } else {
            records.filter { it.subjectId == subjectIdFilter }
        }).filter { it.status != AttendanceStatus.CANCELLED }
            .sortedBy { it.epochDay }

        if (filtered.isEmpty()) return emptyList()

        val groupedByDay = filtered.groupBy { it.epochDay }.toSortedMap()
        var cumulativeAttended = 0
        var cumulativeTotal = 0
        val points = mutableListOf<AttendanceTrendPoint>()

        for ((epochDay, dayRecords) in groupedByDay) {
            cumulativeAttended += dayRecords.count { it.status == AttendanceStatus.PRESENT }
            cumulativeTotal += dayRecords.size
            val pct = if (cumulativeTotal > 0) {
                round(((cumulativeAttended.toDouble() * 100.0) / cumulativeTotal.toDouble()) * 10.0) / 10.0
            } else {
                100.0
            }
            val label = dayRecords.firstOrNull()?.dateLabel ?: LocalDate.ofEpochDay(epochDay).toString()
            points.add(
                AttendanceTrendPoint(
                    epochDay = epochDay,
                    dateLabel = label,
                    cumulativePercentage = pct
                )
            )
        }
        return points.takeLast(14)
    }
}
