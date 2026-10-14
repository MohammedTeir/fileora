@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.deviceagent.app.ui.synapse

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.deviceagent.app.*
import dev.deviceagent.app.ui.*
import java.util.Locale

@Composable
internal fun LiveRouteContent(
    route: SynapseRoute,
    s: UiState,
    vm: AgentViewModel,
    browser: FileBrowserViewModel,
    nav: (SynapseRoute) -> Unit,
    demo: () -> Unit,
    start: () -> Unit,
    notifications: () -> Unit,
    appearance: String,
    onAppearance: (String) -> Unit,
    reducedMotion: Boolean,
    onReducedMotion: (Boolean) -> Unit,
) {
    when (route) {
        SynapseRoute.WELCOME ->
            OnboardingFlow(s, vm) { openAi ->
                nav(if (openAi) SynapseRoute.PROVIDERS else SynapseRoute.AGENT)
            }
        SynapseRoute.AGENT ->
            HomeScreen(
                s,
                vm,
                onOpenSettings = { nav(SynapseRoute.PROVIDERS) },
                onTurnOnNotifications = notifications,
                onStart = start,
                onOpenTasks = { nav(SynapseRoute.ACTIVITY) },
                onAttachments = { nav(SynapseRoute.ATTACHMENTS) },
            )
        SynapseRoute.ACTIVITY,
        SynapseRoute.AUDIT,
        SynapseRoute.UNDO,
        SynapseRoute.SCHEDULE -> ActivityScreen(s, vm)
        SynapseRoute.BROWSE,
        SynapseRoute.ATTACHMENTS ->
            FileBrowserScreen(
                s,
                browser,
                onUseInTask = { paths ->
                    vm.setGoal("Do this with these items:\n" + paths.joinToString("\n") { "- $it" })
                    nav(SynapseRoute.AGENT)
                },
                onAskAi = {
                    vm.setGoal(it)
                    nav(SynapseRoute.AGENT)
                },
            )
        SynapseRoute.SEARCH ->
            SearchScreen(s, browser) {
                browser.reveal(it)
                nav(SynapseRoute.BROWSE)
            }
        SynapseRoute.PROVIDERS,
        SynapseRoute.CREDENTIALS,
        SynapseRoute.MODELS ->
            ScreenPage(
                "LIVE PROVIDER",
                route.title,
                "Configure your account, discover models and test the actual connection.",
            ) {
                AiServiceCard(s, vm)
                LinkRow(
                    "Cloud privacy",
                    "Review current sharing controls",
                    Icons.Default.PrivacyTip,
                ) {
                    nav(SynapseRoute.PRIVACY)
                }
            }
        SynapseRoute.PRIVACY ->
            ScreenPage(
                "LIVE PRIVACY",
                "Cloud sharing",
                "These controls apply to actual agent requests.",
            ) {
                PrivacyCard(s, vm)
                Notice(
                    "Granular controls preview",
                    "The demo illustrates additional per-category toggles. Those toggles do not change the live request policy.",
                )
                Secondary("Explore privacy design", onClick = demo)
            }
        SynapseRoute.SANDBOX ->
            ScreenPage(
                "LIVE ACCESS",
                "Folders & permissions",
                "Select device folders through Android's access controls.",
            ) {
                FoldersCard(s, vm)
                LinkRow(
                    "Browse granted files",
                    "Inspect selected locations",
                    Icons.Default.Folder,
                ) {
                    nav(SynapseRoute.BROWSE)
                }
                LinkRow("Search files", "Find items in granted folders", Icons.Default.Search) {
                    nav(SynapseRoute.SEARCH)
                }
            }
        SynapseRoute.USAGE ->
            ScreenPage(
                "LIVE ACCOUNTING",
                "Provider usage",
                "Usage recorded by the current agent session.",
            ) {
                Section("Recorded usage") {
                    Text(s.usageSummary.ifBlank { "No provider usage recorded yet." })
                }
                Secondary("Explore budgets demo", onClick = demo)
            }
        SynapseRoute.DEVICE -> LiveDevice(s, nav, demo)
        SynapseRoute.SECURITY ->
            ScreenPage(
                "LIVE SAFETY",
                "Execution policy",
                "Existing tool risk checks and approval dialogs remain active.",
            ) {
                AdvancedCard(s, vm)
                Notice(
                    "Optional backends",
                    "Root authorization and autonomous-mode controls are demonstrated in the demo workspace; their backends are not implemented.",
                )
                Secondary("Explore policy demo", onClick = demo)
            }
        SynapseRoute.SETTINGS ->
            ScreenPage(
                "LIVE PREFERENCES",
                "Settings",
                "Settings here affect your installed Synapse app.",
            ) {
                LinkRow(
                    "AI providers",
                    "Credentials, models and connection checks",
                    Icons.Default.Key,
                ) {
                    nav(SynapseRoute.PROVIDERS)
                }
                LinkRow(
                    "Folders & access",
                    "${s.roots.size} selected locations",
                    Icons.Default.Folder,
                ) {
                    nav(SynapseRoute.SANDBOX)
                }
                LinkRow("Cloud privacy", "Control file-content sharing", Icons.Default.PrivacyTip) {
                    nav(SynapseRoute.PRIVACY)
                }
                LinkRow("Safety & advanced", "Review execution settings", Icons.Default.Security) {
                    nav(SynapseRoute.SECURITY)
                }
                LinkRow("Usage", "Current session accounting", Icons.Default.DataUsage) {
                    nav(SynapseRoute.USAGE)
                }
                Section("Appearance & accessibility") {
                    ChoiceChips(listOf("System", "Light", "Dark"), appearance, onAppearance)
                    SwitchSetting(
                        "Reduce motion",
                        "Use immediate screen transitions.",
                        reducedMotion,
                        onChange = onReducedMotion,
                    )
                }
                Section("Notifications") {
                    Text(
                        if (s.notificationsEnabled) "Task notifications are enabled."
                        else "Enable notifications for background task updates."
                    )
                    Secondary("Notification settings", onClick = notifications)
                }
                BackupSection(vm)
                AboutCard()
                Secondary("Explore all demo screens", onClick = demo)
            }
        SynapseRoute.MONITOR,
        SynapseRoute.CHECKPOINT -> LiveMonitor(s, vm, nav)
        SynapseRoute.PLAN,
        SynapseRoute.APPROVAL ->
            ScreenPage(
                "LIVE PLAN REVIEW",
                "Review before execution",
                "Live approvals appear when the policy engine requests them, above every screen.",
            ) {
                Notice(
                    if (s.running) "Task in progress" else "Start with a goal",
                    "Enable dry run to inspect proposed file operations. The agent will request approval for protected changes.",
                )
                SwitchSetting(
                    "Dry run",
                    "Inspect without applying supported file changes.",
                    s.dryRun,
                    !s.running,
                ) {
                    vm.setDryRun(it)
                }
                Primary("Open agent") { nav(SynapseRoute.AGENT) }
                Secondary("Explore plan design", onClick = demo)
            }
        else ->
            ScreenPage(
                "BACKEND NOT AVAILABLE",
                route.title,
                "This operational interface is fully populated in the demo workspace.",
            ) {
                Notice(
                    "Implementation boundary",
                    when (route) {
                        SynapseRoute.RECOVERY ->
                            "The dedicated recovery UI is a simulation. Actual task failures and recovery evidence remain available in task activity."
                        SynapseRoute.MEMORY ->
                            "Editable environment memory is not connected to a persistent agent memory backend yet."
                        SynapseRoute.PACKAGES,
                        SynapseRoute.PACKAGE_DETAIL ->
                            "The optional Linux userspace, package repository and verified installer are not implemented yet."
                        SynapseRoute.TERMINAL ->
                            "A bounded command execution backend is not installed. Demo commands never execute."
                        SynapseRoute.APPS ->
                            "The APK inspection and installation workflow is not connected yet."
                        else ->
                            "The complete capability catalog is a design simulation. Existing agent tools continue to enforce their own policies."
                    },
                    Tone.WARNING,
                )
                Primary("Explore ${route.title.lowercase()} demo", onClick = demo)
                Secondary("View actual task activity") { nav(SynapseRoute.ACTIVITY) }
            }
    }
}

