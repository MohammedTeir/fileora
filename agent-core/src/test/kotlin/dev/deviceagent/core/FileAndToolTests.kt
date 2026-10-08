package dev.deviceagent.core

import dev.deviceagent.core.fs.FileSystem
import dev.deviceagent.core.fs.FsException
import dev.deviceagent.core.agent.PrivacyMode
import dev.deviceagent.core.history.UndoManager
import dev.deviceagent.core.tools.ErrorKind
import dev.deviceagent.core.tools.ToolContext
import dev.deviceagent.core.tools.ToolRegistry
import dev.deviceagent.core.tools.file.FileCopyTool
import dev.deviceagent.core.tools.file.FileCompressTool
import dev.deviceagent.core.tools.file.FileContentSearchTool
import dev.deviceagent.core.tools.file.FileAppendTool
import dev.deviceagent.core.tools.file.FileDeleteTool
import dev.deviceagent.core.tools.file.FileExtractTool
import dev.deviceagent.core.tools.file.FileListTool
import dev.deviceagent.core.tools.file.FileMoveTool
import dev.deviceagent.core.tools.file.FileRenameTool
import dev.deviceagent.core.tools.file.FileSearchTool
import dev.deviceagent.core.tools.file.FileStatTool
import dev.deviceagent.core.tools.file.FileWriteTool
import dev.deviceagent.core.tools.file.WRITE_MAX_BYTES
import dev.deviceagent.core.tools.media.MediaBackend
import dev.deviceagent.core.tools.media.MediaConversion
import dev.deviceagent.core.tools.media.MediaConvertTool
import dev.deviceagent.core.tools.media.MediaProbe
import dev.deviceagent.core.tools.media.MediaProbeTool
import dev.deviceagent.core.tools.system.DefaultTools
import dev.deviceagent.core.util.long
import dev.deviceagent.core.util.str
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import java.nio.file.Paths

private class UnknownSpaceFs(private val d: FileSystem) : FileSystem by d {
    override fun freeSpaceBytes(path: String): Long? = null
}

private fun expectFs(kind: ErrorKind, block: () -> Unit) {
    try { block() } catch (e: FsException) {
        if (e.kind != kind) throw TestFailure("expected $kind but got ${e.kind}: ${e.message}")
        return
    }
    throw TestFailure("expected FsException($kind) but nothing was thrown")
}

