package `in`.campusone.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import `in`.campusone.app.config.FeatureFlags
import `in`.campusone.app.domain.model.SemesterGradeSummary
import `in`.campusone.app.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ImportedStudyFile(
    val displayName: String,
    val localPath: String,
    val mimeType: String,
    val sizeBytes: Long
)

object StudyFileAndPdfHelper {

    /**
     * Copies a PDF or image chosen via the system Storage Access Framework picker
     * into app-private `filesDir/study_vault/` with a 10 MB size guard and downsampled
     * image compression if needed. No storage permissions are required on any Android version.
     */
    suspend fun importFromSafUri(context: Context, uri: Uri): Result<ImportedStudyFile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "application/pdf"
                var displayName = "Study_File_${System.currentTimeMillis()}"
                var sizeBytes = 0L

                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) {
                            displayName = cursor.getString(nameIdx) ?: displayName
                        }
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            sizeBytes = cursor.getLong(sizeIdx)
                        }
                    }
                }

                if (sizeBytes > FeatureFlags.MAX_IMPORT_FILE_BYTES) {
                    throw IllegalArgumentException("File exceeds 10 MB limit (${sizeBytes / (1024 * 1024)} MB).")
                }

                val vaultDir = File(context.filesDir, "study_vault").apply { mkdirs() }
                val ext = when {
                    mimeType.contains("pdf", ignoreCase = true) -> ".pdf"
                    mimeType.contains("png", ignoreCase = true) -> ".png"
                    mimeType.contains(" image", ignoreCase = true) || mimeType.startsWith("image/") -> ".jpg"
                    displayName.endsWith(".pdf", ignoreCase = true) -> ".pdf"
                    else -> ".bin"
                }
                val targetFile = File(vaultDir, "material_${UUID.randomUUID()}$ext")

                if (mimeType.startsWith("image/")) {
                    // Decode with inSampleSize = 2 for lightweight memory-safe storage
                    val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                    val bmp = resolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, opts)
                    }
                    if (bmp != null) {
                        FileOutputStream(targetFile).use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
                        }
                    } else {
                        resolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                        }
                    }
                } else {
                    resolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                    } ?: throw IllegalStateException("Unable to open selected file stream.")
                }

                ImportedStudyFile(
                    displayName = displayName,
                    localPath = targetFile.absolutePath,
                    mimeType = mimeType,
                    sizeBytes = targetFile.length()
                )
            }
        }

    /**
     * Renders a single page of a local PDF file into an Android [Bitmap] using native [PdfRenderer].
     * Returns total page count and the rendered bitmap safely wrapped in [Result].
     */
    suspend fun renderPdfPage(
        localFilePath: String,
        pageIndex: Int,
        renderWidthPx: Int = 1080
    ): Result<Pair<Bitmap, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(localFilePath)
            require(file.exists()) { "Local PDF file not found." }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val pageCount = renderer.pageCount.coerceAtLeast(1)
                    val safeIndex = pageIndex.coerceIn(0, pageCount - 1)
                    renderer.openPage(safeIndex).use { page ->
                        val ratio = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                        val width = renderWidthPx.coerceIn(480, 1600)
                        val height = (width * ratio).toInt().coerceAtLeast(480)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        Pair(bitmap, pageCount)
                    }
                }
            }
        }
    }

    /**
     * Generates a clean, printable PDF Grade Report using Android's native [PdfDocument] API
     * and writes it to `filesDir/gpa_reports/`.
     */
    suspend fun exportGpaReportPdf(
        context: Context,
        profile: UserProfile,
        summaries: List<SemesterGradeSummary>,
        cgpa: Double
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 76, 129)
                textSize = 18f
                isFakeBoldText = true
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 12f
            }

            var y = 48f
            canvas.drawText("CampusOne — Academic SGPA / CGPA Statement", 40f, y, titlePaint)
            y += 24f
            canvas.drawText("Student: ${profile.name.ifBlank { "Student" }} | ${profile.course} (Sem ${profile.semester})", 40f, y, bodyPaint)
            y += 18f
            canvas.drawText("University: ${profile.college} | Scale: ${profile.gradingScale.displayName}", 40f, y, bodyPaint)
            y += 24f
            canvas.drawText("Cumulative CGPA: ${String.format("%.2f", cgpa)}", 40f, y, titlePaint)
            y += 28f

            summaries.forEach { sem ->
                if (y > 760f) return@forEach
                canvas.drawText("Semester ${sem.semester} — SGPA: ${String.format("%.2f", sem.sgpa)} (${sem.totalCredits} Credits)", 40f, y, titlePaint)
                y += 18f
                sem.courses.forEach { c ->
                    if (y <= 790f) {
                        canvas.drawText(" • ${c.subjectName} (${c.credits} Cr): ${c.gradePointOrMarks}", 54f, y, bodyPaint)
                        y += 16f
                    }
                }
                y += 10f
            }

            doc.finishPage(page)
            val dir = File(context.filesDir, "gpa_reports").apply { mkdirs() }
            val outFile = File(dir, "CampusOne_GPA_Report_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outFile).use { doc.writeTo(it) }
            doc.close()
            outFile
        }
    }
}
