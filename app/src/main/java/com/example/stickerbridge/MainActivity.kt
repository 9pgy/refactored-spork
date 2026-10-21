package com.example.stickerbridge

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sticker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class StickerItem(
    val id: String,
    val file: File,
    val displayName: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StickerBridgeApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerBridgeApp() {
    val context = LocalContext.current
    val stickers = remember { mutableStateListOf<StickerItem>() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSticker by remember { mutableStateOf<StickerItem?>(null) }
    var statusMessage by remember { mutableStateOf("استورد أول ستيكر لتبدأ مكتبتك") }

    fun refreshLibrary() {
        val dir = File(context.filesDir, "stickers")
        if (!dir.exists()) dir.mkdirs()
        val existing = dir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp", "gif") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        stickers.clear()
        stickers.addAll(existing.map {
            StickerItem(it.absolutePath, it, it.nameWithoutExtension)
        })
    }

    LaunchedEffect(Unit) { refreshLibrary() }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val dir = File(context.filesDir, "stickers")
                if (!dir.exists()) dir.mkdirs()
                val mime = context.contentResolver.getType(uri).orEmpty()
                val ext = when {
                    mime.contains("png") -> "png"
                    mime.contains("webp") -> "webp"
                    mime.contains("gif") -> "gif"
                    mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
                    else -> "img"
                }
                val destination = File(dir, "sticker_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destination).use { output -> input.copyTo(output) }
                } ?: error("تعذر فتح الملف")
                refreshLibrary()
                selectedSticker = stickers.firstOrNull { it.file.absolutePath == destination.absolutePath }
                statusMessage = "تم استيراد الستيكر بنجاح"
                selectedTab = 0
            } catch (e: Exception) {
                statusMessage = "تعذر استيراد الملف: ${e.localizedMessage ?: "خطأ غير معروف"}"
            }
        }
    }

    fun shareSticker(item: StickerItem) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                item.file
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = when (item.file.extension.lowercase()) {
                    "gif" -> "image/gif"
                    "webp" -> "image/webp"
                    "png" -> "image/png"
                    else -> "image/*"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "مشاركة الستيكر"))
            statusMessage = "اختر Instagram إذا ظهر ضمن خيارات المشاركة"
        } catch (e: Exception) {
            statusMessage = "تعذرت المشاركة: ${e.localizedMessage ?: "خطأ غير معروف"}"
        }
    }

    fun deleteSticker(item: StickerItem) {
        item.file.delete()
        if (selectedSticker?.id == item.id) selectedSticker = null
        refreshLibrary()
        statusMessage = "تم حذف الستيكر من مكتبتك"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("StickerBridge", fontWeight = FontWeight.Bold)
                        Text(
                            "نقل الستيكرات إلى محادثاتك",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { importLauncher.launch(arrayOf("image/*")) },
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(" استيراد")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Sticker, contentDescription = null) },
                    label = { Text("المكتبة") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("استيراد") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("TikTok  →  Instagram DM", fontWeight = FontWeight.Bold)
                    Text(
                        "استورد ملفًا متاحًا للمشاركة، ثم أرسله عبر قائمة مشاركة Android.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            when (selectedTab) {
                0 -> LibraryScreen(
                    stickers = stickers,
                    onSelect = { selectedSticker = it },
                    onShare = { shareSticker(it) },
                    onDelete = { deleteSticker(it) }
                )
                else -> ImportScreen(
                    onImport = { importLauncher.launch(arrayOf("image/*")) }
                )
            }
        }
    }

    selectedSticker?.let { item ->
        StickerPreviewDialog(
            item = item,
            onDismiss = { selectedSticker = null },
            onShare = { shareSticker(item) },
            onDelete = { deleteSticker(item); selectedSticker = null }
        )
    }
}

@Composable
fun LibraryScreen(
    stickers: List<StickerItem>,
    onSelect: (StickerItem) -> Unit,
    onShare: (StickerItem) -> Unit,
    onDelete: (StickerItem) -> Unit
) {
    if (stickers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text("مكتبتك فارغة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "استورد صورة أو ستيكر محفوظًا من جهازك لعرضه هنا.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Text("${stickers.size} عنصر", style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(vertical = 8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(stickers, key = { it.id }) { item ->
                StickerCard(
                    item = item,
                    onClick = { onSelect(item) },
                    onShare = { onShare(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@Composable
fun StickerCard(
    item: StickerItem,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(10.dp)) {
            StickerImage(item.file, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.height(8.dp))
            Text(item.displayName, maxLines = 1, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("إرسال")
                }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun StickerImage(file: File, modifier: Modifier = Modifier) {
    val bitmap = remember(file.absolutePath, file.lastModified()) {
        runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = file.nameWithoutExtension,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(34.dp))
                Text("معاينة غير متاحة", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ImportScreen(onImport: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier.size(76.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text("أضف ستيكرًا إلى مكتبتك", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    "اختر صورة أو ملفًا متاحًا من جهازك. إذا كان تيك توك يسمح بمشاركة الستيكر كملف، احفظه أولًا ثم استورده هنا.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" اختيار ملف")
                }
            }
        }
    }
}

@Composable
fun StickerPreviewDialog(
    item: StickerItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("معاينة الستيكر") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StickerImage(item.file, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)))
                Text(item.displayName, fontWeight = FontWeight.Medium)
                Text(
                    "المشاركة ترسل الملف كوسيط، ولا تضمن تحويله إلى ستيكر أصلي داخل إنستغرام.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text(" مشاركة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}
