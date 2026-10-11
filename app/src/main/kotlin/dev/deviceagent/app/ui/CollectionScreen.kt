package dev.deviceagent.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.deviceagent.app.CollectionKind
import dev.deviceagent.app.CollectionSpec
import dev.deviceagent.app.FileBrowserViewModel
import dev.deviceagent.app.SortMode
import dev.deviceagent.app.UiState
import dev.deviceagent.app.defaultSort
import dev.deviceagent.app.formatBytes
import dev.deviceagent.app.sortEntries
import dev.deviceagent.core.fs.FileInfo

/**
 * One dedicated page: Documents, Images, Videos, Audio, Archives, Apps, Downloads, Recent, Favorites or Large files.
 * Read-only. A file opens its details sheet; a folder (only Favorites can hold one) opens in Browse.
 */
@Composable
internal fun CollectionScreen(
    spec: CollectionSpec,
    s: UiState,
    browser: FileBrowserViewModel,
    onBack: () -> Unit,
    onReveal: (FileInfo) -> Unit,
    onAskAi: (String) -> Unit,
) {
    val c by browser.collection.collectAsState()
    val favorites by browser.favorites.collectAsState()
    val favoriteInfos by browser.favoriteInfos.collectAsState()
    val grid by browser.gridView.collectAsState()
    var details by remember { mutableStateOf<FileInfo?>(null) }
    LaunchedEffect(spec.key, s.roots, s.useAllFilesAccess) { browser.openCollection(spec) }

    // The state may still describe the previous page for a moment after opening a new one.
    val current = c.spec == spec
    val (defaultMode, defaultAsc) = defaultSort(spec)
    val sort = if (current) c.sort else defaultMode
    val ascending = if (current) c.ascending else defaultAsc
    val scanning = !current || c.scanning
    val raw = when {
        spec.kind == CollectionKind.FAVORITES -> favoriteInfos
        current -> c.items
        else -> emptyList()
    }
    val shown = remember(raw, sort, ascending) { sortEntries(raw, sort, ascending) }
    val error = if (current) c.error else null
    val files = shown.count { !it.isDirectory }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back") }
            Spacer(Modifier.weight(1f))
            SortMenu(sort, ascending, browser::setCollectionSort)
            ViewToggle(grid, browser::setGridView)
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${spec.emoji}  ${spec.title}", style = MaterialTheme.typography.headlineSmall)
            Hint(summaryLine(spec, shown, files, scanning, if (current) c.note else null))
            if (scanning && spec.kind != CollectionKind.FAVORITES) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Box(Modifier.weight(1f)) {
            when {
                error != null -> Column(Modifier.padding(16.dp)) {
                    AppCard {
                        Text("Couldn't show this", style = MaterialTheme.typography.titleSmall)
                        Hint(error)
                        OutlinedButton(onClick = { browser.openCollection(spec) }) { Text("Try again") }
                    }
                }
                shown.isEmpty() && !scanning -> Column(Modifier.padding(16.dp)) {
                    AppCard {
                        val (title, hint) = emptyText(spec)
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Hint(hint)
                    }
                }
                grid -> FileGrid(shown, emptySet(), onClick = { e -> if (e.isDirectory) onReveal(e) else details = e }, onLongClick = { details = it })
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    items(shown, key = { it.path }) { e ->
                        CollectionRow(
                            e = e,
                            starred = e.path in favorites,
                            onClick = { if (e.isDirectory) onReveal(e) else details = e },
                            onToggleFavorite = { browser.toggleFavorite(e.path) },
                        )
                    }
                }
            }
        }
    }

    details?.let { e ->
        FileDetailsSheet(
            e = e,
            starred = e.path in favorites,
            onToggleFavorite = { browser.toggleFavorite(e.path) },
            onAskAi = { details = null; onAskAi("Do this with these items:\n- ${e.path}") },
            onDismiss = { details = null },
            onShowInFolder = { details = null; onReveal(e) },
        )
    }
}

@Composable
private fun CollectionRow(e: FileInfo, starred: Boolean, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EntryIcon(e)
        Column(Modifier.weight(1f)) {
            Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Hint(entrySubtitle(e))
            Text(parentLabel(e.path), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Filled.Star,
                contentDescription = if (starred) "Remove from favorites" else "Add to favorites",
                tint = if (starred) Color(0xFFF5A623) else MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

private fun summaryLine(spec: CollectionSpec, items: List<FileInfo>, files: Int, scanning: Boolean, note: String?): String {
    val base = when {
        items.isEmpty() -> if (scanning) "Looking through your folders…" else "Nothing found"
        spec.kind == CollectionKind.LARGE -> "The ${items.size} biggest files · ${formatBytes(items.sumOf { it.size })} together"
        spec.kind == CollectionKind.FAVORITES -> "${items.size} saved ${if (items.size == 1) "item" else "items"}"
        else -> "$files ${if (files == 1) "file" else "files"} · ${formatBytes(items.sumOf { it.size })}"
    }
    return if (note != null) "$base. $note" else base
}

private fun emptyText(spec: CollectionSpec): Pair<String, String> = when (spec.kind) {
    CollectionKind.CATEGORY -> "No ${spec.title.lowercase()} found" to "Nothing of this kind in the folders you chose. Add more folders in Settings."
    CollectionKind.DOWNLOADS -> "No Download folder found" to "Add your Download folder in Settings → Folders and access, and its files will appear here."
    CollectionKind.RECENT -> "No recent files yet" to "Files you add or change will show up here."
    CollectionKind.FAVORITES -> "No favorites yet" to "Tap the star next to a file or folder in Browse to keep it here."
    CollectionKind.LARGE -> "No files found" to "Add a folder in Settings and the biggest files in it will be listed here."
}
