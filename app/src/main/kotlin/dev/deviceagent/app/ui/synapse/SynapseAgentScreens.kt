@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.R
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
        SynapseRoute.UNDO -> UndoDemo(s, send, nav)
        else -> Unit
    }
}

@Composable
private fun WelcomeDemo(nav: (SynapseRoute) -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.heightIn(min = 158.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(
                        "Welcome to",
                        style =
                            MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                lineHeight = 32.sp,
                            ),
                    )
                    Text(
                        "Synapse",
                        style =
                            MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                lineHeight = 32.sp,
                            ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Muted(
                    "Turn your ideas into real actions on your Android device using AI. Synapse can inspect, plan, ask for your approval, and handle the rest."
                )
            }
            ReferenceArtwork(R.drawable.synapse_art_welcome_phones, "Synapse phones and Android tools",
                Modifier.weight(1f).aspectRatio(188f / 172f).clip(RoundedCornerShape(12.dp)))
        }
        Row(
            Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(
                    Triple(SynapseIcon.AGENT, "Powerful AI", "Plan and execute tasks for you"),
                    Triple(
                        SynapseIcon.SECURITY,
                        "Safe and controlled",
                        "You approve sensitive actions",
                    ),
                    Triple(
                        SynapseIcon.DEVICE,
                        "Built for Android",
                        "Works with your device and apps",
                    ),
                )
                .forEach { (icon, title, detail) ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        AssetIcon(icon, size = 20.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                title,
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                detail,
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
        }
        Section("") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Get started",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Column(Modifier.width(72.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        "0 / 4",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = { 0f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                    )
                }
            }
            Text(
                "Complete these steps to set up Synapse on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            listOf(
                    Triple(SynapseIcon.PROVIDERS, "Connect an AI provider", SynapseRoute.PROVIDERS),
                    Triple(SynapseIcon.FOLDER, "Grant folder access", SynapseRoute.SANDBOX),
                    Triple(
                        SynapseIcon.SECURITY,
                        "Set up safety preferences",
                        SynapseRoute.SECURITY,
                    ),
                    Triple(SynapseIcon.SETTINGS, "Configure basic settings", SynapseRoute.SETTINGS),
                )
                .forEachIndexed { index, (icon, title, route) ->
                    Surface(
                        onClick = { nav(route) },
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .6f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                            ) {
                                Text(
                                    "${index + 1}",
                                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (index == 0) ProviderArtwork("Gemini", 30) else IconTile(icon, entryColor(index), 30)
                            Column(Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    listOf(
                                        "Use Gemini, OpenAI, Claude, or a custom provider.",
                                        "Allow Synapse to access files using Android SAF.",
                                        "Choose what actions require your approval.",
                                        "Appearance, language, and other preferences.",
                                    )[index],
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("›", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
        }
        FullButton("Continue Setup  →") { nav(SynapseRoute.PROVIDERS) }
        TextButton(
            { nav(SynapseRoute.AGENT) },
            Modifier.align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text("Skip for now", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun entryColor(index: Int): Color =
    listOf(
        Color(0xFF0867F9),
        Color(0xFF00AE9F),
        Color(0xFF00B66B),
        Color(0xFF8645FF),
        Color(0xFFFF8B00),
        Color(0xFF0867F9),
    )[index % 6]

@Composable
private fun IconTile(icon: SynapseIcon, tint: Color, size: Int = 30) {
    Surface(color = tint.copy(alpha = .09f), shape = RoundedCornerShape(10.dp)) {
        Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
            AssetIcon(icon, tint = tint, size = 18.dp)
        }
    }
}

@Composable
private fun AgentDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    DesignPage(
        SynapseRoute.AGENT,
        "Hello, Mohammed",
        "Describe what you want to do with your device.\nI’ll plan, ask for approval, and handle the rest.",
        nav,
    ) {
        Section("") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetIcon(
                    SynapseIcon.AGENT,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = .45f),
                    size = 18.dp,
                )
                BasicTextField(
                    s.draft,
                    { send(DemoAction.Draft(it)) },
                    Modifier.weight(1f).heightIn(min = 32.dp),
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    decorationBox = { field ->
                        if (s.draft.isBlank())
                            Text(
                                "What would you like to do?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        field()
                    },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(
                        SynapseIcon.FILE to "Add files",
                        SynapseIcon.IMAGE to "Add images",
                        SynapseIcon.FOLDER to "Add folder",
                    )
                    .forEach { (icon, label) ->
                        Surface(
                            onClick = { nav(SynapseRoute.ATTACHMENTS) },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                AssetIcon(
                                    icon,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 12.dp,
                                )
                                Text(label, fontSize = 9.sp)
                            }
                        }
                    }
                Spacer(Modifier.weight(1f))
                FilledIconButton(
                    {
                        send(DemoAction.Submit)
                        nav(SynapseRoute.PLAN)
                    },
                    Modifier.size(36.dp),
                    enabled = s.draft.isNotBlank(),
                ) {
                    AssetIcon(
                        SynapseIcon.PLAY,
                        "Preview demo plan",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 20.dp,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                    SynapseIcon.FOLDER to "Organize Downloads",
                    SynapseIcon.PLAY to "Convert media files",
                    SynapseIcon.TERMINAL to "Run Python project",
                    SynapseIcon.DELETE to "Clean storage",
                )
                .forEachIndexed { index, (icon, goal) ->
                    Surface(
                        onClick = { send(DemoAction.Draft(goal)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                            Modifier.padding(6.dp).heightIn(min = 30.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            IconTile(
                                icon,
                                listOf(
                                    entryColor(0),
                                    entryColor(3),
                                    entryColor(1),
                                    Color(0xFFFF4545),
                                )[index],
                                24,
                            )
                            Text(goal, fontSize = 8.sp, lineHeight = 11.sp)
                        }
                    }
                }
        }
        DashboardHeading("Quick actions") { nav(SynapseRoute.TOOLS) }
        listOf(
                Triple(SynapseIcon.FOLDER, "Manage files", SynapseRoute.SANDBOX),
                Triple(SynapseIcon.TERMINAL, "Run a command", SynapseRoute.TERMINAL),
                Triple(SynapseIcon.APPS, "Manage apps", SynapseRoute.APPS),
                Triple(SynapseIcon.PLAY, "Convert media", SynapseRoute.ATTACHMENTS),
                Triple(SynapseIcon.TOOLS, "Run project", SynapseRoute.TOOLS),
                Triple(SynapseIcon.SCHEDULES, "Schedule task", SynapseRoute.SCHEDULE),
            )
            .chunked(3)
            .forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEachIndexed { index, (icon, label, route) ->
                        Surface(
                            onClick = { nav(route) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(11.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Row(
                                Modifier.padding(8.dp).heightIn(min = 38.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                IconTile(
                                    icon,
                                    entryColor(listOf(0, 1, 2, 4, 5, 3)[rowIndex * 3 + index]),
                                )
                                Column {
                                    Text(
                                        label,
                                        fontSize = 9.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        listOf(
                                            "Organize, move, clean",
                                            "Terminal & shell",
                                            "Install, uninstall, update",
                                            "Audio, video, images",
                                            "Python, Node, etc.",
                                            "Automate with schedules",
                                        )[rowIndex * 3 + index],
                                        fontSize = 7.sp,
                                        lineHeight = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        DashboardHeading("Recent tasks") { nav(SynapseRoute.ACTIVITY) }
        Section("") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(
                        Triple(
                            "Organize Downloads folder",
                            "Completed · 12 files moved",
                            "2 hours ago",
                        ),
                        Triple(
                            "Convert videos to MP4",
                            "Processing · 3 of 5 files",
                            "32 minutes ago",
                        ),
                        Triple("Run Python project", "Pending approval", "1 hour ago"),
                    )
                    .forEachIndexed { index, (title, detail, time) ->
                        if (index > 0)
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = .5.dp,
                            )
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    when (index) {
                                        0 -> {
                                            send(DemoAction.SelectTask("SYN-2044"))
                                            nav(SynapseRoute.TASK_RESULT)
                                        }
                                        1 -> {
                                            send(DemoAction.SelectTask("SYN-2048"))
                                            nav(SynapseRoute.MONITOR)
                                        }
                                        else -> nav(SynapseRoute.PLAN)
                                    }
                                }
                                .heightIn(min = 38.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            IconTile(
                                listOf(
                                    SynapseIcon.APPROVAL,
                                    SynapseIcon.ACTIVITY,
                                    SynapseIcon.HISTORY,
                                )[index],
                                listOf(
                                    entryColor(2),
                                    entryColor(0),
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                )[index],
                            )
                            Column(Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                time,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("⋮", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
            }
        }
    }
}

@Composable
private fun DashboardHeading(title: String, onMore: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(
            "See all  ›",
            Modifier.clickable(onClick = onMore).padding(3.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AttachmentsDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var preview by rememberSaveable { mutableStateOf<String?>(null) }
    var picker by rememberSaveable { mutableStateOf(false) }
    val choices = listOf("screenshot_error.png", "error_log.txt", "PythonProject", "Movies/Trips")
    DesignPage(
        SynapseRoute.ATTACHMENTS,
        "Add attachments",
        "Add files, images, folders, or other context to help Synapse understand your task better.",
        nav,
        footer = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    DesignSearch(
                        s.draft,
                        { send(DemoAction.Draft(it)) },
                        "You can also describe your task here…",
                    )
                }
                Primary("Continue  →") { nav(SynapseRoute.PLAN) }
            }
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                    SynapseIcon.FILE to "Add files",
                    SynapseIcon.IMAGE to "Add images",
                    SynapseIcon.FOLDER to "Add folder",
                    SynapseIcon.APPS to "Add app",
                )
                .forEach { (icon, label) ->
                    Surface(
                        onClick = { picker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            Modifier.padding(8.dp).heightIn(min = 70.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconTile(
                                icon,
                                when (icon) {
                                    SynapseIcon.IMAGE -> entryColor(3)
                                    SynapseIcon.APPS -> entryColor(2)
                                    else -> entryColor(0)
                                },
                                28,
                            )
                            Text(label, style = MaterialTheme.typography.labelSmall)
                            Text(
                                when (icon) {
                                    SynapseIcon.FILE -> "Documents, code, logs, etc."
                                    SynapseIcon.IMAGE -> "Photos, screenshots, designs, etc."
                                    SynapseIcon.FOLDER -> "Select a folder using Android SAF"
                                    else -> "From installed apps (APK or info)"
                                },
                                fontSize = 8.sp,
                                lineHeight = 10.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
        }
        OutlinedButton(
            { picker = true },
            Modifier.fillMaxWidth().heightIn(min = 68.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AssetIcon(SynapseIcon.UPLOAD)
                Text("Drag and drop files here", style = MaterialTheme.typography.titleSmall)
                Text(
                    "or tap to browse sample files",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        DesignSection(
            "Attached items (${s.attachments.size})",
            action = "Clear all",
            onAction = { s.attachments.forEach { send(DemoAction.Attachment(it, false)) } },
        ) {
            if (s.attachments.isEmpty()) Muted("No attachments yet. Choose a sample above.")
            s.attachments.forEach { name ->
                DesignRow(
                    if (name.endsWith("png")) SynapseIcon.IMAGE
                    else if (name.endsWith("txt")) SynapseIcon.FILE else SynapseIcon.FOLDER,
                    name,
                    when {
                        name.endsWith("png") -> "PNG · 1.2 MB · Image"
                        name.endsWith("txt") -> "TXT · 24 KB · Document"
                        name == "PythonProject" -> "Folder · 18 files · 12 MB"
                        else -> "Local sample · not uploaded"
                    },
                    leading = if (name.endsWith("png")) { { VideoThumbnail(0, Modifier.width(32.dp).height(24.dp)) } } else null,
                    onClick = { preview = name },
                    trailing = {
                        IconButton({ preview = name }, Modifier.size(28.dp)) {
                            AssetIcon(SynapseIcon.SEARCH, "Preview $name", size = 16.dp)
                        }
                        IconButton(
                            { send(DemoAction.Attachment(name, false)) },
                            Modifier.size(28.dp),
                        ) {
                            AssetIcon(SynapseIcon.DELETE, "Remove $name", size = 16.dp)
                        }
                    },
                )
            }
        }
        DashboardHeading("Suggestions") { picker = true }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(
                    Triple(SynapseIcon.IMAGE, "Take screenshot", "screenshot_error.png"),
                    Triple(SynapseIcon.FILE, "Add logs", "error_log.txt"),
                    Triple(SynapseIcon.FOLDER, "Select Downloads", "Downloads"),
                    Triple(SynapseIcon.APPS, "Add app info", "App info"),
                )
                .forEach { (icon, title, name) ->
                    Surface(
                        onClick = { send(DemoAction.Attachment(name, true)) },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Row(
                            Modifier.padding(4.dp).heightIn(min = 28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            AssetIcon(icon, size = 14.dp)
                            Text(title, fontSize = 8.sp, lineHeight = 10.sp)
                        }
                    }
                }
        }
        DesignNotice(
            "Why add attachments?",
            "Attachments help Synapse understand your task. Sharing follows your privacy settings.",
        )
        TextButton({ nav(SynapseRoute.PRIVACY) }) {
            Text("Privacy settings", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (picker)
        ModalBottomSheet(onDismissRequest = { picker = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("Choose demo context", style = MaterialTheme.typography.titleLarge)
                choices.forEach { name ->
                    SwitchSetting(name, "Sample only", name in s.attachments) {
                        send(DemoAction.Attachment(name, it))
                    }
                }
                FullButton("Use selected context") { picker = false }
            }
        }
    DemoDetailDialog(
        preview,
        { preview = null },
        "Local attachment preview. File content and media are not uploaded by this demo.",
    )
}

@Composable
private fun PlanSummary(s: DemoState, nav: (SynapseRoute) -> Unit) {
    DesignSection(
        "Task summary",
        SynapseIcon.FILE,
        "Edit request",
        { nav(SynapseRoute.AGENT) },
        subtitle = if (s.includeTrash) "Organize Downloads, create archives, and verify outputs." else videoDemoDescription,
    ) {
        ReferenceMetricCards(
            Triple(SynapseIcon.FILE, "${s.planCount} files", "Input items"),
            Triple(SynapseIcon.FOLDER, "1 folder", "Will be created"),
            Triple(SynapseIcon.HISTORY, "~5–10 minutes", "Estimated time"),
            Triple(SynapseIcon.SETTINGS, "6 steps", "Planned actions"),
        )
    }
}

private val planSteps =
    listOf(
        Triple(
            SynapseIcon.FOLDER,
            "Check and create output folder",
            "Create folder “Converted Videos” in Downloads if it doesn’t exist.",
        ),
        Triple(
            SynapseIcon.FILE,
            "Analyze input files",
            "Check file formats and extract video information (duration, size, etc.).",
        ),
        Triple(
            SynapseIcon.PLAY,
            "Convert videos to MP4",
            "Use FFmpeg to convert 3 video files to MP4 (H.264, AAC).",
        ),
        Triple(
            SynapseIcon.FOLDER,
            "Organize files",
            "Move converted files to “Converted Videos” folder.",
        ),
        Triple(
            SynapseIcon.SECURITY,
            "Verify results",
            "Check that all files were converted successfully.",
        ),
        Triple(SynapseIcon.AUDIT, "Generate summary", "Show results and any issues found."),
    )

@Composable
private fun PlannedSteps(approval: Boolean = false) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var detail by rememberSaveable { mutableStateOf<Int?>(null) }
    DesignSection(
        if (approval) "Planned actions (6)" else "Planned steps (6)",
        SynapseIcon.AUDIT,
        if (expanded) "Collapse all" else if (approval) "Expand all" else "View as flow",
        { expanded = !expanded },
    ) {
        planSteps.forEachIndexed { i, (icon, title, description) ->
            Row(
                Modifier.fillMaxWidth()
                    .clickable { detail = if (detail == i) null else i }
                    .heightIn(min = if (approval) 28.dp else 34.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    Text(
                        "${i+1}",
                        Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                AssetIcon(
                    icon,
                    size = 18.dp,
                    tint =
                        if (i == 2) entryColor(3)
                        else if (i == 4) entryColor(2) else MaterialTheme.colorScheme.primary,
                )
                Column(Modifier.weight(1f).padding(vertical = 1.dp)) {
                    Text(
                        title,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        description,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (expanded || detail == i)
                        Text(
                            "Scope: selected input and approved destination only. Results require verification.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                }
                Text(if (approval) "⌄" else "⋮", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlanPermissions(nav: (SynapseRoute) -> Unit, compact: Boolean = false) {
    DesignSection(
        "Permissions required (3)",
        SynapseIcon.PERMISSIONS,
        "Review all",
        { nav(SynapseRoute.PERMISSIONS) },
        subtitle = "Approval is scoped to these actions.",
    ) {
        val rows =
            listOf(
                Triple(SynapseIcon.FOLDER, "File system access", "Read input files"),
                Triple(
                    SynapseIcon.FILE,
                    "Create and modify files",
                    "Write inside the selected folder",
                ),
                Triple(SynapseIcon.TERMINAL, "Run terminal command", "Use FFmpeg"),
            )
        if (compact)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rows.forEach { (icon, title, description) ->
                    Row(
                        Modifier.weight(1f).padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AssetIcon(icon, size = 16.dp)
                        Column {
                            Text(
                                title,
                                fontSize = 8.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                description,
                                fontSize = 7.sp,
                                lineHeight = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Required",
                                fontSize = 7.sp,
                                lineHeight = 9.sp,
                                color = entryColor(2),
                            )
                        }
                    }
                }
            }
        else
            rows.forEach { (icon, title, description) ->
                DesignRow(icon, title, description, "Required", dense = true)
            }
    }
}

@Composable
private fun PlanDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var edit by rememberSaveable { mutableStateOf(false) }
    DesignPage(
        SynapseRoute.PLAN,
        "Plan preview",
        "Review the generated plan. Check the steps, permissions, and estimated time before approving.",
        nav,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ edit = true }, Modifier.weight(1f).heightIn(min = 32.dp)) {
                    Text("Edit request", style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    { nav(SynapseRoute.APPROVAL) },
                    Modifier.weight(1f).heightIn(min = 32.dp),
                    enabled = s.destination.isNotBlank(),
                ) {
                    Text("Approve and start  →")
                }
            }
        },
    ) {
        PlanSummary(s, nav)
        PlannedSteps()
        PlanPermissions(nav, compact = true)
    }
    if (edit)
        DesignDialog(
            onDismissRequest = { edit = false },
            title = { Text("Edit destination & exclusions") },
            text = {
                Column {
                    OutlinedTextField(
                        s.destination,
                        { send(DemoAction.DestinationChanged(it)) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Destination folder") },
                        isError = s.destination.isBlank(),
                        singleLine = true,
                    )
                    SwitchSetting(
                        "Review duplicate trash operations",
                        "Each destructive scope requires consent.",
                        s.includeTrash,
                    ) {
                        send(DemoAction.IncludeTrash(it))
                    }
                }
            },
            confirmButton = {
                TextButton({ edit = false }, enabled = s.destination.isNotBlank()) { Text("Done") }
            },
        )
}

@Composable
private fun ApprovalDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    if (s.includeTrash) {
        ScopedApprovalDemo(s, send, nav)
        return
    }
    DesignPage(
        SynapseRoute.APPROVAL,
        "Approve actions",
        "Review the planned actions, permissions, and potential changes before starting the task.",
        nav,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    { nav(SynapseRoute.PLAN) },
                    Modifier.weight(1f).heightIn(min = 32.dp),
                ) {
                    Text("Cancel")
                }
                Button(
                    {
                        send(DemoAction.Approve(s.planRevision))
                        nav(SynapseRoute.MONITOR)
                    },
                    Modifier.weight(1.4f).heightIn(min = 32.dp),
                    enabled = s.destination.isNotBlank(),
                ) {
                    Text("Approve and start")
                }
            }
        },
    ) {
        PlanSummary(s, nav)
        PlannedSteps(approval = true)
        PlanPermissions(nav)
        DesignNotice(
            "Synapse will only perform the actions listed above.",
            "You can monitor progress in real time and stop at any time. Changes require approval again.",
        )
    }
}

@Composable
private fun ScopedApprovalDemo(
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
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
    var preview by rememberSaveable { mutableStateOf(false) }
    val complete = s.task.status == DemoTaskStatus.VERIFIED
    val step = if (complete) 6 else (s.task.verified + 3).coerceAtMost(5)
    DesignPage(SynapseRoute.MONITOR, "Monitor progress",
        "Synapse is executing the approved plan. You can monitor the progress in real time and stop if needed.", nav,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DesignAction("Stop execution", Modifier.weight(1f), danger = true,
                    enabled = s.task.status in listOf(DemoTaskStatus.RUNNING, DemoTaskStatus.PAUSED)) { cancel = true }
                DesignAction(if (s.task.status == DemoTaskStatus.PAUSED) "Resume" else "Pause", Modifier.weight(1f),
                    enabled = s.task.status in listOf(DemoTaskStatus.RUNNING, DemoTaskStatus.PAUSED)) { send(DemoAction.PauseResume) }
            }
        }) {
        DesignSection("Task in progress", SynapseIcon.PLAY,
            s.task.status.name.lowercase().replaceFirstChar { it.uppercase() }, subtitle = videoDemoDescription) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text("03:24", style = MaterialTheme.typography.titleSmall) }
            LinearProgressIndicator(progress = { step / 6f }, modifier = Modifier.fillMaxWidth().height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Muted("$step of 6 steps completed"); Muted("${kotlin.math.round(step * 100f / 6).toInt()}%")
            }
        }
        ReferenceMetricCards(
            Triple(SynapseIcon.FILE,"${s.task.total} / ${s.task.total}","Input files"),
            Triple(SynapseIcon.FOLDER,"1 / 1","Folder created"),
            Triple(SynapseIcon.SETTINGS,"${(s.task.verified+1).coerceAtMost(s.task.total)} / ${s.task.total}","Actions completed"),
            Triple(SynapseIcon.HISTORY,if (complete) "0 min" else "~2 min","Remaining"))
        DesignSection("Current step", SynapseIcon.PLAY, if (complete) "Step 6 of 6" else "Step 3 of 6",
            subtitle = if (complete) "All outputs verified." else "Converting video ${(s.task.verified+1).coerceAtMost(s.task.total)} of ${s.task.total} to MP4 using FFmpeg…") {
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Create folder","Analyze files","Convert to MP4","Organize files","Verify results","Generate summary").forEachIndexed { i,label ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(color = if (i <= 2 || complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape = CircleShape) {
                            Text(if (i < 2 || complete) "✓" else "${i+1}",Modifier.padding(horizontal=5.dp,vertical=1.dp),fontSize=8.sp,lineHeight=11.sp,color=if(i<=2||complete) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(label,Modifier.padding(top=5.dp),fontSize=6.sp,lineHeight=8.sp,maxLines=1)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1.2f)) {
                DesignSection("Live log", SynapseIcon.AUDIT, "All events", dense = true) {
                    listOf("Starting task execution.","Creating folder: Converted Videos","Folder created successfully","Analyzing input files…","Found 3 video files (MP4, MOV, AVI)","Converting video 1 of 3… (completed)","Converting video 2 of 3… (in progress)","Converting video 3 of 3… (pending)","Organizing files… (pending)","Verifying results… (pending)","Generating summary… (pending)").forEachIndexed { i,text ->
                        Row(Modifier.padding(vertical = 2.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text("09:41:${12+i*2}",fontSize=7.sp,lineHeight=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if(i<6) "✓" else if(i==6) "●" else "○",fontSize=9.sp,lineHeight=10.sp,color=if(i<6) entryColor(2) else MaterialTheme.colorScheme.primary)
                            Text(if (complete && i>=6) text.substringBefore("…")+" · verified" else text,fontSize=7.sp,lineHeight=10.sp,
                                color=if(i==6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DesignSection("Current output",SynapseIcon.FOLDER, dense = true) {
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
                        VideoThumbnail(0,Modifier.width(45.dp).height(32.dp).clickable { preview = true })
                        Column(Modifier.weight(1f)) {
                            Text("video_2_converted.mp4",fontSize=7.sp,lineHeight=10.sp)
                            Text("MP4 · 1280×720 · 24.6 MB",fontSize=7.sp,lineHeight=9.sp)
                            LinearProgressIndicator(progress={if (complete) 1f else .58f},modifier=Modifier.fillMaxWidth().height(4.dp))
                        }
                    }
                }
                DesignSection("Screenshots (latest)",SynapseIcon.IMAGE,"View all",{preview=true}, dense = true) {
                    listOf(R.drawable.synapse_art_execution_capture_1,R.drawable.synapse_art_execution_capture_2,R.drawable.synapse_art_execution_capture_3,R.drawable.synapse_art_execution_capture_4).chunked(2).forEach { row ->
                        Row(horizontalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.padding(vertical=3.dp)) {
                            row.forEach { drawable -> ReferenceArtwork(drawable,"Sample execution capture",Modifier.weight(1f).aspectRatio(1.4f).clickable { preview=true }) }
                        }
                    }
                }
            }
        }
        Secondary("Simulate next verified item",s.task.status==DemoTaskStatus.RUNNING) {send(DemoAction.Advance)}
        Actions {
            Secondary("Checkpoint") {nav(SynapseRoute.CHECKPOINT)}
            Secondary("Review recovery") {nav(SynapseRoute.RECOVERY)}
            Secondary("View result") {nav(SynapseRoute.TASK_RESULT)}
        }
    }
    DemoDetailDialog(if(preview) "Sample execution evidence" else null,{preview=false},"These approved-design thumbnails illustrate the Demo workflow. No device screenshot, video conversion, or file change occurred.")
    if(cancel) ConfirmAction("Cancel this demo task?","Completed changes remain in the audit. Review rollback separately.","Cancel task",{cancel=false}) {send(DemoAction.Cancel);cancel=false}
}

@Composable
private fun RecoveryDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var selected by rememberSaveable { mutableStateOf("Retry with different settings") }
    var alternative by rememberSaveable { mutableStateOf(false) }
    var ask by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    DesignPage(
        SynapseRoute.RECOVERY,
        if (s.recoveryVerified) "Recovery verified"
        else if (s.recoveryAborted) "Recovery stopped" else "Task needs recovery",
        "Synapse encountered an issue. Review the error and choose how to proceed.",
        nav,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DesignAction(
                    "Abort task",
                    Modifier.weight(1f),
                    danger = true,
                    enabled = !s.recoveryVerified && !s.recoveryAborted,
                ) {
                    send(DemoAction.AbortRecovery)
                }
                DesignAction("Ask for help", Modifier.weight(1f)) { ask = true }
                DesignAction(
                    "Approve recovery",
                    Modifier.weight(1.3f),
                    primary = true,
                    enabled = !s.recoveryAborted && !s.recoveryVerified && s.attempts < 3,
                ) {
                    when (selected) {
                        "Try alternative tool" -> alternative = true
                        "Provide a different file" -> ask = true
                        "Skip this file" -> send(DemoAction.RecoverVideo("skip"))
                        else ->
                            if (s.task.id.startsWith("V3-")) send(DemoAction.RecoverVideo("retry"))
                            else
                            if (s.packageName == "FFmpeg" && s.packageStep == 4)
                                send(DemoAction.RetryRecovery)
                            else {
                                send(DemoAction.PackageSelected("FFmpeg"))
                                nav(SynapseRoute.PACKAGE_DETAIL)
                            }
                    }
                }
            }
        },
    ) {
        DesignSection("Task summary", SynapseIcon.FILE, "View plan", { nav(SynapseRoute.PLAN) }, subtitle = videoDemoDescription) {
            ReferenceMetricCards(Triple(SynapseIcon.FILE,"3 files","Input items"),Triple(SynapseIcon.FOLDER,"1 folder","Created"),Triple(SynapseIcon.SETTINGS,"Step 3 of 6","Failed"),Triple(SynapseIcon.HISTORY,"02:14","Elapsed time"))
        }
        DesignNotice("Error occurred", "Failed to convert video_2.mp4 using FFmpeg. The input file appears to be corrupted or in an unsupported format.", Tone.ERROR) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    VideoThumbnail(2,Modifier.width(58.dp).height(44.dp))
                    Column {
                        Text("video_2.mp4",style=MaterialTheme.typography.titleSmall)
                        Text("MP4 · 1920×1080 · 36.8 MB",fontSize=8.sp,lineHeight=11.sp)
                        Text("Duration: 00:05:42",fontSize=8.sp,lineHeight=11.sp)
                        Text("Conversion failed",fontSize=8.sp,lineHeight=11.sp,color=MaterialTheme.colorScheme.error)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Error message (from FFmpeg)",fontSize=8.sp,lineHeight=11.sp)
                    CodeBlock("ffmpeg -i input.mp4 -c:v libx264 output.mp4\nmoov atom not found\nInvalid data found when processing input", dense = true)
                }
            }
        }
        DesignSection("Recovery options", SynapseIcon.RECOVERY, "${s.attempts} / 3 attempts", dense = true) {
            listOf(
                    "Retry with different settings",
                    "Try alternative tool",
                    "Skip this file",
                    "Provide a different file",
                )
                .forEach { option ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { selected = option }
                            .heightIn(min = 29.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 24.dp) {
                            RadioButton(selected == option, { selected = option }, Modifier.size(26.dp))
                        }
                        Column {
                            Text(option, fontSize=10.sp,lineHeight=12.sp)
                            Text(
                                if (option.startsWith("Retry"))
                                    "Try alternative FFmpeg parameters and fallback method."
                                else if (option.startsWith("Try"))
                                    "Use a different conversion method; approval required."
                                else if (option == "Skip this file") "Continue remaining files; report this file as failed." else "Select a replacement file for this step.",
                                fontSize=8.sp,lineHeight=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
        }
        DesignSection(
            "Preview of recovery plan",
            SynapseIcon.RECOVERY,
            subtitle = "These steps will be executed to recover from the error.", dense = true,
        ) {
            listOf(
                    "Analyze the file with alternative settings",
                    "Try repairing the video file if possible",
                    "Convert using verified fallback parameters",
                    "Continue with remaining steps if successful",
                )
                .forEachIndexed { i, t ->
                    Text("${i+1}. $t", fontSize = 8.sp, lineHeight = 11.sp)
                }
        }
        if (s.recoveryNote.isNotBlank()) Notice("Your clarification", s.recoveryNote)
        if (s.recoveryVerified)
            Primary("Return to task") {
                send(DemoAction.SelectTask(if (s.task.id.startsWith("V3-")) videoDemoId else "SYN-2049"))
                nav(SynapseRoute.MONITOR)
            }
    }
    if (alternative)
        ConfirmAction(
            if (s.task.id.startsWith("V3-")) "Use an alternative conversion method?" else "Use AAC instead of MP3?",
            "Review the alternate output method. Existing sources remain unchanged.",
            "Approve alternative",
            { alternative = false },
        ) {
            if (s.task.id.startsWith("V3-")) send(DemoAction.RecoverVideo("alternative")) else send(DemoAction.Alternative)
            alternative = false
        }
    if (ask)
        DesignDialog(
            onDismissRequest = { ask = false },
            title = { Text("What should the agent know?") },
            text = {
                OutlinedTextField(
                    input,
                    { input = it },
                    label = { Text("Preferred format or destination") },
                )
            },
            confirmButton = {
                TextButton(
                    {
                        send(DemoAction.RecoveryInput(input))
                        ask = false
                    },
                    enabled = input.isNotBlank(),
                ) {
                    Text("Add clarification")
                }
            },
            dismissButton = { TextButton({ ask = false }) { Text("Cancel") } },
        )
}

@Composable
private fun CheckpointDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var automatic by rememberSaveable { mutableStateOf(true) }
    var keep by rememberSaveable { mutableStateOf("10 checkpoints") }
    var created by rememberSaveable { mutableIntStateOf(0) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    DesignPage(
        SynapseRoute.CHECKPOINT,
        "Checkpoints",
        "Manage restore points for your tasks and recorded changes. Task checkpoints do not restore your entire device.",
        nav,
    ) {
        Section("") {
            SwitchSetting(
                "Automatic checkpoints",
                "Before important changes in this demo",
                automatic,
            ) {
                automatic = it
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    DesignChoice(
                        "Keep last",
                        listOf("5 checkpoints", "10 checkpoints", "20 checkpoints"),
                        keep,
                    ) {
                        keep = it
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Storage usage", style = MaterialTheme.typography.bodySmall)
                    Text("1.2 GB / 10 GB", style = MaterialTheme.typography.titleSmall)
                    LinearProgressIndicator(
                        progress = { .12f },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                    )
                }
            }
        }
        Section("") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconTile(SynapseIcon.PLUS, entryColor(3), 26)
                Column(Modifier.weight(1f)) {
                    Text("Create a checkpoint now", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Create a sample restore point",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DesignAction("+ Create", primary = true) {
                    created++
                    send(
                        DemoAction.Notice(
                            "Demo task checkpoint created. No device snapshot was taken."
                        )
                    )
                }
            }
        }
        DesignSection("Checkpoints (${8+created})", SynapseIcon.CHECKPOINTS) {
            repeat(created) { i ->
                DesignRow(
                    SynapseIcon.CHECKPOINTS,
                    "Manual checkpoint ${i+1}",
                    "Created in this demo",
                    "Just now",
                    onClick = { detail = "Manual checkpoint ${i+1}" },
                )
            }
            listOf(
                    "Before converting videos to MP4",
                    "Before organizing Downloads folder",
                    "Before running Python project",
                    "Before installing Termux",
                    "Manual checkpoint",
                    "System checkpoint",
                    "Before device configuration",
                    "Before cleaning storage",
                )
                .forEachIndexed { i, t ->
                    DesignRow(
                        listOf(
                            SynapseIcon.PLAY,
                            SynapseIcon.FOLDER,
                            SynapseIcon.TERMINAL,
                            SynapseIcon.APPS,
                            SynapseIcon.SECURITY,
                            SynapseIcon.SETTINGS,
                            SynapseIcon.PRIVACY,
                            SynapseIcon.IMAGE,
                        )[i],
                        t,
                        "Task snapshot · ${320-i*20} MB",
                        onClick = { detail = t },
                        trailing = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (i < 2) "Today, ${if(i == 0) "09:40" else "08:15"}"
                                    else "Yesterday",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${320-i*20} MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("⋮", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                    )
                }
        }
    }
    if (detail != null)
        DesignDialog(
            onDismissRequest = { detail = null },
            title = { Text(detail!!) },
            text = {
                Text(
                    "Review recorded outputs and conflicts before restoring. This sample checkpoint contains task state only."
                )
            },
            confirmButton = {
                TextButton({
                    detail = null
                    nav(SynapseRoute.UNDO)
                }) {
                    Text("Review restore")
                }
            },
            dismissButton = {
                TextButton({
                    detail = null
                    nav(SynapseRoute.MONITOR)
                }) {
                    Text("Open task")
                }
            },
        )
}

@Composable
private fun ActivityDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    DesignPage(
        SynapseRoute.ACTIVITY,
        "History",
        "View and search completed, failed, and cancelled tasks. See details, results, and restore from checkpoints.",
        nav,
    ) {
        DesignSearch(query, { query = it }, "Search tasks, actions, or results…")
        DesignSegments(listOf("All", "Completed", "Failed", "Cancelled"), filter) { filter = it }
        val filtered =
            s.tasks.filter {
                it.title.contains(query, true) &&
                    when (filter) {
                        "Completed" -> it.status == DemoTaskStatus.VERIFIED
                        "Failed" -> it.status == DemoTaskStatus.FAILED
                        "Cancelled" -> it.status == DemoTaskStatus.CANCELLED
                        else -> true
                    }
            }
        if (filtered.isEmpty()) Notice("No matching tasks", "Try another name or status filter.")
        listOf("Today", "Yesterday").forEachIndexed { index, label ->
            val group = if (index == 0) filtered.take(4) else filtered.drop(4)
            if (group.isNotEmpty())
                DesignSection(label) {
                    group.forEach { task ->
                        Box(Modifier.padding(vertical = 6.dp)) {
                            DesignRow(
                                if (task.status == DemoTaskStatus.VERIFIED) SynapseIcon.APPROVAL
                                else if (task.status == DemoTaskStatus.FAILED) SynapseIcon.WARNING
                                else SynapseIcon.PLAY,
                                task.title,
                                "${task.verified} of ${task.total} verified · ${task.id}",
                                when (task.status) {
                                    DemoTaskStatus.VERIFIED -> "Completed"
                                    DemoTaskStatus.RUNNING -> "Running"
                                    DemoTaskStatus.WAITING -> "Pending"
                                    DemoTaskStatus.FAILED -> "Failed"
                                    DemoTaskStatus.CANCELLED -> "Cancelled"
                                    else -> "Planned"
                                },
                                onClick = {
                                    send(DemoAction.SelectTask(task.id))
                                    nav(SynapseRoute.AUDIT)
                                },
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
        }
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
    DesignPage(
        SynapseRoute.AUDIT,
        s.task.title,
        "${s.task.verified} of ${s.task.total} items verified · ${s.task.status.name.lowercase()}",
        nav,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DesignAction("Run again", Modifier.weight(1f)) { nav(SynapseRoute.PLAN) }
                DesignAction("Undo changes", Modifier.weight(1f)) { nav(SynapseRoute.UNDO) }
                DesignAction("View checkpoint", Modifier.weight(1.3f), primary = true) {
                    nav(SynapseRoute.CHECKPOINT)
                }
            }
        },
    ) {
        DesignSegments(listOf("Summary", "Changes", "Timeline", "Tools & Logs"), tab) { tab = it }
        when (tab) {
            "Summary" -> {
                DesignSection("Task summary", SynapseIcon.FILE) {
                    Text(s.task.title, style = MaterialTheme.typography.bodySmall)
                    InlineStats(
                        "Processed" to "${s.task.verified}",
                        "Skipped" to "${s.task.skipped}",
                        "Blocked" to "${s.task.blocked}",
                        "Task" to s.task.id,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1.2f)) {
                        DesignSection("Output location", SynapseIcon.FOLDER) {
                            Text(evidence.output, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        DesignSection("Task status", SynapseIcon.SECURITY) {
                            Text(
                                if (final) "Completed" else s.task.status.name.lowercase(),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            SmallValue("Verified", "${s.task.verified}")
                            SmallValue("Remaining", "${s.task.total - s.task.settled}")
                        }
                    }
                }
                DesignSection(
                    "Verified outputs",
                    SynapseIcon.FILE,
                    "Open folder",
                    { nav(SynapseRoute.SANDBOX) },
                ) {
                    if (hasEvidence)
                        repeat(s.task.verified.coerceAtMost(3)) { i ->
                            Box(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                DesignRow(
                                    SynapseIcon.FILE,
                                    "Output ${i+1}",
                                    evidence.check,
                                    "Verified",
                                )
                            }
                        }
                    else Muted("No verified outputs yet.")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        DesignSection("Changes made", SynapseIcon.FILE) {
                            SmallValue("Verified outcomes", "${s.task.verified}")
                            SmallValue("Skipped", "${s.task.skipped}")
                            SmallValue("Blocked", "${s.task.blocked}")
                            TextButton({ tab = "Changes" }) {
                                Text(
                                    "Review all changes",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        DesignSection("Verification result", SynapseIcon.SECURITY) {
                            VerifiedRow(
                                "Completed item checks",
                                if (hasEvidence) evidence.check
                                else "No completed output has been verified yet",
                                hasEvidence,
                            )
                            VerifiedRow(
                                "Whole task complete",
                                if (final) "All required outcomes accounted for"
                                else "Remaining, failed or blocked items need review",
                                final,
                            )
                        }
                    }
                }
            }
            "Changes" ->
                DesignSection("Changes made", SynapseIcon.FILE) {
                    DesignRow(SynapseIcon.FILE, "Source", evidence.source)
                    DesignRow(SynapseIcon.FOLDER, "Destination", evidence.output)
                    Muted(
                        "${s.task.verified} verified outcomes; remaining work is not reported as complete."
                    )
                    Secondary("Review undo") { nav(SynapseRoute.UNDO) }
                }
            "Timeline" ->
                DesignSection("Task timeline", SynapseIcon.ACTIVITY) {
                    demoEvents.forEach { (time, event) ->
                        DesignRow(SynapseIcon.ACTIVITY, time, event)
                    }
                }
            else ->
                DesignSection("Tools & Logs", SynapseIcon.TERMINAL) {
                    CodeBlock(
                        "tool: ${evidence.tool}\ninput: ${evidence.source}\noutput: ${evidence.output}\npolicy: approved scope only\nverified_outcomes: ${s.task.verified}\nfinal_state: ${s.task.status.name}"
                    )
                    Muted(evidence.recovery)
                }
        }
    }
}

@Composable
private fun UndoDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable {
        mutableStateOf(listOf("Moved files", "Trash entries", "Created archives"))
    }
    var preview by rememberSaveable { mutableStateOf(false) }
    DesignPage(
        SynapseRoute.UNDO,
        "Undo / Rollback",
        "Revert recorded task changes. Select what to undo and review the impact before proceeding.",
        nav,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DesignAction("Cancel", Modifier.weight(.8f)) { nav(SynapseRoute.AUDIT) }
                DesignAction("Preview changes", Modifier.weight(1f)) { preview = true }
                DesignAction(
                    "Restore selected changes",
                    Modifier.weight(1.4f),
                    danger = true,
                    enabled = !s.rollbackDone && selected.isNotEmpty(),
                ) {
                    confirm = true
                }
            }
        },
    ) {
        DesignSection("Organize Downloads", SynapseIcon.PLAY) {
            Muted("Fixed rollback scenario · completed demo transaction")
        }
        if (s.rollbackDone)
            DesignNotice(
                "Demo restore verified",
                "Selected changes were reversed in the simulation. Your current ticket.png was preserved.",
                Tone.SUCCESS,
            )
        else
            DesignNotice(
                "This will undo changes made by the task",
                "Only recorded changes can be restored. Newly created files are removed only if unchanged.",
                Tone.ERROR,
            )
        DesignSection(
            "Changes to undo (${selected.size})",
            SynapseIcon.UNDO,
            if (selected.isEmpty()) "Select all" else "Deselect all",
            {
                if (!s.rollbackDone)
                    selected =
                        if (selected.isEmpty())
                            listOf("Moved files", "Trash entries", "Created archives")
                        else emptyList()
            },
        ) {
            listOf(
                    "Moved files" to "604 original locations recorded",
                    "Trash entries" to "27 retained copies available",
                    "Created archives" to "14 files · remove only if unchanged",
                )
                .forEach { (title, detail) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(enabled = !s.rollbackDone) {
                                selected =
                                    if (title in selected) selected - title else selected + title
                            }
                            .heightIn(min = 34.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            title in selected,
                            { checked ->
                                selected = if (checked) selected + title else selected - title
                            },
                            enabled = !s.rollbackDone,
                            modifier = Modifier.size(30.dp),
                        )
                        Column {
                            Text(title, style = MaterialTheme.typography.titleSmall)
                            Muted(detail)
                        }
                    }
                }
        }
        DesignSection(
            "Files to be restored",
            SynapseIcon.RECOVERY,
            "Preview files",
            { preview = true },
        ) {
            DesignRow(SynapseIcon.FILE, "Original locations", "Paths are recorded per operation")
            DesignRow(SynapseIcon.SECURITY, "Conflict policy", s.restoreConflictChoice)
        }
        DesignNotice(
            "One conflict needs review",
            "Downloads/Images/ticket.png changed after this task.",
            Tone.WARNING,
        )
        DesignChoice(
            "Resolve conflict",
            listOf("Keep current", "Restore under new name"),
            s.restoreConflictChoice,
        ) {
            send(DemoAction.ConflictChoice(it))
        }
    }
    DemoDetailDialog(
        if (preview) "Restore preview" else null,
        { preview = false },
        "${selected.joinToString()} will be reviewed. Permanent deletion and unsupported system changes cannot be restored.",
    )
    if (confirm)
        ConfirmAction(
            "Apply this demo rollback?",
            "Selected: ${selected.joinToString()}. Conflict policy: ${s.restoreConflictChoice}. This is a simulated transaction.",
            "Restore",
            { confirm = false },
        ) {
            send(DemoAction.Rollback)
            confirm = false
        }
}
