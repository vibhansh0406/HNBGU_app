package `in`.campusone.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private data class CampusRelease(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val checksumUrl: String
)

@Composable
fun CampusOneUpdatePrompt() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var release by remember { mutableStateOf<CampusRelease?>(null) }
    var checking by remember { mutableStateOf(true) }
    var downloading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        release = runCatching { fetchLatestCampusRelease() }.getOrNull()
            ?.takeIf { compareVersions(it.version, BuildConfig.VERSION_NAME) > 0 }
        checking = false
    }

    val available = release
    if (available != null) {
        AlertDialog(
            onDismissRequest = { if (!downloading) release = null },
            title = { Text("CampusOne update available") },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text("Version ${available.version} is ready to install.")
                    if (available.notes.isNotBlank()) {
                        Text(
                            available.notes.take(1200),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = androidx.compose.ui.Modifier.padding(top = androidx.compose.ui.unit.dp(8))
                        )
                    }
                    if (downloading) {
                        CircularProgressIndicator()
                        Text(message ?: "Downloading and verifying update…")
                    } else if (message != null) {
                        Text(message!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !downloading,
                    onClick = {
                        scope.launch {
                            downloading = true
                            message = "Preparing secure download…"
                            try {
                                downloadVerifyAndInstall(context, available) { message = it }
                            } catch (error: Exception) {
                                message = error.message ?: "Update could not be installed."
                            } finally {
                                downloading = false
                            }
                        }
                    }
                ) { Text(if (downloading) "Please wait" else "Update") }
            },
            dismissButton = {
                TextButton(onClick = { release = null }, enabled = !downloading) {
                    Text("Later")
                }
            }
        )
    } else if (message != null && !checking) {
        // Update failures are intentionally non-blocking; the app remains usable.
    }
}

private suspend fun fetchLatestCampusRelease(): CampusRelease? = withContext(Dispatchers.IO) {
    val connection = (URL("https://api.github.com/repos/vibhansh0406/HNBGU_app/releases/latest")
        .openConnection() as HttpURLConnection).apply {
        connectTimeout = 10000
        readTimeout = 15000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "CampusOne-Android-Updater")
    }
    try {
        if (connection.responseCode == 404) return@withContext null
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Update check failed (HTTP ${connection.responseCode}).")
        }
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val version = json.optString("tag_name").removePrefix("v")
        val assets = json.getJSONArray("assets")
        var apkUrl: String? = null
        var checksumUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val name = asset.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) apkUrl = asset.optString("browser_download_url")
            if (name.endsWith(".apk.sha256", ignoreCase = true)) checksumUrl = asset.optString("browser_download_url")
        }
        if (version.isBlank() || apkUrl.isNullOrBlank() || checksumUrl.isNullOrBlank()) {
            throw IllegalStateException("The latest release is missing its APK or checksum.")
        }
        CampusRelease(version, json.optString("body"), apkUrl, checksumUrl)
    } finally {
        connection.disconnect()
    }
}

private fun compareVersions(first: String, second: String): Int {
    val a = first.split('.').map { it.toIntOrNull() ?: 0 }
    val b = second.split('.').map { it.toIntOrNull() ?: 0 }
    for (index in 0 until maxOf(a.size, b.size)) {
        val difference = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
        if (difference != 0) return difference
    }
    return 0
}

private suspend fun downloadVerifyAndInstall(
    context: Context,
    release: CampusRelease,
    onProgress: (String) -> Unit
) = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        withContext(Dispatchers.Main) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            onProgress("Allow CampusOne to install updates, then tap Update again.")
        }
        return@withContext
    }

    val fileName = "CampusOne-${release.version}.apk"
    val request = DownloadManager.Request(Uri.parse(release.apkUrl))
        .setTitle("CampusOne update")
        .setDescription("Downloading version ${release.version}")
        .setMimeType("application/vnd.android.package-archive")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = manager.enqueue(request)
    var apkFile: File? = null
    repeat(180) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query)?.use { cursor ->
            if (cursor.moveToFirst()) {
                when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val path = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        apkFile = File(Uri.parse(path).path ?: "")
                        return@repeat
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        throw IllegalStateException("APK download failed (code $reason).")
                    }
                }
            }
        }
        if (apkFile == null) {
            onProgress("Downloading update…")
            delay(1000)
        }
    }
    val apk = apkFile ?: throw IllegalStateException("Download timed out. Please try again.")
    if (!apk.isFile || apk.length() == 0L) throw IllegalStateException("Downloaded APK is empty.")

    onProgress("Verifying APK integrity…")
    val expected = URL(release.checksumUrl).openStream().bufferedReader().use { it.readText() }
        .trim().substringBefore(Regex("\\s"))
    val digest = MessageDigest.getInstance("SHA-256")
    val actual = apk.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
    if (!actual.equals(expected, ignoreCase = true)) {
        apk.delete()
        throw IllegalStateException("APK checksum verification failed. Update cancelled.")
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    withContext(Dispatchers.Main) {
        onProgress("Verified. Opening Android installer…")
        context.startActivity(installIntent)
    }
}
