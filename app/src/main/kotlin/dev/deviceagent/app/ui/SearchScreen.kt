package dev.deviceagent.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.deviceagent.app.FileBrowserViewModel
import dev.deviceagent.app.FileCategory
import dev.deviceagent.app.UiState
import dev.deviceagent.core.fs.FileInfo

private val SUGGESTED_WORDS = listOf("document", "image", "pdf", "video")
private val QUICK_CATEGORIES = listOf(FileCategory.IMAGES, FileCategory.VIDEOS, FileCategory.DOCUMENTS, FileCategory.AUDIO)

/**
 * Search is read-only: it lists matching files and folders by name. Tapping a result opens its folder in Browse.
 * Matching is by name only, so no file contents are read.
 */
@Composable
internal fun SearchScreen(
    s: UiState,
    browser: FileBrowserViewModel,
    onReveal: (FileInfo) -> Unit,
) {
    val st by browser.search.collectAsState()
    LaunchedEffect(s.roots, s.useAllFilesAccess) { browser.configure(s.roots, s.useAllFilesAccess) }
    val active = st.query.isNotBlank() || st.category != null

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Search", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = st.query,
                onValueChange = { browser.updateSearch(query = it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (active) IconButton(onClick = { browser.updateSearch(query = "", category = null) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                },
                placeholder = { Text("Search files and folders by name") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { browser.commitSearch() }),
            )
            st.category?.let { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryChip("${c.emoji} ${c.label}", true) { browser.updateSearch(category = null) }
                }
            }
            if (st.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (active) {
            Results(st.results, st.searching, st.note, st.error) { browser.commitSearch(); onReveal(it) }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SUGGESTED_WORDS) { w ->
                            AssistChip(onClick = { browser.updateSearch(query = w) }, label = { Text(w) }, shape = CircleShape)
                        }
                    }
                }
                item { Text("Quick categories", style = MaterialTheme.typography.titleMedium) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        QUICK_CATEGORIES.forEach { c ->
                            Column(
                                Modifier.clip(RoundedCornerShape(16.dp)).clickable { browser.updateSearch(category = c) }.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(categoryColor(c).copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center,
                                ) { Text(c.emoji, style = MaterialTheme.typography.titleLarge) }
                                Text(c.label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                if (st.recent.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent searches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = browser::clearRecentSearches) { Text("Clear all") }
                        }
                    }
                    items(st.recent) { r ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { browser.updateSearch(query = r) }.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(r, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    item { Hint("Searching looks at file and folder names in the folders you chose. It never reads file contents.") }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Results(
    results: List<FileInfo>,
    searching: Boolean,
    note: String?,
    error: String?,
    onClick: (FileInfo) -> Unit,
) {
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
        if (error != null) item { Column(Modifier.padding(8.dp)) { AppCard { Text("Couldn't search", style = MaterialTheme.typography.titleSmall); Hint(error) } } }
        else if (!searching && results.isEmpty()) item { Column(Modifier.padding(8.dp)) { AppCard { Text("No matches", style = MaterialTheme.typography.titleSmall); Hint("Try a shorter name, or another category.") } } }
        items(results, key = { it.path }) { e ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(16.dp)).clickable { onClick(e) }.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EntryIcon(e)
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Hint(entrySubtitle(e))
                    Text(parentLabel(e.path), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (note != null) item { Box(Modifier.padding(12.dp)) { Hint(note) } }
    }
}

/** "Internal storage › Download › Lessons" for the folder that holds [path]. */
internal fun parentLabel(path: String): String {
    val parent = path.trimEnd('/').substringBeforeLast('/', "")
    if (parent.isEmpty()) return "/"
    val shown = if (parent.startsWith("/storage/emulated/0")) "Internal storage" + parent.removePrefix("/storage/emulated/0") else parent
    return shown.trimStart('/').replace("/", " › ")
}
