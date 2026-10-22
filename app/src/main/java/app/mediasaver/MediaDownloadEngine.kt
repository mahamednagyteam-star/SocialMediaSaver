package app.mediasaver

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.Executors

/** Shared yt-dlp engine with app-private staging and MediaStore publishing. */
object MediaDownloadEngine {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun download(context: Context, rawUrl: String, onStatus: (String, Boolean) -> Unit) {
        val input = rawUrl.trim()
        val uri = runCatching { Uri.parse(input) }.getOrNull()
        if (input.isBlank() || uri == null ||
            uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()
        ) {
            onStatus("أدخل رابطًا صحيحًا يبدأ بـ https:// أو http://", true)
            return
        }

        postStatus(onStatus, "جارٍ تهيئة محرك التنزيل…", false)
        executor.execute {
            try {
                val appContext = context.applicationContext
                YoutubeDL.getInstance().init(appContext)
                FFmpeg.getInstance().init(appContext)

                val outputDirectory = File(
                    appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: appContext.filesDir,
                    "MediaSaver"
                )
                if (!outputDirectory.exists() && !outputDirectory.mkdirs()) error("تعذر تجهيز مجلد التنزيل المؤقت")
                val before = outputDirectory.listFiles()?.associate { it.absolutePath to it.lastModified() }.orEmpty()

                try {
                    runYtDlp(input, outputDirectory, onStatus)
                } catch (primaryError: Exception) {
                    val fallbackUrl = runCatching { FetchItFallbackResolver.resolve(input) }.getOrNull()
                    if (fallbackUrl.isNullOrBlank() || fallbackUrl == input) throw primaryError
                    postStatus(onStatus, "أجرب محللًا بديلًا لهذا الرابط…", false)
                    runYtDlp(fallbackUrl, outputDirectory, onStatus)
                }

                val outputFile = outputDirectory.listFiles()
                    ?.filter { it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") }
                    ?.filter { before[it.absolutePath] != it.lastModified() }
                    ?.maxByOrNull { it.lastModified() }
                    ?: error("اكتمل المحرك لكن لم أجد ملفًا نهائيًا لحفظه")
                val saved = publishToDownloads(appContext, outputFile)
                if (saved) postStatus(onStatus, "اكتمل التنزيل. افتح مجلد Downloads/MediaSaver للعثور على الملف.", true)
                else postStatus(onStatus, "اكتمل التنزيل داخل مجلد التطبيق؛ تعذر نقله إلى Downloads.", true)
            } catch (error: Exception) {
                val reason = error.message?.lineSequence()?.firstOrNull()?.take(180) ?: "تحقق من الرابط والاتصال بالإنترنت."
                postStatus(onStatus, "تعذر التنزيل: $reason", true)
            }
        }
    }

    private fun publishToDownloads(context: Context, source: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, source.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType(source.extension))
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MediaSaver")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val destination: Uri = context.contentResolver.insert(collection, values) ?: return false
            return try {
                context.contentResolver.openOutputStream(destination)?.use { output ->
                    FileInputStream(source).use { input -> input.copyTo(output) }
                } ?: error("تعذر فتح ملف الحفظ")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(destination, values, null, null)
                true
            } catch (error: Exception) {
                context.contentResolver.delete(destination, null, null)
                throw error
            }
        }

        val legacyDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "MediaSaver")
        if (!legacyDir.exists()) legacyDir.mkdirs()
        val destination = File(legacyDir, source.name)
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output -> input.copyTo(output) }
        }
        return false
    }

    private fun mimeType(extension: String): String = when (extension.lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "application/octet-stream"
    }

    private fun runYtDlp(url: String, outputDirectory: File, onStatus: (String, Boolean) -> Unit) {
        val request = YoutubeDLRequest(url).apply {
            addOption("--no-playlist")
            addOption("--newline")
            addOption("--no-mtime")
            addOption("-f", "bestvideo*+bestaudio/best")
            addOption("--merge-output-format", "mp4")
            addOption("-o", File(outputDirectory, "%(title).180B.%(ext)s").absolutePath)
        }
        YoutubeDL.getInstance().execute(request, null, false) { progress, eta, line ->
            val detail = line.trim().take(100)
            val message = buildString {
                append("جارٍ التنزيل: ")
                append(progress.toInt().coerceIn(0, 100))
                append("%")
                if (eta > 0) append(" • متبقٍ تقريبًا ").append(eta).append(" ثانية")
                if (detail.isNotEmpty()) append("\n").append(detail)
            }
            postStatus(onStatus, message, false)
        }
    }

    private fun postStatus(callback: (String, Boolean) -> Unit, message: String, finished: Boolean) {
        mainHandler.post { callback(message, finished) }
    }
}
