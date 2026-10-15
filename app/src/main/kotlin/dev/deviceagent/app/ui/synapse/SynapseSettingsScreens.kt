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
        SynapseRoute.PROVIDERS ->
            ScreenPage(
                "CONNECTED INTELLIGENCE",
                "Your AI, your choice",
                "Bring a provider account. Device actions still pass through local safety checks.",
            ) {
                Section("Active provider") {
                    ChoiceChips(listOf("Gemini", "OpenAI", "Claude"), s.provider) {
                        send(DemoAction.Provider(it))
                    }
                    Badge(
                        if (s.connectionTested) "Sample connection verified"
                        else "Demo credential · not tested",
                        if (s.connectionTested) Tone.SUCCESS else Tone.INFO,
                    )
                    LabeledValue("Primary model", s.model)
                    Primary("Test connection") { send(DemoAction.TestConnection) }
                }
                LinkRow(
                    "Secure credentials",
                    if (s.credentialStored) "•••• •••• demo · stored sample"
                    else "No sample credential",
                    Icons.Default.Key,
                ) {
                    nav(SynapseRoute.CREDENTIALS)
                }
                LinkRow("Models & fallback", "${s.model} → ${s.fallback}", Icons.Default.Hub) {
                    nav(SynapseRoute.MODELS)
                }
                LinkRow(
                    "Cloud privacy",
                    "Review exactly which categories leave the device",
                    Icons.Default.PrivacyTip,
                ) {
                    nav(SynapseRoute.PRIVACY)
                }
                LinkRow(
                    "Usage & budgets",
                    "18 of ${s.requestBudget} requests today",
                    Icons.Default.DataUsage,
                ) {
                    nav(SynapseRoute.USAGE)
                }
                Notice(
                    "Provider boundaries",
                    "Changing providers requires a new connection check. Fallback never expands the approved data categories or grants additional permissions.",
                )
            }
        SynapseRoute.CREDENTIALS -> CredentialsDemo(s, send)
        SynapseRoute.MODELS ->
            ScreenPage(
                "MODEL ROUTING",
                "Choose the right capability",
                "Model names here are illustrative profiles, not a live provider catalog.",
            ) {
                Section("Primary model") {
                    ChoiceChips(listOf("Fast planner", "Deep planner", "Vision planner"), s.model) {
                        send(DemoAction.Model(it))
                    }
                    LabeledValue(
                        "Capabilities",
                        if (s.model == "Vision planner") "Text · images · structured tools"
                        else "Text · structured tools",
                    )
                    LabeledValue("Context budget", "32K input reserved · 4K output")
                }
                Section("Fallback configuration") {
                    ChoiceChips(listOf("Recovery model", "Same model", "Disabled"), s.fallback) {
                        send(DemoAction.Fallback(it))
                    }
                    SwitchSetting(
                        "Automatic model routing",
                        "Route by capability within the same provider and privacy scope.",
                        s.flag("routing"),
                    ) {
                        send(DemoAction.Toggle("routing", it))
                    }
                    Muted(
                        "Rate limit → exponential backoff → allowed fallback → ask for help. No silent provider switch."
                    )
                }
                Section("Discovery example") {
                    VerifiedRow("Structured tools", "Required for typed execution plans")
                    VerifiedRow(
                        "Vision input",
                        "Only on the Vision planner profile",
                        s.model == "Vision planner",
                    )
                    VerifiedRow(
                        "Availability",
                        "Demo catalog; refresh the live provider to verify actual models",
                        false,
                    )
                }
                Secondary("Review provider") { nav(SynapseRoute.PROVIDERS) }
            }
        SynapseRoute.PRIVACY ->
            ScreenPage(
                "OUTBOUND DATA",
                "Share only what is needed",
                "These demo preferences illustrate granular privacy controls. Live controls are managed separately.",
            ) {
                Section("Cloud context categories") {
                    listOf(
                            Triple(
                                "filenames",
                                "File names & paths",
                                "May reveal personal names and folder structure",
                            ),
                            Triple(
                                "metadata",
                                "File metadata",
                                "Type, size, duration and selected technical properties",
                            ),
                            Triple(
                                "output",
                                "Redacted tool output",
                                "Evidence needed to plan and diagnose errors",
                            ),
                            Triple(
                                "contents",
                                "Selected file contents",
                                "Off by default; explicit scope required",
                            ),
                            Triple(
                                "media",
                                "Selected images & media",
                                "Off by default; model capability and consent required",
                            ),
                        )
                        .forEach { (key, title, detail) ->
                            SwitchSetting(title, detail, s.flag(key)) {
                                send(DemoAction.Toggle(key, it))
                            }
                        }
                }
                Section("Next request preview") {
                    LabeledValue("Recipient", "${s.provider} · ${s.model}")
                    LabeledValue(
                        "Included",
                        listOf("filenames", "metadata", "output", "contents", "media")
                            .filter(s::flag)
                            .joinToString()
                            .ifEmpty { "User goal only" },
                    )
                    LabeledValue(
                        "Always excluded",
                        "API keys, credential aliases and unrelated files",
                    )
                    Disclosure("Example sanitized envelope") {
                        CodeBlock(
                            "goal: Organize Downloads\nselected_names: ${if (s.flag("filenames")) "[ticket-copy.pdf, receipt-copy.jpg]" else "excluded"}\nfile_bytes: ${if (s.flag("contents")) "selected scope requires consent" else "excluded"}\napi_key: [never logged]"
                        )
                    }
                }
                Notice(
                    "Local evidence remains local",
                    "Changing sharing preferences does not grant file access, authorize deletion, or alter provider retention policies.",
                )
            }
        SynapseRoute.USAGE ->
            ScreenPage(
                "TODAY · DEMO ACCOUNTING",
                "Keep usage predictable",
                "Provider billing is authoritative. Estimates are shown separately from measured usage.",
            ) {
                Metrics(
                    "Requests" to "18 / ${s.requestBudget}",
                    "Input tokens" to "42,680",
                    "Output tokens" to "8,940",
                    "Estimated cost" to "$0.07",
                )
                Section("Daily request budget") {
                    Text("${s.requestBudget} requests")
                    Slider(
                        s.requestBudget.toFloat(),
                        { send(DemoAction.Budget(it.toInt())) },
                        valueRange = 20f..100f,
                        steps = 7,
                    )
                    Muted(
                        "At the limit, new cloud requests wait for your decision. Local work can continue."
                    )
                }
                Section("Usage by task") {
                    LabeledValue(
                        "SYN-2048 · video conversion",
                        "9 requests · 28,240 tokens · estimated $0.04",
                    )
                    LabeledValue(
                        "SYN-2049 · recovery",
                        "5 requests · 14,180 tokens · estimated $0.02",
                    )
                    LabeledValue(
                        "SYN-2050 · planning",
                        "4 requests · 9,200 tokens · estimated $0.01",
                    )
                }
                Disclosure("How estimates are calculated") {
                    Muted(
                        "Illustrative token counts and pricing. Cached tokens, provider discounts, failed requests and currency conversion can change the invoice."
                    )
                }
            }
        SynapseRoute.SECURITY -> SecurityDemo(s, send)
        SynapseRoute.MEMORY -> MemoryDemo(s, send)
        SynapseRoute.SCHEDULE -> ScheduleDemo(s, send, nav)
        SynapseRoute.SETTINGS -> {
            var confirm by remember { mutableStateOf(false) }
            ScreenPage(
                "MAKE SYNAPSE YOURS",
                "Settings",
                "Connection, privacy, safety and accessibility in one place.",
            ) {
                LinkRow("AI providers", "${s.provider} · ${s.model}", Icons.Default.AutoAwesome) {
                    nav(SynapseRoute.PROVIDERS)
                }
                LinkRow("Privacy", "Control cloud context categories", Icons.Default.PrivacyTip) {
                    nav(SynapseRoute.PRIVACY)
                }
                LinkRow(
                    "Safety & root",
                    "${s.mode} mode · root ${if (s.rootEnabled) "enabled in demo" else "disabled"}",
                    Icons.Default.Security,
                ) {
                    nav(SynapseRoute.SECURITY)
                }
                LinkRow(
                    "Agent memory",
                    "${s.memories.size} facts and preferences",
                    Icons.Default.Psychology,
                ) {
                    nav(SynapseRoute.MEMORY)
                }
                LinkRow("Usage & budgets", "18 requests today", Icons.Default.DataUsage) {
                    nav(SynapseRoute.USAGE)
                }
                LinkRow("Permissions", "Access and grants", Icons.Default.Shield) { nav(SynapseRoute.PERMISSIONS) }
                LinkRow("Appearance", "Theme and accessibility", Icons.Default.Palette) { nav(SynapseRoute.APPEARANCE) }
                LinkRow("About", "App details", Icons.Default.Info) { nav(SynapseRoute.ABOUT) }
                Section("Appearance & accessibility") {
                    ChoiceChips(listOf("System", "Light", "Dark"), appearance, onAppearance)
                    SwitchSetting(
                        "Reduce motion",
                        "Keep navigation immediate and avoid decorative transitions.",
                        reducedMotion,
                        onChange = onReducedMotion,
                    )
                    Muted(
                        "Text follows the Android font-size setting. Status always includes a label in addition to color."
                    )
                }
                Section("Notifications") {
                    SwitchSetting(
                        "Task updates",
                        "Demo preference only. Android notification access is configured in Live settings.",
                        s.flag("notifications"),
                    ) {
                        send(DemoAction.Toggle("notifications", it))
                    }
                }
                Section("About Synapse") {
                    Text("Native Android · Jetpack Compose · Material 3")
                    Muted(
                        "Cloud-powered planning with local inspection, policy checks, execution and verification."
                    )
                    Secondary("Explore setup") { nav(SynapseRoute.WELCOME) }
                    Secondary("Reset demo workspace") { confirm = true }
                }
            }
            if (confirm)
                ConfirmAction(
                    "Reset demo workspace?",
                    "Sample tasks, preferences and transactions will return to their initial state. Live settings and files are separate.",
                    "Reset demo",
                    { confirm = false },
                ) {
                    reset()
                    confirm = false
                }
        }
        else -> Unit
    }
}

