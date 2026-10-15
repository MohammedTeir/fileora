@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun PrivacyCatalogDemo(
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    var history by rememberSaveable { mutableStateOf("Keep for 30 days") }
    var downloads by rememberSaveable { mutableStateOf("Keep for 7 days") }
    var command by rememberSaveable { mutableStateOf("Keep for 14 days") }
    var clear by rememberSaveable { mutableStateOf(false) }
    DesignPage(
        SynapseRoute.PRIVACY,
        "Privacy",
        "Control your data, privacy, and what information is shared.",
        nav,
    ) {
        DesignSection("Privacy Overview", SynapseIcon.PRIVACY) {
            InlineStats(
                "Local processing" to "Enabled",
                "Cloud requests" to "Controlled",
                "Telemetry" to "Disabled",
            )
        }
        DesignSection("Data Sharing", SynapseIcon.UPLOAD) {
            listOf(
                    Triple(
                        "filenames",
                        "Send file names and paths",
                        "May contain personal names and folder structure.",
                    ),
                    Triple(
                        "contents",
                        "Send conversation content",
                        "Selected text only; off by default.",
                    ),
                    Triple(
                        "media",
                        "Send attached files",
                        "Selected media needs explicit consent.",
                    ),
                    Triple(
                        "metadata",
                        "Send device context",
                        "Device and file metadata within approved scope.",
                    ),
                    Triple(
                        "output",
                        "Send command execution data",
                        "Redacted tool output for analysis.",
                    ),
                )
                .forEach { (key, title, detail) ->
                    SwitchSetting(title, detail, s.flag(key)) { send(DemoAction.Toggle(key, it)) }
                }
        }
        DesignSection("Data Retention", SynapseIcon.MEMORY) {
            DesignChoice(
                "Conversation history",
                listOf("Keep for 7 days", "Keep for 30 days", "Delete after task"),
                history,
            ) {
                history = it
            }
            DesignChoice(
                "Downloaded files",
                listOf("Keep for 7 days", "Keep for 30 days", "Delete after task"),
                downloads,
            ) {
                downloads = it
            }
            DesignChoice(
                "Command history",
                listOf("Keep for 7 days", "Keep for 14 days", "Delete after task"),
                command,
            ) {
                command = it
            }
            Muted("Demo retention preferences do not delete live data.")
        }
        DesignSection("Privacy Controls", SynapseIcon.SETTINGS) {
            SwitchSetting(
                "Hide sensitive content",
                "Redact secrets and personal information.",
                !s.flag("showSensitive"),
            ) {
                send(DemoAction.Toggle("showSensitive", !it))
            }
            DesignRow(
                SynapseIcon.DELETE,
                "Clear sample sharing preferences",
                "Live settings are separate",
                onClick = { clear = true },
            )
        }
    }
    if (clear)
        ConfirmAction(
            "Clear sample sharing preferences?",
            "All demo outbound data categories will be disabled.",
            "Clear preferences",
            { clear = false },
        ) {
            listOf("filenames", "contents", "media", "metadata", "output").forEach {
                send(DemoAction.Toggle(it, false))
            }
            clear = false
        }
}

