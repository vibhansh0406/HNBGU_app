package com.campusone.app.util

import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import java.io.File

object CampusIntentHelper {

    fun dialPhone(context: Context, phone: String) {
        try {
            val clean = phone.trim()
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun sendEmail(context: Context, email: String, subject: String = "Query via CampusOne") {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${email.trim()}")).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun copyToClipboard(context: Context, label: String, value: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText(label, value))
        } catch (_: Exception) {
        }
    }

    fun openInMaps(context: Context, lat: Double, lng: Double, label: String) {
        try {
            val encodedLabel = Uri.encode(label)
            val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } catch (_: Exception) {
        }
    }

    fun addExamOrEventToCalendar(
        context: Context,
        title: String,
        location: String,
        description: String,
        startEpochMillis: Long
    ) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startEpochMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startEpochMillis + 7_200_000L)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun shareFileViaProvider(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, file.name).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }

    fun canScheduleExactAlarmsSafely(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                am?.canScheduleExactAlarms() ?: false
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}

