package dev.deviceagent.app.ui.synapse

import kotlinx.serialization.json.*

enum class Destination(val label: String) {
    AGENT("Agent"),
    ACTIVITY("Activity"),
    DEVICE("Device"),
    SETTINGS("Settings"),
}

enum class SynapseRoute(val title: String, val destination: Destination, val number: Int) {
    WELCOME("Welcome to Synapse", Destination.AGENT, 1),
    AGENT("Synapse", Destination.AGENT, 2),
    ATTACHMENTS("Attachments & sharing", Destination.AGENT, 3),
    PLAN("Review plan", Destination.AGENT, 4),
    APPROVAL("Review changes", Destination.AGENT, 5),
    MONITOR("Execution monitor", Destination.AGENT, 6),
    RECOVERY("Diagnosis & recovery", Destination.AGENT, 7),
    CHECKPOINT("Checkpoints", Destination.ACTIVITY, 8),
    ACTIVITY("Activity", Destination.ACTIVITY, 9),
    AUDIT("Task audit", Destination.ACTIVITY, 10),
    UNDO("Undo & restore", Destination.ACTIVITY, 11),
    PROVIDERS("AI providers", Destination.SETTINGS, 12),
    CREDENTIALS("Secure credentials", Destination.SETTINGS, 13),
    MODELS("Models & routing", Destination.SETTINGS, 14),
    PRIVACY("Cloud privacy", Destination.SETTINGS, 15),
    USAGE("Usage & budgets", Destination.SETTINGS, 16),
    DEVICE("Your device", Destination.DEVICE, 17),
    SANDBOX("Folders & sandbox", Destination.DEVICE, 18),
    PACKAGES("Linux & packages", Destination.DEVICE, 19),
    PACKAGE_DETAIL("Package transaction", Destination.DEVICE, 20),
    SECURITY("Safety & root", Destination.SETTINGS, 21),
    MEMORY("Agent memory", Destination.SETTINGS, 22),
    SCHEDULE("Schedules", Destination.ACTIVITY, 23),
    TERMINAL("Command workspace", Destination.DEVICE, 24),
    APPS("Apps & APK inspector", Destination.DEVICE, 25),
    TOOLS("Tool catalog", Destination.DEVICE, 26),
    SETTINGS("Settings", Destination.SETTINGS, 27),
    PERMISSIONS("Permissions", Destination.SETTINGS, 28),
    APPEARANCE("Appearance", Destination.SETTINGS, 29),
    ABOUT("About", Destination.SETTINGS, 30),
    TASK_RESULT("Task completed", Destination.AGENT, 31),
    BROWSE("Browse files", Destination.DEVICE, 0),
    SEARCH("Search files", Destination.DEVICE, 0),
}

fun Destination.startRoute(): SynapseRoute =
    when (this) {
        Destination.AGENT -> SynapseRoute.AGENT
        Destination.ACTIVITY -> SynapseRoute.ACTIVITY
        Destination.DEVICE -> SynapseRoute.DEVICE
        Destination.SETTINGS -> SynapseRoute.SETTINGS
    }

enum class PreviewState {
    READY,
    LOADING,
    EMPTY,
    ERROR,
    SUCCESS,
    OFFLINE,
}

enum class DemoTaskStatus {
    RUNNING,
    PAUSED,
    WAITING,
    PLANNED,
    VERIFIED,
    PARTIAL,
    CANCELLED,
    FAILED,
}

data class DemoTask(
    val id: String,
    val title: String,
    val total: Int,
    val verified: Int = 0,
    val skipped: Int = 0,
    val blocked: Int = 0,
    val status: DemoTaskStatus = DemoTaskStatus.PLANNED,
) {
    val settled: Int
        get() = verified + skipped + blocked

    val running: Int
        get() = if (status == DemoTaskStatus.RUNNING && settled < total) 1 else 0

    val queued: Int
        get() = total - settled - running

    val progress: Float
        get() = if (total == 0) 0f else verified.toFloat() / total

    init {
        require(total >= 0 && verified >= 0 && skipped >= 0 && blocked >= 0 && settled <= total)
    }
}

