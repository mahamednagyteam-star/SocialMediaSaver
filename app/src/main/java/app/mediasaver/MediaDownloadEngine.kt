package app.mediasaver

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.concurrent.Executors

/** Unified downloader: yt-dlp first, with FetchIt's legacy site resolvers as a fallback. */
object MediaDownloadEngine {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun download(
        context: Context,
        rawUrl: String,
        onStatus: (message: String, finished: Boolean) -> Unit
    ) {
        val input = rawUrl.trim()
        val uri = runCatching { Uri.parse(input) }.getOrNull()
        if (input.isBlank() || uri == null ||
            uri.scheme?.lowercase() !in setOf("http", "https") ||
            uri.host.isNullOrBlank()
        ) {
            onStatus("أدخل رابطًا صحيحًا يبدأ بـ https:// أو http://", true)
            return
        }

        onStatus("جارٍ تهيئة محرك التنزيل…", false)
        executor.execute {
            try {
                val appContext = context.applicationContext
                YoutubeDL.getInstance().init(appContext)
                FFmpeg.getInstance().init(appContext)

                val downloadsRoot = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val outputDirectory = File(downloadsRoot, "MediaSaver")
                if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                    error("تعذر إنشاء مجلد التنزيلات")
                }

                try {
                    runYtDlp(input, outputDirectory, onStatus)
                } catch (primaryError: Exception) {
                    val fallbackUrl = runCatching { FetchItFallbackResolver.resolve(input) }.getOrNull()
                    if (fallbackUrl.isNullOrBlank() || fallbackUrl == input) {
                        throw primaryError
                    }
                    postStatus(onStatus, "تعذر الاستخراج الأساسي؛ أجرب محلل FetchIt البديل…", false)
                    runYtDlp(fallbackUrl, outputDirectory, onStatus)
                }

                postStatus(onStatus, "اكتمل التنزيل. ستجد الملف في Downloads/MediaSaver.", true)
            } catch (error: Exception) {
                val reason = error.message?.lineSequence()?.firstOrNull()?.take(180)
                    ?: "تحقق من الرابط والاتصال بالإنترنت."
                postStatus(onStatus, "تعذر التنزيل: $reason", true)
            }
        }
    }

    private fun runYtDlp(
        url: String,
        outputDirectory: File,
        onStatus: (String, Boolean) -> Unit
    ) {
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
                if (eta > 0) {
                    append(" • متبقٍ تقريبًا ")
                    append(eta)
                    append(" ثانية")
                }
                if (detail.isNotEmpty()) {
                    append("\n")
                    append(detail)
                }
            }
            postStatus(onStatus, message, false)
        }
    }

    private fun postStatus(
        callback: (String, Boolean) -> Unit,
        message: String,
        finished: Boolean
    ) {
        mainHandler.post { callback(message, finished) }
    }
}
