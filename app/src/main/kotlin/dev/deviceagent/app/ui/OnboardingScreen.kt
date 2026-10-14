package dev.deviceagent.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import dev.deviceagent.app.AgentViewModel
import dev.deviceagent.app.FolderAccess
import dev.deviceagent.app.NotificationAccess
import dev.deviceagent.app.OnboardingStep
import dev.deviceagent.app.R
import dev.deviceagent.app.Tone
import dev.deviceagent.app.UiState
import dev.deviceagent.app.folderAccess
import dev.deviceagent.app.notificationAccess
import dev.deviceagent.app.openNotificationSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BrandBlue = Color(0xFF1558E6)
private val BrandBlueLight = Color(0xFF2D8CFF)

/**
 * 01. Splash: brand background, logo, name. On a first launch it also says what is happening, because
 * that is the one launch where the person is waiting on a screen they have never seen.
 */
@Composable
internal fun SplashScreen(firstLaunch: Boolean) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(if (shown) 1f else 0f, tween(700), label = "splash")
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BrandBlue, BrandBlueLight))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.alpha(progress).scale(0.9f + 0.1f * progress),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(104.dp).clip(RoundedCornerShape(28.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.fileora_launcher), contentDescription = null, modifier = Modifier.size(80.dp))
            }
            Text("Fileora", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
            Text("Your files, organized by just asking.", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
        }
        if (firstLaunch) {
            Column(
                Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(horizontal = 48.dp, vertical = 40.dp).alpha(progress),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color.White, trackColor = Color.White.copy(alpha = 0.3f))
                Text("Getting things ready for the first time…", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private class WelcomePage(val icon: FileoraIconName, val title: String, val body: String, val bullets: List<String> = emptyList())

private val WELCOME_PAGES = listOf(
    WelcomePage(
        FileoraIconName.AiSparkle,
        "Meet Fileora",
        "Tell your phone what you want in plain words, like “move my PDFs into Documents”, and Fileora plans it, shows you, and only then does it.",
    ),
    WelcomePage(
        FileoraIconName.Folder,
        "What you can do",
        "Everything you need to keep storage tidy.",
        listOf(
            "📁  Browse and search your files by name",
            "✨  Ask the AI to organize and clean up",
            "👀  Review every change before it happens",
            "↩️  Undo a task if you change your mind",
            "🕒  Run repeating tasks on a schedule",
        ),
    ),
    WelcomePage(
        FileoraIconName.PrivacyLock,
        "Private by design",
        "You stay in control of your files.",
        listOf(
            "🔒  Only the folders you choose are visible",
            "🙈  The AI sees file names, not contents, unless you turn that on",
            "✅  Nothing changes without a preview and your OK",
        ),
    ),
)

/**
 * 02 Welcome, 03 Setup and permissions, then the optional AI introduction and restore, and the completed state.
 * Every permission here is optional: someone who says no still gets a working app (Browse and Search need a
 * folder, but nothing forces them to grant it today).
 */
@Composable
internal fun OnboardingFlow(s: UiState, vm: AgentViewModel, onFinish: (openAiSettings: Boolean) -> Unit) {
    var stepName by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME.name) }
    val step = OnboardingStep.valueOf(stepName)
    val go: (OnboardingStep) -> Unit = { stepName = it.name }
    BackHandler(enabled = step != OnboardingStep.WELCOME) { go(step.back()) }

    // The main screen's message bar is not on screen yet, so errors from adding a folder are shown here.
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(s.notice) {
        s.notice?.let {
            snackbar.showSnackbar(it, withDismissAction = true, duration = SnackbarDuration.Long)
            vm.dismissNotice()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(onSkip = { go(OnboardingStep.SETUP) }, onStart = { go(OnboardingStep.SETUP) })
                OnboardingStep.SETUP -> SetupStep(s, vm, onBack = { go(step.back()) }, onNext = { go(step.next()) })
                OnboardingStep.AI -> AiStep(
                    s, vm,
                    onBack = { go(step.back()) },
                    onLater = { go(step.next()) },
                    onSetUpNow = { vm.completeOnboarding(); onFinish(true) },
                )
                OnboardingStep.DONE -> DoneStep(s, onBack = { go(step.back()) }, onFinish = { vm.completeOnboarding(); onFinish(false) })
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).systemBarsPadding())
    }
}

// ───────── 02 Welcome ─────────

