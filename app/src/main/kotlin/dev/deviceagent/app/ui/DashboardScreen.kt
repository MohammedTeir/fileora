package dev.deviceagent.app.ui

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.FileCategory
import dev.deviceagent.app.formatBytes
import dev.deviceagent.core.fs.FileInfo
import java.util.Calendar

/** Colour used for a category's tile. Alpha-blended so it works on both the light and the dark theme. */
internal fun categoryColor(c: FileCategory): Color = when (c) {
    FileCategory.DOCUMENTS -> Color(0xFF2F7BFF)
    FileCategory.IMAGES -> Color(0xFF22B573)
    FileCategory.VIDEOS -> Color(0xFF7A5AF8)
    FileCategory.AUDIO -> Color(0xFF2FA8F5)
    FileCategory.ARCHIVES -> Color(0xFF8A5CF5)
    FileCategory.APPS -> Color(0xFF1FB59B)
}

/**
 * Home: a read-only overview. Nothing here changes files. The tiles open the other tabs, and "Clean up"
 * only drafts a request for the AI tab, where the usual plan, confirmation and undo apply.
 */
@Composable
internal fun DashboardScreen(
    recent: List<FileInfo>,
    favorites: List<FileInfo>,
    onReveal: (FileInfo) -> Unit,
    onSearch: () -> Unit,
    onBrowse: () -> Unit,
    onOpenCategory: (FileCategory) -> Unit,
    onAskAi: (String?) -> Unit,
    onOpenTasks: () -> Unit,
) {
    val usage = remember { storageUsage() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileoraIcon(FileoraIconName.Folder, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.size(10.dp))
                Text("Fileora", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "Search") }
            }
        }

        item { GreetingBanner() }

        usage?.let { u -> item { StorageCard(u) } }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTile("✨", "AI Assistant", "Ask in plain words", Modifier.weight(1f)) { onAskAi(null) }
                    QuickTile("🧹", "Clean up", "Find your biggest files", Modifier.weight(1f)) {
                        onAskAi("Show me the 20 biggest files in my folders")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTile("📁", "Browse", "Look through folders", Modifier.weight(1f), onClick = onBrowse)
                    QuickTile("🕒", "Tasks", "History and Undo", Modifier.weight(1f), onClick = onOpenTasks)
                }
            }
        }

        if (recent.isNotEmpty()) {
            item { Text("Recent files", style = MaterialTheme.typography.titleMedium) }
            items(recent, key = { "recent:" + it.path }) { e -> FileRow(e, onReveal) }
        }

        if (favorites.isNotEmpty()) {
            item { Text("Favorites", style = MaterialTheme.typography.titleMedium) }
            items(favorites, key = { "fav:" + it.path }) { e -> FileRow(e, onReveal) }
        }

        item { Text("Categories", style = MaterialTheme.typography.titleMedium) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FileCategory.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { c -> CategoryTile(c, Modifier.weight(1f)) { onOpenCategory(c) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(e: FileInfo, onClick: (FileInfo) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick(e) }.padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EntryIcon(e)
        Column(Modifier.weight(1f)) {
            Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Hint(entrySubtitle(e))
        }
    }
}

@Composable
private fun GreetingBanner() {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val hello = when {
        hour < 12 -> "Good morning!"
        hour < 18 -> "Good afternoon!"
        else -> "Good evening!"
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1558E6), Color(0xFF2D8CFF)))),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(hello, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Your files, organized and always within reach.", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
            }
            FileoraIcon(FileoraIconName.FolderOpen, contentDescription = null, modifier = Modifier.size(64.dp))
        }
    }
}

private class StorageUsage(val used: Long, val total: Long) {
    val percent: Int get() = if (total <= 0) 0 else (used * 100 / total).toInt()
}

@Suppress("DEPRECATION")
private fun storageUsage(): StorageUsage? = runCatching {
    val st = StatFs(Environment.getExternalStorageDirectory().path)
    val total = st.blockCountLong * st.blockSizeLong
    StorageUsage(used = total - st.availableBytesLong, total = total)
}.getOrNull()?.takeIf { it.total > 0 }

@Composable
private fun StorageCard(u: StorageUsage) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Storage", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${u.percent}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${formatBytes(u.used)} used of ${formatBytes(u.total)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(
            progress = { u.percent / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}

@Composable
private fun EmojiBubble(emoji: String, color: Color, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = (size * 0.45f).sp) }
}

@Composable
private fun QuickTile(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmojiBubble(emoji, MaterialTheme.colorScheme.primary, 38)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

@Composable
private fun CategoryTile(c: FileCategory, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EmojiBubble(c.emoji, categoryColor(c), 56)
        Text(c.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
