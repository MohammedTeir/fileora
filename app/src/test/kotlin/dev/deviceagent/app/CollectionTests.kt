package dev.deviceagent.app

import dev.deviceagent.core.fs.FileInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionTests {
    private fun f(path: String, size: Long = 0, ms: Long = 0, dir: Boolean = false) =
        FileInfo(path, path.substringAfterLast('/'), dir, size, ms)

    @Test fun keysRoundTripAndRejectGarbage() {
        val all = FileCategory.entries.map { CollectionSpec(CollectionKind.CATEGORY, it) } +
            listOf(CollectionKind.DOWNLOADS, CollectionKind.RECENT, CollectionKind.FAVORITES, CollectionKind.LARGE).map { CollectionSpec(it) }
        all.forEach { assertEquals(it, CollectionSpec.fromKey(it.key)) }
        assertNull(CollectionSpec.fromKey(null))
        assertNull(CollectionSpec.fromKey("NOPE"))
        assertNull(CollectionSpec.fromKey("CATEGORY:NOPE"))
        assertNull(CollectionSpec.fromKey("CATEGORY"))
    }

    @Test fun downloadsMeansAFolderAboveTheFile() {
        assertTrue(isInDownloads("/storage/emulated/0/Download/a.pdf"))
        assertTrue(isInDownloads("/r/downloads/sub/a.pdf"))
        assertFalse(isInDownloads("/r/Download")) // the folder itself is not a file inside it
        assertFalse(isInDownloads("/r/Downloaded/a.pdf"))
        assertFalse(isInDownloads("/r/docs/Download.pdf"))
    }

    @Test fun categoryPagesTakeOnlyVisibleFilesOfThatCategory() {
        val docs = CollectionSpec(CollectionKind.CATEGORY, FileCategory.DOCUMENTS)
        assertTrue(matchesCollection(f("/r/a.PDF"), docs))
        assertFalse(matchesCollection(f("/r/a.jpg"), docs))
        assertFalse(matchesCollection(f("/r/.hidden.pdf"), docs))
        assertFalse(matchesCollection(f("/r/folder.pdf", dir = true), docs))
    }

    @Test fun recentAndLargeTakeAnyVisibleFile() {
        assertTrue(matchesCollection(f("/r/x.bin"), CollectionSpec(CollectionKind.LARGE)))
        assertFalse(matchesCollection(f("/r/.x"), CollectionSpec(CollectionKind.RECENT)))
        assertFalse(matchesCollection(f("/r/Download/x", dir = true), CollectionSpec(CollectionKind.DOWNLOADS)))
        assertFalse(matchesCollection(f("/r/Download/x"), CollectionSpec(CollectionKind.FAVORITES)))
    }

    @Test fun largestFilesAreBiggestFirstAndSkipFoldersAndHidden() {
        val list = listOf(f("/r/a", 5), f("/r/b", 50), f("/r/.c", 500), f("/r/d", 999, dir = true), f("/r/e", 20))
        assertEquals(listOf("b", "e"), largestFiles(list, 2).map { it.name })
    }

    @Test fun defaultsMatchWhatEachPageIsFor() {
        assertEquals(SortMode.SIZE to false, defaultSort(CollectionSpec(CollectionKind.LARGE)))
        assertEquals(SortMode.NAME to true, defaultSort(CollectionSpec(CollectionKind.FAVORITES)))
        assertEquals(SortMode.MODIFIED to false, defaultSort(CollectionSpec(CollectionKind.RECENT)))
    }

    @Test fun folderSummaryAddsUpFilesFoldersAndBytes() {
        val s = FolderSummary().plus(listOf(f("/r/a", 10), f("/r/b", 5), f("/r/sub", 999, dir = true))).plus(listOf(f("/r/sub/c", 7)))
        assertEquals(FolderSummary(files = 3, folders = 1, bytes = 22), s)
    }
}
