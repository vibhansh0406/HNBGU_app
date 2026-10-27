package `in`.campusone.app.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Installs a global UncaughtExceptionHandler that persists the stack trace and device
 * diagnostics to app-private storage (`filesDir/crash_logs/last_crash.txt`) so the user
 * can inspect or copy any crash log directly from Settings -> Crash Log Inspector without
 * needing a laptop or ADB logcat.
 */
object CrashLogManager {
    private const val DIR_NAME = "crash_logs"
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashReport(appContext, thread.name, throwable)
            } catch (_: Throwable) {
                // Never throw inside the crash handler
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun writeCrashReport(context: Context, threadName: String, throwable: Throwable) {
        try {
            val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
            val file = File(dir, FILE_NAME)
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .withZone(ZoneId.systemDefault())
                .format(Instant.now())
            val report = buildString {
                appendLine("=== CampusOne On-Device Crash Report ===")
                appendLine("Timestamp : $timestamp")
                appendLine("Thread    : $threadName")
                appendLine("Device    : ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
                appendLine("Exception : ${throwable::class.java.name}: ${throwable.message}")
                appendLine("----------------------------------------")
                appendLine(sw.toString())
            }
            file.writeText(report, Charsets.UTF_8)
        } catch (_: Exception) {
        }
    }

    fun readLastCrashLog(context: Context): String? {
        return try {
            val file = File(File(context.filesDir, DIR_NAME), FILE_NAME)
            if (file.exists() && file.length() > 0L) file.readText(Charsets.UTF_8) else null
        } catch (_: Exception) {
            null
        }
    }

    fun clearCrashLog(context: Context) {
        try {
            val file = File(File(context.filesDir, DIR_NAME), FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }
}
