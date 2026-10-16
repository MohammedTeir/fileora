@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deviceagent.app.Tone

@Composable
internal fun DemoSettingsRoute(
    route: SynapseRoute,
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
    appearance: String,
    onAppearance: (String) -> Unit,
    reducedMotion: Boolean,
    onReducedMotion: (Boolean) -> Unit,
    reset: () -> Unit,
) {
    when (route) {
        SynapseRoute.PERMISSIONS -> PermissionsDesign(nav)
        SynapseRoute.APPEARANCE ->
            AppearanceDesign(appearance, onAppearance, reducedMotion, onReducedMotion, nav)
        SynapseRoute.ABOUT -> AboutDesign(nav)
        SynapseRoute.PROVIDERS -> ProviderCatalogDemo(s, send, nav)
        SynapseRoute.CREDENTIALS -> CredentialCatalogDemo(s, send, nav)
        SynapseRoute.MODELS -> ModelCatalogDemo(s, send, nav)
        SynapseRoute.PRIVACY -> PrivacyCatalogDemo(s, send, nav)
        SynapseRoute.USAGE -> UsageCatalogDemo(s, send, nav)
        SynapseRoute.SECURITY -> SecurityCatalogDemo(s, send, nav)
        SynapseRoute.MEMORY -> MemoryCatalogDemo(s, send, nav)
        SynapseRoute.SCHEDULE -> ScheduleDemo(s, send, nav)
        SynapseRoute.SETTINGS -> SettingsOverviewDemo(s, send, nav, reset)
        else -> Unit
    }
}

@Composable
private fun ScheduleDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var edit by rememberSaveable { mutableStateOf(false) }
    var goal by rememberSaveable(s.scheduleGoal) { mutableStateOf(s.scheduleGoal) }
    var time by rememberSaveable(s.scheduleTime) { mutableStateOf(s.scheduleTime) }
    val schedules =
        listOf(s.scheduleGoal, "Backup photos", "Clean storage", "Generate device report")
    DesignPage(
        SynapseRoute.SCHEDULE,
        "Schedules",
        "Set up recurring or one-time tasks. This demo does not register background jobs.",
        nav,
    ) {
        Section("") { InlineStats("Scheduled tasks" to "4", "Next task" to s.scheduleGoal) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f)) { DesignSearch(query, { query = it }, "Search schedules…") }
            DesignAction("Create schedule", primary = true) { edit = true }
        }
        DesignSegments(listOf("All", "Active", "Paused", "Recurring"), filter) { filter = it }
        DesignSection("Upcoming", SynapseIcon.SCHEDULES) {
            schedules
                .filter { it.contains(query, true) }
                .forEachIndexed { i, title ->
                    val enabled = if (i == 0) s.scheduleEnabled else !s.flag("schedulePaused$i")
                    if (filter != "Active" || enabled)
                        if (filter != "Paused" || !enabled)
                            DesignRow(
                                listOf(
                                    SynapseIcon.PLAY,
                                    SynapseIcon.IMAGE,
                                    SynapseIcon.DELETE,
                                    SynapseIcon.FILE,
                                )[i],
                                title,
                                if (i == 0) "Every day at ${s.scheduleTime}" else "Weekly · 02:00",
                                if (i == 0) "Today, ${s.scheduleTime}" else "Weekly, 02:00",
                                onClick = {
                                    goal = title
                                    edit = true
                                },
                            )
                }
        }
        DesignSection("All schedules (4)", SynapseIcon.SCHEDULES) {
            schedules.forEachIndexed { i, title ->
                SwitchSetting(
                    title,
                    if (i == 0) "Every day at ${s.scheduleTime} · Next run today"
                    else "Weekly · Next run 02:00",
                    if (i == 0) s.scheduleEnabled else !s.flag("schedulePaused$i"),
                ) { enabled ->
                    if (i == 0) send(DemoAction.Schedule(s.scheduleGoal, s.scheduleTime, enabled))
                    else send(DemoAction.Toggle("schedulePaused$i", !enabled))
                }
            }
        }
        Secondary("View history") { nav(SynapseRoute.ACTIVITY) }
    }
    if (edit)
        DesignDialog(
            onDismissRequest = { edit = false },
            title = { Text("Demo schedule") },
            text = {
                Column {
                    OutlinedTextField(
                        goal,
                        { goal = it },
                        label = { Text("Task goal") },
                        isError = goal.isBlank(),
                    )
                    OutlinedTextField(
                        time,
                        { time = it },
                        label = { Text("Time (HH:mm)") },
                        isError = !Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(time),
                    )
                    Muted(
                        "Saved to the demo scenario only. Live schedules require their own approval."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    {
                        send(DemoAction.Schedule(goal, time, true))
                        edit = false
                    },
                    enabled =
                        goal.isNotBlank() && Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(time),
                ) {
                    Text("Save schedule")
                }
            },
            dismissButton = { TextButton({ edit = false }) { Text("Cancel") } },
        )
}

