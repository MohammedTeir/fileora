package dev.deviceagent.core

import dev.deviceagent.core.agent.TaskStatus
import dev.deviceagent.core.policy.ConfirmationAnswer
import dev.deviceagent.core.policy.ConfirmationRequest
import dev.deviceagent.core.policy.ConfirmationResponse
import dev.deviceagent.core.policy.InteractionMode
import dev.deviceagent.core.policy.MAX_REVIEW_ITEMS
import dev.deviceagent.core.util.str
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private fun Fx.moveArgs(pattern: String = "*.pdf") = args(
    "source_folder" to p("Download"),
    "destination_folder" to p("Docs"),
    "pattern" to pattern,
)

private fun Fx.pdfs(vararg names: String) = names.forEach { file("Download/$it", it) }

private fun JsonObject.errorKind(): String? = (this["error"] as? JsonObject)?.str("kind")

fun registerReviewChangesTests() {
    test("review: a batch move offers every file with its destination and is selectable") {
        fx { f ->
            f.pdfs("a.pdf", "b.pdf"); f.file("Download/keep.txt")
            var seen: ConfirmationRequest? = null
            f.user.onReview = { r -> seen = r; ConfirmationAnswer(ConfirmationResponse.ALLOW_ONCE, r.items.map { it.id }.toSet()) }
            f.provider.calls("file.move_matching" to f.moveArgs()).says("Moved them.")
            val out = f.run("move my pdfs")
            val r = seen ?: throw TestFailure("no review was requested")
            check(r.selectable) { "should be selectable" }
            eq(setOf(f.p("Download/a.pdf"), f.p("Download/b.pdf")), r.items.map { it.id }.toSet(), "offered ids")
            eq(f.p("Docs/a.pdf"), r.items.first { it.id == f.p("Download/a.pdf") }.detail, "destination shown")
            check(f.exists("Docs/a.pdf") && f.exists("Docs/b.pdf")) { "both approved files moved" }
            check(f.exists("Download/keep.txt")) { "non-matching file untouched" }
            eq(TaskStatus.SUCCESS, out.status, "status")
        }
    }

    test("review: approving only some files moves only those, keeps the rest, verifies, and reports it honestly") {
        fx { f ->
            f.pdfs("a.pdf", "b.pdf", "c.pdf")
            f.user.onReview = { ConfirmationAnswer(ConfirmationResponse.ALLOW_ONCE, setOf(f.p("Download/a.pdf"), f.p("Download/c.pdf"))) }
            f.provider.calls("file.move_matching" to f.moveArgs()).says("Done.")
            val out = f.run("move my pdfs")
            check(f.exists("Docs/a.pdf") && f.exists("Docs/c.pdf")) { "approved files moved" }
            check(!f.exists("Docs/b.pdf") && f.exists("Download/b.pdf")) { "rejected file must stay put" }
            eq(TaskStatus.PARTIAL_SUCCESS, out.status, "partial approval is not full success")
            check(out.report.contains("leave 1 file(s) where they are")) { "report must say what the user kept: ${out.report}" }
            eq(0, out.unverifiedChanges, "the narrowed move is still verified")
            eq(0, out.failures, "failures")
            val result = f.provider.lastResults().single().content["untrusted_data"]!!.jsonObject
            eq(2, result["moved"]!!.jsonPrimitive.int, "moved count")
            eq(1, f.provider.lastResults().single().content["untrusted_data"]!!.jsonObject["kept_in_place_by_user"]!!.jsonPrimitive.int, "model told what was kept")
            check(out.canUndo) { "moved files stay undoable" }
        }
    }

    test("review: a file that appears while the person is reviewing is never moved") {
        fx { f ->
            f.pdfs("a.pdf", "b.pdf")
            f.user.onReview = { r ->
                f.file("Download/late.pdf", "late") // shows up after the plan was shown
                ConfirmationAnswer(ConfirmationResponse.ALLOW_ONCE, r.items.map { it.id }.toSet())
            }
            f.provider.calls("file.move_matching" to f.moveArgs()).says("Done.")
            f.run("move my pdfs")
            check(f.exists("Docs/a.pdf") && f.exists("Docs/b.pdf")) { "reviewed files moved" }
            check(f.exists("Download/late.pdf") && !f.exists("Docs/late.pdf")) { "unreviewed file must not be touched" }
        }
    }

    test("review: rejecting every file declines the action and moves nothing") {
        fx { f ->
            f.pdfs("a.pdf", "b.pdf")
            f.user.onReview = { ConfirmationAnswer(ConfirmationResponse.ALLOW_ONCE, emptySet()) }
            f.provider.calls("file.move_matching" to f.moveArgs()).says("They said no.")
            val out = f.run("move my pdfs")
            check(f.exists("Download/a.pdf") && f.exists("Download/b.pdf") && !f.exists("Docs")) { "nothing moved" }
            eq("DECLINED_BY_USER", f.provider.lastResults().single().content.errorKind(), "model sees a decline")
            eq(TaskStatus.WAITING_FOR_PERMISSION, out.status, "status")
        }
    }

    test("review: ids that were never offered are ignored and cannot widen the approval") {
        fx { f ->
            f.pdfs("a.pdf", "b.pdf"); f.file("Download/secret.docx")
            f.user.onReview = {
                ConfirmationAnswer(ConfirmationResponse.ALLOW_ONCE, setOf(f.p("Download/a.pdf"), f.p("Download/secret.docx"), "/nowhere/ghost.pdf"))
            }
            f.provider.calls("file.move_matching" to f.moveArgs()).says("Done.")
            f.run("move my pdfs")
            check(f.exists("Docs/a.pdf")) { "the offered, approved file moved" }
            check(f.exists("Download/b.pdf") && !f.exists("Docs/b.pdf")) { "offered but not approved stays" }
            check(f.exists("Download/secret.docx") && !f.exists("Docs/secret.docx")) { "an id outside the plan is ignored" }
        }
    }

    test("review: Don't allow and Stop still win over any ticked files") {
        fx { f ->
            f.pdfs("a.pdf", "b.pdf")
            f.user.onReview = { r -> ConfirmationAnswer(ConfirmationResponse.DENY, r.items.map { it.id }.toSet()) }
            f.provider.calls("file.move_matching" to f.moveArgs()).says("Declined.")
            f.run("move my pdfs")
            check(f.exists("Download/a.pdf") && f.exists("Download/b.pdf") && !f.exists("Docs")) { "DENY moves nothing" }
        }
        fx { f ->
            f.pdfs("a.pdf", "b.pdf")
            f.user.onReview = { r -> ConfirmationAnswer(ConfirmationResponse.CANCEL_TASK, r.items.map { it.id }.toSet()) }
            f.provider.calls("file.move_matching" to f.moveArgs())
            val out = f.run("move my pdfs")
            eq(TaskStatus.CANCELLED, out.status, "status")
            check(f.exists("Download/a.pdf") && !f.exists("Docs")) { "cancel moves nothing" }
        }
    }

    test("review: a channel without file-by-file review still works all or nothing") {
        fx { f ->
            f.pdfs("a.pdf", "b.pdf")
            f.user.onConfirm = { ConfirmationResponse.ALLOW_ONCE } // no onReview: default falls back to confirm()
            f.provider.calls("file.move_matching" to f.moveArgs()).says("Done.")
            val out = f.run("move my pdfs")
            check(f.exists("Docs/a.pdf") && f.exists("Docs/b.pdf")) { "everything moved" }
            eq(TaskStatus.SUCCESS, out.status, "status")
        }
    }

    test("review: one file, or more than the review limit, is not offered for file-by-file review") {
        fx { f ->
            f.pdfs("only.pdf")
            var seen: ConfirmationRequest? = null
            f.user.onReview = { r -> seen = r; ConfirmationAnswer(ConfirmationResponse.ALLOW_ONCE, null) }
            f.provider.calls("file.move_matching" to f.moveArgs()).says("Done.")
            f.run("move my pdf")
            check(seen?.selectable == false && seen?.items?.isEmpty() == true) { "a single file needs no review page" }
            check(f.exists("Docs/only.pdf")) { "and it is still moved on Allow" }
        }
        fx { f ->
            repeat(MAX_REVIEW_ITEMS + 1) { f.file("Download/f$it.pdf", "x") }
            var seen: ConfirmationRequest? = null
            f.user.onReview = { r -> seen = r; ConfirmationAnswer(ConfirmationResponse.DENY, null) }
            f.provider.calls("file.move_matching" to f.moveArgs()).says("Declined.")
            f.run("move my pdfs")
            check(seen?.selectable == false && seen?.items?.isEmpty() == true) { "too many files: all-or-nothing, never a truncated list" }
            eq(MAX_REVIEW_ITEMS + 1, seen!!.fileCount, "the full count is still shown")
        }
    }

    test("review: suggest-only mode and dry runs never ask for a review or move anything") {
        fx(mode = InteractionMode.MANUAL) { f ->
            f.pdfs("a.pdf", "b.pdf")
            f.user.onReview = { throw TestFailure("manual mode must not ask") }
            f.provider.calls("file.move_matching" to f.moveArgs()).says("Proposed.")
            f.run("move my pdfs")
            check(f.exists("Download/a.pdf") && !f.exists("Docs")) { "manual mode moves nothing" }
            eq(0, f.user.confirmations.size, "no confirmation asked")
        }
    }
}