data class DemoMemory(
    val id: String,
    val text: String,
    val scope: String,
    val observed: Boolean = false,
)

data class DemoPackage(
    val name: String,
    val version: String,
    val size: String,
    val purpose: String,
)

val demoPackages =
    listOf(
        DemoPackage("FFmpeg", "7.1", "286 MiB", "Media conversion and probing"),
        DemoPackage("Python", "3.12", "142 MiB", "Isolated scripts and development"),
        DemoPackage("Git", "2.47", "34 MiB", "Source control"),
        DemoPackage("Curl", "8.11", "8 MiB", "Verified HTTPS transfers"),
        DemoPackage("ImageMagick", "7.1", "94 MiB", "Image resizing and conversion"),
    )
val duplicateNames =
    listOf("ticket-copy.pdf", "itinerary (1).pdf", "receipt-copy.jpg") +
        (4..27).map { "duplicate-${it.toString().padStart(2, '0')}.jpg" }
val demoEvents =
    listOf(
        "14:31:02" to "Destination access verified",
        "14:31:03" to "Lisbon_0327.mp4 inspected · H.264 / AAC",
        "14:31:04" to "Conversion started · originals retained",
        "14:31:12" to "Temporary output: 34 MiB",
        "14:31:18" to "Checkpoint metadata persisted for completed items",
    )

/**
 * Pure demo state. It holds no credentials, Context, executor, network client, or real permissions.
 */
data class DemoState(
    val tasks: List<DemoTask> =
        listOf(
            DemoTask(
                "SYN-2048",
                "Make travel videos smaller",
                500,
                320,
                4,
                2,
                DemoTaskStatus.RUNNING,
            ),
            DemoTask("SYN-2049", "Extract lesson audio", 156, status = DemoTaskStatus.WAITING),
            DemoTask("SYN-2050", "Organize Downloads", 618),
            DemoTask("SYN-2044", "Organize receipts", 84, 84, status = DemoTaskStatus.VERIFIED),
            DemoTask("SYN-2045", "Install Python", 4, 4, status = DemoTaskStatus.VERIFIED),
            DemoTask(
                "SYN-2046",
                "Build notes app",
                6,
                3,
                blocked = 1,
                status = DemoTaskStatus.FAILED,
            ),
            DemoTask("SYN-2047", "Clean old exports", 45, 12, status = DemoTaskStatus.CANCELLED),
        ),
    val activeTaskId: String = "SYN-2048",
    val draft: String = "",
    val destination: String = "Downloads/Organized",
    val planRevision: Int = 1,
    val approvedRevision: Int? = null,
    val includeTrash: Boolean = false,
    val selectedDuplicates: Set<String> = duplicateNames.toSet(),
    val consent: Boolean = false,
    val packageName: String = "FFmpeg",
    val packageStep: Int = -1,
    val installed: Set<String> = setOf("FFmpeg", "Python", "Git", "Curl"),
    val attempts: Int = 0,
    val recoveryVerified: Boolean = false,
    val recoveryAborted: Boolean = false,
    val recoveryNote: String = "",
    val rollbackDone: Boolean = false,
    val restoreConflictChoice: String = "Keep current",
    val provider: String = "Gemini",
    val model: String = "Fast planner",
    val fallback: String = "Recovery model",
    val credentialStored: Boolean = true,
    val connectionTested: Boolean = false,
    val mode: String = "Normal",
    val rootStatus: String = "Detected",
    val rootEnabled: Boolean = false,
    val requestBudget: Int = 40,
    val terminalCommand: String = "python scripts/catalog.py --source /workspace/input",
    val terminalComplete: Boolean = false,
    val scheduleGoal: String = "Compress new travel videos",
    val scheduleTime: String = "22:00",
    val scheduleEnabled: Boolean = true,
    val memories: List<DemoMemory> =
        listOf(
            DemoMemory("android", "Android 15 · ARM64", "Device · observed 2m ago", true),
            DemoMemory("storage", "48.2 GiB free", "Device · observed 2m ago", true),
            DemoMemory("delete", "Never delete automatically", "All tasks"),
            DemoMemory("audio", "Save audio in Music/Converted", "Media tasks"),
            DemoMemory("packages", "Ask before installing packages", "All tasks"),
            DemoMemory(
                "failure",
                "AAC stream copy verified for Porto_0087.mp4",
                "SYN-2048 · task context",
                true,
            ),
        ),
    val flags: Map<String, Boolean> =
        mapOf(
            "filenames" to true,
            "metadata" to true,
            "output" to true,
            "contents" to false,
            "media" to false,
            "routing" to false,
            "charging" to true,
            "notifications" to true,
        ),
    val attachments: Set<String> = setOf("Movies/Trips"),
    val folders: Set<String> = setOf("Downloads", "Movies/Trips", "Music/Converted"),
    val notice: String? = null,
) {
    val task: DemoTask
        get() = tasks.first { it.id == activeTaskId }

    val planCount: Int
        get() = 618 + if (includeTrash) selectedDuplicates.size else 0

    fun flag(key: String): Boolean = flags[key] == true

    fun updateTask(id: String = activeTaskId, transform: (DemoTask) -> DemoTask): DemoState =
        copy(tasks = tasks.map { if (it.id == id) transform(it) else it })
}

