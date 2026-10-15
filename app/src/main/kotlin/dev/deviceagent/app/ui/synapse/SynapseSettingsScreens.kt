@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
        DesignSearch(query, { query = it }, "Search schedules…")
        Primary("Create schedule") { edit = true }
        ChoiceChips(listOf("All", "Active", "Paused", "Recurring"), filter) { filter = it }
        DesignSection("Upcoming", SynapseIcon.SCHEDULES) {
            schedules
                .filter { it.contains(query, true) }
                .forEachIndexed { i, title ->
                    val enabled = if (i == 0) s.scheduleEnabled else !s.flag("schedulePaused$i")
                    if (filter != "Active" || enabled)
                        if (filter != "Paused" || !enabled)
                            DesignRow(
                                SynapseIcon.SCHEDULES,
                                title,
                                if (i == 0) "Every day at ${s.scheduleTime}" else "Weekly · 02:00",
                                "Next run",
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
                    if (i == 0) "Every day at ${s.scheduleTime}" else "Weekly · sample schedule",
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
    var selected by rememberSaveable { mutableStateOf("Results") }
    val complete = s.task.status == DemoTaskStatus.VERIFIED
    DesignPage(
        SynapseRoute.TASK_RESULT,
        if (complete) "Task completed" else "Task result",
        "Review verified outcomes and recorded evidence.",
        nav,
    ) {
        DesignSection("Task summary", SynapseIcon.FILE, "View plan", { nav(SynapseRoute.PLAN) }) {
            Text(s.task.title, style = MaterialTheme.typography.titleSmall)
            InlineStats(
                "Processed" to "${s.task.verified}",
                "Skipped" to "${s.task.skipped}",
                "Blocked" to "${s.task.blocked}",
                "Status" to s.task.status.name.lowercase(),
            )
        }
        if (!complete)
            Notice(
                "Work remains",
                "Only ${s.task.verified} of ${s.task.total} items are verified. This task is not complete.",
                Tone.WARNING,
            )
        DesignSection("Results", SynapseIcon.FOLDER) {
            ChoiceChips(listOf("Results", "Created folders", "Logs"), selected) { selected = it }
            when (selected) {
                "Results" -> {
                    if (s.task.verified == 0) Muted("No verified outputs yet.")
                    repeat(s.task.verified.coerceAtMost(3)) { i ->
                        DesignRow(
                            SynapseIcon.FILE,
                            "Verified output ${i+1}",
                            "Demo evidence · original preserved",
                            "Verified",
                            onClick = { nav(SynapseRoute.AUDIT) },
                        )
                    }
                    if (s.task.verified > 3)
                        Muted("${s.task.verified-3} additional verified outputs in the audit.")
                }
                "Created folders" ->
                    DesignRow(
                        SynapseIcon.FOLDER,
                        s.destination,
                        "Scoped output destination",
                        onClick = { nav(SynapseRoute.SANDBOX) },
                    )
                else ->
                    CodeBlock(
                        "task: ${s.task.id}\nverified: ${s.task.verified}\nstatus: ${s.task.status}\nsource: demo fixture"
                    )
            }
        }
        DesignSection("Output location", SynapseIcon.FOLDER) {
            Text(s.destination, style = MaterialTheme.typography.bodySmall)
            Secondary("Open in file manager") { nav(SynapseRoute.SANDBOX) }
        }
        DesignSection("What’s next?", SynapseIcon.TOOLS) {
            DesignRow(SynapseIcon.AGENT, "Create a new task", onClick = { nav(SynapseRoute.AGENT) })
            DesignRow(
                SynapseIcon.AUDIT,
                "View full activity log",
                onClick = { nav(SynapseRoute.AUDIT) },
            )
            DesignRow(SynapseIcon.UNDO, "Review undo", onClick = { nav(SynapseRoute.UNDO) })
        }
        FullButton("Back to Agent") { nav(SynapseRoute.AGENT) }
    }
}
