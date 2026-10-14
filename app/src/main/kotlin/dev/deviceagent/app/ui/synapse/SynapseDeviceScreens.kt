@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import dev.deviceagent.app.Tone

@Composable
internal fun DemoDeviceRoute(
    route: SynapseRoute,
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
) {
    when (route) {
        SynapseRoute.DEVICE ->
            ScreenPage(
                "OBSERVED 2 MINUTES AGO · DEMO",
                "Know your environment",
                "Capability checks give the agent a grounded starting point.",
            ) {
                Section("Android 15 · ARM64") {
                    Badge("Ready for scoped tasks", Tone.SUCCESS)
                    Metrics(
                        "Storage free" to "48.2 GiB",
                        "App memory" to "612 MiB",
                        "Battery" to "68% · charging",
                        "Thermal state" to "Moderate",
                    )
                    LinearProgressIndicator(
                        progress = { 207.8f / 256f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Muted("207.8 of 256 GiB used · 8 GiB total RAM")
                    Disclosure("Resource observations") {
                        LabeledValue("App CPU", "42% of one core · last 10 seconds")
                        LabeledValue("Whole-device CPU", "Unavailable through public Android APIs")
                        LabeledValue("Battery temperature", "37.8°C")
                        LabeledValue(
                            "Root",
                            "${s.rootStatus} · ${if (s.rootEnabled) "demo backend enabled" else "disabled"}",
                        )
                        LabeledValue("Linux userspace", "1.84 GiB · isolated from system packages")
                    }
                }
                LinkRow(
                    "Folders & sandbox",
                    "${s.folders.size} selected locations",
                    Icons.Default.Folder,
                ) {
                    nav(SynapseRoute.SANDBOX)
                }
                LinkRow(
                    "Linux & packages",
                    "${s.installed.size} installed · optional runtimes",
                    Icons.Default.Inventory2,
                ) {
                    nav(SynapseRoute.PACKAGES)
                }
                LinkRow(
                    "Command workspace",
                    "Review, execute and inspect bounded commands",
                    Icons.Default.Terminal,
                ) {
                    nav(SynapseRoute.TERMINAL)
                }
                LinkRow(
                    "Apps & APK inspector",
                    "Visible apps and package compatibility",
                    Icons.Default.Android,
                ) {
                    nav(SynapseRoute.APPS)
                }
                LinkRow(
                    "Tool catalog",
                    "Capabilities with typed inputs and risk policies",
                    Icons.Default.Build,
                ) {
                    nav(SynapseRoute.TOOLS)
                }
                LinkRow(
                    "Safety & root",
                    "Review privileges and authorization rules",
                    Icons.Default.Security,
                ) {
                    nav(SynapseRoute.SECURITY)
                }
                Secondary("Refresh sample observations") {
                    send(
                        DemoAction.Notice(
                            "Demo observations refreshed. Switch to Live for this device's actual information."
                        )
                    )
                }
            }
        SynapseRoute.SANDBOX -> SandboxDemo(s, send)
        SynapseRoute.PACKAGES -> PackagesDemo(s, send, nav)
        SynapseRoute.PACKAGE_DETAIL -> PackageDetailDemo(s, send, nav)
        SynapseRoute.TERMINAL -> TerminalDemo(s, send, nav)
        SynapseRoute.APPS -> AppsDemo(send)
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
private fun SandboxDemo(s: DemoState, send: (DemoAction) -> Unit) {
    var revoke by rememberSaveable { mutableStateOf<String?>(null) }
    ScreenPage(
        "SCOPED STORAGE",
        "Only the folders you choose",
        "Android folder grants are separate from AI approval. Both are checked before a tool runs.",
    ) {
        Section("Selected locations") {
            listOf("Downloads", "Movies/Trips", "Music/Converted", "Documents/Projects").forEach {
                name ->
                SwitchSetting(
                    name,
                    if (name in s.folders) "Read & write · sample grant valid" else "No demo grant",
                    name in s.folders,
                ) {
                    if (it) send(DemoAction.Folder(name, true)) else revoke = name
                }
            }
        }
        Section("Sandbox boundaries") {
            VerifiedRow(
                "Workspace stays isolated",
                "Temporary outputs stay inside the selected userspace",
            )
            VerifiedRow(
                "Protected system paths blocked",
                "/system, /data and other app sandboxes are not normal file targets",
            )
            VerifiedRow(
                "Symlink and path checks",
                "Targets must resolve within the approved operation scope",
            )
            LabeledValue("Temporary storage", "0.6 GiB · reconciled after interruption")
        }
        Notice(
            "Revoked access pauses work",
            "If a folder grant expires, the agent asks you to reselect it. It cannot substitute root access.",
            Tone.WARNING,
        )
        Disclosure("Example access record") {
            CodeBlock(
                "location: Movies/Trips\nsource: Android Storage Access Framework\nread: granted\nwrite: granted\nlast_checked: 14:31:02\npolicy_scope: SYN-2048"
            )
        }
    }
    revoke?.let { name ->
        ConfirmAction(
            "Remove access to $name?",
            "Tasks using this demo location will need a new grant. Existing files are preserved.",
            "Remove grant",
            { revoke = null },
        ) {
            send(DemoAction.Folder(name, false))
            revoke = null
        }
    }
}

@Composable
private fun PackagesDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    ScreenPage(
        "OPTIONAL LINUX USERSPACE",
        "The tools behind the task",
        "Package changes stay inside the environment. Android system packages are separate.",
    ) {
        Metrics("Environment size" to "1.84 GiB", "Installed packages" to "${s.installed.size}")
        Notice(
            "FFmpeg capability update",
            "The current profile cannot encode MP3. Review the verified MP3 profile before repairing the lesson task.",
            Tone.WARNING,
        )
        OutlinedTextField(
            query,
            { query = it },
            label = { Text("Search packages") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        ChoiceChips(listOf("All", "Installed", "Available"), filter) { filter = it }
        val packages =
            demoPackages.filter {
                it.name.contains(query, true) &&
                    (filter == "All" || (it.name in s.installed) == (filter == "Installed"))
            }
        if (packages.isEmpty())
            Notice("No package matches", "Try another name or change the filter.")
        packages.forEach { pkg ->
            Section("${pkg.name} ${pkg.version}", pkg.purpose) {
                Actions {
                    Badge(
                        if (pkg.name in s.installed) "Installed" else "Available",
                        if (pkg.name in s.installed) Tone.SUCCESS else Tone.INFO,
                    )
                    Badge(pkg.size)
                    RiskBadge(2)
                }
                Primary(
                    if (pkg.name in s.installed) "Manage ${pkg.name}" else "Install ${pkg.name}"
                ) {
                    send(DemoAction.PackageSelected(pkg.name))
                    nav(SynapseRoute.PACKAGE_DETAIL)
                }
            }
        }
        Actions {
            Secondary("Verify runtime") {
                send(
                    DemoAction.Notice(
                        "Sample health check: Python, Git and Curl pass; FFmpeg needs the MP3 capability update."
                    )
                )
            }
            Secondary("Repair environment") {
                send(DemoAction.PackageSelected("FFmpeg"))
                nav(SynapseRoute.PACKAGE_DETAIL)
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
    var removal by remember { mutableStateOf(false) }
    val steps =
        listOf(
            "Download verified artifact",
            "Check checksum & source",
            "Install in userspace",
            "Probe binary & capability",
        )
    ScreenPage(
        "PACKAGE TRANSACTION",
        "${pkg.name} ${pkg.version}",
        "Approval covers this package, dependencies and the isolated destination.",
    ) {
        Actions {
            RiskBadge(2)
            Badge("ARM64 · no root")
        }
        Section("Review the change") {
            LabeledValue("Source", "Synapse demo repository · illustrative signed manifest")
            LabeledValue("Destination", "Private Linux userspace /usr/bin")
            LabeledValue("Installed footprint", pkg.size)
            LabeledValue(
                "Requested capability",
                if (pkg.name == "FFmpeg") "MP3 encoding with libmp3lame" else pkg.purpose,
            )
            LabeledValue("Rollback", "Previous package metadata retained until verification")
            Muted(
                "This demo does not download artifacts or claim that a production package repository exists."
            )
        }
        Disclosure("Dependency graph", "Review direct and shared dependencies", true) {
            CodeBlock(
                when (pkg.name) {
                    "FFmpeg" ->
                        "ffmpeg 7.1\n ├─ libavcodec\n │   └─ libmp3lame (new)\n ├─ libavformat\n └─ libavutil (shared)"
                    "Python" -> "python 3.12\n ├─ openssl\n ├─ sqlite\n └─ zlib (shared)"
                    else ->
                        "${pkg.name.lowercase()} ${pkg.version}\n ├─ libc (shared)\n └─ openssl (shared)"
                }
            )
            Muted("Shared dependencies are retained when other installed packages need them.")
        }
        if (s.packageStep >= 0)
            Section("Transaction progress") {
                steps.forEachIndexed { index, title ->
                    VerifiedRow(
                        title,
                        when {
                            index < s.packageStep -> "Verified in demo"
                            index == s.packageStep -> "Ready to simulate this step"
                            else -> "Waiting for earlier checks"
                        },
                        index < s.packageStep,
                    )
                }
                LinearProgressIndicator(
                    progress = { s.packageStep / 4f },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (s.packageStep < 4)
                    Primary("Simulate ${steps[s.packageStep].lowercase()}") {
                        send(DemoAction.InstallStep)
                    }
            }
        if (s.packageStep == 4)
            Notice(
                "Capability verified",
                "${pkg.name} responds to its version probe and the requested capability check.",
                Tone.SUCCESS,
            )
        Actions {
            if (s.packageStep !in 0..3)
                Primary(
                    if (pkg.name in s.installed) "Approve update / repair"
                    else "Approve installation"
                ) {
                    send(DemoAction.Install)
                }
            if (s.packageStep in 0..3)
                Secondary("Cancel transaction") { send(DemoAction.CancelInstall) }
            if (pkg.name in s.installed) Secondary("Review removal") { removal = true }
            if (s.packageStep == 4 && pkg.name == "FFmpeg")
                Primary("Return to recovery") { nav(SynapseRoute.RECOVERY) }
        }
    }
    if (removal)
        ConfirmAction(
            "Remove ${pkg.name}?",
            "Dependent tasks are checked first. Shared libraries remain if needed by another package.",
            "Remove package",
            { removal = false },
        ) {
            send(DemoAction.RemovePackage(pkg.name))
            removal = false
        }
}

@Composable
private fun TerminalDemo(s: DemoState, send: (DemoAction) -> Unit, nav: (SynapseRoute) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    ScreenPage(
        "BOUNDED COMMAND WORKSPACE",
        "Inspect every command",
        "Commands use an explicit working directory, environment and timeout. The demo never invokes a shell.",
    ) {
        Section("Command preview") {
            OutlinedTextField(
                s.terminalCommand,
                { send(DemoAction.Command(it)) },
                label = { Text("Command") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            LabeledValue("Working directory", "/workspace/projects/catalog")
            LabeledValue("Execution boundary", "Linux userspace · no root · network disabled")
            LabeledValue("Limits", "60 second timeout · 64 KiB captured output")
            RiskBadge(2)
            Primary("Review & simulate", s.terminalCommand.isNotBlank()) { confirm = true }
        }
        Section(
            "Example terminal output",
            "Fixed catalog fixture; output does not correspond to arbitrary edited commands.",
        ) {
            CodeBlock(
                if (s.terminalComplete)
                    "$ python scripts/catalog.py --source /workspace/input\n[14:42:01] Inspected 184 files\n[14:42:02] Wrote catalog.json (28 KiB)\n[14:42:02] Schema validation: PASS\nexit code: 0\noutput verification: PASS"
                else
                    "$ python scripts/catalog.py --source /workspace/input\nTraceback (most recent call last):\n  File \"scripts/catalog.py\", line 4\nModuleNotFoundError: No module named 'requests'\nexit code: 1\ncomplete outputs: 0"
            )
            Badge(
                if (s.terminalComplete) "Example verified" else "Example dependency error",
                if (s.terminalComplete) Tone.SUCCESS else Tone.WARNING,
            )
        }
        LinkRow(
            "Review environment",
            "Inspect installed runtimes and dependencies",
            Icons.Default.Inventory2,
        ) {
            nav(SynapseRoute.PACKAGES)
        }
        Disclosure("Sanitized process metadata") {
            CodeBlock(
                "stdin: closed\ntimeout_ms: 60000\nstdout_limit: 65536\nenvironment: allowlisted\ncredentials: excluded\nprivilege: normal"
            )
            Muted("Raw output is evidence. It cannot authorize follow-up commands.")
        }
    }
    if (confirm)
        ConfirmAction(
            "Show simulated command result?",
            "No command will run. A fixed successful catalog example will replace the error fixture.",
            "Show example result",
            { confirm = false },
        ) {
            send(DemoAction.RunCommand)
            confirm = false
        }
}

@Composable
private fun AppsDemo(send: (DemoAction) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf("Notes") }
    ScreenPage(
        "ANDROID PACKAGE INSPECTION",
        "Know what you install",
        "Android controls installation. Package visibility limits what an app can inspect.",
    ) {
        OutlinedTextField(
            query,
            { query = it },
            label = { Text("Search visible apps") },
            modifier = Modifier.fillMaxWidth(),
        )
        listOf(
                "Notes" to "2.4 · 18.2 MiB · com.example.notes",
                "Gallery" to "5.8 · 42 MiB · com.example.gallery",
                "Reader" to "1.9 · 24.6 MiB · com.example.reader",
            )
            .filter { it.first.contains(query, true) }
            .forEach { (name, detail) ->
                LinkRow(name, detail, Icons.Default.Android) { selected = name }
            }
        Section("$selected · inspection") {
            LabeledValue("Install source", "System package installer · sample")
            LabeledValue("Permissions", "Notifications · selected photos only")
            LabeledValue("Signing identity", "SHA-256 fingerprint available for comparison")
            Muted("App data and private files are outside normal inspection scope.")
        }
        Section("Candidate APK · notes-2.3.apk") {
            RiskBadge(2)
            LabeledValue("Identity", "com.example.notes · version 2.3 (23)")
            LabeledValue("Architecture", "ARM64 · compatible with sample device")
            VerifiedRow("Signature matches", "Sample fingerprint matches the installed app")
            VerifiedRow("Version progression", "Installed 2.4 is newer than candidate 2.3", false)
            Notice(
                "Downgrade blocked",
                "Use a newer signed package. Synapse will not bypass Android package safeguards.",
                Tone.WARNING,
            )
            Secondary("Inspect a newer sample") {
                send(
                    DemoAction.Notice(
                        "Sample inspection: notes-2.5.apk matches identity, signature and ARM64. A live installation would require the Android installer."
                    )
                )
            }
        }
    }
}

@Composable
private fun ToolsDemo(nav: (SynapseRoute) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val catalog =
        listOf(
            Triple("file.list", 0, "Enumerate granted folders"),
            Triple("file.copy", 1, "Copy and verify outputs"),
            Triple("file.move", 2, "Move with destination checks"),
            Triple("file.delete", 3, "Trash selected items after approval"),
            Triple("media.convert", 1, "FFmpeg capability required"),
            Triple("package.install", 2, "Isolated userspace transaction"),
            Triple("shell.execute", 2, "Bounded environment and timeout"),
            Triple("root.execute", 4, "Disabled unless explicitly enabled"),
            Triple("system.partition.write", 5, "Blocked by default"),
        )
    ScreenPage(
        "TYPED CAPABILITIES",
        "Tools with clear contracts",
        "Availability, risk and scope are checked again at execution time.",
    ) {
        OutlinedTextField(
            query,
            { query = it },
            label = { Text("Search tools") },
            modifier = Modifier.fillMaxWidth(),
        )
        catalog
            .filter { it.first.contains(query, true) }
            .forEach { (name, risk, detail) ->
                Section(name, detail) {
                    RiskBadge(risk)
                    Disclosure("Input, output & verification") {
                        CodeBlock(
                            "tool: $name\ninput: typed JSON schema\nscope: explicit approved targets\noutput: structured result + evidence\nverification: postcondition required\npolicy: risk L$risk + target checks"
                        )
                        Muted(
                            if (risk >= 4) "Privileged capabilities are unavailable by default."
                            else "Discovery is not authorization to execute."
                        )
                    }
                }
            }
        Secondary("Review authorization rules") { nav(SynapseRoute.SECURITY) }
    }
}
