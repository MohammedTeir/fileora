package dev.deviceagent.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenTitle("Device Agent", "Tell me what to do with your files. I'll ask before I change anything.") }

        if (!s.notificationsEnabled) item {
            Banner(
                Tone.WARNING, Icons.Filled.Notifications, "Turn on notifications",
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
                Tone.WARNING, Icons.Filled.Warning, "A task was interrupted",
                "It stopped before it finished (the app closed or the phone restarted)." +
                    if (s.checkpointDescription.isNotBlank()) "\nLast step: ${cleanProgress(s.checkpointDescription)}" else "",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::resumeTask) { Text("Continue") }
                    OutlinedButton(onClick = { showDiscard = true }) { Text("Discard") }
                }
            }
        }

        if (s.running) item { RunningCard(s, vm) } else item { TaskCard(s, vm, ready, ai, onOpenSettings, onStart) { showSchedule = true } }

        if (!s.running) s.outcome?.let { o ->
            item {
                val tone = statusTone(o.status)
                Banner(
                    tone,
                    if (tone == Tone.SUCCESS) Icons.Filled.CheckCircle else if (tone == Tone.INFO) Icons.Filled.Info else Icons.Filled.Warning,
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
                        Expandable("Show details") {
                            if (report.isNotBlank()) Text(report, style = MaterialTheme.typography.bodySmall)
                            o.modelMessage?.takeIf { it.isNotBlank() }?.let {
                                val note = formatAssistantNote(it)
                                if (note.isNotBlank()) Text("Assistant note\n${note.take(1200)}${if (note.length > 1200) "…" else ""}", style = MaterialTheme.typography.bodySmall)
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
        if (tone == Tone.SUCCESS) Icons.Filled.CheckCircle else if (tone == Tone.INFO) Icons.Filled.Info else Icons.Filled.Warning,
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
private fun TaskCard(
    s: UiState,
    vm: AgentViewModel,
    ready: Boolean,
    ai: String,
    onOpenSettings: () -> Unit,
    onStart: () -> Unit,
    onRepeat: () -> Unit,
) {
    AppCard {
        CardTitle("What should I do?")
        OutlinedTextField(
            value = s.goal,
            onValueChange = vm::setGoal,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            placeholder = { Text("For example: move all PDFs from Download into Documents/PDFs") },
        )
        if (s.goal.isBlank()) {
            Hint("Tap an idea to start:")
            EXAMPLES.forEach { example ->
                Surface(
                    onClick = { vm.setGoal(example) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(example, Modifier.padding(horizontal = 14.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium) }
            }
        }

        Text("How careful should I be?", style = MaterialTheme.typography.labelLarge)
        ChoiceRow(!s.dryRun, "Ask me before changing files", "I'll show what I plan and wait for your OK.") { vm.setDryRun(false) }
        ChoiceRow(s.dryRun, "Preview only", "I'll show a plan. Nothing is changed.") { vm.setDryRun(true) }
        if (s.manualMode) Hint("Suggest-only mode is on (Settings → Advanced): I'll list changes but never make them.")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(
                if (s.sendFileContents) "$ai can read the contents of files it needs." else "$ai only sees file and folder names.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenSettings) { Text("Change") }
        }

        Button(onClick = onStart, enabled = s.goal.isNotBlank() && ready, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (s.dryRun) "Show me the plan" else "Start")
        }
        if (!ready) Hint("Finish the setup above to start.")
        TextButton(onClick = onRepeat, enabled = s.goal.isNotBlank() && ready, modifier = Modifier.fillMaxWidth()) { Text("Repeat this task on a schedule…") }
    }
}

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
