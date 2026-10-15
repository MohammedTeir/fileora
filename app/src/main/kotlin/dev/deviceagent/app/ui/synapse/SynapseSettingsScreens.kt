@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
        SynapseRoute.APPEARANCE -> AppearanceDesign(appearance, onAppearance, reducedMotion, onReducedMotion)
        SynapseRoute.ABOUT -> AboutDesign(nav)
        SynapseRoute.PROVIDERS -> ProviderCatalogDemo(s,send,nav)
        SynapseRoute.CREDENTIALS -> CredentialCatalogDemo(s,send,nav)
        SynapseRoute.MODELS -> ModelCatalogDemo(s,send,nav)
        SynapseRoute.PRIVACY -> PrivacyCatalogDemo(s, send, nav)
        SynapseRoute.USAGE -> UsageCatalogDemo(s, send, nav)
        SynapseRoute.SECURITY -> SecurityCatalogDemo(s, send, nav)
        SynapseRoute.MEMORY -> MemoryCatalogDemo(s, send, nav)
        SynapseRoute.SCHEDULE -> ScheduleDemo(s, send, nav)
        SynapseRoute.SETTINGS -> SettingsOverviewDemo(s,send,nav,reset)
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
    val schedules=listOf(s.scheduleGoal,"Backup photos","Clean storage","Generate device report")
    DesignPage(SynapseRoute.SCHEDULE,"Schedules","Set up recurring or one-time tasks. This demo does not register background jobs.",nav) {
        Section("") { InlineStats("Scheduled tasks" to "4","Next task" to s.scheduleGoal) }
        DesignSearch(query,{query=it},"Search schedules…")
        Primary("Create schedule"){edit=true}
        ChoiceChips(listOf("All","Active","Paused","Recurring"),filter){filter=it}
        DesignSection("Upcoming",SynapseIcon.SCHEDULES) {
            schedules.filter { it.contains(query,true) }.forEachIndexed { i,title ->
                val enabled=if(i==0) s.scheduleEnabled else !s.flag("schedulePaused$i")
                if(filter!="Active" || enabled) if(filter!="Paused" || !enabled) DesignRow(SynapseIcon.SCHEDULES,title,if(i==0) "Every day at ${s.scheduleTime}" else "Weekly · 02:00","Next run",onClick={goal=title;edit=true})
            }
        }
        DesignSection("All schedules (4)",SynapseIcon.SCHEDULES) {
            schedules.forEachIndexed { i,title -> SwitchSetting(title,if(i==0) "Every day at ${s.scheduleTime}" else "Weekly · sample schedule",if(i==0) s.scheduleEnabled else !s.flag("schedulePaused$i")) { enabled ->
                if(i==0) send(DemoAction.Schedule(s.scheduleGoal,s.scheduleTime,enabled)) else send(DemoAction.Toggle("schedulePaused$i",!enabled))
            } }
        }
        Secondary("View history"){nav(SynapseRoute.ACTIVITY)}
    }
    if(edit) AlertDialog(onDismissRequest={edit=false},title={Text("Demo schedule")},text={Column{
        OutlinedTextField(goal,{goal=it},label={Text("Task goal")},isError=goal.isBlank())
        OutlinedTextField(time,{time=it},label={Text("Time (HH:mm)")},isError=!Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(time))
        Muted("Saved to the demo scenario only. Live schedules require their own approval.")
    }},confirmButton={TextButton({send(DemoAction.Schedule(goal,time,true));edit=false},enabled=goal.isNotBlank() && Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(time)){Text("Save schedule")}},dismissButton={TextButton({edit=false}){Text("Cancel")}})
}

@Composable
internal fun PermissionsDesign(nav: (SynapseRoute) -> Unit) {
    ScreenPage("SETTINGS / PERMISSIONS", "Permissions", "Review access needed for device operations. Actual grants must be checked in Android settings.") {
        Notice("Permission status", "This design preview does not read or request Android permissions. Never interpret example values as actual grants.")
        Section("File access") {
            LinkRow("Selected folders", "Android Storage Access Framework; grant only the folders you choose", Icons.Default.Folder) { nav(SynapseRoute.SANDBOX) }
            Muted("Broad file access, if offered, requires a separate Android system consent flow.")
        }
        Section("Notifications") {
            LabeledValue("Task alerts", "Check notification permission in Live settings")
        }
        Section("Special access") {
            LabeledValue("Accessibility service", "Not requested or required by the current Synapse implementation")
            LabeledValue("Modify system settings", "Not requested")
            LabeledValue("Camera, microphone, location, contacts", "Not requested by the current app")
        }
        Secondary("Review security policy") { nav(SynapseRoute.SECURITY) }
    }
}