@Composable
internal fun TaskResultDesign(s: DemoState, nav: (SynapseRoute) -> Unit) {
    var selected by rememberSaveable { mutableStateOf("Converted files") }
    var preview by rememberSaveable { mutableStateOf<Int?>(null) }
    var upload by rememberSaveable { mutableStateOf(false) }
    val complete = s.task.status == DemoTaskStatus.VERIFIED
    val video = s.task.id.startsWith("V3-VIDEO")
    DesignPage(SynapseRoute.TASK_RESULT,if(complete) "Task completed" else "Task result",
        if (complete) "Synapse has finished executing the plan successfully." else "Review verified outcomes and recorded evidence.",nav,
        footer={ Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            DesignAction("Create a new task  →",Modifier.weight(1f),primary=true) {nav(SynapseRoute.AGENT)}
            DesignAction("Back to Agent",Modifier.weight(1f)) {nav(SynapseRoute.AGENT)}
        } }) {
        DesignSection("Task summary",SynapseIcon.FILE,"View plan",{nav(SynapseRoute.PLAN)},subtitle=if(video) videoDemoDescription else s.task.title) {
            ReferenceMetricCards(Triple(SynapseIcon.FILE,"${s.task.verified} files","Processed"),Triple(SynapseIcon.FOLDER,"1 folder","Created"),Triple(SynapseIcon.SETTINGS,if(complete) "6 steps" else "In progress","${if(complete) "Completed" else "Verification"}"),Triple(SynapseIcon.HISTORY,"4 min 28 sec","Total time"))
        }
        if(!complete) DesignNotice("Work remains","Only ${s.task.verified} of ${s.task.total} items are verified. This task is not complete.",Tone.WARNING)
        DesignSection("Results",SynapseIcon.FOLDER,"Open folder",{nav(SynapseRoute.SANDBOX)},subtitle=if(complete) "All planned actions were completed successfully." else "Only verified outputs are listed.") {
            DesignSegments(listOf("Converted files", "Created folders", "Logs"),selected) {selected=it}
            when(selected) {
                "Converted files" -> {
                    if(s.task.verified==0) Muted("No verified outputs yet.")
                    repeat(s.task.verified.coerceAtMost(3)) { i ->
                        if(video) DemoVideoRow(i) {preview=i}
                        else DesignRow(SynapseIcon.FILE,"Verified output ${i+1}","Demo evidence · source preserved","Verified",onClick={nav(SynapseRoute.AUDIT)})
                    }
                }
                "Created folders" -> DesignRow(SynapseIcon.FOLDER,s.destination,"Approved output destination",onClick={nav(SynapseRoute.SANDBOX)})
                else -> CodeBlock("task: ${s.task.id}\nverified: ${s.task.verified}\nstatus: ${s.task.status}\nsource: demo fixture")
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                DesignSection("Output location",SynapseIcon.FOLDER,dense=true) {
                    Text("/storage/emulated/0/Download/\nConverted Videos",style=MaterialTheme.typography.bodySmall)
                    DesignAction("Open in file manager") {nav(SynapseRoute.SANDBOX)}
                }
            }
            Column(Modifier.weight(1f)) {
                DesignSection("What’s next?",SynapseIcon.TOOLS,subtitle="Try these suggestions:",dense=true) {
                    DesignDetailRow(SynapseIcon.PLAY,"Play the converted videos",onClick={preview=0})
                    DesignDetailRow(SynapseIcon.UPLOAD,"Upload to cloud storage",onClick={upload=true})
                    DesignDetailRow(SynapseIcon.PLUS,"Create a new task",onClick={nav(SynapseRoute.AGENT)})
                    DesignDetailRow(SynapseIcon.HISTORY,"View full activity log",onClick={nav(SynapseRoute.AUDIT)})
                }
            }
        }
    }
    if(preview!=null) DesignDialog(onDismissRequest={preview=null},title={Text("${demoVideos[preview!!].name}_converted.mp4")},text={
        Column {VideoThumbnail(preview!!,Modifier.fillMaxWidth().height(170.dp));Muted("Demo thumbnail preview. The original video file is not stored by this simulation.")}
    },confirmButton={TextButton({preview=null}) {Text("Close")}})
    DemoDetailDialog(if(upload) "Demo upload preview" else null,{upload=false},"The three verified sample outputs would be shared only after provider selection and explicit upload approval. No media is uploaded in Demo mode.")
}
