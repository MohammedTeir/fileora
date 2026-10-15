@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
            DesignRow(SynapseIcon.APPROVAL,"Organize Downloads folder","Completed · 12 files moved","2 hours ago",onClick = { send(DemoAction.SelectTask("SYN-2044"));nav(SynapseRoute.TASK_RESULT) })
            DesignRow(SynapseIcon.ACTIVITY,"Convert videos to MP4","Processing · 3 of 5 files","32 minutes ago",onClick = { send(DemoAction.SelectTask("SYN-2048"));nav(SynapseRoute.MONITOR) })
            DesignRow(SynapseIcon.TERMINAL,"Run Python project","Pending approval","1 hour ago",onClick = { nav(SynapseRoute.PLAN) })
        }
    }
}

@Composable
private fun AttachmentsDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var preview by rememberSaveable { mutableStateOf<String?>(null) }
    var picker by rememberSaveable { mutableStateOf(false) }
    val choices = listOf("screenshot_error.png","error_log.txt","PythonProject","Movies/Trips")
    DesignPage(SynapseRoute.ATTACHMENTS,"Add attachments","Add files, images, folders, or other context to help Synapse understand your task better.",nav) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(SynapseIcon.FILE to "Add files",SynapseIcon.IMAGE to "Add images",SynapseIcon.FOLDER to "Add folder",SynapseIcon.APPS to "Add app").forEach { (icon,label) ->
                Surface(onClick = { picker = true },modifier = Modifier.weight(1f),shape = RoundedCornerShape(10.dp),color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.padding(8.dp).heightIn(min = 70.dp),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(8.dp)) { AssetIcon(icon);Text(label,style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
        OutlinedButton({ picker = true },Modifier.fillMaxWidth().heightIn(min = 68.dp),shape = RoundedCornerShape(12.dp)) { AssetIcon(SynapseIcon.UPLOAD);Spacer(Modifier.width(8.dp));Text("Tap to browse sample files") }
        DesignSection("Attached items (${s.attachments.size})", action = "Clear all",onAction = { s.attachments.forEach { send(DemoAction.Attachment(it,false)) } }) {
            if(s.attachments.isEmpty()) Muted("No attachments yet. Choose a sample above.")
            s.attachments.forEach { name ->
                DesignRow(if(name.endsWith("png")) SynapseIcon.IMAGE else if(name.endsWith("txt")) SynapseIcon.FILE else SynapseIcon.FOLDER,name,"Local sample · not uploaded",onClick = { preview = name },trailing = {
                    IconButton({ preview = name }) { AssetIcon(SynapseIcon.SEARCH,"Preview $name",size = 16.dp) }
                    IconButton({ send(DemoAction.Attachment(name,false)) }) { AssetIcon(SynapseIcon.DELETE,"Remove $name",size = 16.dp) }
                })
            }
        }
        DesignSection("Suggestions") {
            DesignRow(SynapseIcon.IMAGE,"Take screenshot","Attach an example capture",onClick = { send(DemoAction.Attachment("screenshot_error.png",true)) })
            DesignRow(SynapseIcon.FILE,"Add logs","Include error logs",onClick = { send(DemoAction.Attachment("error_log.txt",true)) })
        }
        Notice("Why add attachments?","Attachments help Synapse understand your task and diagnose issues. Sharing remains subject to your privacy settings.")
        FullButton("Continue  →") { nav(SynapseRoute.PLAN) }
        Secondary("Privacy settings") { nav(SynapseRoute.PRIVACY) }
    }
    if(picker) ModalBottomSheet(onDismissRequest = { picker = false }) {
        Column(Modifier.padding(16.dp)) {
            Text("Choose demo context",style = MaterialTheme.typography.titleLarge)
            choices.forEach { name -> SwitchSetting(name,"Sample only",name in s.attachments) { send(DemoAction.Attachment(name,it)) } }
            FullButton("Use selected context") { picker = false }
        }
    }
    DemoDetailDialog(preview,{ preview = null },"Local attachment preview. File content and media are not uploaded by this demo.")
}

@Composable
private fun PlanSummary(s: DemoState, nav: (SynapseRoute) -> Unit) {
    DesignSection("Task summary",SynapseIcon.FILE,"Edit request",{ nav(SynapseRoute.AGENT) }) {
        Text("Organize Downloads into folders, create archives, and verify the results.",style = MaterialTheme.typography.bodySmall)
        InlineStats("Input files" to "618", "Destination" to "1 folder", "Estimated time" to "5–10 min", "Planned actions" to "6 steps")
    }
}

private val planSteps = listOf(
    Triple(SynapseIcon.FOLDER,"Check and create output folder","Verify the selected destination and create missing folders."),
    Triple(SynapseIcon.FILE,"Analyze input files","Check formats, sizes, and hashes before changes."),
    Triple(SynapseIcon.PLAY,"Create archives","Compress 14 source archives; retain originals."),
    Triple(SynapseIcon.FOLDER,"Organize files","Move 604 files into the selected category folders."),
    Triple(SynapseIcon.SECURITY,"Verify results","Check every output and record its evidence."),
    Triple(SynapseIcon.AUDIT,"Generate summary","Show results, skipped items, and issues."),
)

@Composable
private fun PlannedSteps() {
    DesignSection("Planned steps (6)",SynapseIcon.AUDIT) {
        planSteps.forEachIndexed { i,(icon,title,detail) ->
            DesignRow(icon,"${i+1}  $title",detail)
        }
    }
}

@Composable
private fun PlanPermissions(nav: (SynapseRoute) -> Unit) {
    DesignSection("Permissions required (3)",SynapseIcon.PERMISSIONS,"Review all",{ nav(SynapseRoute.PERMISSIONS) }) {
        DesignRow(SynapseIcon.FOLDER,"File system access","Read input files", "Required")
        DesignRow(SynapseIcon.FILE,"Create and modify files","Write only inside the selected folder", "Required")
        DesignRow(SynapseIcon.SECURITY,"Scoped approval","Review moves before execution", "Required")
    }
}

@Composable
private fun PlanDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    DesignPage(SynapseRoute.PLAN,"Plan preview","Review the generated plan. Check the steps, permissions, and estimated time before approving.",nav) {
        PlanSummary(s,nav)
        PlannedSteps()
        PlanPermissions(nav)
        Disclosure("Edit destination & exclusions",s.destination) {
            OutlinedTextField(s.destination,{ send(DemoAction.DestinationChanged(it)) },Modifier.fillMaxWidth(),label = { Text("Destination folder") },isError = s.destination.isBlank(),singleLine = true)
            SwitchSetting("Review duplicate trash operations","Each destructive scope requires consent.",s.includeTrash) { send(DemoAction.IncludeTrash(it)) }
        }
        FullButton("Approve & Run",s.destination.isNotBlank()) { nav(SynapseRoute.APPROVAL) }
        Secondary("Edit request") { nav(SynapseRoute.AGENT) }
    }
}

