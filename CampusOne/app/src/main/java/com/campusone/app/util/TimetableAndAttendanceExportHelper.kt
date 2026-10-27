package com.campusone.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.campusone.app.domain.model.SubjectSummary
import com.campusone.app.domain.model.TimetableSlot
import com.campusone.app.domain.model.UserProfile
import com.campusone.app.domain.usecase.TimetableEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object TimetableAndAttendanceExportHelper {

    private val DAY_NAMES = mapOf(
        1 to "Monday",
        2 to "Tuesday",
        3 to "Wednesday",
        4 to "Thursday",
        5 to "Friday",
        6 to "Saturday"
    )

    suspend fun exportAsPdf(
        context: Context,
        profile: UserProfile,
        slots: List<TimetableSlot>,
        subjects: List<SubjectSummary>
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(842, 1190, 1).create()
            val page = doc.startPage(pageInfo)
            drawScheduleAndAttendanceSheet(
                canvas = page.canvas,
                width = 842f,
                height = 1190f,
                profile = profile,
                slots = slots,
                subjects = subjects
            )
            doc.finishPage(page)

            val outDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
            val outFile = File(outDir, "CampusOne_Timetable_Attendance_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outFile).use { doc.writeTo(it) }
            doc.close()
            outFile
        }
    }

    suspend fun exportAsPng(
        context: Context,
        profile: UserProfile,
        slots: List<TimetableSlot>,
        subjects: List<SubjectSummary>
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val width = 1080
            val height = 1520
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawScheduleAndAttendanceSheet(
                canvas = canvas,
                width = width.toFloat(),
                height = height.toFloat(),
                profile = profile,
                slots = slots,
                subjects = subjects
            )

            val outDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
            val outFile = File(outDir, "CampusOne_Timetable_Attendance_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            outFile
        }
    }

    private fun drawScheduleAndAttendanceSheet(
        canvas: Canvas,
        width: Float,
        height: Float,
        profile: UserProfile,
        slots: List<TimetableSlot>,
        subjects: List<SubjectSummary>
    ) {
        val scale = width / 842f
        canvas.drawColor(Color.rgb(248, 250, 252))

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 76, 129)
        }
        canvas.drawRect(0f, 0f, width, 110f * scale, headerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f * scale
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(212, 228, 255)
            textSize = 14f * scale
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 76, 129)
            textSize = 18f * scale
            isFakeBoldText = true
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 13f * scale
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 12f * scale
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
        }

        val studentName = profile.name.ifBlank { "Student" }
        canvas.drawText(
            "CampusOne — Weekly Timetable & Attendance Summary",
            36f * scale,
            48f * scale,
            titlePaint
        )
        canvas.drawText(
            "$studentName • ${profile.course} (Sem ${profile.semester}, Sec ${profile.section}) • ${profile.college}",
            36f * scale,
            78f * scale,
            subtitlePaint
        )

        var y = 142f * scale
        canvas.drawText("1. Weekly Class Schedule", 36f * scale, y, sectionPaint)
        y += 18f * scale

        for (day in 1..6) {
            val daySlots = TimetableEngine.slotsForDay(slots, day)
            val cardRect = RectF(36f * scale, y, width - 36f * scale, y + 66f * scale)
            canvas.drawRoundRect(cardRect, 10f * scale, 10f * scale, cardPaint)
            canvas.drawRoundRect(cardRect, 10f * scale, 10f * scale, borderPaint)

            val dayTitle = DAY_NAMES[day] ?: "Day $day"
            canvas.drawText(dayTitle, 50f * scale, y + 26f * scale, bodyBoldPaint)

            val summaryLine = if (daySlots.isEmpty()) {
                "No scheduled classes"
            } else {
                daySlots.joinToString("   |   ") { s ->
                    "${s.startTime}-${s.endTime} ${s.subjectCode} (${s.room})"
                }
            }
            canvas.drawText(summaryLine.take(95), 50f * scale, y + 48f * scale, bodyPaint)
            y += 76f * scale
        }

        y += 18f * scale
        canvas.drawText("2. Attendance Summary & Safe-Bunk Status", 36f * scale, y, sectionPaint)
        y += 18f * scale

        if (subjects.isEmpty()) {
            canvas.drawText("No subjects recorded yet.", 44f * scale, y + 24f * scale, bodyPaint)
        } else {
            subjects.take(8).forEach { sub ->
                if (y + 54f * scale > height - 24f * scale) return@forEach
                val rowRect = RectF(36f * scale, y, width - 36f * scale, y + 48f * scale)
                canvas.drawRoundRect(rowRect, 8f * scale, 8f * scale, cardPaint)
                canvas.drawRoundRect(rowRect, 8f * scale, 8f * scale, borderPaint)

                val pctStr = String.format("%.1f%%", sub.advice.currentPercentage)
                val statusStr = if (sub.advice.isAboveTarget) {
                    "Safe Bunks: ${sub.advice.safeBunksAvailable} (Target ${sub.targetPercentage}%)"
                } else {
                    "Attend Next: ${sub.advice.classesMustAttendToRecover} (Target ${sub.targetPercentage}%)"
                }
                canvas.drawText(
                    "${sub.code} • ${sub.name} (${sub.attendedCount}/${sub.totalClassesCount})",
                    50f * scale,
                    y + 22f * scale,
                    bodyBoldPaint
                )
                canvas.drawText(
                    "$pctStr  —  $statusStr",
                    50f * scale,
                    y + 40f * scale,
                    bodyPaint
                )
                y += 56f * scale
            }
        }
    }
}
