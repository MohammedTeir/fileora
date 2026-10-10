package dev.deviceagent.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.deviceagent.app.FileBrowserViewModel
import dev.deviceagent.app.FileCategory
import dev.deviceagent.app.SortMode
import dev.deviceagent.app.UiState
import dev.deviceagent.app.breadcrumbs
import dev.deviceagent.app.categoryOf
import dev.deviceagent.app.extensionBadge
import dev.deviceagent.app.formatBytes
import dev.deviceagent.app.formatWhen
import dev.deviceagent.core.fs.FileInfo

/** Shows "Internal storage" instead of the raw Android path for the main shared-storage root. */
internal fun friendlyRootName(root: String): String =
    if (root.trimEnd('/') == "/storage/emulated/0") "Internal storage" else root

/**
 * A read-only visual file browser over the same sandboxed folders the agent can use.
 * Nothing here changes files. "Use in a task" hands the selected paths to the AI tab as a draft request,
 * so every change still goes through the agent's confirmation, verification and undo.
 */
@Composable
internal fun FileBrowserScreen(
    s: UiState,
    browser: FileBrowserViewModel,
    onUseInTask: (List<String>) -> Unit,
) {
    val b by browser.state.collectAsState()
    val favorites by browser.favorites.collectAsState()
    var details by remember { mutableStateOf<FileInfo?>(null) }
    LaunchedEffect(s.roots, s.useAllFilesAccess) { browser.configure(s.roots, s.useAllFilesAccess) }
    BackHandler(enabled = b.path != null) { browser.up() }

    val inFolder = b.path != null
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Browse Files", style = MaterialTheme.typography.headlineSmall)
            if (inFolder) {
                Crumbs(b.path!!, b.root!!, onOpen = browser::open, onRoots = browser::openRoots)
                OutlinedTextField(
                    value = b.query,
                    onValueChange = browser::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Search this folder") },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { CategoryChip("All", b.category == null) { browser.setCategory(null) } }
                        items(FileCategory.entries.filter { it != FileCategory.ARCHIVES && it != FileCategory.APPS }) { c ->
                            CategoryChip(c.label, b.category == c) { browser.setCategory(if (b.category == c) null else c) }
                        }
                    }
                    SortMenu(b.sort, b.ascending, browser::setSort)
                }
                if (b.selected.isEmpty()) Hint("Tap a file for details. Press and hold to select.")
            }
            if (b.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        Box(Modifier.weight(1f)) {
            when {
                b.error != null -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppCard {
                        Text("Couldn't show this", style = MaterialTheme.typography.titleSmall)
                        Hint(b.error!!)
                        if (inFolder) OutlinedButton(onClick = browser::refresh) { Text("Try again") }
                    }
                }
                !inFolder -> RootList(b.roots, hasFolders = s.roots.isNotEmpty() || s.useAllFilesAccess, onOpen = browser::open)
                else -> FolderList(b.visible, b.selected, favorites, b.query.isNotBlank() || b.category != null, browser::open, browser::toggleSelected, browser::toggleFavorite) { details = it }
            }
        }

        if (b.selected.isNotEmpty()) {
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${b.selected.size} selected", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    TextButton(onClick = browser::clearSelection) { Text("Clear") }
                    Button(onClick = { onUseInTask(b.selected.sorted()); browser.clearSelection() }) { Text("Ask AI") }
                }
            }
        }
    }

    details?.let { e ->
        FileDetailsSheet(
            e = e,
            starred = e.path in favorites,
            onToggleFavorite = { browser.toggleFavorite(e.path) },
            onAskAi = { details = null; onUseInTask(listOf(e.path)) },
            onDismiss = { details = null },
        )
    }
}

@Composable
internal fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, shape = CircleShape)
}

@Composable
private fun SortMenu(sort: SortMode, ascending: Boolean, onSort: (SortMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(
                when (sort) { SortMode.NAME -> "Name"; SortMode.SIZE -> "Size"; SortMode.MODIFIED -> "Date" } + if (ascending) " ↑" else " ↓",
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(when (mode) { SortMode.NAME -> "Name"; SortMode.SIZE -> "Size"; SortMode.MODIFIED -> "Date modified" }) },
                    trailingIcon = { if (mode == sort) Icon(Icons.Filled.Check, contentDescription = "Selected", modifier = Modifier.size(18.dp)) },
                    onClick = { onSort(mode); open = false },
                )
            }
        }
    }
}

