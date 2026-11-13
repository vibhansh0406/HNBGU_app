package `in`.campusone.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
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

    private fun bearer(): String =
        sessionStore.session.value?.accessToken?.takeIf { it.isNotBlank() } ?: config.anonKey

    override suspend fun upload(
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
            var uploadError: String? = null
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
            } catch (e: Exception) {
                uploadError = e.message
            }
            if (uploadError != null) {
                conn.disconnect()
                throw IllegalStateException("Upload stream failed: $uploadError")
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code !in 200..299) {
                throw IllegalStateException("Storage upload failed ($code): ${body.take(200)}")
            }
            "$storageBase/object/public/$bucket/$path"
        }
    }

    override suspend fun download(url: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("apikey", config.anonKey)
            conn.setRequestProperty("Authorization", "Bearer ${bearer()}")
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.errorStream?.close()
                conn.disconnect()
                throw IllegalStateException("File download failed ($code)")
            }
            val bytes = conn.inputStream.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(64 * 1024)
                var read: Int
                while (input.read(chunk).also { read = it } != -1) {
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            }
            conn.disconnect()
            bytes
        }
    }
}
