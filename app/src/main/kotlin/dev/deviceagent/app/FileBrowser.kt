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

// ───────── Collection pages (Documents, Images, Downloads, Recent, Favorites, Large files…) ─────────

enum class CollectionKind { CATEGORY, DOWNLOADS, RECENT, FAVORITES, LARGE }

/** Which dedicated page to show. Every page is read-only: it lists files, it never changes them. */
data class CollectionSpec(val kind: CollectionKind, val category: FileCategory? = null) {
    val title: String
        get() = when (kind) {
            CollectionKind.CATEGORY -> category?.label ?: "Files"
            CollectionKind.DOWNLOADS -> "Downloads"
            CollectionKind.RECENT -> "Recent files"
            CollectionKind.FAVORITES -> "Favorites"
            CollectionKind.LARGE -> "Large files"
        }

    val emoji: String
        get() = when (kind) {
            CollectionKind.CATEGORY -> category?.emoji ?: "📁"
            CollectionKind.DOWNLOADS -> "⬇️"
            CollectionKind.RECENT -> "🕒"
            CollectionKind.FAVORITES -> "⭐"
            CollectionKind.LARGE -> "📊"
        }

    /** Stable text form, so the open page survives a screen rotation. */
    val key: String get() = if (kind == CollectionKind.CATEGORY) "CATEGORY:${category?.name}" else kind.name

    companion object {
        fun fromKey(key: String?): CollectionSpec? {
            if (key == null) return null
            val kind = runCatching { CollectionKind.valueOf(key.substringBefore(':')) }.getOrNull() ?: return null
            if (kind != CollectionKind.CATEGORY) return CollectionSpec(kind)
            val category = runCatching { FileCategory.valueOf(key.substringAfter(':', "")) }.getOrNull() ?: return null
            return CollectionSpec(kind, category)
        }
    }
}

const val COLLECTION_TOP_LIMIT = 100
const val COLLECTION_MAX_ITEMS = 2_000
const val COLLECTION_MAX_VISITED = 20_000

/** Newest first for most pages, biggest first for Large files, A to Z for Favorites. */
fun defaultSort(spec: CollectionSpec): Pair<SortMode, Boolean> = when (spec.kind) {
    CollectionKind.LARGE -> SortMode.SIZE to false
    CollectionKind.FAVORITES -> SortMode.NAME to true
    else -> SortMode.MODIFIED to false
}

/** True if a folder named Download or Downloads is somewhere above the file. */
fun isInDownloads(path: String): Boolean =
    path.split('/').filter { it.isNotEmpty() }.dropLast(1).any { it.equals("download", true) || it.equals("downloads", true) }

/** Which scanned files belong on a page. Folders and dot-named (hidden) files never do. Favorites are not scanned. */
fun matchesCollection(e: FileInfo, spec: CollectionSpec): Boolean {
    if (e.isDirectory || isHiddenName(e.name)) return false
    return when (spec.kind) {
        CollectionKind.CATEGORY -> categoryOf(e.name) == spec.category
        CollectionKind.DOWNLOADS -> isInDownloads(e.path)
        CollectionKind.RECENT, CollectionKind.LARGE -> true
        CollectionKind.FAVORITES -> false
    }
}

/** The biggest visible files, biggest first. */
fun largestFiles(entries: List<FileInfo>, limit: Int): List<FileInfo> =
    entries.filter { !it.isDirectory && !isHiddenName(it.name) }.sortedByDescending { it.size }.take(limit)

/** Totals for the Folder details sheet. [truncated] means the scan stopped at its safety limit. */
data class FolderSummary(val files: Int = 0, val folders: Int = 0, val bytes: Long = 0, val truncated: Boolean = false)

/** Adds one folder's direct children to a running total. Files add their size; folders only count. */
fun FolderSummary.plus(children: List<FileInfo>): FolderSummary = copy(
    files = files + children.count { !it.isDirectory },
    folders = folders + children.count { it.isDirectory },
    bytes = bytes + children.filter { !it.isDirectory }.sumOf { it.size },
)
