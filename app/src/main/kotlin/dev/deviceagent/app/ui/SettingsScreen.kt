package dev.deviceagent.app.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
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

private enum class SettingsPage(val title: String) {
    AI("AI service"),
    FOLDERS("Folders and access"),
    PRIVACY("Privacy"),
    ADVANCED("Advanced"),
    ABOUT("About Fileora"),
}

/** Settings is a short list of groups; each group opens on its own page. */
@Composable
internal fun SettingsScreen(s: UiState, vm: AgentViewModel) {
    var pageName by rememberSaveable { mutableStateOf<String?>(null) }
    val page = SettingsPage.entries.firstOrNull { it.name == pageName }
    BackHandler(enabled = page != null) { pageName = null }
    if (page == null) SettingsHome(s) { pageName = it.name } else SettingsDetail(page, s, vm) { pageName = null }
}

@Composable
private fun SettingsHome(s: UiState, onOpen: (SettingsPage) -> Unit) {
    val context = LocalContext.current
    val name = providerName(s.provider)
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 4.dp)) }
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    SettingsRow(
                        { FileoraIcon(FileoraIconName.AiSparkle, contentDescription = null, modifier = Modifier.size(28.dp)) },
                        "AI service",
                        if (s.apiKeyMask == null) "Add your API key" else "$name · ${s.model.ifBlank { "choose a model" }}",
                    ) { onOpen(SettingsPage.AI) }
                    RowDivider()
                    SettingsRow(
                        { FileoraIcon(FileoraIconName.Folder, contentDescription = null, modifier = Modifier.size(28.dp)) },
                        "Folders and access",
                        when {
                            s.useAllFilesAccess && s.allFilesAccessGranted -> "All shared storage"
                            s.useAllFilesAccess -> "Waiting for Android permission"
                            s.roots.isEmpty() -> "No folders yet"
                            else -> "${s.roots.size} folder" + if (s.roots.size == 1) "" else "s"
                        },
                    ) { onOpen(SettingsPage.FOLDERS) }
                    RowDivider()
                    SettingsRow(
                        { FileoraIcon(FileoraIconName.PrivacyLock, contentDescription = null, modifier = Modifier.size(28.dp)) },
                        "Privacy",
                        if (s.sendFileContents) "$name can read file contents" else "$name sees file names only",
                    ) { onOpen(SettingsPage.PRIVACY) }
                    RowDivider()
                    SettingsRow(
                        { FileoraIcon(FileoraIconName.Notifications, contentDescription = null, modifier = Modifier.size(28.dp)) },
                        "Notifications",
                        if (s.notificationsEnabled) "On · opens Android settings" else "Off · tap to turn on",
                    ) { openNotificationSettings(context) }
                    RowDivider()
                    SettingsRow(
                        { FileoraIcon(FileoraIconName.Settings, contentDescription = null, modifier = Modifier.size(28.dp)) },
                        "Advanced",
                        if (s.manualMode) "Suggest only is on" else "Suggest-only mode, backup model",
                    ) { onOpen(SettingsPage.ADVANCED) }
                    RowDivider()
                    SettingsRow(
                        { Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) },
                        "About Fileora",
                        if (version.isNotBlank()) "Version $version" else "Version and privacy",
                    ) { onOpen(SettingsPage.ABOUT) }
                }
            }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SettingsRow(icon: @Composable () -> Unit, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { icon() }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Hint(subtitle)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsDetail(page: SettingsPage, s: UiState, vm: AgentViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 8.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Settings") }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(page.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 4.dp)) }
            item {
                when (page) {
                    SettingsPage.AI -> AiServiceCard(s, vm)
                    SettingsPage.FOLDERS -> FoldersCard(s, vm)
                    SettingsPage.PRIVACY -> PrivacyCard(s, vm)
                    SettingsPage.ADVANCED -> AdvancedCard(s, vm)
                    SettingsPage.ABOUT -> AboutCard()
                }
            }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
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
                IconButton(onClick = { removeTarget = r }) { FileoraIcon(FileoraIconName.Delete, contentDescription = "Remove ${r.name}") }
            }
        }
        OutlinedButton(onClick = { pickFolder.launch(null) }) {
            FileoraIcon(FileoraIconName.Add, contentDescription = null)
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
        Text("Fileora${if (version.isNotBlank()) " · version $version" else ""}", style = MaterialTheme.typography.bodyMedium)
        Hint("Your key and your history stay on this phone. Only your request and file names (and file contents, if you allow it) are sent to the AI service you chose.")
    }
}
