@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.R

@Composable
internal fun DemoDeviceRoute(
    route: SynapseRoute,
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    when (route) {
        SynapseRoute.DEVICE -> DeviceOverviewDemo(s, send, nav)
        SynapseRoute.SANDBOX -> SandboxDemo(s, send, nav)
        SynapseRoute.PACKAGES -> PackagesDemo(s, send, nav)
        SynapseRoute.PACKAGE_DETAIL -> PackageDetailDemo(s, send, nav)
        SynapseRoute.TERMINAL -> TerminalDemo(s, send, nav)
        SynapseRoute.APPS -> AppsDemo(send, nav)
        SynapseRoute.TOOLS -> ToolsDemo(nav)
        SynapseRoute.BROWSE,
        SynapseRoute.SEARCH ->
            ScreenPage(
                "SAMPLE FILE CONTEXT",
                route.title,
                "Switch to Live to browse the actual folders you have granted.",
            ) {
                listOf(
                        "Movies/Trips/Lisbon_0327.mp4" to "Video · 142 MiB",
                        "Downloads/ticket-copy.pdf" to "Document · 846 KiB",
                        "Music/Converted/Lesson_01.mp3" to "Audio · 8.2 MiB",
                    )
                    .forEach { (name, detail) ->
                        LinkRow(name, detail, Icons.AutoMirrored.Filled.InsertDriveFile) {
                            send(DemoAction.Attachment(name, true))
                            nav(SynapseRoute.ATTACHMENTS)
                        }
                    }
            }
        else -> Unit
    }
}

@Composable
private fun DeviceOverviewDemo(
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    DesignPage(
        SynapseRoute.DEVICE,
        "Device",
        "Explore your device, manage files, apps, and available tools.",
        nav,
    ) {
        DesignSection(
            "Device information",
            SynapseIcon.DEVICE,
            "Refresh",
            { send(DemoAction.Notice("Demo observations refreshed.")) },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssetIcon(SynapseIcon.DEVICE, description = "Sample Android device", size = 66.dp)
                Column(Modifier.weight(1f)) {
                    Text("Samsung Galaxy A04s", style = MaterialTheme.typography.titleSmall)
                    Muted("Android 13 (One UI Core 5.1)")
                    SmallValue("Model", "SM-A047F")
                    SmallValue("Android version", "13")
                    SmallValue("Security patch", "2023-10-01")
                    SmallValue("Root status", s.rootStatus)
                    SmallValue("Storage", "64 GB (28.4 GB free)")
                    LinearProgressIndicator(progress = { .55f }, modifier = Modifier.fillMaxWidth())
                    SmallValue("Memory (RAM)", "4 GB (1.8 GB free)")
                    LinearProgressIndicator(
                        progress = { .55f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFF9A45FF),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                DesignSection("Battery", SynapseIcon.DEVICE) {
                    Text("100%", style = MaterialTheme.typography.titleLarge)
                    Muted("Charging")
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF00B84A),
                    )
                    SmallValue("Temperature", "34°C")
                    SmallValue("Health", "Good")
                }
            }
            Column(Modifier.weight(1f)) {
                DesignSection("Storage", SynapseIcon.MEMORY) {
                    Muted("64 GB total")
                    StorageUsageBar()
                    SmallValue("Apps", "18.2 GB")
                    SmallValue("Media", "12.6 GB")
                    SmallValue("Other", "4.8 GB")
                    SmallValue("Free", "28.4 GB")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                DesignSection("Network", SynapseIcon.NETWORK) {
                    SmallValue("Wi-Fi", "Connected")
                    SmallValue("IP address", "192.168.1.105")
                    SmallValue("Mobile data", "Off")
                }
            }
            Column(Modifier.weight(1f)) {
                DesignSection("System", SynapseIcon.SETTINGS) {
                    SmallValue("CPU", "Octa-core 2.0 GHz")
                    SmallValue("Architecture", "arm64")
                    SmallValue("Uptime", "3 days, 4 hours")
                }
            }
        }
        DesignSection("Quick tools", SynapseIcon.TOOLS, "See all", { nav(SynapseRoute.TOOLS) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                        "Open files" to SynapseRoute.SANDBOX,
                        "Manage apps" to SynapseRoute.APPS,
                        "Run terminal" to SynapseRoute.TERMINAL,
                        "Packages" to SynapseRoute.PACKAGES,
                    )
                    .forEach { (label, target) ->
                        DesignAction(label, Modifier.weight(1f)) { nav(target) }
                    }
            }
        }
    }
}

