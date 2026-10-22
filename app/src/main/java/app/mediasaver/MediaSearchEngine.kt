package app.mediasaver

import android.net.Uri
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal data class MediaSearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val platform: String
)

/**
 * Searches publicly indexed pages. This is intentionally not presented as an official
 * platform API: indexing coverage and markup can change at any time.
 */
internal object MediaSearchEngine {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val platforms = linkedMapOf(
        "YouTube" to "youtube.com",
        "TikTok" to "tiktok.com",
        "Instagram" to "instagram.com",
        "Facebook" to "facebook.com",
        "X" to "x.com"
    )

    fun search(query: String, selectedPlatform: String, onResult: (List<MediaSearchResult>, String?) -> Unit) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) {
            onResult(emptyList(), "اكتب اسم المحتوى أولًا.")
            return
        }
        executor.execute {
            val results = mutableListOf<MediaSearchResult>()
            val errors = mutableListOf<String>()
            val targets = if (selectedPlatform == "الكل") platforms.keys.toList() else listOf(selectedPlatform)
            for (platform in targets) {
                val domain = platforms[platform] ?: continue
                try {
                    val searchUrl = "https://html.duckduckgo.com/html/".toHttpUrl().newBuilder()
                        .addQueryParameter("q", "site:$domain \"$cleaned\"")
                        .build()
                    val request = Request.Builder()
                        .url(searchUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                        .header("Accept-Language", "ar,en;q=0.8")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            errors += platform
                        } else {
                            val doc = Jsoup.parse(response.body?.string().orEmpty())
                            doc.select(".result").forEach { element ->
                                val anchor = element.selectFirst("a.result__a") ?: return@forEach
                                val rawHref = anchor.attr("href")
                                val resolved = resolveSearchUrl(rawHref)
                                val host = runCatching { Uri.parse(resolved).host?.lowercase().orEmpty() }.getOrDefault("")
                                if (!resolved.startsWith("https://") && !resolved.startsWith("http://")) return@forEach
                                if (!(host == domain || host.endsWith(".$domain"))) return@forEach
                                val title = anchor.text().trim()
                                if (title.isBlank() || results.any { it.url == resolved }) return@forEach
                                results += MediaSearchResult(
                                    title = title,
                                    url = resolved,
                                    snippet = element.selectFirst(".result__snippet")?.text()?.trim().orEmpty(),
                                    platform = platform
                                )
                            }
                        }
                    }
                } catch (_: Exception) {
                    errors += platform
                }
            }
            val message = when {
                results.isNotEmpty() -> null
                errors.size == targets.size -> "تعذّر جلب النتائج الآن. جرّب لاحقًا أو تحقق من اتصال الإنترنت."
                else -> "لم تظهر نتائج مفهرسة لهذا البحث. جرّب كلمات أخرى."
            }
            mainHandler.post { onResult(results.take(25), message) }
        }
    }

    private fun resolveSearchUrl(raw: String): String {
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return raw
        val redirect = uri.getQueryParameter("uddg")
        if (!redirect.isNullOrBlank()) {
            return runCatching { URLDecoder.decode(redirect, StandardCharsets.UTF_8.name()) }.getOrDefault(redirect)
        }
        return raw
    }
}
