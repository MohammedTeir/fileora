package dev.deviceagent.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.deviceagent.core.policy.ConfirmationRequest
import dev.deviceagent.core.policy.ConfirmationResponse

class MainActivity : ComponentActivity() {
    private val vm: AgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Screen(vm) } }
    }
}

@Composable
private fun Screen(vm: AgentViewModel) {
    val s by vm.state.collectAsState()
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) vm.addRoot(uri) }
    var keyInput by remember { mutableStateOf("") }
    var modelInput by remember(s.model) { mutableStateOf(s.model) }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Device Agent", style = MaterialTheme.typography.headlineMedium)

        s.notice?.let { msg ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(msg, Modifier.weight(1f))
                    TextButton(onClick = vm::dismissNotice) { Text("OK") }
                }
            }
        }

        // ── 1. AI provider ──
        Section("1. AI provider (Gemini)") {
            if (s.apiKeyMask != null) {
                Text("API key: ${s.apiKeyMask}")
                OutlinedButton(onClick = vm::removeApiKey) { Text("Remove key") }
            } else {
                OutlinedTextField(
                    value = keyInput, onValueChange = { keyInput = it }, label = { Text("Gemini API key") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { vm.saveApiKey(keyInput); keyInput = "" }, enabled = keyInput.isNotBlank()) { Text("Save key") }
            }
            OutlinedTextField(
                value = modelInput, onValueChange = { modelInput = it; vm.setModel(it) },
                label = { Text("Model") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.refreshModels() }, enabled = s.apiKeyMask != null) { Text("List models") }
                OutlinedButton(onClick = { vm.testConnection() }, enabled = s.apiKeyMask != null) { Text("Test connection") }
            }
            s.availableModels.take(30).forEach { id ->
                TextButton(onClick = { vm.setModel(id) }) { Text(id, fontSize = 13.sp) }
            }
        }

        // ── 2. Folders ──
        Section("2. Folders the agent may use") {
            Text("The agent can only see folders you add here. It cannot access anything else.", style = MaterialTheme.typography.bodySmall)
            s.roots.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("/${r.name}", Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                    TextButton(onClick = { vm.removeRoot(r.name) }) { Text("Remove") }
                }
            }
            Button(onClick = { pickFolder.launch(null) }) { Text("Add folder") }
        }

        // ── 3. Task ──
        Section("3. What should I do?") {
            OutlinedTextField(
                value = s.goal, onValueChange = vm::setGoal, modifier = Modifier.fillMaxWidth(), minLines = 3,
                label = { Text("e.g. Copy every .mp4 from Download into Music/Lessons") },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = s.dryRun, onCheckedChange = vm::setDryRun)
                Text("  Dry run: show the plan, change nothing")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = s.manualMode, onCheckedChange = vm::setManualMode)
                Text("  Manual mode: propose only, never execute")
            }
            Button(onClick = vm::runTask, enabled = !s.running && s.goal.isNotBlank()) { Text(if (s.running) "Working…" else "Run") }
        }

        // ── 4. Progress / result ──
        if (s.log.isNotEmpty()) {
            Section("Activity") { s.log.takeLast(40).forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp) } }
        }
        s.outcome?.let { o ->
            Section("Result: ${statusLabel(o.status)}") {
                // The engine's own facts come first; the model's words are secondary.
                Text(o.report, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                o.modelMessage?.let { Text("\nAssistant says:\n$it") }
                if (o.unverifiedChanges > 0) Text("\n${o.unverifiedChanges} change(s) could not be verified automatically.")
                if (s.usageSummary.isNotEmpty()) Text("\n${s.usageSummary}", style = MaterialTheme.typography.bodySmall)
                if (o.canUndo || s.restorableUndoTaskId != null) OutlinedButton(onClick = vm::undoLastTask) { Text("Undo this task") }
            }
        }
        if (s.history.isNotEmpty()) {
            Section("History") {
                s.history.take(20).forEach { entry ->
                    Text(
                        "${entry.kind} · ${entry.taskId}\n${entry.details}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }

    s.pendingConfirmation?.let { ConfirmDialog(it, vm::answerConfirmation) }
    s.pendingQuestion?.let { q -> QuestionDialog(q, vm::answerQuestion) }
}

/** Plain-language label; PLAN_ONLY must read as "nothing was changed", never as a success. */
private fun statusLabel(status: dev.deviceagent.core.agent.TaskStatus): String = when (status) {
    dev.deviceagent.core.agent.TaskStatus.PLAN_ONLY -> "PLAN ONLY (nothing was changed)"
    else -> status.name
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ConfirmDialog(req: ConfirmationRequest, onAnswer: (ConfirmationResponse) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAnswer(ConfirmationResponse.DENY) },
        title = { Text("Allow this? (${req.risk.name.lowercase()} risk)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(req.summary)
                if (req.fileCount > 0) Text("${req.fileCount} file(s), ${req.bytes} bytes")
                req.affectedSample.take(5).forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                Text(if (req.reversible) "This can be undone." else "This cannot be undone.", style = MaterialTheme.typography.bodySmall)
                if (req.grantScope != null) TextButton(onClick = { onAnswer(ConfirmationResponse.ALLOW_FOR_TASK) }) { Text("Always allow ${req.grantScope} for this task") }
                TextButton(onClick = { onAnswer(ConfirmationResponse.CANCEL_TASK) }) { Text("Cancel the whole task") }
            }
        },
        confirmButton = { TextButton(onClick = { onAnswer(ConfirmationResponse.ALLOW_ONCE) }) { Text("Allow once") } },
        dismissButton = { TextButton(onClick = { onAnswer(ConfirmationResponse.DENY) }) { Text("Deny") } },
    )
}

@Composable
private fun QuestionDialog(question: String, onAnswer: (String?) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onAnswer(null) },
        title = { Text("The agent has a question") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(question)
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onAnswer(text) }) { Text("Answer") } },
        dismissButton = { TextButton(onClick = { onAnswer(null) }) { Text("Skip") } },
    )
}