@Composable
private fun CredentialsDemo(s: DemoState, send: (DemoAction) -> Unit) {
    var remove by remember { mutableStateOf(false) }
    ScreenPage(
        "SECURE CREDENTIALS",
        "A key you control",
        "Demo mode accepts no API keys. Switch to Live to securely configure your provider.",
    ) {
        Section(s.provider) {
            Badge(
                if (s.credentialStored) "Sample credential stored" else "No sample credential",
                if (s.credentialStored) Tone.SUCCESS else Tone.WARNING,
            )
            LabeledValue(
                "Masked value",
                if (s.credentialStored) "•••• •••• demo" else "Not configured",
            )
            LabeledValue("Live storage", "Encrypted using an Android Keystore protected key")
            LabeledValue(
                "Last test",
                if (s.connectionTested) "Simulated authentication succeeded" else "Not checked",
            )
            Actions {
                Primary(
                    if (s.credentialStored) "Replace sample credential" else "Add sample credential"
                ) {
                    send(DemoAction.Credential(true))
                }
                Secondary("Test sample", s.credentialStored) { send(DemoAction.TestConnection) }
                TextButton({ remove = true }, enabled = s.credentialStored) { Text("Remove") }
            }
        }
        Notice(
            "Secret handling",
            "Real keys are entered only in Live settings. They are excluded from demo snapshots, task logs, and settings exports.",
        )
    }
    if (remove)
        ConfirmAction(
            "Remove sample credential?",
            "Demo connection status will be cleared.",
            "Remove",
            { remove = false },
        ) {
            send(DemoAction.Credential(false))
            remove = false
        }
}