@Composable
private fun ApprovalDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    if(s.includeTrash) { ScopedApprovalDemo(s,send,nav);return }
    DesignPage(SynapseRoute.APPROVAL,"Approve actions","Review the planned actions, permissions, and potential changes before starting the task.",nav) {
        PlanSummary(s,nav)
        PlannedSteps()
        PlanPermissions(nav)
        Notice("You remain in control","Approval applies to revision ${s.planRevision}, destination ${s.destination}, and the listed actions only. Changes invalidate approval.")
        FullButton("Approve scoped plan",s.destination.isNotBlank()) { send(DemoAction.Approve(s.planRevision));nav(SynapseRoute.MONITOR) }
        Actions { Secondary("Edit plan") { nav(SynapseRoute.PLAN) };Secondary("Cancel") { nav(SynapseRoute.AGENT) } }
    }
}

@Composable
private fun ScopedApprovalDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
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
    var cancel by rememberSaveable { mutableStateOf(false) }
    DesignPage(SynapseRoute.MONITOR,"Monitor progress","Synapse is executing the approved plan. Monitor progress and stop if needed.",nav) {
        DesignSection("Task in progress",SynapseIcon.PLAY, s.task.status.name.lowercase()) {
            Text(s.task.title,style = MaterialTheme.typography.bodySmall)
            TaskProgress(s.task)
        }
        Section("") { InlineStats("Input files" to "${s.task.total}","Verified" to "${s.task.verified}","Blocked" to "${s.task.blocked}","Elapsed" to "03:24") }
        DesignSection("Current step",SynapseIcon.PLAY) {
            Text(if(s.task.status == DemoTaskStatus.VERIFIED) "Verification complete" else "Verify output ${s.task.settled+1}",style = MaterialTheme.typography.titleSmall)
            Muted("Source retained · scoped output · verification required")
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Inspect","Plan","Execute","Verify").forEach { stage -> Column(horizontalAlignment = Alignment.CenterHorizontally) { AssetIcon(if(stage=="Verify") SynapseIcon.ACTIVITY else SynapseIcon.APPROVAL,size = 16.dp);Text(stage,style = MaterialTheme.typography.labelSmall) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1.2f)) {
                DesignSection("Live log",SynapseIcon.AUDIT) {
                    demoEvents.forEach { (time,event) -> Text("$time  ✓  $event",Modifier.padding(vertical = 5.dp),style = MaterialTheme.typography.bodySmall) }
                }
            }
            Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DesignSection("Current output",SynapseIcon.FOLDER) { AssetIcon(SynapseIcon.FILE,size = 40.dp);Muted("Local output preview");LinearProgressIndicator(progress = { s.task.verified.toFloat()/s.task.total.coerceAtLeast(1) },modifier = Modifier.fillMaxWidth()) }
                DesignSection("Evidence",SynapseIcon.SECURITY) { Muted("File exists\nDuration checked\nSource preserved");TextButton({ nav(SynapseRoute.AUDIT) }) { Text("View all") } }
            }
        }
        Actions {
            Secondary("Stop execution",s.task.status in listOf(DemoTaskStatus.RUNNING,DemoTaskStatus.PAUSED)) { cancel = true }
            Primary(if(s.task.status == DemoTaskStatus.PAUSED) "Resume" else "Pause",s.task.status in listOf(DemoTaskStatus.RUNNING,DemoTaskStatus.PAUSED)) { send(DemoAction.PauseResume) }
        }
        Secondary("Simulate next verified item",s.task.status == DemoTaskStatus.RUNNING) { send(DemoAction.Advance) }
        Actions { Secondary("Checkpoint") { nav(SynapseRoute.CHECKPOINT) };Secondary("Review recovery") { nav(SynapseRoute.RECOVERY) };Secondary("View result") { nav(SynapseRoute.TASK_RESULT) } }
    }
    if(cancel) ConfirmAction("Cancel this demo task?","Completed changes remain in the audit. Review rollback separately.","Cancel task",{ cancel=false }) { send(DemoAction.Cancel);cancel=false }
}

