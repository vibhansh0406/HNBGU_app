package `in`.campusone.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface FileStore {
    suspend fun upload(
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: suspend (Int) -> Unit
    ): Result<String>

    suspend fun download(url: String): Result<ByteArray>
}

object StorageBuckets {
    const val STUDY_RESOURCES = "study-resources"
}

@Singleton
class SupabaseFileStore @Inject constructor(
    private val config: SupabaseConfig,
    private val sessionStore: SessionStore
) : FileStore {

    private val storageBase = config.url.trimEnd('/') + "/storage/v1"
    private val workerBase = config.storageApiUrl.trimEnd('/')

    private fun bearer(): String =
        sessionStore.session.value?.accessToken?.takeIf { it.isNotBlank() } ?: config.anonKey

    override suspend fun upload(
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: suspend (Int) -> Unit
    ): Result<String> {
        if (workerBase.isNotBlank()) {
            return uploadThroughWorker(bucket, path, bytes, contentType, onProgress)
        }
        return uploadToLegacySupabaseStorage(bucket, path, bytes, contentType, onProgress)
    }

    private suspend fun uploadThroughWorker(
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: suspend (Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (bytes.isEmpty()) throw IllegalStateException("Nothing to upload: file is empty")
            val accessToken = sessionStore.session.value?.accessToken?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Sign in before uploading")
            val boundary = "CampusOne" + UUID.randomUUID().toString().replace("-", "")
            val filename = path.substringAfterLast('/').ifBlank { "upload.bin" }
                .replace("\"", "_").replace("\r", "").replace("\n", "")
            val prefix = buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Disposition: form-data; name=\"bucket\"\r\n\r\n")
                append(bucket).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Disposition: form-data; name=\"path\"\r\n\r\n")
                append(path).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(filename).append("\"\r\n")
                append("Content-Type: ").append(contentType).append("\r\n\r\n")
            }.toByteArray(Charsets.UTF_8)
            val suffix = "\r\n--".plus(boundary).plus("--\r\n").toByteArray(Charsets.UTF_8)
            val totalLength = prefix.size.toLong() + bytes.size.toLong() + suffix.size.toLong()

            val conn = URL("$workerBase/v1/objects").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 120_000
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.setFixedLengthStreamingMode(totalLength)

            try {
                conn.outputStream.use { out ->
                    out.write(prefix)
                    val chunk = 64 * 1024
                    var offset = 0
                    while (offset < bytes.size) {
                        val len = minOf(chunk, bytes.size - offset)
                        out.write(bytes, offset, len)
                        offset += len
                        onProgress((offset * 100L / bytes.size).toInt())
                    }
                    out.write(suffix)
                    out.flush()
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                if (code !in 200..299) {
                    throw IllegalStateException("Storage Worker upload failed ($code): ${body.take(200)}")
                }
                val id = JSONObject(body).getString("id")
                if (id.isBlank()) throw IllegalStateException("Storage Worker returned no object ID")
                "$workerBase/v1/objects/$id/download"
            } finally {
                conn.disconnect()
            }
        }
    }

    private suspend fun uploadToLegacySupabaseStorage(
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: suspend (Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (bytes.isEmpty()) throw IllegalStateException("Nothing to upload: file is empty")
            val conn = URL("$storageBase/object/$bucket/$path").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("apikey", config.anonKey)
            conn.setRequestProperty("Authorization", "Bearer ${bearer()}")
            conn.setRequestProperty("Content-Type", contentType)
            conn.setRequestProperty("x-upsert", "true")
            conn.setFixedLengthStreamingMode(bytes.size)
            try {
                conn.outputStream.use { out ->
                    val chunk = 64 * 1024
                    var offset = 0
                    while (offset < bytes.size) {
                        val len = minOf(chunk, bytes.size - offset)
                        out.write(bytes, offset, len)
                        offset += len
                        onProgress((offset * 100L / bytes.size).toInt())
                    }
                    out.flush()
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                if (code !in 200..299) {
                    throw IllegalStateException("Storage upload failed ($code): ${body.take(200)}")
                }
                "$storageBase/object/public/$bucket/$path"
            } finally {
                conn.disconnect()
            }
        }
    }

    override suspend fun download(url: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val isWorkerUrl = workerBase.isNotBlank() && url.startsWith("$workerBase/v1/objects/")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 120_000
            if (isWorkerUrl) {
                val accessToken = sessionStore.session.value?.accessToken?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Sign in before downloading this file")
                conn.setRequestProperty("Authorization", "Bearer $accessToken")
            } else {
                conn.setRequestProperty("apikey", config.anonKey)
                conn.setRequestProperty("Authorization", "Bearer ${bearer()}")
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val message = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("File download failed ($code): ${message.take(160)}")
            }
            val result = conn.inputStream.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(64 * 1024)
                var read: Int
                while (input.read(chunk).also { read = it } != -1) {
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            }
            conn.disconnect()
            result
        }
    }
}