@Composable
private fun SandboxDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var location by rememberSaveable { mutableStateOf("Internal storage") }
    var items by rememberSaveable {
        mutableStateOf(
            listOf(
                "Alarms",
                "Android",
                "DCIM",
                "Documents",
                "Download",
                "Movies",
                "Music",
                "Notifications",
                "Pictures",
                "Podcasts",
                "Recordings",
                "Ringtones",
                "device_info.txt",
                "log.txt",
                "screenshot.png",
                "video.mp4",
                "archive.zip",
            )
        )
    }
    var selected by rememberSaveable { mutableStateOf(listOf("Download")) }
    var query by rememberSaveable { mutableStateOf("") }
    var action by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    DesignPage(
        SynapseRoute.SANDBOX,
        "Files",
        "Browse and manage your device files and folders. Sample storage only.",
        nav,
        footer = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${selected.size} selected",
                    Modifier.weight(.8f),
                    style = MaterialTheme.typography.bodySmall,
                )
                listOf("Copy", "Move", "Rename", "Delete").forEach { op ->
                    DesignAction(
                        op,
                        Modifier.weight(1f),
                        danger = op == "Delete",
                        enabled = selected.isNotEmpty() && (op != "Rename" || selected.size == 1),
                    ) {
                        name = selected.firstOrNull().orEmpty()
                        action = op
                    }
                }
            }
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.width(102.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Internal storage", style = MaterialTheme.typography.titleSmall)
                StorageUsageBar()
                Text("28.4 GB free", style = MaterialTheme.typography.bodySmall)
                listOf(
                        Triple("Apps", "18.2 GB", Color(0xFF0867F9)),
                        Triple("Media", "12.6 GB", Color(0xFF8855E8)),
                        Triple("Other", "4.8 GB", Color(0xFFF4AF35)),
                        Triple("Free", "28.4 GB", MaterialTheme.colorScheme.outlineVariant),
                    )
                    .forEach { (label, value, color) ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(Modifier.size(4.dp).background(color, RoundedCornerShape(50)))
                            Text(
                                label,
                                Modifier.weight(1f),
                                fontSize = 7.sp,
                                lineHeight = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                value,
                                fontSize = 7.sp,
                                lineHeight = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    listOf(
                            "Internal storage",
                            "SD card",
                            "Downloads",
                            "Pictures",
                            "Videos",
                            "Audio",
                            "Documents",
                            "Apps",
                            "System (Root)",
                            "Recent files",
                            "Large files",
                            "Screenshots",
                            "Synapse workspace",
                            "Trash",
                        )
                        .forEach { title ->
                            if (title == "Recent files") {
                                HorizontalDivider(
                                    Modifier.padding(vertical = 3.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = .5.dp,
                                )
                                Text("Quick access", fontSize = 8.sp, lineHeight = 11.sp)
                            }
                            Surface(
                                onClick = {
                                    location = title
                                    selected = emptyList()
                                },
                                color =
                                    if (location == title)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Row(
                                    Modifier.heightIn(min = 24.dp).padding(3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    AssetIcon(
                                        when (title) {
                                            "Internal storage",
                                            "SD card" -> SynapseIcon.DEVICE
                                            "Downloads" -> SynapseIcon.DOWNLOAD
                                            "Pictures",
                                            "Screenshots" -> SynapseIcon.IMAGE
                                            "Videos" -> SynapseIcon.PLAY
                                            "Audio" -> SynapseIcon.PAUSE
                                            "Documents",
                                            "Large files" -> SynapseIcon.FILE
                                            "Apps" -> SynapseIcon.APPS
                                            "System (Root)" -> SynapseIcon.ROOT
                                            "Recent files" -> SynapseIcon.HISTORY
                                            "Trash" -> SynapseIcon.DELETE
                                            else -> SynapseIcon.FOLDER
                                        },
                                        size = 12.dp,
                                    )
                                    Text(title, fontSize = 8.sp, lineHeight = 11.sp)
                                }
                            }
                        }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(location, style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) { DesignSearch(query, { query = it }, "Find files…") }
                    TextButton(
                        {
                            name = ""
                            action = "New folder"
                        },
                        contentPadding = PaddingValues(3.dp),
                    ) {
                        Text("+ New folder", fontSize = 8.sp, lineHeight = 11.sp)
                    }
                }
                Row {
                    Text("Name", Modifier.weight(1f), fontSize = 8.sp, lineHeight = 12.sp)
                    Text("Type", Modifier.width(26.dp), fontSize = 8.sp, lineHeight = 12.sp)
                    Text("Size", Modifier.width(35.dp), fontSize = 8.sp, lineHeight = 12.sp)
                    Text("Modified", Modifier.width(42.dp), fontSize = 8.sp, lineHeight = 12.sp)
                }
                val visible = items.filter { it.contains(query, true) }
                if (visible.isEmpty()) Muted("No matching files")
                visible.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth()
                            .toggleable(item in selected, role = Role.Checkbox) { checked ->
                                selected = if (checked) selected + item else selected - item
                            }
                            .background(
                                if (item in selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                RoundedCornerShape(4.dp),
                            )
                            .heightIn(min = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            Modifier.size(11.dp)
                                .border(
                                    .7.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(2.dp),
                                )
                                .background(
                                    if (item in selected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    RoundedCornerShape(2.dp),
                                )
                        ) {
                            if (item in selected)
                                AssetIcon(
                                    SynapseIcon.APPROVAL,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    size = 11.dp,
                                )
                        }
                        AssetIcon(
                            if (item.contains('.')) SynapseIcon.FILE else SynapseIcon.FOLDER,
                            size = 12.dp,
                        )
                        Text(
                            item,
                            Modifier.weight(1f),
                            fontSize = 8.sp,
                            lineHeight = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Text(
                            if (item.contains('.')) item.substringAfterLast('.').uppercase()
                            else "Folder",
                            Modifier.width(26.dp),
                            fontSize = 7.sp,
                            lineHeight = 10.sp,
                        )
                        Text(
                            when (item) {
                                "device_info.txt" -> "12 KB"
                                "log.txt" -> "320 KB"
                                "screenshot.png" -> "2.4 MB"
                                "video.mp4" -> "24.1 MB"
                                "archive.zip" -> "5.8 MB"
                                else -> "–"
                            },
                            Modifier.width(35.dp),
                            fontSize = 7.sp,
                            lineHeight = 10.sp,
                        )
                        Text(
                            "Sep 20",
                            Modifier.width(42.dp),
                            fontSize = 7.sp,
                            lineHeight = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Disclosure("Granted folders", "Android SAF access remains separately scoped") {
            s.folders.forEach { folder ->
                DesignRow(SynapseIcon.FOLDER, folder, "Sample folder grant")
            }
            Secondary("Permissions") { nav(SynapseRoute.PERMISSIONS) }
        }
    }
    if (action != null)
        DesignDialog(
            onDismissRequest = { action = null },
            title = { Text("${action} · Demo") },
            text = {
                Column {
                    Text(
                        "Selected: ${selected.joinToString().ifBlank{"none"}}. No real files are changed."
                    )
                    if (action in listOf("New folder", "Rename"))
                        OutlinedTextField(
                            name,
                            { name = it },
                            label = { Text("Name") },
                            isError = name.isBlank() || '/' in name,
                        )
                }
            },
            confirmButton = {
                TextButton(
                    {
                        when (action) {
                            "New folder" -> items = items + name
                            "Delete" -> items = items - selected.toSet()
                            "Rename" ->
                                items = items.map { if (it == selected.first()) name else it }
                            else ->
                                send(
                                    DemoAction.Notice(
                                        "${action} simulated for ${selected.size} items."
                                    )
                                )
                        }
                        selected = emptyList()
                        action = null
                    },
                    enabled =
                        action !in listOf("New folder", "Rename") ||
                            name.isNotBlank() && '/' !in name && name !in items,
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = { TextButton({ action = null }) { Text("Cancel") } },
        )
}

@Composable
private fun AppsDemo(send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    val names =
        listOf(
            "WhatsApp",
            "YouTube",
            "Chrome",
            "Termux",
            "Synapse",
            "Google Drive",
            "Gallery",
            "Camera",
            "Settings",
        )
    DesignPage(
        SynapseRoute.APPS,
        "Apps",
        "View, manage, and inspect installed applications. Demo inventory.",
        nav,
        footer = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("App actions", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Install APK", "Uninstall", "Disable", "Check updates").forEach { label
                        ->
                        DesignAction(label, Modifier.weight(1f)) { detail = label }
                    }
                }
            }
        },
    ) {
        DesignStatCards(
            Triple(SynapseIcon.APPS, "Installed apps", "72"),
            Triple(SynapseIcon.MEMORY, "App storage", "18.2 GB"),
            Triple(SynapseIcon.RECOVERY, "Updates available", "3 apps"),
        )
        DesignSearch(query, { query = it }, "Search apps, package name, or actions…")
        DesignSegments(listOf("All", "User", "System", "Updates"), filter) { filter = it }
        Section("", dense = true) {
            Column {
                names
                    .filterIndexed { i, n ->
                        n.contains(query, true) &&
                            when (filter) {
                                "System" -> i >= 6
                                "User" -> i < 6
                                "Updates" -> i in listOf(0, 2, 3)
                                else -> true
                            }
                    }
                    .forEach { title ->
                        DesignRow(
                            when (title) {
                                "Termux" -> SynapseIcon.TERMINAL
                                "Settings" -> SynapseIcon.SETTINGS
                                "Camera",
                                "Gallery" -> SynapseIcon.IMAGE
                                else -> SynapseIcon.APPS
                            },
                            title,
                            if (title == "Synapse") "dev.deviceagent.app · 24 MB"
                            else "Sample package · 38 MB",
                            "Open",
                            onClick = { detail = title },
                            leading =
                                if (title == "Synapse") {
                                    {
                                        Image(
                                            painterResource(R.drawable.synapse_brand_mark),
                                            null,
                                            Modifier.size(24.dp),
                                        )
                                    }
                                } else null,
                        )
                    }
            }
        }
    }
    DemoDetailDialog(
        detail,
        { detail = null },
        "Sample app details and action preview. Installation, removal, and launch are not performed by this demo.",
    )
}

@Composable
private fun PackagesDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    DesignPage(
        SynapseRoute.PACKAGES,
        "Packages",
        "Search, install, update, and manage packages in your Linux environment.",
        nav,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DesignAction("Install package", Modifier.weight(1f)) {
                    send(DemoAction.PackageSelected("ImageMagick"))
                    nav(SynapseRoute.PACKAGE_DETAIL)
                }
                DesignAction("Update packages", Modifier.weight(1f)) {
                    send(DemoAction.PackageSelected("FFmpeg"))
                    nav(SynapseRoute.PACKAGE_DETAIL)
                }
                DesignAction("Check health", Modifier.weight(1f)) {
                    send(
                        DemoAction.Notice(
                            "Demo check: Python, Git and Curl pass; FFmpeg needs the MP3 profile."
                        )
                    )
                }
            }
        },
    ) {
        DesignStatCards(
            Triple(SynapseIcon.PACKAGES, "Installed packages", "${s.installed.size}"),
            Triple(SynapseIcon.RECOVERY, "Updates available", "1"),
            Triple(SynapseIcon.MEMORY, "Package storage", "1.84 GB"),
        )
        DesignSearch(query, { query = it }, "Search packages (e.g. python, git, nodejs)…")
        DesignSegments(listOf("All", "Installed", "Available"), filter) { filter = it }
        val packages =
            demoPackages.filter {
                it.name.contains(query, true) &&
                    (filter == "All" || (it.name in s.installed) == (filter == "Installed"))
            }
        if (packages.isEmpty()) Notice("No package matches", "Try another name or filter.")
        Section("", dense = true) {
            Column {
                packages.forEach { pkg ->
                    DesignCatalogRow(
                        SynapseIcon.PACKAGES,
                        pkg.name,
                        pkg.purpose,
                        status = if (pkg.name in s.installed) "Installed" else "Available",
                        meta = pkg.version,
                        action = if (pkg.name in s.installed) "Open" else "Install",
                    ) {
                        send(DemoAction.PackageSelected(pkg.name))
                        nav(SynapseRoute.PACKAGE_DETAIL)
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageDetailDemo(
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    val pkg = demoPackages.first { it.name == s.packageName }
    var removal by rememberSaveable { mutableStateOf(false) }
    val steps =
        listOf(
            "Download verified artifact",
            "Check checksum & source",
            "Install in userspace",
            "Probe binary & capability",
        )
    DesignPage(
        SynapseRoute.PACKAGE_DETAIL,
        "Package Transaction",
        "Install, update, or remove packages and monitor the process.",
        nav,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (s.packageStep in 0..3) {
                    DesignAction(
                        "Simulate ${steps[s.packageStep].lowercase()}",
                        Modifier.weight(1.7f),
                        primary = true,
                    ) {
                        send(DemoAction.InstallStep)
                    }
                    DesignAction("Cancel transaction", Modifier.weight(1f), danger = true) {
                        send(DemoAction.CancelInstall)
                    }
                } else
                    DesignAction(
                        if (pkg.name in s.installed) "Approve update / repair"
                        else "Approve installation",
                        Modifier.weight(1.5f),
                        primary = true,
                    ) {
                        send(DemoAction.Install)
                    }
                if (pkg.name in s.installed)
                    DesignAction("Review removal", Modifier.weight(1f)) { removal = true }
                if (s.packageStep == 4 && pkg.name == "FFmpeg")
                    DesignAction("Return to recovery", Modifier.weight(1f)) {
                        nav(SynapseRoute.RECOVERY)
                    }
            }
        },
    ) {
        DesignSection(
            if (s.packageStep < 0) "Review package"
            else if (s.packageStep < 4) "Installing package" else "Capability verified",
            SynapseIcon.DOWNLOAD,
        ) {
            DesignRow(SynapseIcon.PACKAGES, pkg.name, "${pkg.version} · ${pkg.purpose}")
            InlineStats(
                "Repository" to "Demo main",
                "Download" to pkg.size,
                "Destination" to "Userspace",
            )
            LinearProgressIndicator(
                progress = { s.packageStep.coerceAtLeast(0) / 4f },
                modifier = Modifier.fillMaxWidth(),
            )
            steps.forEachIndexed { i, title ->
                DesignRow(
                    if (i < s.packageStep) SynapseIcon.APPROVAL else SynapseIcon.ACTIVITY,
                    title,
                    when {
                        i < s.packageStep -> "Verified in demo"
                        i == s.packageStep -> "Ready to simulate this step"
                        else -> "Waiting for earlier checks"
                    },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                DesignSection("Dependencies", SynapseIcon.PACKAGES) {
                    listOf(
                            "libc",
                            "openssl",
                            "zlib",
                            if (pkg.name == "FFmpeg") "libmp3lame" else "sqlite",
                        )
                        .forEach { SmallValue(it, "Library") }
                }
            }
            Column(Modifier.weight(1f)) {
                DesignSection("Package information", SynapseIcon.ABOUT) {
                    SmallValue("Name", pkg.name)
                    SmallValue("Version", pkg.version)
                    SmallValue("Size", pkg.size)
                    SmallValue("Architecture", "ARM64")
                    Text(
                        "Demo manifest · no repository contacted",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        DesignSection("Installation log", SynapseIcon.TERMINAL) {
            CodeBlock(
                "[demo] package: ${pkg.name}\n[demo] source: reviewed manifest\n[demo] step: ${s.packageStep.coerceAtLeast(0)} / 4\n[demo] verification: ${if(s.packageStep==4)"passed" else "pending"}"
            )
        }
    }
    if (removal)
        ConfirmAction(
            "Remove ${pkg.name}?",
            "Dependent tasks are checked first. Shared libraries remain if another package needs them.",
            "Remove package",
            { removal = false },
        ) {
            send(DemoAction.RemovePackage(pkg.name))
            removal = false
        }
}

@Composable
private fun TerminalDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var session by rememberSaveable { mutableIntStateOf(1) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    DesignPage(
        SynapseRoute.TERMINAL,
        "Terminal",
        "Run commands in a secure environment. This terminal is a simulation.",
        nav,
    ) {
        DesignStatCards(
            Triple(SynapseIcon.TERMINAL, "Environment", "Android (Termux)"),
            Triple(SynapseIcon.SECURITY, "Sandboxed", "Isolated"),
            Triple(SynapseIcon.APPROVAL, "Status", "Ready"),
        )
        DesignSection(
            "Terminal $session",
            SynapseIcon.TERMINAL,
            "+ New session",
            {
                session++
                send(DemoAction.Command(""))
            },
        ) {
            CodeBlock(
                "synapse@demo:~ $ ${s.terminalCommand}\n\nOS: Android 13 (One UI Core 5.1)\nHost: Samsung SM-A047F\nKernel: 4.19.191-perf+\nUptime: 3 days, 4 hours\nPackages: 312 (apt)\nShell: bash 5.1.16\nCPU: ARMv8 (8) @ 2.0GHz\nMemory: 1762MiB / 3671MiB\n\n${if(s.terminalComplete)"[demo] Command preview completed. No shell was invoked." else "[demo] Waiting for command preview."}",
                dense = true,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    DesignSearch(
                        s.terminalCommand,
                        { send(DemoAction.Command(it)) },
                        "Command preview",
                    )
                }
                DesignAction(
                    "Run preview",
                    primary = true,
                    enabled = s.terminalCommand.isNotBlank(),
                ) {
                    send(DemoAction.RunCommand)
                }
                DesignAction("Clear") { send(DemoAction.Command("")) }
            }
        }
        DesignSection("Quick commands", SynapseIcon.TOOLS, dense = true) {
            listOf(
                    "List files" to "ls -la",
                    "Show device info" to "getprop ro.product.model",
                    "Check storage" to "df -h",
                    "Show processes" to "ps",
                    "Install package" to "pkg install python",
                    "Update packages" to "pkg upgrade",
                    "Show IP address" to "ip addr",
                    "Cache usage" to "du -sh ~/.cache",
                )
                .chunked(4)
                .forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (title, cmd) ->
                            Surface(
                                onClick = { send(DemoAction.Command(cmd)) },
                                modifier = Modifier.weight(1f),
                                color =
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Column(Modifier.padding(5.dp)) {
                                    Text(title, fontSize = 8.sp, lineHeight = 11.sp)
                                    Text(
                                        cmd,
                                        fontSize = 7.sp,
                                        lineHeight = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow =
                                            androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
        }
        DesignSection("Recent sessions", SynapseIcon.HISTORY, dense = true) {
            DesignDetailRow(
                SynapseIcon.TERMINAL,
                "System information",
                "neofetch, termux-info",
                "Today",
                onClick = { detail = "System information" },
            )
            DesignDetailRow(
                SynapseIcon.TERMINAL,
                "Storage check",
                "df -h, du -sh",
                "Yesterday",
                onClick = { detail = "Storage check" },
            )
            DesignDetailRow(
                SynapseIcon.TERMINAL,
                "Install tools",
                "pkg install python nodejs",
                "Yesterday",
                onClick = { send(DemoAction.Command("pkg install python nodejs")) },
            )
        }
        Secondary("Review execution policy") { nav(SynapseRoute.SECURITY) }
    }
    DemoDetailDialog(detail, { detail = null })
}

@Composable
private fun ToolsDemo(nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All categories") }
    var status by rememberSaveable { mutableStateOf("All status") }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var enabled by rememberSaveable {
        mutableStateOf(
            listOf(
                "Shell",
                "File Operations",
                "Package Manager",
                "App Management",
                "Network",
                "Device Info",
                "Screenshot",
                "Screen Recording",
                "Audio Recorder",
                "Text Processing",
            )
        )
    }
    val groups =
        listOf(
            "Core system tools" to
                listOf(
                    SynapseIcon.TERMINAL to "Shell",
                    SynapseIcon.FOLDER to "File Operations",
                    SynapseIcon.PACKAGES to "Package Manager",
                    SynapseIcon.APPS to "App Management",
                    SynapseIcon.NETWORK to "Network",
                    SynapseIcon.DEVICE to "Device Info",
                ),
            "Media tools" to
                listOf(
                    SynapseIcon.IMAGE to "Screenshot",
                    SynapseIcon.PLAY to "Screen Recording",
                    SynapseIcon.PAUSE to "Audio Recorder",
                    SynapseIcon.RECOVERY to "Media Converter",
                ),
            "AI and analysis tools" to
                listOf(
                    SynapseIcon.FILE to "Text Processing",
                    SynapseIcon.IMAGE to "Image Analysis",
                    SynapseIcon.TERMINAL to "Code Analysis",
                    SynapseIcon.PROVIDERS to "Web Search",
                ),
        )
    DesignPage(
        SynapseRoute.TOOLS,
        "Tools",
        "Manage available tools for your AI agent and device operations.",
        nav,
    ) {
        DesignStatCards(
            Triple(SynapseIcon.TOOLS, "Available tools", "14"),
            Triple(SynapseIcon.APPS, "Enabled tools", "${enabled.size}"),
            Triple(SynapseIcon.TERMINAL, "Tool execution", "Demo only"),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(Modifier.weight(1f)) {
                DesignSearch(
                    query,
                    { query = it },
                    "Search tools, description, or category…",
                    dense = true,
                )
            }
            Box(Modifier.width(98.dp)) {
                DesignPreference("", listOf("All categories") + groups.map { it.first }, category) {
                    category = it
                }
            }
            Box(Modifier.width(78.dp)) {
                DesignPreference("", listOf("All status", "Enabled", "Disabled"), status) {
                    status = it
                }
            }
        }
        if (
            groups.none { (title, tools) ->
                tools.any { (_, name) ->
                    (name.contains(query, true) || title.contains(query, true)) &&
                        (category == "All categories" || category == title) &&
                        (status == "All status" || (name in enabled) == (status == "Enabled"))
                }
            }
        )
            DesignNotice("No matching tools", "Try another query, category, or status.")
        groups.forEach { (title, tools) ->
            val visible =
                tools.filter { (_, name) ->
                    (name.contains(query, true) || title.contains(query, true)) &&
                        (category == "All categories" || category == title) &&
                        (status == "All status" || (name in enabled) == (status == "Enabled"))
                }
            if (visible.isNotEmpty())
                DesignCatalogSection(title, SynapseIcon.TOOLS, "${visible.size} tools") {
                    visible.forEach { (icon, name) ->
                        DesignCatalogRow(
                            icon,
                            name,
                            when (name) {
                                "Shell" -> "Execute shell commands in sandbox."
                                "File Operations" -> "Read, write, move and manage files."
                                "Package Manager" -> "Install, update and remove packages."
                                "App Management" -> "Manage installed applications."
                                "Network" -> "Network information and operations."
                                "Device Info" -> "Device information and status."
                                else -> "Sample ${name.lowercase()} tool."
                            },
                            status = if (name in enabled) "Enabled" else "Disabled",
                            meta = "v1.0",
                            action = if (name in enabled) "Configure" else "Install",
                        ) {
                            detail = name
                        }
                    }
                }
        }
    }
    if (detail != null)
        DesignDialog(
            onDismissRequest = { detail = null },
            title = { Text(detail!!) },
            text = {
                Text(
                    "Change this demo tool’s enabled state. No tool is installed or executed on your device."
                )
            },
            confirmButton = {
                TextButton({
                    val n = detail!!
                    enabled = if (n in enabled) enabled - n else enabled + n
                    detail = null
                }) {
                    Text(if (detail in enabled) "Disable sample" else "Enable sample")
                }
            },
            dismissButton = { TextButton({ detail = null }) { Text("Cancel") } },
        )
}