@Composable
private fun RecoveryDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var selected by rememberSaveable { mutableStateOf("Retry with different settings") }
    var alternative by rememberSaveable { mutableStateOf(false) }
    var ask by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    DesignPage(SynapseRoute.RECOVERY,if(s.recoveryVerified) "Recovery verified" else if(s.recoveryAborted) "Recovery stopped" else "Task needs recovery","Synapse encountered an issue. Review the error and choose how to proceed.",nav) {
        DesignSection("Task summary",SynapseIcon.FILE) { Text("Extract lesson audio",style = MaterialTheme.typography.titleSmall);InlineStats("Input files" to "156","Failed step" to "Audio encode","Elapsed" to "02:14") }
        Notice("Error occurred","The installed FFmpeg profile does not provide the MP3 encoder.",Tone.ERROR) {
            CodeBlock("Unknown encoder 'libmp3lame'\nexit code: 1\ncomplete outputs: 0")
        }
        DesignSection("Recovery options",SynapseIcon.RECOVERY,"${s.attempts} / 3 attempts") {
            listOf("Retry with different settings","Try alternative tool","Provide a different file").forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { selected=option }.heightIn(min=48.dp),verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected==option,{ selected=option });Column { Text(option,style = MaterialTheme.typography.titleSmall);Muted(if(option.startsWith("Retry")) "Review a verified FFmpeg package profile" else if(option.startsWith("Try")) "Use AAC instead; explicit consent required" else "Add clarification before proceeding") }
                }
            }
        }
        Disclosure("Preview of recovery plan","Source and destination are rechecked.",initiallyOpen=true) {
            listOf("Inspect encoder availability","Verify package source and checksum","Retry only after the prerequisite changes","Validate output and preserve the source").forEachIndexed { i,t -> Text("${i+1}. $t",style=MaterialTheme.typography.bodySmall) }
        }
        FullButton("Approve recovery",!s.recoveryAborted && !s.recoveryVerified && s.attempts<3) {
            when(selected) {
                "Try alternative tool" -> alternative=true
                "Provide a different file" -> ask=true
                else -> if(s.packageName=="FFmpeg" && s.packageStep==4) send(DemoAction.RetryRecovery) else { send(DemoAction.PackageSelected("FFmpeg"));nav(SynapseRoute.PACKAGE_DETAIL) }
            }
        }
        Actions { Secondary("Abort task",!s.recoveryVerified && !s.recoveryAborted) { send(DemoAction.AbortRecovery) };Secondary("Ask for help") { ask=true } }
        if(s.recoveryNote.isNotBlank()) Notice("Your clarification",s.recoveryNote)
        if(s.recoveryVerified) Primary("Return to task") { send(DemoAction.SelectTask("SYN-2049"));nav(SynapseRoute.MONITOR) }
    }
    if(alternative) ConfirmAction("Use AAC instead of MP3?","This changes the output format. Existing sources remain unchanged.","Approve AAC output",{alternative=false}) { send(DemoAction.Alternative);alternative=false }
    if(ask) AlertDialog(onDismissRequest={ask=false},title={Text("What should the agent know?")},text={OutlinedTextField(input,{input=it},label={Text("Preferred format or destination")})},confirmButton={TextButton({send(DemoAction.RecoveryInput(input));ask=false},enabled=input.isNotBlank()){Text("Add clarification")}},dismissButton={TextButton({ask=false}){Text("Cancel")}})
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