sealed interface DemoAction {
    data class SelectTask(val id: String) : DemoAction

    data class Draft(val text: String) : DemoAction

    data object Submit : DemoAction

    data class DestinationChanged(val text: String) : DemoAction

    data class IncludeTrash(val include: Boolean) : DemoAction

    data class SelectDuplicate(val name: String, val selected: Boolean) : DemoAction

    data class Consent(val checked: Boolean) : DemoAction

    data class Approve(val revision: Int) : DemoAction

    data object PauseResume : DemoAction

    data object Advance : DemoAction

    data object Cancel : DemoAction

    data object Rollback : DemoAction

    data class ConflictChoice(val choice: String) : DemoAction

    data class PackageSelected(val name: String) : DemoAction

    data object Install : DemoAction

    data object InstallStep : DemoAction

    data object CancelInstall : DemoAction

    data class RemovePackage(val name: String) : DemoAction

    data object RetryRecovery : DemoAction

    data object Alternative : DemoAction

    data class RecoveryInput(val text: String) : DemoAction

    data object AbortRecovery : DemoAction

    data class Provider(val name: String) : DemoAction

    data class Model(val name: String) : DemoAction

    data class Fallback(val name: String) : DemoAction

    data class Credential(val stored: Boolean) : DemoAction

    data object TestConnection : DemoAction

    data class Toggle(val key: String, val value: Boolean) : DemoAction

    data class Mode(val name: String) : DemoAction

    data class RootStatus(val name: String) : DemoAction

    data class RootEnabled(val enabled: Boolean) : DemoAction

    data class Budget(val requests: Int) : DemoAction

    data class Command(val text: String) : DemoAction

    data object RunCommand : DemoAction

    data class Schedule(val goal: String, val time: String, val enabled: Boolean) : DemoAction

    data class SaveMemory(val id: String, val text: String, val scope: String) : DemoAction

    data class DeleteMemory(val id: String) : DemoAction

    data class Attachment(val name: String, val selected: Boolean) : DemoAction

    data class Folder(val name: String, val selected: Boolean) : DemoAction

    data class Notice(val text: String?) : DemoAction
}