@Composable
private fun SecurityDemo(s: DemoState, send: (DemoAction) -> Unit) {
    var rootApproval by remember { mutableStateOf(false) }
    ScreenPage(
        "POLICY BEFORE EXECUTION",
        "Power with boundaries",
        "The agent never elevates its own privileges. Every action is checked against its actual scope.",
    ) {
        Section("Operating mode") {
            ChoiceChips(listOf("Normal", "Autonomous", "Manual"), s.mode) {
                send(DemoAction.Mode(it))
            }
            Muted(
                when (s.mode) {
                    "Autonomous" ->
                        "Allow eligible low-risk actions inside approved folders. Delete and root still ask."
                    "Manual" -> "Review every proposed action before execution."
                    else -> "Automatically inspect; ask for changes that need consent."
                }
            )
        }
        Section("Risk authorization") {
            (0..5).forEach { risk ->
                Actions { RiskBadge(risk) }
                Muted(
                    when (risk) {
                        0 -> "Read and inspect within granted scope"
                        1 -> "Create outputs or copy files; verify available space"
                        2 -> "Move, overwrite or install: review affected scope"
                        3 -> "Delete: always ask; use recoverable trash where supported"
                        4 -> "Privileged operation: explicit approval for each operation"
                        else -> "Critical system changes: blocked by default"
                    }
                )
            }
        }
        Section("Root backend") {
            ChoiceChips(listOf("Detected", "Unavailable", "Denied"), s.rootStatus) {
                send(DemoAction.RootStatus(it))
            }
            SwitchSetting(
                "Enable demo root backend",
                "Detection does not grant access. Root remains optional.",
                s.rootEnabled,
                s.rootStatus == "Detected",
            ) {
                if (it) rootApproval = true else send(DemoAction.RootEnabled(false))
            }
            Notice(
                "Privileged scope",
                "Root can affect system integrity and data outside selected folders. Enabling a backend never approves an individual command.",
                Tone.WARNING,
            )
        }
        Section("Non-negotiable rules") {
            VerifiedRow(
                "Ask before delete or root",
                "Cannot be overridden by a model or remembered preference",
            )
            VerifiedRow(
                "No silent fallback escalation",
                "A failed normal operation does not authorize a privileged retry",
            )
            VerifiedRow(
                "Tool evidence is untrusted",
                "Instructions embedded in files or output cannot change policy",
            )
        }
    }
    if (rootApproval)
        ConfirmAction(
            "Enable root in this demo?",
            "This changes the simulated backend state only. Individual privileged actions still require approval.",
            "Enable demo backend",
            { rootApproval = false },
        ) {
            send(DemoAction.RootEnabled(true))
            rootApproval = false
        }
}