@Composable
internal fun AppearanceDesign(
    appearance: String,
    onAppearance: (String) -> Unit,
    reducedMotion: Boolean,
    onReducedMotion: (Boolean) -> Unit,
) {
    ScreenPage("SETTINGS / APPEARANCE", "Appearance", "Personalize the interface while keeping status and safety labels readable.") {
        Section("Theme") {
            ChoiceChips(listOf("Light", "Dark", "System"), appearance, onAppearance)
        }
        Section("Display & accessibility") {
            SwitchSetting("Reduce motion", "Minimize decorative transitions.", reducedMotion, onChange = onReducedMotion)
            LabeledValue("Text size", "Follows Android display and font settings")
            LabeledValue("Screen reader", "Uses Android TalkBack; configure in system accessibility settings")
            LabeledValue("Contrast", "Uses accessible semantic Material color roles")
        }
        Notice("Planned appearance options", "Custom accent swatches, typeface selection and density controls are visual concepts only; they are not currently persisted settings.")
    }
}

@Composable
internal fun AboutDesign(nav: (SynapseRoute) -> Unit) {
    ScreenPage("SETTINGS / ABOUT", "About Synapse", "Your AI-powered device operator.") {
        Section("Synapse") {
            Text("Native Android · Jetpack Compose · Material 3")
            Muted("Cloud-assisted planning, locally scoped permissions, execution evidence and review.")
        }
        Section("Build information") {
            Muted("Version and build identifiers are shown in Live mode from the installed application.")
            LabeledValue("Platform", "Android")
        }
        Section("Help & safety") {
            LinkRow("Permissions", "Review the access model", Icons.Default.Shield) { nav(SynapseRoute.PERMISSIONS) }
            LinkRow("Privacy", "Understand cloud data sharing", Icons.Default.PrivacyTip) { nav(SynapseRoute.PRIVACY) }
            LinkRow("Security", "Review policy and privileged operations", Icons.Default.Security) { nav(SynapseRoute.SECURITY) }
        }
    }
}

@Composable
internal fun TaskResultDesign(s: DemoState, nav: (SynapseRoute) -> Unit) {
    var selected by rememberSaveable { mutableStateOf("Results") }
    val complete = s.task.status == DemoTaskStatus.VERIFIED
    DesignPage(SynapseRoute.TASK_RESULT,if(complete) "Task completed" else "Task result","Review verified outcomes and recorded evidence.",nav) {
        DesignSection("Task summary",SynapseIcon.FILE,"View plan",{nav(SynapseRoute.PLAN)}) {
            Text(s.task.title,style=MaterialTheme.typography.titleSmall)
            InlineStats("Processed" to "${s.task.verified}","Skipped" to "${s.task.skipped}","Blocked" to "${s.task.blocked}","Status" to s.task.status.name.lowercase())
        }
        if(!complete) Notice("Work remains","Only ${s.task.verified} of ${s.task.total} items are verified. This task is not complete.",Tone.WARNING)
        DesignSection("Results",SynapseIcon.FOLDER) {
            ChoiceChips(listOf("Results","Created folders","Logs"),selected){selected=it}
            when(selected) {
                "Results" -> {
                    if(s.task.verified==0) Muted("No verified outputs yet.")
                    repeat(s.task.verified.coerceAtMost(3)) { i -> DesignRow(SynapseIcon.FILE,"Verified output ${i+1}","Demo evidence · original preserved","Verified",onClick={nav(SynapseRoute.AUDIT)}) }
                    if(s.task.verified>3) Muted("${s.task.verified-3} additional verified outputs in the audit.")
                }
                "Created folders" -> DesignRow(SynapseIcon.FOLDER,s.destination,"Scoped output destination",onClick={nav(SynapseRoute.SANDBOX)})
                else -> CodeBlock("task: ${s.task.id}\nverified: ${s.task.verified}\nstatus: ${s.task.status}\nsource: demo fixture")
            }
        }
        DesignSection("Output location",SynapseIcon.FOLDER) { Text(s.destination,style=MaterialTheme.typography.bodySmall);Secondary("Open in file manager"){nav(SynapseRoute.SANDBOX)} }
        DesignSection("What’s next?",SynapseIcon.TOOLS) {
            DesignRow(SynapseIcon.AGENT,"Create a new task",onClick={nav(SynapseRoute.AGENT)})
            DesignRow(SynapseIcon.AUDIT,"View full activity log",onClick={nav(SynapseRoute.AUDIT)})
            DesignRow(SynapseIcon.UNDO,"Review undo",onClick={nav(SynapseRoute.UNDO)})
        }
        FullButton("Back to Agent"){nav(SynapseRoute.AGENT)}
    }
}