fun reduceDemo(s: DemoState, a: DemoAction): DemoState =
    when (a) {
        is DemoAction.SelectTask ->
            if (s.tasks.any { it.id == a.id }) s.copy(activeTaskId = a.id) else s
        is DemoAction.Draft -> s.copy(draft = a.text.take(4000))
        DemoAction.Submit ->
            if (s.draft.isBlank()) s.copy(notice = "Describe a goal first.")
            else
                s.copy(
                        activeTaskId = "SYN-2050",
                        approvedRevision = null,
                        consent = false,
                        planRevision = s.planRevision + 1,
                    )
                    .updateTask("SYN-2050") {
                        it.copy(
                            title = s.draft,
                            total = s.planCount,
                            verified = 0,
                            skipped = 0,
                            blocked = 0,
                            status = DemoTaskStatus.PLANNED,
                        )
                    }
        is DemoAction.DestinationChanged ->
            s.copy(
                destination = a.text.take(200),
                planRevision = s.planRevision + 1,
                approvedRevision = null,
                consent = false,
            )
        is DemoAction.IncludeTrash ->
            s.copy(
                includeTrash = a.include,
                planRevision = s.planRevision + 1,
                approvedRevision = null,
                consent = false,
            )
        is DemoAction.SelectDuplicate ->
            if (a.name !in duplicateNames) s
            else
                s.copy(
                    selectedDuplicates =
                        if (a.selected) s.selectedDuplicates + a.name
                        else s.selectedDuplicates - a.name,
                    planRevision = s.planRevision + 1,
                    approvedRevision = null,
                    consent = false,
                )
        is DemoAction.Consent -> s.copy(consent = a.checked)
        is DemoAction.Approve ->
            when {
                a.revision != s.planRevision ->
                    s.copy(notice = "The plan changed. Review the latest revision.")
                s.destination.isBlank() -> s.copy(notice = "Choose a destination before approving.")
                s.includeTrash && s.selectedDuplicates.isNotEmpty() && !s.consent ->
                    s.copy(notice = "Review and acknowledge the selected trash operations.")
                else ->
                    s.copy(
                            approvedRevision = a.revision,
                            activeTaskId = "SYN-2050",
                            rollbackDone = false,
                            notice = "Demo plan approved. No device files were changed.",
                        )
                        .updateTask("SYN-2050") {
                            it.copy(
                                total = s.planCount,
                                verified = 0,
                                skipped = 0,
                                blocked = 0,
                                status = DemoTaskStatus.RUNNING,
                            )
                        }
            }
        DemoAction.PauseResume ->
            s.updateTask {
                when (it.status) {
                    DemoTaskStatus.RUNNING -> it.copy(status = DemoTaskStatus.PAUSED)
                    DemoTaskStatus.PAUSED -> it.copy(status = DemoTaskStatus.RUNNING)
                    else -> it
                }
            }
        DemoAction.Advance ->
            s.updateTask { task ->
                if (task.status != DemoTaskStatus.RUNNING) task
                else {
                    val verified =
                        minOf(task.verified + 1, task.total - task.skipped - task.blocked)
                    val done = verified + task.skipped + task.blocked == task.total
                    task.copy(
                        verified = verified,
                        status =
                            if (done) {
                                if (task.blocked > 0) DemoTaskStatus.PARTIAL
                                else DemoTaskStatus.VERIFIED
                            } else DemoTaskStatus.RUNNING,
                    )
                }
            }
        DemoAction.Cancel ->
            s.updateTask {
                    if (
                        it.status in
                            setOf(
                                DemoTaskStatus.VERIFIED,
                                DemoTaskStatus.FAILED,
                                DemoTaskStatus.CANCELLED,
                            )
                    )
                        it
                    else it.copy(status = DemoTaskStatus.CANCELLED)
                }
                .copy(notice = "Demo execution cancelled. Completed changes are listed for review.")
        DemoAction.Rollback ->
            s.copy(
                rollbackDone = true,
                notice = "Demo restore verified. The changed ticket.png was preserved.",
            )
        is DemoAction.ConflictChoice -> s.copy(restoreConflictChoice = a.choice)
        is DemoAction.PackageSelected ->
            if (demoPackages.any { it.name == a.name })
                s.copy(packageName = a.name, packageStep = -1)
            else s
        DemoAction.Install -> if (s.packageStep in 0..3) s else s.copy(packageStep = 0)
        DemoAction.InstallStep ->
            if (s.packageStep in 0..3)
                s.copy(
                    packageStep = s.packageStep + 1,
                    installed = if (s.packageStep == 3) s.installed + s.packageName else s.installed,
                )
            else s
        DemoAction.CancelInstall ->
            s.copy(
                packageStep = -1,
                notice = "Demo transaction cancelled before capability verification.",
            )
        is DemoAction.RemovePackage ->
            if (a.name == "FFmpeg" && s.tasks.any { it.status == DemoTaskStatus.RUNNING })
                s.copy(notice = "FFmpeg is used by a running demo task. Pause or finish it first.")
            else s.copy(installed = s.installed - a.name, packageStep = -1)
        DemoAction.RetryRecovery ->
            when {
                s.recoveryVerified -> s.copy(notice = "Recovery has already been verified.")
                s.recoveryAborted ->
                    s.copy(notice = "This recovery was aborted. Reset the demo to try again.")
                s.attempts >= 3 ->
                    s.copy(
                        notice =
                            "Recovery limit reached. Review the evidence before choosing another strategy."
                    )
                s.packageName != "FFmpeg" || s.packageStep != 4 ->
                    s.copy(notice = "Verify the MP3-enabled FFmpeg profile before retrying.")
                else ->
                    s.copy(
                            attempts = s.attempts + 1,
                            recoveryVerified = true,
                            notice =
                                "Demo retry verified: MP3 output exists, decodes, and matches duration.",
                        )
                        .updateTask("SYN-2049") {
                            it.copy(verified = 1, status = DemoTaskStatus.RUNNING)
                        }
            }
        DemoAction.Alternative ->
            if (s.recoveryAborted || s.recoveryVerified || s.attempts >= 3)
                s.copy(notice = "Recovery is complete, aborted or at its attempt limit.")
            else
                s.copy(
                        attempts = s.attempts + 1,
                        recoveryVerified = true,
                        notice = "AAC output explicitly approved and verified in this demo.",
                    )
                    .updateTask("SYN-2049") {
                        it.copy(verified = 1, status = DemoTaskStatus.RUNNING)
                    }
        is DemoAction.RecoveryInput ->
            s.copy(
                recoveryNote = a.text.take(1000),
                notice = "Your clarification was added to the demo diagnosis.",
            )
        DemoAction.AbortRecovery ->
            s.copy(recoveryAborted = true).updateTask("SYN-2049") {
                it.copy(status = DemoTaskStatus.CANCELLED)
            }
        is DemoAction.Provider -> s.copy(provider = a.name, connectionTested = false)
        is DemoAction.Model -> s.copy(model = a.name)
        is DemoAction.Fallback -> s.copy(fallback = a.name)
        is DemoAction.Credential -> s.copy(credentialStored = a.stored, connectionTested = false)
        DemoAction.TestConnection ->
            s.copy(
                connectionTested = s.credentialStored,
                notice =
                    if (s.credentialStored)
                        "Simulated authentication and model discovery succeeded."
                    else "Add the demo credential first.",
            )
        is DemoAction.Toggle -> s.copy(flags = s.flags + (a.key to a.value))
        is DemoAction.Mode -> s.copy(mode = a.name)
        is DemoAction.RootStatus -> s.copy(rootStatus = a.name, rootEnabled = false)
        is DemoAction.RootEnabled ->
            if (s.rootStatus == "Detected")
                s.copy(
                    rootEnabled = a.enabled,
                    notice =
                        "Demo backend setting only. Each privileged operation still requires approval.",
                )
            else s.copy(rootEnabled = false, notice = "Root is not available in this scenario.")
        is DemoAction.Budget -> s.copy(requestBudget = a.requests.coerceIn(20, 100))
        is DemoAction.Command ->
            s.copy(terminalCommand = a.text.take(2000), terminalComplete = false)
        DemoAction.RunCommand ->
            if (s.terminalCommand.isBlank()) s.copy(notice = "Enter a command preview.")
            else
                s.copy(
                    terminalComplete = true,
                    notice = "Simulated example output only; no command was executed.",
                )
        is DemoAction.Schedule ->
            if (!Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(a.time) || a.goal.isBlank())
                s.copy(notice = "Enter a goal and a 24-hour time such as 22:00.")
            else
                s.copy(
                    scheduleGoal = a.goal.take(500),
                    scheduleTime = a.time,
                    scheduleEnabled = a.enabled,
                    notice = "Demo schedule saved. No background job was registered.",
                )
        is DemoAction.SaveMemory ->
            if (a.text.isBlank() || s.memories.any { it.id == a.id && it.observed }) s
            else
                s.copy(
                    memories =
                        s.memories.filterNot { it.id == a.id } +
                            DemoMemory(a.id, a.text.take(500), a.scope),
                    notice = "Demo preference saved. Safety rules still take precedence.",
                )
        is DemoAction.DeleteMemory -> s.copy(memories = s.memories.filterNot { it.id == a.id })
        is DemoAction.Attachment ->
            s.copy(attachments = if (a.selected) s.attachments + a.name else s.attachments - a.name)
        is DemoAction.Folder ->
            s.copy(folders = if (a.selected) s.folders + a.name else s.folders - a.name)
        is DemoAction.Notice -> s.copy(notice = a.text)
    }

