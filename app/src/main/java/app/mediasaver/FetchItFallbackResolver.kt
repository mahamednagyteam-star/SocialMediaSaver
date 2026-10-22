package app.mediasaver

import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

/**
 * Small compatibility fallback adapted from FetchIt's MIT-licensed URL fetchers.
 * These public third-party resolvers can change or become unavailable; yt-dlp remains primary.
 */
internal object FetchItFallbackResolver {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun resolve(url: String): String? {
        val host = Uri.parse(url).host?.lowercase().orEmpty()
        return when {
            host == "youtu.be" || host.endsWith(".youtube.com") || host == "youtube.com" ->
                resolveWithCobalt(url)
            host == "instagram.com" || host.endsWith(".instagram.com") ->
                resolveInstagram(url)
            host == "facebook.com" || host.endsWith(".facebook.com") || host == "fb.watch" ->
                resolveFacebook(url)
            host == "twitter.com" || host.endsWith(".twitter.com") ||
                host == "x.com" || host.endsWith(".x.com") ->
                resolveTwitter(url)
            else -> null
        }
    }

    private fun resolveWithCobalt(url: String): String? {
        val body = JSONObject().put("url", url).toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.cobalt.tools/api/json")
            .post(body)
            .header("Accept", "application/json")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val json = JSONObject(response.body?.string().orEmpty())
            val status = json.optString("status")
            if (status == "error" || status == "rate-limit") null
            else json.optString("url").takeIf { it.startsWith("https://") || it.startsWith("http://") }
        }
    }

    private fun resolveInstagram(url: String): String? {
        val postId = Regex("""instagram\.com/(?:p|reel|tv)/([^/?#]+)""")
            .find(url)?.groupValues?.getOrNull(1) ?: return null
        val request = Request.Builder().url("https://imginn.com/p/$postId/").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val document = Jsoup.parse(response.body?.string().orEmpty())
            document.select("a[download], a[href]").firstOrNull {
                val href = it.attr("href")
                href.startsWith("http") && (href.contains(".mp4") || href.contains(".jpg") || href.contains(".webp"))
            }?.attr("href")?.takeIf { it.startsWith("http") }
        }
    }

    private fun resolveTwitter(url: String): String? {
        val request = Request.Builder()
            .url("https://twitsave.com/info?url=" + Uri.encode(url))
            .header("User-Agent", USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val document = Jsoup.parse(response.body?.string().orEmpty())
            document.select("video[src], video source[src]").firstOrNull()?.attr("src")
                ?.takeIf { it.startsWith("http") }
        }
    }

    private fun resolveFacebook(url: String): String? {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val html = response.body?.string().orEmpty()
            val hd = Regex("""browser_native_hd_url\\?":\\?"([^"]+)""").find(html)?.groupValues?.getOrNull(1)
            val sd = Regex("""browser_native_sd_url\\?":\\?"([^"]+)""").find(html)?.groupValues?.getOrNull(1)
            decodeEscapedUrl(hd) ?: decodeEscapedUrl(sd)
        }
    }

    private fun decodeEscapedUrl(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return value
            .replace("\\u0026", "&")
            .replace("\\u003D", "=")
            .replace("\\u0025", "%")
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .takeIf { it.startsWith("https://") || it.startsWith("http://") }
    }

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/122.0 Mobile Safari/537.36"
}
