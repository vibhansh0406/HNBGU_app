package `in`.campusone.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import `in`.campusone.app.MainActivity
import `in`.campusone.app.R
import `in`.campusone.app.data.remote.AuthManager
import `in`.campusone.app.data.remote.SupabaseSyncRepository
import javax.inject.Inject

/** Notification channel IDs for CampusOne notification categories. */
object NotificationChannels {
    const val ANNOUNCEMENTS = "campusone_announcements"
    const val NOTICES = "campusone_notices"
    const val EXAMS = "campusone_exams"
    const val ASSIGNMENTS = "campusone_assignments"
    const val STUDY_MATERIAL = "campusone_study_material"
    const val ADMIN_ALERTS = "campusone_admin_alerts"

    const val ANNOUNCEMENTS_NAME = "College Announcements"
    const val NOTICES_NAME = "Section Notices"
    const val EXAMS_NAME = "Exam Updates"
    const val ASSIGNMENTS_NAME = "Assignments"
    const val STUDY_MATERIAL_NAME = "Study Material"
    const val ADMIN_ALERTS_NAME = "Administrative Alerts"
}

/** Notification categories mapped to channel IDs. */
object NotificationCategory {
    const val ANNOUNCEMENT = "announcement"
    const val NOTICE = "notice"
    const val EXAM = "exam"
    const val ASSIGNMENT = "assignment"
    const val STUDY_MATERIAL = "study_material"
    const val ADMIN_ALERT = "admin_alert"
}

/**
 * Firebase Messaging Service for CampusOne.
 *
 * Handles FCM token generation, refresh, and incoming message processing.
 * FCM tokens are registered with Supabase via the device_tokens table.
 * Notification payloads are displayed as local notifications with deep-link support.
 */
@AndroidEntryPoint
class CampusFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var supabaseSync: SupabaseSyncRepository

    private val notificationManagerCompat by lazy {
        NotificationManagerCompat.from(this)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /** Creates all notification channels required by CampusOne. */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                NotificationChannels.ANNOUNCEMENTS,
                NotificationChannels.ANNOUNCEMENTS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "University-wide announcements from the admin"
            },
            NotificationChannel(
                NotificationChannels.NOTICES,
                NotificationChannels.NOTICES_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Section-level notices from CRs and faculty"
            },
            NotificationChannel(
                NotificationChannels.EXAMS,
                NotificationChannels.EXAMS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Exam schedule and update notifications"
            },
            NotificationChannel(
                NotificationChannels.ASSIGNMENTS,
                NotificationChannels.ASSIGNMENTS_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Assignment and deadline reminders"
            },
            NotificationChannel(
                NotificationChannels.STUDY_MATERIAL,
                NotificationChannels.STUDY_MATERIAL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "New study material and resource uploads"
            },
            NotificationChannel(
                NotificationChannels.ADMIN_ALERTS,
                NotificationChannels.ADMIN_ALERTS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical administrative alerts"
            }
        )
        manager.createNotificationChannels(channels)
    }

    /** Called when FCM registration token is generated or refreshed. */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        lifecycleScope.launch {
            registerFcmToken(token)
        }
    }

    /** Registers the FCM token with Supabase device_tokens table. */
    private suspend fun registerFcmToken(token: String) {
        val uid = authManager.currentUserId() ?: return
        val email = authManager.currentUserEmail() ?: ""
        val deviceName = android.os.Build.MODEL

        val result = supabaseSync.registerDeviceToken(
            uid = uid,
            fcmToken = token,
            devicePlatform = "android",
            deviceName = deviceName,
            email = email
        )

        if (!result) {
            // Token registration failed; will be retried on next token refresh
            // or when the app next signs in.
        }
    }

    /** Handles incoming FCM messages. */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload for notification display
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val title = data["title"] ?: data["body"] ?: "CampusOne"
            val body = data["body"] ?: "New notification"
            val category = data["category"] ?: NotificationCategory.NOTICE
            val targetType = data["target_type"] ?: ""
            val targetId = data["target_id"] ?: ""

            showNotification(
                title = title,
                body = body,
                category = category,
                targetType = targetType,
                targetId = targetId
            )
        }

        // Handle notification payload (received when app is in background)
        remoteMessage.notification?.let { notification ->
            val category = remoteMessage.data["category"] ?: NotificationCategory.NOTICE
            val targetType = remoteMessage.data["target_type"] ?: ""
            val targetId = remoteMessage.data["target_id"] ?: ""

            showNotification(
                title = notification.title ?: "CampusOne",
                body = notification.body ?: "New notification",
                category = category,
                targetType = targetType,
                targetId = targetId
            )
        }
    }

    /** Displays a local notification with deep-link support. */
    private fun showNotification(
        title: String,
        body: String,
        category: String,
        targetType: String,
        targetId: String
    ) {
        val channelId = when (category) {
            NotificationCategory.ANNOUNCEMENT -> NotificationChannels.ANNOUNCEMENTS
            NotificationCategory.NOTICE -> NotificationChannels.NOTICES
            NotificationCategory.EXAM -> NotificationChannels.EXAMS
            NotificationCategory.ASSIGNMENT -> NotificationChannels.ASSIGNMENTS
            NotificationCategory.STUDY_MATERIAL -> NotificationChannels.STUDY_MATERIAL
            NotificationCategory.ADMIN_ALERT -> NotificationChannels.ADMIN_ALERTS
            else -> NotificationChannels.NOTICES
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            action = "in.campusone.app.NOTIFICATION_CLICK"
            putExtra("target_type", targetType)
            putExtra("target_id", targetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(when (category) {
                NotificationCategory.ANNOUNCEMENT, NotificationCategory.ADMIN_ALERT,
                NotificationCategory.EXAM -> NotificationCompat.PRIORITY_HIGH
                else -> NotificationCompat.PRIORITY_DEFAULT
            })
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(when (category) {
                NotificationCategory.ANNOUNCEMENT -> NotificationCompat.CATEGORY_EVENT
                NotificationCategory.NOTICE -> NotificationCompat.CATEGORY_MESSAGE
                NotificationCategory.EXAM -> NotificationCompat.CATEGORY_EVENT
                NotificationCategory.ASSIGNMENT -> NotificationCompat.CATEGORY_REMINDER
                NotificationCategory.STUDY_MATERIAL -> NotificationCompat.CATEGORY_RECOMMENDATION
                NotificationCategory.ADMIN_ALERT -> NotificationCompat.CATEGORY_ALARM
                else -> NotificationCompat.CATEGORY_MESSAGE
            })
            .build()

        notificationManagerCompat.notify(
            (System.currentTimeMillis() / 1000).toInt(),
            notification
        )
    }
}