@Composable
private fun LiveMonitor(s: UiState, vm: AgentViewModel, nav: (SynapseRoute) -> Unit) {
    var cancel by remember { mutableStateOf(false) }
    ScreenPage(
        "LIVE EXECUTION",
        "Task monitor",
        "Only actual observations from the current agent session appear here.",
    ) {
        Badge(if (s.running) "Running" else "Idle")
        if (s.running) LinearProgressIndicator(Modifier.fillMaxWidth())
        Section("Goal") { Text(s.goal.ifBlank { "No goal entered yet." }) }
        if (s.checkpointAvailable)
            Section("Saved checkpoint") {
                Text(s.checkpointDescription)
                Primary("Resume saved task", !s.running) { vm.resumeTask() }
            }
        if (s.log.isEmpty())
            Notice(
                "No execution evidence yet",
                "Start a task to see inspection, policy, tool and verification events.",
            )
        else
            Section("Recent execution evidence") {
                s.log.takeLast(40).forEachIndexed { index, line ->
                    Disclosure(
                        "Event ${s.log.size.coerceAtMost(40).let { s.log.size - it } + index + 1}",
                        line.take(140),
                    ) {
                        CodeBlock(line)
                    }
                }
            }
        Actions {
            if (s.running) Secondary("Stop task") { cancel = true }
            Primary("Task history & results") { nav(SynapseRoute.ACTIVITY) }
            Secondary("Return to agent") { nav(SynapseRoute.AGENT) }
        }
    }
    if (cancel)
        ConfirmAction(
            "Stop the live task?",
            "Completed changes remain recorded. Review the task result before attempting undo.",
            "Stop task",
            { cancel = false },
        ) {
            vm.cancelTask()
            cancel = false
        }
}

