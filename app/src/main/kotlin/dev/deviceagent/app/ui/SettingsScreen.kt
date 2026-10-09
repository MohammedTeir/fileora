package dev.deviceagent.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.deviceagent.app.AgentViewModel
import dev.deviceagent.app.ProviderChoice
import dev.deviceagent.app.RootUi
import dev.deviceagent.app.UiState
import dev.deviceagent.app.providerName
import dev.deviceagent.app.suggestModel

@Composable
internal fun SettingsScreen(s: UiState, vm: AgentViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenTitle("Settings") }
        item { AiServiceCard(s, vm) }
        item { FoldersCard(s, vm) }
        item { PrivacyCard(s, vm) }
        item { AdvancedCard(s, vm) }
        item { AboutCard() }
    }
}

private val KEY_HELP = mapOf(
    ProviderChoice.GEMINI to "Get a key at aistudio.google.com",
    ProviderChoice.OPENAI to "Get a key at platform.openai.com",
    ProviderChoice.CLAUDE to "Get a key at console.anthropic.com",
)

@Composable
private fun AiServiceCard(s: UiState, vm: AgentViewModel) {
    var keyInput by remember { mutableStateOf("") }
    var replacing by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val name = providerName(s.provider)
    val hasKey = s.apiKeyMask != null

    AppCard {
        CardTitle("AI service")
        Hint("This is the AI that plans your tasks. You use your own account and key.")
        ProviderChoice.entries.forEach { choice ->
            ChoiceRow(selected = choice == s.provider, title = providerName(choice), subtitle = null, enabled = !s.running) {
                keyInput = ""; replacing = false
                vm.setProvider(choice)
            }
        }
        HorizontalDivider()

        if (hasKey && !replacing) {
            Text("API key saved: ${s.apiKeyMask}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { replacing = true }) { Text("Replace key") }
                OutlinedButton(onClick = { confirmRemove = true }) { Text("Remove key") }
            }
        } else {
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("$name API key") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Hint(KEY_HELP[s.provider].orEmpty() + ". It's stored securely on this phone.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.saveApiKey(keyInput); keyInput = ""; replacing = false }, enabled = keyInput.isNotBlank()) { Text("Save key") }
                if (replacing) OutlinedButton(onClick = { replacing = false; keyInput = "" }) { Text("Cancel") }
            }
        }

        if (hasKey) {
            HorizontalDivider()
            ModelPicker(
                label = "AI model",
                value = s.model,
                models = s.availableModels,
                suggested = suggestModel(s.provider, s.availableModels),
                placeholder = "Choose a model",
                onPick = vm::setModel,
            )
            if (s.availableModels.isEmpty()) {
                OutlinedTextField(
                    value = s.model, onValueChange = vm::setModel, label = { Text("Model name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Hint("Tap “Load models” to pick from a list instead of typing.")
            }
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.refreshModels() }, enabled = !s.busy) { Text("Load models") }
                OutlinedButton(onClick = { vm.testConnection() }, enabled = !s.busy) { Text("Test connection") }
            }
        }
    }

    if (confirmRemove) ConfirmDialog(
        title = "Remove your $name key?",
        text = "You'll need to paste it again to keep using $name.",
        confirmLabel = "Remove",
        onConfirm = { confirmRemove = false; vm.removeApiKey() },
        onDismiss = { confirmRemove = false },
    )
}

