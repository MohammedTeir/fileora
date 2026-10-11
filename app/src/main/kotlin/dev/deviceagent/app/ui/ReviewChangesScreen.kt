package dev.deviceagent.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.deviceagent.app.formatBytes
import dev.deviceagent.core.policy.ConfirmationRequest
import dev.deviceagent.core.policy.ConfirmationResponse

private val FullScreen = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)

/**
 * "Review changes": shown instead of the small approval dialog when a batch touches several files and the
 * person can approve them one by one. Everything starts ticked; whatever stays unticked is left where it is.
 * The agent still plans, verifies and records undo for exactly the files that were approved.
 */
@Composable
internal fun ReviewChangesDialog(req: ConfirmationRequest, onAnswer: (ConfirmationResponse, Set<String>?) -> Unit) {
    var checked by remember(req) { mutableStateOf(req.items.map { it.id }.toSet()) }
    val total = req.items.size
    val chosen = req.items.filter { it.id in checked }
    val chosenBytes = chosen.sumOf { it.bytes }

    Dialog(onDismissRequest = {}, properties = FullScreen) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Review changes", style = MaterialTheme.typography.headlineSmall)
                    Text(req.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (req.reversible) "You can undo this later from Tasks." else "This can't be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (req.reversible) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    )
                }

                Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${chosen.size} of $total selected · ${formatBytes(chosenBytes)}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { checked = if (chosen.size == total) emptySet() else req.items.map { it.id }.toSet() }) {
                        Text(if (chosen.size == total) "Select none" else "Select all")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    items(req.items, key = { it.id }) { item ->
                        val on = item.id in checked
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { checked = if (on) checked - item.id else checked + item.id }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(checked = on, onCheckedChange = null)
                            Column(Modifier.weight(1f)) {
                                Text(item.label.substringAfterLast('/'), style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Hint("From  " + parentLabel(item.label))
                                item.detail?.let { Hint("To  " + parentLabel(it)) }
                            }
                            Text(formatBytes(item.bytes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (chosen.size in 1 until total) Hint("The other ${total - chosen.size} file(s) will stay where they are.")
                        Button(
                            onClick = { onAnswer(ConfirmationResponse.ALLOW_ONCE, checked) },
                            enabled = chosen.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (chosen.size == total) "Approve all $total files" else "Approve ${chosen.size} of $total files") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { onAnswer(ConfirmationResponse.DENY, null) }, modifier = Modifier.weight(1f)) { Text("Reject all") }
                            TextButton(
                                onClick = { onAnswer(ConfirmationResponse.CANCEL_TASK, null) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) { Text("Stop the task") }
                        }
                    }
                }
            }
        }
    }
}
