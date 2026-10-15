package dev.deviceagent.app.ui.synapse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
    liveContent: @Composable (SynapseRoute, (SynapseRoute) -> Unit, () -> Unit) -> Unit,
) {
    var resetEpoch by rememberSaveable { mutableIntStateOf(0) }
    var demo by rememberSaveable { mutableStateOf(false) }
    var stack by rememberSaveable { mutableStateOf(initialRoute.name) }
    var preview by rememberSaveable { mutableStateOf(PreviewState.READY.name) }
    var directory by rememberSaveable { mutableStateOf(false) }
    var states by remember { mutableStateOf(false) }
    val route = SynapseRoute.valueOf(stack.substringAfterLast('|'))
    val navigate: (SynapseRoute) -> Unit = { target ->
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Synapse", style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (demo) "Demo · ${s.provider} / ${s.model}" else modelLabel,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    },
                    navigationIcon = {
                        if (stack.contains('|'))
                            IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    },
                    actions = {
                        TextButton({
                            demo = !demo
                            preview = PreviewState.READY.name
                        }) {
                            Text(if (demo) "Go live" else "Try demo")
                        }
                        IconButton({ directory = !directory }) {
                            Icon(Icons.Default.GridView, "All screens")
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (!rail)
                    NavigationBar {
                        Destination.entries.forEach { destination ->
                            NavigationBarItem(
                                route.destination == destination,
                                {
                                    stack = destination.startRoute().name
                                    directory = false
                                    preview = PreviewState.READY.name
                                },
                                icon = { Icon(destination.icon(), null) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
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
                                icon = { Icon(destination.icon(), null) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (demo)
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Demo data · simulated actions",
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Box {
                                    TextButton({ states = true }) { Text("States") }
                                    DropdownMenu(states, { states = false }) {
                                        PreviewState.entries.forEach { state ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        state.name
                                                            .lowercase()
                                                            .replaceFirstChar(Char::uppercase)
                                                    )
                                                },
                                                onClick = {
                                                    preview = state.name
                                                    states = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    Box(Modifier.weight(1f).widthIn(max = 840.dp).fillMaxWidth()) {
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
                SynapseRoute.entries
                    .filter { it.destination == destination }
                    .forEach { route ->
                        LinkRow(
                            if (route.number > 0)
                                "${route.number.toString().padStart(2, '0')} · ${route.title}"
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
