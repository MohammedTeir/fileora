package dev.deviceagent.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.deviceagent.app.ui.*
import dev.deviceagent.app.ui.synapse.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val vm: AgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot(vm) }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.inForeground = true
    }

    override fun onStop() {
        AppVisibility.inForeground = false
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        vm.refreshAllFilesAccessStatus()
        vm.refreshNotificationStatus()
    }
}

@Composable
private fun AppRoot(vm: AgentViewModel) {
    val workspace: SynapseWorkspaceViewModel = viewModel()
    val appearance by workspace.appearance.collectAsState()
    val reducedMotion by workspace.reducedMotion.collectAsState()
    var splashDone by rememberSaveable { mutableStateOf(false) }
    val firstLaunch = rememberSaveable { !vm.state.value.onboardingDone }
    LaunchedEffect(Unit) {
        delay(if (reducedMotion) 0L else 500L)
        splashDone = true
    }
    AgentTheme(appearance) { if (!splashDone) SplashScreen(firstLaunch) else MainApp(vm) }
}

@Composable
private fun MainApp(vm: AgentViewModel) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val browser: FileBrowserViewModel = viewModel()
    LaunchedEffect(s.roots, s.useAllFilesAccess) { browser.configure(s.roots, s.useAllFilesAccess) }
    var startAfterPermission by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    // Messages appear at the bottom of whatever screen you're on (they used to be a card at the
    // very top of a long page).
    LaunchedEffect(s.notice) {
        s.notice?.let {
            snackbar.showSnackbar(it, withDismissAction = true, duration = SnackbarDuration.Long)
            vm.dismissNotice()
        }
    }

    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            vm.refreshNotificationStatus()
            if (startAfterPermission) {
                startAfterPermission = false
                vm.runTask()
            }
        }

    val turnOnNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 && !vm.notificationsAsked()) {
            vm.markNotificationsAsked()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings(context)
        }
    }

    // The first time someone starts a task, ask for notification permission so they hear back when
    // they leave the app.
    val start: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 && !s.notificationsEnabled && !vm.notificationsAsked()) {
            vm.markNotificationsAsked()
            startAfterPermission = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.runTask()
        }
    }

    val workspace: SynapseWorkspaceViewModel = viewModel()
    val demo by workspace.demo.collectAsState()
    val appearance by workspace.appearance.collectAsState()
    val reducedMotion by workspace.reducedMotion.collectAsState()
    SynapseWorkspace(
        s = demo,
        send = workspace::dispatch,
        initialRoute = if (!s.onboardingDone) SynapseRoute.WELCOME else SynapseRoute.AGENT,
        modelLabel = "${s.provider.name} · ${s.model.ifBlank { "Choose a model" }}",
        snackbar = snackbar,
        appearance = appearance,
        onAppearance = workspace::appearance,
        reducedMotion = reducedMotion,
        onReducedMotion = workspace::reducedMotion,
        reset = workspace::resetDemo,
    ) { route, nav, explore ->
        LiveRouteContent(
            route,
            s,
            vm,
            browser,
            nav,
            explore,
            start,
            turnOnNotifications,
            appearance,
            workspace::appearance,
            reducedMotion,
            workspace::reducedMotion,
        )
    }

    // Dialogs sit above every tab, so an approval request is never hidden behind another screen.
    s.pendingConfirmation?.let { req ->
        if (req.selectable) ReviewChangesDialog(req, vm::answerReview)
        else ApprovalDialog(req, vm::answerConfirmation)
    }
    s.pendingQuestion?.let { QuestionDialog(it, vm::answerQuestion) }
}

internal fun openNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
