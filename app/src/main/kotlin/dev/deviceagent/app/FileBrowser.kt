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

/** Broad kinds of files, used by the category chips, the Home categories and Search. */
enum class FileCategory(val label: String, val emoji: String, val extensions: Set<String>) {
    DOCUMENTS("Documents", "\uD83D\uDCC4", setOf("pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "csv", "ppt", "pptx", "md")),
    IMAGES("Images", "\uD83D\uDDBC\uFE0F", setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "bmp")),
    VIDEOS("Videos", "\uD83C\uDFAC", setOf("mp4", "mkv", "mov", "avi", "webm", "3gp")),
    AUDIO("Audio", "\uD83C\uDFB5", setOf("mp3", "m4a", "wav", "flac", "ogg", "aac", "opus")),
    ARCHIVES("Archives", "\uD83D\uDDDC\uFE0F", setOf("zip", "rar", "7z", "tar", "gz")),
    APPS("Apps", "\uD83D\uDCE6", setOf("apk", "xapk")),
}

/** The category a file name belongs to, or null if it fits none of them. */
fun categoryOf(name: String): FileCategory? {
    val ext = name.substringAfterLast('.', "").lowercase()
    if (ext.isEmpty()) return null
    return FileCategory.entries.firstOrNull { ext in it.extensions }
}

/**
 * Case-insensitive name filter; a blank query keeps everything.
 * With a [category], folders stay visible (so you can keep navigating) and files must match it.
 */
fun filterEntries(entries: List<FileInfo>, query: String, category: FileCategory? = null): List<FileInfo> {
    val q = query.trim()
    return entries.filter { e ->
        (q.isEmpty() || e.name.contains(q, ignoreCase = true)) &&
            (category == null || e.isDirectory || categoryOf(e.name) == category)
    }
}

/** True if [e] should be listed for a search. With a category, only files of that category match. */
fun matchesSearch(e: FileInfo, query: String, category: FileCategory?): Boolean {
    val q = query.trim()
    if (q.isEmpty() && category == null) return false
    if (q.isNotEmpty() && !e.name.contains(q, ignoreCase = true)) return false
    if (category != null && (e.isDirectory || categoryOf(e.name) != category)) return false
    return true
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

/** Dot-names (".config", ".thumbnails") are system or app clutter, not something the user would call recent. */
fun isHiddenName(name: String): Boolean = name.startsWith('.')

/** The newest visible files, newest first. Folders and hidden names are skipped. */
fun mostRecentFiles(entries: List<FileInfo>, limit: Int): List<FileInfo> =
    entries.filter { !it.isDirectory && !isHiddenName(it.name) }.sortedByDescending { it.modifiedMs }.take(limit)
