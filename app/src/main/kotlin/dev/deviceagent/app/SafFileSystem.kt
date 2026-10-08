package dev.deviceagent.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import dev.deviceagent.core.fs.FileInfo
import dev.deviceagent.core.fs.FileSystem
import dev.deviceagent.core.fs.FsException
import dev.deviceagent.core.tools.ErrorKind
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest

/** A folder the user granted through the system folder picker. [name] is how the agent sees it. */
class SafRoot(val name: String, val treeUri: Uri)

/**
 * [FileSystem] over the Storage Access Framework. This is the only storage the app can rely on
 * across Android versions without broad permissions: the agent can touch exactly the folders
 * the user picked, and nothing else. Paths look like `/<rootName>/sub/folder/file.ext`.
 *
 * Limits that are inherent to SAF (and the reason the core treats them as "unknown"):
 *  - no free-space query            -> [freeSpaceBytes] returns null
 *  - no symlinks                    -> [FileInfo.isSymlink] is always false
 *  - lookups are linear per folder  -> very large folders are slow
 *  - folder moves need provider support for DocumentsContract.moveDocument
 *
 * NOTE: written against the documented API; not yet compiled or run on a device.
 */
class SafFileSystem(private val context: Context, safRoots: List<SafRoot>) : FileSystem {
    private val resolver = context.contentResolver
    private val byName = safRoots.associateBy { it.name }
    override val roots: List<String> = safRoots.map { "/${it.name}" }

    // ───────── pure path helpers ─────────

    private fun segments(path: String): List<String> {
        val out = ArrayList<String>()
        for (s in path.split('/')) when (s) {
            "", "." -> Unit
            ".." -> if (out.isNotEmpty()) out.removeAt(out.size - 1)
            else -> out += s
        }
        return out
    }

    override fun normalize(path: String) = "/" + segments(path).joinToString("/")
    override fun parent(path: String): String? = segments(path).let { if (it.size <= 1) null else "/" + it.dropLast(1).joinToString("/") }
    override fun child(path: String, name: String) = normalize("$path/$name")
    override fun name(path: String) = segments(path).lastOrNull() ?: "/"
    override fun rootOf(path: String): String? = segments(path).firstOrNull()?.takeIf { it in byName }?.let { "/$it" }
    override fun isWithin(path: String, ancestor: String): Boolean {
        val p = normalize(path); val a = normalize(ancestor)
        return p == a || p.startsWith("$a/")
    }

    // ───────── resolving paths to documents ─────────

    private fun locate(path: String): Pair<SafRoot, List<String>> {
        if (!path.startsWith("/")) throw FsException(ErrorKind.INVALID_ARGUMENTS, "Path must be absolute", path)
        val segs = segments(path)
        val root = segs.firstOrNull()?.let { byName[it] }
            ?: throw FsException(ErrorKind.OUTSIDE_SANDBOX, "Path is outside the folders you granted", path)
        return root to segs.drop(1)
    }

    /** The document at [path], or null if some segment does not exist. */
    private fun doc(path: String): DocumentFile? {
        val (root, rel) = locate(path)
        var cur: DocumentFile = DocumentFile.fromTreeUri(context, root.treeUri)
            ?: throw FsException(ErrorKind.PERMISSION_DENIED, "Access to this folder is no longer granted", path)
        for (seg in rel) cur = cur.findFile(seg) ?: return null
        return cur
    }

    private fun mustDoc(path: String): DocumentFile = doc(path) ?: throw FsException(ErrorKind.NOT_FOUND, "No such file or directory", path)

    private inline fun <T> io(path: String?, block: () -> T): T = try {
        block()
    } catch (e: FsException) {
        throw e
    } catch (e: SecurityException) {
        throw FsException(ErrorKind.PERMISSION_DENIED, "Permission denied", path)
    } catch (e: FileNotFoundException) {
        throw FsException(ErrorKind.NOT_FOUND, "No such file or directory", path)
    } catch (e: IOException) {
        val msg = e.message ?: "I/O error"
        throw FsException(if (msg.contains("ENOSPC") || msg.contains("No space", true)) ErrorKind.NO_SPACE else ErrorKind.IO, msg, path)
    } catch (e: UnsupportedOperationException) {
        throw FsException(ErrorKind.IO, "This storage location does not support that operation", path)
    } catch (e: IllegalArgumentException) {
        throw FsException(ErrorKind.IO, e.message ?: "Invalid operation", path)
    } catch (e: IllegalStateException) {
        throw FsException(ErrorKind.IO, e.message ?: "Invalid state", path)
    }

    private fun info(path: String, d: DocumentFile) = FileInfo(
        path = normalize(path),
        name = d.name ?: name(path),
        isDirectory = d.isDirectory,
        size = if (d.isDirectory) 0 else d.length(),
        modifiedMs = d.lastModified(),
        isSymlink = false,
    )

    // ───────── FileSystem operations ─────────

    override fun stat(path: String): FileInfo? = io(path) { doc(path)?.let { info(path, it) } }

    override fun list(path: String): List<FileInfo> = io(path) {
        val d = mustDoc(path)
        if (!d.isDirectory) throw FsException(ErrorKind.INVALID_ARGUMENTS, "Not a directory", path)
        d.listFiles().mapNotNull { c -> c.name?.let { info(child(path, it), c) } }.sortedBy { it.name.lowercase() }
    }

