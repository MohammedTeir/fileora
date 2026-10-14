package dev.deviceagent.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.deviceagent.app.Tone
import dev.deviceagent.app.approvalTitle
import dev.deviceagent.app.formatBytes
import dev.deviceagent.app.friendlyTool
import dev.deviceagent.app.intervalLabel
import dev.deviceagent.app.riskLabel
import dev.deviceagent.app.riskTone
import dev.deviceagent.core.policy.ConfirmationRequest
import dev.deviceagent.core.policy.ConfirmationResponse

private val NoDismiss = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)

/**
 * "Can I do this?" — shown on top of every screen. Big, stacked buttons (the stock dialog squeezes
 * four choices into one row), and it can't be dismissed by an accidental tap outside.
 */
@Composable
internal fun ApprovalDialog(req: ConfirmationRequest, onAnswer: (ConfirmationResponse) -> Unit) {
    val tone = riskTone(req)
    val (bg, fg) = toneColors(tone)
    Dialog(onDismissRequest = {}, properties = NoDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) {
            Column(
                Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(approvalTitle(req.tool), style = MaterialTheme.typography.headlineSmall)

                Surface(shape = RoundedCornerShape(50), color = bg) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SynapseIcon(
                            if (tone == Tone.INFO) SynapseIconName.Permissions else SynapseIconName.Warning,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(riskLabel(req.risk), style = MaterialTheme.typography.labelLarge, color = fg)
                    }
                }

                Text(req.summary, style = MaterialTheme.typography.bodyLarge)

                if (req.fileCount > 0) {
                    Text(
                        "${req.fileCount} file(s) · ${formatBytes(req.bytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                val shown = req.affectedSample.take(5)
                if (shown.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        shown.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val more = req.fileCount - shown.size
                        if (more > 0) Hint("…and $more more")
                    }
                }

                Text(
                    if (req.reversible) "You can undo this later from the Activity tab." else "This can't be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (req.reversible) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    fontWeight = if (req.reversible) FontWeight.Normal else FontWeight.SemiBold,
                )

                Button(onClick = { onAnswer(ConfirmationResponse.ALLOW_ONCE) }, modifier = Modifier.fillMaxWidth()) { Text("Allow") }
                OutlinedButton(onClick = { onAnswer(ConfirmationResponse.DENY) }, modifier = Modifier.fillMaxWidth()) { Text("Don't allow") }
                if (req.grantScope != null) {
                    TextButton(onClick = { onAnswer(ConfirmationResponse.ALLOW_FOR_TASK) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Always allow me to ${friendlyTool(req.grantScope!!)} in this task")
                    }
                }
                TextButton(
                    onClick = { onAnswer(ConfirmationResponse.CANCEL_TASK) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Stop the whole task") }
            }
        }
    }
}

/** The agent needs a decision from the user ("Which folder should I use?"). */
@Composable
internal fun QuestionDialog(question: String, onAnswer: (String?) -> Unit) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = {}, properties = NoDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("I have a question", style = MaterialTheme.typography.headlineSmall)
                Text(question, style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your answer") },
                    minLines = 2,
                )
                Button(onClick = { onAnswer(text) }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Send answer") }
                TextButton(onClick = { onAnswer(null) }, modifier = Modifier.fillMaxWidth()) { Text("Skip — I'd rather not answer") }
            }
        }
    }
}

@Composable
internal fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Pick how often a task repeats — no typing hours. */
@Composable
internal fun ScheduleDialog(goal: String, onSchedule: (Long) -> Unit, onDismiss: () -> Unit) {
    var hours by remember { mutableLongStateOf(24L) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repeat this task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(goal, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                listOf(1L, 6L, 24L, 168L).forEach { h ->
                    ChoiceRow(selected = hours == h, title = intervalLabel(h), subtitle = null) { hours = h }
                }
                Hint("Repeating tasks only prepare suggestions. They never change your files on their own.")
            }
        },
        confirmButton = { TextButton(onClick = { onSchedule(hours) }) { Text("Repeat") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
