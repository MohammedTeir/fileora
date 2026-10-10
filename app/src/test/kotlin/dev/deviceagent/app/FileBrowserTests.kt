package dev.deviceagent.app

import dev.deviceagent.core.fs.FileInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class FileBrowserTests {
    private fun f(name: String, size: Long = 0, ms: Long = 0, dir: Boolean = false) =
        FileInfo("/r/$name", name, dir, size, ms)

    private val sample = listOf(
        f("b.txt", size = 5, ms = 20), f("A.pdf", size = 50, ms = 10), f("zdir", dir = true, ms = 5), f("adir", dir = true, ms = 30),
    )

    @Test fun foldersAlwaysComeFirst() {
        assertEquals(listOf("adir", "zdir", "A.pdf", "b.txt"), sortEntries(sample, SortMode.NAME, true).map { it.name })
        assertEquals(listOf("zdir", "adir", "b.txt", "A.pdf"), sortEntries(sample, SortMode.NAME, false).map { it.name })
    }

    @Test fun sortBySizeAndDate() {
        assertEquals(listOf("b.txt", "A.pdf"), sortEntries(sample, SortMode.SIZE, true).filter { !it.isDirectory }.map { it.name })
        assertEquals(listOf("b.txt", "A.pdf"), sortEntries(sample, SortMode.MODIFIED, false).filter { !it.isDirectory }.map { it.name })
    }

    @Test fun filterIsCaseInsensitiveAndBlankKeepsAll() {
        assertEquals(listOf("A.pdf"), filterEntries(sample, "PDF").map { it.name })
        assertEquals(4, filterEntries(sample, "  ").size)
    }

    @Test fun breadcrumbsWalkFromTheRoot() {
        assertEquals(listOf("Download"), breadcrumbs("/Download", "/Download").map { it.label })
        val crumbs = breadcrumbs("/Download/Lessons/L1", "/Download")
        assertEquals(listOf("Download", "Lessons", "L1"), crumbs.map { it.label })
        assertEquals("/Download/Lessons", crumbs[1].path)
    }

    @Test fun extensionBadgeHandlesOddNames() {
        assertEquals("PDF", extensionBadge("a.pdf"))
        assertEquals("FILE", extensionBadge("README"))
        assertEquals("FILE", extensionBadge(".hidden"))
        assertEquals("GZ", extensionBadge("x.tar.gz"))
    }

    @Test fun categoriesAreDetectedFromTheExtension() {
        assertEquals(FileCategory.DOCUMENTS, categoryOf("Report.PDF"))
        assertEquals(FileCategory.VIDEOS, categoryOf("a.b.mkv"))
        assertEquals(null, categoryOf("README"))
        assertEquals(null, categoryOf("weird.xyz"))
    }

    @Test fun categoryFilterKeepsFoldersButOnlyMatchingFiles() {
        // sample = b.txt, A.pdf, zdir, adir: both files are documents, both folders always stay.
        assertEquals(listOf("b.txt", "A.pdf", "zdir", "adir"), filterEntries(sample, "", FileCategory.DOCUMENTS).map { it.name })
        // No image files in the sample, so only the folders remain.
        assertEquals(listOf("zdir", "adir"), filterEntries(sample, "", FileCategory.IMAGES).map { it.name })
    }

    @Test fun searchNeedsAQueryOrACategoryAndSkipsFoldersForCategories() {
        assertEquals(false, matchesSearch(f("a.pdf"), "", null))
        assertEquals(true, matchesSearch(f("Plan.pdf"), "plan", null))
        assertEquals(true, matchesSearch(f("docs", dir = true), "doc", null))
        assertEquals(false, matchesSearch(f("docs", dir = true), "", FileCategory.DOCUMENTS))
        assertEquals(true, matchesSearch(f("x.jpg"), "", FileCategory.IMAGES))
    }
}
