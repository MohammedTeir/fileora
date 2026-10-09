package dev.deviceagent.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import dev.deviceagent.app.AgentViewModel
import dev.deviceagent.app.HistoryItem
import dev.deviceagent.app.ScheduledTask
import dev.deviceagent.app.UiState
import dev.deviceagent.app.cleanProgress
import dev.deviceagent.app.formatAssistantNote
import dev.deviceagent.app.formatWhen
import dev.deviceagent.app.friendlyReport
import dev.deviceagent.app.intervalLabel
import dev.deviceagent.app.statusExplanation
import dev.deviceagent.app.statusTitle
import dev.deviceagent.app.statusTone

@Composable
internal fun ActivityScreen(s: UiState, vm: AgentViewModel) {
    var undoTarget by remember { mutableStateOf<String?>(null) }
    var stopTarget by remember { mutableStateOf<ScheduledTask?>(null) }
    var detailsTarget by remember { mutableStateOf<HistoryItem?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle("Activity", "What I've done for you, and what repeats.") }

        if (s.scheduledTasks.isNotEmpty()) {
            item { CardTitle("Repeating tasks") }
            items(s.scheduledTasks, key = { it.id }) { task ->
                AppCard {
                    Text(task.goal, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Hint("${intervalLabel(task.intervalHours)} · suggests changes, never makes them")
                    TextButton(onClick = { stopTarget = task }) { Text("Stop repeating") }
                }
            }
        }

        item { CardTitle("History") }
        if (s.history.isEmpty()) {
            item {
                AppCard {
                    Text("Nothing here yet", style = MaterialTheme.typography.titleSmall)
                    Hint("Tasks you run will show up here, so you can see what happened and undo it.")
                }
            }
        } else {
            items(s.history, key = { it.taskId }) { item ->
                HistoryCard(item) { detailsTarget = item }
            }
        }
    }

    undoTarget?.let { id ->
        ConfirmDialog(
            title = "Undo this task?",
            text = "I'll put your files back the way they were before this task. Anything that can't be put back safely is skipped.",
            confirmLabel = "Undo",
            onConfirm = { undoTarget = null; vm.undoTask(id) },
            onDismiss = { undoTarget = null },
        )
    }
    stopTarget?.let { task ->
        ConfirmDialog(
            title = "Stop repeating this task?",
            text = task.goal,
            confirmLabel = "Stop repeating",
            onConfirm = { stopTarget = null; vm.cancelScheduledTask(task.id) },
            onDismiss = { stopTarget = null },
        )
    }
    detailsTarget?.let { item ->
        TaskDetailsDialog(
            item = item,
            onUndo = { detailsTarget = null; undoTarget = item.taskId },
            onDismiss = { detailsTarget = null },
        )
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, onOpen: () -> Unit) {
    val (bg, fg) = item.status?.let { toneColors(statusTone(it)) } ?: (MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface)
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.clickable(onClick = onOpen).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.goal, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Hint(formatWhen(item.startedAtMs))
                }
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Show task details")
            }
            androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(50), color = bg) {
                Text(
                    item.status?.let { statusTitle(it) } ?: "Didn't finish",
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = fg,
                )
            }
            Hint(if (item.canUndo) "Tap to see what happened and undo changes." else "Tap to see what happened.")
        }
    }
}

@Composable
private fun TaskDetailsDialog(item: HistoryItem, onUndo: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Task details", style = MaterialTheme.typography.headlineSmall)
                Text(item.goal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Hint(formatWhen(item.startedAtMs))
                item.status?.let {
                    Text(statusTitle(it), style = MaterialTheme.typography.labelLarge)
                    Text(statusExplanation(it), style = MaterialTheme.typography.bodyMedium)
                }
                if (item.stoppedReason != null) {
                    Text("Why it stopped", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(cleanProgress(item.stoppedReason), style = MaterialTheme.typography.bodyMedium)
                }
                if (item.providerError != null) {
                    Text("AI service", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(item.providerError, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "${item.stepsCount} AI request(s) · ${item.toolCalls} tool action(s) · ${item.failures} problem(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.unverifiedChanges > 0) Text("${item.unverifiedChanges} change(s) could not be checked automatically.", style = MaterialTheme.typography.bodyMedium)
                val report = friendlyReport(item.report)
                if (report.isNotBlank()) {
                    Text("What happened", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(report, style = MaterialTheme.typography.bodyMedium)
                }
                item.modelMessage?.let { note ->
                    val clean = formatAssistantNote(note)
                    if (clean.isNotBlank()) {
                        Text("AI note", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text(clean, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text("Activity", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                if (item.steps.isEmpty()) Hint("No recorded file changes or actions.")
                item.steps.take(100).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                if (item.canUndo) {
                    OutlinedButton(onClick = onUndo, modifier = Modifier.fillMaxWidth()) { Text("Undo changes from this task") }
                } else if (item.status != null) {
                    Hint("There are no reversible changes saved for this task.")
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}
