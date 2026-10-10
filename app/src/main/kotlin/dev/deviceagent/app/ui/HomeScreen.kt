package dev.deviceagent.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.deviceagent.app.AgentViewModel
import dev.deviceagent.app.HistoryItem
import dev.deviceagent.app.Tone
import dev.deviceagent.app.UiState
import dev.deviceagent.app.cleanProgress
import dev.deviceagent.app.friendlyReport
import dev.deviceagent.app.formatAssistantNote
import dev.deviceagent.app.providerName
import dev.deviceagent.app.providerProblem
import dev.deviceagent.app.setupGaps
import dev.deviceagent.app.statusExplanation
import dev.deviceagent.app.statusTitle
import dev.deviceagent.app.statusTone

private val EXAMPLES = listOf(
    "Move all PDFs from Download into Documents/PDFs",
    "Show me the 20 biggest files in my folders",
    "Zip everything in Pictures/Trip into one file",
    "Copy every .mp4 from Download into Music/Lessons",
)

@Composable
internal fun HomeScreen(
    s: UiState,
    vm: AgentViewModel,
    onOpenSettings: () -> Unit,
    onTurnOnNotifications: () -> Unit,
    onStart: () -> Unit,
) {
    var showSchedule by remember { mutableStateOf(false) }
    var showUndo by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    val gaps = setupGaps(s)
    val ready = gaps.isEmpty()
    val ai = providerName(s.provider)

    val idle = !s.running && s.outcome == null
    Column(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Fileora", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp, start = 4.dp)) }

        if (!s.notificationsEnabled) item {
            Banner(
                Tone.WARNING, FileoraIconName.Notifications, "Turn on notifications",
                "I'll tell you when a task needs your OK or is finished — even when the app is closed.",
            ) { Button(onClick = onTurnOnNotifications) { Text("Turn on") } }
        }

        if (!ready) item {
            AppCard {
                CardTitle("Finish setup")
                StepRow(1, s.apiKeyMask != null, "Connect an AI service", "Paste an API key from Google, OpenAI or Anthropic.")
                StepRow(
                    2, s.model.isNotBlank(), "Choose an AI model",
                    if (s.apiKeyMask != null) "Pick one in Settings." else "I'll suggest one as soon as your key is saved.",
                )
                StepRow(
                    3, s.useAllFilesAccess || s.roots.isNotEmpty(), "Choose where I can work",
                    when {
                        s.useAllFilesAccess && s.allFilesAccessGranted -> "Full shared-storage access is allowed. System app folders remain blocked."
                        s.useAllFilesAccess -> "Allow full file access in Android settings to use this mode."
                        else -> "I can only see the folders you choose."
                    },
                )
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Open Settings") }
            }
        }

        if (s.checkpointAvailable && !s.running) item {
            Banner(
                Tone.WARNING, FileoraIconName.Warning, "A task was interrupted",
                "It stopped before it finished (the app closed or the phone restarted)." +
                    if (s.checkpointDescription.isNotBlank()) "\nLast step: ${cleanProgress(s.checkpointDescription)}" else "",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::resumeTask) { Text("Continue") }
                    OutlinedButton(onClick = { showDiscard = true }) { Text("Discard") }
                }
            }
        }

        if (s.running) item { RunningCard(s, vm) }

        if (idle && s.goal.isBlank()) {
            item { Hero(Modifier.fillParentMaxHeight(0.38f)) }
            items(EXAMPLES) { example -> SuggestionRow(example) { vm.setGoal(example) } }
        }

        if (!s.running) s.outcome?.let { o ->
            item {
                val tone = statusTone(o.status)
                Banner(
                    tone,
                    if (tone == Tone.SUCCESS) FileoraIconName.Success else if (tone == Tone.INFO) FileoraIconName.AiAssistant else FileoraIconName.Warning,
                    statusTitle(o.status),
                    statusExplanation(o.status),
                ) {
                    if (o.status == dev.deviceagent.core.agent.TaskStatus.BLOCKED && o.stoppedReason != null) {
                        Text("I stopped before finishing because this task reached its safety limit. Changes already made can still be undone.", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (o.unverifiedChanges > 0) Text("${o.unverifiedChanges} change(s) couldn't be double-checked automatically.", style = MaterialTheme.typography.bodyMedium)
                    if (o.providerError != null) Text(providerProblem(o.providerError), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (o.canUndo || s.restorableUndoTaskId != null) OutlinedButton(onClick = { showUndo = true }) { Text("Undo changes") }
                        Button(onClick = vm::clearOutcome) { Text("Done") }
                    }
                    val report = friendlyReport(o.report)
                    if (report.isNotBlank() || o.modelMessage != null || s.usageSummary.isNotEmpty()) {
                        Expandable("View summary") {
                            if (report.isNotBlank()) Text(report, style = MaterialTheme.typography.bodySmall)
                            o.modelMessage?.takeIf { it.isNotBlank() }?.let {
                                val note = formatAssistantNote(it)
                                if (note.isNotBlank()) {
                                    Text("AI summary", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Text(note, maxLines = 14, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (s.usageSummary.isNotEmpty()) Text(s.usageSummary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (!s.running && s.outcome == null) {
            s.mostRecentTask?.takeIf { it.status != null && it.taskId != s.dismissedRecentTaskId }?.let { recent ->
                item { RecentTaskCard(recent) { showUndo = true } }
            }
        }
    }
    if (!s.running) Composer(s, vm, ready, ai, onOpenSettings, onStart) { showSchedule = true }
    }

    if (showSchedule) ScheduleDialog(
        goal = s.goal,
        onSchedule = { hours -> showSchedule = false; vm.scheduleCurrent(hours) },
        onDismiss = { showSchedule = false },
    )
    if (showUndo) ConfirmDialog(
        title = "Undo this task?",
        text = "I'll put your files back the way they were before this task. Anything that can't be put back safely is skipped.",
        confirmLabel = "Undo",
        onConfirm = { showUndo = false; vm.undoLastTask() },
        onDismiss = { showUndo = false },
    )
    if (showDiscard) ConfirmDialog(
        title = "Discard the interrupted task?",
        text = "I'll forget it. Changes already made stay as they are.",
        confirmLabel = "Discard",
        onConfirm = { showDiscard = false; vm.discardCheckpoint() },
        onDismiss = { showDiscard = false },
    )
}

@Composable
private fun RecentTaskCard(task: HistoryItem, onUndo: () -> Unit) {
    val status = task.status ?: return
    val tone = statusTone(status)
    Banner(
        tone,
        if (tone == Tone.SUCCESS) FileoraIconName.Success else if (tone == Tone.INFO) FileoraIconName.AiAssistant else FileoraIconName.Warning,
        "Your last task: ${statusTitle(status)}",
        "${task.goal}\n${statusExplanation(status)}",
    ) {
        if (task.steps.isNotEmpty()) {
            Expandable("Show what happened") {
                task.steps.take(40).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (task.canUndo) OutlinedButton(onClick = onUndo) { Text("Undo changes") }
    }
}

@Composable
private fun Hero(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        FileoraIcon(FileoraIconName.AiSparkle, contentDescription = null, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text("What should I do with your files?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("I'll show you the plan and ask before I change anything.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SuggestionRow(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(text, Modifier.padding(horizontal = 16.dp, vertical = 14.dp), style = MaterialTheme.typography.bodyMedium) }
}

/** The ChatGPT/Claude-style input pinned to the bottom: write the request, pick preview-only, send. */
@Composable
private fun Composer(
    s: UiState,
    vm: AgentViewModel,
    ready: Boolean,
    ai: String,
    onOpenSettings: () -> Unit,
    onStart: () -> Unit,
    onSchedule: () -> Unit,
) {
    val canSend = s.goal.isNotBlank() && ready
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 10.dp)) {
                TextField(
                    value = s.goal,
                    onValueChange = vm::setGoal,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 6,
                    placeholder = { Text("Ask Fileora to organize your files…") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )
                Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = s.dryRun,
                        onClick = { vm.setDryRun(!s.dryRun) },
                        label = { Text("Preview only") },
                        shape = CircleShape,
                    )
                    TextButton(onClick = onSchedule, enabled = canSend) { Text("Schedule") }
                    Spacer(Modifier.weight(1f))
                    SendButton(enabled = canSend, onClick = onStart)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpenSettings).padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileoraIcon(FileoraIconName.PrivacyLock, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    !ready -> "Finish setup to start · tap to open Settings"
                    s.manualMode -> "Suggest-only mode: I'll list changes but never make them."
                    s.sendFileContents -> "$ai can read the contents of files it needs."
                    else -> "$ai only sees file and folder names."
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val fg = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(onClick = onClick, enabled = enabled, shape = CircleShape, color = bg, modifier = Modifier.size(40.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp).semanticsLabel("Send")) {
                val w = size.width
                val h = size.height
                val sw = 2.2.dp.toPx()
                drawLine(fg, Offset(w / 2, h * 0.92f), Offset(w / 2, h * 0.08f), sw, StrokeCap.Round)
                drawLine(fg, Offset(w * 0.12f, h * 0.45f), Offset(w / 2, h * 0.05f), sw, StrokeCap.Round)
                drawLine(fg, Offset(w * 0.88f, h * 0.45f), Offset(w / 2, h * 0.05f), sw, StrokeCap.Round)
            }
        }
    }
}

private fun Modifier.semanticsLabel(label: String): Modifier =
    this.then(Modifier.semantics { contentDescription = label })

@Composable
private fun RunningCard(s: UiState, vm: AgentViewModel) {
    val waiting = s.pendingConfirmation != null || s.pendingQuestion != null
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(12.dp))
            Text(if (waiting) "Waiting for your answer…" else "Working on it…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (!waiting) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (s.goal.isNotBlank()) Text(s.goal, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        s.log.lastOrNull()?.let { Hint(cleanProgress(it)) }
        Hint("You can leave the app. I'll send a notification when I need you or when I'm done.")
        OutlinedButton(onClick = vm::cancelTask) { Text("Stop") }
        if (s.log.isNotEmpty()) {
            Expandable("Show activity") {
                s.log.takeLast(40).forEach { Text(cleanProgress(it), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
