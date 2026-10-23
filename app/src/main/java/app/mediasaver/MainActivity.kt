package app.mediasaver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.dataString
        setContent { MediaSaverApp(sharedUrl) }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MediaSaverApp(sharedUrl: String? = null) {
    val context: Context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var url by remember { mutableStateOf(sharedUrl.orEmpty()) }
    var darkMode by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("الصق رابط الفيديو أو الصورة لبدء التنزيل.") }
    var isDownloading by remember { mutableStateOf(false) }
    var selectedPlatform by remember { mutableStateOf("الكل") }
    var selectedQuality by remember { mutableStateOf("best") }
    var audioOnly by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("الرئيسية") }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MediaSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf<String?>(null) }
    val accent = Color(0xFF27D6A0)
    val colors = if (darkMode) darkColorScheme(
        primary = accent, onPrimary = Color(0xFF08261D),
        background = Color(0xFF0C1117), surface = Color(0xFF151D26),
        secondary = Color(0xFF8BA7FF)
    ) else lightColorScheme(primary = Color(0xFF008F69), secondary = Color(0xFF405BB5))

    fun startDownload(input: String) {
        val detected = detectPlatform(input)
        if (selectedPlatform != "الكل" && detected != selectedPlatform) {
            selectedTab = "التنزيل"
            status = "الرابط لا يطابق المنصة المختارة ($selectedPlatform). اختر المنصة الصحيحة أو «الكل»."
            return
        }
        selectedTab = "التنزيل"
        isDownloading = true
        MediaDownloadEngine.download(context, input, selectedQuality, audioOnly) { message, finished ->
            status = message
            if (finished) isDownloading = false
        }
    }

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(13.dp))
                                    .background(Brush.linearGradient(listOf(accent, Color(0xFF8BE7C8)))),
                                contentAlignment = Alignment.Center
                            ) { Text("↓", color = Color(0xFF08261D), fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall) }
                            Column {
                                Text("Media Saver", fontWeight = FontWeight.ExtraBold)
                                Text("نزّل • احفظ • شاهد لاحقًا", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    actions = {
                        TextButton(onClick = { darkMode = !darkMode }) {
                            Text(if (darkMode) "☼" else "☾", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                )
            },
            bottomBar = {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        listOf("الرئيسية" to "⌂", "بحث" to "⌕", "التنزيل" to "↓", "الإعدادات" to "⚙").forEach { (tab, symbol) ->
                            TextButton(onClick = { selectedTab = tab }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(symbol, style = MaterialTheme.typography.titleLarge, color = if (selectedTab == tab) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(tab, style = MaterialTheme.typography.labelSmall, color = if (selectedTab == tab) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (selectedTab) {
                    "بحث" -> {
                        Text("ابحث عن محتواك", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("اكتب اسم فيديو أو موضوع، ثم اختر منصة أو ابحث في الكل.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("مثال: تعلم الرسم للمبتدئين") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp)
                        )
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("الكل", "YouTube", "TikTok", "Instagram", "Facebook", "X").forEach { platform ->
                                Surface(
                                    onClick = { selectedPlatform = platform },
                                    shape = CircleShape,
                                    color = if (selectedPlatform == platform) accent else MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        platform,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                        color = if (selectedPlatform == platform) Color(0xFF08261D) else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                isSearching = true
                                searchMessage = null
                                searchResults = emptyList()
                                MediaSearchEngine.search(searchQuery, selectedPlatform) { results, message ->
                                    searchResults = results
                                    searchMessage = message
                                    isSearching = false
                                }
                            },
                            enabled = !isSearching && searchQuery.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF08261D))
                        ) {
                            Text(if (isSearching) "جارٍ البحث…" else "⌕  ابحث في المنصات", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "النتائج تعتمد على الصفحات العامة المفهرسة، وقد لا تشمل كل المنشورات أو الحسابات.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isSearching) {
                            Text("بنبحث عن نتائج عامة من المصادر المختارة…")
                        } else if (searchMessage != null) {
                            Text(searchMessage!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        searchResults.forEach { result ->
                            Card(shape = RoundedCornerShape(20.dp)) {
                                Column(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(result.platform, style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
                                    Text(result.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (result.snippet.isNotBlank()) {
                                        Text(result.snippet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(result.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                    Button(
                                        onClick = { startDownload(result.url) },
                                        enabled = !isDownloading,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(if (isDownloading) "يوجد تنزيل قيد التشغيل" else "↓  تحميل هذا المحتوى")
                                    }
                                }
                            }
                        }
                    }
                    "الإعدادات" -> {
                        Text("الإعدادات", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Card(shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("المظهر", fontWeight = FontWeight.Bold)
                                Text("بدّل بين الوضع الداكن والفاتح من أعلى الشاشة.")
                                Text("مكان الحفظ", fontWeight = FontWeight.Bold)
                                Text("Downloads/MediaSaver على Android 10 فما فوق.")
                                Text("الخصوصية", fontWeight = FontWeight.Bold)
                                Text("استخدم الروابط العامة للمحتوى الذي تملكه أو لديك إذن بحفظه.")
                            }
                        }
                    }
                    "التنزيل" -> {
                        Text("التنزيل الحالي", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Card(shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(if (isDownloading) "التنزيل قيد التشغيل" else "حالة آخر عملية", fontWeight = FontWeight.Bold)
                                Text(status)
                                Text("مجلد الحفظ: Downloads/MediaSaver", style = MaterialTheme.typography.bodySmall)
                                Button(onClick = { selectedTab = "الرئيسية" }, modifier = Modifier.fillMaxWidth()) { Text("تنزيل رابط آخر") }
                            }
                        }
                    }
                    else -> {
                        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("مركز التنزيل", style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
                                Text("كل وسائطك في مكان واحد", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                                Text("روابطك، خيارات الجودة، والتنزيلات في تجربة واحدة بسيطة.", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("روابط مباشرة", "جودة مرنة", "حفظ منظم").forEach { feature ->
                                        Surface(color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(50)) {
                                            Text(feature, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Color(0xFFB9F5DF), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                    listOf("الكل", "YouTube", "TikTok", "Instagram", "Facebook", "X").forEach { platform ->
                                        Surface(
                                            onClick = { selectedPlatform = platform },
                                            shape = CircleShape,
                                            color = if (selectedPlatform == platform) accent else MaterialTheme.colorScheme.background
                                        ) {
                                            Text(platform, modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                                color = if (selectedPlatform == platform) Color(0xFF08261D) else MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                        Text("أضف رابطًا", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        OutlinedTextField(
                            value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("الصق رابط الفيديو أو الصورة هنا") },
                            leadingIcon = { Text("↗", modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.titleLarge) },
                            trailingIcon = { TextButton(onClick = { clipboard.getText()?.text?.let { url = it.trim() } }) { Text("لصق") } },
                            singleLine = true, shape = RoundedCornerShape(18.dp)
                        )
                        Text("نوع الملف والدقة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("best" to "أفضل جودة", "360" to "360p", "480" to "480p", "720" to "720p", "1080" to "1080p").forEach { (value, label) ->
                                Surface(
                                    onClick = { selectedQuality = value; audioOnly = false },
                                    shape = CircleShape,
                                    color = if (!audioOnly && selectedQuality == value) accent else MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        label,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                        color = if (!audioOnly && selectedQuality == value) Color(0xFF08261D) else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        TextButton(onClick = { audioOnly = !audioOnly }) {
                            Text(if (audioOnly) "✓ تنزيل الصوت فقط (بدون تحويل MP3)" else "♫ تنزيل الصوت فقط")
                        }
                        Button(
                            enabled = !isDownloading,
                            onClick = { startDownload(url) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF08261D))
                        ) { Text(if (isDownloading) "جارٍ التنزيل…" else "↓   ابدأ التنزيل", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium) }

                        Card(shape = RoundedCornerShape(22.dp)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                    Text("▣", style = MaterialTheme.typography.headlineSmall)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                                    Text("حفظ تلقائي", fontWeight = FontWeight.Bold)
                                    Text("تُحفظ الملفات في مجلد MediaSaver عند اكتمال التنزيل.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Text("الحالة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        Text("تختلف المواقع المدعومة حسب تحديثات المحرك. لا تستخدم التطبيق للوصول إلى محتوى خاص أو لتجاوز قيود المنصات.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}


private fun detectPlatform(rawUrl: String): String? {
    val host = runCatching { Uri.parse(rawUrl.trim()).host?.lowercase().orEmpty() }.getOrDefault("")
    return when {
        host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be" -> "YouTube"
        host == "tiktok.com" || host.endsWith(".tiktok.com") -> "TikTok"
        host == "instagram.com" || host.endsWith(".instagram.com") || host == "instagr.am" || host.endsWith(".instagr.am") -> "Instagram"
        host == "facebook.com" || host.endsWith(".facebook.com") || host == "fb.watch" -> "Facebook"
        host == "x.com" || host.endsWith(".x.com") || host == "twitter.com" || host.endsWith(".twitter.com") -> "X"
        else -> null
    }
}
