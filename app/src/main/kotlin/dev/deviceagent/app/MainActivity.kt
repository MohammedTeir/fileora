package dev.deviceagent.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.deviceagent.app.ui.ActivityScreen
import dev.deviceagent.app.ui.AgentTheme
import dev.deviceagent.app.ui.ApprovalDialog
import dev.deviceagent.app.ui.DashboardScreen
import dev.deviceagent.app.ui.HomeScreen
import dev.deviceagent.app.ui.FileBrowserScreen
import dev.deviceagent.app.ui.FileoraIcon
import dev.deviceagent.app.ui.FileoraIconName
import dev.deviceagent.app.ui.QuestionDialog
import dev.deviceagent.app.ui.SearchScreen
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
private const val TAB_BROWSE = 1
private const val TAB_SEARCH = 2
private const val TAB_AI = 3
private const val TAB_SETTINGS = 4

@Composable
private fun AppRoot(vm: AgentViewModel) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val browser: FileBrowserViewModel = viewModel()
    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    val recent by browser.recent.collectAsState()
    val favoriteInfos by browser.favoriteInfos.collectAsState()
    LaunchedEffect(tab) { if (tab == TAB_HOME) browser.refreshRecent() }
    var showTasks by rememberSaveable { mutableStateOf(false) }
    val go: (Int) -> Unit = { tab = it; showTasks = false }
    LaunchedEffect(s.roots, s.useAllFilesAccess) { browser.configure(s.roots, s.useAllFilesAccess) }
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
            Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = tab == TAB_HOME, onClick = { go(TAB_HOME) },
                    icon = { FileoraIcon(FileoraIconName.Home, contentDescription = null) }, label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = tab == TAB_BROWSE, onClick = { go(TAB_BROWSE) },
                    icon = { FileoraIcon(FileoraIconName.Folder, contentDescription = null) }, label = { Text("Browse") },
                )
                NavigationBarItem(
                    selected = tab == TAB_SEARCH, onClick = { go(TAB_SEARCH) },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) }, label = { Text("Search") },
                )
                NavigationBarItem(
                    selected = tab == TAB_AI, onClick = { go(TAB_AI) },
                    icon = { FileoraIcon(FileoraIconName.AiSparkle, contentDescription = null) }, label = { Text("AI") },
                )
                NavigationBarItem(
                    selected = tab == TAB_SETTINGS, onClick = { go(TAB_SETTINGS) },
                    icon = { FileoraIcon(FileoraIconName.Settings, contentDescription = null) }, label = { Text("Settings") },
                )
            }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding).imePadding()) {
            if (showTasks) {
                BackHandler { showTasks = false }
                Column(Modifier.fillMaxSize()) {
                    TextButton(onClick = { showTasks = false }, modifier = Modifier.padding(start = 8.dp)) { Text("‹ Back") }
                    Box(Modifier.weight(1f)) { ActivityScreen(s, vm) }
                }
            } else when (tab) {
                TAB_HOME -> DashboardScreen(
                    recent = recent,
                    favorites = favoriteInfos,
                    onReveal = { info -> browser.reveal(info); go(TAB_BROWSE) },
                    onSearch = { go(TAB_SEARCH) },
                    onBrowse = { go(TAB_BROWSE) },
                    onOpenCategory = { c -> browser.updateSearch(query = "", category = c); go(TAB_SEARCH) },
                    onAskAi = { prompt -> prompt?.let(vm::setGoal); go(TAB_AI) },
                    onOpenTasks = { showTasks = true },
                )
                TAB_BROWSE -> FileBrowserScreen(s, browser) { paths ->
                    vm.setGoal("Do this with these items:\n" + paths.joinToString("\n") { "- $it" })
                    go(TAB_AI)
                }
                TAB_SEARCH -> SearchScreen(s, browser) { info ->
                    browser.reveal(info)
                    go(TAB_BROWSE)
                }
                TAB_AI -> HomeScreen(
                    s = s, vm = vm,
                    onOpenSettings = { go(TAB_SETTINGS) },
                    onTurnOnNotifications = turnOnNotifications,
                    onStart = start,
                    onOpenTasks = { showTasks = true },
                )
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
