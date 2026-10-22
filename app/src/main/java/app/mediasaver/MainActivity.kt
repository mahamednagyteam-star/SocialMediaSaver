package app.mediasaver

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MediaSaverApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaSaverApp() {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var darkMode by remember { mutableStateOf(false) }
    val colors = if (darkMode) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Media Saver", fontWeight = FontWeight.Bold)
                            Text("حفظ الوسائط بسهولة", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    actions = {
                        androidx.compose.material3.TextButton(onClick = { darkMode = !darkMode }) {
                            Text(if (darkMode) "فاتح" else "داكن")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("نزّل واحفظ ملفاتك", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "الصق رابط ملف صورة أو فيديو مباشر تملك حق حفظه. سيظهر تقدم التنزيل في إشعارات الهاتف.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text("رابط التحميل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://example.com/video.mp4") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Button(
                    onClick = { startDownload(context, url) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("ابدأ التحميل")
                }

                Card(shape = RoundedCornerShape(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Folder, contentDescription = null)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("مكان الحفظ", fontWeight = FontWeight.Bold)
                            Text("الملفات تُحفظ في مجلد Downloads باستخدام مدير التنزيلات في أندرويد.")
                        }
                    }
                }

                Text("قبل التحميل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "روابط صفحات TikTok وInstagram وYouTube وغيرها ليست عادةً روابط ملفات مباشرة. استخدم خيارات التنزيل أو المشاركة الرسمية، ولا تحاول حفظ محتوى خاص أو محمي دون إذن.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
                Text(
                    "الإصدار الأولي • دعم الروابط المباشرة",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun startDownload(context: Context, rawUrl: String) {
    val input = rawUrl.trim()
    if (input.isBlank()) {
        Toast.makeText(context, "أدخل الرابط أولًا", Toast.LENGTH_SHORT).show()
        return
    }

    val uri = runCatching { Uri.parse(input) }.getOrNull()
    if (uri == null || uri.scheme?.lowercase(Locale.ROOT) !in listOf("http", "https") || uri.host.isNullOrBlank()) {
        Toast.makeText(context, "الرابط غير صحيح. استخدم رابطًا يبدأ بـ https://", Toast.LENGTH_LONG).show()
        return
    }

    val path = uri.path.orEmpty().lowercase(Locale.ROOT)
    val supported = listOf(".mp4", ".m4v", ".mov", ".webm", ".mp3", ".m4a", ".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf")
        .any { path.endsWith(it) }
    if (!supported) {
        Toast.makeText(
            context,
            "هذا يبدو رابط صفحة وليس ملفًا مباشرًا. استخدم رابط الملف أو خيارات التنزيل الرسمية للمنصة.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    try {
        val fileName = URLUtil.guessFileName(input, null, null).ifBlank { "media_download" }
        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription("جارٍ تنزيل الملف")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "بدأ التنزيل. تابع التقدم من الإشعارات.", Toast.LENGTH_LONG).show()
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر بدء التنزيل. تحقق من الرابط وحاول مرة أخرى.", Toast.LENGTH_LONG).show()
    }
}
