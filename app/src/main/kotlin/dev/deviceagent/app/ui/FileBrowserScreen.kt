package dev.deviceagent.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.deviceagent.app.FileBrowserViewModel
import dev.deviceagent.app.SortMode
import dev.deviceagent.app.UiState
import dev.deviceagent.app.breadcrumbs
import dev.deviceagent.app.extensionBadge
import dev.deviceagent.app.formatBytes
import dev.deviceagent.app.formatWhen
import dev.deviceagent.core.fs.FileInfo

/**
 * A read-only visual file browser over the same sandboxed folders the agent can use.
 * Nothing here changes files. "Use in a task" hands the selected paths to Home as a draft goal,
 * so every change still goes through the agent's confirmation, verification and undo.
 */
@Composable
internal fun FileBrowserScreen(
    s: UiState,
    browser: FileBrowserViewModel,
    onUseInTask: (List<String>) -> Unit,
) {
    val b by browser.state.collectAsState()
    LaunchedEffect(s.roots, s.useAllFilesAccess) { browser.configure(s.roots, s.useAllFilesAccess) }
    BackHandler(enabled = b.path != null) { browser.up() }

    val inFolder = b.path != null
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenTitle("Files", "Look around. To change anything, ask me from Home.")
            if (inFolder) {
                Crumbs(b.path!!, b.root!!, onOpen = browser::open, onRoots = browser::openRoots)
                OutlinedTextField(
                    value = b.query,
                    onValueChange = browser::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Filter this folder by name") },
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SortMode.entries.toList()) { mode ->
                        val label = when (mode) { SortMode.NAME -> "Name"; SortMode.SIZE -> "Size"; SortMode.MODIFIED -> "Date" }
                        FilterChip(
                            selected = b.sort == mode,
                            onClick = { browser.setSort(mode) },
                            label = { Text(if (b.sort == mode) label + if (b.ascending) " ↑" else " ↓" else label) },
                        )
                    }
                }
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
                else -> FolderList(b.visible, b.selected, b.query.isNotBlank(), browser::open, browser::toggleSelected)
            }
        }

        if (b.selected.isNotEmpty()) {
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${b.selected.size} selected", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    TextButton(onClick = browser::clearSelection) { Text("Clear") }
                    Button(onClick = { onUseInTask(b.selected.sorted()); browser.clearSelection() }) { Text("Use in a task") }
                }
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
                Text(c.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            AppCard(Modifier.clickable { onOpen(root) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FileoraIcon(FileoraIconName.Folder, contentDescription = null, modifier = Modifier.size(28.dp))
                    Text(root, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun FolderList(
    entries: List<FileInfo>,
    selected: Set<String>,
    filtering: Boolean,
    onOpen: (String) -> Unit,
    onToggle: (String) -> Unit,
) {
    if (entries.isEmpty()) {
        Column(Modifier.padding(16.dp)) {
            AppCard {
                Text(if (filtering) "No matches" else "This folder is empty", style = MaterialTheme.typography.titleSmall)
                if (filtering) Hint("Try a shorter name.")
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
        items(entries, key = { it.path }) { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { if (selected.isNotEmpty() || !e.isDirectory) onToggle(e.path) else onOpen(e.path) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Checkbox(checked = e.path in selected, onCheckedChange = { onToggle(e.path) })
                if (e.isDirectory) {
                    FileoraIcon(FileoraIconName.Folder, contentDescription = "Folder", modifier = Modifier.size(28.dp))
                } else {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(width = 44.dp, height = 28.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(extensionBadge(e.name), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Hint(if (e.isDirectory) formatWhen(e.modifiedMs) else formatBytes(e.size) + " · " + formatWhen(e.modifiedMs))
                }
            }
        }
    }
}