/** Versioned, bounded snapshot for process recreation; no real secret is ever accepted here. */
object DemoStateCodec {
    fun encode(s: DemoState): String =
        buildJsonObject {
                put("version", 1)
                put(
                    "tasks",
                    JsonArray(
                        s.tasks.map { t ->
                            buildJsonObject {
                                put("id", t.id)
                                put("title", t.title)
                                put("total", t.total)
                                put("verified", t.verified)
                                put("skipped", t.skipped)
                                put("blocked", t.blocked)
                                put("status", t.status.name)
                            }
                        }
                    ),
                )
                val strings =
                    mapOf(
                        "activeTaskId" to s.activeTaskId,
                        "draft" to s.draft,
                        "destination" to s.destination,
                        "packageName" to s.packageName,
                        "provider" to s.provider,
                        "model" to s.model,
                        "fallback" to s.fallback,
                        "mode" to s.mode,
                        "rootStatus" to s.rootStatus,
                        "terminalCommand" to s.terminalCommand,
                        "scheduleGoal" to s.scheduleGoal,
                        "scheduleTime" to s.scheduleTime,
                        "recoveryNote" to s.recoveryNote,
                        "restoreConflictChoice" to s.restoreConflictChoice,
                    )
                strings.forEach { (k, v) -> put(k, v) }
                mapOf(
                        "planRevision" to s.planRevision,
                        "approvedRevision" to (s.approvedRevision ?: -1),
                        "packageStep" to s.packageStep,
                        "attempts" to s.attempts,
                        "requestBudget" to s.requestBudget,
                    )
                    .forEach { (k, v) -> put(k, v) }
                mapOf(
                        "includeTrash" to s.includeTrash,
                        "consent" to s.consent,
                        "rollbackDone" to s.rollbackDone,
                        "recoveryVerified" to s.recoveryVerified,
                        "recoveryAborted" to s.recoveryAborted,
                        "credentialStored" to s.credentialStored,
                        "connectionTested" to s.connectionTested,
                        "rootEnabled" to s.rootEnabled,
                        "terminalComplete" to s.terminalComplete,
                        "scheduleEnabled" to s.scheduleEnabled,
                    )
                    .forEach { (k, v) -> put(k, v) }
                put("flags", JsonObject(s.flags.mapValues { JsonPrimitive(it.value) }))
                mapOf(
                        "selectedDuplicates" to s.selectedDuplicates,
                        "installed" to s.installed,
                        "attachments" to s.attachments,
                        "folders" to s.folders,
                    )
                    .forEach { (k, v) -> put(k, JsonArray(v.map { JsonPrimitive(it) })) }
                put(
                    "memories",
                    JsonArray(
                        s.memories.map {
                            buildJsonObject {
                                put("id", it.id)
                                put("text", it.text)
                                put("scope", it.scope)
                                put("observed", it.observed)
                            }
                        }
                    ),
                )
            }
            .toString()