    override fun mkdirs(path: String) {
        io(path) {
            val (root, rel) = locate(path)
            var cur: DocumentFile = DocumentFile.fromTreeUri(context, root.treeUri)
                ?: throw FsException(ErrorKind.PERMISSION_DENIED, "Access to this folder is no longer granted", path)
            for (seg in rel) {
                val existing = cur.findFile(seg)
                cur = when {
                    existing == null -> cur.createDirectory(seg) ?: throw FsException(ErrorKind.IO, "Could not create folder '$seg'", path)
                    existing.isDirectory -> existing
                    else -> throw FsException(ErrorKind.ALREADY_EXISTS, "A file named '$seg' already exists", path)
                }
            }
        }
    }

    override fun copyFile(src: String, dst: String) {
        io(src) {
            val s = mustDoc(src)
            if (s.isDirectory) throw FsException(ErrorKind.INVALID_ARGUMENTS, "Source is a directory", src)
            val parentPath = parent(dst) ?: throw FsException(ErrorKind.INVALID_ARGUMENTS, "Destination has no parent folder", dst)
            val parentDoc = doc(parentPath) ?: throw FsException(ErrorKind.NOT_FOUND, "Destination folder does not exist", parentPath)
            val newName = name(dst)
            if (parentDoc.findFile(newName) != null) throw FsException(ErrorKind.ALREADY_EXISTS, "Already exists", dst)
            val created = parentDoc.createFile(s.type ?: "application/octet-stream", newName)
                ?: throw FsException(ErrorKind.IO, "Could not create the destination file", dst)
            try {
                // Some providers append an extension for known MIME types; make the name exact.
                if (created.name != newName && !created.renameTo(newName)) throw FsException(ErrorKind.IO, "Could not give the copy its exact name", dst)
                val input = resolver.openInputStream(s.uri) ?: throw FsException(ErrorKind.IO, "Could not open the source", src)
                val output = resolver.openOutputStream(created.uri, "w") ?: throw FsException(ErrorKind.IO, "Could not open the destination", dst)
                input.use { i -> output.use { o -> i.copyTo(o, 64 * 1024) } }
            } catch (e: Throwable) {
                created.delete() // never leave a half-written file behind
                throw e
            }
        }
    }

    override fun move(src: String, dst: String) {
        io(src) {
            val s = mustDoc(src)
            if (doc(dst) != null) throw FsException(ErrorKind.ALREADY_EXISTS, "Already exists", dst)
            val srcParentPath = parent(src) ?: throw FsException(ErrorKind.INVALID_ARGUMENTS, "Cannot move a root folder", src)
            val dstParentPath = parent(dst) ?: throw FsException(ErrorKind.INVALID_ARGUMENTS, "Destination has no parent folder", dst)
            val dstParent = doc(dstParentPath) ?: throw FsException(ErrorKind.NOT_FOUND, "Destination folder does not exist", dstParentPath)
            val newName = name(dst)
            if (normalize(srcParentPath) == normalize(dstParentPath)) {
                if (!s.renameTo(newName)) throw FsException(ErrorKind.IO, "Rename failed", src)
                return@io
            }
            val srcParent = mustDoc(srcParentPath)
            val moved: Uri? = try {
                DocumentsContract.moveDocument(resolver, s.uri, srcParent.uri, dstParent.uri)
            } catch (e: Exception) {
                null // provider does not support move
            }
            if (moved != null) {
                if (name(dst) != (DocumentFile.fromSingleUri(context, moved)?.name ?: newName)) {
                    DocumentFile.fromSingleUri(context, moved)?.renameTo(newName)
                }
            } else if (!s.isDirectory) {
                copyFile(src, dst)
                if (!s.delete()) { delete(dst); throw FsException(ErrorKind.IO, "Copied but could not remove the original", src) }
            } else {
                throw FsException(ErrorKind.IO, "This storage location cannot move folders between directories", src)
            }
        }
    }

    override fun delete(path: String) {
        io(path) {
            val d = mustDoc(path)
            if (d.isDirectory && d.listFiles().isNotEmpty()) throw FsException(ErrorKind.IO, "Directory is not empty", path)
            if (!d.delete()) throw FsException(ErrorKind.IO, "Could not delete", path)
        }
    }

    override fun sha256(path: String): String = io(path) {
        val d = mustDoc(path)
        val md = MessageDigest.getInstance("SHA-256")
        (resolver.openInputStream(d.uri) ?: throw FsException(ErrorKind.IO, "Could not open the file", path)).use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }

    override fun readBytes(path: String, maxBytes: Int): ByteArray = io(path) {
        val d = mustDoc(path)
        (resolver.openInputStream(d.uri) ?: throw FsException(ErrorKind.IO, "Could not open the file", path)).use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            var left = maxBytes
            while (left > 0) {
                val n = input.read(buf, 0, minOf(buf.size, left))
                if (n < 0) break
                out.write(buf, 0, n)
                left -= n
            }
            out.toByteArray()
        }
    }

    /** SAF offers no free-space query. */
    override fun freeSpaceBytes(path: String): Long? = null
}
