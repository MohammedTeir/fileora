@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp

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
        DesignSection(
            "Privacy Overview",
            SynapseIcon.PRIVACY,
            subtitle = "Manage how data is processed, stored, and shared.",
            dense = true,
        ) {
            DesignMetrics(
                Triple(SynapseIcon.DEVICE, "Local processing", "Enabled"),
                Triple(SynapseIcon.UPLOAD, "Cloud requests", "Controlled"),
                Triple(SynapseIcon.MEMORY, "Retention", "Minimal"),
                Triple(SynapseIcon.PRIVACY, "Telemetry", "Disabled"),
            )
        }
        DesignSection(
            "Data Sharing",
            SynapseIcon.UPLOAD,
            subtitle = "Control what information is sent to AI providers.",
            dense = true,
        ) {
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
                    DesignSwitch(title, detail, s.flag(key)) { send(DemoAction.Toggle(key, it)) }
                }
        }
        DesignSection(
            "Data Retention",
            SynapseIcon.MEMORY,
            subtitle = "Configure how long your data is stored.",
            dense = true,
        ) {
            DesignPreference(
                "Conversation history",
                listOf("Keep for 7 days", "Keep for 30 days", "Delete after task"),
                history,
            ) {
                history = it
            }
            DesignPreference(
                "Downloaded files",
                listOf("Keep for 7 days", "Keep for 30 days", "Delete after task"),
                downloads,
            ) {
                downloads = it
            }
            DesignPreference(
                "Command history",
                listOf("Keep for 7 days", "Keep for 14 days", "Delete after task"),
                command,
            ) {
                command = it
            }
            Muted("Demo retention preferences do not delete live data.")
        }
        DesignSection(
            "Privacy Controls",
            SynapseIcon.SETTINGS,
            subtitle = "Additional privacy and data control options.",
            dense = true,
        ) {
            DesignSwitch(
                "Use local models when available",
                "Demo preference; approved scope still applies.",
                !s.flag("disableLocalPreference"),
            ) {
                send(DemoAction.Toggle("disableLocalPreference", !it))
            }
            DesignSwitch(
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
        DesignSection("Usage Overview", SynapseIcon.USAGE, dense = true) {
            DesignMetrics(
                Triple(SynapseIcon.FILE, "API requests", "1,250 / 10,000"),
                Triple(SynapseIcon.UPLOAD, "Input tokens", "2.4M"),
                Triple(SynapseIcon.DOWNLOAD, "Output tokens", "0.8M"),
                Triple(SynapseIcon.HISTORY, "Total time", "12.5 hours"),
            )
            LinearProgressIndicator(
                progress = { .125f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
        }
        Section("", dense = true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssetIcon(SynapseIcon.USAGE, size = 16.dp)
                Text(
                    "Usage by Provider",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                Box(Modifier.width(185.dp)) {
                    DesignSegments(listOf("Requests", "Input tokens", "Output tokens"), metric) {
                        metric = it
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
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
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                )
                                Text(
                                    "${values[i].toInt()} (${String.format(java.util.Locale.ROOT, "%.1f", values[i] / 12.5f)}%)",
                                    style =
                                        MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                    color = colors[i],
                                )
                            }
                            LinearProgressIndicator(
                                progress = { values[i] / 1250f },
                                modifier =
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp).height(3.dp),
                                color = colors[i],
                            )
                        }
                }
            }
        }
        val gridColor = MaterialTheme.colorScheme.outlineVariant
        Section("", dense = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssetIcon(SynapseIcon.ACTIVITY, size = 16.dp)
                Text(
                    "Usage History",
                    Modifier.weight(1f).padding(start = 7.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
                Box(Modifier.width(105.dp)) {
                    DesignPreference("", listOf("Last 7 days", "Last 30 days"), period) {
                        period = it
                    }
                }
            }
            Canvas(
                Modifier.fillMaxWidth().height(76.dp).semantics {
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
        DesignSection("Recent Requests", SynapseIcon.HISTORY, dense = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Provider", "Model", "Tokens", "Time", "Status").forEach {
                    Text(
                        it,
                        Modifier.weight(if (it == "Model") 1.6f else 1f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            repeat(4) { i ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { detail = "Request ${i + 1}" }
                        .heightIn(min = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        listOf("Gemini", "OpenAI", "Claude", "Gemini")[i],
                        Modifier.weight(1f),
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                    )
                    Text(
                        listOf(
                            "gemini-1.5-pro",
                            "gpt-4o-mini",
                            "claude-3-5-sonnet",
                            "gemini-1.5-flash",
                        )[i],
                        Modifier.weight(1.6f),
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                    )
                    Text("12.4K", Modifier.weight(1f), fontSize = 8.sp, lineHeight = 11.sp)
                    Text("8.2s", Modifier.weight(1f), fontSize = 8.sp, lineHeight = 11.sp)
                    Text(
                        "● Success",
                        Modifier.weight(1f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp,
                        color = Color(0xFF008C49),
                    )
                }
            }
        }
        DesignSection("Task budgets", SynapseIcon.SECURITY, dense = true) {
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
        DesignSection("Security Overview", SynapseIcon.SECURITY, dense = true) {
            DesignMetrics(
                Triple(SynapseIcon.SECURITY, "Security status", "Protected"),
                Triple(SynapseIcon.PERMISSIONS, "Execution mode", s.mode),
                Triple(
                    SynapseIcon.ROOT,
                    "Root access",
                    if (s.rootEnabled) "Demo enabled" else "Disabled",
                ),
                Triple(SynapseIcon.PACKAGES, "Sandbox", "Enabled"),
            )
        }
        DesignSection("Execution Permissions", SynapseIcon.PERMISSIONS, dense = true) {
            DesignPreference("Operating mode", listOf("Normal", "Autonomous", "Manual"), s.mode) {
                send(DemoAction.Mode(it))
            }
            DesignDetailRow(
                SynapseIcon.TERMINAL,
                "Run terminal commands",
                "Scoped execution; risky commands require approval",
                "Ask",
            )
            DesignDetailRow(
                SynapseIcon.FILE,
                "Access files and directories",
                "Only user-granted folders",
                "Ask",
            )
            DesignDetailRow(
                SynapseIcon.PACKAGES,
                "Install and manage packages",
                "Review exact package and source",
                "Ask",
            )
            DesignDetailRow(
                SynapseIcon.ROOT,
                "Change system settings",
                "Per-operation privileged approval",
                "Restricted",
            )
        }
        DesignSection("Risk Levels", SynapseIcon.WARNING, dense = true) {
            DesignDetailRow(
                SynapseIcon.APPROVAL,
                "Low risk",
                "Safe scoped reads and copies",
                "Policy checked",
            )
            DesignDetailRow(
                SynapseIcon.WARNING,
                "Medium risk",
                "Review moves, overwrites, or installs",
                "Ask",
            )
            DesignDetailRow(
                SynapseIcon.SECURITY,
                "High risk",
                "Delete and privileged operations always ask",
                "Always ask",
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                DesignSection("Root Access", SynapseIcon.ROOT, dense = true) {
                    DesignPreference(
                        "Detection",
                        listOf("Detected", "Unavailable", "Denied"),
                        s.rootStatus,
                    ) {
                        send(DemoAction.RootStatus(it))
                    }
                    DesignSwitch(
                        "Enable demo root backend",
                        "Does not approve privileged commands.",
                        s.rootEnabled,
                        s.rootStatus == "Detected",
                    ) {
                        if (it) rootApproval = true else send(DemoAction.RootEnabled(false))
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                DesignSection("Sandbox & Isolation", SynapseIcon.PACKAGES, dense = true) {
                    DesignDetailRow(
                        SynapseIcon.SECURITY,
                        "Sandbox enforced",
                        "Targets rechecked before execution",
                    )
                    DesignDetailRow(
                        SynapseIcon.APPROVAL,
                        "No silent escalation",
                        "Failure never grants root",
                    )
                }
            }
        }
        DesignSection("Security Events", SynapseIcon.AUDIT, dense = true) {
            Row(Modifier.fillMaxWidth()) {
                listOf("Time", "Action", "Status", "Scope").forEach {
                    Text(
                        it,
                        Modifier.weight(1f),
                        fontSize = 8.sp,
                        lineHeight = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            listOf(
                    Triple("Access file", "Allowed", "Granted folder"),
                    Triple("Install package", "Blocked", "Not approved"),
                    Triple("Delete file", "Blocked", "No consent"),
                )
                .forEachIndexed { i, (action, result, scope) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Today 09:${32 - i * 2}",
                            Modifier.weight(1f),
                            fontSize = 8.sp,
                            lineHeight = 11.sp,
                        )
                        Text(action, Modifier.weight(1f), fontSize = 8.sp, lineHeight = 11.sp)
                        Text(
                            "● $result",
                            Modifier.weight(1f),
                            fontSize = 8.sp,
                            lineHeight = 11.sp,
                            color =
                                if (result == "Allowed") Color(0xFF008C49)
                                else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            scope,
                            Modifier.weight(1f),
                            fontSize = 8.sp,
                            lineHeight = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
        footer = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DesignAction("Add preference", Modifier.weight(1f), primary = true) {
                        editId = "preference-${java.util.UUID.randomUUID()}"
                        value = ""
                        scope = "All tasks"
                    }
                    DesignAction("Clear preferences", Modifier.weight(1f), danger = true) {
                        deleteId = "all"
                    }
                }
                Text(
                    "Preferences never override safety policy. Observed facts are rechecked.",
                    style =
                        MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
                )
            }
        },
    ) {
        DesignSection("Memory Overview", SynapseIcon.MEMORY, dense = true) {
            DesignMetrics(
                Triple(SynapseIcon.FILE, "Memory items", "${s.memories.size}"),
                Triple(SynapseIcon.MEMORY, "Storage used", "48 MB"),
                Triple(SynapseIcon.HISTORY, "Retention", "30 days"),
                Triple(SynapseIcon.SECURITY, "Memory", "Enabled"),
            )
        }
        DesignSection("Memory Controls", SynapseIcon.SETTINGS, dense = true) {
            listOf(
                    "Conversation context",
                    "Important information",
                    "Device information",
                    "Commands and tasks",
                    "Personalization",
                    "Automatic summarization",
                )
                .forEach { title ->
                    DesignSwitch(title, "", !s.flag("memoryOff$title")) {
                        send(DemoAction.Toggle("memoryOff$title", !it))
                    }
                }
        }
        DesignSection("Memory Storage", SynapseIcon.MEMORY, dense = true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LinearProgressIndicator(
                    progress = { .24f },
                    modifier = Modifier.weight(1f).height(4.dp),
                )
                Text("48 MB / 200 MB", style = MaterialTheme.typography.bodySmall)
            }
            DesignMetrics(
                Triple(SynapseIcon.AGENT, "Conversations", "18 MB"),
                Triple(SynapseIcon.FILE, "Files", "12 MB"),
                Triple(SynapseIcon.DEVICE, "Device info", "8 MB"),
                Triple(SynapseIcon.SETTINGS, "Preferences", "6 MB"),
            )
        }
        Section("", dense = true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AssetIcon(SynapseIcon.HISTORY, size = 14.dp)
                Text(
                    "Stored Memory",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                Box(Modifier.width(130.dp)) {
                    DesignSearch(query, { query = it }, "Search memory…")
                }
                Box(Modifier.width(85.dp)) {
                    DesignPreference("", listOf("All", "Observed", "Preferences"), filter) {
                        filter = it
                    }
                }
            }
            s.memories
                .filter {
                    it.text.contains(query, true) &&
                        (filter == "All" || it.observed == (filter == "Observed"))
                }
                .forEach { memory ->
                    DesignDetailRow(
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
    }
    if (editId != null)
        DesignDialog(
            onDismissRequest = { editId = null },
            title = { Text("Edit preference") },
            text = {
                Column {
                    OutlinedTextField(value, { value = it }, label = { Text("Preference") })
                    DesignPreference(
                        "Scope",
                        listOf("All tasks", "Media tasks", "This task"),
                        scope,
                    ) {
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
