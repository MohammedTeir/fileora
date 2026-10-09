package dev.deviceagent.core.fs

import dev.deviceagent.core.tools.ErrorKind
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest

/**
 * [FileSystem] on java.nio. Every path is checked against the sandbox roots after resolving
 * symlinks, so `..` tricks and symlinks that point outside the roots are rejected.
 * Used by the unit tests and usable on Android for direct-path storage. Optional [blockedPaths]
 * add protected subtrees that remain inaccessible even when they sit inside a granted root.
 */
class JvmFileSystem(rootPaths: List<String>, blockedPaths: List<String> = emptyList()) : FileSystem {
    private val realRoots: List<Path> = rootPaths.map { Paths.get(it).toRealPath() }
    private val blockedRoots: List<Path> = blockedPaths.map { Paths.get(it).toAbsolutePath().normalize() }
    override val roots: List<String> = realRoots.map { it.toString() }

    private fun isBlocked(path: Path): Boolean = blockedRoots.any { path.toAbsolutePath().normalize().startsWith(it) }

    private fun guard(path: String): Path {
        val p = try { Paths.get(path) } catch (e: InvalidPathException) {
            throw FsException(ErrorKind.INVALID_ARGUMENTS, "Invalid path", path)
        }
        if (!p.isAbsolute) throw FsException(ErrorKind.INVALID_ARGUMENTS, "Path must be absolute", path)
        val n = p.normalize()
        if (isBlocked(n)) throw FsException(ErrorKind.OUTSIDE_SANDBOX, "Path is protected and cannot be accessed", path)
        // Resolve symlinks on the deepest entry that exists, then re-append the missing tail.
        var existing: Path = n
        val tail = ArrayList<Path>()
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            tail.add(0, existing.fileName ?: break)
            existing = existing.parent ?: break
        }
        val real = try { existing.toRealPath() } catch (e: IOException) {
            throw FsException(ErrorKind.OUTSIDE_SANDBOX, "Path cannot be resolved inside an accessible folder", path)
        }
        var resolved = real
        for (t in tail) resolved = resolved.resolve(t)
        if (realRoots.none { resolved.startsWith(it) }) {
            throw FsException(ErrorKind.OUTSIDE_SANDBOX, "Path is outside the accessible folders", path)
        }
        if (isBlocked(resolved)) throw FsException(ErrorKind.OUTSIDE_SANDBOX, "Path is protected and cannot be accessed", path)
        return n
    }

    private inline fun <T> io(path: String?, block: () -> T): T {
        try {
            return block()
        } catch (e: FsException) {
            throw e
        } catch (e: NoSuchFileException) {
            throw FsException(ErrorKind.NOT_FOUND, "No such file or directory", path ?: e.file)
        } catch (e: FileAlreadyExistsException) {
            throw FsException(ErrorKind.ALREADY_EXISTS, "Already exists", path ?: e.file)
        } catch (e: AccessDeniedException) {
            throw FsException(ErrorKind.PERMISSION_DENIED, "Permission denied", path ?: e.file)
        } catch (e: DirectoryNotEmptyException) {
            throw FsException(ErrorKind.IO, "Directory is not empty", path ?: e.file)
        } catch (e: FileSystemException) {
            val msg = e.reason ?: e.message ?: "I/O error"
            val kind = if (msg.contains("No space", ignoreCase = true)) ErrorKind.NO_SPACE else ErrorKind.IO
            throw FsException(kind, msg, path ?: e.file)
        } catch (e: IOException) {
            throw FsException(ErrorKind.IO, e.message ?: "I/O error", path)
        }
    }

    override fun normalize(path: String): String =
        try { Paths.get(path).normalize().toString() } catch (e: InvalidPathException) { path }

    override fun parent(path: String): String? = Paths.get(normalize(path)).parent?.toString()
    override fun child(path: String, name: String): String = Paths.get(path).resolve(name).normalize().toString()
    override fun name(path: String): String = Paths.get(path).fileName?.toString() ?: path

    override fun rootOf(path: String): String? {
        val n = guard(path)
        return realRoots.firstOrNull { n.startsWith(it) }?.toString()
    }

    override fun isWithin(path: String, ancestor: String): Boolean =
        Paths.get(normalize(path)).startsWith(Paths.get(normalize(ancestor)))

    private fun info(p: Path): FileInfo {
        val a = Files.readAttributes(p, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        return FileInfo(
            path = p.toString(),
            name = p.fileName?.toString() ?: p.toString(),
            isDirectory = a.isDirectory,
            size = if (a.isDirectory) 0 else a.size(),
            modifiedMs = a.lastModifiedTime().toMillis(),
            isSymlink = a.isSymbolicLink,
        )
    }

    override fun stat(path: String): FileInfo? {
        val p = guard(path)
        if (!Files.exists(p, LinkOption.NOFOLLOW_LINKS)) return null
        return io(path) { info(p) }
    }

    override fun list(path: String): List<FileInfo> {
        val p = guard(path)
        return io(path) {
            if (!Files.exists(p)) throw NoSuchFileException(path)
            if (!Files.isDirectory(p)) throw FsException(ErrorKind.INVALID_ARGUMENTS, "Not a directory", path)
            Files.newDirectoryStream(p).use { ds -> ds.filterNot { isBlocked(it) }.map { info(it) } }.sortedBy { it.name.lowercase() }
        }
    }

    override fun mkdirs(path: String) {
        val p = guard(path)
        io(path) { Files.createDirectories(p) }
    }

    override fun copyFile(src: String, dst: String) {
        val s = guard(src); val d = guard(dst)
        io(src) { Files.copy(s, d) }
    }

    override fun move(src: String, dst: String) {
        val s = guard(src); val d = guard(dst)
        io(src) { Files.move(s, d) }
    }

    override fun delete(path: String) {
        val p = guard(path)
        io(path) { Files.delete(p) }
    }

    override fun sha256(path: String): String {
        val p = guard(path)
        return io(path) {
            val md = MessageDigest.getInstance("SHA-256")
            Files.newInputStream(p).use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }
    }

    override fun readBytes(path: String, maxBytes: Int): ByteArray {
        val p = guard(path)
        return io(path) {
            Files.newInputStream(p).use { input ->
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
    }

    override fun writeBytes(path: String, bytes: ByteArray, append: Boolean) {
        val p = guard(path)
        io(path) {
            p.parent?.let { Files.createDirectories(it) }
            val options = if (append) {
                arrayOf(java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.APPEND)
            } else {
                arrayOf(java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)
            }
            Files.write(p, bytes, *options)
        }
    }

    override fun freeSpaceBytes(path: String): Long {
        var p: Path = guard(path)
        while (!Files.exists(p)) p = p.parent ?: break
        return io(path) { Files.getFileStore(p).usableSpace }
    }
}