@Composable
private fun ColumnScope.WelcomeStep(onSkip: () -> Unit, onStart: () -> Unit) {
    val pager = rememberPagerState(pageCount = { WELCOME_PAGES.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == WELCOME_PAGES.lastIndex
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onSkip) { Text("Skip") }
    }
    HorizontalPager(pager, Modifier.weight(1f)) { i ->
        val p = WELCOME_PAGES[i]
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            IconTile(p.icon, 120)
            Spacer(Modifier.height(28.dp))
            Text(p.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(p.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (p.bullets.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    p.bullets.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                }
            }
        }
    }
    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics { contentDescription = "Page ${pager.currentPage + 1} of ${WELCOME_PAGES.size}" },
        ) {
            repeat(WELCOME_PAGES.size) { i ->
                val on = i == pager.currentPage
                Box(
                    Modifier.size(if (on) 24.dp else 8.dp, 8.dp).clip(CircleShape)
                        .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Button(
            onClick = { if (last) onStart() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(if (last) "Get Started" else "Next") }
    }
}

// ───────── 03 Setup and permissions ─────────

@Composable
private fun ColumnScope.SetupStep(s: UiState, vm: AgentViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    var pickerCancelled by rememberSaveable { mutableStateOf(false) }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) pickerCancelled = true else { pickerCancelled = false; vm.addRoot(uri) }
    }
    val folders = folderAccess(s.roots.size, s.useAllFilesAccess, pickerCancelled)

    // Bumped after the system dialog closes, so the permission state is read again even if "enabled" did not change.
    var recheck by remember { mutableIntStateOf(0) }
    val activity = remember(context) { context.findActivity() }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshNotificationStatus()
        recheck++
    }
    val notifications = remember(s.notificationsEnabled, recheck) {
        notificationAccess(
            enabled = s.notificationsEnabled,
            sdkInt = Build.VERSION.SDK_INT,
            alreadyAsked = vm.notificationsAsked(),
            shouldShowRationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS) } ?: false,
        )
    }
    val requestNotifications = {
        vm.markNotificationsAsked()
        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    StepScaffold(
        onBack = onBack,
        footer = {
            if (folders == FolderAccess.GRANTED) {
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Continue") }
            } else {
                OutlinedButton(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Continue without a folder") }
            }
        },
    ) {
        StepHeader(FileoraIconName.Permissions, "Let's set up access", "Fileora asks for as little as possible. Here is what and why. You can change all of this later in Settings.")

        AppCard {
            CardTitle("📁  Your files")
            Hint("Fileora can only work inside folders you pick. Android shows its own folder picker, and Fileora gets that folder and what's inside it, nothing else. Pick a folder you want tidied, like Download or Documents.")
            when (folders) {
                FolderAccess.GRANTED -> {
                    Banner(
                        Tone.SUCCESS, FileoraIconName.Success,
                        if (s.useAllFilesAccess && s.roots.isEmpty()) "Full file access is on" else "Folder added",
                        if (s.roots.isNotEmpty()) s.roots.joinToString { it.name } else null,
                    )
                    OutlinedButton(onClick = { pickFolder.launch(null) }) { Text("Add another folder") }
                }
                FolderAccess.CANCELLED -> {
                    Banner(
                        Tone.WARNING, FileoraIconName.Warning, "No folder chosen",
                        "Fileora can't see any files until you pick a folder. Nothing was shared. You can also do this later in Settings.",
                    ) {
                        Button(onClick = { pickFolder.launch(null) }) { Text("Choose a folder") }
                    }
                }
                FolderAccess.NONE -> Button(onClick = { pickFolder.launch(null) }, modifier = Modifier.fillMaxWidth()) { Text("Choose a folder") }
            }
            Hint("Need your whole storage? That is a stronger, optional permission in Settings → Folders and access. Starting with one folder is the safer choice.")
        }

        AppCard {
            CardTitle("🔔  Notifications")
            Hint("So you hear back when a task finishes or needs your OK while you are in another app. Fileora never sends ads.")
            when (notifications) {
                NotificationAccess.GRANTED -> Banner(Tone.SUCCESS, FileoraIconName.Success, "Notifications are on")
                NotificationAccess.NOT_ASKED -> Button(onClick = requestNotifications, modifier = Modifier.fillMaxWidth()) { Text("Allow notifications") }
                NotificationAccess.DENIED -> Banner(
                    Tone.WARNING, FileoraIconName.Warning, "Notifications are off",
                    "Without them you won't be told when a task needs your approval. Fileora still works.",
                ) {
                    Button(onClick = requestNotifications) { Text("Try again") }
                }
                NotificationAccess.BLOCKED -> Banner(
                    Tone.WARNING, FileoraIconName.Warning, "Notifications are blocked",
                    "Android won't show the permission prompt again, so this can only be changed in Android Settings.",
                ) {
                    Button(onClick = { openNotificationSettings(context) }) { Text("Open Android settings") }
                }
            }
        }

        AppCard {
            CardTitle("🔒  Your privacy")
            Hint("Only the AI request uses the internet. By default it sees file names and sizes, never what's inside your files. Every change is previewed first, and recent tasks can be undone.")
        }
    }
}

// ───────── Optional: AI introduction and restore ─────────

@Composable
private fun ColumnScope.AiStep(s: UiState, vm: AgentViewModel, onBack: () -> Unit, onLater: () -> Unit, onSetUpNow: () -> Unit) {
    val scope = rememberCoroutineScope()
    var restoreMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { vm.importSettingsFrom(uri) }
            restoreMessage = if (ok) true to "Settings restored. Add your API key to finish." else false to "That doesn't look like a Fileora settings file."
        }
    }
    val hasKey = s.apiKeyMask != null

    StepScaffold(
        onBack = onBack,
        footer = {
            if (hasKey) {
                Button(onClick = onLater, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Continue") }
            } else {
                Button(onClick = onSetUpNow, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Set up AI now") }
                TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) { Text("Maybe later") }
            }
        },
    ) {
        StepHeader(
            FileoraIconName.AiSparkle, "AI is optional",
            "Browsing, search and favorites work without it. To ask in plain words, add your own API key from Gemini, OpenAI or Claude.",
        )
        if (hasKey) Banner(Tone.SUCCESS, FileoraIconName.Success, "AI is already set up", s.model.ifBlank { null })

        AppCard {
            CardTitle("How it stays private")
            Hint("• Your key is kept in this phone's secure storage and never shown in full.")
            Hint("• By default only file names and sizes are sent. File contents are sent only if you turn that on in Settings → Privacy.")
            Hint("• The AI proposes, Fileora checks, and you approve. Nothing is changed behind your back.")
            Hint("• You pay your AI provider directly for what you use. Fileora doesn't add a charge.")
        }

        AppCard {
            CardTitle("Moving from another phone?")
            Hint("Restore your AI choices from a settings file you exported earlier. API keys and folders are never in the file, so you add them again.")
            OutlinedButton(onClick = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Restore from a file") }
            restoreMessage?.let { (ok, text) -> Banner(if (ok) Tone.SUCCESS else Tone.ERROR, if (ok) FileoraIconName.Success else FileoraIconName.Error, text) }
        }
    }
}

// ───────── Setup complete ─────────

@Composable
private fun ColumnScope.DoneStep(s: UiState, onBack: () -> Unit, onFinish: () -> Unit) {
    val hasFolder = s.roots.isNotEmpty() || s.useAllFilesAccess
    val hasKey = s.apiKeyMask != null
    val allDone = hasFolder && s.notificationsEnabled && hasKey
    StepScaffold(
        onBack = onBack,
        footer = { Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Start using Fileora") } },
    ) {
        StepHeader(
            if (allDone) FileoraIconName.Success else FileoraIconName.Folder,
            if (allDone) "You're all set" else "Ready when you are",
            if (allDone) "Everything is on. Have a look around, or ask the AI to tidy something." else "Anything below that is not ticked can be finished later in Settings.",
        )
        AppCard {
            StepRow(1, hasFolder, "A folder to work in", if (hasFolder) "Browse and Search can show your files." else "Add one in Settings → Folders and access.")
            StepRow(2, s.notificationsEnabled, "Notifications", if (s.notificationsEnabled) "You'll hear when a task needs you." else "Turn them on so you don't miss approvals.")
            StepRow(3, hasKey, "AI assistant", if (hasKey) "Ready to take requests." else "Add an API key in Settings → AI service.")
        }
        if (!hasFolder) Banner(Tone.INFO, FileoraIconName.Folder, "No folder yet", "Browse, Search and the AI have nothing to look at until you add one.")
    }
}

// ───────── shared pieces ─────────

@Composable
private fun ColumnScope.StepScaffold(onBack: () -> Unit, footer: @Composable () -> Unit, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        TextButton(onClick = onBack) { Text("‹ Back") }
    }
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { footer() }
}

@Composable
private fun StepHeader(icon: FileoraIconName, title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, 64)
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
    }
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun IconTile(icon: FileoraIconName, size: Int) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 3.5f).dp)).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) { FileoraIcon(icon, contentDescription = null, modifier = Modifier.size((size * 0.6f).dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
