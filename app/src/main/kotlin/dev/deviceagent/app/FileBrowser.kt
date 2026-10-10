package dev.deviceagent.app

import dev.deviceagent.core.fs.FileInfo

/** Pure helpers for the File Browser tab. No Android types, so they are unit-tested on the JVM. */

enum class SortMode { NAME, SIZE, MODIFIED }

/** Folders always come first; within each group the chosen sort applies, with name as the tie-breaker. */
fun sortEntries(entries: List<FileInfo>, mode: SortMode, ascending: Boolean): List<FileInfo> {
    val primary: Comparator<FileInfo> = when (mode) {
        SortMode.NAME -> compareBy<FileInfo> { it.name.lowercase() }
        SortMode.SIZE -> compareBy<FileInfo> { it.size }
        SortMode.MODIFIED -> compareBy<FileInfo> { it.modifiedMs }
    }
    val directed = if (ascending) primary else primary.reversed()
    return entries.sortedWith(
        compareBy<FileInfo> { !it.isDirectory }
            .then(directed)
            .then(compareBy<FileInfo> { it.name.lowercase() }),
    )
}

/** Case-insensitive name filter; a blank query keeps everything. */
fun filterEntries(entries: List<FileInfo>, query: String): List<FileInfo> {
    val q = query.trim()
    return if (q.isEmpty()) entries else entries.filter { it.name.contains(q, ignoreCase = true) }
}

data class Crumb(val label: String, val path: String)

/** Path segments from [root] down to [path], each tappable. [path] must be [root] or inside it. */
fun breadcrumbs(path: String, root: String): List<Crumb> {
    val cleanRoot = root.trimEnd('/')
    val cleanPath = path.trimEnd('/')
    val rootLabel = cleanRoot.substringAfterLast('/').ifEmpty { "/" }
    val crumbs = arrayListOf(Crumb(rootLabel, cleanRoot.ifEmpty { "/" }))
    if (cleanPath == cleanRoot || !cleanPath.startsWith("$cleanRoot/")) return crumbs
    var current = cleanRoot
    for (segment in cleanPath.removePrefix("$cleanRoot/").split('/').filter { it.isNotEmpty() }) {
        current = "$current/$segment"
        crumbs += Crumb(segment, current)
    }
    return crumbs
}

/** Short badge text shown in place of a file-type icon (PDF, MP4, TXT…). */
fun extensionBadge(name: String): String {
    val ext = name.substringAfterLast('.', "")
    return if (ext.isEmpty() || ext.length > 5 || name.startsWith('.') && name.count { it == '.' } == 1) "FILE" else ext.uppercase()
}
