package app.mediasaver

import android.content.Context
import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MediaSaverApp() }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MediaSaverApp() {
    val context: Context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var darkMode by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("الصق رابطًا عامًا لوسائط تملكها أو لديك إذن بتنزيلها.") }
    var isDownloading by remember { mutableStateOf(false) }
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
                        Text(
                            "نزّل واحفظ وسائطك",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "محرك yt-dlp مدمج لمحاولة استخراج الوسائط من مواقع كثيرة، مع FFmpeg لدمج مسارات الصوت والفيديو عند الحاجة.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text("رابط الوسائط", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://example.com/video أو رابط منصة") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Button(
                    enabled = !isDownloading,
                    onClick = {
                        isDownloading = true
                        MediaDownloadEngine.download(context, url) { message, finished ->
                            status = message
                            if (finished) isDownloading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text(if (isDownloading) "جارٍ التحميل…" else "ابدأ التحميل")
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
                            Text("يُحفظ المحتوى في مجلد Downloads/MediaSaver.")
                        }
                    }
                }

                Text("حالة التنزيل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "تختلف المواقع المدعومة حسب تحديثات محرك yt-dlp وسياسات كل منصة. لا يدعم التطبيق تجاوز تسجيل الدخول أو الوصول إلى محتوى خاص أو محمي؛ استخدمه فقط للمحتوى المسموح لك بحفظه.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
                Text(
                    "نسخة تجريبية • قد لا تعمل بعض المواقع أو الروابط",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
