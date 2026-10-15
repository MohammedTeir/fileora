@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deviceagent.app.Tone

@Composable
internal fun DemoAgentRoute(
    route: SynapseRoute,
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    when (route) {
        SynapseRoute.WELCOME -> WelcomeDemo(nav)
        SynapseRoute.AGENT -> AgentDemo(s, send, nav)
        SynapseRoute.ATTACHMENTS -> AttachmentsDemo(s, send, nav)
        SynapseRoute.PLAN -> PlanDemo(s, send, nav)
        SynapseRoute.APPROVAL -> ApprovalDemo(s, send, nav)
        SynapseRoute.MONITOR -> MonitorDemo(s, send, nav)
        SynapseRoute.RECOVERY -> RecoveryDemo(s, send, nav)
        SynapseRoute.CHECKPOINT -> CheckpointDemo(s, send, nav)
        SynapseRoute.ACTIVITY -> ActivityDemo(s, send, nav)
        SynapseRoute.AUDIT -> AuditDemo(s, nav)
        SynapseRoute.UNDO -> UndoDemo(s, send)
        else -> Unit
    }
}

@Composable
private fun WelcomeDemo(nav: (SynapseRoute) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Welcome to", style = MaterialTheme.typography.headlineMedium)
                Text("Synapse", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Muted("Turn your ideas into real actions on your Android device using AI. Synapse can inspect, plan, ask for your approval, and handle the rest.")
            }
            Image(painterResource(R.drawable.synapse_brand_mark), "Synapse brand mark", Modifier.weight(1f).padding(16.dp).aspectRatio(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Triple(SynapseIcon.AGENT,"Powerful AI","Plan and execute tasks for you"), Triple(SynapseIcon.SECURITY,"Safe and controlled","You approve sensitive actions"), Triple(SynapseIcon.DEVICE,"Built for Android","Works with your device and apps")).forEach { (icon,title,detail) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssetIcon(icon)
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Section("Get started") {
            Muted("Complete these steps to set up Synapse on your device.")
            listOf(
                Triple(SynapseIcon.PROVIDERS,"Connect an AI provider",SynapseRoute.PROVIDERS),
                Triple(SynapseIcon.FOLDER,"Grant folder access",SynapseRoute.SANDBOX),
                Triple(SynapseIcon.SECURITY,"Set up safety preferences",SynapseRoute.SECURITY),
                Triple(SynapseIcon.SETTINGS,"Configure basic settings",SynapseRoute.SETTINGS),
            ).forEachIndexed { index,(icon,title,route) ->
                DesignRow(icon,title,listOf("Use Gemini, OpenAI, Claude, or a custom provider.","Allow Synapse to access files using Android SAF.","Choose what actions require your approval.","Appearance, language, and other preferences.")[index],onClick = { nav(route) })
            }
        }
        FullButton("Continue Setup  →") { nav(SynapseRoute.PROVIDERS) }
        TextButton({ nav(SynapseRoute.AGENT) }, Modifier.align(Alignment.CenterHorizontally)) { Text("Skip for now") }
    }
}

@Composable
private fun AgentDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    DesignPage(SynapseRoute.AGENT,"Hello, Mohammed","Describe what you want to do with your device.\nI’ll plan, ask for approval, and handle the rest.",nav) {
        Section("") {
            BasicTextField(s.draft,{ send(DemoAction.Draft(it)) },Modifier.fillMaxWidth().heightIn(min = 42.dp),textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),decorationBox = { field ->
                if(s.draft.isBlank()) Muted("What would you like to do?")
                field()
            })
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf("Add files","Add images","Add folder").forEach { label -> TextButton({ nav(SynapseRoute.ATTACHMENTS) },contentPadding = PaddingValues(5.dp)) { Text(label, fontSize = 10.sp) } }
                Spacer(Modifier.weight(1f))
                IconButton({ if(s.draft.isNotBlank()) { send(DemoAction.Submit); nav(SynapseRoute.PLAN) } },enabled = s.draft.isNotBlank()) { AssetIcon(SynapseIcon.PLAY,"Preview demo plan") }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Organize Downloads","Convert media files","Run Python project","Clean storage").forEach { goal -> AssistChip({ send(DemoAction.Draft(goal)) },{ Text(goal, fontSize = 10.sp) }) }
        }
        Text("Quick actions", style = MaterialTheme.typography.titleMedium)
        listOf(
            Triple(SynapseIcon.FOLDER,"Manage files",SynapseRoute.SANDBOX),
            Triple(SynapseIcon.TERMINAL,"Run a command",SynapseRoute.TERMINAL),
            Triple(SynapseIcon.APPS,"Manage apps",SynapseRoute.APPS),
            Triple(SynapseIcon.PLAY,"Convert media",SynapseRoute.ATTACHMENTS),
            Triple(SynapseIcon.TOOLS,"Run project",SynapseRoute.TOOLS),
            Triple(SynapseIcon.SCHEDULES,"Schedule task",SynapseRoute.SCHEDULE),
        ).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (icon,label,route) ->
                    Surface(onClick = { nav(route) },modifier = Modifier.weight(1f),shape = RoundedCornerShape(10.dp),color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.padding(8.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { AssetIcon(icon,size = 18.dp);Text(label,style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
        Text("Recent tasks", style = MaterialTheme.typography.titleMedium)
        Section("") {
            DesignRow(SynapseIcon.APPROVAL,"Organize Downloads folder","Completed · 12 files moved","2 hours ago",onClick = { nav(SynapseRoute.TASK_RESULT) })
            DesignRow(SynapseIcon.ACTIVITY,"Convert videos to MP4","Processing · 3 of 5 files","32 minutes ago",onClick = { send(DemoAction.SelectTask("SYN-2048"));nav(SynapseRoute.MONITOR) })
            DesignRow(SynapseIcon.TERMINAL,"Run Python project","Pending approval","1 hour ago",onClick = { nav(SynapseRoute.PLAN) })
        }
    }
}

@Composable
private fun AttachmentsDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    ScreenPage(
        "SELECTED CONTEXT",
        "Your files, your choice",
        "Attachments stay local until a permitted cloud request includes them.",
    ) {
        Section("Choose sample attachments") {
            listOf("Movies/Trips", "itinerary.jpg", "Lesson_01_Speaking.mp4").forEach { name ->
                SwitchSetting(
                    name,
                    when (name) {
                        "Movies/Trips" -> "Folder · 500 videos · 87 GiB"
                        "itinerary.jpg" -> "Image · 2.3 MiB · vision model needed"
                        else -> "Video · 142 MiB · local processing"
                    },
                    name in s.attachments,
                ) {
                    send(DemoAction.Attachment(name, it))
                }
            }
        }
        Section("What will be shared?") {
            LabeledValue("Destination", "${s.provider} · ${s.model}")
            LabeledValue(
                "Local processing",
                if (s.attachments.isEmpty()) "No items selected" else s.attachments.joinToString(),
            )
            LabeledValue(
                "Cloud context",
                if (s.flag("filenames")) "Selected names and allowed metadata"
                else "User goal only; filenames excluded",
            )
            LabeledValue("Excluded", "Video bytes; file contents unless explicitly enabled")
            if ("itinerary.jpg" in s.attachments)
                Notice(
                    "Image consent required",
                    "Choose a vision-capable model and enable selected-image sharing before a real upload.",
                    Tone.WARNING,
                )
            Disclosure("Outbound categories") {
                CodeBlock(
                    "provider: ${s.provider}\nfilenames: ${s.flag("filenames")}\nmetadata: ${s.flag("metadata")}\nfile_contents: ${s.flag("contents")}\nmedia: ${s.flag("media")}"
                )
            }
        }
        Actions {
            Primary("Use selected context") { nav(SynapseRoute.AGENT) }
            Secondary("Privacy settings") { nav(SynapseRoute.PRIVACY) }
        }
    }
}

@Composable
private fun PlanDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    ScreenPage(
        "PLAN REVISION ${s.planRevision}",
        "See the changes first",
        "${s.tasks.first{it.id=="SYN-2050"}.title}. Inspect the scope before anything runs.",
    ) {
        Badge("Dry run · no changes made", icon = Icons.Default.Visibility)
        Metrics(
            "Move" to "604 files",
            "Compress" to "14 archives",
            "Trash" to if (s.includeTrash) "${s.selectedDuplicates.size} selected" else "Off",
            "Untouched" to if (s.includeTrash) "${2481-s.planCount} files" else "1,863 files",
        )
        Section("Destination & exclusions") {
            OutlinedTextField(
                s.destination,
                { send(DemoAction.DestinationChanged(it)) },
                label = { Text("Destination folder") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = s.destination.isBlank(),
            )
            SwitchSetting(
                "Review duplicate trash operations",
                "Matching hashes and retained copies are required. Each destructive scope needs consent.",
                s.includeTrash,
            ) {
                send(DemoAction.IncludeTrash(it))
            }
            Muted(
                "Changing a destination, selection, or destructive scope invalidates the previous approval."
            )
        }
        Section("The proposed steps") {
            listOf(
                    Triple("1 · Inspect and compare", "2,481 files · 27 duplicate candidates", 0),
                    Triple("2 · Create category folders", "Use the selected destination only", 1),
                    Triple("3 · Move selected files", "184 APKs · 322 images · 98 videos", 2),
                    Triple("4 · Compress source archives", "14 files; verify output contents", 1),
                    Triple(
                        "5 · Review duplicate removal",
                        if (s.includeTrash)
                            "${s.selectedDuplicates.size} selected for recoverable trash"
                        else "Excluded from this plan",
                        3,
                    ),
                )
                .forEach { (title, detail, risk) ->
                    Disclosure(title, detail) {
                        RiskBadge(risk)
                        CodeBlock(
                            "tool: ${if(risk==3) "file.delete" else if(risk==2) "file.move" else if(risk==0) "file.hash" else "file.compress"}\nscope: ${s.destination}\nverification: required"
                        )
                        Muted("Targets are checked again immediately before execution.")
                    }
                }
        }
        Notice(
            "Space and reversibility",
            "Estimated 1.2 GiB is recoverable only after trash is emptied. Moves and output creation have recorded undo actions.",
        )
        Actions {
            Primary("Approve & Run", enabled = s.destination.isNotBlank()) {
                nav(SynapseRoute.APPROVAL)
            }
            Secondary("Review targets") { nav(SynapseRoute.APPROVAL) }
            TextButton({ nav(SynapseRoute.AGENT) }) { Text("Cancel") }
        }
    }
}

@Composable
private fun ApprovalDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    val destructive = s.includeTrash && s.selectedDuplicates.isNotEmpty()
    ScreenPage(
        "EXACT SCOPE · REVISION ${s.planRevision}",
        if (destructive) "Review before removing" else "Approve this plan",
        "Approval applies only to the selected files and this version of the plan.",
    ) {
        RiskBadge(if (destructive) 3 else 2)
        Notice(
            if (destructive) "${s.selectedDuplicates.size} files will move to trash"
            else "604 moves and 14 archive jobs",
            if (destructive)
                "Retained copies have matching hashes in this scenario. Restore remains subject to later file changes."
            else "Original content is preserved and destination checks are required.",
            if (destructive) Tone.WARNING else Tone.INFO,
        )
        Section("Scope") {
            LabeledValue("Destination", s.destination)
            LabeledValue("Policy", "Delete and root always require specific approval")
            LabeledValue("Excluded", "Protected locations and unselected files")
        }
        if (s.includeTrash)
            Section("Duplicate candidates", "Review all 27; only selected entries are included.") {
                duplicateNames.forEach { name ->
                    SwitchSetting(
                        name,
                        "Matching hash · retained original present",
                        name in s.selectedDuplicates,
                    ) {
                        send(DemoAction.SelectDuplicate(name, it))
                    }
                }
                SwitchSetting(
                    "I reviewed the selected trash operations",
                    "This does not authorize future deletions.",
                    s.consent,
                ) {
                    send(DemoAction.Consent(it))
                }
            }
        Actions {
            Primary(
                if (destructive) "Approve ${s.selectedDuplicates.size} trash operations & run"
                else "Approve scoped plan",
                enabled = s.destination.isNotBlank() && (!destructive || s.consent),
            ) {
                send(DemoAction.Approve(s.planRevision))
                nav(SynapseRoute.MONITOR)
            }
            Secondary("Edit plan") { nav(SynapseRoute.PLAN) }
            TextButton({ nav(SynapseRoute.AGENT) }) { Text("Cancel") }
        }
    }
}

@Composable
private fun MonitorDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var cancel by remember { mutableStateOf(false) }
    ScreenPage(
        s.task.id,
        s.task.title,
        "Observed results become completed work only after verification.",
    ) {
        Actions {
            Badge(s.task.status.name.lowercase())
            RiskBadge(if (s.task.id == "SYN-2050" && s.includeTrash) 3 else 1)
        }
        Section("Verified progress") {
            TaskProgress(s.task)
            Muted("Checkpoint ${s.task.settled} · completed outcomes saved")
        }
        Section("Agent loop") {
            ChoiceChips(
                listOf("Inspect", "Plan", "Policy", "Execute", "Observe", "Verify"),
                if (s.task.status == DemoTaskStatus.VERIFIED) "Verify" else "Execute",
            ) { stage ->
                send(DemoAction.Notice("$stage evidence is included in the task audit."))
            }
            LinkRow(
                "Inspect the audit",
                "Policy decisions, tool results, verification",
                Icons.AutoMirrored.Filled.FactCheck,
            ) {
                nav(SynapseRoute.AUDIT)
            }
        }
        Section("Current item") {
            Text(
                if (s.task.id == "SYN-2048")
                    "Lisbon_${(s.task.settled+1).coerceAtMost(s.task.total).toString().padStart(4,'0')}.mp4"
                else "Item ${(s.task.settled+1).coerceAtMost(s.task.total)} of ${s.task.total}",
                style = MaterialTheme.typography.titleMedium,
            )
            Muted("Local output · source retained · waiting for verification")
            Metrics("Elapsed" to "42m 18s", "Remaining estimate" to "24–31m")
            Secondary(
                "Simulate next verified item",
                enabled = s.task.status == DemoTaskStatus.RUNNING,
            ) {
                send(DemoAction.Advance)
            }
        }
        Section("Execution stream") {
            demoEvents.forEach { (time, event) ->
                Disclosure(event, time) {
                    CodeBlock(
                        "task_id: ${s.task.id}\nobserved: $event\nsource: demo fixture\nsecrets: redacted"
                    )
                }
            }
        }
        Actions {
            Primary(
                if (s.task.status == DemoTaskStatus.PAUSED) "Resume" else "Pause",
                enabled = s.task.status in listOf(DemoTaskStatus.RUNNING, DemoTaskStatus.PAUSED),
            ) {
                send(DemoAction.PauseResume)
            }
            Secondary("Checkpoint") { nav(SynapseRoute.CHECKPOINT) }
            Secondary(
                "Cancel task",
                enabled = s.task.status in listOf(DemoTaskStatus.RUNNING, DemoTaskStatus.PAUSED),
            ) {
                cancel = true
            }
        }
        if (s.task.blocked > 0)
            LinkRow(
                "Review blocked items",
                "Investigate evidence and recovery attempts",
                Icons.Default.Warning,
            ) {
                nav(SynapseRoute.RECOVERY)
            }
    }
    if (cancel)
        ConfirmAction(
            "Cancel this demo task?",
            "Completed changes remain in the audit. Review rollback separately.",
            "Cancel task",
            { cancel = false },
        ) {
            send(DemoAction.Cancel)
            cancel = false
        }
}