@Composable
private fun Crumbs(path: String, root: String, onOpen: (String) -> Unit, onRoots: () -> Unit) {
    val crumbs = breadcrumbs(path, root)
    LazyRow(verticalAlignment = Alignment.CenterVertically) {
        item { TextButton(onClick = onRoots) { Text("All folders") } }
        items(crumbs) { c ->
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onOpen(c.path) }, enabled = c.path != path) {
                Text(if (c.path == root) friendlyRootName(root) else c.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RootList(roots: List<String>, hasFolders: Boolean, onOpen: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (roots.isEmpty()) item {
            AppCard {
                Text(if (hasFolders) "Nothing to show yet" else "No folders yet", style = MaterialTheme.typography.titleSmall)
                Hint(if (hasFolders) "Your folders are loading, or Android hasn't granted access." else "Add a folder in Settings and it will appear here.")
            }
        }
        items(roots) { root ->
            Surface(
                onClick = { onOpen(root) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FileoraIcon(FileoraIconName.Folder, contentDescription = null, modifier = Modifier.size(32.dp))
                    Text(friendlyRootName(root), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderList(
    entries: List<FileInfo>,
    selected: Set<String>,
    favorites: Set<String>,
    filtering: Boolean,
    onOpen: (String) -> Unit,
    onToggle: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDetails: (FileInfo) -> Unit,
) {
    if (entries.isEmpty()) {
        Column(Modifier.padding(16.dp)) {
            AppCard {
                Text(if (filtering) "No matches" else "This folder is empty", style = MaterialTheme.typography.titleSmall)
                if (filtering) Hint("Try a shorter name or clear the filter.")
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
        items(entries, key = { it.path }) { e ->
            val isSelected = e.path in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .combinedClickable(
                        onClick = {
                            when {
                                selected.isNotEmpty() -> onToggle(e.path)
                                e.isDirectory -> onOpen(e.path)
                                else -> onDetails(e)
                            }
                        },
                        onLongClick = { onToggle(e.path) },
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EntryIcon(e)
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Hint(entrySubtitle(e))
                }
                when {
                    isSelected -> Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    selected.isEmpty() -> {
                        val starred = e.path in favorites
                        IconButton(onClick = { onToggleFavorite(e.path) }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = if (starred) "Remove from favorites" else "Add to favorites",
                                tint = if (starred) Color(0xFFF5A623) else MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        if (e.isDirectory) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

internal fun entrySubtitle(e: FileInfo): String =
    if (e.isDirectory) formatWhen(e.modifiedMs) else extensionBadge(e.name) + " · " + formatBytes(e.size) + " · " + formatWhen(e.modifiedMs)

/** Folder icon for folders; a coloured emoji tile for files, tinted by their category. */
@Composable
internal fun EntryIcon(e: FileInfo) {
    if (e.isDirectory) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            FileoraIcon(FileoraIconName.Folder, contentDescription = "Folder", modifier = Modifier.size(32.dp))
        }
    } else {
        val c = categoryOf(e.name)
        val tint = if (c != null) categoryColor(c) else MaterialTheme.colorScheme.onSurfaceVariant
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Text(c?.emoji ?: "📄", style = MaterialTheme.typography.titleMedium) }
    }
}

/** What kind of file this is, in words: "Documents", or "MKV file" when it fits no category. */
private fun typeLabel(e: FileInfo): String = categoryOf(e.name)?.label?.removeSuffix("s") ?: (extensionBadge(e.name) + " file")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileDetailsSheet(
    e: FileInfo,
    starred: Boolean,
    onToggleFavorite: () -> Unit,
    onAskAi: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                EntryIcon(e)
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Hint(typeLabel(e) + " · " + formatBytes(e.size))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow("Location", parentLabel(e.path))
            DetailRow("Size", formatBytes(e.size))
            DetailRow("Modified", formatWhen(e.modifiedMs))
            DetailRow("Type", typeLabel(e))
            Hint("Changes to this file, like moving or renaming it, are done from the AI tab so you can review and undo them.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onToggleFavorite, modifier = Modifier.weight(1f)) {
                    Text(if (starred) "Remove favorite" else "Add to favorites")
                }
                Button(onClick = onAskAi, modifier = Modifier.weight(1f)) { Text("Ask AI") }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(84.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
