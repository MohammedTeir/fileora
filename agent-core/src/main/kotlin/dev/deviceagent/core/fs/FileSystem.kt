package dev.deviceagent.core.fs

import dev.deviceagent.core.tools.ErrorKind

class FsException(val kind: ErrorKind, message: String, val path: String? = null) : Exception(message)

data class FileInfo(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedMs: Long,
    val isSymlink: Boolean = false,
)

/**
 * The only way tools touch storage. Implementations enforce the sandbox: every operation on a
 * path outside [roots] must throw [FsException] with kind OUTSIDE_SANDBOX.
 *
 * JVM implementation: [JvmFileSystem]. Android implementation (Storage Access Framework) lives
 * in the app module. Methods are blocking; callers run them on an IO dispatcher.
 */
interface FileSystem {
    /** Folders the agent may access. The model is told exactly this list. */
    val roots: List<String>

    fun normalize(path: String): String
    fun parent(path: String): String?
    fun child(path: String, name: String): String
    fun name(path: String): String
    fun rootOf(path: String): String?
    fun isWithin(path: String, ancestor: String): Boolean

    /** Returns null when the path does not exist. */
    fun stat(path: String): FileInfo?
    fun list(path: String): List<FileInfo>
    fun mkdirs(path: String)

    /** Copies one file; fails with ALREADY_EXISTS instead of overwriting. */
    fun copyFile(src: String, dst: String)

    /** Moves a file or directory; fails with ALREADY_EXISTS instead of overwriting. */
    fun move(src: String, dst: String)

    /** Deletes a file or an EMPTY directory. */
    fun delete(path: String)

    fun sha256(path: String): String

    /** Reads at most [maxBytes] bytes from the START of a file. Fails with NOT_FOUND / OUTSIDE_SANDBOX like every other call. */
    fun readBytes(path: String, maxBytes: Int): ByteArray

    /** Writes bytes, replacing an existing file unless [append] is true. Creates the file if missing. */
    fun writeBytes(path: String, bytes: ByteArray, append: Boolean = false)

    /** Free bytes at [path], or null when the storage cannot say (e.g. Storage Access Framework). */
    fun freeSpaceBytes(path: String): Long?
}