@Composable
private fun LiveDevice(s: UiState, nav: (SynapseRoute) -> Unit, demo: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val info =
        remember(refresh) {
            val memory = ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(
                memory
            )
            val storage = StatFs(context.filesDir.absolutePath)
            fun gib(bytes: Long) =
                String.format(Locale.getDefault(), "%.1f GiB", bytes / 1073741824.0)
            listOf(
                "Storage free" to gib(storage.availableBytes),
                "Storage capacity" to gib(storage.totalBytes),
                "RAM available" to gib(memory.availMem),
                "RAM total" to gib(memory.totalMem),
            )
        }
    ScreenPage(
        "THIS DEVICE · LIVE",
        "Environment overview",
        "Public Android observations. Unavailable capabilities are shown explicitly.",
    ) {
        Section("${Build.MANUFACTURER} ${Build.MODEL}") {
            LabeledValue("Android", "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}")
            LabeledValue("Architecture", Build.SUPPORTED_ABIS.joinToString())
            Metrics(*info.toTypedArray())
            Muted("Storage describes the app's data volume; removable volumes can differ.")
        }
        Section("Capabilities") {
            LabeledValue("Folder grants", "${s.roots.size} selected locations")
            LabeledValue("Root backend", "Not implemented · no detection attempted")
            LabeledValue("Linux userspace", "Not installed")
            LabeledValue("Whole-device CPU", "Unavailable through public APIs")
        }
        LinkRow("Folders & sandbox", "Review grants and browse files", Icons.Default.Folder) {
            nav(SynapseRoute.SANDBOX)
        }
        LinkRow("Browse files", "Your granted locations", Icons.Default.FolderOpen) {
            nav(SynapseRoute.BROWSE)
        }
        LinkRow("Search files", "Find files by name", Icons.Default.Search) {
            nav(SynapseRoute.SEARCH)
        }
        Actions {
            Secondary("Refresh observations") { refresh++ }
            Secondary("Explore environment demo", onClick = demo)
        }
    }
}