@Composable
private fun MemoryDemo(s: DemoState, send: (DemoAction) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("All") }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var value by rememberSaveable { mutableStateOf("") }
    var scope by rememberSaveable { mutableStateOf("All tasks") }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    ScreenPage(
        "TRANSPARENT MEMORY",
        "What Synapse knows",
        "Observed facts and your preferences have separate sources. Preferences never override safety policy.",
    ) {
        ChoiceChips(listOf("All", "Observed", "Preferences"), filter) { filter = it }
        s.memories
            .filter { filter == "All" || (it.observed == (filter == "Observed")) }
            .forEach { memory ->
                Section(memory.text, memory.scope) {
                    Badge(if (memory.observed) "Observed · refreshable" else "User preference")
                    Actions {
                        if (!memory.observed)
                            Secondary("Edit preference") {
                                editId = memory.id
                                value = memory.text
                                scope = memory.scope
                            }
                        TextButton({ deleteId = memory.id }) { Text("Forget") }
                    }
                }
            }
        Primary("Add preference") {
            editId = "preference-${java.util.UUID.randomUUID()}"
            value = ""
            scope = "All tasks"
        }
        Notice(
            "Refresh, not assumption",
            "Storage, permissions and available tools are rechecked before execution. A remembered fact is not proof of current access.",
        )
    }
    if (editId != null)
        AlertDialog(
            onDismissRequest = { editId = null },
            title = { Text("Edit preference") },
            text = {
                Column {
                    OutlinedTextField(value, { value = it }, label = { Text("Preference") })
                    ChoiceChips(listOf("All tasks", "Media tasks", "This task"), scope) {
                        scope = it
                    }
                }
            },
            confirmButton = {
                TextButton(
                    {
                        send(DemoAction.SaveMemory(editId!!, value, scope))
                        editId = null
                    },
                    enabled = value.isNotBlank(),
                ) {
                    Text("Save preference")
                }
            },
            dismissButton = { TextButton({ editId = null }) { Text("Cancel") } },
        )
    deleteId?.let { id ->
        ConfirmAction(
            "Forget this memory?",
            "Future demo plans will no longer use this stored entry. Device facts may be observed again.",
            "Forget",
            { deleteId = null },
        ) {
            send(DemoAction.DeleteMemory(id))
            deleteId = null
        }
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