    fun decode(text: String?): DemoState =
        runCatching {
                require(text != null && text.length <= 64000)
                val j = Json.parseToJsonElement(text).jsonObject
                require(j["version"]?.jsonPrimitive?.int == 1)
                fun str(k: String) = j.getValue(k).jsonPrimitive.content
                fun int(k: String) = j.getValue(k).jsonPrimitive.int
                fun bool(k: String) = j.getValue(k).jsonPrimitive.boolean
                fun set(k: String) =
                    j.getValue(k).jsonArray.map { it.jsonPrimitive.content }.toSet()
                val tasks =
                    j.getValue("tasks").jsonArray.map { el ->
                        val t = el.jsonObject
                        DemoTask(
                            t.getValue("id").jsonPrimitive.content,
                            t.getValue("title").jsonPrimitive.content,
                            t.getValue("total").jsonPrimitive.int,
                            t.getValue("verified").jsonPrimitive.int,
                            t.getValue("skipped").jsonPrimitive.int,
                            t.getValue("blocked").jsonPrimitive.int,
                            DemoTaskStatus.valueOf(t.getValue("status").jsonPrimitive.content),
                        )
                    }
                require(
                    tasks.map { it.id }.toSet() == DemoState().tasks.map { it.id }.toSet() &&
                        tasks.any { it.id == str("activeTaskId") }
                )
                require(int("packageStep") in -1..4 && int("attempts") in 0..3)
                require(demoPackages.any { it.name == str("packageName") })
                require(set("selectedDuplicates").all { it in duplicateNames })
                DemoState(
                    tasks,
                    str("activeTaskId"),
                    str("draft"),
                    str("destination"),
                    int("planRevision"),
                    int("approvedRevision").takeIf { it >= 0 },
                    bool("includeTrash"),
                    set("selectedDuplicates"),
                    bool("consent"),
                    str("packageName"),
                    int("packageStep"),
                    set("installed"),
                    int("attempts"),
                    bool("recoveryVerified"),
                    bool("recoveryAborted"),
                    str("recoveryNote"),
                    bool("rollbackDone"),
                    str("restoreConflictChoice"),
                    str("provider"),
                    str("model"),
                    str("fallback"),
                    bool("credentialStored"),
                    bool("connectionTested"),
                    str("mode"),
                    str("rootStatus"),
                    bool("rootEnabled"),
                    int("requestBudget"),
                    str("terminalCommand"),
                    bool("terminalComplete"),
                    str("scheduleGoal"),
                    str("scheduleTime"),
                    bool("scheduleEnabled"),
                    j.getValue("memories").jsonArray.map {
                        val m = it.jsonObject
                        DemoMemory(
                            m.getValue("id").jsonPrimitive.content,
                            m.getValue("text").jsonPrimitive.content,
                            m.getValue("scope").jsonPrimitive.content,
                            m.getValue("observed").jsonPrimitive.boolean,
                        )
                    },
                    j.getValue("flags").jsonObject.mapValues { it.value.jsonPrimitive.boolean },
                    set("attachments"),
                    set("folders"),
                )
            }
            .getOrElse { DemoState() }
}