fun registerFileAndToolTests() {
    // ───────── sandbox ─────────

    test("sandbox: path outside the roots is rejected") {
        fx { f ->
            expectFs(ErrorKind.OUTSIDE_SANDBOX) { f.fs.stat(f.outside.toString()) }
            expectFs(ErrorKind.OUTSIDE_SANDBOX) { f.fs.list("/etc") }
        }
    }

    test("sandbox: '..' traversal out of the root is rejected") {
        fx { f ->
            expectFs(ErrorKind.OUTSIDE_SANDBOX) { f.fs.stat(f.p("../outside")) }
            expectFs(ErrorKind.OUTSIDE_SANDBOX) { f.fs.mkdirs(f.p("a/../../outside/x")) }
        }
    }

    test("sandbox: a symlink that points outside the root is rejected") {
        fx { f ->
            Files.writeString(f.outside.resolve("secret.txt"), "top secret")
            Files.createSymbolicLink(f.root.resolve("link"), f.outside)
            expectFs(ErrorKind.OUTSIDE_SANDBOX) { f.fs.list(f.p("link")) }
            expectFs(ErrorKind.OUTSIDE_SANDBOX) { f.fs.sha256(f.p("link/secret.txt")) }
            expectFs(ErrorKind.OUTSIDE_SANDBOX) { f.fs.copyFile(f.p("link/secret.txt"), f.p("stolen.txt")) }
            check(!f.exists("stolen.txt")) { "file escaped the sandbox" }
        }
    }

    test("sandbox: relative paths are refused") {
        fx { f -> expectFs(ErrorKind.INVALID_ARGUMENTS) { f.fs.stat("phone/a.txt") } }
    }

    // ───────── read tools ─────────

    test("file.list returns names, types and sizes, sorted, and hides the trash") {
        fx { f ->
            f.file("b.txt", "12345"); f.file("A.txt", "1"); f.dir("sub"); f.dir(".agent_trash")
            val r = FileListTool().execute(args("path" to f.root.toString()), ToolContext("t", f.fs))
            check(r.ok)
            val names = r.data["entries"]!!.jsonArray.map { it.jsonObject.str("name")!! }
            eq(listOf("A.txt", "b.txt", "sub"), names, "entries")
            eq(3, r.data.long("total")!!.toInt(), "total")
        }
    }

    test("file.list on a missing folder fails with NOT_FOUND") {
        fx { f ->
            val r = FileListTool().execute(args("path" to f.p("nope")), ToolContext("t", f.fs))
            check(!r.ok); eq(ErrorKind.NOT_FOUND, r.error!!.kind)
        }
    }

    test("file.search: glob, substring, kind filter, recursion") {
        fx { f ->
            f.file("Lessons/L1/talk.mp4"); f.file("Lessons/L2/Talk.MP4"); f.file("Lessons/L2/notes.txt"); f.dir("Lessons/mp4stuff")
            val t = ToolContext("t", f.fs)
            val glob = FileSearchTool().execute(args("root" to f.root.toString(), "pattern" to "*.mp4"), t)
            eq(2, glob.data["matches"]!!.jsonArray.size, "glob matches (case-insensitive)")
            val sub = FileSearchTool().execute(args("root" to f.root.toString(), "pattern" to "mp4", "kind" to "directory"), t)
            eq(1, sub.data["matches"]!!.jsonArray.size, "directory-only matches")
            val capped = FileSearchTool().execute(args("root" to f.root.toString(), "pattern" to "*", "max_results" to 2), t)
            check(capped.data["truncated"].toString() == "true") { "should report truncation" }
        }
    }

    test("file.search_content finds text lines, skips binary files, and reports bounds") {
        fx { f ->
            f.file("docs/a.txt", "first\nNeedle appears here\nlast")
            f.file("docs/b.txt", "needle again")
            Files.write(f.root.resolve("docs/image.bin"), byteArrayOf(0, 1, 2, 3))
            val result = FileContentSearchTool().execute(
                args("root" to f.p("docs"), "query" to "NEEDLE"),
                ToolContext("t", f.fs, PrivacyMode.CONTENTS_ALLOWED),
            )
            check(result.ok) { result.error.toString() }
            eq(2, result.data["matches"]!!.jsonArray.size, "content matches")
            eq(1L, result.data.long("skipped_binary"), "binary count")
        }
    }

    test("file.search_content is blocked by names-only privacy mode") {
        fx { f ->
            f.file("private.txt", "secret needle")
            val result = FileContentSearchTool().execute(
                args("root" to f.root.toString(), "query" to "needle"),
                ToolContext("t", f.fs, PrivacyMode.NAMES_ONLY),
            )
            eq(ErrorKind.INVALID_ARGUMENTS, result.error!!.kind, "privacy block")
            check(!result.ok)
        }
    }

    test("media tools use the host backend and verify a newly created output") {
        fx { f ->
            val source = f.file("input.media", "media")
            val backend = object : MediaBackend {
                override fun probe(path: String) = MediaProbe("fake", durationSeconds = 1.5, playable = true)
                override fun convert(source: String, destination: String, format: String?): MediaConversion {
                    f.fs.writeBytes(destination, "converted".toByteArray())
                    return MediaConversion(format ?: "fake", playable = true, bytesWritten = 9)
                }
            }
            val t = ToolContext("media", f.fs, mediaBackend = backend)
            val probe = MediaProbeTool().execute(args("path" to source), t)
            check(probe.ok); eq("fake", probe.data.str("format"), "format")
            val converted = MediaConvertTool().execute(args("source" to source, "destination" to f.p("output.media"), "format" to "fake"), t)
            check(converted.ok) { converted.error.toString() }
            check(f.exists("output.media")); eq(1, converted.undo.size, "output undo")
        }
    }

    test("media tools fail clearly when no host backend is available") {
        fx { f ->
            f.file("input.media", "media")
            val result = MediaProbeTool().execute(args("path" to f.p("input.media")), ToolContext("media", f.fs))
            eq(ErrorKind.INVALID_ARGUMENTS, result.error!!.kind, "backend unavailable")
        }
    }

    test("file.stat reports existence, and folder totals") {
        fx { f ->
            f.file("d/a.txt", "123"); f.file("d/b.txt", "45")
            val t = ToolContext("t", f.fs)
            val dirInfo = FileStatTool().execute(args("path" to f.p("d")), t)
            eq(5L, dirInfo.data.long("size"), "folder size"); eq(2L, dirInfo.data.long("files"), "folder file count")
            eq("false", FileStatTool().execute(args("path" to f.p("zzz")), t).data["exists"].toString(), "exists")
        }
    }

    // ───────── mutating tools ─────────

    test("file.write creates UTF-8 text, reports a checksum, and can be undone") {
        fx { f ->
            val path = f.p("hello.txt")
            val t = ToolContext("t", f.fs)
            val plan = FileWriteTool().plan(args("path" to path, "content" to "héllo"), t)
            check(plan.error == null)
            val r = FileWriteTool().execute(args("path" to path, "content" to "héllo"), t)
            check(r.ok) { r.error.toString() }
            eq("héllo", f.read("hello.txt"), "written content")
            check(r.data.str("sha256")!!.length == 64) { "write must report SHA-256" }
            val report = UndoManager().revert(r.undo, f.fs)
            eq(1, report.reverted, "undo count")
            check(!f.exists("hello.txt")) { "undo removes a newly created file" }
        }
    }

    test("file.append appends text and overwrite restores the old version") {
        fx { f ->
            f.file("log.txt", "one")
            val t = ToolContext("t", f.fs)
            val append = FileAppendTool().execute(args("path" to f.p("log.txt"), "content" to "\ntwo"), t)
            check(append.ok); eq("one\ntwo", f.read("log.txt"), "appended content")
            val write = FileWriteTool().execute(args("path" to f.p("log.txt"), "content" to "replacement"), t)
            check(write.ok); eq("replacement", f.read("log.txt"), "replaced content")
            val report = UndoManager().revert(write.undo, f.fs)
            eq(1, report.reverted, "undo overwrite count")
            eq("one\ntwo", f.read("log.txt"), "restored old content")
        }
    }

    test("file.write rejects oversized content before changing the file") {
        fx { f ->
            f.file("safe.txt", "keep")
            val t = ToolContext("t", f.fs)
            val content = "x".repeat(WRITE_MAX_BYTES + 1)
            val plan = FileWriteTool().plan(args("path" to f.p("safe.txt"), "content" to content), t)
            eq(ErrorKind.INVALID_ARGUMENTS, plan.error!!.kind)
            eq("keep", f.read("safe.txt"), "oversized write must be side-effect free")
        }
    }

    test("file.copy copies content, creates parents, and records undo") {
        fx { f ->
            val src = f.file("in/a.mp3", "audio-bytes")
            val t = ToolContext("t", f.fs)
            val r = FileCopyTool().execute(args("source" to src, "destination" to f.p("out/deep/a.mp3")), t)
            check(r.ok) { r.error.toString() }
            eq("audio-bytes", f.read("out/deep/a.mp3"), "copied content")
            check(f.exists("in/a.mp3")) { "source must remain" }
            eq(3, r.undo.size, "undo: 2 new folders + 1 file")
        }
    }

    test("file.copy never overwrites (plan fails with ALREADY_EXISTS)") {
        fx { f ->
            val src = f.file("a.txt", "new"); f.file("b.txt", "precious")
            val plan = FileCopyTool().plan(args("source" to src, "destination" to f.p("b.txt")), ToolContext("t", f.fs))
            eq(ErrorKind.ALREADY_EXISTS, plan.error!!.kind)
            eq("precious", f.read("b.txt"), "existing file untouched")
        }
    }

    test("file.copy of a folder copies the whole tree and skips the trash") {
        fx { f ->
            f.file("src/a.txt", "1"); f.file("src/sub/b.txt", "22"); f.file("src/.agent_trash/old", "x")
            val t = ToolContext("t", f.fs)
            val args = args("source" to f.p("src"), "destination" to f.p("dst"))
            val plan = FileCopyTool().plan(args, t)
            eq(2, plan.fileCount, "planned file count"); eq(3L, plan.bytes, "planned bytes")
            check(FileCopyTool().execute(args, t).ok)
            check(f.exists("dst/sub/b.txt")); check(!f.exists("dst/.agent_trash")) { "trash must not be copied" }
        }
    }

    test("file.copy refuses to copy a folder into itself") {
        fx { f ->
            f.file("src/a.txt")
            val plan = FileCopyTool().plan(args("source" to f.p("src"), "destination" to f.p("src/inner")), ToolContext("t", f.fs))
            eq(ErrorKind.INVALID_ARGUMENTS, plan.error!!.kind)
        }
    }

    test("file.compress and file.extract round-trip a folder with bounded archive output") {
        fx { f ->
            f.file("source/a.txt", "alpha")
            f.file("source/sub/b.txt", "beta")
            val t = ToolContext("archive", f.fs)
            val zip = f.p("bundle.zip").toString()
            val compressed = FileCompressTool().execute(args("source" to f.p("source"), "destination" to zip), t)
            check(compressed.ok) { "compression failed: ${compressed.error}" }
            check(f.exists("bundle.zip")) { "archive was not created" }
            val extracted = FileExtractTool().execute(args("archive" to zip, "destination" to f.p("out")), t)
            check(extracted.ok) { "extraction failed: ${extracted.error}" }
            check(f.exists("out/source/a.txt") && f.exists("out/source/sub/b.txt")) { "extracted files missing" }
            eq("alpha", Files.readString(f.root.resolve("out/source/a.txt")), "extracted content")
        }
    }

    test("file.extract rejects zip-slip entries before writing outside destination") {
        fx { f ->
            val zipPath = f.root.resolve("unsafe.zip")
            java.util.zip.ZipOutputStream(Files.newOutputStream(zipPath)).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("../../escaped.txt"))
                zip.write("nope".toByteArray())
                zip.closeEntry()
            }
            val result = FileExtractTool().execute(args("archive" to zipPath.toString(), "destination" to f.p("out")), ToolContext("archive", f.fs))
            eq(ErrorKind.INVALID_ARGUMENTS, result.error!!.kind, "zip-slip error")
            check(!f.exists("out/escaped.txt")) { "unsafe entry was written" }
        }
    }

    test("file.copy plan fails when the missing source does not exist") {
        fx { f ->
            val plan = FileCopyTool().plan(args("source" to f.p("ghost"), "destination" to f.p("x")), ToolContext("t", f.fs))
            eq(ErrorKind.NOT_FOUND, plan.error!!.kind)
        }
    }

    test("file.move and file.rename work, never overwrite, and can be undone") {
        fx { f ->
            f.file("a.txt", "A"); f.file("keep.txt", "K")
            val t = ToolContext("t", f.fs)
            val mv = FileMoveTool().execute(args("source" to f.p("a.txt"), "destination" to f.p("box/a.txt")), t)
            check(mv.ok); check(!f.exists("a.txt") && f.exists("box/a.txt"))
            val rn = FileRenameTool().execute(args("path" to f.p("box/a.txt"), "new_name" to "z.txt"), t)
            check(rn.ok); check(f.exists("box/z.txt"))
            val bad = FileRenameTool().plan(args("path" to f.p("box/z.txt"), "new_name" to "../keep.txt"), t)
            eq(ErrorKind.INVALID_ARGUMENTS, bad.error!!.kind, "new_name with a slash is refused")
            val clash = FileMoveTool().plan(args("source" to f.p("box/z.txt"), "destination" to f.p("keep.txt")), t)
            eq(ErrorKind.ALREADY_EXISTS, clash.error!!.kind)
            // undo restores the original layout, including removing the folder the move created
            val report = UndoManager().revert(mv.undo + rn.undo, f.fs)
            check(f.exists("a.txt") && !f.exists("box")) { "undo should restore a.txt and remove box/ (notes: ${report.notes})" }
        }
    }

    test("file.move refuses to move an accessible root or a folder into itself") {
        fx { f ->
            f.dir("d")
            val t = ToolContext("t", f.fs)
            eq(ErrorKind.INVALID_ARGUMENTS, FileMoveTool().plan(args("source" to f.root.toString(), "destination" to f.p("x")), t).error!!.kind, "root")
            eq(ErrorKind.INVALID_ARGUMENTS, FileMoveTool().plan(args("source" to f.p("d"), "destination" to f.p("d/e")), t).error!!.kind, "into itself")
        }
    }

    test("file.delete moves to a restorable trash, never destroys, and refuses roots") {
        fx { f ->
            f.file("old/x.txt", "data")
            val t = ToolContext("t1", f.fs)
            val r = FileDeleteTool().execute(args("path" to f.p("old")), t)
            check(r.ok); check(!f.exists("old"))
            val trashed = r.data.str("trashed_to")!!
            check(Files.exists(Paths.get(trashed, "x.txt"))) { "content must survive in the trash" }
            eq(ErrorKind.INVALID_ARGUMENTS, FileDeleteTool().plan(args("path" to f.root.toString()), t).error!!.kind, "root")
            eq(ErrorKind.INVALID_ARGUMENTS, FileDeleteTool().plan(args("path" to trashed), t).error!!.kind, "already in trash")
        }
    }

    test("system.storage reports free space for each root") {
        fx { f ->
            val tool = DefaultTools.all().first { it.spec.name == "system.storage" }
            val r = tool.execute(args(), ToolContext("t", f.fs))
            check(r.ok); check(r.data["volumes"]!!.jsonArray.size == 1)
            check((r.data["volumes"] as JsonArray)[0].jsonObject.long("free_bytes")!! > 0)
        }
    }

    test("unknown free space (SAF) never blocks a copy and is reported as unknown, not as a huge number") {
        fx(wrapFs = { UnknownSpaceFs(it) }) { f ->
            val src = f.file("a.bin", "x".repeat(100))
            val t = ToolContext("t", f.fs)
            val plan = FileCopyTool().plan(args("source" to src, "destination" to f.p("b.bin")), t)
            check(plan.error == null) { "plan must not fail when space is unknown: ${plan.error}" }
            val vol = DefaultTools.all().first { it.spec.name == "system.storage" }.execute(args(), t).data["volumes"]!!.jsonArray[0].jsonObject
            eq("false", vol["free_space_known"].toString(), "free_space_known"); eq("null", vol["free_bytes"].toString(), "free_bytes")
        }
    }

    test("registry rejects duplicate tool names and exposes every spec") {
        val reg = ToolRegistry(DefaultTools.all())
        check(reg.specs().size >= 10)
        try { reg.register(FileListTool()); throw TestFailure("duplicate accepted") } catch (e: IllegalArgumentException) { }
    }
}
