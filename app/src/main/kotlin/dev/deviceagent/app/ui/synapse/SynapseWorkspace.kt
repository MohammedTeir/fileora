package dev.deviceagent.app.ui.synapse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.app.R
import dev.deviceagent.app.ui.SynapseDesignTokens

private fun Destination.icon(): ImageVector =
    when (this) {
        Destination.AGENT -> Icons.Default.AutoAwesome
        Destination.ACTIVITY -> Icons.Default.History
        Destination.DEVICE -> Icons.Default.PhoneAndroid
        Destination.SETTINGS -> Icons.Default.Settings
    }

/** Demo dispatch has no reference to a device executor; live content is supplied separately. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SynapseWorkspace(
    s: DemoState,
    send: (DemoAction) -> Unit,
    initialRoute: SynapseRoute,
    modelLabel: String,
    snackbar: SnackbarHostState,
    appearance: String,
    onAppearance: (String) -> Unit,
    reducedMotion: Boolean,
    onReducedMotion: (Boolean) -> Unit,
    reset: () -> Unit,
    initialDemo: Boolean = false,
    liveContent: @Composable (SynapseRoute, (SynapseRoute) -> Unit, () -> Unit) -> Unit,
) {
    var resetEpoch by rememberSaveable { mutableIntStateOf(0) }
    var demo by rememberSaveable { mutableStateOf(initialDemo) }
    var stack by rememberSaveable { mutableStateOf(initialRoute.name) }
    var preview by rememberSaveable { mutableStateOf(PreviewState.READY.name) }
    var directory by rememberSaveable { mutableStateOf(false) }
    var states by remember { mutableStateOf(false) }
    val route = SynapseRoute.valueOf(stack.substringAfterLast('|'))
    val navigate: (SynapseRoute) -> Unit = { target ->
        if (demo && directory && target in listOf(SynapseRoute.TASK_RESULT, SynapseRoute.AUDIT, SynapseRoute.UNDO)) send(DemoAction.SelectTask(completedVideoDemoId))
        if (target != route) stack = (stack.split('|').takeLast(20) + target.name).joinToString("|")
        preview = PreviewState.READY.name
        directory = false
    }
    val back: () -> Unit = {
        stack = stack.substringBeforeLast('|', route.destination.startRoute().name)
        preview = PreviewState.READY.name
    }
    BackHandler(stack.contains('|') || directory) { if (directory) directory = false else back() }
    val holder = rememberSaveableStateHolder()
    LaunchedEffect(s.notice) {
        s.notice?.let {
            snackbar.showSnackbar(it, withDismissAction = true)
            send(DemoAction.Notice(null))
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rail = maxWidth >= 600.dp
        val expanded = maxWidth >= 1000.dp
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides if (demo) 32.dp else 48.dp
        ) {
            Scaffold(
                topBar = {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Row(
                            Modifier.fillMaxWidth()
                                .statusBarsPadding()
                                .heightIn(min = SynapseDesignTokens.topBarHeight)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (stack.contains('|'))
                                IconButton(back, Modifier.size(40.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            Image(
                                painterResource(R.drawable.synapse_brand_mark),
                                "Synapse",
                                Modifier.size(30.dp),
                            )
                            Column(Modifier.weight(1f).padding(start = 7.dp)) {
                                Text("Synapse", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (demo) "Demo data · simulated actions"
                                    else "Your AI-powered device operator",
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                onClick = { demo = !demo; preview = PreviewState.READY.name },
                                color = if (demo) Color(0xFFEAF8F1) else MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                            ) {
                                Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(5.dp).background(if (demo) Color(0xFF00A85B) else MaterialTheme.colorScheme.primary, CircleShape))
                                    Text(if (demo) "Go live" else "Try demo", fontSize = 9.sp, lineHeight = 12.sp)
                                }
                            }
                            IconButton({ navigate(if (route.destination == Destination.AGENT) SynapseRoute.ACTIVITY else SynapseRoute.SEARCH) }, Modifier.size(32.dp)) {
                                Icon(if (route.destination == Destination.AGENT) Icons.Default.NotificationsNone else Icons.Default.Search,
                                    if (route.destination == Destination.AGENT) "Notifications" else "Search", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box {
                                IconButton({ states = true }, Modifier.size(30.dp)) {
                                    Surface(color = MaterialTheme.colorScheme.outlineVariant, shape = CircleShape) {
                                        Icon(Icons.Default.Person, "Workspace menu", Modifier.padding(3.dp).size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                DropdownMenu(states, { states = false }) {
                                    DropdownMenuItem(
                                        { Text("All screens") },
                                        {
                                            directory = !directory
                                            states = false
                                        },
                                    )
                                    if (demo)
                                        PreviewState.entries.forEach { state ->
                                            DropdownMenuItem(
                                                { Text("${state.name.lowercase()} preview") },
                                                {
                                                    preview = state.name
                                                    states = false
                                                },
                                            )
                                        }
                                }
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (
                        !rail &&
                            route !in
                                listOf(
                                    SynapseRoute.WELCOME,
                                    SynapseRoute.AUDIT,
                                    SynapseRoute.PERMISSIONS,
                                )
                    )
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .navigationBarsPadding()
                                    .heightIn(min = SynapseDesignTokens.navigationHeight)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Destination.entries.forEach { destination ->
                                    val selected = route.destination == destination
                                    Column(
                                        Modifier.weight(1f)
                                            .background(
                                                if (selected)
                                                    MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent,
                                                RoundedCornerShape(12.dp),
                                            )
                                            .selectable(selected, role = Role.Tab) {
                                                stack = destination.startRoute().name
                                                directory = false
                                                preview = PreviewState.READY.name
                                            }
                                            .heightIn(min = 46.dp)
                                            .padding(vertical = 5.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        AssetIcon(
                                            destination.assetIcon(),
                                            tint =
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                            size = 20.dp,
                                        )
                                        Text(
                                            destination.label,
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp,
                                            color =
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                },
            ) { padding ->
                Row(
                    Modifier.fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .imePadding()
                ) {
                    if (rail)
                        NavigationRail {
                            Destination.entries.forEach { destination ->
                                NavigationRailItem(
                                    route.destination == destination,
                                    {
                                        stack = destination.startRoute().name
                                        directory = false
                                        preview = PreviewState.READY.name
                                    },
                                    icon = {
                                        AssetIcon(
                                            destination.assetIcon(),
                                            tint =
                                                if (route.destination == destination)
                                                    MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    label = { Text(destination.label, fontSize = 10.sp) },
                                )
                            }
                        }
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.weight(1f)
                                .widthIn(max = 840.dp)
                                .fillMaxWidth()
                                .testTag("route-${route.name}")
                        ) {
                            if (directory) ScreenDirectory(route, navigate)
                            else
                                holder.SaveableStateProvider(
                                    "${if (demo) "demo_$resetEpoch" else "live"}_${route.name}"
                                ) {
                                    if (demo)
                                        PreviewPane(
                                            route,
                                            PreviewState.valueOf(preview),
                                            { preview = PreviewState.READY.name },
                                        ) {
                                            DemoRouteContent(
                                                route,
                                                s,
                                                send,
                                                navigate,
                                                appearance,
                                                onAppearance,
                                                reducedMotion,
                                                onReducedMotion,
                                                {
                                                    reset()
                                                    resetEpoch++
                                                },
                                            )
                                        }
                                    else
                                        liveContent(route, navigate) {
                                            demo = true
                                            preview = PreviewState.READY.name
                                        }
                                }
                        }
                    }
                    if (expanded)
                        Column(
                            Modifier.width(260.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text("Workspace context", style = MaterialTheme.typography.titleMedium)
                            Notice(
                                if (demo) "Simulation active" else "Live workspace",
                                if (demo)
                                    "Sample actions update only the demo scenario. No commands, uploads or file changes occur."
                                else
                                    "Live actions use your selected folders, provider and existing safety checks.",
                            )
                            if (demo) {
                                LabeledValue("Selected task", s.task.id)
                                TaskProgress(s.task)
                                Secondary("Open monitor") { navigate(SynapseRoute.MONITOR) }
                            }
                            Secondary("Browse all screens") { directory = true }
                        }
                }
            }
        }
    }
}

@Composable
internal fun DemoRouteContent(
    route: SynapseRoute,
    s: DemoState,
    send: (DemoAction) -> Unit,
    nav: (SynapseRoute) -> Unit,
    appearance: String = "System",
    onAppearance: (String) -> Unit = {},
    reducedMotion: Boolean = false,
    onReducedMotion: (Boolean) -> Unit = {},
    reset: () -> Unit = {},
) {
    when (route.number) {
        31 -> TaskResultDesign(s, nav)
        in 1..11 -> DemoAgentRoute(route, s, send, nav)
        in 12..16,
        21,
        22,
        23,
        27,
        28,
        29,
        30 ->
            DemoSettingsRoute(
                route,
                s,
                send,
                nav,
                appearance,
                onAppearance,
                reducedMotion,
                onReducedMotion,
                reset,
            )
        else -> DemoDeviceRoute(route, s, send, nav)
    }
}

@Composable
private fun ScreenDirectory(current: SynapseRoute, nav: (SynapseRoute) -> Unit) {
    ScreenPage(
        "WORKSPACE DIRECTORY",
        "Every screen, within reach",
        "Choose a destination. Try demo to explore fully populated sample workflows.",
    ) {
        Destination.entries.forEach { destination ->
            Section(destination.label) {
                (referenceRoutes + SynapseRoute.entries.filter { it !in referenceRoutes })
                    .filter { it.destination == destination }
                    .forEach { route ->
                        LinkRow(
                            if (route in referenceRoutes)
                                "${(referenceRoutes.indexOf(route) + 1).toString().padStart(2, '0')} · ${route.title}"
                            else route.title,
                            if (route == current) "Current screen" else destination.label,
                            destination.icon(),
                        ) {
                            nav(route)
                        }
                    }
            }
        }
    }
}

internal fun Destination.assetIcon(): SynapseIcon =
    when (this) {
        Destination.AGENT -> SynapseIcon.AGENT
        Destination.ACTIVITY -> SynapseIcon.ACTIVITY
        Destination.DEVICE -> SynapseIcon.DEVICE
        Destination.SETTINGS -> SynapseIcon.SETTINGS
    }

/** PDF page order is independent of the stable legacy route number. */
internal val referenceRoutes =
    listOf(
        SynapseRoute.WELCOME,
        SynapseRoute.AGENT,
        SynapseRoute.ATTACHMENTS,
        SynapseRoute.PLAN,
        SynapseRoute.APPROVAL,
        SynapseRoute.MONITOR,
        SynapseRoute.RECOVERY,
        SynapseRoute.TASK_RESULT,
        SynapseRoute.CHECKPOINT,
        SynapseRoute.ACTIVITY,
        SynapseRoute.SCHEDULE,
        SynapseRoute.AUDIT,
        SynapseRoute.UNDO,
        SynapseRoute.DEVICE,
        SynapseRoute.SANDBOX,
        SynapseRoute.APPS,
        SynapseRoute.TERMINAL,
        SynapseRoute.TOOLS,
        SynapseRoute.PACKAGES,
        SynapseRoute.PACKAGE_DETAIL,
        SynapseRoute.SETTINGS,
        SynapseRoute.PROVIDERS,
        SynapseRoute.CREDENTIALS,
        SynapseRoute.MODELS,
        SynapseRoute.PRIVACY,
        SynapseRoute.USAGE,
        SynapseRoute.SECURITY,
        SynapseRoute.MEMORY,
        SynapseRoute.PERMISSIONS,
        SynapseRoute.APPEARANCE,
        SynapseRoute.ABOUT,
    )
