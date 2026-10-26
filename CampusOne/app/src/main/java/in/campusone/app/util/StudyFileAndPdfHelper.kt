package `in`.campusone.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.usecase.GpaCalculationResult
import java.io.File
import java.io.FileOutputStream

object StudyFileAndPdfHelper {
    private const val MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L

    fun validateFileSize(sizeBytes: Long): Result<Unit> = runCatching {
        require(sizeBytes in 1..MAX_FILE_SIZE_BYTES) {
            "Selected files must be between 1 byte and 10 MB."
        }
    }

    fun exportGpaReportToPdf(
        context: Context,
        profile: UserProfile,
        result: GpaCalculationResult
    ): Result<File> = runCatching {
        val outputDirectory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "documents")
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory) {
            "Could not create the local documents folder."
        }

        val outputFile = File(outputDirectory, "campusone_grade_report_${System.currentTimeMillis()}.pdf")
        val document = PdfDocument()
        try {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(595, 842, 1).create()
            )
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 16f
                isAntiAlias = true
            }
            var y = 56f
            page.canvas.drawText("CampusOne Local Grade Report", 40f, y, paint)
            paint.textSize = 12f
            y += 32f
            page.canvas.drawText("Student: ${profile.name}", 40f, y, paint)
            y += 22f
            page.canvas.drawText("${profile.courseName} • Semester ${profile.semester}", 40f, y, paint)
            y += 30f
            page.canvas.drawText("SGPA: ${result.formattedSgpa}   CGPA: ${result.formattedCgpa}", 40f, y, paint)
            y += 22f
            page.canvas.drawText("Percentage: ${result.formattedPercentage}", 40f, y, paint)
            y += 32f
            result.semesterTrend.lastOrNull()?.courses.orEmpty().forEach { course ->
                if (y > 790f) return@forEach
                page.canvas.drawText(
                    "${course.subjectCode} — ${course.subjectName}: ${course.gradePoint} (${course.credits} credits)",
                    40f,
                    y,
                    paint
                )
                y += 22f
            }
            document.finishPage(page)
            FileOutputStream(outputFile).use(document::writeTo)
        } catch (error: Exception) {
            outputFile.delete()
            throw error
        } finally {
            document.close()
        }
        outputFile
    }
}