@Composable
private fun RecoveryDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var alternative by remember { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var ask by rememberSaveable { mutableStateOf(false) }
    ScreenPage(
        "SYN-2049 · ATTEMPT ${s.attempts.coerceAtLeast(1)} OF 3",
        "A clear path through failure",
        "Lesson_01_Speaking.mp4 · MP3 extraction",
    ) {
        if (s.recoveryVerified)
            Notice(
                "Retry verified",
                "The sample output exists, decodes, and matches the source duration.",
                Tone.SUCCESS,
            )
        if (s.recoveryAborted)
            Notice(
                "Recovery aborted",
                "Completed work is preserved. Reset the demo to explore another recovery.",
                Tone.WARNING,
            )
        Section("1 · Observed error") {
            CodeBlock("exit code: 1\nstderr: Unknown encoder 'libmp3lame'\ncomplete outputs: 0")
            RiskBadge(1)
        }
        Section("2 · Diagnosis") {
            VerifiedRow("Source readable", "Container and audio stream detected")
            VerifiedRow("Destination writable", "Selected folder grant is valid")
            VerifiedRow(
                "MP3 encoder missing",
                "Encoder enumeration does not include libmp3lame",
                false,
            )
            Muted("The evidence points to the package profile, not a corrupt source.")
        }
        Section("3 · Proposed fix") {
            Text("Install a verified MP3-enabled FFmpeg profile in the isolated Linux userspace.")
            Metrics("Download" to "38.4 MiB", "Added storage" to "112 MiB")
            Secondary("Review package & dependencies") {
                send(DemoAction.PackageSelected("FFmpeg"))
                nav(SynapseRoute.PACKAGE_DETAIL)
            }
        }
        Section("4 · Safety check") {
            VerifiedRow("No root required", "Restricted to the selected userspace")
            VerifiedRow("Source verification required", "A failed checksum blocks installation")
            VerifiedRow("Bounded retry", "Maximum 3 attempts; no unchanged retry loop")
        }
        Actions {
            Primary(
                "Approve fix & retry",
                enabled = !s.recoveryAborted && !s.recoveryVerified && s.attempts < 3,
            ) {
                if (s.packageName == "FFmpeg" && s.packageStep == 4) send(DemoAction.RetryRecovery)
                else {
                    send(DemoAction.PackageSelected("FFmpeg"))
                    nav(SynapseRoute.PACKAGE_DETAIL)
                }
            }
            Secondary(
                "Use alternative tool",
                enabled = !s.recoveryAborted && !s.recoveryVerified && s.attempts < 3,
            ) {
                alternative = true
            }
            Secondary("Ask user input") { ask = !ask }
            TextButton(
                { send(DemoAction.AbortRecovery) },
                enabled = !s.recoveryVerified && !s.recoveryAborted,
            ) {
                Text("Abort")
            }
        }
        if (ask)
            Section("What should the agent know?") {
                OutlinedTextField(
                    input,
                    { input = it },
                    label = { Text("Preferred format or destination") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Primary("Add clarification", enabled = input.isNotBlank()) {
                    send(DemoAction.RecoveryInput(input))
                    ask = false
                }
            }
        if (s.recoveryNote.isNotBlank()) Notice("Your clarification", s.recoveryNote)
        if (s.recoveryVerified)
            Primary("Return to task") {
                send(DemoAction.SelectTask("SYN-2049"))
                nav(SynapseRoute.MONITOR)
            }
    }
    if (alternative)
        ConfirmAction(
            "Use AAC instead of MP3?",
            "This changes the output format. Existing sources remain unchanged.",
            "Approve AAC output",
            { alternative = false },
        ) {
            send(DemoAction.Alternative)
            alternative = false
        }
}

@Composable
private fun CheckpointDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    ScreenPage(
        s.task.id,
        "Pick up where you left off",
        "Resume after reconciling outputs, permissions, and available resources.",
    ) {
        Section(s.task.title) { TaskProgress(s.task) }
        Section("Checkpoint ${s.task.settled}", "Saved 8 seconds ago · demo snapshot") {
            VerifiedRow(
                "Completed outcomes recorded",
                "${s.task.verified} verified, ${s.task.skipped} skipped, ${s.task.blocked} blocked",
            )
            VerifiedRow(
                "Partial output needs reconciliation",
                "Current item is not counted as complete",
                false,
            )
            Muted(
                "Pause finishes at a safe boundary. A resumed task rechecks sources and destination grants."
            )
        }
        Metrics(
            "Battery" to "68% · charging",
            "Thermal status" to "Moderate",
            "Storage available" to "48.2 GiB",
            "Temporary output" to "0.6 GiB",
        )
        Actions {
            Primary(
                if (s.task.status == DemoTaskStatus.PAUSED) "Resume from checkpoint"
                else "Pause at safe boundary",
                enabled = s.task.status in listOf(DemoTaskStatus.PAUSED, DemoTaskStatus.RUNNING),
            ) {
                send(DemoAction.PauseResume)
            }
            Secondary("Review cancellation & rollback") { nav(SynapseRoute.UNDO) }
            Secondary("Open monitor") { nav(SynapseRoute.MONITOR) }
        }
    }
}

@Composable
private fun ActivityDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var tab by rememberSaveable { mutableStateOf("Active") }
    var query by rememberSaveable { mutableStateOf("") }
    ScreenPage(
        "YOUR WORK, ACCOUNTED FOR",
        "Activity",
        "Every task has an outcome. Every change has a trail.",
    ) {
        ChoiceChips(listOf("Active", "History", "Scheduled"), tab) { tab = it }
        OutlinedTextField(
            query,
            { query = it },
            label = { Text("Search tasks, files or IDs") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        when (tab) {
            "Active" -> {
                val tasks =
                    s.tasks.filter {
                        it.status in
                            setOf(
                                DemoTaskStatus.RUNNING,
                                DemoTaskStatus.PAUSED,
                                DemoTaskStatus.WAITING,
                                DemoTaskStatus.PLANNED,
                            ) && (it.title.contains(query, true) || it.id.contains(query, true))
                    }
                if (tasks.isEmpty()) Notice("No matches", "Try a different goal or task ID.")
                tasks.forEach { task ->
                    DemoTaskCard(task) {
                        send(DemoAction.SelectTask(task.id))
                        nav(
                            if (task.id == "SYN-2049" && !s.recoveryVerified) SynapseRoute.RECOVERY
                            else SynapseRoute.MONITOR
                        )
                    }
                }
            }
            "History" -> {
                val tasks =
                    s.tasks.filter {
                        it.status in
                            setOf(
                                DemoTaskStatus.VERIFIED,
                                DemoTaskStatus.PARTIAL,
                                DemoTaskStatus.CANCELLED,
                                DemoTaskStatus.FAILED,
                            ) && (it.title.contains(query, true) || it.id.contains(query, true))
                    }
                if (tasks.isEmpty()) Notice("No matching history", "Try another task name or ID.")
                tasks.forEach { task ->
                    LinkRow(
                        task.title,
                        "${task.id} · ${task.status.name.lowercase()} · ${task.verified}/${task.total} verified",
                        Icons.Default.History,
                    ) {
                        send(DemoAction.SelectTask(task.id))
                        nav(SynapseRoute.AUDIT)
                    }
                }
            }

            else ->
                LinkRow(
                    s.scheduleGoal,
                    "${if(s.scheduleEnabled)"Enabled" else "Paused"} · after ${s.scheduleTime} when conditions permit",
                    Icons.Default.Schedule,
                ) {
                    nav(SynapseRoute.SCHEDULE)
                }
        }
        Secondary("Manage schedules") { nav(SynapseRoute.SCHEDULE) }
    }
}

private data class AuditExample(
    val tool: String,
    val source: String,
    val output: String,
    val check: String,
    val recovery: String,
)

@Composable
private fun AuditDemo(s: DemoState, nav: (SynapseRoute) -> Unit) {
    var tab by rememberSaveable { mutableStateOf("Summary") }
    val evidence =
        when (s.task.id) {
            "SYN-2049" ->
                AuditExample(
                    "media.extract_audio",
                    "Lessons/Lesson_01_Speaking.mp4",
                    "Music/Converted/Lesson_01.mp3",
                    "Audio decodes and duration matches",
                    if (s.recoveryVerified) "Approved recovery verified"
                    else "MP3 encoder missing; awaiting approval",
                )
            "SYN-2050" ->
                AuditExample(
                    "file.move",
                    "Downloads/receipt.jpg",
                    "${s.destination}/Images/receipt.jpg",
                    "Destination exists and content hash matches",
                    "No recovery needed in this fixture",
                )
            "SYN-2044" ->
                AuditExample(
                    "file.move",
                    "Downloads/receipt-084.pdf",
                    "Documents/Receipts/receipt-084.pdf",
                    "All 84 destination hashes match",
                    "No recovery needed",
                )
            "SYN-2045" ->
                AuditExample(
                    "package.install",
                    "Verified Python 3.12 artifact",
                    "Linux userspace /usr/bin/python",
                    "Version probe and import smoke test pass",
                    "Missing shared library repaired; second probe passed",
                )
            "SYN-2046" ->
                AuditExample(
                    "shell.execute",
                    "Projects/notes-app",
                    "build/outputs/apk/debug",
                    "APK output absent; final build failed",
                    "Dependency refresh attempted; incompatible plugin remains",
                )
            "SYN-2047" ->
                AuditExample(
                    "file.delete",
                    "Downloads/Exports/export-012.zip",
                    "Recoverable trash entry",
                    "12 completed trash entries recorded",
                    "Cancelled by user; remaining 33 untouched",
                )
            else ->
                AuditExample(
                    "media.convert",
                    "Movies/Trips/Porto_0087.mp4",
                    "Movies/Trips/Compressed/Porto_0087_720p.mp4",
                    "Expected video/audio streams and duration",
                    "AAC stream copy verified for this item",
                )
        }
    val hasEvidence = s.task.verified > 0
    val final = s.task.status == DemoTaskStatus.VERIFIED
    ScreenPage(
        s.task.id,
        "The complete picture",
        "Review observations, policy decisions, and verified results.",
    ) {
        Badge(
            s.task.status.name.lowercase(),
            when (s.task.status) {
                DemoTaskStatus.VERIFIED -> Tone.SUCCESS
                DemoTaskStatus.FAILED -> Tone.ERROR
                DemoTaskStatus.WAITING,
                DemoTaskStatus.PARTIAL -> Tone.WARNING
                else -> Tone.INFO
            },
        )
        ChoiceChips(listOf("Summary", "Changes", "Timeline", "Tools"), tab) { tab = it }
        when (tab) {
            "Summary" -> {
                Section(s.task.title) {
                    TaskProgress(s.task)
                    LabeledValue(
                        "Final verification",
                        if (final) "All required checks passed in this fixture"
                        else "Pending or incomplete; not a full success",
                    )
                }
                Section("Verification evidence") {
                    VerifiedRow(
                        "Completed item checks",
                        if (hasEvidence) evidence.check
                        else "No completed output has been verified yet",
                        hasEvidence,
                    )
                    LabeledValue("Errors & recovery", evidence.recovery)
                    VerifiedRow(
                        "Whole task complete",
                        if (final) "All required outcomes accounted for"
                        else "Remaining, failed or blocked items need review",
                        final,
                    )
                }
            }
            "Changes" ->
                Section("Task change example") {
                    LabeledValue("Source", evidence.source)
                    LabeledValue("Destination", evidence.output)
                    LabeledValue(
                        "Observed result",
                        if (hasEvidence) evidence.check else "Proposed output; not yet verified",
                    )
                    LinkRow(
                        "Explore rollback review",
                        "Fixed Downloads scenario with a changed-file conflict",
                        Icons.Default.Restore,
                    ) {
                        nav(SynapseRoute.UNDO)
                    }
                }
            "Timeline" ->
                Section("Sample audit trail") {
                    listOf(
                            "14:29:02" to "Scope inspected: ${evidence.source}",
                            "14:29:04" to "Plan and policy checked: ${evidence.tool}",
                            "14:29:05" to evidence.recovery,
                            "14:31:18" to "${s.task.verified} verified outcomes recorded",
                        )
                        .forEach { (time, event) ->
                            Disclosure(event, time) {
                                Text(
                                    "Observation from demo fixture ${s.task.id}. Provider keys are excluded."
                                )
                            }
                        }
                }
            else ->
                Section("Typed tool evidence") {
                    CodeBlock(
                        "tool: ${evidence.tool}\ninput: ${evidence.source}\noutput: ${evidence.output}\npolicy: approved scope only\nverified_outcomes: ${s.task.verified}\nfinal_state: ${s.task.status.name}"
                    )
                    Muted("Tool output is untrusted data, not instructions.")
                }
        }
        Actions {
            Secondary("View checkpoint") { nav(SynapseRoute.CHECKPOINT) }
            Secondary("Explore rollback demo") { nav(SynapseRoute.UNDO) }
        }
    }
}

@Composable
private fun UndoDemo(s: DemoState, send: (DemoAction) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    ScreenPage(
        "DOWNLOADS ROLLBACK SCENARIO",
        "Restore with confidence",
        "This fixed sample transaction demonstrates restoration and conflict review, independently of the selected task.",
    ) {
        if (s.rollbackDone)
            Notice(
                "Demo restore verified",
                "The selected changes were reversed in the simulation. Your current ticket.png was preserved.",
                Tone.SUCCESS,
            )
        Section("Reversible changes") {
            VerifiedRow("604 moved files", "Original locations recorded")
            VerifiedRow("27 trash entries", "Retained copies available in this scenario")
            VerifiedRow("14 created archives", "Remove only if unchanged since creation")
        }
        Notice(
            "One conflict needs review",
            "Downloads/Images/ticket.png changed after this task.",
            Tone.WARNING,
        )
        Section("Resolve conflict") {
            ChoiceChips(listOf("Keep current", "Restore under new name"), s.restoreConflictChoice) {
                send(DemoAction.ConflictChoice(it))
            }
            Muted("Permanent deletion and unsupported system changes cannot be restored.")
        }
        Primary("Restore selected changes", enabled = !s.rollbackDone) { confirm = true }
    }
    if (confirm)
        ConfirmAction(
            "Apply this demo rollback?",
            "The selected conflict policy is: ${s.restoreConflictChoice}. The restore will be recorded as a new transaction.",
            "Restore",
            { confirm = false },
        ) {
            send(DemoAction.Rollback)
            confirm = false
        }
}