@Composable
internal fun UsageCatalogDemo(
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    var metric by rememberSaveable { mutableStateOf("Requests") }
    var period by rememberSaveable { mutableStateOf("Last 30 days") }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = listOf(Color(0xFF0867F9), Color(0xFF00B84A), Color(0xFF9333EA), Color(0xFFF59E0B))
    val values = listOf(620f, 380f, 180f, 70f)
    DesignPage(
        SynapseRoute.USAGE,
        "Usage",
        "Track your usage, view details, and set limits for AI providers.",
        nav,
    ) {
        DesignSection("Usage Overview", SynapseIcon.USAGE) {
            InlineStats(
                "API requests" to "1,250 / 10,000",
                "Input tokens" to "2.4M",
                "Output tokens" to "0.8M",
                "Total time" to "12.5 hours",
            )
            LinearProgressIndicator(progress = { .125f }, modifier = Modifier.fillMaxWidth())
        }
        DesignSection("Usage by Provider", SynapseIcon.USAGE) {
            ChoiceChips(listOf("Requests", "Input tokens", "Output tokens"), metric) { metric = it }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                    Canvas(
                        Modifier.fillMaxSize().padding(8.dp).semantics {
                            contentDescription =
                                "Demo $metric distribution: Gemini 49.6 percent, OpenAI 30.4 percent, Claude 14.4 percent, Custom 5.6 percent"
                        }
                    ) {
                        var start = -90f
                        values.forEachIndexed { i, v ->
                            val sweep = v / 1250f * 360
                            drawArc(colors[i], start, sweep, false, style = Stroke(14.dp.toPx()))
                            start += sweep
                        }
                    }
                    Text(
                        if (metric == "Requests") "1,250"
                        else if (metric == "Input tokens") "2.4M" else "0.8M",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Column(Modifier.weight(1f)) {
                    listOf("Google Gemini", "OpenAI", "Anthropic Claude", "Custom Provider")
                        .forEachIndexed { i, name ->
                            Text(name, style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = { values[i] / 1250f },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                color = colors[i],
                            )
                        }
                }
            }
        }
        val gridColor = MaterialTheme.colorScheme.outlineVariant
        DesignSection("Usage History", SynapseIcon.ACTIVITY) {
            DesignChoice("Period", listOf("Last 7 days", "Last 30 days"), period) { period = it }
            Canvas(
                Modifier.fillMaxWidth().height(100.dp).semantics {
                    contentDescription = "Illustrative usage history, $period"
                }
            ) {
                repeat(5) { n ->
                    val y = size.height * n / 4
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                val count = if (period == "Last 7 days") 7 else 30
                val gap = size.width / count
                repeat(count) { i ->
                    val height = (20 + (i * 37) % 80) / 100f * size.height
                    drawLine(
                        colors[0],
                        Offset(gap * i + gap * .25f, size.height),
                        Offset(gap * i + gap * .25f, size.height - height),
                        gap * .22f,
                    )
                    drawLine(
                        colors[1],
                        Offset(gap * i + gap * .55f, size.height),
                        Offset(gap * i + gap * .55f, size.height - height * .55f),
                        gap * .16f,
                    )
                    drawLine(
                        colors[2],
                        Offset(gap * i + gap * .78f, size.height),
                        Offset(gap * i + gap * .78f, size.height - height * .72f),
                        gap * .16f,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                    "Day 1",
                    "Day ${if(period=="Last 7 days")3 else 10}",
                    "Day ${if(period=="Last 7 days")5 else 20}",
                    "Day ${if(period=="Last 7 days")7 else 30}",
                )
                .forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
        DesignSection("Recent Requests", SynapseIcon.HISTORY) {
            repeat(4) { i ->
                DesignRow(
                    SynapseIcon.PROVIDERS,
                    listOf("Gemini", "OpenAI", "Claude", "Gemini")[i],
                    "Today · 12.4K tokens · 8.2s",
                    "Success",
                    onClick = { detail = "Request ${i+1}" },
                )
            }
        }
        DesignSection("Task budgets", SynapseIcon.SECURITY) {
            Text("${s.requestBudget} requests per task")
            DesignSlider(
                s.requestBudget.toFloat(),
                { send(DemoAction.Budget(it.toInt())) },
                valueRange = 20f..100f,
                label = "Task request budget",
            )
        }
    }
    DemoDetailDialog(
        detail,
        { detail = null },
        "Illustrative usage record. No provider is contacted by the demo.",
    )
}

@Composable
internal fun SecurityCatalogDemo(
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    var rootApproval by rememberSaveable { mutableStateOf(false) }
    DesignPage(
        SynapseRoute.SECURITY,
        "Security",
        "Control what Synapse can do on your device and keep your system safe.",
        nav,
    ) {
        DesignSection("Security Overview", SynapseIcon.SECURITY) {
            InlineStats(
                "Security status" to "Protected",
                "Execution mode" to s.mode,
                "Root access" to if (s.rootEnabled) "Demo enabled" else "Disabled",
                "Sandbox" to "Enabled",
            )
        }
        DesignSection("Execution Permissions", SynapseIcon.PERMISSIONS) {
            DesignChoice("Operating mode", listOf("Normal", "Autonomous", "Manual"), s.mode) {
                send(DemoAction.Mode(it))
            }
            DesignRow(
                SynapseIcon.TERMINAL,
                "Run terminal commands",
                "Scoped execution; risky commands require approval",
                "Ask",
            )
            DesignRow(
                SynapseIcon.FILE,
                "Access files and directories",
                "Only user-granted folders",
                "Ask",
            )
            DesignRow(
                SynapseIcon.PACKAGES,
                "Install and manage packages",
                "Review exact package and source",
                "Ask",
            )
            DesignRow(
                SynapseIcon.ROOT,
                "Change system settings",
                "Per-operation privileged approval",
                "Restricted",
            )
        }
        DesignSection("Risk Levels", SynapseIcon.WARNING) {
            DesignRow(
                SynapseIcon.APPROVAL,
                "Low risk",
                "Safe scoped reads and copies",
                "Policy checked",
            )
            DesignRow(
                SynapseIcon.WARNING,
                "Medium risk",
                "Review moves, overwrites, or installs",
                "Ask",
            )
            DesignRow(
                SynapseIcon.SECURITY,
                "High risk",
                "Delete and privileged operations always ask",
                "Always ask",
            )
        }
        DesignSection("Root Access", SynapseIcon.ROOT) {
            DesignChoice("Detection", listOf("Detected", "Unavailable", "Denied"), s.rootStatus) {
                send(DemoAction.RootStatus(it))
            }
            SwitchSetting(
                "Enable demo root backend",
                "Does not approve any privileged command.",
                s.rootEnabled,
                s.rootStatus == "Detected",
            ) {
                if (it) rootApproval = true else send(DemoAction.RootEnabled(false))
            }
        }
        DesignSection("Sandbox & Isolation", SynapseIcon.PACKAGES) {
            VerifiedRow(
                "Sandbox enforced",
                "Permissions and targets are rechecked before execution",
            )
            VerifiedRow(
                "No silent fallback escalation",
                "A failed normal operation never grants root",
            )
        }
        DesignSection("Security Events", SynapseIcon.AUDIT) {
            DesignRow(SynapseIcon.FOLDER, "Access file", "Read · approved scope", "Allowed")
            DesignRow(
                SynapseIcon.ROOT,
                "Install package",
                "Privileged action not approved",
                "Blocked",
            )
            DesignRow(SynapseIcon.DELETE, "Delete file", "Explicit consent missing", "Blocked")
        }
    }
    if (rootApproval)
        ConfirmAction(
            "Enable root in this demo?",
            "This changes the simulated backend only. Every privileged action still requires approval.",
            "Enable demo backend",
            { rootApproval = false },
        ) {
            send(DemoAction.RootEnabled(true))
            rootApproval = false
        }
}

@Composable
internal fun MemoryCatalogDemo(
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var value by rememberSaveable { mutableStateOf("") }
    var scope by rememberSaveable { mutableStateOf("All tasks") }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    DesignPage(
        SynapseRoute.MEMORY,
        "Memory",
        "Manage what Synapse remembers to provide a more helpful and contextual experience.",
        nav,
    ) {
        DesignSection("Memory Overview", SynapseIcon.MEMORY) {
            InlineStats(
                "Memory items" to "${s.memories.size}",
                "Storage used" to "48 MB",
                "Retention" to "30 days",
                "Memory" to "Enabled",
            )
        }
        DesignSection("Memory Controls", SynapseIcon.SETTINGS) {
            listOf(
                    "Conversation context",
                    "Important information",
                    "Device information",
                    "Commands and tasks",
                    "Personalization",
                    "Automatic summarization",
                )
                .forEach { title ->
                    SwitchSetting(title, "", !s.flag("memoryOff$title")) {
                        send(DemoAction.Toggle("memoryOff$title", !it))
                    }
                }
        }
        DesignSection("Memory Storage", SynapseIcon.MEMORY) {
            LinearProgressIndicator(progress = { .24f }, modifier = Modifier.fillMaxWidth())
            SmallValue("Used", "48 MB / 200 MB")
        }
        DesignSection("Stored Memory", SynapseIcon.HISTORY) {
            DesignSearch(query, { query = it }, "Search memory…")
            ChoiceChips(listOf("All", "Observed", "Preferences"), filter) { filter = it }
            s.memories
                .filter {
                    it.text.contains(query, true) &&
                        (filter == "All" || it.observed == (filter == "Observed"))
                }
                .forEach { memory ->
                    DesignRow(
                        if (memory.observed) SynapseIcon.DEVICE else SynapseIcon.MEMORY,
                        memory.text,
                        memory.scope,
                        if (memory.observed) "Observed" else "Preference",
                        onClick = {
                            if (memory.observed) deleteId = memory.id
                            else {
                                editId = memory.id
                                value = memory.text
                                scope = memory.scope
                            }
                        },
                    )
                }
        }
        Actions {
            Primary("Add preference") {
                editId = "preference-${java.util.UUID.randomUUID()}"
                value = ""
                scope = "All tasks"
            }
            Secondary("Clear preferences") { deleteId = "all" }
        }
        Muted(
            "Preferences never override safety policy. Observed facts are rechecked before execution."
        )
    }
    if (editId != null)
        DesignDialog(
            onDismissRequest = { editId = null },
            title = { Text("Edit preference") },
            text = {
                Column {
                    OutlinedTextField(value, { value = it }, label = { Text("Preference") })
                    DesignChoice("Scope", listOf("All tasks", "Media tasks", "This task"), scope) {
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
            dismissButton = {
                TextButton({
                    deleteId = editId
                    editId = null
                }) {
                    Text("Forget")
                }
            },
        )
    if (deleteId != null)
        ConfirmAction(
            "Forget this memory?",
            "Observed device facts may be refreshed. Live data is separate.",
            "Forget",
            { deleteId = null },
        ) {
            if (deleteId == "all")
                s.memories
                    .filterNot { it.observed }
                    .forEach { send(DemoAction.DeleteMemory(it.id)) }
            else send(DemoAction.DeleteMemory(deleteId!!))
            deleteId = null
        }
}
