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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.deviceagent.app.ui.ActivityScreen
import dev.deviceagent.app.ui.AgentTheme
import dev.deviceagent.app.ui.ApprovalDialog
import dev.deviceagent.app.ui.HomeScreen
import dev.deviceagent.app.ui.FileoraIcon
import dev.deviceagent.app.ui.FileoraIconName
import dev.deviceagent.app.ui.QuestionDialog
import dev.deviceagent.app.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    private val vm: AgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AgentTheme { AppRoot(vm) } }
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

private const val TAB_HOME = 0
private const val TAB_ACTIVITY = 1
private const val TAB_SETTINGS = 2

@Composable
private fun AppRoot(vm: AgentViewModel) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    var startAfterPermission by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    // Messages appear at the bottom of whatever screen you're on (they used to be a card at the very top of a long page).
    LaunchedEffect(s.notice) {
        s.notice?.let {
            snackbar.showSnackbar(it, withDismissAction = true, duration = SnackbarDuration.Long)
            vm.dismissNotice()
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
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

    // The first time someone starts a task, ask for notification permission so they hear back when they leave the app.
    val start: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 && !s.notificationsEnabled && !vm.notificationsAsked()) {
            vm.markNotificationsAsked()
            startAfterPermission = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.runTask()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == TAB_HOME, onClick = { tab = TAB_HOME },
                    icon = { FileoraIcon(FileoraIconName.Home, contentDescription = null) }, label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = tab == TAB_ACTIVITY, onClick = { tab = TAB_ACTIVITY },
                    icon = { FileoraIcon(FileoraIconName.History, contentDescription = null) }, label = { Text("Activity") },
                )
                NavigationBarItem(
                    selected = tab == TAB_SETTINGS, onClick = { tab = TAB_SETTINGS },
                    icon = { FileoraIcon(FileoraIconName.Settings, contentDescription = null) }, label = { Text("Settings") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding).imePadding()) {
            when (tab) {
                TAB_HOME -> HomeScreen(
                    s = s, vm = vm,
                    onOpenSettings = { tab = TAB_SETTINGS },
                    onTurnOnNotifications = turnOnNotifications,
                    onStart = start,
                )
                TAB_ACTIVITY -> ActivityScreen(s, vm)
                else -> SettingsScreen(s, vm)
            }
        }
    }

    // Dialogs sit above every tab, so an approval request is never hidden behind another screen.
    s.pendingConfirmation?.let { ApprovalDialog(it, vm::answerConfirmation) }
    s.pendingQuestion?.let { QuestionDialog(it, vm::answerQuestion) }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