@Composable
private fun ModelPicker(
    label: String,
    value: String,
    models: List<String>,
    suggested: String?,
    placeholder: String,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { open = true }, enabled = models.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(value.ifBlank { placeholder }, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                models.take(80).forEach { id ->
                    DropdownMenuItem(
                        text = { Text(if (id == suggested) "$id  (suggested)" else id) },
                        onClick = { onPick(id); open = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun FoldersCard(s: UiState, vm: AgentViewModel) {
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) vm.addRoot(uri) }
    var removeTarget by remember { mutableStateOf<RootUi?>(null) }

    AppCard {
        CardTitle("Folders I can use")
        Hint(
            when {
                s.useAllFilesAccess && s.allFilesAccessGranted -> "Full file access is on, so I can work anywhere in shared storage. The folders below are kept as a backup."
                s.useAllFilesAccess -> "Full file access is on but Android hasn't granted it. Tasks will wait until you allow it."
                else -> "I can only see the folders you add here. Nothing else on your phone."
            },
        )
        if (s.roots.isEmpty()) Text("No folders yet.", style = MaterialTheme.typography.bodyMedium)
        s.roots.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = { removeTarget = r }) { Icon(Icons.Filled.Delete, contentDescription = "Remove ${r.name}") }
            }
        }
        OutlinedButton(onClick = { pickFolder.launch(null) }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("  Add a folder")
        }

        HorizontalDivider()
        SwitchRow(
            title = "Allow access to all files (advanced)",
            subtitle = if (!s.allFilesAccessSupported) "Not available on this Android version (needs Android 11 or newer)."
            else "Lets me work anywhere in shared storage, not only in the folders above. Android asks you to approve this in system settings. System app folders stay off-limits, and photo/video tools only work with chosen folders.",
            checked = s.useAllFilesAccess,
            enabled = !s.running && s.allFilesAccessSupported,
            onChange = vm::setAllFilesAccessMode,
        )
        if (s.allFilesAccessSupported) {
            Hint(if (s.allFilesAccessGranted) "Android permission: allowed" else "Android permission: not allowed yet")
            if (!s.allFilesAccessGranted) {
                OutlinedButton(onClick = vm::openAllFilesAccessSettings, enabled = !s.running) { Text("Open Android settings") }
            }
        }
    }

    removeTarget?.let { r ->
        ConfirmDialog(
            title = "Remove “${r.name}”?",
            text = "I won't be able to use this folder anymore. Your files are not deleted.",
            confirmLabel = "Remove",
            onConfirm = { removeTarget = null; vm.removeRoot(r.name) },
            onDismiss = { removeTarget = null },
        )
    }
}

@Composable
private fun PrivacyCard(s: UiState, vm: AgentViewModel) {
    val name = providerName(s.provider)
    AppCard {
        CardTitle("Privacy")
        SwitchRow(
            title = "Let $name read file contents",
            subtitle = if (s.sendFileContents) "On: when a task needs it, the text of a file is sent to $name."
            else "Off: $name only sees file and folder names, never what's inside your files.",
            checked = s.sendFileContents,
            onChange = vm::setSendFileContents,
        )
    }
}

@Composable
private fun AdvancedCard(s: UiState, vm: AgentViewModel) {
    var open by remember { mutableStateOf(false) }
    AppCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CardTitle("Advanced")
            Box(Modifier.weight(1f))
            OutlinedButton(onClick = { open = !open }) { Text(if (open) "Hide" else "Show") }
        }
        if (open) {
            SwitchRow(
                title = "Suggest only",
                subtitle = "I plan and list changes but never make them. Repeating tasks always work this way.",
                checked = s.manualMode,
                onChange = vm::setManualMode,
            )
            HorizontalDivider()
            SwitchRow(
                title = "Retry failed steps with a second model",
                subtitle = "If a step fails, a different AI model takes over for the retry.",
                checked = s.automaticModelRouting,
                onChange = vm::setAutomaticModelRouting,
            )
            if (s.automaticModelRouting) {
                ModelPicker(
                    label = "Backup model",
                    value = s.recoveryModel,
                    models = s.availableModels,
                    suggested = null,
                    placeholder = "Choose a backup model",
                    onPick = vm::setRecoveryModel,
                )
                if (s.availableModels.isEmpty()) {
                    OutlinedTextField(
                        value = s.recoveryModel, onValueChange = vm::setRecoveryModel, label = { Text("Backup model name") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (s.recoveryModel.isBlank() || s.recoveryModel == s.model) {
                    Text(
                        "Pick a backup model that's different from your main one, or turn this off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    AppCard {
        CardTitle("About")
        Text("Device Agent${if (version.isNotBlank()) " · version $version" else ""}", style = MaterialTheme.typography.bodyMedium)
        Hint("Your key and your history stay on this phone. Only your request and file names (and file contents, if you allow it) are sent to the AI service you chose.")
    }
}
